#!/usr/bin/env python3
"""Ses seti testleri (27 prosedurel WAV: 20 oyun sesi + 7 dunya turu place_*). Calistirma:  python3 tools/sounds/test_sounds.py
(analiz tablosunu basar + unittest)."""
import itertools
import re
import subprocess
import sys
import tempfile
import unittest
import wave
from pathlib import Path

import numpy as np

SOUNDS = Path(__file__).resolve().parents[2] / "app/src/main/assets/sounds"
LETTERS = [f"letter_{i}" for i in range(1, 8)]
LETTER_HZ = [220.00, 246.94, 277.18, 329.63, 369.99, 440.00, 493.88]   # A3 B3 C#4 E4 F#4 A4 B4
EVENTS = LETTERS + ["delete", "clear", "correct", "bonus", "duplicate", "invalid",
                    "shuffle", "hint", "coin", "bonus_full", "level_complete", "nav", "error"]
PLACES = ["place_avrupa", "place_asya", "place_ortadogu", "place_afrika", "place_amerika",
          "place_okyanusya", "place_complete"]
REGIONS = PLACES[:6]                                # 6 bolge giris melodisi (place_complete haric)
ALL = EVENTS + PLACES
POSITIVE = ["correct", "bonus", "bonus_full", "hint", "coin", "level_complete"]
NEGATIVE_DOWN = ["invalid", "error", "delete"]     # net alcalan kontur
NOISE_LIKE = ["clear", "shuffle"]                  # perdesiz (gurultu) sesler
TONAL = [n for n in ALL if n not in NOISE_LIKE]
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


def spectrum(x, nfft=1 << 17):
    p = np.abs(np.fft.rfft(x * np.hanning(len(x)), nfft)) ** 2
    return np.fft.rfftfreq(nfft, 1 / SR), p


def dominant_hz(x, lo=100.0, hi=3500.0, nfft=1 << 17):
    """En guclu spektral tepe (parabolik ara degerleme)."""
    f, p = spectrum(x, nfft)
    m = (f >= lo) & (f <= hi)
    k = int(np.argmax(np.where(m, p, 0)))
    a, b, c = np.log(p[k - 1] + 1e-30), np.log(p[k] + 1e-30), np.log(p[k + 1] + 1e-30)
    d = 0.5 * (a - c) / (a - 2 * b + c)
    return float((k + d) * SR / nfft)


def autocorr_hz(x, fmin=150.0, fmax=700.0):
    """Normalize otokorelasyon temel frekansi (parabolik ara degerleme)."""
    x = x - x.mean()
    n = len(x)
    ac = np.fft.irfft(np.abs(np.fft.rfft(x, 2 * n)) ** 2)[:n]
    ac = ac / ac[0]
    lo, hi = int(SR / fmax), int(SR / fmin)
    lag = lo + int(np.argmax(ac[lo:hi]))
    a, b, c = ac[lag - 1], ac[lag], ac[lag + 1]
    return SR / (lag + 0.5 * (a - c) / (a - 2 * b + c))


def partial_count(x, floor_db=-35.0, lo=100.0, hi=16000.0, min_sep=0.06):
    """Ana tepeden en fazla |floor_db| dB asagida, birbirinden >= %6 uzak spektral tepe sayisi."""
    f, p = spectrum(x)
    db = 10 * np.log10(p / p.max() + 1e-30)
    idx = [k for k in range(1, len(f) - 1) if db[k] >= floor_db and db[k] >= db[k - 1] and db[k] > db[k + 1] and lo <= f[k] <= hi]
    idx.sort(key=lambda k: -db[k])
    kept = []
    for k in idx:
        if all(abs(f[k] - f[j]) / f[j] > min_sep for j in kept):
            kept.append(k)
    return len(kept)


def nonfundamental_ratio(x, f0=None):
    """Temel (+-%7) disindaki enerji orani. Duz sinus ~0."""
    f, p = spectrum(x)
    f0 = f0 or dominant_hz(x)
    band = (f > f0 * 0.93) & (f < f0 * 1.07)
    return float(1.0 - p[band].sum() / p.sum())


def attack_ms(x):
    """Ilk notanin atagi: ilk 50 ms icindeki tepenin %90'ina ulasma suresi (1 ms kayan maksimum zarfi)."""
    w = int(0.001 * SR)
    seg = np.abs(x[:int(0.05 * SR)])
    env = np.array([seg[max(0, i - w):i + 1].max() for i in range(len(seg))])
    return float(np.argmax(env >= 0.9 * env.max()) / SR * 1000)


