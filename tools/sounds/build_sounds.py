#!/usr/bin/env python3
"""Kelime Oyunu ses seti uretici (tamamen prosedurel sentez, yalnizca numpy).

Calistirma:  python3 tools/sounds/build_sounds.py [--out DIR]

* Ag erisimi, indirme, harici ornek YOK. Tum sesler burada sentezlenir; lisans: kendi
  uretimimiz, CC0.
* Ses ailesi: marimba / kalimba / ahsap vurus + hafif cam-can tinisi (glockenspiel).
  - Modal sentez: temel + inharmonik ustkismi, her kismi kendi ustel sonumuyle.
  - 1-3 ms yumusak atak + cok kisa filtreli gurultu "vurus" gecisi.
  - Yumusak alcak geciren filtre (sifir fazli), sabit seed'li mikro rastgelelik
    (perde, genlik, zamanlama sapmasi, hafif vurma/beating) ile "makine" hissi azaltilir.
  - Hafif yanki: ustel sonumlu gurultu darbe cevabi ile FFT evrisimi (%12-20 islak).
* Sabit seed: ayni numpy surumuyle bire bir ayni cikti.
* app/src/main/assets/sounds/SOUNDS.md dosyasini yazar.

Cikti: mono, 44.1 kHz, 16-bit WAV; sure 0.05-2.5 sn; tepe -3 dBFS; olaylar arasi RMS farki kucuk.
"""
import argparse
import wave
from pathlib import Path

import numpy as np

SR = 44100
SEED = 20261025
PEAK_DBFS = -3.0
TARGET_RMS_DBFS = -18.0
MIN_DUR, MAX_DUR = 0.05, 2.5

REPO = Path(__file__).resolve().parents[2]
DEFAULT_OUT = REPO / "app/src/main/assets/sounds"

EVENTS = ["letter_1", "letter_2", "letter_3", "letter_4", "letter_5", "letter_6", "letter_7",
          "delete", "clear", "correct", "bonus", "duplicate", "invalid", "shuffle", "hint",
          "coin", "bonus_full", "level_complete", "nav", "error"]

# A majör pentatonik, yukselen: A3 B3 C#4 E4 F#4 A4 B4
LETTER_NOTES_HZ = [220.00, 246.94, 277.18, 329.63, 369.99, 440.00, 493.88]

NOTE_INDEX = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5, "F#": 6, "G": 7, "G#": 8, "A": 9, "A#": 10, "B": 11}


def hz(name):
    """'C#4' -> Hz (A4 = 440, esit tamperaman)."""
    pitch, octave = name[:-1], int(name[-1])
    midi = 12 * (octave + 1) + NOTE_INDEX[pitch]
    return 440.0 * 2 ** ((midi - 69) / 12)


# ---------------------------------------------------------------- yardimcilar

def rms(x):
    return float(np.sqrt(np.mean(x ** 2))) if len(x) else 0.0


def fft_filter(x, shape):
    """Sifir fazli frekans cevabi uygula: shape(f) -> kazanc."""
    n = len(x)
    nfft = 1 << int(np.ceil(np.log2(n + 1)))
    spec = np.fft.rfft(x, nfft)
    f = np.fft.rfftfreq(nfft, 1 / SR)
    return np.fft.irfft(spec * shape(f), nfft)[:n]


def lowpass(x, fc, order=4):
    return fft_filter(x, lambda f: 1.0 / np.sqrt(1.0 + (f / fc) ** (2 * order)))


def bandpass(x, lo, hi, order=2):
    return fft_filter(x, lambda f: (1.0 / np.sqrt(1.0 + (f / hi) ** (2 * order)))
                      * (1.0 / np.sqrt(1.0 + (lo / np.maximum(f, 1e-3)) ** (2 * order))))


def ramp(n, dur_s):
    k = max(2, int(dur_s * SR))
    r = np.ones(n)
    k = min(k, n)
    r[:k] = 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, k))
    return r


def place(buf, sig, t0):
    i = int(round(t0 * SR))
    if i < 0:
        sig = sig[-i:]
        i = 0
    end = min(len(buf), i + len(sig))
    if end > i:
        buf[i:end] += sig[:end - i]


# ---------------------------------------------------------------- enstrumanlar

