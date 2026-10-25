"""Kelime oyunu seviye üreticisi.

Format (levels.txt.gz, gzip + UTF-8, satır başına bir seviye):
    id|harfler|kelime,satır,sütun,y;kelime,satır,sütun,d;...
y = yatay (soldan sağa), d = dikey (yukarıdan aşağı); satır/sütun 0 tabanlı.
"""
import argparse
import gzip
import itertools
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

# (ilk id, son id, (min harf, max harf), (min kelime, max kelime))
CURVE = [
    (1, 100, (3, 4), (2, 4)),
    (101, 1000, (4, 5), (3, 6)),
    (1001, 4000, (5, 6), (4, 9)),
    (4001, 10000, (6, 7), (5, 12)),
]

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


# ---------------------------------------------------------------- curve
def level_spec(level_id, rng):
    """(min_harf, max_harf, harf_sayısı, min_kelime, max_kelime, hedef_kelime)"""
    for a, b, (l_lo, l_hi), (w_lo, w_hi) in CURVE:
        if level_id <= b:
            break
    t = min(1.0, max(0.0, (level_id - a) / float(max(1, b - a))))
    letters = l_hi if rng.random() < t else l_lo
    target = round(w_lo + t * (w_hi - w_lo) + rng.uniform(-1.0, 1.0))
    target = max(w_lo, min(w_hi, target))
    return l_lo, l_hi, letters, w_lo, w_hi, target


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
def generate(words, count, seed, log=None):
    from validate_levels import validate

    rng = random.Random(seed)
    words = sorted(set(w for w in words if w))
    words_set = set(words)
    index = build_index(words)
    bases_by_len = {}
    for key in sorted(index):
        for w in index[key]:
            bases_by_len.setdefault(len(w), []).append(w)

    levels = []
    seen_keys = set()
    used_bases = set()
    relaxed = 0
    for lid in range(1, count + 1):
        l_lo, l_hi, n_letters, w_lo, w_hi, target = level_spec(lid, rng)
        built = None
        attempts = 0
        while built is None:
            attempts += 1
            lo_w, lo_l, hi_l, tgt = w_lo, l_lo, l_hi, target
            L = n_letters
            if attempts > 60:  # eğriyi gevşet
                lo_w, lo_l, hi_l = 2, 3, 7
                L = rng.randint(lo_l, hi_l)
                tgt = max(2, min(tgt, 4))
                if attempts > 400:
                    raise RuntimeError("seviye %d üretilemedi" % lid)
            elif attempts > 20:
                L = rng.randint(l_lo, l_hi)
            pool_bases = bases_by_len.get(L)
            if not pool_bases:
                continue
            base = pool_bases[rng.randrange(len(pool_bases))]
            if base in used_bases:
                continue
            cands = spellable(base, index)
            if len(cands) < lo_w:
                continue
            grid = build_grid(base, cands, tgt, rng)
            if len(grid.placed) < lo_w:
                continue
            placements = grid.normalized()
            letters = shuffle_letters(base, rng)
            key = ("".join(sorted(letters)), frozenset(p.word for p in placements))
            if key in seen_keys:
                continue
            level = Level(lid, letters, placements)
            errs = validate(level, words_set)
            if errs:
                raise RuntimeError("üretici hatalı seviye üretti %d: %s" % (lid, errs))
            built = (level, key, base)
            if attempts > 60:
                relaxed += 1
        level, key, base = built
        levels.append(level)
        seen_keys.add(key)
        used_bases.add(base)
        if log and lid % 1000 == 0:
            log("  %d/%d seviye" % (lid, count))
    if log:
        log("eğri gevşetilen seviye sayısı: %d" % relaxed)
    generate.last_relaxed = relaxed
    return levels


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
