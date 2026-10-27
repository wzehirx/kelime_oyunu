"""Kelime oyunu seviye üreticisi (v1.4).

Format (levels.txt.gz, gzip + UTF-8, satır başına bir seviye):
    id|harfler|kelime,satır,sütun,y;kelime,satır,sütun,d;...
y = yatay (soldan sağa), d = dikey (yukarıdan aşağı); satır/sütun 0 tabanlı.

Kelime kaynakları: ızgara kelimeleri YALNIZCA ızgara havuzundan (tools/words/grid_pool.txt,
sıklık sırasıyla), doğrulama/bonus için kabul setinden (app/.../words.txt).

Zorluk sırası (SERT kurallar): seviye numarası arttıkça harf sayısı ASLA düşmez ve kelime sayısı
küresel olarak düşmez (id sırası (harf, kelime) çiftlerinin üzerinde monoton bir zincirdir).
Hedef eğri (10.000 seviye): 3 harf ilk 6, 4 harf ~7-30, 5 harf ~31-200, 6 harf ~201-3500, kalanı 7 harf.
Küçük havuzda aynı harf kümesi en çok MAX_REPS kez, birbirinden en az %50 farklı kelime kümeleriyle
yeniden kullanılır; 6/7 harf sınırı ile gerekirse 5/6 harf bandı, arzın izin verdiği kadar kaydırılır.
"""
import argparse
import gzip
import itertools
import multiprocessing
import os
import random
import sys
from collections import namedtuple

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
DEFAULT_WORDS = os.path.join(REPO, "app", "src", "main", "assets", "words.txt")
DEFAULT_POOL = os.path.join(REPO, "tools", "words", "grid_pool.txt")
DEFAULT_BLOCKLIST = os.path.join(REPO, "tools", "words", "blocklist.txt")
DEFAULT_OUT = os.path.join(REPO, "app", "src", "main", "assets", "levels.txt.gz")

ALPHABET = frozenset("abcçdefgğhıijklmnoöprsştuüvyz")
MAX_GRID = 9
MIN_WORD = 3

Placement = namedtuple("Placement", "word row col dir")
Level = namedtuple("Level", "id letters words")  # words: tuple[Placement]

# harf sayısı -> (aday kurarken en az, en çok kelime hedefi)
GROUPS = {
    3: (2, 4),
    4: (3, 6),
    5: (5, 9),
    6: (8, 13),
    7: (10, 16),
}
# 10.000 seviyelik hedef bantlar (kümülatif son seviye): 3 harf ilk 6, 4 harf 7-30, 5 harf 31-200, 6 harf 201-3500.
BAND_ENDS = {3: 6, 4: 30, 5: 200, 6: 3500}
BASE_COUNT = 10000
# 3-5 harfli bantlarda kelime sayısı tavanı (zincir: sonraki bant bu tavandan başlar); 6. bant için arama yapılır.
WORD_CAPS = {3: 3, 4: 5, 5: 8}
SPLIT_MIN = 10          # 6 harfli bant en az bu kelime sayısına kadar çıkar; 7 harfli bant buradan başlar
MAX_REPS = 3            # aynı harf kümesinin en çok kullanımı
MIN_DIFF = 0.5          # tekrarlarda kelime kümeleri arası en az fark: 1 - |A∩B| / |A∪B|
BUILDS_PER_CLASS = {3: 4, 4: 8, 5: 16, 6: 28, 7: 32}

# Kısa ve yaygın görünen kelimeler için ağırlık (uzunluk -> ağırlık).
LENGTH_WEIGHT = {3: 1.0, 4: 3.0, 5: 3.0, 6: 2.0, 7: 0.7}


# ---------------------------------------------------------------- format
def format_level(level):
    ws = ";".join("%s,%d,%d,%s" % (p.word, p.row, p.col, p.dir) for p in level.words)
    return "%d|%s|%s" % (level.id, level.letters, ws)


def parse_level(line):
    line = line.rstrip("\r\n")
    parts = line.split("|")
    if len(parts) != 3:
        raise ValueError("beklenmeyen alan sayısı: %r" % line)
    words = []
    if parts[2]:
        for item in parts[2].split(";"):
            f = item.split(",")
            if len(f) != 4 or f[3] not in ("y", "d"):
                raise ValueError("bozuk kelime alanı: %r" % item)
            words.append(Placement(f[0], int(f[1]), int(f[2]), f[3]))
    return Level(int(parts[0]), parts[1], tuple(words))


