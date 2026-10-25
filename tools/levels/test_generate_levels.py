import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from generate_levels import (  # noqa: E402
    Level, Placement, format_level, generate, parse_level, read_levels, write_levels,
)
from validate_levels import check_monotonic, validate  # noqa: E402

WORDS = """kalem elma alem mal kal ala lem ela kale ekle leke kem mek kek
kelam melek lale ekmek mele ele elem kama mama kamel amel lama
ali alem kala mak kaleme malak makale makale kapak kaplan pala
kap pal alp lap pak kek kaka kakel akla kalp kala pek pekala""".split()


def L(i, letters, *ws):
    return Level(i, letters, tuple(Placement(*w) for w in ws))


class GenerateTest(unittest.TestCase):
    def test_generated_levels_validate(self):
        ws = set(WORDS)
        levels = generate(WORDS, 12, 7)
        self.assertEqual(len(levels), 12)
        self.assertEqual([l.id for l in levels], list(range(1, 13)))
        keys = set()
        for l in levels:
            self.assertEqual(validate(l, ws), [], format_level(l))
            self.assertTrue(3 <= len(l.letters) <= 7)
            self.assertGreaterEqual(len(l.words), 2)
            keys.add((tuple(sorted(l.letters)), frozenset(p.word for p in l.words)))
        self.assertEqual(len(keys), 12)

    def test_generated_levels_are_monotonic(self):
        levels = generate(WORDS, 12, 7)
        letters = [len(l.letters) for l in levels]
        self.assertEqual(letters, sorted(letters))
        # küçük sözlükte kelime monotonluğu gevşetilebilir; harf sayısı asla
        self.assertEqual(check_monotonic(levels)[1]["letter_drops"], 0)

    def test_determinism(self):
        a = generate(WORDS, 10, 3)
        b = generate(WORDS, 10, 3)
        self.assertEqual([format_level(x) for x in a], [format_level(x) for x in b])
        c = generate(WORDS, 10, 4)
        self.assertNotEqual([format_level(x) for x in a], [format_level(x) for x in c])

    def test_roundtrip(self):
        levels = generate(WORDS, 6, 1)
        with tempfile.TemporaryDirectory() as d:
            p1 = os.path.join(d, "a.gz")
            p2 = os.path.join(d, "b.gz")
            write_levels(p1, levels)
            back = read_levels(p1)
            self.assertEqual(back, levels)
            write_levels(p2, levels)
            with open(p1, "rb") as f1, open(p2, "rb") as f2:
                self.assertEqual(f1.read(), f2.read())
        l = parse_level("5|mlea|kalem,0,0,y;elma,0,1,d")
        self.assertEqual(l.id, 5)
        self.assertEqual(l.words[1], Placement("elma", 0, 1, "d"))
        self.assertEqual(format_level(l), "5|mlea|kalem,0,0,y;elma,0,1,d")


class MonotonicTest(unittest.TestCase):
    @staticmethod
    def lv(i, n_letters, n_words):
        letters = "abcdefg"[:n_letters]
        return Level(i, letters, tuple(Placement("w%d" % k, 0, 0, "y") for k in range(n_words)))

    def test_ok(self):
        levels = [self.lv(1, 3, 2), self.lv(2, 3, 3), self.lv(3, 4, 3), self.lv(4, 4, 5)]
        errs, rep = check_monotonic(levels)
        self.assertEqual(errs, [])
        self.assertEqual((rep["letter_drops"], rep["word_drops"]), (0, 0))

    def test_letter_drop_is_error(self):
        levels = [self.lv(1, 4, 3), self.lv(2, 3, 3)]
        errs, rep = check_monotonic(levels)
        self.assertEqual(rep["letter_drops"], 1)
        self.assertTrue(any("harf sayısı düştü" in e for e in errs), errs)

    def test_word_drop_counted_and_error(self):
        levels = [self.lv(1, 3, 3), self.lv(2, 4, 2), self.lv(3, 4, 4), self.lv(4, 5, 3)]
        errs, rep = check_monotonic(levels)
        self.assertEqual(rep["letter_drops"], 0)
        self.assertEqual(rep["word_drops"], 2)
        self.assertEqual(rep["word_drop_places"], [(2, 3, 2), (4, 4, 3)])
        self.assertEqual(len([e for e in errs if "kelime sayısı düştü" in e]), 2)

    def test_empty_and_single(self):
        self.assertEqual(check_monotonic([])[0], [])
        self.assertEqual(check_monotonic([self.lv(1, 3, 2)])[0], [])


class ValidateTest(unittest.TestCase):
    ws = {"kalem", "elma", "ela", "kal", "mal", "ale", "kale"}

    def test_good(self):
        # kalem yatay; elma dikey, e (0,3) -> kalem'in 'e' sütun 3
        good = L(1, "kalem", ("kalem", 0, 0, "y"), ("elma", 0, 3, "d"))
        # elma: e,l,m,a ; kalem 'e' 3. indeks, sütun 3 -> elma e kesişir
        self.assertEqual(validate(good, self.ws), [])

    def test_wrong_letter(self):
        bad = L(1, "kalem", ("kalem", 0, 0, "y"), ("elma", 0, 3, "d"))
        bad = L(1, "kalex", *bad.words)
        self.assertTrue(any("havuz" in e for e in validate(bad, self.ws)))

    def test_not_in_dictionary(self):
        bad = L(1, "kalem", ("kalem", 0, 0, "y"), ("elma", 0, 3, "d"))
        self.assertTrue(any("sözlükte" in e for e in validate(bad, {"kalem"})))

    def test_conflict(self):
        bad = L(1, "kalem", ("kalem", 0, 0, "y"), ("elma", 0, 2, "d"))
        self.assertTrue(any("çakışması" in e for e in validate(bad, self.ws)))

    def test_adjacent_wrong_word(self):
        # kalem (satır 0) ve altında ela (satır 1, sütun 0) -> dikey 'ke','al','la'.. plansız
        bad = L(1, "kalem", ("kalem", 0, 0, "y"), ("ela", 1, 0, "y"), ("mal", 0, 4, "d"))
        errs = validate(bad, self.ws)
        self.assertTrue(any("plansız" in e for e in errs), errs)

    def test_disconnected(self):
        bad = L(1, "kalem", ("kalem", 0, 0, "y"), ("ela", 3, 0, "y"))
        self.assertTrue(any("bağlantılı" in e for e in validate(bad, self.ws)))

    def test_duplicate_and_counts(self):
        dup = L(1, "kalem", ("kalem", 0, 0, "y"), ("kalem", 2, 0, "y"))
        self.assertTrue(any("tekrar" in e for e in validate(dup, self.ws)))
        single = L(1, "kalem", ("kalem", 0, 0, "y"))
        self.assertTrue(any("en az 2" in e for e in validate(single, self.ws)))
        small = L(1, "ka", ("kal", 0, 0, "y"), ("kal", 0, 0, "d"))
        self.assertTrue(any("3-7" in e for e in validate(small, self.ws)))

    def test_grid_too_big(self):
        ws = {"abcdefghij", "kal"}
        big = L(1, "abcdefg", ("abcdefghij", 0, 0, "y"), ("kal", 0, 0, "d"))
        self.assertTrue(any("sınır" in e for e in validate(big, ws)))


if __name__ == "__main__":
    unittest.main()
