#!/usr/bin/env python3
"""Ses seti testleri. Calistirma:  python3 -m pytest tools/sounds/test_sounds.py -q
ya da  python3 tools/sounds/test_sounds.py  (analiz tablosunu basar + unittest)."""
import itertools
import re
import unittest
import wave
from pathlib import Path

import numpy as np

SOUNDS = Path(__file__).resolve().parents[2] / "app/src/main/assets/sounds"
EVENTS = ["letter", "delete", "clear", "correct", "bonus", "duplicate", "invalid",
          "shuffle", "hint", "coin", "bonus_full", "level_complete", "nav", "error"]
SYNTH_TONAL = ["letter", "correct", "bonus", "bonus_full", "level_complete", "invalid", "error", "duplicate", "coin", "hint"]
POSITIVE = ["correct", "bonus", "level_complete"]
NEGATIVE = ["invalid", "error", "duplicate"]
SR = 44100


def load(name):
    with wave.open(str(SOUNDS / f"{name}.wav"), "rb") as w:
        assert w.getsampwidth() == 2
        x = np.frombuffer(w.readframes(w.getnframes()), dtype="<i2").astype(np.float64) / 32768
        return x, w.getnchannels(), w.getframerate(), w.getsampwidth() * 8


def dbfs(v):
    return 20 * np.log10(max(v, 1e-9))


def peak_db(x):
    return dbfs(np.max(np.abs(x)))


def rms_db(x):
    return dbfs(np.sqrt(np.mean(x ** 2)))


def centroid(x):
    p = np.abs(np.fft.rfft(x * np.hanning(len(x)))) ** 2
    f = np.fft.rfftfreq(len(x), 1 / SR)
    return float(np.sum(f * p) / np.sum(p))


def f0_contour(x, fmin=150.0, fmax=2500.0, frame=1764, hop=441, min_periodicity=0.7):
    """Normalize otokorelasyon ile kare basina temel frekans. [(t, f0)] doner (sesli kareler)."""
    lo, hi = int(SR / fmax), int(SR / fmin)
    energies = [np.sum(x[i:i + frame] ** 2) for i in range(0, max(1, len(x) - frame), hop)]
    emax = max(energies) if energies else 0
    out = []
    for k, i in enumerate(range(0, max(1, len(x) - frame), hop)):
        seg = x[i:i + frame]
        if len(seg) < frame or energies[k] < emax * 10 ** (-30 / 10):
            continue
        seg = seg - seg.mean()
        n = len(seg)
        spec = np.fft.rfft(seg, 2 * n)
        ac = np.fft.irfft(spec * np.conj(spec))[:hi + 2]
        ac = ac / (ac[0] + 1e-12)
        cand = ac[lo:hi]
        best = cand.max()
        if best < min_periodicity:
            continue
        # oktav hatasini onlemek icin en kucuk lag'daki yerel tepe (>= %90 en iyiye)
        peaks = [j for j in range(1, len(cand) - 1)
                 if cand[j] >= cand[j - 1] and cand[j] >= cand[j + 1] and cand[j] >= 0.9 * best]
        lag = lo + peaks[0] if peaks else lo + int(np.argmax(cand))
        out.append(((i + frame / 2) / SR, SR / lag))
    return out


def f0_slope(x):
    """Yari ton / sn cinsinden dogrusal egim; sesli kare < 4 ise None."""
    c = f0_contour(x)
    if len(c) < 4:
        return None
    t = np.array([a for a, _ in c])
    st = 12 * np.log2(np.array([b for _, b in c]))
    return float(np.polyfit(t, st, 1)[0])


