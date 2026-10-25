"""Ham sozcuk satirlarini oyun sozlugune donusturur.

Kurallar (sirayla):
  1. Bos satirlar ve bastaki/sondaki bosluklar atilir.
  2. Ozel isim sezgisi: kaynakta ilk harfi buyuk, geri kalani kucuk olan
     girisler (ornegin "Ankara", "Izmir") ozel isim sayilir ve atilir; ancak
     ayni sozcugun kucuk harfli bicimi girdide de varsa ("Agac" + "agac")
     kucuk harfli bicim zaten alinir. TUMU BUYUK yazilmis girisler ("ISIK")
     ozel isim sayilmaz; Turkce kurallarla kucultulur.
  3. Turkce kucuk harfe cevirme: I->i degil I->ı, İ->i (Locale("tr") gibi).
  4. Sapkali ünlüler duzlestirilir: â->a, î->i, û->u (oyunda oyuncu "dukkan"
     yazar; TDK "dükkân" yazar).
  5. Yalnizca ALPHABET harflerinden olusan, uzunlugu 2-15 olan, en az bir
     ünlü içeren tek sozcukler kalir. Bosluk, tire, rakam, kesme isareti vb.
     iceren her sey atilir. Ünlüsüz iki harfli girisler (cm, mg, kr, br...)
     birim/simge kisaltmalaridir ve atilir.
  6. Benzersiz ve Turkce alfabe sirasina gore siralanmis liste dondurulur.
"""
from typing import Iterable

ALPHABET = "abcçdefgğhıijklmnoöprsştuüvyz"
VOWELS = frozenset("aeıioöuü")
MIN_LEN = 2
MAX_LEN = 15

_ALPHABET_SET = frozenset(ALPHABET)
_ORDER = {ch: i for i, ch in enumerate(ALPHABET)}
_FOLD = str.maketrans({"â": "a", "î": "i", "û": "u"})


def turkish_lower(s: str) -> str:
    """Locale("tr") kucuk harf: I->ı, İ->i, digerleri str.lower()."""
    return s.replace("I", "ı").replace("İ", "i").lower()


def _sort_key(word: str):
    return [_ORDER[c] for c in word]


def _is_proper_noun_shape(raw: str) -> bool:
    """Ilk harf buyuk ve sozcuk TUMU BUYUK degil (ornegin "Ankara")."""
    if raw[0] == turkish_lower(raw[0]):
        return False
    return len(raw) == 1 or turkish_lower(raw[1:]) == raw[1:]


def _normalize(raw: str) -> str:
    return turkish_lower(raw).translate(_FOLD)


def _valid(word: str) -> bool:
    return (
        MIN_LEN <= len(word) <= MAX_LEN
        and _ALPHABET_SET.issuperset(word)
        and not VOWELS.isdisjoint(word)
    )


def clean(lines: Iterable[str]) -> list[str]:
    stripped = [s for s in (line.strip() for line in lines) if s]
    # Kucuk harfle baslayan (ya da TUMU BUYUK) girislerin normalize bicimleri.
    plain = {
        _normalize(s)
        for s in stripped
        if not _is_proper_noun_shape(s)
    }
    result = set()
    for s in stripped:
        word = _normalize(s)
        if _is_proper_noun_shape(s) and word not in plain:
            continue
        if _valid(word):
            result.add(word)
    return sorted(result, key=_sort_key)
