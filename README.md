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
- **Dünya turu:** 10.000 seviye 100 ülkeye bölünmüştür (aşağıda).
- Kelime listesi TDK madde başlarından oluşur; çekimli biçimler (ör. "kitaplar") listede yoktur, geçersiz sayılır.

## Dünya turu
Oyun, Türkiye'den başlayıp ülke ülke ilerleyen bir yolculuktur. Görsel arka plan yoktur; her şey ses ve anlatımla yaşar.
- **100 ülke, 10.000 seviye:** ilk 10 ülke 20'şer, sonraki 30 ülke 50'şer seviyedir; kalan 60 ülke artan 8.300 seviyeyi eşit paylaşır (138 ya da 139). Türkiye 20 seviyedir.
- **Yeni ülke:** ülkenin ilk seviyesi açılınca önce bölgesine özel bir melodi çalar (Avrupa, Asya, Orta Doğu, Afrika, Amerika, Okyanusya için altı ayrı melodi); melodi bittikten sonra (yaklaşık 2 saniye; en uzun melodi ~1,95 sn) ülke tanıtılır, böylece konuşma melodiyle çakışmaz: "Yeni ülke: Türkiye, Asya. Başkenti Ankara. 20 seviyelik yolculuk başlıyor. Biliyor muydun? …". Ses kapalıysa tanıtım gecikmeden okunur. **Tanıtım her ülke için yalnızca bir kez** yapılır: menüye çıkıp girmek ya da uygulamayı kapatıp açmak tekrarlamaz; ülkenin ortasından devam ederken de tanıtım yoktur. "İlerlemeyi sıfırla" bu kaydı da siler, yeni oyunda Türkiye tanıtımı yeniden çalar.
- **Ülke bitişi:** ülkenin son seviyesi bitince **tek bir uzun jingle** (ülke tamamlama sesi) çalar; seviye bitiş jingle'ı ve jeton sesi o anda çalmaz (ses çakışması olmasın), **25 jeton** ödül (bir kez) ve ikinci bir bilgi gelir, sıradaki ülkenin adı söylenir.
- **Dünya turu ekranı:** ana menüdeki "Dünya turu" düğmesi 100 ülkelik, TalkBack ile gezilen bir liste açar. Her satır tek düğümdür: tamamlanan ülke ("Türkiye, tamamlandı. Başkenti Ankara. …"), şu an bulunduğun ülke ("… 5 / 50 seviye, şu an buradasın") ya da kilitli ülke. Ülkenin son seviyesini bitirip "Sonraki seviye"ye henüz basmadıysan da liste ülkeyi "tamamlandı" gösterir. Açılışta odak bulunduğun ülkededir; geri tuşu menüdeki "Dünya turu" düğmesine döner.
- **İlerleme seviyeden türetilir:** bulunduğun ülke, seviye sırandan hesaplanır (tek ek kayıt: tanıtımı yapılan en son ülkenin sırası). Eski kayıtlar olduğu gibi çalışır. Yer listesi okunamazsa ülke özellikleri sessizce kapanır, oyun eskisi gibi sürer.

## Erişilebilirlik
- Her düğmenin Türkçe, anlamlı bir etiketi var. Dokunma hedefleri en az 48dp.
- Bulmaca ızgarası, kutu kutu değil **kelime kelime bir liste** olarak okunur: "Kelime 2, 5 harfli, açık harfler: _ A _ E _".
- Seçtiğin her harf ve her sonuç duyurulur. Duyurular tek merkezden yönetilir, birbirini ezmez.
- Her olay için ayrı, kısa bir ses efekti vardır (olumlu sesler yükselen, olumsuzlar alçalan perdeli). Harf sesi harfe değil kelimedeki sıraya bağlıdır: 1. harf en kalın, her yeni harf bir nota daha ince (7. harf en ince); harf silince bir sonraki harf yine düşük notadan başlar. Ses efektleri Ayarlar'dan kapatılabilir.
- Büyük yazı boyutunda ve küçük ekranlarda düzen taşmaz. Açık/koyu tema ve yüksek kontrast renkler kullanılır.
- Not: Geliştirme ortamında gerçek TalkBack ile test yapılamadı. Erişilebilirlik; Compose semantik ağacı, duyuru metinleri ve odak davranışı üzerinden otomatik testlerle doğrulandı. Gerçek cihazda geri bildirim çok değerlidir.

## İndirme
Her sürümün **debug imzalı APK**'sı [Releases](https://github.com/wzehirx/kelime_oyunu/releases) sayfasındadır. Sürümler **v1.0, v1.1, v1.2 ve v1.3** ayrıca **release APK** içerir.

| Sürüm | İçerik |
|---|---|
| v0.2 | Oyun motoru, sözlük, seviye verisi, ViewModel (henüz oynanabilir arayüz yok) |
| v0.3 | Arayüz, TalkBack duyuruları, menüler, ayarlar |
| v0.4 | Ses efektleri |
| v0.5 | Hata düzeltmeleri, imza ve cila |
| v1.0 | İlk tam sürüm: debug ve release APK |
| v1.1 | Harf sesi seçim sırasına göre kalından inceye; ses seti yeniden üretildi (daha doğal tını). Debug ve release APK |
| v1.2 | Seviye eğrisi düzeltildi: harf ve kelime sayısı seviye numarasıyla hiç düşmez. Debug ve release APK |
| v1.3 | Dünya turu: 100 ülke, bölgeye özel giriş melodileri, ülke tanıtımı ve tamamlama bonusu (25 jeton), Dünya turu ekranı. Debug ve release APK |

