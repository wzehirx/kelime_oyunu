# Kelime Oyunu (Words of Wonders benzeri) – Tasarım Spec'i

Tarih: 2026-10-25 · Durum: **kullanıcı onayı bekliyor**

## 1. Amaç ve başarı ölçütü
Words of Wonders tarzı kelime bulmaca oyununun, **görme engelliler için TalkBack (ve türevleri) ile tam erişilebilir** Android sürümü.

Başarı ölçütü: Görme engelli bir kullanıcı, ekranı görmeden ve yardım almadan, yalnızca TalkBack ile bir seviyeyi baştan sona bitirebilmeli.

## 2. Kapsam
**v1.0'da var:** En az **10.000 seviye** (bkz. 8), harf havuzu, dokunarak harf girişi, ızgara kelimeleri, bonus kelime, karıştır, ipucu (jeton ile), ilerleme kaydı, ses efektleri, ayarlar, ana menü. Tamamen çevrimdışı, Türkçe.

**v1.0'da yok:** Çarkıfelek, arka plan kartları, mağaza, reklam, çevrimiçi özellikler, klavye girişi.

## 3. Oyun kuralları
- Her seviyede 3–7 harf ve bu harflerle yazılabilen, birbirini kesen kelimelerden oluşan bir ızgara vardır.
- Oyuncu harflere tek tek dokunarak kelime oluşturur ve **Gönder** der.
- Sonuç: *ızgara kelimesi* (kutular dolar), *bonus kelime* (geçerli ama ızgarada yok, bir kez sayılır), *tekrar*, *geçersiz*.
- Tüm ızgara kelimeleri bulununca seviye biter.
- Bonus kelimeler bir sayacı doldurur, dolunca jeton verilir. Seviye bitişi de jeton verir.
- İpucu jeton harcar: rastgele bir kutunun harfini açar. "Kelime aç" ikinci bir ipucu türüdür.
- Puan sistemi yok.
- Bir kelimenin gerçek mi uydurma mı olduğu önemlidir, anlamı önemsizdir.

## 4. Teknik mimari
- **Native Kotlin, Jetpack Compose, MVVM**, tek aktivite, `minSdk 26`.
- Depolama: DataStore (ilerleme, jeton, ayarlar).
- Katmanlar:
  - `domain`: saf Kotlin. `Level`, `GameEngine`, `WordDictionary` (Türkçe büyük/küçük harf kuralları: İ/ı, I/i).
  - `data`: `assets` içindeki sözlük ve seviye dosyaları, `ProgressRepository`.
  - `ui`: `MainMenu`, `GameScreen`, `LevelCompleteScreen`, `Settings`.
  - `a11y`: `Announcer` (duyuru tek merkezden, sıra kontrollü) ve okunacak metin üreticileri.
  - `audio`: `SoundManager` (SoundPool, olay → ses eşlemesi).

## 5. Erişilebilirlik tasarımı
- Harf düğmeleri TalkBack'te tek tek odaklanılır, çift dokunuşla seçilir. Sürükleme zorunlu değildir.
- "Oluşturulan kelime" alanı canlı bölgedir. Her eklenen harf okunur.
- Sabit ve sıralı eylem düğmeleri: **Gönder, Sil, Temizle, Karıştır, İpucu**.
- Izgara, kutu kutu değil kelime kelime bir liste olarak sunulur: "Kelime 2, 5 harfli, açık harfler: _ A _ E _".
- Sonuçlar duyurulur: "Doğru, KALEM", "Bonus kelime", "Bu kelime zaten bulundu", "Geçersiz kelime", "Seviye tamamlandı".
- Tüm hedefler ≥ 48dp, mantıklı odak sırası, bilgi yalnızca renkle veya görselle verilmez.
- Duyuru metinleri tek yerden yönetilir ve birim testle doğrulanır.