MARIMBA = dict(ratios=(1.0, 3.93, 9.14), amps=(1.0, 0.85, 0.38), taus=(1.0, 0.50, 0.22))
KALIMBA = dict(ratios=(1.0, 5.4), amps=(1.0, 0.22), taus=(1.0, 0.14))
WOOD = dict(ratios=(1.0, 2.6, 5.1), amps=(1.0, 0.9, 0.5), taus=(1.0, 0.6, 0.35))
GLOCK = dict(ratios=(1.0, 2.76, 5.40, 8.93), amps=(1.0, 0.34, 0.18, 0.07), taus=(1.0, 0.40, 0.22, 0.12))
BELL = dict(ratios=(1.0, 2.0, 2.76, 4.07, 5.40), amps=(1.0, 0.38, 0.42, 0.18, 0.16), taus=(1.0, 0.65, 0.45, 0.28, 0.2))


def modal_note(rng, f, dur, inst, tau, vel=1.0, glide_st=0.0, glide_tau=0.08,
               strike=0.10, strike_fc=4000.0, strike_tau=0.004, attack=None, beat=0.12):
    """Modal sentez: her kismi kendi ustel sonumuyle. `tau` temel kismin sonum sabiti (sn);
    inst['taus'] bunun carpanlari. Mikro sapma: kismi perde ve genlik, atak suresi, vurma."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    atk = attack if attack is not None else rng.uniform(0.0015, 0.0030)
    env_atk = ramp(n, atk)
    glide = glide_st * (1.0 - np.exp(-t / glide_tau)) if glide_st else np.zeros(n)
    bend = 2.0 ** (glide / 12.0)
    for r, a, m in zip(inst["ratios"], inst["amps"], inst["taus"]):
        fr = f * r * (1.0 + rng.normal(0, 0.0007))
        if fr > 0.45 * SR:
            continue
        a = a * (1.0 + rng.normal(0, 0.05))
        phase = 2 * np.pi * np.cumsum(fr * bend) / SR + rng.uniform(0, 2 * np.pi)
        out += a * np.sin(phase) * np.exp(-t / (tau * m))
        if beat and r == 1.0:   # hafif vurma: cok yakin ikinci mod
            ph2 = 2 * np.pi * np.cumsum(fr * bend * (1.0 + rng.uniform(0.002, 0.005))) / SR + rng.uniform(0, 2 * np.pi)
            out += a * beat * np.sin(ph2) * np.exp(-t / (tau * m * 0.8))
    out *= env_atk
    if strike:
        k = int(0.03 * SR)
        nz = lowpass(rng.randn(k), strike_fc, 2) * np.exp(-np.arange(k) / SR / strike_tau)
        out[:k] += strike * nz * ramp(k, 0.0005)
    return out * vel


def swept_noise(rng, dur, f_start, f_end, width_oct, tau, attack):
    """STFT ile zamanla kayan bant gecirgen gurultu (hisirti / suprme)."""
    n = int(dur * SR)
    N, H = 1024, 256
    win = np.hanning(N)
    noise = rng.randn(n + N)
    out = np.zeros(n + N)
    f = np.fft.rfftfreq(N, 1 / SR)
    for s in range(0, n, H):
        tc = (s + N / 2) / SR / dur
        fc = f_start * (f_end / f_start) ** min(1.0, tc)
        mask = np.exp(-0.5 * (np.log2(np.maximum(f, 1.0) / fc) / width_oct) ** 2)
        out[s:s + N] += np.fft.irfft(np.fft.rfft(noise[s:s + N] * win) * mask, N)
    out = out[:n]
    t = np.arange(n) / SR
    return out * np.exp(-t / tau) * ramp(n, attack)


def reverb(x, rng, rt, mix, lp=5000.0, predelay=0.008):
    """Ustel sonumlu gurultu darbe cevabi (60 dB / rt) ile FFT evrisimi; islak orani `mix`."""
    L = int(rt * 1.1 * SR)
    t = np.arange(L) / SR
    ir = rng.randn(L) * np.exp(-6.9 * t / rt)
    ir = lowpass(ir, lp, 2)
    ir = np.concatenate([np.zeros(int(predelay * SR)), ir])
    dry = np.concatenate([x, np.zeros(len(ir))])
    nfft = 1 << int(np.ceil(np.log2(len(dry) + len(ir))))
    wet = np.fft.irfft(np.fft.rfft(dry, nfft) * np.fft.rfft(ir, nfft), nfft)[:len(dry)]
    wet *= np.sqrt(np.sum(dry ** 2) / (np.sum(wet ** 2) + 1e-12))
    return (1.0 - mix) * dry + mix * wet


# ---------------------------------------------------------------- olay tasarimlari
# Her fonksiyon (rng) -> (ham sinyal, ayar sozlugu: fc, rev=(rt, mix) veya None, max_dur, trim_db)

def _buf(dur):
    return np.zeros(int(dur * SR))


def design_letter(i):
    def f(rng):
        fr = LETTER_NOTES_HZ[i]
        tau = 0.105 * (220.0 / fr) ** 0.25
        x = modal_note(rng, fr, 0.34, MARIMBA, tau, strike=0.14)
        return x, dict(fc=7500, rev=(0.30, 0.13), max_dur=0.42, trim=0.0)
    return f


def design_delete(rng):
    x = modal_note(rng, hz("E4"), 0.26, KALIMBA, 0.09, glide_st=-7.0, glide_tau=0.06, strike=0.10)
    return x, dict(fc=5000, rev=(0.28, 0.12), max_dur=0.30, trim=-0.5)


def design_clear(rng):
    x = swept_noise(rng, 0.40, 5200.0, 700.0, 0.75, 0.13, 0.025)
    return x, dict(fc=7000, rev=(0.30, 0.12), max_dur=0.42, trim=0.0, soft=1.8)


def design_correct(rng):
    buf = _buf(0.62)
    for k, (nm, t0) in enumerate((("A4", 0.0), ("C#5", 0.085), ("E5", 0.17))):
        tau = 0.10 if k < 2 else 0.17
        n = modal_note(rng, hz(nm), 0.40, MARIMBA, tau, vel=0.85 + 0.1 * k, strike=0.11)
        place(buf, n, t0 + rng.normal(0, 0.002))
    return buf, dict(fc=9000, rev=(0.35, 0.15), max_dur=0.62, trim=0.0)


def design_bonus(rng):
    buf = _buf(0.80)
    for k, (nm, t0) in enumerate((("C#6", 0.0), ("E6", 0.075), ("A6", 0.15))):
        n = modal_note(rng, hz(nm), 0.62, GLOCK, 0.17 + 0.05 * k, vel=0.8 + 0.1 * k, strike=0.05, strike_fc=7000, beat=0.0)
        place(buf, n, t0 + rng.normal(0, 0.002))
    return buf, dict(fc=11000, rev=(0.40, 0.17), max_dur=0.80, trim=-0.5)


def design_duplicate(rng):
    buf = _buf(0.42)
    for k, t0 in enumerate((0.0, 0.13)):
        n = modal_note(rng, 640.0, 0.22, WOOD, 0.035, vel=1.0 - 0.2 * k, strike=0.28, strike_fc=3500, beat=0.0)
        place(buf, n, t0 + rng.normal(0, 0.002))
    return buf, dict(fc=4500, rev=(0.28, 0.12), max_dur=0.40, trim=-1.0, soft=1.6)


def design_invalid(rng):
    buf = _buf(0.66)
    for k, (nm, t0) in enumerate((("E4", 0.0), ("A3", 0.17))):
        n = modal_note(rng, hz(nm), 0.46, MARIMBA, 0.13, vel=0.95 - 0.1 * k, strike=0.07)
        place(buf, n, t0 + rng.normal(0, 0.002))
    return buf, dict(fc=3200, rev=(0.30, 0.13), max_dur=0.66, trim=0.0)


def design_shuffle(rng):
    buf = _buf(0.46)
    t0 = 0.0
    for k in range(8):
        d = int(0.07 * SR)
        lo, hi = rng.uniform(1100, 1700), rng.uniform(4200, 6500)
        b = bandpass(rng.randn(d), lo, hi) * np.exp(-np.arange(d) / SR / 0.02) * ramp(d, 0.003)
        place(buf, b * rng.uniform(0.55, 1.0), t0)
        t0 += rng.uniform(0.03, 0.05)
    return buf, dict(fc=8000, rev=(0.30, 0.12), max_dur=0.46, trim=-0.5, soft=2.2)


def design_hint(rng):
    buf = _buf(1.00)
    for k, (nm, t0) in enumerate((("E5", 0.0), ("A5", 0.10), ("C#6", 0.20), ("E6", 0.30))):
        n = modal_note(rng, hz(nm), 0.70, BELL, 0.30 + 0.04 * k, vel=0.75 + 0.08 * k, strike=0.04, strike_fc=7000, beat=0.0)
        place(buf, n, t0 + rng.normal(0, 0.002))
    return buf, dict(fc=10000, rev=(0.45, 0.18), max_dur=1.05, trim=-0.5)


def design_coin(rng):
    buf = _buf(0.60)
    for nm, t0, vel, tau in (("B5", 0.0, 0.75, 0.05), ("E6", 0.065, 1.0, 0.18)):
        n = modal_note(rng, hz(nm), 0.50, BELL, tau, vel=vel, strike=0.05, strike_fc=8000, beat=0.0)
        place(buf, n, t0 + rng.normal(0, 0.001))
    return buf, dict(fc=11000, rev=(0.30, 0.13), max_dur=0.55, trim=-0.5)


def design_bonus_full(rng):
    buf = _buf(1.60)
    seq = ("A5", "B5", "C#6", "E6", "F#6", "A6")
    for k, nm in enumerate(seq):
        n = modal_note(rng, hz(nm), 0.9, GLOCK, 0.22 + 0.05 * k, vel=0.65 + 0.07 * k, strike=0.05, strike_fc=8000, beat=0.0)
        place(buf, n, k * 0.085 + rng.normal(0, 0.002))
    for nm, t0 in (("A5", 0.55), ("E6", 0.55), ("C#7", 0.57)):  # parilti akoru
        n = modal_note(rng, hz(nm), 1.0, BELL, 0.40, vel=0.55, strike=0.03, strike_fc=8000, beat=0.0)
        place(buf, n, t0 + rng.normal(0, 0.002))
    return buf, dict(fc=11000, rev=(0.50, 0.20), max_dur=1.45, trim=-0.5)


def design_level_complete(rng):
    buf = _buf(2.30)
    for k, nm in enumerate(("A3", "E4", "A4", "C#5", "E5")):
        n = modal_note(rng, hz(nm), 0.5, MARIMBA, 0.13, vel=0.8 + 0.04 * k, strike=0.09)
        place(buf, n, k * 0.115 + rng.normal(0, 0.002))
    t0 = 0.66
    for nm, v in (("A3", 0.7), ("A4", 0.7)):
        place(buf, modal_note(rng, hz(nm), 1.4, MARIMBA, 0.35, vel=v, strike=0.06), t0)
    for nm in ("C#5", "E5", "A5"):
        place(buf, modal_note(rng, hz(nm), 1.4, BELL, 0.55, vel=0.55, strike=0.03, strike_fc=7000, beat=0.0), t0 + rng.normal(0, 0.003))
    return buf, dict(fc=9000, rev=(0.50, 0.20), max_dur=2.10, trim=0.0)


def design_nav(rng):
    x = modal_note(rng, 1100.0, 0.12, WOOD, 0.016, strike=0.30, strike_fc=3500, beat=0.0)
    return x, dict(fc=3600, rev=None, max_dur=0.12, trim=-1.0, soft=2.0)


def design_error(rng):
    x = modal_note(rng, 190.0, 0.30, dict(ratios=(1.0, 2.3), amps=(1.0, 0.25), taus=(1.0, 0.4)), 0.075,
                   glide_st=-6.5, glide_tau=0.05, strike=0.22, strike_fc=900, strike_tau=0.006, beat=0.0)
    return x, dict(fc=2500, rev=(0.25, 0.12), max_dur=0.30, trim=-1.0)


DESIGNS = {f"letter_{i + 1}": design_letter(i) for i in range(7)}
DESIGNS.update(delete=design_delete, clear=design_clear, correct=design_correct, bonus=design_bonus,
               duplicate=design_duplicate, invalid=design_invalid, shuffle=design_shuffle, hint=design_hint,
               coin=design_coin, bonus_full=design_bonus_full, level_complete=design_level_complete,
               nav=design_nav, error=design_error)

DESCRIPTIONS = {
    "delete": "marimba/kalimba, kısa aşağı süpürmeli pluck",
    "clear": "filtreli gürültü, aşağı süpüren swish",
    "correct": "marimba, yükselen 3 notalık arpej (A4-C#5-E5)",
    "bonus": "glockenspiel kıvılcımı (C#6-E6-A6)",
    "duplicate": "iki yumuşak ahşap vuruş, aynı perde",
    "invalid": "marimba, alçalan iki nota (E4-A3)",
    "shuffle": "kısa filtreli gürültü darbeleri (kart hışırtısı)",
    "hint": "çan, yukarı doğru parıltılı dizi (E5-A5-C#6-E6)",
    "coin": "çan 'ting' (B5-E6)",
    "bonus_full": "glockenspiel + çan, uzun parıltılı yükseliş",
    "level_complete": "marimba arpej + çan akoru (A majör), yankılı",
    "nav": "çok yumuşak kısa ahşap tık",
    "error": "alçak yumuşak 'tok' (aşağı eğimli)",
}
for _i, _n in enumerate(("A3", "B3", "C#4", "E4", "F#4", "A4", "B4")):
    DESCRIPTIONS[f"letter_{_i + 1}"] = f"marimba {_n} ({LETTER_NOTES_HZ[_i]:.2f} Hz), kelimede {_i + 1}. harf"


# ---------------------------------------------------------------- uretim

def render(name, idx):
    rng = np.random.RandomState(SEED + 101 * idx)
    x, cfg = DESIGNS[name](rng)
    x = x - x.mean()
    x = lowpass(x, cfg["fc"], 4)
    if cfg["rev"]:
        x = reverb(x, rng, cfg["rev"][0], cfg["rev"][1])
    # sondaki sessizligi kirp, sureyi sinirla, yumusak cikis
    thr = np.max(np.abs(x)) * 10 ** (-50 / 20)
    nz = np.nonzero(np.abs(x) > thr)[0]
    n = min(int(nz[-1]) + 1 if len(nz) else len(x), int(cfg["max_dur"] * SR), int(MAX_DUR * SR) - 2)
    x = x[:n]
    fade = min(int(0.05 * SR), n // 3)
    x[-fade:] *= 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, fade))
    x = x - x.mean()
    x[0] = 0.0
    # seviye: hedef RMS, tepe tavani
    if cfg.get("soft"):   # yumusak sinirlayici: tepeli (vurmali / gurultulu) seslerde RMS'i yukseltir
        k = cfg["soft"]
        x = np.tanh(k * x / np.max(np.abs(x))) / np.tanh(k)
        x = x - x.mean()
    g_rms = 10 ** ((TARGET_RMS_DBFS + cfg["trim"]) / 20) / (rms(x) + 1e-12)
    g_peak = 10 ** (PEAK_DBFS / 20) / (np.max(np.abs(x)) + 1e-12)
    x = x * min(g_rms, g_peak)
    return np.clip(np.round(x * 32767), -32768, 32767).astype("<i2")


def write_wav(path, pcm):
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def write_sounds_md(out):
    lines = [
        "# Ses efektleri: kaynak ve lisans",
        "",
        "Bu dosya `tools/sounds/build_sounds.py` tarafından üretilir; elle değiştirmeyin.",
        "Tüm sesler **tamamen prosedürel sentezle** (yalnızca numpy) üretilmiştir: harici örnek, indirme ya da",
        "üçüncü taraf kaynak yoktur. Lisans: kendi üretimimiz, **CC0**. Atıf gerekmez.",
        "Tüm dosyalar: mono, 44.1 kHz, 16-bit WAV; tepe -3 dBFS; hedef RMS yaklaşık -18 dBFS.",
        "Aile: marimba / kalimba / ahşap vuruş + hafif çan (glockenspiel) tınısı; modal sentez, yumuşak",
        "alçak geçiren filtre, sabit seed'li mikro sapma, hafif yankı. Olumlu sesler yükselen, olumsuzlar",
        "alçalan perde konturu taşır. Harf sesi kelimedeki sıraya göredir (1. harf en kalın, 7. en ince;",
        "A majör pentatonik A3 B3 C#4 E4 F#4 A4 B4). Dosyalar kulakla doğrulanmadı, nicel analizle denetlendi.",
        "",
        "| Olay | Dosya | Kaynak | Lisans |",
        "|---|---|---|---|",
    ]
    for n in EVENTS:
        lines.append(f"| {n} | `{n}.wav` | prosedürel sentez, build_sounds.py ({DESCRIPTIONS[n]}) | CC0 (kendi üretimimiz) |")
    lines += ["", "Yeniden üretim: `python3 tools/sounds/build_sounds.py` (sabit seed, aynı numpy sürümüyle bire bir aynı çıktı).", ""]
    (out / "SOUNDS.md").write_text("\n".join(lines), encoding="utf-8")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = ap.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    for old in args.out.glob("*.wav"):
        old.unlink()
    for idx, n in enumerate(EVENTS):
        write_wav(args.out / f"{n}.wav", render(n, idx))
    write_sounds_md(args.out)
    print(f"{len(EVENTS)} dosya -> {args.out}")


if __name__ == "__main__":
    main()