def pitch_contour(x, frame=2048, hop=441, lo=100.0, hi=3500.0, gate_db=-30.0):
    """[(t, f0)] kare basina en guclu spektral tepe; yalnizca yeterince guclu kareler."""
    starts = range(0, max(1, len(x) - frame), hop)
    en = [np.sum(x[i:i + frame] ** 2) for i in starts]
    emax = max(en)
    out = []
    for i, e in zip(starts, en):
        if e < emax * 10 ** (gate_db / 10) or len(x[i:i + frame]) < frame:
            continue
        out.append(((i + frame / 2) / SR, dominant_hz(x[i:i + frame], lo, hi, 1 << 15)))
    return out


def f0_slope(x):
    c = pitch_contour(x)
    if len(c) < 4:
        return None
    t = np.array([a for a, _ in c])
    st = 12 * np.log2(np.array([b for _, b in c]))
    return float(np.polyfit(t, st, 1)[0])


def f0_net_semitones(x):
    """Son ucte bir sesli karenin medyani eksi ilk 3 sesli karenin medyani (yari ton)."""
    c = pitch_contour(x)
    if len(c) < 4:
        return None
    st = 12 * np.log2(np.array([b for _, b in c]))
    k = max(1, len(st) // 3)
    return float(np.median(st[-k:]) - np.median(st[:3]))


def centroid_track(x, parts=3):
    """Zaman uclu bolumlerde spektral centroid (Hz)."""
    k = len(x) // parts
    return [centroid(x[i * k:(i + 1) * k]) for i in range(parts)]


def onset_times(x, win=1024, hop=256, min_gap=0.05, rel=0.12):
    """Spektral akis (log-genlik spektrum artislari) tepe secimiyle vurus (onset) zamanlari, sn.
    Cok sesli / yankili sinyalde de calisir; min_gap'ten yakin tepeler tek sayilir."""
    w = np.hanning(win)
    mag = np.array([np.abs(np.fft.rfft(x[i:i + win] * w)) for i in range(0, len(x) - win, hop)])
    flux = np.maximum(np.diff(np.log1p(200.0 * mag), axis=0), 0).sum(axis=1)
    flux = flux / flux.max()
    gap = int(min_gap * SR / hop)
    out = []
    for i in range(1, len(flux) - 1):
        if flux[i] >= rel and flux[i] >= flux[i - 1] and flux[i] > flux[i + 1] and (not out or i - out[-1] >= gap):
            out.append(i)
    return np.array(out) * hop / SR


def rhythm_features(x):
    """(onset sayisi, onset/sn, IOI degisim katsayisi). Duzenli vals/arp -> dusuk, sinkoplu -> yuksek."""
    o = onset_times(x)
    ioi = np.diff(o)
    cv = float(ioi.std() / ioi.mean()) if len(ioi) > 1 else 0.0
    return len(o), len(o) / (len(x) / SR), cv


def region_vec(a):
    """Bolge imza uzayi: (log2 centroid, log2 onset/sn, 2*IOI degisim katsayisi)."""
    return np.array([np.log2(a["centroid"]), np.log2(a["rate"]), 2.0 * a["ioicv"]])


def analyze(name):
    x, ch, sr, bits = load(name)
    head = x[:int(0.12 * SR)]   # ilk 120 ms: vurus + ust kismilar (tinin karakteri)
    d = dict(name=name, dur=len(x) / sr, peak=peak_db(x), rms=rms_db(x), centroid=centroid(x),
             attack=attack_ms(x), partials=partial_count(head), f0=dominant_hz(x, 100.0, 3500.0),
             nonfund=nonfundamental_ratio(head))
    d["onsets"], d["rate"], d["ioicv"] = rhythm_features(x)
    if name in NOISE_LIKE or name in PLACES:   # place_*: cok perdeli melodi, tek f0 konturu anlamsiz
        d.update(slope=None, net=None, ctrack=centroid_track(x))
    else:
        d.update(slope=f0_slope(x), net=f0_net_semitones(x))
    return d


class SoundSetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.A = {n: analyze(n) for n in ALL}

    def test_all_files_exist_and_format(self):
        self.assertEqual(len(EVENTS), 20)
        self.assertEqual(len(PLACES), 7)
        self.assertEqual(len(ALL), 27)
        self.assertEqual(len(set(ALL)), 27)
        for n in ALL:
            with self.subTest(n):
                self.assertTrue((SOUNDS / f"{n}.wav").is_file(), f"{n}.wav yok")
                x, ch, sr, bits = load(n)
                self.assertEqual((ch, sr, bits), (1, 44100, 16))

    def test_no_extra_wavs(self):
        self.assertEqual({p.stem for p in SOUNDS.glob("*.wav")}, set(ALL))   # tam 27 dosya, fazladan yok
        self.assertFalse((SOUNDS / "letter.wav").exists())

    def test_total_size(self):
        self.assertLess(sum(p.stat().st_size for p in SOUNDS.iterdir()), 3 * 1024 * 1024)

    def test_duration(self):
        for n in ALL:
            with self.subTest(n):
                self.assertTrue(0.05 <= self.A[n]["dur"] <= 2.5, self.A[n]["dur"])
        for n in LETTERS:
            self.assertTrue(0.25 <= self.A[n]["dur"] <= 0.45, (n, self.A[n]["dur"]))
        for n in PLACES:
            self.assertTrue(1.0 <= self.A[n]["dur"] <= 2.5, (n, self.A[n]["dur"]))
        for n in REGIONS:   # konusmadan once calan giris melodisi: kisa ve yorucu degil
            self.assertTrue(1.2 <= self.A[n]["dur"] <= 2.0, (n, self.A[n]["dur"]))
        self.assertTrue(1.8 <= self.A["place_complete"]["dur"] <= 2.3, self.A["place_complete"]["dur"])

    def test_peak(self):
        for n in ALL:
            with self.subTest(n):
                self.assertLessEqual(self.A[n]["peak"], -1.0)
                self.assertGreater(self.A[n]["peak"], -8.0, "cok kisik")

    def test_rms_consistency(self):
        r = [self.A[n]["rms"] for n in ALL]
        self.assertLessEqual(max(r) - min(r), 6.0, {n: round(self.A[n]["rms"], 1) for n in ALL})

    def test_no_dc_or_clicks(self):
        for n in ALL:
            with self.subTest(n):
                x = load(n)[0]
                self.assertLess(abs(x.mean()), 0.01)
                self.assertLess(abs(x[0]), 0.05)
                self.assertLess(abs(x[-1]), 0.01)
                tail = x[-int(0.005 * SR):]      # son 5 ms: tik yok (sessize yumusak iner)
                self.assertLess(np.max(np.abs(tail)), 0.01, "son 5 ms'de tik")
                self.assertLess(np.max(np.abs(np.diff(tail))), 0.005, "son 5 ms'de ani sicrama")
                self.assertGreater(np.max(np.abs(x)), 0.1)

    def test_letters_fundamentals_match_notes_and_strictly_rise(self):
        fs = []
        for n, target in zip(LETTERS, LETTER_HZ):
            with self.subTest(n):
                x = load(n)[0]
                f_fft = dominant_hz(x, 150.0, 700.0)
                self.assertAlmostEqual(f_fft / target, 1.0, delta=0.02, msg=f"{n}: FFT {f_fft:.1f} Hz != {target}")
                f_ac = autocorr_hz(x[:int(0.15 * SR)])
                self.assertAlmostEqual(f_ac / target, 1.0, delta=0.02, msg=f"{n}: otokorelasyon {f_ac:.1f} Hz != {target}")
                fs.append(f_fft)
        self.assertTrue(all(b > a for a, b in zip(fs, fs[1:])), fs)

    def test_no_clipping(self):
        for n in ALL:
            with self.subTest(n):
                with wave.open(str(SOUNDS / f"{n}.wav"), "rb") as w:
                    pcm = np.frombuffer(w.readframes(w.getnframes()), dtype="<i2").astype(int)
                self.assertLess(int(np.max(np.abs(pcm))), 32767 * 10 ** (-1.0 / 20), "tam olcek / kirpma")
                self.assertEqual(int(np.sum(np.abs(pcm) >= 32767)), 0)

    def test_spectral_richness_not_plain_sine(self):
        # metrik duz sinusu reddetmeli (ozdenetim)
        t = np.arange(int(0.3 * SR)) / SR
        sine = np.sin(2 * np.pi * 330 * t) * np.exp(-t / 0.1)
        self.assertLess(nonfundamental_ratio(sine), 0.01)
        self.assertLessEqual(partial_count(sine), 1)
        for n in TONAL:
            with self.subTest(n):
                a = self.A[n]
                self.assertGreaterEqual(a["partials"], 3 if a["dur"] > 0.15 else 2, a)  # nav: 90 ms'lik tik, en az 2 kismi
                self.assertGreater(a["nonfund"], 0.05, a)

    def test_attack_is_short(self):
        for n in TONAL:
            with self.subTest(n):
                self.assertLess(self.A[n]["attack"], 15.0, self.A[n]["attack"])

    def test_pitch_direction_positive_vs_negative(self):
        """Olumlu sesler yukselen, olumsuzlar alcalan, duplicate duz perde konturu tasir."""
        for n in POSITIVE:
            with self.subTest(n):
                self.assertGreater(self.A[n]["net"], 3.0, self.A[n])
        for n in ("invalid", "error", "delete"):
            with self.subTest(n):
                self.assertLess(self.A[n]["net"], -2.5, self.A[n])
        self.assertLess(abs(self.A["duplicate"]["net"] or 0), 1.0, "duplicate notr (duz) olmali")
        self.assertLess(self.A["invalid"]["net"], -4.0)
        self.assertGreater(min(self.A[n]["net"] for n in POSITIVE) - max(self.A[n]["net"] for n in NEGATIVE_DOWN), 6.0)

    def test_noise_sounds_direction(self):
        c = self.A["clear"]["ctrack"]
        self.assertGreater(c[0], 1.5 * c[-1], c)          # clear: asagi suprulen
        s = self.A["shuffle"]
        self.assertGreater(s["centroid"], 1500, s)         # shuffle: hisirti, tonsuz-parlak

    def test_positive_brighter_than_negative(self):
        pos = np.mean([self.A[n]["centroid"] for n in POSITIVE])
        neg = np.mean([self.A[n]["centroid"] for n in ("invalid", "error", "duplicate")])
        self.assertGreater(pos, 1.3 * neg)

    def test_regions_pairwise_distinguishable(self):
        """6 bolge sesi (place_complete haric) ikili olarak ayirt edilebilir: (log2 centroid, log2 onset/sn,
        2*IOI degisim katsayisi) uzayinda en az 0,45 uzakta (olculen en yakin cift ~0,56; en uzak > 2)."""
        for p, q in itertools.combinations(REGIONS, 2):
            with self.subTest(f"{p}-{q}"):
                d = float(np.linalg.norm(region_vec(self.A[p]) - region_vec(self.A[q])))
                self.assertGreater(d, 0.45, f"{p} ve {q} cok benzer (d={d:.2f})")
        # ritim imzasi: tek basina onset yogunlugu da bolgeleri ayirmali (en az 3 farkli yogunluk sinifi)
        self.assertGreaterEqual(len({round(self.A[n]["rate"] / 2.0) for n in REGIONS}), 3)
        # bolge sesleri tamamlama sesinden de farkli
        for n in REGIONS:
            with self.subTest(n):
                d = float(np.linalg.norm(region_vec(self.A[n]) - region_vec(self.A["place_complete"])))
                self.assertGreater(d, 0.3, f"{n} place_complete'e cok benzer (d={d:.2f})")

    def test_region_rhythm_character(self):
        r = {n: self.A[n] for n in REGIONS}
        self.assertEqual(max(REGIONS, key=lambda n: r[n]["rate"]), "place_okyanusya")      # sik arp
        self.assertEqual(max(REGIONS, key=lambda n: r[n]["centroid"]), "place_okyanusya")  # en parlak
        self.assertLess(r["place_asya"]["rate"], r["place_afrika"]["rate"])                # seyrek gong < polirtim
        self.assertGreater(r["place_ortadogu"]["partials"], r["place_afrika"]["partials"])  # ud: zengin kismi
        self.assertGreaterEqual(self.A["place_afrika"]["onsets"], 10)                      # polirtim: yogun vuruslar

    def test_every_pair_distinguishable(self):
        """Olay ciftleri (log centroid, log sure, log temel frekans) uzayinda ayrik olmali.
        Harf sesleri kendi aralarinda yalnizca perdeyle ayrilir (ayri test)."""
        def vec(n):
            a = self.A[n]
            return np.array([np.log2(a["centroid"]), np.log2(a["dur"]), np.log2(a["f0"])])
        names = ["letter_4"] + [n for n in EVENTS if n not in LETTERS]
        for p, q in itertools.combinations(names, 2):
            with self.subTest(f"{p}-{q}"):
                d = np.linalg.norm(vec(p) - vec(q))
                self.assertGreater(d, 0.45, f"{p} ve {q} cok benzer (d={d:.2f})")

    def test_envelope_signatures_differ(self):
        d = {n: self.A[n]["dur"] for n in EVENTS}
        self.assertTrue(1.6 <= d["level_complete"] <= 2.5, d["level_complete"])
        self.assertGreater(d["bonus_full"], d["bonus"])
        self.assertGreater(d["level_complete"], d["bonus_full"])
        self.assertLess(d["error"], d["invalid"])

    def test_reproducible_bit_for_bit(self):
        """Ayni seed'le iki ayri calistirma ayni baytlari uretir; depodaki dosyalar da ayni (+-2 LSB)."""
        script = Path(__file__).with_name("build_sounds.py")
        with tempfile.TemporaryDirectory() as a, tempfile.TemporaryDirectory() as b:
            for d in (a, b):
                subprocess.run([sys.executable, str(script), "--out", d], check=True, capture_output=True)
            for n in ALL:
                with self.subTest(n):
                    self.assertEqual((Path(a) / f"{n}.wav").read_bytes(), (Path(b) / f"{n}.wav").read_bytes())
                    ref = np.frombuffer(wave.open(str(Path(a) / f"{n}.wav")).readframes(10 ** 7), dtype="<i2").astype(int)
                    cur = np.frombuffer(wave.open(str(SOUNDS / f"{n}.wav")).readframes(10 ** 7), dtype="<i2").astype(int)
                    self.assertEqual(len(ref), len(cur))
                    self.assertLessEqual(int(np.max(np.abs(ref - cur))), 2)

    def test_build_script_is_offline_numpy_only(self):
        src = Path(__file__).with_name("build_sounds.py").read_text(encoding="utf-8")
        for bad in ("urllib", "requests", "socket", "subprocess", "ffmpeg", "http://", "https://"):
            self.assertNotIn(bad, src)

    def test_sounds_md_has_a_row_per_file(self):
        md = (SOUNDS / "SOUNDS.md").read_text(encoding="utf-8")
        self.assertNotIn("Kenney", md)
        self.assertEqual(len([l for l in md.splitlines() if re.match(r"\|\s*[a-z_0-9]+\s*\|\s*`", l)]), 27)
        for n in ALL:
            with self.subTest(n):
                rows = [l for l in md.splitlines() if re.match(rf"\|\s*{n}\s*\|", l)]
                self.assertEqual(len(rows), 1, f"{n} icin tek satir olmali")
                cols = [c.strip() for c in rows[0].strip("|").split("|")]
                self.assertEqual(cols[1], f"`{n}.wav`")
                self.assertIn("build_sounds.py", cols[2])
                self.assertTrue(re.search(r"CC0", cols[3]), "lisans CC0 olmali")


def print_table():
    print(f"{'olay':15} {'sure':>5} {'RMS':>6} {'tepe':>6} {'centroid':>8} {'f0 Hz':>7} {'atak ms':>7} {'kismi':>5} {'nonfund':>7} {'net st':>7} {'egim':>6} {'onset':>5} {'on/sn':>5} {'ioicv':>5}")
    for n in ALL:
        a = analyze(n)
        s = "-" if a["slope"] is None else f"{a['slope']:+.1f}"
        nt = "-" if a["net"] is None else f"{a['net']:+.1f}"
        print(f"{n:15} {a['dur']:5.2f} {a['rms']:6.1f} {a['peak']:6.1f} {a['centroid']:8.0f} {a['f0']:7.1f} "
              f"{a['attack']:7.1f} {a['partials']:5d} {a['nonfund']:7.2f} {nt:>7} {s:>6} {a['onsets']:5d} {a['rate']:5.1f} {a['ioicv']:5.2f}")


if __name__ == "__main__":
    print_table()
    unittest.main(argv=["x"], verbosity=1)
