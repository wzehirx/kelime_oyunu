> **Tarihsel not:** Bu araştırma v1.0 ses setinin (Kenney örnekleri) kaynağıydı. Güncel ses seti tamamen prosedürel sentezdir (`build_sounds.py`); Kenney/OGA dosyaları artık kullanılmıyor.

# Ses efekti kaynak araştırması (Kelime Oyunu)

Amaç: Görme engelli kullanıcılar için birincil geri bildirim kanalı olarak kısa, net ses efektleri. Yalnızca **CC0** (veya sayfada açıkça CC0 yazan) kaynaklar kullanıldı.

Yöntem ve sınırlar:
- İndirme komutları `curl -sSL` ile çalıştırıldı (TLS doğrulaması açık). Freesound 403 verdiği için kullanılmadı.
- Lisans kanıtı: Kenney paketlerinin zip içindeki `License.txt` dosyası ve sayfalardaki "License(s)" alanı. OGA sayfalarında lisans yalnızca sayfa üst verisinde yer alıyor; SFX_0 zip'inde lisans dosyası yok.
- Süreler ve biçimler `ffprobe` ile ölçüldü. Dosyalar **dinlenmedi**; olay eşlemeleri dosya adlarına ve sürelere dayanıyor. Son karar için bir kez kulakla doğrulama gerekir.
- Dosya sayıları ölçümdür. Kenney sayfalarındaki "N assets" ifadeleri bazen farklı (ör. Digital Audio sayfası 60, zip'te 62 ses dosyası).

## 1. Kaynak tablosu

| # | Paket | Sayfa | İndirme | Lisans kanıtı | Biçim | Dosya sayısı | Boyut |
|---|---|---|---|---|---|---|---|
| K1 | Kenney Interface Sounds 1.0 | https://kenney.nl/assets/interface-sounds | `curl -sSL -o kenney_interface-sounds.zip "https://kenney.nl/media/pages/assets/interface-sounds/fa43c1dd4d-1677589452/kenney_interface-sounds.zip"` | Sayfa: "Creative Commons CC0". Zip içi License.txt: "License: (Creative Commons Zero, CC0)" | OGG Vorbis, mono, 44.1 kHz | 100 | 835 KB |
| K2 | Kenney UI Audio | https://kenney.nl/assets/ui-audio | `curl -sSL -o kenney_ui-audio.zip "https://kenney.nl/media/pages/assets/ui-audio/490d233f68-1677590494/kenney_ui-audio.zip"` | Sayfa CC0. License.txt: "License (Creative Commons Zero, CC0)" | OGG Vorbis, stereo, 44.1 kHz | 51 (+Preview.ogg) | 412 KB |
| K3 | Kenney Digital Audio | https://kenney.nl/assets/digital-audio | `curl -sSL -o kenney_digital-audio.zip "https://kenney.nl/media/pages/assets/digital-audio/216eac4753-1677590265/kenney_digital-audio.zip"` | Sayfa CC0. License.txt: CC0 | OGG Vorbis, mono, 44.1 kHz | 62 (+Preview.ogg) | 990 KB |
| K4 | Kenney Casino Audio 1.1 | https://kenney.nl/assets/casino-audio | `curl -sSL -o kenney_casino-audio.zip "https://kenney.nl/media/pages/assets/casino-audio/2472606a04-1721639069/kenney_casino-audio.zip"` | Sayfa CC0. License.txt: CC0 | OGG Vorbis, stereo, 44.1 kHz | 54 (+Preview.ogg) | 877 KB |
| O1 | OGA "Hi-Tech Button Sound Pack (non-themed)" (Circlerun) | https://opengameart.org/content/hi-tech-button-sound-pack-i-non-themed | `curl -sSL -o SFX_0.zip "https://opengameart.org/sites/default/files/SFX_0.zip"`, sonra `unar -o <boş_dizin> SFX.rar` (zip içinde RAR v4) | Sayfa "License(s): CC0". Zip içinde lisans dosyası yok | WAV (16-bit PCM, 44.1 kHz) ve 2 MP3 | 15 | 271 KB (zip) |
| O2 | OGA "Pop sounds" (EZduzziteh) | https://opengameart.org/content/pop-sounds-0 | `curl -sSL -o pop1.wav "https://opengameart.org/sites/default/files/pop1.wav"` (pop1–pop9) | Sayfa "License(s): CC0" | WAV (PCM, 44.1 kHz) | 9 | ~10–60 KB/dosya |
| O3 | OGA "Chiptune SFX pack" (NesVST) | https://opengameart.org/content/chiptune-sfx-pack | `curl -sSL -o coin_1.wav "https://opengameart.org/sites/default/files/coin_1.wav"` (sayfadaki 12 dosya) | Sayfa "License(s): CC0" | WAV (PCM) | 12 | 8-bit tarzı, kısa |
| O4 | OGA "UI click sounds sci-fi" (EZduzziteh) | https://opengameart.org/content/ui-click-sounds-sci-fi | `curl -sSL -o sfx_ui_click_1.mp3 "https://opengameart.org/sites/default/files/sfx_ui_click_1.mp3"` (1–8) | Sayfa "License(s): CC0" | MP3, 128 kbps, 48 kHz | 8 | ~5 KB/dosya |

Kenney paketleri için tek satırlık toplu indirme: sayfa bağlantılarındaki `.zip` yolu `curl -sSL -O` ile alınır; `unzip` ile açılır (zip içinde `Audio/` klasörü var).

## 2. Olay → önerilen dosya eşlemesi

Süreler ölçülmüştür. Hepsi TalkBack konuşmasının kendisiyle çakışmaması için 0.6 sn altında tutuldu (karıştırma istisnası aşağıda).

| Olay | Birincil öneri | Süre | Alternatif | Not |
|---|---|---|---|---|
| Harf seçme (kısa tık, perde değişebilir) | K1 `Audio/pluck_001.ogg` | 0.10 sn | K1 `pluck_002.ogg` (0.16 sn); K1 `click_001.ogg` (0.10 sn); O2 `pop1.wav` (0.06 sn) | Tonlu olduğu için perde kaydırmaya uygun. Perdeyi playbackRate ile değiştir. |
| Harf silme | K1 `Audio/drop_001.ogg` | 0.11 sn | K1 `back_001.ogg` (0.06 sn) | Geri alma/kaldırma çağrışımı için yeterli. |
| Temizleme (tüm harfler) | K1 `Audio/scratch_001.ogg` | 0.14 sn | K1 `minimize_001.ogg` (0.26 sn) | "Süpürme" hissi. Silmeden ayırt etmek için farklı dosya. |
| Doğru kelime | K1 `Audio/confirmation_001.ogg` | 0.29 sn | K3 `Audio/powerUp2.ogg` (0.47 sn) | Olumlu, kısa onay. |
| Bonus kelime | K3 `Audio/powerUp4.ogg` | 0.55 sn | K3 `powerUp2.ogg` (0.47 sn); O3 `coin_2.wav` (0.35 sn) | Doğru kelimeden daha belirgin olmalı; ikisinin sesi karışmamalı. |
| Tekrar kelime (zaten bulunmuş) | K1 `Audio/tick_002.ogg` | 0.02 sn | K1 `question_001.ogg` (0.49 sn) | Nötr, çok kısa uyarı. Süre çok kısa, dinleme gerekli. |
| Geçersiz kelime | K1 `Audio/error_001.ogg` | 0.16 sn | O3 `fail.wav` (0.36 sn) | Hata seslerinden ayrı tutulmalı (aşağıdaki satır). |
| Hata (sistem) | O3 `damaged.wav` | 0.26 sn | O3 `fail_2.wav` (1.11 sn) | Geçersiz kelime ile aynı dosya olmasın. |
| Karıştırma | K4 `Audio/card-shuffle.ogg` | 3.06 sn | K4 `card-place-1.ogg` (0.69 sn) | Tek uzun ses. Kullanmadan önce ~0.6–0.8 sn'ye kırpılmalı (ffmpeg `-t`), ya da ses seçilmeli. |
| İpucu | K1 `Audio/bong_001.ogg` | 0.12 sn | K3 `highUp.ogg` (0.55 sn) | Dikkat çekici, kısa. |
| Jeton kazanma | K4 `Audio/chip-lay-1.ogg` | 0.17 sn | K4 `chips-stack-1.ogg` (0.29 sn) | Casino çipi, jeton kavramıyla uyumlu. |
| Bonus sayacı dolma | K3 `Audio/phaserUp1.ogg` | 0.50 sn | K3 `highUp.ogg` (0.55 sn) | Yükselen ton. Sayaç ilerledikçe aynı sesin perdesi artırılabilir. Ancak perde ilerlemeye bağlı ise prosedürel sentez gerekir (bkz. bölüm 4). |
| Seviye tamamlama | O3 `level_up.wav` | 0.95 sn | O3 `level_passed.wav` (1.53 sn); O3 `level_passed_2.wav` (1.85 sn) | 8-bit tarzı, belirgin kutlama. |
| Menü gezinme (odak değişimi) | K2 `Audio/rollover1.ogg` | 0.23 sn | K1 `tick_001.ogg`; O4 `sfx_ui_click_7.mp3` (0.24 sn) | Odak değişiminde çalınır. |
| Menü seçim/etkinleştirme | K2 `Audio/click1.ogg` | 0.09 sn | K1 `click_001.ogg`; O4 `sfx_ui_click_1.mp3` (0.46 sn) | Onay için kısa tık. |

Notlar:
- Doğru/bonus/seviye sesleri birbirinden kolay ayırt edilebilecek perde ve süre farkına sahip olmalı. Önerilen dosyalar arasında bunu dinleyerek doğrulayın.
- Karıştırma sesi (3.06 sn) bu eşlemede tek uzun sestir. Diğer olaylar ≤0.55 sn.

## 3. İndirilemeyen / uygun olmayanlar

| Kaynak | Sonuç | Neden |
|---|---|---|
| Freesound | Kullanılmadı | Kullanıcı talimatı; 403 veriyor. |
| OGA "UI Sound Library" (SoundIdeas, `UI Sound Library.zip`, 3.4 MB) | **Elendi** | Sayfada CC-BY 3.0 yazıyor (CC0 değil). Atıf gerektirir ve sayfa dahil lisans tartışması var. |
| OGA "ui-sound-effects-library" | **Elendi** | Aynı nedenle: CC-BY 3.0. |
| OGA "ui-pack" (kenney_ui-pack.zip) | Kapsam dışı | Görsel UI sanatı; ses içermiyor. |
| OGA "free-game-gui" | Kapsam dışı | Görsel GUI paketi; ses içermiyor. |
| OGA diğer ses paketleri (8-bit platformer, dark-sci-fi, rpg-sound-pack vb.) | İncelenmedi | Lisans kontrol edilmedi; bu araştırmada önerilmiyor. |
| Kenney `Preview.ogg` dosyaları | Kullanılmaz | Önizleme dosyaları, paket içeriği değil. |
| SFX_0 zip içindeki RAR | İşlendi, açıldı | Zip içinde lisans dosyası yok; lisans yalnızca OGA sayfasına dayanıyor. Üretimde OGA sayfa URL'si kaydedilmeli. |

## 4. CC0 ile karşılanamayan olaylar: "prosedürel sentez gerekir"

CC0 örnekler yeterli değil, bunun yerine çalışma zamanında üretilen ton (sine/triangle, belirli frekans) gerekiyor:

1. **Harf seçme, perdesi değiştirilebilir (tam notalı):** Harf sırasına göre sabit bir gam (ör. pentatonik) üretilmeli. Örnek kayıtları sürükleyip playbackRate ile değiştirmek perde doğruluğunu ve tınıyı bozar.
2. **Bonus sayacı dolma (ilerlemeye bağlı perde):** Sayaç yüzdesine göre frekans artışı gerekir. Hazır `phaserUp` veya `highUp` sabit bir eğri verir; ilerlemeyi birebir izlemez.

Diğer tüm olaylar (silme, temizleme, doğru/bonus/tekrar/geçersiz, karıştırma, ipucu, jeton, seviye, menü, hata) yukarıdaki CC0 dosyalarla karşılanıyor. Prosedürel sentez, bu iki olayda ihtiyaç duyulursa eklenmeli.

## 5. Kısa sonuç
- Önerilen birincil kaynak: Kenney Interface Sounds (K1) ve Digital Audio (K3), tamamlayıcı olarak Casino Audio (K4) ve OGA chiptune (O3).
- Tüm önerilen seçimler CC0. Lisansı belirsiz paket önerilmedi.
- Dinleme doğrulaması henüz yapılmadı.