def f0_net_semitones(x):
    """Son ucte bir sesli karenin medyani eksi ilk ucte bir (yari ton)."""
    c = f0_contour(x)
    if len(c) < 4:
        return None
    st = 12 * np.log2(np.array([b for _, b in c]))
    k = max(1, len(st) // 3)
    return float(np.median(st[-k:]) - np.median(st[:k]))


def analyze(name):
    x, ch, sr, bits = load(name)
    return dict(name=name, dur=len(x) / sr, peak=peak_db(x), rms=rms_db(x), centroid=centroid(x),
                slope=f0_slope(x), net=f0_net_semitones(x))


class SoundSetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.A = {n: analyze(n) for n in EVENTS}

    def test_files_exist_and_format(self):
        for n in EVENTS:
            with self.subTest(n):
                self.assertTrue((SOUNDS / f"{n}.wav").is_file(), f"{n}.wav yok")
                x, ch, sr, bits = load(n)
                self.assertEqual((ch, sr, bits), (1, 44100, 16))

    def test_no_extra_wavs(self):
        self.assertEqual({p.stem for p in SOUNDS.glob("*.wav")}, set(EVENTS))

    def test_total_size(self):
        self.assertLess(sum(p.stat().st_size for p in SOUNDS.iterdir()), 3 * 1024 * 1024)

    def test_duration(self):
        for n in EVENTS:
            with self.subTest(n):
                self.assertTrue(0.05 <= self.A[n]["dur"] <= 2.5, self.A[n]["dur"])

    def test_peak(self):
        for n in EVENTS:
            with self.subTest(n):
                self.assertLessEqual(self.A[n]["peak"], -1.0)
                self.assertGreater(self.A[n]["peak"], -8.0, "cok kisik")

    def test_rms_consistency(self):
        r = [self.A[n]["rms"] for n in EVENTS]
        self.assertLessEqual(max(r) - min(r), 6.0, {n: round(self.A[n]["rms"], 1) for n in EVENTS})

    def test_no_dc_or_leading_silence(self):
        for n in EVENTS:
            with self.subTest(n):
                x = load(n)[0]
                self.assertLess(abs(x.mean()), 0.01)
                self.assertGreater(np.max(np.abs(x[:int(0.02 * SR)])) , 0.0)  # basta sessizlik kirpilmis
                self.assertLess(abs(x[0]), 0.05)   # tiklama olmasin
                self.assertLess(abs(x[-1]), 0.01)

    def test_letter_is_clean_tone_at_base_pitch(self):
        x = load("letter")[0]
        c = f0_contour(x, min_periodicity=0.9)
        self.assertGreaterEqual(len(c), 3)
        f = np.median([b for _, b in c])
        self.assertAlmostEqual(f, 523.25, delta=8)
        self.assertLess(abs(f0_slope(x) or 0), 6)  # perde sabit
        self.assertTrue(0.10 <= self.A["letter"]["dur"] <= 0.14)
        # 0.8 ve 1.6 hizinda (hizlandirilmis kopya) temel frekans 418-837 Hz araliginda kalir
        for rate in (0.8, 1.6):
            self.assertTrue(400 < f * rate < 850)
            self.assertGreater(self.A["letter"]["dur"] / rate, 0.05)

    def test_pitch_direction_positive_vs_negative(self):
        """Olumlu ses ailesi yukselen, olumsuz alcalan/duz perde konturu tasir."""
        for n in POSITIVE:
            with self.subTest(n):
                self.assertGreater(self.A[n]["net"], 3.0, self.A[n])
                self.assertGreater(self.A[n]["slope"], 5.0, self.A[n])
        for n in ("invalid", "error"):
            with self.subTest(n):
                self.assertLess(self.A[n]["net"], -4.0, self.A[n])
                self.assertLess(self.A[n]["slope"], -5.0, self.A[n])
        dup = self.A["duplicate"]
        self.assertLess(abs(dup["net"] or 0), 1.0, "duplicate notr (duz) olmali")
        pos_min = min(self.A[n]["slope"] for n in POSITIVE)
        neg_max = max(self.A[n]["slope"] for n in ("invalid", "error"))
        self.assertGreater(pos_min - neg_max, 10.0)

    def test_groups_separable(self):
        """Basit ayirici: skor = egim (yari ton/sn) normalize + log merkez frekans.
        Her olumlu ornek her olumsuzdan yuksek skor almali (dogrusal ayrilabilirlik)."""
        def score(n):
            a = self.A[n]
            slope = a["slope"] if a["slope"] is not None else 0.0
            return slope / 20.0 + np.log2(a["centroid"] / 1000.0)
        pos = [score(n) for n in POSITIVE]
        neg = [score(n) for n in NEGATIVE]
        self.assertGreater(min(pos), max(neg), (pos, neg))
        # olumlu grup ortalama olarak daha parlak
        self.assertGreater(np.mean([self.A[n]["centroid"] for n in POSITIVE]),
                           1.3 * np.mean([self.A[n]["centroid"] for n in NEGATIVE]))

    def test_every_pair_distinguishable(self):
        """Her olay cifti, (log merkez frekans, log sure, perde egimi) uzayinda yeterince uzak
        ya da dalga bicimi acisindan farkli olmali."""
        def vec(n):
            a = self.A[n]
            slope = (a["slope"] or 0.0) / 20.0
            return np.array([np.log2(a["centroid"]), np.log2(a["dur"]), slope])
        for p, q in itertools.combinations(EVENTS, 2):
            with self.subTest(f"{p}-{q}"):
                d = np.linalg.norm(vec(p) - vec(q))
                self.assertGreater(d, 0.45, f"{p} ve {q} cok benzer (d={d:.2f})")

    def test_envelope_signatures_differ(self):
        """Sure farki: sonuc/hata ailesi birbirinden en az %25 sure farkiyla ayrilir."""
        d = {n: self.A[n]["dur"] for n in EVENTS}
        self.assertGreater(d["level_complete"], 1.5)
        self.assertGreater(d["bonus_full"], 1.5 * d["bonus"] * 0.9)
        self.assertGreater(d["bonus"], 1.25 * d["correct"] * 0.9 )

    def test_sounds_md_has_source_and_license(self):
        md = (SOUNDS / "SOUNDS.md").read_text(encoding="utf-8")
        for n in EVENTS:
            with self.subTest(n):
                rows = [l for l in md.splitlines() if re.match(rf"\|\s*{n}\s*\|", l)]
                self.assertEqual(len(rows), 1, f"{n} icin tek satir olmali")
                cols = [c.strip() for c in rows[0].strip("|").split("|")]
                self.assertEqual(cols[1], f"`{n}.wav`")
                self.assertTrue(cols[2], "kaynak bos")
                self.assertTrue(re.search(r"CC0", cols[3]), "lisans CC0 olmali")
                self.assertTrue("build_sounds.py" in cols[2] or "kenney.nl" in cols[2], cols[2])


def print_table():
    print(f"{'olay':15} {'sure':>6} {'RMS':>7} {'tepe':>7} {'centroid':>9} {'f0 egim st/s':>13} {'net st':>7}")
    for n in EVENTS:
        a = analyze(n)
        s = "-" if a["slope"] is None else f"{a['slope']:+.1f}"
        nt = "-" if a["net"] is None else f"{a['net']:+.1f}"
        print(f"{n:15} {a['dur']:6.3f} {a['rms']:7.1f} {a['peak']:7.1f} {a['centroid']:9.0f} {s:>13} {nt:>7}")


if __name__ == "__main__":
    print_table()
    unittest.main(argv=["x"], verbosity=1)
