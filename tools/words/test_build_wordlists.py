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


class IVariantTest(unittest.TestCase):
    def test_variants(self):
        self.assertEqual(sorted(bw.ivariants("adi")), ["adı"])
        self.assertEqual(sorted(bw.ivariants("kitap")), ["kıtap"])
        self.assertEqual(sorted(bw.ivariants("aşık")), [])  # i yok
        self.assertEqual(sorted(bw.ivariants("iki")), ["ikı", "ıki", "ıkı"])

    def test_oddity_needs_much_more_common_variant(self):
        acc = {"adi", "adı", "dişi", "dışı", "kil", "kıl"}
        cnt = {"adi": 9020, "adı": 62958, "dişi": 5912, "dışı": 19534, "kil": 565, "kıl": 2280}
        self.assertTrue(bw.is_ivariant_oddity("adi", acc, cnt))       # 7x: yadırgatıcı yazılış
        self.assertFalse(bw.is_ivariant_oddity("dişi", acc, cnt))     # 3.3x: ayrı, yaygın sözcük
        self.assertFalse(bw.is_ivariant_oddity("adı", acc, cnt))      # yön: yalnız i -> ı
        self.assertFalse(bw.is_ivariant_oddity("adi", {"adi"}, cnt))  # ı'lı biçim kabul setinde değil


class BlockedRootsTest(unittest.TestCase):
    def test_isemek_blocks_forms_but_not_ise(self):
        b = bw.blocked_set(["işemek"])
        self.assertIn("işemek", b)
        self.assertNotIn("işe", b)  # iş + yönelme ("işe yaramaz"); ROOT_ALLOW
        self.assertTrue(bw.is_blocked_form("işedi", [row("işemek", "işe", "Verb")], b))
        self.assertFalse(bw.is_blocked_form("işe", [row("iş"), row("işemek", "işe", "Verb")], b))


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
        for w in ("aptal", "salak", "öküz", "eşek", "meme", "it", "karı", "döl", "kaka",
                  "hıyar", "moruk", "gerzek", "embesil", "zampara", "lavuk", "geber", "defol", "sümük"):
            self.assertNotIn(w, self.blocklist)  # hafif/nötr: kabul setinde kalır (ızgaraya girmez, aşağıya bakın)

    # ---- QA raporu (2026-10) regresyonları ----
    QA_FULLY_REMOVED = (
        "düzüş düzüşen düzüşme düzüşüp işemek işeme işedi işer işeyen frikik ibiş kaşar kaşara kavata kerata deyyus "
        "dallama kıç kıça kıçı kıçım kıçında kodumu kodumun kodumuz anan ananı ananın ananla ananızı anasını anasına "
        "bacın avrat hödük kıro osuruk gebert zıkkım yuh oha lan ulan penis penisi penise vajina anüs sperm sperma "
        "porno orgazm kondom mastur masturi seksisi seksüel hitler hitleri çingene kafir kafirce").split()
    QA_KEPT_ACCEPTED_NOT_IN_GRID = "hıyar öküz moruk gerzek embesil zampara lavuk geber defol sümük".split()
    QA_POOL_EXCLUDED = (
        "penis porno orgazm sperm kondom taciz cinsel zina meni testis kıç meme döl kasık sütyen bakire metres çiş apış "
        "osur kaka kak kuka halt lavuk zampara yalaka yalak geberik geber moruk hıyar gerzek embesil defol çirkef leş "
        "ali eren ayla kaya mine emin rina rami nisa sena alperen asuman hande nafiz feyzal gökmen bahadır aksu "
        "aut bop bre flu gri ski uzo yoo jig jip jem jen jul jüt rua ruz rop pog poy zom şom şua evç ekü "
        "ayni kani kari akil akim ari ati atik adi bati bani").split()

    def test_qa_words_fully_removed_from_accept_set_and_grid(self):
        for w in self.QA_FULLY_REMOVED:
            self.assertNotIn(w, self.words, w)
            self.assertNotIn(w, self.pool, w)

    def test_qa_mild_insults_stay_accepted_but_never_in_grid(self):
        for w in self.QA_KEPT_ACCEPTED_NOT_IN_GRID:
            self.assertIn(w, self.words, w)
            self.assertNotIn(w, self.pool, w)

    def test_qa_pool_exclusions(self):
        for w in self.QA_POOL_EXCLUDED:
            self.assertNotIn(w, self.pool, w)
        # kabul setinde kalabilenler: oyuncu yazarsa geçersiz sayılmasın
        for w in ("taciz", "cinsel", "zina", "meme", "ali", "kaya", "emin", "ayni", "adi"):
            self.assertIn(w, self.words, w)
        # bağlam sözcükleri engellenmedi
        for w in ("iş", "işe", "işte", "bacı", "ananas", "anason", "kod", "kıçüstü"):
            self.assertIn(w, self.words, w)

    def test_ud_added_through_accept_include(self):
        self.assertIn("ud", bw.read_list(bw.ACCEPT_INCLUDE))
        self.assertIn("ud", self.words)
        self.assertNotIn("ud", self.pool)

    def test_no_pool_word_has_odd_ivariant(self):
        # ı'lı biçimi kabul setinde ve >= IVAR_RATIO kat yaygın olan i'li sözcük havuza girmez
        freq = {}
        for w, c in bw.read_freq(os.path.join(bw.HERE, ".cache", "tr_full.txt")) if os.path.exists(
                os.path.join(bw.HERE, ".cache", "tr_full.txt")) else []:
            freq.setdefault(w, c)
        if not freq:
            self.skipTest("sıklık önbelleği yok")
        bad = [w for w in self.pool if bw.is_ivariant_oddity(w, self.words, freq)]
        self.assertEqual(bad, [])

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
