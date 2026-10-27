"""Seviye doğrulayıcı. Kullanım: python3 validate_levels.py [levels.txt.gz] [--words words.txt]"""
import argparse
import os
import sys
from collections import Counter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from generate_levels import (  # noqa: E402
    DEFAULT_BLOCKLIST, DEFAULT_OUT, DEFAULT_POOL, DEFAULT_WORDS, EASY_RANKS, MAX_GRID, MAX_REPS, MIN_DIFF,
    load_words, read_levels, parse_level,
)

# (son seviye id, ızgara sözcüğü havuz sırası < sınır): erken seviyeler yaygın sözcüklerden kurulur
EARLY_RANK_LIMITS = ((60, EASY_RANKS[0]), (700, EASY_RANKS[1]))

import gzip  # noqa: E402


def validate(level, words_set, pool_set=None, blocked=None):
    """Tek seviye denetimi. pool_set verilirse ızgara kelimeleri havuzda olmalı; blocked verilirse
    hiçbir kelime engelli listede olmamalı; words_set (kabul seti) her zaman denetlenir."""
    errors = []
    pool = Counter(level.letters)
    if not 3 <= len(level.letters) <= 7:
        errors.append("harf sayısı 3-7 dışında: %d" % len(level.letters))
    if len(level.words) < 2:
        errors.append("en az 2 kelime gerekli")

    seen = set()
    for p in level.words:
        w = p.word
        if w in seen:
            errors.append("kelime tekrarı: %s" % w)
        seen.add(w)
        if len(w) < 3:
            errors.append("kelime çok kısa: %s" % w)
        if w not in words_set:
            errors.append("sözlükte yok: %s" % w)
        if pool_set is not None and w not in pool_set:
            errors.append("ızgara havuzunda yok: %s" % w)
        if blocked and w in blocked:
            errors.append("engelli listede: %s" % w)
        need = Counter(w)
        if any(need[ch] > pool[ch] for ch in need):
            errors.append("havuzdan yazılamıyor: %s" % w)
        if p.dir not in ("y", "d"):
            errors.append("geçersiz yön: %s" % w)
        if p.row < 0 or p.col < 0:
            errors.append("negatif konum: %s" % w)
    if errors and any(p.dir not in ("y", "d") for p in level.words):
        return errors

    grid = {}
    for p in level.words:
        dr, dc = (0, 1) if p.dir == "y" else (1, 0)
        for k, ch in enumerate(p.word):
            pos = (p.row + dr * k, p.col + dc * k)
            if grid.get(pos, ch) != ch:
                errors.append("kesişimde harf çakışması: %s hücre %s" % (p.word, pos))
            grid.setdefault(pos, ch)
    if not grid:
        return errors + ["boş ızgara"]

    rows = [r for r, _ in grid]
    cols = [c for _, c in grid]
    if min(rows) != 0 or min(cols) != 0:
        errors.append("konumlar normalleştirilmemiş")
    if max(rows) - min(rows) + 1 > MAX_GRID or max(cols) - min(cols) + 1 > MAX_GRID:
        errors.append("ızgara %dx%d sınırı aşıyor" % (MAX_GRID, MAX_GRID))

    # ızgaradaki tüm >=2 uzunluktaki diziler planlanmış olmalı
    planned = {(p.word, p.row, p.col, p.dir) for p in level.words}
    runs = set()
    for (r, c) in grid:
        if (r, c - 1) not in grid and (r, c + 1) in grid:
            k = c
            s = ""
            while (r, k) in grid:
                s += grid[(r, k)]
                k += 1
            runs.add((s, r, c, "y"))
        if (r - 1, c) not in grid and (r + 1, c) in grid:
            k = r
            s = ""
            while (k, c) in grid:
                s += grid[(k, c)]
                k += 1
            runs.add((s, r, c, "d"))
    for run in sorted(runs - planned):
        errors.append("plansız dizi: %s (%d,%d,%s)" % run)
    for pl in sorted(planned - runs):
        errors.append("yerleşim ızgarayla uyuşmuyor: %s (%d,%d,%s)" % pl)

    # bağlantılılık: kelimeler kesişim hücreleriyle birleşir
    parent = list(range(len(level.words)))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    owner = {}
    for i, p in enumerate(level.words):
        dr, dc = (0, 1) if p.dir == "y" else (1, 0)
        for k in range(len(p.word)):
            pos = (p.row + dr * k, p.col + dc * k)
            if pos in owner:
                parent[find(i)] = find(owner[pos])
            else:
                owner[pos] = i
    if len({find(i) for i in range(len(level.words))}) > 1:
        errors.append("ızgara bağlantılı değil")
    return errors


