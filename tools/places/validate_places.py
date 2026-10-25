#!/usr/bin/env python3
"""places.json dogrulayici. Kullanim: python3 tools/places/validate_places.py [dosya]"""
import json
import os
import re
import sys

KITALAR = {"Asya", "Avrupa", "Afrika", "Kuzey Amerika", "Güney Amerika", "Okyanusya"}
BOLGELER = {"avrupa", "asya", "ortadogu", "afrika", "amerika", "okyanusya"}
ALANLAR = ("id", "ad", "kita", "bolge", "baskent", "bilgi1", "bilgi2")
BEKLENEN_SAYI = 100
MIN_BOLGE = 8
MAX_BILGI = 130

HARF = "A-Za-zÇĞİıÖŞÜçğöşüÂâÎîÛû"
AD_RE = re.compile(r"^[" + HARF + r"][" + HARF + r" '\-]*$")
BILGI_RE = re.compile(r"^[" + HARF + r" ,;:!?'\-.]+$")
ID_RE = re.compile(r"^[a-z]{2,3}$")

DEFAULT_PATH = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "..", "app", "src", "main", "assets", "places.json",
)


def validate(data):
    """Hata mesajlari listesi dondurur; bos liste gecerli demektir."""
    errors = []
    if not isinstance(data, list):
        return ["kok oge liste olmali"]
    if len(data) != BEKLENEN_SAYI:
        errors.append("kayit sayisi %d, beklenen %d" % (len(data), BEKLENEN_SAYI))
    ids, adlar = set(), set()
    bolge_sayac = {b: 0 for b in BOLGELER}
    for i, r in enumerate(data):
        etiket = "#%d" % (i + 1)
        if not isinstance(r, dict):
            errors.append("%s: kayit sozluk olmali" % etiket)
            continue
        etiket = "#%d (%s)" % (i + 1, r.get("id"))
        eksik = False
        for a in ALANLAR:
            v = r.get(a)
            if not isinstance(v, str) or not v.strip():
                errors.append("%s: '%s' alani eksik, bos ya da metin degil" % (etiket, a))
                eksik = True
        if eksik:
            continue
        if not ID_RE.match(r["id"]):
            errors.append("%s: gecersiz id" % etiket)
        if r["id"] in ids:
            errors.append("%s: tekrar eden id" % etiket)
        ids.add(r["id"])
        if r["ad"] in adlar:
            errors.append("%s: tekrar eden ad" % etiket)
        adlar.add(r["ad"])
        for a in ("ad", "baskent"):
            if not AD_RE.match(r[a]):
                errors.append("%s: '%s' alaninda gecersiz karakter" % (etiket, a))
        if r["kita"] not in KITALAR:
            errors.append("%s: gecersiz kita '%s'" % (etiket, r["kita"]))
        if r["bolge"] not in BOLGELER:
            errors.append("%s: gecersiz bolge '%s'" % (etiket, r["bolge"]))
        else:
            bolge_sayac[r["bolge"]] += 1
        for a in ("bilgi1", "bilgi2"):
            s = r[a]
            if len(s) > MAX_BILGI:
                errors.append("%s: %s %d karakter, en fazla %d" % (etiket, a, len(s), MAX_BILGI))
            if not s.endswith("."):
                errors.append("%s: %s noktayla bitmeli" % (etiket, a))
            if s.count(".") != 1:
                errors.append("%s: %s tek cumle olmali" % (etiket, a))
            if not BILGI_RE.match(s):
                errors.append("%s: %s emoji, parantez, rakam ya da garip simge iceriyor" % (etiket, a))
        if r["bilgi1"] == r["bilgi2"]:
            errors.append("%s: bilgi1 ve bilgi2 ayni" % etiket)
    if data and isinstance(data[0], dict) and data[0].get("ad") != "Türkiye":
        errors.append("ilk kayit Türkiye olmali")
    for b, n in sorted(bolge_sayac.items()):
        if n < MIN_BOLGE:
            errors.append("bolge '%s' icin %d yer var, en az %d gerekli" % (b, n, MIN_BOLGE))
    return errors


def main(argv):
    path = argv[1] if len(argv) > 1 else DEFAULT_PATH
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    errors = validate(data)
    if errors:
        for e in errors:
            print("HATA:", e)
        print("Gecersiz: %d hata" % len(errors))
        return 1
    sayac = {}
    for r in data:
        sayac[r["bolge"]] = sayac.get(r["bolge"], 0) + 1
    print("Gecerli: %d yer; bolgeler: %s" % (len(data), ", ".join("%s=%d" % kv for kv in sorted(sayac.items()))))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
