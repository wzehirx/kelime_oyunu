#!/usr/bin/env python3
import copy
import json
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import validate_places as vp

BOLGE_KITA = [("avrupa", "Avrupa"), ("asya", "Asya"), ("ortadogu", "Asya"),
              ("afrika", "Afrika"), ("amerika", "Kuzey Amerika"), ("okyanusya", "Okyanusya")]


def iyi_veri():
    """100 kayitlik gecerli sentetik veri."""
    veri = [{"id": "tr", "ad": "Türkiye", "kita": "Asya", "bolge": "ortadogu",
             "baskent": "Ankara", "bilgi1": "Birinci bilgi cümlesi.", "bilgi2": "İkinci farklı bilgi."}]
    harf = "abcdefghijklmnopqrstuvwxyz"
    n = 0
    while len(veri) < 100:
        bolge, kita = BOLGE_KITA[n % 6]
        kod = "x" + harf[n % 26] + harf[(n // 26) % 26]
        veri.append({"id": kod, "ad": "Ülke " + kod, "kita": kita, "bolge": bolge,
                     "baskent": "Kent " + kod, "bilgi1": "Bu ülke " + kod + " hakkında bir bilgidir.",
                     "bilgi2": "Bu ülkenin ikinci bilgisi " + kod + " ile ilgilidir."})
        n += 1
    return veri


class ValidatorTest(unittest.TestCase):
    def hatalar(self, veri):
        return vp.validate(veri)

    def test_iyi_veri_gecer(self):
        self.assertEqual(self.hatalar(iyi_veri()), [])

    def test_yanlis_sayi(self):
        self.assertTrue(self.hatalar(iyi_veri()[:99]))

    def test_tekrar_id(self):
        v = iyi_veri(); v[5]["id"] = v[4]["id"]
        self.assertTrue(any("id" in e for e in self.hatalar(v)))

    def test_tekrar_ad(self):
        v = iyi_veri(); v[5]["ad"] = v[4]["ad"]
        self.assertTrue(any("ad" in e for e in self.hatalar(v)))

    def test_ilk_turkiye_degil(self):
        v = iyi_veri(); v[0], v[1] = v[1], v[0]
        self.assertTrue(any("Türkiye" in e for e in self.hatalar(v)))

    def test_bos_alan(self):
        v = iyi_veri(); v[3]["baskent"] = " "
        self.assertTrue(self.hatalar(v))

    def test_yanlis_tur(self):
        v = iyi_veri(); v[3]["bilgi1"] = 5
        self.assertTrue(self.hatalar(v))

    def test_gecersiz_kita_ve_bolge(self):
        v = iyi_veri(); v[3]["kita"] = "Antarktika"
        self.assertTrue(any("kita" in e for e in self.hatalar(v)))
        v = iyi_veri(); v[3]["bolge"] = "kuzey"
        self.assertTrue(any("bolge" in e for e in self.hatalar(v)))

    def test_uzun_bilgi(self):
        v = iyi_veri(); v[3]["bilgi1"] = "A" * 131 + "."
        self.assertTrue(any("en fazla" in e for e in self.hatalar(v)))

    def test_noktasiz_bilgi(self):
        v = iyi_veri(); v[3]["bilgi2"] = "Noktasız cümle"
        self.assertTrue(any("noktayla" in e for e in self.hatalar(v)))

    def test_yasakli_karakterler(self):
        for kotu in ("Parantezli (cümle).", "Emoji içerir \U0001F600.", "Rakam 123 var.",
                     "Garip simge # var.", "Kiril Ж var."):
            v = iyi_veri(); v[3]["bilgi1"] = kotu
            self.assertTrue(self.hatalar(v), kotu)

    def test_iki_cumle(self):
        v = iyi_veri(); v[3]["bilgi1"] = "Bir. İki."
        self.assertTrue(self.hatalar(v))

    def test_ayni_bilgiler(self):
        v = iyi_veri(); v[3]["bilgi2"] = v[3]["bilgi1"]
        self.assertTrue(any("ayni" in e for e in self.hatalar(v)))

    def test_bolge_yetersiz(self):
        v = iyi_veri()
        for r in v:
            if r["bolge"] == "okyanusya":
                r["bolge"] = "avrupa"; r["kita"] = "Avrupa"
        self.assertTrue(any("okyanusya" in e for e in self.hatalar(v)))

    def test_kok_liste_degil(self):
        self.assertTrue(self.hatalar({}))


class GercekDosyaTest(unittest.TestCase):
    def test_places_json_gecerli(self):
        with open(vp.DEFAULT_PATH, encoding="utf-8") as f:
            veri = json.load(f)
        self.assertEqual(vp.validate(veri), [])


if __name__ == "__main__":
    unittest.main()
