"""Kabul seti (words.txt) ve ızgara havuzu (grid_pool.txt) üretimi. build_wordlists.sh çağırır.

  prepare: TDK listesi + sıklık listesi + Zemberek fiil kökleri -> cache/candidates.txt
  select : cache/analysis.tsv (Zemberek çözümlemeleri) -> words.txt ve grid_pool.txt

Politika burada (Python, testlidir); Zemberek yalnızca ham çözümleme verir (zemberek/WordTool.java).
Ayrıntı ve eşik gerekçeleri: tools/words/SOURCES.md bölüm "v1.4".
"""
import argparse
import itertools
import os
import sys
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from clean_words import ALPHABET, _ORDER, turkish_lower  # noqa: E402

REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
TDK_WORDS = os.path.join(HERE, "tdk_words.txt")
BLOCKLIST = os.path.join(HERE, "blocklist.txt")
BLOCKLIST_EXTRA = os.path.join(HERE, "blocklist_extra_forms.txt")
POOL_EXCLUDE = os.path.join(HERE, "pool_exclude.txt")
ACCEPT_EXCLUDE = os.path.join(HERE, "accept_exclude.txt")
ACCEPT_INCLUDE = os.path.join(HERE, "accept_include.txt")
VOWELS = frozenset("aeıioöuü")
OUT_WORDS = os.path.join(REPO, "app", "src", "main", "assets", "words.txt")
OUT_POOL = os.path.join(HERE, "grid_pool.txt")

MIN_LEN, MAX_LEN = 2, 7           # kabul seti uzunluğu
POOL_MIN_LEN = 3                   # ızgara havuzu uzunluğu: 3-7

# Eşikler (gerekçe SOURCES.md "v1.4" bölümünde)
ACCEPT_MIN_COUNT = 20              # sıklık listesinden çekimli biçim kabulü için en az geçiş sayısı
# Izgara havuzu: sıklık listesinde en az bu kadar geçiş; kısa sözcükler daha sıkı (SOURCES.md'ye bakın).
# 3-4 harfli eşikler QA raporu sonrası yükseltildi (3: 50 -> 200, 4: 20 -> 40): çok nadir/yabancı-şüpheli
# kısa sözcükler (agu, kup, füg ...) ızgaraya girmesin.
POOL_MIN_COUNT = {3: 50, 4: 20, 5: 10, 6: 7, 7: 7}
# i/ı yazım varyantı: ızgara sözcüğünde bazı i'ler ı yapılınca KABUL SETİNDEKİ bir sözcük çıkıyor ve o,
# bu sözcükten en az bu kadar kat yaygınsa (ayni/aynı, akil/akıl, adi/adı ...) sözcük ızgaraya girmez.
IVAR_RATIO = 5
# Kök eşlemesinde engellenmeyecek fiil kökleri: "işemek" engelli ama "işe" = iş + yönelme (işe yaramaz).
ROOT_ALLOW = frozenset({"işe"})

ACCEPT_POS = {"Noun", "Adjective", "Adverb", "Verb", "Pronoun", "Conjunction",
              "PostPositive", "Determiner", "Question", "Numeral"}
POOL_POS = {"Noun", "Adjective", "Adverb", "Verb"}
BAD_SECONDARY = {"ProperNoun", "Abbreviation"}

# Argo filtresine eklenen belirsiz kayıtlardan yalnızca bunlar (blocklist_ambiguous.txt)
AMBIGUOUS_TAKEN = ["seks", "oral", "anal", "kerhane", "travesti", "gey", "fuhuş"]


def sort_key(w):
    return [_ORDER[c] for c in w]


def is_word(w, lo, hi):
    return lo <= len(w) <= hi and all(c in ALPHABET for c in w)


def read_list(path):
    out = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.split("#", 1)[0].strip()
            if line:
                out.append(turkish_lower(line))
    return out


def read_freq(path):
    """[(kelime, sayı)] sıklık sırasıyla (dosya sırası)."""
    out = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            p = line.split()
            if len(p) == 2 and p[1].isdigit():
                out.append((p[0], int(p[1])))
    return out


def read_lexicon(path):
    """Zemberek sözlüğü: [(lemma, root, pos, secpos, attrs-küme)]"""
    out = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            p = line.rstrip("\n").split("\t")
            if len(p) >= 4:
                out.append((p[0], p[1], p[2], p[3], set(p[4].split(",")) if len(p) > 4 and p[4] else set()))
    return out