def write_levels(path, levels):
    data = "".join(format_level(l) + "\n" for l in levels).encode("utf-8")
    with open(path, "wb") as raw:
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0, compresslevel=9) as gz:
            gz.write(data)


def read_levels(path):
    with gzip.open(path, "rb") as f:
        text = f.read().decode("utf-8")
    return [parse_level(l) for l in text.split("\n") if l.strip()]


# ---------------------------------------------------------------- index
def build_index(words):
    index = {}
    for w in words:
        if MIN_WORD <= len(w) <= 7 and all(ch in ALPHABET for ch in w):
            index.setdefault("".join(sorted(w)), []).append(w)
    for k in index:
        index[k] = sorted(set(index[k]))
    return index


def spellable(pool, index):
    key_pool = sorted(pool)
    keys = set()
    for k in range(MIN_WORD, len(key_pool) + 1):
        for comb in itertools.combinations(key_pool, k):
            keys.add("".join(comb))
    out = []
    for k in sorted(keys):
        out.extend(index.get(k, ()))
    return out


# ---------------------------------------------------------------- placement
class Grid:
    def __init__(self):
        self.cells = {}
        self.by_letter = {}
        self.placed = []
        self.box = None  # minr, maxr, minc, maxc

    def _newbox(self, word, r, c, d):
        n = len(word)
        r2, c2 = (r, c + n - 1) if d == "y" else (r + n - 1, c)
        if self.box is None:
            return (r, r2, c, c2)
        a, b, cc, dd = self.box
        return (min(a, r), max(b, r2), min(cc, c), max(dd, c2))

    def can_place(self, word, r, c, d):
        n = len(word)
        dr, dc = (0, 1) if d == "y" else (1, 0)
        cells = self.cells
        if (r - dr, c - dc) in cells or (r + dr * n, c + dc * n) in cells:
            return False
        a, b, cc, dd = self._newbox(word, r, c, d)
        if b - a + 1 > MAX_GRID or dd - cc + 1 > MAX_GRID:
            return False
        crossings = 0
        prev_existing = False
        for k, ch in enumerate(word):
            pos = (r + dr * k, c + dc * k)
            cur = cells.get(pos)
            if cur is not None:
                if cur != ch or prev_existing:
                    return False
                crossings += 1
                prev_existing = True
            else:
                prev_existing = False
                if d == "y":
                    side = ((pos[0] - 1, pos[1]), (pos[0] + 1, pos[1]))
                else:
                    side = ((pos[0], pos[1] - 1), (pos[0], pos[1] + 1))
                if side[0] in cells or side[1] in cells:
                    return False
        return 1 <= crossings < n

    def place(self, word, r, c, d):
        self.box = self._newbox(word, r, c, d)
        dr, dc = (0, 1) if d == "y" else (1, 0)
        for k, ch in enumerate(word):
            pos = (r + dr * k, c + dc * k)
            if pos not in self.cells:
                self.cells[pos] = ch
                self.by_letter.setdefault(ch, []).append(pos)
        self.placed.append(Placement(word, r, c, d))

    def options(self, word):
        seen = set()
        out = []
        for i, ch in enumerate(word):
            for (r, c) in self.by_letter.get(ch, ()):
                for d in ("y", "d"):
                    start = (r, c - i, d) if d == "y" else (r - i, c, d)
                    if start in seen:
                        continue
                    seen.add(start)
                    if self.can_place(word, start[0], start[1], d):
                        out.append(start)
        return out

    def normalized(self):
        a, _, cc, _ = self.box
        return tuple(Placement(p.word, p.row - a, p.col - cc, p.dir) for p in self.placed)


_COMMON = {}  # kelime -> 0..1 (1 = havuzun en yaygını); fork ile işçilere paylaşılır


def word_weight(w):
    return LENGTH_WEIGHT.get(len(w), 1.0) * (1.0 + 2.0 * _COMMON.get(w, 0.5))


def weighted_order(cands, rng):
    keyed = []
    for w in cands:
        keyed.append((rng.random() ** (1.0 / word_weight(w)), w))
    keyed.sort(reverse=True)
    return [w for _, w in keyed]


