"""Seviye doğrulayıcı. Kullanım: python3 validate_levels.py [levels.txt.gz] [--words words.txt]"""
import argparse
import os
import sys
from collections import Counter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from generate_levels import (  # noqa: E402
    DEFAULT_OUT, DEFAULT_WORDS, MAX_GRID, load_words, read_levels, parse_level,
)

import gzip  # noqa: E402


def validate(level, words_set):
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


def print_table(levels, step=1000):
    print("id aralığı: harf ort/min/maks | kelime ort/min/maks")
    for i in range(0, len(levels), step):
        chunk = levels[i:i + step]
        ls = [len(l.letters) for l in chunk]
        ws = [len(l.words) for l in chunk]
        print("%5d-%5d: %.2f/%d/%d | %.2f/%d/%d" % (
            chunk[0].id, chunk[-1].id, sum(ls) / len(ls), min(ls), max(ls),
            sum(ws) / len(ws), min(ws), max(ws)))


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("levels", nargs="?", default=DEFAULT_OUT)
    ap.add_argument("--words", default=DEFAULT_WORDS)
    a = ap.parse_args(argv)
    words_set = set(load_words(a.words))
    bad = 0
    keys = set()
    dup = 0
    n = 0
    with gzip.open(a.levels, "rb") as f:
        lines = f.read().decode("utf-8").split("\n")
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
        errs = validate(lv, words_set)
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
    levels = [parse_level(l) for l in lines if l.strip()]
    mono_errs, rep = check_monotonic(levels)
    for e in mono_errs:
        print(e)
    bad += len(mono_errs)
    print("harf düşüşü: %d, kelime düşüşü: %d" % (rep["letter_drops"], rep["word_drops"]))
    for lid, x, y in rep["word_drop_places"]:
        print("  kelime düşüşü: seviye %d (%d -> %d)" % (lid, x, y))
    print_table(levels)
    size = os.path.getsize(a.levels)
    print("seviye: %d, hatalı: %d, tekrar: %d, boyut: %d bayt (%.2f MB)" % (n, bad, dup, size, size / 1e6))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