def verb_roots_from_lexicon(lex):
    """Zemberek fiil kökleri (2-7 harf, informal olmayan). Ek: lemma -mak/-mek ile biter, root = kök."""
    roots = set()
    for lemma, root, pos, sec, attrs in lex:
        if pos == "Verb" and "Informal" not in attrs and sec not in BAD_SECONDARY and is_word(root, MIN_LEN, MAX_LEN):
            roots.add(root)
    return roots


def verb_roots_from_mastars(words):
    """TDK mastarlarından -mak/-mek atılarak fiil kökleri (sat, yat, gel, yaz ...)."""
    roots = set()
    for w in words:
        if (w.endswith("mak") or w.endswith("mek")) and is_word(w[:-3], MIN_LEN, MAX_LEN):
            roots.add(w[:-3])
    return roots


def blocked_set(block_words):
    """Argo filtresi: listedeki sözcükler + (mastar ise) kökleri. Kökler de çekimli/türemiş biçimleri
    yakalamak için lemma/root eşlemesinde kullanılır."""
    b = set(block_words)
    for w in list(b):
        if (w.endswith("mak") or w.endswith("mek")) and w[:-3] not in ROOT_ALLOW:
            b.add(w[:-3])
    return b


def load_blocklist():
    return read_list(BLOCKLIST)


def load_extra_blocked_forms():
    return read_list(BLOCKLIST_EXTRA) if os.path.exists(BLOCKLIST_EXTRA) else []


def ivariants(w):
    """w'deki i harflerinin bir kısmını ı yaparak elde edilen yazım varyantları (w hariç)."""
    idx = [k for k, c in enumerate(w) if c == "i"]
    for mask in itertools.product("iı", repeat=len(idx)):
        v = list(w)
        for k, c in zip(idx, mask):
            v[k] = c
        v = "".join(v)
        if v != w:
            yield v


def is_ivariant_oddity(w, accept, count, ratio=IVAR_RATIO):
    """True: w'nin ı'lı biçimi kabul setinde ve en az `ratio` kat yaygın (w yadırgatıcı yazılıştır)."""
    cw = count.get(w, 0)
    return any(v in accept and count.get(v, 0) >= ratio * max(cw, 1) for v in ivariants(w))


def parse_analysis(path):
    """{kelime: [(lemma, root, pos, sec, informal)]}; çözümlemesiz kelime için boş liste."""
    res = defaultdict(list)
    with open(path, encoding="utf-8") as f:
        for line in f:
            p = line.rstrip("\n").split("\t")
            if len(p) == 2:
                res[p[0]]
            elif len(p) >= 6:
                res[p[0]].append((p[1], p[2], p[3], p[4], p[5] == "1"))
    return res


def good_rows(rows, blocked, pos_set):
    out = []
    for lemma, root, pos, sec, inf in rows:
        if sec in BAD_SECONDARY or inf or pos not in pos_set:
            continue
        if lemma in blocked or root in blocked or turkish_lower(lemma) in blocked:
            continue
        out.append((lemma, root, pos, sec, inf))
    return out


def is_blocked_form(word, rows, blocked):
    """Sözcüğün kendisi listedeyse ya da TÜM çözümlemeleri engelli lemma/köke gidiyorsa engellidir
    ('amlar' -> am: engelli; 'ama' -> bağlaç 'ama' da var: engelli değil)."""
    if word in blocked:
        return True
    if not rows:
        return False
    return all(turkish_lower(l) in blocked or turkish_lower(r) in blocked for l, r, _, _, _ in rows)


def pool_ok(word, rows, blocked):
    """Izgara: çekimsiz (lemma == sözcük ya da fiil kökü == sözcük) içerik sözcüğü."""
    for lemma, root, pos, sec, inf in good_rows(rows, blocked, POOL_POS):
        if pos == "Verb":
            if root == word:
                return True
        elif turkish_lower(lemma) == word:
            return True
    return False


