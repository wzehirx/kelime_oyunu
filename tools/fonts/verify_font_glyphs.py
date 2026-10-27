#!/usr/bin/env python3
"""Gömülü yazı tipi dosyalarında Türkçe harflerin (ç ğ ı İ ö ş ü ve büyükleri) bulunduğunu doğrular.

Yalnızca standart kütüphane kullanır (TTF 'cmap' tablosu, biçim 4 ve 12). Kullanım:
    python3 tools/fonts/verify_font_glyphs.py app/src/main/res/font/*.ttf
Eksik harf varsa çıkış kodu 1 olur. Ayrıca SHA-256 yazdırır (assets/licenses/ATKINSON-FONTS.txt ile karşılaştır).
"""
import hashlib
import struct
import sys

REQUIRED = "çğıöşüÇĞİÖŞÜ"


def cmap_codepoints(data: bytes) -> set:
    num_tables = struct.unpack(">H", data[4:6])[0]
    cmap_off = None
    for i in range(num_tables):
        tag, _, off, _ = struct.unpack(">4sIII", data[12 + 16 * i: 28 + 16 * i])
        if tag == b"cmap":
            cmap_off = off
    if cmap_off is None:
        raise SystemExit("cmap tablosu yok")
    n = struct.unpack(">H", data[cmap_off + 2: cmap_off + 4])[0]
    points = set()
    for i in range(n):
        _, _, sub = struct.unpack(">HHI", data[cmap_off + 4 + 8 * i: cmap_off + 12 + 8 * i])
        off = cmap_off + sub
        fmt = struct.unpack(">H", data[off: off + 2])[0]
        if fmt == 4:
            segx2 = struct.unpack(">H", data[off + 6: off + 8])[0]
            seg = segx2 // 2
            ends = struct.unpack(">%dH" % seg, data[off + 14: off + 14 + segx2])
            starts = struct.unpack(">%dH" % seg, data[off + 16 + segx2: off + 16 + 2 * segx2])
            for s, e in zip(starts, ends):
                if s != 0xFFFF:
                    points.update(range(s, e + 1))
        elif fmt == 12:
            groups = struct.unpack(">I", data[off + 12: off + 16])[0]
            for g in range(groups):
                s, e, _ = struct.unpack(">III", data[off + 16 + 12 * g: off + 28 + 12 * g])
                points.update(range(s, e + 1))
    return points


def main(paths):
    bad = 0
    for p in paths:
        data = open(p, "rb").read()
        have = cmap_codepoints(data)
        missing = [c for c in REQUIRED if ord(c) not in have]
        print(f"{p}: sha256={hashlib.sha256(data).hexdigest()} eksik={''.join(missing) or 'yok'}")
        bad += bool(missing)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