## 6. Ses efektleri ve veri seti (yüksek öncelik)
Ses, görme engelli kullanıcı için ana geri bildirim kanalıdır. Konuşma (TalkBack) ile çakışmamalı, kısa ve ayırt edilebilir olmalıdır.

**Olay listesi (en az):** harf seçme (perde harfin sırasına göre değişir), harf silme, temizleme, doğru kelime, bonus kelime, tekrar, geçersiz kelime, karıştır, ipucu kullanma, jeton kazanma, bonus sayacı dolması, seviye tamamlama, menü gezinme, hata/ipucu yok.

**Kaynak stratejisi (öncelik sırasıyla):**
1. **CC0 / serbest lisanslı** kaynaklardan derleme (Kenney, OpenGameArt vb.). Freesound bu ortamda erişilemiyor (403).
2. Eksik olaylar için **Python ile prosedürel sentez** (numpy, wav). Telif sorunu olmaz, tutarlı bir ses ailesi çıkar.
3. İsteğe bağlı: ElevenLabs bağlantısı ile üretim (kredi harcar, onay gerekir).

**Teslimat:** `assets/sounds/` altında tutarlı biçimde (mono, 44.1 kHz, normalize edilmiş, kısa), `SOUNDS.md` ile olay → dosya → kaynak → lisans tablosu. Lisansı belirsiz hiçbir ses kullanılmaz.

## 7. Kelime listesi
- Hedef: TDK Güncel Türkçe Sözlük kelimeleri. Gerekli bilgi yalnızca "kelime gerçek mi".
- Yöntem 1: TDK API'si (`sozluk.gov.tr`) ile toplama. **Bu ortamdan şu an erişilemiyor.**
- Yöntem 2: GitHub veya açık veri setlerinden TDK kaynaklı kelime listesi indirme.
- Temizleme: tek kelime, yalnızca Türkçe harfler, özel isimler ve kısaltmalar çıkarılır, küçük harfe normalize edilir, mükerrerler atılır.
- Çıktı: tek kelime/satır düz metin dosyası, kaynak ve lisans notu ile.
- Lisans/kullanım koşulu belirsizse kullanıcıya bildirilir.

## 8. Seviye üretimi
**Hedef: v1.0'da en az 10.000 seviye.** Seviyeler önceden üretilir ve uygulamaya sıkıştırılmış tek dosya olarak (tahmini birkaç MB) gömülür. Seviye başına yalnızca harfler, kelimeler ve yerleşim saklanır. Zorluk eğrisi 10.000 seviyeye yayılır. İsteğe bağlı genişleme: 10.000. seviyeden sonra sıra sayısını tohum alan, cihazda çalışan deterministik bir üretici ile sonsuz mod. Bu v1.0 şartı değildir, zaman kalırsa eklenir.

