#!/usr/bin/env python3
"""Kelime Oyunu ses seti uretici.

Calistirma:  python3 tools/sounds/build_sounds.py [--cache DIR] [--out DIR]

* CC0 Kenney paketlerini (Interface/UI/Casino Audio) HTTPS ile indirir. Zip'ler gecici
  dizine gider, depoya girmez. Her paketin License.txt dosyasinda CC0 ifadesi aranir;
  bulunamazsa uretim durur (lisansi kanitlanamayan ses kullanilmaz).
* Secilen orneklerden mono / 44.1 kHz / 16-bit WAV uretir (sessizlik kirpma, fade,
  tepe -3 dBFS, hedef RMS).
* Ton gerektiren olaylar (harf, dogru, bonus, ...) numpy ile sentezlenir. Sabit seed,
  ayni girdiyle bire bir ayni cikti.
* app/src/main/assets/sounds/SOUNDS.md dosyasini (olay -> dosya -> kaynak -> lisans) yazar.

Gereksinimler: python3, numpy, ffmpeg (ogg cozmek icin). TLS dogrulamasi her zaman acik.
"""
import argparse
import io
import subprocess
import sys
import tempfile
import urllib.request
import wave
import zipfile
from pathlib import Path

import numpy as np

SR = 44100
SEED = 20261025
PEAK_DBFS = -3.0          # tepe tavani
TARGET_RMS_DBFS = -19.0   # tum dosyalar icin hedef RMS (kirpilmis dosya uzerinden)
MIN_DUR, MAX_DUR = 0.05, 2.5
LETTER_BASE_HZ = 523.25   # C5; playbackRate 0.8..1.6 -> ~418..837 Hz

REPO = Path(__file__).resolve().parents[2]
DEFAULT_OUT = REPO / "app/src/main/assets/sounds"

EVENTS = ["letter", "delete", "clear", "correct", "bonus", "duplicate", "invalid",
          "shuffle", "hint", "coin", "bonus_full", "level_complete", "nav", "error"]

KENNEY = {
    "interface-sounds": ("Kenney Interface Sounds 1.0", "https://kenney.nl/assets/interface-sounds",
                         "https://kenney.nl/media/pages/assets/interface-sounds/fa43c1dd4d-1677589452/kenney_interface-sounds.zip"),
    "ui-audio": ("Kenney UI Audio", "https://kenney.nl/assets/ui-audio",
                 "https://kenney.nl/media/pages/assets/ui-audio/490d233f68-1677590494/kenney_ui-audio.zip"),
    "casino-audio": ("Kenney Casino Audio 1.1", "https://kenney.nl/assets/casino-audio",
                     "https://kenney.nl/media/pages/assets/casino-audio/2472606a04-1721639069/kenney_casino-audio.zip"),
}

# olay -> (paket, zip ici dosya, azami sure sn, kirpma modu)
SAMPLES = {
    "delete":  ("interface-sounds", "Audio/drop_001.ogg", 0.30, "head"),
    "clear":   ("interface-sounds", "Audio/scratch_001.ogg", 0.30, "head"),
    "hint":    ("interface-sounds", "Audio/bong_001.ogg", 0.40, "head"),
    "nav":     ("ui-audio", "Audio/rollover1.ogg", 0.20, "head"),
    "coin":    ("casino-audio", "Audio/chips-stack-1.ogg", 0.35, "head"),
    "shuffle": ("casino-audio", "Audio/card-shuffle.ogg", 0.80, "loudest"),
}


# --------------------------------------------------------------------------- indirme
def fetch_pack(key, cache):
    """Zip'i indirir (TLS dogrulamali), License.txt'te CC0 arar, dosyalari bellege okur."""
    title, _page, url = KENNEY[key]
    zpath = cache / f"{key}.zip"
    if not zpath.exists():
        print(f"indiriliyor: {url}")
        with urllib.request.urlopen(url, timeout=120) as r:  # varsayilan, dogrulamali SSL
            zpath.write_bytes(r.read())
    zf = zipfile.ZipFile(zpath)
    lic = zf.read("License.txt").decode("utf-8", "replace")
    if "Creative Commons Zero" not in lic and "CC0" not in lic:
        sys.exit(f"{title}: License.txt CC0 icermiyor, kullanilamaz")
    return zf