def build_grid(base, cands, target, rng):
    grid = Grid()
    grid.place(base, 0, 0, "y")
    used = {base}
    order = [w for w in weighted_order(cands, rng) if w != base]
    progress = True
    while progress and len(grid.placed) < target:
        progress = False
        for w in order:
            if w in used:
                continue
            opts = grid.options(w)
            if not opts:
                continue
            r, c, d = opts[rng.randrange(len(opts))]
            grid.place(w, r, c, d)
            used.add(w)
            progress = True
            if len(grid.placed) >= target:
                break
    return grid


def shuffle_letters(base, rng):
    letters = list(base)
    for _ in range(10):
        rng.shuffle(letters)
        if "".join(letters) != base:
            break
    return "".join(letters)


# ---------------------------------------------------------------- aday havuzu
Candidate = namedtuple("Candidate", "klass letters words placements score")


def _score(placements):
    """Kısa/yaygın kelimeli seviyeler daha yüksek puan alır."""
    return sum(word_weight(p.word) for p in placements) / len(placements)


def _diff(a, b):
    """1 - Jaccard benzerliği."""
    return 1.0 - len(a & b) / len(a | b)


_SHARED = None  # (index,) — fork ile işçilere paylaşılır


def _build_class(args):
    """Bir anagram sınıfı (aynı harf çoklu kümesi) için birkaç aday kurar."""
    seed, key, bases, index = args
    L = len(key)
    lo, hi = GROUPS[L]
    rng = random.Random("%d:%s" % (seed, key))
    cands = spellable(key, index)
    if len(cands) < 2:
        return []
    out = []
    seen = set()
    for _ in range(BUILDS_PER_CLASS[L]):
        base = max(rng.sample(bases, min(2, len(bases))), key=lambda w: _COMMON.get(w, 0.0))
        drop = rng.choice((0.0, 0.0, 0.2, 0.4))
        pool = [w for w in cands if w == base or rng.random() >= drop]
        target = rng.randint(lo, hi)
        grid = build_grid(base, pool, target, rng)
        placements = grid.normalized()
        if len(placements) < 2:
            continue
        ws = frozenset(p.word for p in placements)
        if ws in seen:
            continue
        seen.add(ws)
        out.append(Candidate(key, base, ws, placements, _score(placements)))
    return out


def _build_shared(args):
    seed, key, bases = args
    return _build_class((seed, key, bases, _SHARED[0]))


def build_pool(words, seed, log=None, processes=None):
    """{harf sayısı: [Candidate]} — havuz, seed ile bire bir yeniden üretilebilir."""
    index = build_index(words)
    jobs = []
    for key in sorted(index):
        if len(key) in GROUPS:
            jobs.append((seed, key, index[key]))
    if processes is None:
        processes = min(4, os.cpu_count() or 1) if len(jobs) > 500 else 1
    global _SHARED
    _SHARED = (index,)
    if processes > 1:
        with multiprocessing.get_context("fork").Pool(processes) as p:
            results = p.map(_build_shared, jobs, chunksize=32)
    else:
        results = [_build_shared(j) for j in jobs]
    pool = {L: [] for L in GROUPS}
    for cands in results:
        for c in cands:
            pool[len(c.klass)].append(c)
    if log:
        log("aday havuzu: " + ", ".join("%d harf: %d" % (L, len(v)) for L, v in sorted(pool.items())))
    return pool


# ---------------------------------------------------------------- seçim
def _pick(cands, quota, wlo, whi, mode, used):
    """Kelime sayısı [wlo, whi] aralığındaki adaylardan en çok `quota` seviye seçer.

    mode "asc": düşük kelime sayısından başlayarak; "fill": sayılara eşit yayarak.
    Önce her harf kümesinden bir seviye, sonra (MAX_REPS'e kadar, kelime kümesi >= MIN_DIFF farklı) tekrarlar.
    `used` {klass: [wordset]} yerinde güncellenir. Döner: seçilenler (seçim sırasıyla).
    """
    buckets = {}
    for c in cands:
        n = len(c.words)
        if wlo <= n <= whi:
            buckets.setdefault(n, []).append(c)
    for n in buckets:
        buckets[n].sort(key=lambda c: (-c.score, c.klass, sorted(c.words)))
    ws = sorted(buckets)
    chosen = []

    def ok(c, reuse):
        # reuse 0: yalnız kullanılmamış harf kümeleri; 1: farklı kelime sayısıyla tekrar; 2: herhangi bir tekrar.
        # Tekrarlar >= MIN_DIFF farklı kelime kümesi taşımalı; en çok MAX_REPS kullanım.
        u = used.get(c.klass)
        if not u:
            return True
        if reuse == 0 or len(u) >= MAX_REPS:
            return False
        if reuse == 1 and any(len(c.words) == len(x) for x in u):
            return False
        return all(_diff(c.words, x) >= MIN_DIFF for x in u)

    def take(c):
        used.setdefault(c.klass, []).append(c.words)
        chosen.append(c)

    for reuse in (0, 1, 2):
        if len(chosen) >= quota:
            break
        if mode == "asc":
            for n in ws:
                for c in buckets[n]:
                    if len(chosen) >= quota:
                        break
                    if ok(c, reuse):
                        take(c)
        else:
            pos = {n: 0 for n in ws}
            while len(chosen) < quota:
                progressed = False
                for n in ws:
                    lst = buckets[n]
                    while pos[n] < len(lst) and not ok(lst[pos[n]], reuse):
                        pos[n] += 1
                    if pos[n] < len(lst) and len(chosen) < quota:
                        take(lst[pos[n]])
                        pos[n] += 1
                        progressed = True
                if not progressed:
                    break
    return chosen