Geliştirme sırasında çalışan ayrı bir Python aracı sözlükten seviyeleri üretir: harf kümesi seçilir, o harflerle yazılabilen kelimeler bulunur, kesişen bir ızgara yerleştirilir. Uygulama yalnızca hazır seviye dosyalarını okur. Zorluk, harf ve kelime sayısıyla artar. Üretilen her seviye (10.000'inin tamamı) bir doğrulama betiğinden geçer, tekrarlayan harf/kelime kombinasyonları elenir (çözülebilir mi, tüm kelimeler sözlükte mi, ızgara tutarlı mı).

## 9. Süreç ve görev dağılımı
Claude Pro limitleri değerli olduğu için iş, işin zorluğuna göre modellere dağıtılır:

| İş | Model |
|---|---|
| Ses efekti araştırması, dosya/kaynak okuma, veri toplama | **Haiku** |
| Kodlama (uygulama, araçlar, betikler) | **Sonnet** |
| Kod testi (kodu yazandan **ayrı** bir Sonnet) | **Sonnet** (ayrı ajan) |
| Testten geçmeyen ve düzeltilemeyen kod | **Opus** |

Koordinasyon bu oturumdadır. Alt görevler ayrı ajanlara verilir.

**Limit zamanlayıcısı:** Kodlama başladığında her **2,5 saatte** bir (150 dk) zamanlayıcı devreye girer. Kodlama durmuş mu, limit mi dolmuş diye bakar, durmuşsa devam komutu verir, durmamışsa sessizce yeniden kurulur. Not: zamanlayıcı yalnızca oturum/container yaşadığı sürece çalışır. Uzun süre etkinlik olmazsa container geri alınabilir, bu yüzden her aşama GitHub'a push edilir.

**Sürümler:** Ara sürümler (v0.x) tamamen benim sorumluluğumdadır ve benim tarafımdan test edilir. Kullanıcı yalnızca **v1.0**'ı oynar.
- v0.1 iskelet + oyun motoru + birim testler
- v0.2 sözlük + seviye verisi
- v0.3 arayüz + erişilebilirlik
- v0.4 ses
- v0.5 cila ve kapsamlı test
- v1.0 final

## 10. Test stratejisi
- Domain katmanı: JUnit birim testleri.
- UI ve erişilebilirlik: Compose UI testleri (Robolectric ile JVM'de), semantics ağacı ve duyuru metinleri doğrulanır.
- Seviye verisi: otomatik doğrulama betiği.
- Her sürümde ayrı test ajanı çalışır. Düzeltilemeyen hata Opus'a gider.

## 11. Yayın ve imzalama
- Tüm geliştirme GitHub'a push edilir (geliştirme dalı: `ccr-b0baec48-v0gwgl`).
- Her sürüm için GitHub'da **debug imzalı APK** bulunur.
- **v1.0:** debug imzalı APK **ve** release APK birlikte yayınlanır. Release APK, bu oturum için üretilen ayrı bir anahtar dosyasıyla (`keytool`) imzalanır. **Anahtar dosyası ve şifresi depoya konmaz**, yalnızca bu Claude oturumunun dosya sisteminde (depo dışında, `.gitignore` ile de korunan bir klasörde) tutulur. README yalnızca imzanın SHA-256 parmak izini ve anahtarın gizli tutulduğunu belirtir. İstersen v1.0 tesliminde anahtarı sana ayrıca dosya olarak veririm.
- Yayın yöntemi: öncelik GitHub Actions + `gh release` ile. Bu çalışmazsa APK'lar depoda `apk/` klasörüne commit edilir (GitHub MCP araçlarında release asset yükleme yok).
- README: oyun tanımı, kurulum, erişilebilirlik notları, sürüm listesi, anahtar dosyası açıklaması, lisanslar.

## 12. Riskler ve kısıtlar
1. **Gerçek TalkBack testi yapılamaz.** Ortamda KVM yok, emülatör çalışmaz. Semantics ve duyuru metinleri otomatik doğrulanır ama gerçek konuşma/odak deneyimini yalnızca sen v1.0'da duyacaksın. Bu yüzden ara sürümleri de dilersen telefonda denemen faydalı olur.
2. **Android SDK ortamda kurulu değil.** `dl.google.com` erişilebilir, komut satırı araçlarını kurup derleme yapacağız. Java 21 ve Gradle 8.14 mevcut.
3. **TDK API'ye erişilemiyor.** Yöntem 2'ye düşme ihtimali yüksek.
4. **Anahtar yalnızca oturumda durur.** Container geri alınırsa anahtar kaybolur. Sonraki bir güncelleme farklı bir anahtarla imzalanırsa cihazdaki eski sürümün üzerine kurulmaz (önce kaldırmak gerekir). Bunu önlemek için v1.0 tesliminde anahtarı sana dosya olarak vereceğim.
5. **Zamanlayıcı** oturum yaşamıyorsa çalışmaz.

## 13. Açık kararlar
- Ses kaynağı için ElevenLabs kullanımı (kredi) gerekirse ayrıca onay istenecek.
- Sonsuz mod (10.000 seviyeden sonra) v1.0'a dahil mi, sonraya mı kalsın? Öneri: sonraya.
