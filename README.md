# Kelime Oyunu

Words of Wonders tarzı bir Türkçe kelime bulmaca oyunu. **Görme engelli kullanıcılar için, TalkBack ve türevleriyle (ör. Samsung Voice Assistant) baştan sona oynanabilecek şekilde tasarlandı.** Yalnızca Android, tamamen çevrimdışı, internet izni yok.

## Nasıl oynanır
- Her seviyede birkaç harf (3–7) verilir. Bu harflerle kelimeler kurup bulmaca listesindeki kelimeleri bulursun.
- Harflere **dokunarak** (TalkBack'te çift dokunuşla) kelime kurarsın. Klavye gerekmez.
- Eylemler sabit sırada: **Gönder, Sil, Temizle, Karıştır, İpucu (harf aç, 10 jeton), Kelime aç (30 jeton)**.
- Sonuç her zaman sesli duyurulur: "Doğru, KALEM", "Bonus kelime, …", "Bu kelime zaten bulundu", "Geçersiz kelime".
- Bulmacada olmayan ama geçerli bir kelime **bonus kelimedir**. 5 bonus kelime 20 jeton kazandırır.
- Seviye bitince 10 jeton kazanırsın. Başlangıçta 50 jeton var.
- Puan sistemi yok. Amaç seviyeleri geçmek. **10.000 seviye** var.
- Kelime listesi TDK madde başlarından oluşur; çekimli biçimler (ör. "kitaplar") listede yoktur, geçersiz sayılır.

## Erişilebilirlik
- Her düğmenin Türkçe, anlamlı bir etiketi var. Dokunma hedefleri en az 48dp.
- Bulmaca ızgarası, kutu kutu değil **kelime kelime bir liste** olarak okunur: "Kelime 2, 5 harfli, açık harfler: _ A _ E _".
- Seçtiğin her harf ve her sonuç duyurulur. Duyurular tek merkezden yönetilir, birbirini ezmez.
- Her olay için ayrı, kısa bir ses efekti vardır (olumlu sesler yükselen, olumsuzlar alçalan perdeli). Harf sesi harfe değil kelimedeki sıraya bağlıdır: 1. harf en kalın, her yeni harf bir nota daha ince (7. harf en ince); harf silince bir sonraki harf yine düşük notadan başlar. Ses efektleri Ayarlar'dan kapatılabilir.
- Büyük yazı boyutunda ve küçük ekranlarda düzen taşmaz. Açık/koyu tema ve yüksek kontrast renkler kullanılır.
- Not: Geliştirme ortamında gerçek TalkBack ile test yapılamadı. Erişilebilirlik; Compose semantik ağacı, duyuru metinleri ve odak davranışı üzerinden otomatik testlerle doğrulandı. Gerçek cihazda geri bildirim çok değerlidir.

## İndirme
Her sürümün **debug imzalı APK**'sı [Releases](https://github.com/wzehirx/kelime_oyunu/releases) sayfasındadır. Sürüm **v1.0** ve **v1.1** ayrıca **release APK** içerir.

| Sürüm | İçerik |
|---|---|
| v0.2 | Oyun motoru, sözlük, seviye verisi, ViewModel (henüz oynanabilir arayüz yok) |
| v0.3 | Arayüz, TalkBack duyuruları, menüler, ayarlar |
| v0.4 | Ses efektleri |
| v0.5 | Hata düzeltmeleri, imza ve cila |
| v1.0 | İlk tam sürüm: debug ve release APK |
| v1.1 | Harf sesi seçim sırasına göre kalından inceye; ses seti yeniden üretildi (daha doğal tını). Debug ve release APK |

> Debug APK'lar depodaki ortak `app/debug.keystore` ile imzalanır; sürümler birbirinin üzerine kurulur. Release APK farklı bir anahtarla imzalıdır, debug ile aynı cihazda üst üste kurulmaz (önce birini kaldır).

### Release imzası
v1.0 release APK, bu proje için üretilen özel bir anahtarla imzalanmıştır. **Anahtar dosyası ve şifresi bu depoda yoktur** (herkese açık bir depoda tutulması, başkasının aynı imzayla sahte APK üretmesine izin verirdi).

- Sertifika SHA-256 parmak izi: `D4:22:33:45:87:08:C7:27:7C:70:08:41:8F:0B:EC:19:47:1F:94:7B:90:0B:52:0F:D6:33:CE:E3:56:B4:B9:4C`
- Doğrulama: `apksigner verify --print-certs kelime-oyunu-v1.0-release.apk`

## Veri kaynakları ve lisanslar
- **Kelime listesi:** TDK Güncel Türkçe Sözlük kaynaklı, [bilalozdemir/tr-word-list](https://github.com/bilalozdemir/tr-word-list) deposundan alınıp temizlendi (çok sözcüklü, kısaltma, özel isim, harf dışı karakter içerenler çıkarıldı, sapkalı ünlüler düzleştirildi). Kaynak README'si CC BY-SA 4.0 beyan eder; depoda LICENSE dosyası yoktur ve TDK'nın yeniden dağıtım koşulları belirsizdir. Ayrıntı: [`tools/words/SOURCES.md`](tools/words/SOURCES.md). Ticari bir kullanım öncesi hukuki kontrol önerilir.
- **Ses efektleri:** 20 ses dosyasının tamamı bu projede, harici örnek veya indirme olmadan, `tools/sounds/build_sounds.py` ile prosedürel olarak sentezlenir (marimba/kalimba/ahşap ve hafif çan tınısı; yalnızca numpy). Kendi üretimimiz, CC0. Olay, dosya, kaynak ve lisans tablosu: [`app/src/main/assets/sounds/SOUNDS.md`](app/src/main/assets/sounds/SOUNDS.md).
- **Seviyeler:** `tools/levels/` altındaki araçla bu sözlükten üretildi (10.000 seviye, hepsi otomatik doğrulandı: çözülebilir, plansız ek kelime yok, benzersiz).

## Geliştirme
- Native **Kotlin + Jetpack Compose**, MVVM, `minSdk 26`, `targetSdk 35`.
- Android SDK kurulumu: `tools/setup-android-sdk.sh`
- Test ve debug derleme: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- Release derleme (kendi anahtarınla): ortam değişkenleri `KELIME_KEYSTORE`, `KELIME_KEYSTORE_PASSWORD`, `KELIME_KEY_ALIAS`, `KELIME_KEY_PASSWORD` ayarlanıp `./gradlew :app:assembleRelease`
- Kelime/seviye/ses araçları: `tools/words`, `tools/levels`, `tools/sounds` (Python 3).
- Tasarım ve plan: [`docs/superpowers/specs`](docs/superpowers/specs), [`docs/superpowers/plans`](docs/superpowers/plans).
- Yayın: sürüm numarası (`versionName`) değiştirilip dala push edildiğinde GitHub Actions testleri çalıştırır, debug APK'yı derler ve o sürüm için GitHub release oluşturur (`.github/workflows/release.yml`).

## Bilinen sınırlar
- Çekimli kelime biçimleri sözlükte yoktur.
- Bazı seviyelerde nadir TDK kelimeleri çıkabilir (kelime sıklığı verisi kullanılmadı).
- Ses efektleri nicel analizle (süre, seviye, temel frekans, atak, kısmi sayısı, perde yönü) denetlendi, kulakla dinlenerek ayarlanmadı; geri bildirime açığız.