def _band_sizes(count):
    """Hedef bant sonları (kümülatif), count'a göre ölçeklenir."""
    scale = count / BASE_COUNT
    ends = {}
    prev = 0
    for L in (3, 4, 5, 6):
        e = max(prev + 1, int(round(BAND_ENDS[L] * scale))) if count >= 10 else prev
        ends[L] = min(e, count)
        prev = ends[L]
    return ends


EXT5_STEPS = (0, 100, 200, 350, 500, 700, 1000, 1400, 1900, 2500)


def plan_levels(pool, count, log=None):
    """Zincirli seçim. Döner (seçilen aday listesi [(harf, [aday])], rapor).

    Bantlar 3, 4, 5 hedef boyutta seçilir; 6/7 sınırı (b) ve 6. bandın uzunluğu arza göre aranır. 6. ve 7.
    bandın toplam arzı kalan seviyeleri karşılamıyorsa 5 harfli bant EXT5_STEPS adımlarıyla uzatılır.
    """
    ends = _band_sizes(count)
    report = {}
    used3, used4 = {}, {}
    chosen = []
    floor = 0
    carry = 0
    for L, used in ((3, used3), (4, used4)):
        quota = ends[L] - (ends[L - 1] if L > 3 else 0) + carry
        quota = min(quota, count - sum(len(x) for _, x in chosen))
        lo = max(floor, GROUPS[L][0])
        picked = _pick(pool[L], quota, lo, max(lo, WORD_CAPS[L]), "fill", used) if quota > 0 else []
        carry = max(0, quota - len(picked))
        if picked:
            floor = max(len(c.words) for c in picked)
        chosen.append((L, picked))
        report["band%d" % L] = len(picked)
    n34 = sum(len(x) for _, x in chosen)
    base5 = ends[5] - ends[4] + carry
    report["target5"] = base5
    for ext in EXT5_STEPS:
        used5 = {}
        quota5 = min(base5 + ext, count - n34)
        lo = max(floor, GROUPS[5][0])
        p5 = _pick(pool[5], quota5, lo, max(lo, WORD_CAPS[5]), "fill", used5) if quota5 > 0 else []
        floor5 = max([len(c.words) for c in p5] or [floor])
        rest = count - n34 - len(p5)
        lo6 = max(floor5, GROUPS[6][0])
        t6 = max(0, ends[6] - ends[5])
        best = None
        search = []
        for b in range(max(lo6, SPLIT_MIN), GROUPS[6][1] + 1):
            s6 = len(_pick(pool[6], rest, lo6, b, "fill", {}))
            s7 = len(_pick(pool[7], rest, b, GROUPS[7][1], "fill", {}))
            search.append((b, s6, s7))
            if s6 + s7 < rest:
                continue
            q6 = max(min(t6, rest), rest - s7)
            if q6 > s6:
                continue
            cand = (q6 - min(t6, rest), b, q6)
            if best is None or cand < best:
                best = cand
        report["search"] = search
        report["ext5"] = ext
        if best is None:
            if len(p5) < quota5:  # 5 harfli arz da bitti: daha fazla uzatmanın anlamı yok
                break
            continue
        _, b, q6 = best
        used6, used7 = {}, {}
        p6 = _pick(pool[6], q6, lo6, b, "fill", used6)
        floor6 = max([len(c.words) for c in p6] or [lo6])
        p7 = _pick(pool[7], rest - len(p6), floor6, GROUPS[7][1], "fill", used7)
        chosen.append((5, p5))
        chosen.append((6, p6))
        chosen.append((7, p7))
        report.update(band5=len(p5), band6=len(p6), band7=len(p7), split_b=b)
        report.pop("infeasible", None)
        return chosen, report
    report["infeasible"] = True
    return chosen, report