def prepare(cache):
    tdk = [w for w in read_list(TDK_WORDS) if is_word(w, MIN_LEN, MAX_LEN)]
    freq = read_freq(os.path.join(cache, "tr_full.txt"))
    lex = read_lexicon(os.path.join(cache, "lexicon.tsv"))
    cands = set(tdk)
    cands |= verb_roots_from_lexicon(lex)
    cands |= verb_roots_from_mastars(read_list(TDK_WORDS))
    for i, (w, c) in enumerate(freq):
        if c >= ACCEPT_MIN_COUNT and is_word(w, MIN_LEN, MAX_LEN):
            cands.add(w)
    with open(os.path.join(cache, "candidates.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(sorted(cands, key=sort_key)) + "\n")
    print("aday sayısı: %d" % len(cands))


def select(cache, accept_min=ACCEPT_MIN_COUNT, pool_min=None, log=print):
    pool_min = dict(POOL_MIN_COUNT if pool_min is None else pool_min)
    tdk_all = read_list(TDK_WORDS)
    tdk = {w for w in tdk_all if is_word(w, MIN_LEN, MAX_LEN)}
    freq = read_freq(os.path.join(cache, "tr_full.txt"))
    lex = read_lexicon(os.path.join(cache, "lexicon.tsv"))
    analysis = parse_analysis(os.path.join(cache, "analysis.tsv"))
    ambiguous = set(read_list(os.path.join(HERE, "blocklist_ambiguous.txt")))
    assert all(w in ambiguous for w in AMBIGUOUS_TAKEN)

    block_words = load_blocklist()
    blocked = blocked_set(block_words)
    extra = set(load_extra_blocked_forms())

    def blocked_word(w):
        return w in blocked or w in extra or is_blocked_form(w, analysis.get(w, []), blocked)

    accept = set()
    why = defaultdict(int)
    # 1) TDK madde başları
    for w in tdk:
        if blocked_word(w):
            why["tdk_blocked"] += 1
        else:
            accept.add(w)
    # 2) fiil kökleri: Zemberek + TDK mastarları
    roots = verb_roots_from_lexicon(lex) | verb_roots_from_mastars(tdk_all)
    for w in roots:
        if VOWELS.isdisjoint(w):
            continue
        if blocked_word(w):
            why["root_blocked"] += 1
        else:
            accept.add(w)
    # 3) sıklık listesinden çekimli/yaygın biçimler (Zemberek geçerli, özel isim/kısaltma/informal değil)
    n_freq = 0
    for w, c in freq:
        if c < accept_min:
            break
        if not is_word(w, MIN_LEN, MAX_LEN) or w in accept or VOWELS.isdisjoint(w):
            continue  # ünlüsüz diziler (kg, tv, dvd ...) kısaltmadır
        rows = analysis.get(w)
        if not rows:
            continue
        if blocked_word(w):
            why["freq_blocked"] += 1
            continue
        if good_rows(rows, blocked, ACCEPT_POS):
            accept.add(w)
            n_freq += 1
    accept -= set(read_list(ACCEPT_EXCLUDE)) if os.path.exists(ACCEPT_EXCLUDE) else set()
    # Elle eklenenler (ör. "ud"): engel listesi uygulandıktan sonra; ama engelli bir sözcük asla eklenmez.
    include = set(read_list(ACCEPT_INCLUDE)) if os.path.exists(ACCEPT_INCLUDE) else set()
    include = {w for w in include if is_word(w, MIN_LEN, MAX_LEN) and not blocked_word(w)}
    accept |= include
    log("kabul seti: %d (tdk %d, +kök/+sıklık...), sıklıktan eklenen: %d, engellenen: %s"
        % (len(accept), len(tdk), n_freq, dict(why)))

    pool_exclude = set(read_list(POOL_EXCLUDE)) if os.path.exists(POOL_EXCLUDE) else set()
    # Kalite kapısı: ızgara kelimesi TDK madde başı ya da TDK mastarının kökü olmalı
    # (Zemberek'in bildiği ama TDK'de olmayan yabancı yazımlar 'charter', 'sticker' gibi elenir).
    tdk_gate = tdk | verb_roots_from_mastars(tdk_all)
    count = {}
    for w, c in freq:
        count.setdefault(w, c)
    pool = []  # sıklık sırasıyla (en yaygın önce)
    n_ivar = 0
    floor_count = min(pool_min.values())
    for w, c in freq:
        if c < floor_count:
            break
        if not is_word(w, POOL_MIN_LEN, MAX_LEN) or c < pool_min[len(w)] or w not in accept or w in pool_exclude or w not in tdk_gate:
            continue
        rows = analysis.get(w)
        if rows and not blocked_word(w) and pool_ok(w, rows, blocked):
            if is_ivariant_oddity(w, accept, count):
                n_ivar += 1
                continue
            pool.append(w)
    log("ızgara havuzu: %d, i/ı varyantı nedeniyle elenen: %d" % (len(pool), n_ivar))
    return sorted(accept, key=sort_key), pool


def write_list(path, words):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(words) + "\n")


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("step", choices=["prepare", "select"])
    ap.add_argument("--cache", required=True)
    a = ap.parse_args(argv)
    if a.step == "prepare":
        prepare(a.cache)
        return 0
    accept, pool = select(a.cache)
    write_list(OUT_WORDS, accept)
    write_list(OUT_POOL, pool)
    print("words.txt: %d, grid_pool.txt: %d" % (len(accept), len(pool)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