def decode(data):
    """ogg/wav baytlarini ffmpeg ile mono, 44.1 kHz float32'ye cevirir."""
    p = subprocess.run(["ffmpeg", "-v", "error", "-i", "pipe:0", "-ac", "1", "-ar", str(SR),
                        "-f", "f32le", "pipe:1"], input=data, capture_output=True, check=True)
    return np.frombuffer(p.stdout, dtype="<f4").astype(np.float64)


# --------------------------------------------------------------------------- isleme
def trim(x, thresh_db=-42.0, pad_ms=3.0):
    env = np.abs(x)
    k = int(0.001 * SR)
    env = np.convolve(env, np.ones(k) / k, mode="same")
    idx = np.where(env > env.max() * 10 ** (thresh_db / 20))[0]
    if len(idx) == 0:
        return x
    pad = int(pad_ms * SR / 1000)
    return x[max(0, idx[0] - pad): idx[-1] + pad + 1]


def loudest_window(x, dur):
    n = int(dur * SR)
    if len(x) <= n:
        return x
    c = np.concatenate([[0], np.cumsum(x ** 2)])
    e = c[n:] - c[:-n]
    s = int(np.argmax(e))
    return x[s: s + n]


def fades(x, fade_in_ms=1.0, fade_out_ms=6.0):
    x = x.copy()
    fi = min(int(fade_in_ms * SR / 1000), len(x) // 4)
    fo = min(int(fade_out_ms * SR / 1000), len(x) // 2)
    if fi:
        x[:fi] *= np.linspace(0, 1, fi)
    if fo:
        x[-fo:] *= np.cos(np.linspace(0, np.pi / 2, fo)) ** 2
    return x


def rms_db(x):
    return 20 * np.log10(max(np.sqrt(np.mean(x ** 2)), 1e-9))


def level(x):
    """Yumusak sinirlayici ile tepe <= PEAK_DBFS, RMS ~ TARGET_RMS_DBFS (mumkun oldugunca)."""
    ceil = 10 ** (PEAK_DBFS / 20)
    x = x / np.max(np.abs(x))

    def shaped(g):
        return ceil * np.tanh(g * x / ceil)

    lo, hi = 0.01, 40.0
    for _ in range(60):  # bisection: RMS gain ile monoton artar
        mid = np.sqrt(lo * hi)
        if rms_db(shaped(mid)) < TARGET_RMS_DBFS:
            lo = mid
        else:
            hi = mid
    y = shaped(np.sqrt(lo * hi))
    # tanh tepeyi tam ceil'e dayamaz ama asla gecmez; yine de tepeyi -3 dBFS'e olcekle
    return y * (ceil / np.max(np.abs(y)))


def write_wav(path, x):
    pcm = np.clip(np.round(x * 32767), -32768, 32767).astype("<i2")
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


# --------------------------------------------------------------------------- sentez
def t_axis(dur):
    return np.arange(int(round(dur * SR))) / SR


def tone(freq, dur, harm=(1.0, 0.25, 0.08), attack=0.004, tau=0.06, glide_to=None, detune=0.0):
    """Zarflı ton. freq sabit ya da glide_to'ya ussel kayar. detune: ikinci katman (Hz)."""
    t = t_axis(dur)
    f = freq * (glide_to / freq) ** (t / dur) if glide_to else np.full_like(t, freq)
    ph = 2 * np.pi * np.cumsum(f) / SR
    y = np.zeros_like(t)
    for i, a in enumerate(harm, start=1):
        y += a * np.sin(i * ph)
        if detune:
            y += 0.5 * a * np.sin(i * (ph + 2 * np.pi * np.cumsum(np.full_like(t, detune)) / SR))
    env = np.minimum(1, t / attack) * np.exp(-t / tau)
    tail = int(0.008 * SR)
    env[-tail:] *= np.linspace(1, 0, tail)
    return y * env


def place(parts, total):
    """parts: [(baslangic_sn, dizi)] -> tek dizi."""
    out = np.zeros(int(round(total * SR)))
    for start, y in parts:
        s = int(round(start * SR))
        e = min(len(out), s + len(y))
        out[s:e] += y[: e - s]
    return out


def synth_all(rng):
    S = {}
    # harf: tek temiz kisa ton (120 ms); perde SoundPool hiziyla 0.8-1.6
    S["letter"] = tone(LETTER_BASE_HZ, 0.120, harm=(1, 0.22, 0.06), attack=0.004, tau=0.035)

    # dogru: yukselen ikili (E5 -> B5, saf beşli), yumusak
    S["correct"] = place([(0.0, tone(659.26, 0.14, tau=0.07)),
                          (0.11, tone(987.77, 0.22, tau=0.10))], 0.33)

    # bonus: parlak ucul arpej (C6 E6 G6), daha cok ust harmonik
    br = (1, 0.45, 0.25, 0.12)
    S["bonus"] = place([(0.00, tone(1046.5, 0.16, harm=br, tau=0.07)),
                        (0.085, tone(1318.5, 0.16, harm=br, tau=0.07)),
                        (0.17, tone(1568.0, 0.34, harm=br, tau=0.14))], 0.50)

    # bonus_full: daha uzun parlak arpej (G5 B5 D6 G6 B6) + uzun son nota
    notes = [783.99, 987.77, 1174.7, 1568.0, 1975.5]
    parts = [(0.075 * i, tone(f, 0.18, harm=br, tau=0.08)) for i, f in enumerate(notes[:-1])]
    parts.append((0.075 * 4, tone(notes[-1], 0.55, harm=br, tau=0.22, detune=2.5)))
    S["bonus_full"] = place(parts, 0.075 * 4 + 0.55)

    # seviye tamamlama: ~1.8 sn zafer arpeji, yukselen, uzun parlak son nota
    lh = (1, 0.40, 0.18, 0.08)
    seq = [(0.00, 523.25), (0.12, 659.26), (0.24, 783.99), (0.36, 1046.5),
           (0.54, 783.99 * 1.0), (0.66, 1046.5), (0.78, 1318.5)]
    parts = [(s, tone(f, 0.22, harm=lh, tau=0.09)) for s, f in seq[:4]]
    parts += [(0.52, tone(880.0, 0.18, harm=lh, tau=0.08)),
              (0.64, tone(1046.5, 0.18, harm=lh, tau=0.08)),
              (0.76, tone(1318.5, 0.18, harm=lh, tau=0.08)),
              (0.90, tone(1568.0, 0.90, harm=lh, tau=0.38, detune=2.0))]
    S["level_complete"] = place(parts, 1.85)

    # gecersiz: alcalan yumusak ikili (A4 -> E4), ucgen benzeri (az harmonik)
    sm = (1, 0.12, 0.03)
    S["invalid"] = place([(0.0, tone(440.0, 0.13, harm=sm, tau=0.07)),
                          (0.12, tone(293.66, 0.22, harm=sm, tau=0.11))], 0.32)

    # hata: dusuk, tek alcalan glissando + hafif vizilti (sistem hatasi)
    g = tone(300.0, 0.42, harm=(1, 0.5, 0.3, 0.2), attack=0.006, tau=0.20, glide_to=170.0)
    buzz = 0.05 * rng.standard_normal(len(g))
    buzz = np.convolve(buzz, np.ones(24) / 24, mode="same") * np.exp(-t_axis(0.42) / 0.2)
    S["error"] = g + buzz * np.max(np.abs(g))

    # tekrar: notr cift tik, sabit perde (700 Hz), ikisi de ayni
    tk = tone(700.0, 0.06, harm=(1, 0.1), attack=0.002, tau=0.014)
    S["duplicate"] = place([(0.0, tk), (0.085, tk)], 0.15)
    return S


def process_synth(x):
    x = trim(x, thresh_db=-50.0, pad_ms=1.0)
    return level(fades(x, 0.5, 4.0))


def process_sample(x, maxdur, mode):
    x = x - np.mean(x)
    x = trim(x)
    if len(x) / SR > maxdur:
        x = loudest_window(x, maxdur) if mode == "loudest" else x[: int(maxdur * SR)]
        fo = 80.0 if mode == "loudest" else 25.0
        x = fades(x, 3.0, fo)
    else:
        x = fades(x, 1.0, 8.0)
    return level(x)


# --------------------------------------------------------------------------- SOUNDS.md
def sounds_md(info):
    L = ["# Ses efektleri: kaynak ve lisans", "",
         "Bu dosya `tools/sounds/build_sounds.py` tarafindan uretilir; elle degistirmeyin.",
         "Tum dosyalar: mono, 44.1 kHz, 16-bit WAV; tepe -3 dBFS; hedef RMS -19 dBFS.",
         "Hicbir dosya atif gerektirmez. Lisansi kanitlanamayan ses kullanilmamistir.", "",
         "| Olay | Dosya | Kaynak | Lisans |", "|---|---|---|---|"]
    for ev in EVENTS:
        L.append(f"| {ev} | `{ev}.wav` | {info[ev][0]} | {info[ev][1]} |")
    L += ["", "## Kaynak paketler (CC0)", ""]
    for key, (title, page, url) in KENNEY.items():
        L.append(f"- {title}: {page} (zip: {url}); zip icindeki `License.txt` "
                 "\"Creative Commons Zero, CC0\" ifadesini icerir ve build sirasinda denetlenir.")
    L += ["", "## Kullanilmayan adaylar", "",
          "- OpenGameArt \"Hi-Tech Button Sound Pack (non-themed)\" "
          "(https://opengameart.org/content/hi-tech-button-sound-pack-i-non-themed): sayfada CC0 yazar, "
          "ancak zip icinde lisans dosyasi yoktur; kanitlanamadigi icin KULLANILMADI.",
          "- OpenGameArt UI Sound Library / ui-sound-effects-library: CC-BY 3.0, KULLANILMADI.",
          "- Freesound: erisilemiyor, KULLANILMADI.", ""]
    return "\n".join(L)


# --------------------------------------------------------------------------- ana
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cache", help="zip'lerin indirilecegi dizin (varsayilan: gecici dizin)")
    ap.add_argument("--out", default=str(DEFAULT_OUT))
    a = ap.parse_args()
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    cache = Path(a.cache) if a.cache else Path(tempfile.mkdtemp(prefix="kelime_sounds_"))
    cache.mkdir(parents=True, exist_ok=True)
    if REPO in cache.resolve().parents or cache.resolve() == REPO:
        sys.exit("cache dizini depo icinde olamaz")

    rng = np.random.default_rng(SEED)
    info = {}
    packs = {}
    for ev, (pack, member, maxdur, mode) in SAMPLES.items():
        if pack not in packs:
            packs[pack] = fetch_pack(pack, cache)
        raw = decode(packs[pack].read(member))
        write_wav(out / f"{ev}.wav", process_sample(raw, maxdur, mode))
        title, page, _ = KENNEY[pack]
        base = member.split("/")[-1]
        extra = f", en yuksek enerjili {maxdur:g} sn'lik kesit" if mode == "loudest" else ""
        info[ev] = (f"{title}, `{base}` ({page}){extra}", "CC0")
    for ev, x in synth_all(rng).items():
        write_wav(out / f"{ev}.wav", process_synth(x))
        info[ev] = ("prosedürel sentez, build_sounds.py", "CC0 (kendi üretimimiz)")
    (out / "SOUNDS.md").write_text(sounds_md(info), encoding="utf-8")
    total = sum(p.stat().st_size for p in out.glob("*.wav"))
    print(f"{len(EVENTS)} dosya yazildi -> {out} ({total/1024:.0f} KB)")


if __name__ == "__main__":
    main()