def check_monotonic(levels):
    """Zorluk sırası denetimi. Döner: (hatalar, rapor).

    Harf sayısı seviye numarasıyla hiç düşmemeli (sert hata). Kelime sayısı
    düşüşleri sayılıp rapora yazılır (ilk 20 yer). Bu sürümde küresel kelime
    monotonluğu sağlandığı için herhangi bir kelime düşüşü de sert hatadır.
    """
    errors = []
    letter_drops = []
    word_drops = []
    for prev, cur in zip(levels, levels[1:]):
        if len(cur.letters) < len(prev.letters):
            letter_drops.append((cur.id, len(prev.letters), len(cur.letters)))
        if len(cur.words) < len(prev.words):
            word_drops.append((cur.id, len(prev.words), len(cur.words)))
    for lid, a, b in letter_drops[:20]:
        errors.append("harf sayısı düştü: seviye %d (%d -> %d)" % (lid, a, b))
    if len(letter_drops) > 20:
        errors.append("... toplam %d harf düşüşü" % len(letter_drops))
    for lid, a, b in word_drops[:20]:
        errors.append("kelime sayısı düştü: seviye %d (%d -> %d)" % (lid, a, b))
    if len(word_drops) > 20:
        errors.append("... toplam %d kelime düşüşü" % len(word_drops))
    report = {"letter_drops": len(letter_drops), "word_drops": len(word_drops),
              "word_drop_places": word_drops[:20]}
    return errors, report


BANDS = ((3, "3 harf"), (4, "4 harf"), (5, "5 harf"), (6, "6 harf"), (7, "7 harf"))


def print_table(levels, step=1000):
    print("id aralığı: harf ort/min/maks | kelime ort/min/maks")
    for i in range(0, len(levels), step):
        chunk = levels[i:i + step]
        ls = [len(l.letters) for l in chunk]
        ws = [len(l.words) for l in chunk]
        print("%5d-%5d: %.2f/%d/%d | %.2f/%d/%d" % (
            chunk[0].id, chunk[-1].id, sum(ls) / len(ls), min(ls), max(ls),
            sum(ws) / len(ws), min(ws), max(ws)))


def print_band_table(levels):
    print("harf sayısına göre dağılım: id aralığı | seviye | kelime min/ort/maks")
    for L, name in BANDS:
        chunk = [l for l in levels if len(l.letters) == L]
        if not chunk:
            print("%s: yok" % name)
            continue
        ws = [len(l.words) for l in chunk]
        print("%s: %d-%d | %d | %d/%.1f/%d" % (name, chunk[0].id, chunk[-1].id, len(chunk), min(ws),
                                          sum(ws) / len(ws), max(ws)))


