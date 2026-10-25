import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from clean_words import ALPHABET, clean, turkish_lower  # noqa: E402


class TurkishLowerTest(unittest.TestCase):
    def test_dotted_and_dotless_i(self):
        self.assertEqual(turkish_lower("IŞIK"), "ışık")
        self.assertEqual(turkish_lower("İZMİR"), "izmir")
        self.assertEqual(turkish_lower("ISI"), "ısı")
        self.assertEqual(turkish_lower("İSİ"), "isi")

    def test_other_letters(self):
        self.assertEqual(turkish_lower("ÇĞÖŞÜ"), "çğöşü")


class CleanTest(unittest.TestCase):
    def test_plan_example(self):
        got = clean(["Ağaç", "ağaç", "at  yarışı", "Ankara", "a-b", "İzmir", "kitap"])
        self.assertEqual(got, ["ağaç", "kitap"])

    def test_multiword_hyphen_digit_apostrophe_dropped(self):
        self.assertEqual(clean(["at yarışı", "a-b", "x2", "ab1", "Ali'nin", "ev'e", "kalem"]), ["kalem"])

    def test_all_caps_is_lowercased_with_turkish_rules(self):
        self.assertEqual(clean(["IŞIK", "ISI", "İSİ"]), ["ısı", "ışık", "isi"])

    def test_isi_and_isi_are_different_words(self):
        self.assertEqual(sorted(clean(["ısı", "isi"])), sorted(["ısı", "isi"]))
        self.assertEqual(len(clean(["ısı", "isi"])), 2)

    def test_proper_noun_marked_by_capital_dropped(self):
        self.assertEqual(clean(["İzmir", "Ankara", "Duma"]), [])

    def test_capitalised_kept_if_lowercase_form_present(self):
        self.assertEqual(clean(["Kalem", "kalem"]), ["kalem"])
        self.assertEqual(clean(["Ilık", "ılık"]), ["ılık"])

    def test_foreign_letters_dropped(self):
        self.assertEqual(clean(["q", "qq", "wc", "xyz", "taxi", "wifi", "kalem"]), ["kalem"])

    def test_length_bounds(self):
        self.assertEqual(clean(["a", "ev", "a" * 15, "a" * 16]), ["a" * 15, "ev"])

    def test_two_letter_abbreviations_without_vowel_dropped(self):
        self.assertEqual(clean(["cm", "mg", "kr", "br", "ev", "su"]), ["ev", "su"])

    def test_circumflex_folded(self):
        self.assertEqual(clean(["dükkân", "bekâr", "hâlâ", "kâr"]), ["bekar", "dükkan", "hala", "kar"])

    def test_dedup_and_sorted_by_turkish_alphabet(self):
        got = clean(["zeki", "çay", "cam", "ağaç", "ada", "ışık", "iki", "ılık", "şef", "sol", "ölü", "oda", "ümit", "ufuk", "gül", "göl", "gol", "cam"])
        self.assertEqual(got, ["ada", "ağaç", "cam", "çay", "gol", "göl", "gül", "ılık", "ışık", "iki",
                               "oda", "ölü", "sol", "şef", "ufuk", "ümit", "zeki"])

    def test_whitespace_and_blank_lines(self):
        self.assertEqual(clean(["  kalem \n", "", "\n", "kitap\r\n"]), ["kalem", "kitap"])

    def test_output_only_alphabet_letters(self):
        for w in clean(["çiçek", "güneş", "ğ", "ğa", "Âdem", "âlem"]):
            self.assertTrue(set(w) <= set(ALPHABET), w)
        self.assertEqual(ALPHABET, "abcçdefgğhıijklmnoöprsştuüvyz")


if __name__ == "__main__":
    unittest.main()