def generate(words, count, seed, log=None, processes=None, pool=None, blocked=()):
    """words: kabul seti (doğrulama); pool: ızgara kelimeleri, YAYGINLIK SIRASIYLA (yoksa words)."""
    from validate_levels import validate

    words = sorted(set(w for w in words if w))
    words_set = set(words)
    grid_words = list(dict.fromkeys(w for w in (pool if pool is not None else words) if w in words_set))
    grid_words = [w for w in grid_words if w not in set(blocked)]
    n = max(1, len(grid_words) - 1)
    _COMMON.clear()
    for i, w in enumerate(grid_words):
        _COMMON[w] = 1.0 - i / n if pool is not None else 0.5
    cand_pool = build_pool(grid_words, seed, log=log, processes=processes)
    rng = random.Random(seed)

    chosen, report = plan_levels(cand_pool, count, log=log)
    flat = [(L, c) for L, picked in chosen for c in picked]
    relaxed = 0
    if len(flat) < count:
        if log:
            log("bant planı yetmedi: %s" % (report,))
        # Küçük/zayıf havuz (yalnızca testler): kalan adayları monotonluğu gevşeterek ekle.
        taken = {(c.klass, c.words) for _, c in flat}
        rest_c = []
        for L in sorted(GROUPS):
            rest_c.extend(c for c in cand_pool[L] if (c.klass, c.words) not in taken)
        rest_c.sort(key=lambda c: (len(c.klass), -c.score, c.klass, sorted(c.words)))
        for c in rest_c:
            if len(flat) >= count:
                break
            flat.append((len(c.klass), c))
            relaxed += 1
    if len(flat) < count:
        raise RuntimeError("yeterli seviye yok: %d/%d (%s)" % (len(flat), count, report))
    # Sıra: (harf, kelime) zinciri; aynı kovada önce ilk kullanımlar, sonra tekrarlar (aynı harf kümesinin
    # tekrarları kovanın içinde bile birbirinden uzak kalır), sonra yaygın kelimeli ızgaralar öne.
    seen_reps = {}
    rep_idx = []
    for _, c in flat:
        rep_idx.append(seen_reps.get(c.klass, 0))
        seen_reps[c.klass] = rep_idx[-1] + 1
    order = sorted(range(len(flat)), key=lambda i: (
        flat[i][0], len(flat[i][1].words), rep_idx[i], -flat[i][1].score, flat[i][1].klass, i))
    levels = []
    for lid, i in enumerate(order, 1):
        c = flat[i][1]
        letters = shuffle_letters(c.letters, rng)
        level = Level(lid, letters, c.placements)
        errs = validate(level, words_set, pool_set=set(grid_words), blocked=set(blocked))
        if errs:
            raise RuntimeError("üretici hatalı seviye üretti %d: %s" % (lid, errs))
        levels.append(level)
    if log:
        log("plan raporu: %s" % (report,))
        log("gevşetilerek eklenen seviye sayısı: %d" % relaxed)
    generate.last_relaxed = relaxed
    generate.last_report = report
    return levels


def load_words(path):
    with open(path, encoding="utf-8") as f:
        return [l.split("#", 1)[0].strip() for l in f if l.split("#", 1)[0].strip()]


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--count", type=int, default=10000)
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--words", default=DEFAULT_WORDS)
    ap.add_argument("--pool", default=DEFAULT_POOL)
    ap.add_argument("--blocklist", default=DEFAULT_BLOCKLIST)
    ap.add_argument("--out", default=DEFAULT_OUT)
    a = ap.parse_args(argv)
    words = load_words(a.words)
    pool = load_words(a.pool)
    blocked = load_words(a.blocklist)
    print("%d kabul kelimesi, %d ızgara kelimesi, %d engelli" % (len(words), len(pool), len(blocked)))
    levels = generate(words, a.count, a.seed, log=print, pool=pool, blocked=blocked)
    write_levels(a.out, levels)
    print("%d seviye yazıldı: %s (%d bayt)" % (len(levels), a.out, os.path.getsize(a.out)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