> Debug APK'lar depodaki ortak `app/debug.keystore` ile imzalanır; sürümler birbirinin üzerine kurulur. Release APK farklı bir anahtarla imzalıdır, debug ile aynı cihazda üst üste kurulmaz (önce birini kaldır).

### Release imzası
v1.0 release APK, bu proje için üretilen özel bir anahtarla imzalanmıştır. **Anahtar dosyası ve şifresi bu depoda yoktur** (herkese açık bir depoda tutulması, başkasının aynı imzayla sahte APK üretmesine izin verirdi).

- Sertifika SHA-256 parmak izi: `D4:22:33:45:87:08:C7:27:7C:70:08:41:8F:0B:EC:19:47:1F:94:7B:90:0B:52:0F:D6:33:CE:E3:56:B4:B9:4C`
- Doğrulama: `apksigner verify --print-certs kelime-oyunu-v1.0-release.apk`

## Veri kaynakları ve lisanslar
- **Kelime listesi:** TDK Güncel Türkçe Sözlük kaynaklı, [bilalozdemir/tr-word-list](https://github.com/bilalozdemir/tr-word-list) deposundan alınıp temizlendi (çok sözcüklü, kısaltma, özel isim, harf dışı karakter içerenler çıkarıldı, sapkalı ünlüler düzleştirildi). Kaynak README'si CC BY-SA 4.0 beyan eder; depoda LICENSE dosyası yoktur ve TDK'nın yeniden dağıtım koşulları belirsizdir. Ayrıntı: [`tools/words/SOURCES.md`](tools/words/SOURCES.md). Ticari bir kullanım öncesi hukuki kontrol önerilir.
- **Ses efektleri:** 27 ses dosyasının tamamı bu projede, harici örnek veya indirme olmadan, `tools/sounds/build_sounds.py` ile prosedürel olarak sentezlenir (marimba/kalimba/ahşap ve hafif çan tınısı; yalnızca numpy). Kendi üretimimiz, CC0. Olay, dosya, kaynak ve lisans tablosu: [`app/src/main/assets/sounds/SOUNDS.md`](app/src/main/assets/sounds/SOUNDS.md).
- **Yerler (Dünya turu):** `app/src/main/assets/places.json` (100 yer: ad, kıta, bölge, başkent ve iki kısa bilgi). Bilgiler elle yazıldı, harici kaynaktan kopyalanmadı; CC0. Biçim ve içerik denetimi: `python3 tools/places/validate_places.py` (testi: `tools/places/test_validate_places.py`).
- **Seviyeler:** `tools/levels/` altındaki araçla bu sözlükten üretildi (10.000 seviye, hepsi otomatik doğrulandı: çözülebilir, plansız ek kelime yok, ızgara en çok 9×9, benzersiz). Zorluk yalnızca artar: harf sayısı seviye numarasıyla hiç düşmez (sırayla 3 harf 110, 4 harf 450, 5 harf 1.600, 6 harf 3.800, 7 harf 4.040 seviye) ve kelime sayısı da küresel olarak düşmez (2 kelimeden 16 kelimeye). Yeniden üretim: `python3 tools/levels/generate_levels.py --count 10000 --seed 1`, denetim: `python3 tools/levels/validate_levels.py`.

## Geliştirme
- Native **Kotlin + Jetpack Compose**, MVVM, `minSdk 26`, `targetSdk 35`.
- Android SDK kurulumu: `tools/setup-android-sdk.sh`
- Test ve debug derleme: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- Release derleme (kendi anahtarınla): ortam değişkenleri `KELIME_KEYSTORE`, `KELIME_KEYSTORE_PASSWORD`, `KELIME_KEY_ALIAS`, `KELIME_KEY_PASSWORD` ayarlanıp `./gradlew :app:assembleRelease`
- Kelime/seviye/ses araçları: `tools/words`, `tools/levels`, `tools/sounds`, `tools/places` (Python 3).
- Tasarım ve plan: [`docs/superpowers/specs`](docs/superpowers/specs), [`docs/superpowers/plans`](docs/superpowers/plans).
- Yayın: sürüm numarası (`versionName`) değiştirilip dala push edildiğinde GitHub Actions testleri çalıştırır, debug APK'yı derler ve o sürüm için GitHub release oluşturur (`.github/workflows/release.yml`).

## Bilinen sınırlar
- Çekimli kelime biçimleri sözlükte yoktur.
- Bazı seviyelerde nadir TDK kelimeleri çıkabilir (kelime sıklığı verisi kullanılmadı).
- Ses efektleri nicel analizle (süre, seviye, temel frekans, atak, kısmi sayısı, perde yönü) denetlendi, kulakla dinlenerek ayarlanmadı; geri bildirime açığız.
