"""Kelime oyunu seviye üreticisi.

Format (levels.txt.gz, gzip + UTF-8, satır başına bir seviye):
    id|harfler|kelime,satır,sütun,y;kelime,satır,sütun,d;...
y = yatay (soldan sağa), d = dikey (yukarıdan aşağı); satır/sütun 0 tabanlı.

Zorluk sırası (v1.2): seviye numarası arttıkça harf sayısı ASLA düşmez (önce
tüm 3 harfliler, sonra 4, 5, 6, 7) ve kelime sayısı da küresel olarak düşmez.
Üretim: her harf grubu için geniş bir aday havuzu kurulur; gruplar sırayla
seçilir, her grup bir önceki grubun son kelime sayısının altındaki adayları
eler ("floor" yöntemi) ve grup içinde kelime sayısına göre sıralanır.
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
DEFAULT_OUT = os.path.join(REPO, "app", "src", "main", "assets", "levels.txt.gz")

ALPHABET = frozenset("abcçdefgğhıijklmnoöprsştuüvyz")
MAX_GRID = 9
MIN_WORD = 3

Placement = namedtuple("Placement", "word row col dir")
Level = namedtuple("Level", "id letters words")  # words: tuple[Placement]

# harf sayısı -> (havuz için en düşük kelime hedefi, en çok kelime)
GROUPS = {
    3: (2, 4),
    4: (3, 6),
    5: (6, 10),
    6: (10, 13),
    7: (13, 16),
}
# Grupların seviye payı. 3 harfli grup: mümkün olan tüm benzersiz seviyeler.
# 7 harfli grup kalan seviyeleri alır.
SHARES = {4: 0.045, 5: 0.16, 6: 0.38}
BUILDS_PER_CLASS = 6

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


def weighted_order(cands, rng):
    keyed = []
    for w in cands:
        wt = LENGTH_WEIGHT.get(len(w), 1.0)
        keyed.append((rng.random() ** (1.0 / wt), w))
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


# ---------------------------------------------------------------- generate
Candidate = namedtuple("Candidate", "klass letters words placements score")


def _score(placements):
    """Kısa/yaygın kelimeli seviyeler daha yüksek puan alır."""
    return sum(LENGTH_WEIGHT.get(len(p.word), 1.0) for p in placements) / len(placements)


def _build_class(args):
    """Bir anagram sınıfı (aynı harf çoklu kümesi) için birkaç aday kurar."""
    seed, key, bases, index = args
    L = len(key)
    lo, hi = GROUPS[L]
    rng = random.Random("%d:%s" % (seed, key))
    out = []
    seen = set()
    for _ in range(BUILDS_PER_CLASS):
        base = bases[rng.randrange(len(bases))]
        cands = spellable(base, index)
        if len(cands) < 2:
            return out
        target = rng.randint(lo, hi)
        grid = build_grid(base, cands, target, rng)
        placements = grid.normalized()
        if len(placements) < 2:
            continue
        ws = frozenset(p.word for p in placements)
        if ws in seen:
            continue
        seen.add(ws)
        out.append(Candidate(key, base, ws, placements, _score(placements)))
    return out


def build_pool(words, seed, log=None, processes=None):
    """{harf sayısı: [Candidate]} — havuz, seed ile bire bir yeniden üretilebilir."""
    index = build_index(words)
    jobs = []
    for key in sorted(index):
        if len(key) in GROUPS:
            jobs.append((seed, key, index[key], index))
    if processes is None:
        processes = min(4, os.cpu_count() or 1) if len(jobs) > 1500 else 1
    if processes > 1:
        # index büyük: fork ile paylaş, işe yalnızca küçük anahtar gönder
        global _SHARED
        _SHARED = (index,)
        with multiprocessing.get_context("fork").Pool(processes) as pool:
            results = pool.map(_build_shared, [(j[0], j[1], j[2]) for j in jobs], chunksize=64)
    else:
        results = [_build_class(j) for j in jobs]
    pool = {L: [] for L in GROUPS}
    for cands in results:
        for c in cands:
            pool[len(c.klass)].append(c)
    if log:
        log("aday havuzu: " + ", ".join("%d harf: %d" % (L, len(v)) for L, v in sorted(pool.items())))
    return pool


_SHARED = None


def _build_shared(args):
    seed, key, bases = args
    return _build_class((seed, key, bases, _SHARED[0]))


def _select_group(cands, quota, floor, cap, taken, rng):
    """Kelime sayısı [floor, cap] aralığındaki adaylardan, sayılara eşit
    yayılmış (water-filling) en iyi `quota` adayı seçer. Önce sınıf başına bir
    seviye; yetmezse aynı harf kümesinden farklı kelime kümeli seviyeler."""
    buckets = {}
    for c in cands:
        n = len(c.words)
        if floor <= n <= cap and (c.klass, c.words) not in taken:
            buckets.setdefault(n, []).append(c)
    for n in buckets:
        buckets[n].sort(key=lambda c: (-c.score, c.klass, sorted(c.words)))
    chosen = []
    used_class = set()
    for reuse in (False, True):
        pos = {n: 0 for n in buckets}
        while len(chosen) < quota:
            active = []
            for n in sorted(buckets):
                lst = buckets[n]
                while pos[n] < len(lst) and (
                    (not reuse and lst[pos[n]].klass in used_class)
                    or (lst[pos[n]].klass, lst[pos[n]].words) in taken
                ):
                    pos[n] += 1
                if pos[n] < len(lst):
                    active.append(n)
            if not active:
                break
            share = max(1, (quota - len(chosen)) // len(active))
            progressed = False
            for n in active:
                for _ in range(share):
                    lst = buckets[n]
                    while pos[n] < len(lst) and (
                        (not reuse and lst[pos[n]].klass in used_class)
                        or (lst[pos[n]].klass, lst[pos[n]].words) in taken
                    ):
                        pos[n] += 1
                    if pos[n] >= len(lst) or len(chosen) >= quota:
                        break
                    c = lst[pos[n]]
                    pos[n] += 1
                    chosen.append(c)
                    used_class.add(c.klass)
                    taken.add((c.klass, c.words))
                    progressed = True
            if not progressed:
                break
        if len(chosen) >= quota:
            break
    return chosen


def generate(words, count, seed, log=None, processes=None):
    from validate_levels import validate

    words = sorted(set(w for w in words if w))
    words_set = set(words)
    pool = build_pool(words, seed, log=log, processes=processes)
    rng = random.Random(seed)

    quotas = {L: int(round(SHARES[L] * count)) for L in SHARES}
    chosen = []
    taken = set()
    floor = 0
    carry = 0
    stats = {}
    for L in sorted(GROUPS):
        _, cap = GROUPS[L]
        if L == 3:
            quota = count  # tümü; aşağıda sınıf başına bir seviye ile sınırlı
        elif L == 7:
            quota = count - len(chosen)
        else:
            quota = quotas[L] + carry
        quota = min(quota, count - len(chosen))
        if L == 3:
            picked = _select_group_one_per_class(pool[3], taken)
        else:
            picked = _select_group(pool[L], quota, floor, cap, taken, rng)
        carry = max(0, quota - len(picked)) if L != 3 else 0
        if picked:
            floor = max(len(c.words) for c in picked)
        stats[L] = len(picked)
        chosen.extend(picked)
    relaxed = 0
    if len(chosen) < count:
        # Küçük/zayıf sözlük: kalan adayları kelime monotonluğunu gevşeterek ekle.
        rest = []
        for L in sorted(GROUPS):
            rest.extend(c for c in pool[L] if (c.klass, c.words) not in taken)
        rest.sort(key=lambda c: (len(c.klass), -c.score, c.klass, sorted(c.words)))
        for c in rest:
            if len(chosen) >= count:
                break
            chosen.append(c)
            taken.add((c.klass, c.words))
            relaxed += 1
    if len(chosen) < count:
        raise RuntimeError("yeterli benzersiz seviye yok: %d/%d" % (len(chosen), count))
    # Harf sayısı sert garanti: (harf, kelime) sözlüksel sıra; eşitlikte seçim sırası.
    order = sorted(range(len(chosen)), key=lambda i: (len(chosen[i].klass), len(chosen[i].words), i))
    levels = []
    for lid, i in enumerate(order, 1):
        c = chosen[i]
        letters = shuffle_letters(c.letters, rng)
        level = Level(lid, letters, c.placements)
        errs = validate(level, words_set)
        if errs:
            raise RuntimeError("üretici hatalı seviye üretti %d: %s" % (lid, errs))
        levels.append(level)
    if log:
        log("grup başına seçilen: " + ", ".join("%d harf: %d" % (L, n) for L, n in sorted(stats.items())))
        log("gevşetilerek eklenen seviye sayısı: %d" % relaxed)
    generate.last_relaxed = relaxed
    return levels


def _select_group_one_per_class(cands, taken):
    """3 harfli grup: sınıf başına en kalabalık (sonra en iyi) tek seviye."""
    best = {}
    for c in cands:
        cur = best.get(c.klass)
        if cur is None or (len(c.words), c.score) > (len(cur.words), cur.score):
            best[c.klass] = c
    out = [best[k] for k in sorted(best)]
    for c in out:
        taken.add((c.klass, c.words))
    return out


def load_words(path):
    with open(path, encoding="utf-8") as f:
        return [l.strip() for l in f if l.strip()]


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--count", type=int, default=10000)
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--words", default=DEFAULT_WORDS)
    ap.add_argument("--out", default=DEFAULT_OUT)
    a = ap.parse_args(argv)
    words = load_words(a.words)
    print("%d kelime okundu" % len(words))
    levels = generate(words, a.count, a.seed, log=print)
    write_levels(a.out, levels)
    print("%d seviye yazıldı: %s (%d bayt)" % (len(levels), a.out, os.path.getsize(a.out)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
