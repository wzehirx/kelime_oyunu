# Ses efektleri: kaynak ve lisans

Bu dosya `tools/sounds/build_sounds.py` tarafından üretilir; elle değiştirmeyin.
Tüm sesler **tamamen prosedürel sentezle** (yalnızca numpy) üretilmiştir: harici örnek, indirme ya da
üçüncü taraf kaynak yoktur. Lisans: kendi üretimimiz, **CC0**. Atıf gerekmez.
Tüm dosyalar: mono, 44.1 kHz, 16-bit WAV; tepe -3 dBFS; hedef RMS yaklaşık -18 dBFS.
Aile: marimba / kalimba / ahşap vuruş + hafif çan (glockenspiel) tınısı; modal sentez, yumuşak
alçak geçiren filtre, sabit seed'li mikro sapma, hafif yankı. Olumlu sesler yükselen, olumsuzlar
alçalan perde konturu taşır. Harf sesi kelimedeki sıraya göredir (1. harf en kalın, 7. en ince;
A majör pentatonik A3 B3 C#4 E4 F#4 A4 B4). Dosyalar kulakla doğrulanmadı, nicel analizle denetlendi.
Dünya turu: `place_*` 7 ses (6 bölge giriş melodisi + ülke tamamlama), ekran okuyucu konuşmasından önce
çalar; 1,2-2,0 sn (tamamlama ~2 sn). Bölgeler ritim, dizi ve tınıyla ayrışır; ud için Karplus-Strong,
gong için inharmonik modal sentez kullanılır. Toplam 27 dosya.

| Olay | Dosya | Kaynak | Lisans |
|---|---|---|---|
| letter_1 | `letter_1.wav` | prosedürel sentez, build_sounds.py (marimba A3 (220.00 Hz), kelimede 1. harf) | CC0 (kendi üretimimiz) |
| letter_2 | `letter_2.wav` | prosedürel sentez, build_sounds.py (marimba B3 (246.94 Hz), kelimede 2. harf) | CC0 (kendi üretimimiz) |
| letter_3 | `letter_3.wav` | prosedürel sentez, build_sounds.py (marimba C#4 (277.18 Hz), kelimede 3. harf) | CC0 (kendi üretimimiz) |
| letter_4 | `letter_4.wav` | prosedürel sentez, build_sounds.py (marimba E4 (329.63 Hz), kelimede 4. harf) | CC0 (kendi üretimimiz) |
| letter_5 | `letter_5.wav` | prosedürel sentez, build_sounds.py (marimba F#4 (369.99 Hz), kelimede 5. harf) | CC0 (kendi üretimimiz) |
| letter_6 | `letter_6.wav` | prosedürel sentez, build_sounds.py (marimba A4 (440.00 Hz), kelimede 6. harf) | CC0 (kendi üretimimiz) |
| letter_7 | `letter_7.wav` | prosedürel sentez, build_sounds.py (marimba B4 (493.88 Hz), kelimede 7. harf) | CC0 (kendi üretimimiz) |
| delete | `delete.wav` | prosedürel sentez, build_sounds.py (marimba/kalimba, kısa aşağı süpürmeli pluck) | CC0 (kendi üretimimiz) |
| clear | `clear.wav` | prosedürel sentez, build_sounds.py (filtreli gürültü, aşağı süpüren swish) | CC0 (kendi üretimimiz) |
| correct | `correct.wav` | prosedürel sentez, build_sounds.py (marimba, yükselen 3 notalık arpej (A4-C#5-E5)) | CC0 (kendi üretimimiz) |
| bonus | `bonus.wav` | prosedürel sentez, build_sounds.py (glockenspiel kıvılcımı (C#6-E6-A6)) | CC0 (kendi üretimimiz) |
| duplicate | `duplicate.wav` | prosedürel sentez, build_sounds.py (iki yumuşak ahşap vuruş, aynı perde) | CC0 (kendi üretimimiz) |
| invalid | `invalid.wav` | prosedürel sentez, build_sounds.py (marimba, alçalan iki nota (E4-A3)) | CC0 (kendi üretimimiz) |
| shuffle | `shuffle.wav` | prosedürel sentez, build_sounds.py (kısa filtreli gürültü darbeleri (kart hışırtısı)) | CC0 (kendi üretimimiz) |
| hint | `hint.wav` | prosedürel sentez, build_sounds.py (çan, yukarı doğru parıltılı dizi (E5-A5-C#6-E6)) | CC0 (kendi üretimimiz) |
| coin | `coin.wav` | prosedürel sentez, build_sounds.py (çan 'ting' (B5-E6)) | CC0 (kendi üretimimiz) |
| bonus_full | `bonus_full.wav` | prosedürel sentez, build_sounds.py (glockenspiel + çan, uzun parıltılı yükseliş) | CC0 (kendi üretimimiz) |
| level_complete | `level_complete.wav` | prosedürel sentez, build_sounds.py (marimba arpej + çan akoru (A majör), yankılı) | CC0 (kendi üretimimiz) |
| nav | `nav.wav` | prosedürel sentez, build_sounds.py (çok yumuşak kısa ahşap tık) | CC0 (kendi üretimimiz) |
| error | `error.wav` | prosedürel sentez, build_sounds.py (alçak yumuşak 'tok' (aşağı eğimli)) | CC0 (kendi üretimimiz) |
| place_avrupa | `place_avrupa.wav` | prosedürel sentez, build_sounds.py (Avrupa: D majör 3/4 vals, marimba bas-akor-akor + hafif çan melodisi) | CC0 (kendi üretimimiz) |
| place_asya | `place_asya.wav` | prosedürel sentez, build_sounds.py (Asya: pelog benzeri pentatonik (E F A B C), gong + seyrek kalimba/marimba melodisi) | CC0 (kendi üretimimiz) |
| place_ortadogu | `place_ortadogu.wav` | prosedürel sentez, build_sounds.py (Orta Doğu: hicaz dizisi (D Eb F# G A Bb C D), Karplus-Strong ud pluck taksimi) | CC0 (kendi üretimimiz) |
| place_afrika | `place_afrika.wav` | prosedürel sentez, build_sounds.py (Afrika: mbira/kalimba, G pentatonik, 3'e 2 çapraz ritim) | CC0 (kendi üretimimiz) |
| place_amerika | `place_amerika.wav` | prosedürel sentez, build_sounds.py (Amerika: C majör, son klave sinkoplu Latin marimba motifi + ahşap klave) | CC0 (kendi üretimimiz) |
| place_okyanusya | `place_okyanusya.wav` | prosedürel sentez, build_sounds.py (Okyanusya: D pentatonik, dalga konturlu parıltılı vibrafon/çan arpı) | CC0 (kendi üretimimiz) |
| place_complete | `place_complete.wav` | prosedürel sentez, build_sounds.py (Ülke tamamlama: pasaport damgası 'tok' vuruş + yükselen 4 notalık A majör motif ve çan) | CC0 (kendi üretimimiz) |

Yeniden üretim: `python3 tools/sounds/build_sounds.py` (sabit seed, aynı numpy sürümüyle bire bir aynı çıktı).