def check_reuse(levels):
    """Harf kümesi tekrarları: en çok MAX_REPS, kelime kümeleri >= MIN_DIFF farklı. Döner (hatalar, istatistik)."""
    from collections import defaultdict
    groups = defaultdict(list)
    for l in levels:
        groups[("".join(sorted(l.letters)))].append(l)
    errors = []
    dist = []
    for key, lst in groups.items():
        if len(lst) > MAX_REPS:
            errors.append("harf kümesi %s %d kez kullanılmış (en çok %d)" % (key, len(lst), MAX_REPS))
        for i in range(len(lst)):
            for j in range(i + 1, len(lst)):
                a = frozenset(p.word for p in lst[i].words)
                b = frozenset(p.word for p in lst[j].words)
                d = 1.0 - len(a & b) / len(a | b)
                if d < MIN_DIFF:
                    errors.append("seviye %d ve %d aynı harf kümesi, kelime farkı %.2f < %.2f"
                                  % (lst[i].id, lst[j].id, d, MIN_DIFF))
                dist.append(abs(lst[j].id - lst[i].id))
    stats = {
        "classes": len(groups),
        "reused_classes": sum(1 for v in groups.values() if len(v) > 1),
        "by_count": {n: sum(1 for v in groups.values() if len(v) == n) for n in range(1, MAX_REPS + 1)},
        "min_gap": min(dist) if dist else None,
        "median_gap": sorted(dist)[len(dist) // 2] if dist else None,
    }
    return errors[:20], stats


def check_curve(levels):
    """Hedef eğrinin SERT kısımları (ilk 100 seviyede <= 6 üç harfli, 40. seviye >= 4 harf ve >= 4 kelime)."""
    errors = []
    first = levels[:100]
    n3 = sum(1 for l in first if len(l.letters) == 3)
    if n3 > 6:
        errors.append("ilk 100 seviyede %d üç harfli seviye (en çok 6)" % n3)
    if len(levels) >= 40:
        l40 = levels[39]
        if len(l40.letters) < 4 or len(l40.words) < 4:
            errors.append("40. seviye: %d harf, %d kelime (en az 4 harf ve 4 kelime)" % (len(l40.letters), len(l40.words)))
    return errors


def check_early_rank(levels, pool_list, limits=EARLY_RANK_LIMITS):
    """Erken seviyelerde ızgara sözcüğünün sıklık sırası (grid_pool.txt'teki sıra, 0 = en yaygın) sınırın altında mı?
    Döner (hatalar, {id sınırı: erken seviyelerdeki en büyük sıra})."""
    rank = {w: i for i, w in enumerate(pool_list)}
    errors = []
    worst = {}
    for last_id, limit in limits:
        top = (-1, None, None)
        for l in levels:
            if l.id > last_id:
                break
            for p in l.words:
                r = rank.get(p.word, 1 << 30)
                if r > top[0]:
                    top = (r, l.id, p.word)
        worst[last_id] = top[0]
        if top[0] >= limit:
            errors.append("seviye %d (<= %d): '%s' havuz sırası %d >= %d" % (top[1], last_id, top[2], top[0], limit))
    return errors, worst


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("levels", nargs="?", default=DEFAULT_OUT)
    ap.add_argument("--words", default=DEFAULT_WORDS)
    ap.add_argument("--pool", default=DEFAULT_POOL)
    ap.add_argument("--blocklist", default=DEFAULT_BLOCKLIST)
    ap.add_argument("--max-bytes", type=int, default=6_000_000)
    a = ap.parse_args(argv)
    words_set = set(load_words(a.words))
    pool_set = set(load_words(a.pool))
    blocked = set(load_words(a.blocklist))
    bad = 0
    keys = set()
    dup = 0
    n = 0
    with gzip.open(a.levels, "rb") as f:
        lines = f.read().decode("utf-8").split("\n")
    levels = []
    for line in lines:
        if not line.strip():
            continue
        n += 1
        try:
            lv = parse_level(line)
        except Exception as e:  # noqa: BLE001
            print("satır %d ayrıştırılamadı: %s" % (n, e))
            bad += 1
            continue
        levels.append(lv)
        errs = validate(lv, words_set, pool_set, blocked)
        if lv.id != n:
            errs.append("ardışık olmayan id (beklenen %d, bulunan %d)" % (n, lv.id))
        key = ("".join(sorted(lv.letters)), frozenset(p.word for p in lv.words))
        if key in keys:
            errs.append("tekrarlanan seviye (harf+kelime kümesi)")
            dup += 1
        keys.add(key)
        if errs:
            bad += 1
            if bad <= 20:
                print("seviye %d: %s" % (lv.id, "; ".join(errs)))
    mono_errs, rep = check_monotonic(levels)
    reuse_errs, reuse = check_reuse(levels)
    curve_errs = check_curve(levels)
    early_errs, early_worst = check_early_rank(levels, load_words(a.pool))
    curve_errs += early_errs
    print("erken seviyelerde en büyük havuz sırası: %s" % early_worst)
    for e in mono_errs + reuse_errs + curve_errs:
        print(e)
    bad += len(mono_errs) + len(reuse_errs) + len(curve_errs)
    print("harf düşüşü: %d, kelime düşüşü: %d" % (rep["letter_drops"], rep["word_drops"]))
    for lid, x, y in rep["word_drop_places"]:
        print("  kelime düşüşü: seviye %d (%d -> %d)" % (lid, x, y))
    print_table(levels)
    print_band_table(levels)
    print("harf kümesi tekrarı: %s" % reuse)
    size = os.path.getsize(a.levels)
    if size >= a.max_bytes:
        print("dosya çok büyük: %d bayt" % size)
        bad += 1
    print("seviye: %d, hatalı: %d, tekrar: %d, boyut: %d bayt (%.2f MB)" % (n, bad, dup, size, size / 1e6))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
