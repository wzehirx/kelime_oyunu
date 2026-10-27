import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import build_wordlists as bw  # noqa: E402
from clean_words import ALPHABET  # noqa: E402

BLOCKED = bw.blocked_set(["am", "sikmek", "göt", "zenci"])


def row(lemma, root=None, pos="Noun", sec="None", informal=False):
    return (lemma, root or lemma, pos, sec, informal)


class PolicyTest(unittest.TestCase):
    def test_verb_roots_from_mastars(self):
        self.assertEqual(bw.verb_roots_from_mastars(["satmak", "gelmek", "kalem", "ok", "ymek", "mak"]),
                         {"sat", "gel"})  # 'mak' -> '' ve 'ymek' -> 'y' (1 harf) alınmaz

    def test_blocked_set_adds_verb_roots(self):
        self.assertIn("sik", BLOCKED)
        self.assertIn("sikmek", BLOCKED)

    def test_inflection_of_blocked_lemma_is_blocked(self):
        self.assertTrue(bw.is_blocked_form("amlar", [row("am")], BLOCKED))
        self.assertTrue(bw.is_blocked_form("sikti", [row("sikmek", "sik", "Verb")], BLOCKED))

    def test_ambiguous_word_with_clean_analysis_is_kept(self):
        # 'ama' bağlaçtır; am+Dat çözümlemesi de vardır ama temiz çözümleme olduğu için engellenmez
        rows = [row("am"), row("ama", pos="Conjunction")]
        self.assertFalse(bw.is_blocked_form("ama", rows, BLOCKED))

    def test_surface_in_blocklist_always_blocked(self):
        self.assertTrue(bw.is_blocked_form("göt", [row("göt"), row("göt", pos="Adjective")], BLOCKED))
        self.assertTrue(bw.is_blocked_form("am", [], BLOCKED))

    def test_good_rows_filters_proper_abbreviation_informal(self):
        rows = [row("Ankara", sec="ProperNoun"), row("kdv", sec="Abbreviation"),
                row("falan", informal=True), row("kitap")]
        self.assertEqual([r[0] for r in bw.good_rows(rows, set(), bw.ACCEPT_POS)], ["kitap"])
        self.assertEqual(bw.good_rows([row("ah", pos="Interjection")], set(), bw.ACCEPT_POS), [])

    def test_pool_ok_wants_uninflected_content_words(self):
        self.assertTrue(bw.pool_ok("kitap", [row("kitap")], set()))
        self.assertTrue(bw.pool_ok("sat", [row("satmak", "sat", "Verb")], set()))
        self.assertFalse(bw.pool_ok("evler", [row("ev")], set()))       # çekimli
        self.assertFalse(bw.pool_ok("satar", [row("satmak", "sat", "Verb")], set()))  # çekimli fiil
        self.assertFalse(bw.pool_ok("ben", [row("ben", pos="Pronoun")], set()))  # içerik sözcüğü değil
        self.assertFalse(bw.pool_ok("ankara", [row("Ankara", sec="ProperNoun")], set()))


def _read(path):
    with open(path, encoding="utf-8") as f:
        return [l.rstrip("\n") for l in f]


class OutputFilesTest(unittest.TestCase):
    """Üretilmiş words.txt ve grid_pool.txt üzerinde kullanıcı hatası regresyonları."""

    @classmethod
    def setUpClass(cls):
        cls.words_lines = _read(bw.OUT_WORDS)
        cls.words = set(cls.words_lines)
        cls.pool_lines = _read(bw.OUT_POOL)
        cls.pool = set(cls.pool_lines)
        cls.blocklist = set(bw.read_list(bw.BLOCKLIST))

    def test_words_format(self):
        self.assertEqual(self.words_lines, sorted(self.words_lines, key=bw.sort_key))
        self.assertEqual(len(self.words_lines), len(self.words))
        for w in self.words_lines:
            self.assertTrue(2 <= len(w) <= 7 and set(w) <= set(ALPHABET), w)

    def test_user_reported_words_accepted(self):
        for w in ("sat", "yat", "tas", "taş", "evler", "kitap", "gel", "yaz"):
            self.assertIn(w, self.words)

    def test_slang_rejected(self):
        for w in ("am", "sik", "göt", "bok", "piç", "çük", "ibne", "puşt", "yarak"):
            self.assertNotIn(w, self.words)

    def test_whole_blocklist_rejected_everywhere(self):
        self.assertGreaterEqual(len(self.blocklist), 90)
        self.assertEqual(self.blocklist & self.words, set())
        self.assertEqual(self.blocklist & self.pool, set())
        extra = set(bw.read_list(bw.BLOCKLIST_EXTRA))
        self.assertEqual(extra & self.words, set())
        self.assertEqual(extra & self.pool, set())

    def test_blocklist_contents(self):
        cand = set(bw.read_list(os.path.join(bw.HERE, "blocklist_candidates.txt")))
        self.assertTrue(cand <= self.blocklist)
        for w in bw.AMBIGUOUS_TAKEN:
            self.assertIn(w, self.blocklist)
        for w in ("aptal", "salak", "öküz", "eşek", "meme", "it", "karı", "kıç", "döl", "kaka"):
            self.assertNotIn(w, self.blocklist)  # hafif/nötr: oyunda kalır

    def test_ast_accepted_but_not_in_grid_pool(self):
        self.assertIn("ast", self.words)
        self.assertNotIn("ast", self.pool)

    def test_pool_shape(self):
        self.assertLessEqual(self.pool, self.words)
        self.assertGreaterEqual(len(self.pool), 8000)
        for w in self.pool:
            self.assertTrue(3 <= len(w) <= 7)
        self.assertIn("sat", self.pool)
        self.assertNotIn("evler", self.pool)  # çekimli biçimler ızgaraya girmez


if __name__ == "__main__":
    unittest.main()
