# Kelime Oyunu

Words of Wonders tarzı bir Türkçe kelime bulmaca oyunu. **Görme engelli kullanıcılar için, TalkBack ve türevleriyle (ör. Samsung Voice Assistant) baştan sona oynanabilecek şekilde tasarlandı.** Yalnızca Android, tamamen çevrimdışı, internet izni yok.

## Nasıl oynanır
- Her seviyede birkaç harf (3–7) verilir. Bu harflerle kelimeler kurup bulmaca listesindeki kelimeleri bulursun.
- Harflere **dokunarak** (TalkBack'te çift dokunuşla) kelime kurarsın. Klavye gerekmez.
- Eylemler sabit sırada: **Gönder, Sil, Temizle, Karıştır, İpucu (harf aç, 15 jeton), Kelime aç (40 jeton)**.
- Sonuç her zaman sesli duyurulur: "Doğru, KALEM", "Bonus kelime, …", "Bu kelime zaten bulundu", "Geçersiz kelime".
- Bulmacada olmayan ama geçerli bir kelime **bonus kelimedir**. 5 bonus kelime 10 jeton kazandırır.
- Seviye bitince 5 jeton kazanırsın; ülke bitince 25 jeton. Başlangıçta 50 jeton var. Tüm jeton sayıları tek yerde tanımlıdır (`domain/GameEconomy.kt`), ekrandaki metinler oradan üretilir.
- Puan sistemi yok. Amaç seviyeleri geçmek. **10.000 seviye** var.
- **Dünya turu:** 10.000 seviye 100 ülkeye bölünmüştür (aşağıda).
- Kelime listesi (v1.4): TDK madde başları + fiil kökleri ("sat", "yat") + yaygın çekimli biçimler ("evler", "kitapta"; en çok 7 harf); argo ve küfür kabul edilmez.

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

## Görsel tasarım (v2.0 çalışması)
Görsel katman erişilebilirliğin ÜSTÜNE eklenir; TalkBack anlamsal ağacı, etiketler, okuma sırası ve duyuru zamanlaması değişmez (süs öğeleri `clearAndSetSemantics` ile gizlidir). Belirtim: [`visual-design.md`](docs/superpowers/specs/2026-10-27-visual-design.md), [`visual-handoff.md`](docs/superpowers/specs/2026-10-27-visual-handoff.md).
- **Kimlik:** seyahat defteri. Açık "Deniz kâğıdı", koyu "Gece haritası"; fildişi harf jetonları; bölge damga mürekkepleri (altı bölge). Tüm metin/zemin çiftleri iki temada WCAG AA (metin 4,5:1, bileşen 3:1; `ThemeContrastTest`).
- **Ekranlar:** karo logolu ana menü ve "Devam et" kahraman paneli; çizgili defter satırlı Dünya turu; pusula halkası harf çarkı, gerçek çapraz bulmaca ızgarası (ekran okuyucudan gizli), kompakt kelime listesi; ülke bitişinde damga, seviye bitişinde konfeti.
- **Ayarlar:** Ses efektleri, **Titreşim** ve **Animasyonlar** (üçü de varsayılan açık). Sistem animasyon ölçeği 0 ya da Animasyonlar kapalıysa tüm hareket anlık olur. Titreşim `performHapticFeedback` ile verilir (izin yok).
- **Yazı tipi:** [Atkinson Hyperlegible Next](https://github.com/googlefonts/atkinson-hyperlegible-next) (SIL OFL 1.1), Regular ve Bold, uygulamaya gömülü; lisans `assets/licenses/` altında. Türkçe harfler `tools/fonts/verify_font_glyphs.py` ile doğrulanır.
- Simgeler Canvas ile çizilir; `material-icons-extended` ve yeni bağımlılık yoktur.

## İndirme
Her sürümün **debug imzalı APK**'sı [Releases](https://github.com/wzehirx/kelime_oyunu/releases) sayfasındadır. Sürümler **v1.0–v1.5** ayrıca **release APK** içerir.

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
| v1.4 | Düzgün sözlük (Zemberek + sıklık listesi: fiil kökleri ve çekimli biçimler kabul, argo reddedilir, ızgarada yalnızca yaygın sözcükler), hızlı yükselen ve daha zor seviye eğrisi (3 harfli yalnızca ilk 6 seviye), jeton dengesi (seviye +5, bonus sayacı +10, harf ipucu 15, kelime aç 40). Debug ve release APK |
| v1.5 | Veri temizliği: ızgaradan uygunsuz sözcükler ve özel isimler çıkarıldı, kabul setinden küfür kalıpları çıkarıldı, ilk 700 seviyede yalnızca yaygın sözcükler. Debug ve release APK |

> Not: v2.0 görsel yenilemesi bu tabloda henüz listelenmedi; sürüm satırı yayın sırasında eklenecek.

> Debug APK'lar depodaki ortak `app/debug.keystore` ile imzalanır; sürümler birbirinin üzerine kurulur. Release APK farklı bir anahtarla imzalıdır, debug ile aynı cihazda üst üste kurulmaz (önce birini kaldır).

### Release imzası
v1.0 release APK, bu proje için üretilen özel bir anahtarla imzalanmıştır. **Anahtar dosyası ve şifresi bu depoda yoktur** (herkese açık bir depoda tutulması, başkasının aynı imzayla sahte APK üretmesine izin verirdi).

- Sertifika SHA-256 parmak izi: `D4:22:33:45:87:08:C7:27:7C:70:08:41:8F:0B:EC:19:47:1F:94:7B:90:0B:52:0F:D6:33:CE:E3:56:B4:B9:4C`
- Doğrulama: `apksigner verify --print-certs kelime-oyunu-v1.0-release.apk`

## Veri kaynakları ve lisanslar
- **Kelime listesi (v1.4):** iki katmanlı, `tools/words/build_wordlists.sh` ile tekrarlanabilir. **Kabul seti** (`app/src/main/assets/words.txt`, 55.055 sözcük, 2-7 harf): TDK madde başları ([bilalozdemir/tr-word-list](https://github.com/bilalozdemir/tr-word-list), temizlenmiş) + fiil kökleri (TDK mastarlarından ve [Zemberek](https://github.com/ahmetaa/zemberek-nlp) sözlüğünden: `sat`, `yat`, `gel`) + Zemberek'in geçerli saydığı, sıklık listesinde yeterince geçen çekimli biçimler (`evler`, `kitapta`) − argo filtresi (+ elle eklenen `ud`, `accept_include.txt`). **Izgara havuzu** (`tools/words/grid_pool.txt`, 9.678 sözcük, uygulamaya girmez): yalnızca yaygın, çekimsiz içerik sözcükleri; "ast" gibi nadir kayıtlar, kişi/yer adları (`ali`, `kaya`, `mine` ...), yabancı/şüpheli kısa sözcükler (`aut`, `bop` ...), cinsellik/hakaret sözcükleri ve ı'lı yazılışı normal olan i'li yazılışlar (`ayni`, `akil`, `adi` ...) bulmacaya girmez (`pool_exclude.txt` + sıklık oranıyla otomatik i/ı süzgeci). **Argo filtresi** (`tools/words/blocklist.txt`, 126 sözcük + `blocklist_extra_forms.txt` ile 76 elle eklenen çekim/kalıp): `am`, `sik`, `göt`, `penis`, `porno`, `kıç`, `lan`, `çingene` vb. ve çekimli biçimleri kabul edilmez; `hıyar`, `öküz` gibi hafif hakaretler kabul setinde kalır ama ızgaraya girmez. Kaynaklar ve lisanslar: Zemberek **Apache-2.0**; sıklık listesi [hermitdave/FrequencyWords](https://github.com/hermitdave/FrequencyWords) (kod MIT, **içerik OpenSubtitles altyazılarından derlenmiştir ve içerik lisansı belirtilmemiştir**; listenin kendisi depoya konmaz, yalnızca ondan süzülmüş sözcükler girer). Zemberek jar'ları ve sıklık listesi de depoda yoktur, betik indirir (SHA-256 doğrulamalı). TDK kaynağının lisans belirsizliği değişmedi: README'si CC BY-SA 4.0 beyan eder, depoda LICENSE dosyası yoktur, TDK'nın yeniden dağıtım koşulları belirsizdir; ticari kullanım öncesi hukuki kontrol önerilir. Ayrıntı ve eşik gerekçeleri: [`tools/words/SOURCES.md`](tools/words/SOURCES.md).
- **Ses efektleri:** 27 ses dosyasının tamamı bu projede, harici örnek veya indirme olmadan, `tools/sounds/build_sounds.py` ile prosedürel olarak sentezlenir (marimba/kalimba/ahşap ve hafif çan tınısı; yalnızca numpy). Kendi üretimimiz, CC0. Olay, dosya, kaynak ve lisans tablosu: [`app/src/main/assets/sounds/SOUNDS.md`](app/src/main/assets/sounds/SOUNDS.md).
- **Yerler (Dünya turu):** `app/src/main/assets/places.json` (100 yer: ad, kıta, bölge, başkent ve iki kısa bilgi). Bilgiler elle yazıldı, harici kaynaktan kopyalanmadı; CC0. Biçim ve içerik denetimi: `python3 tools/places/validate_places.py` (testi: `tools/places/test_validate_places.py`).
- **Seviyeler (v1.4):** `tools/levels/` altındaki araçla üretildi (10.000 seviye; hepsi otomatik doğrulandı: çözülebilir, plansız ek kelime yok, ızgara en çok 9×9, ızgara kelimeleri havuzda, tüm kelimeler kabul setinde, engelli kelime yok, her seviyede harf havuzunu tam kullanan en az bir kelime). Zorluk yalnızca artar: harf sayısı hiç düşmez ve kelime sayısı küresel olarak düşmez. Eğri: **3 harf yalnızca ilk 6 seviye**, 4 harf 7-30, 5 harf 31-700, 6 harf 701-5.264, 7 harf 5.265-10.000; kelime sayısı 2'den 16'ya. Erken seviyeler yaygın sözcüklerden kurulur: ızgara sözcüğünün havuz (sıklık) sırası id ≤ 60'ta < 2000, id ≤ 700'de < 3000 (en büyük gerçek değerler 1973 ve 2997). Küçük havuz yüzünden aynı harf kümesi en çok 3 kez, birbirinden en az %50 farklı kelime kümeleriyle kullanılır. Yeniden üretim: `python3 tools/levels/generate_levels.py --count 10000 --seed 1`, denetim: `python3 tools/levels/validate_levels.py`.

## Geliştirme
- Native **Kotlin + Jetpack Compose**, MVVM, `minSdk 26`, `targetSdk 35`.
- Android SDK kurulumu: `tools/setup-android-sdk.sh`
- Test ve debug derleme: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- Release derleme (kendi anahtarınla): ortam değişkenleri `KELIME_KEYSTORE`, `KELIME_KEYSTORE_PASSWORD`, `KELIME_KEY_ALIAS`, `KELIME_KEY_PASSWORD` ayarlanıp `./gradlew :app:assembleRelease`
- Kelime/seviye/ses araçları: `tools/words`, `tools/levels`, `tools/sounds`, `tools/places` (Python 3).
- Tasarım ve plan: [`docs/superpowers/specs`](docs/superpowers/specs), [`docs/superpowers/plans`](docs/superpowers/plans).
- Yayın: sürüm numarası (`versionName`) değiştirilip dala push edildiğinde GitHub Actions testleri çalıştırır, debug APK'yı derler ve o sürüm için GitHub release oluşturur (`.github/workflows/release.yml`).

## Bilinen sınırlar
- Kabul seti çekimli biçimleri yalnızca sıklık listesinde yeterince geçenler için içerir; çok nadir bir çekim geçersiz sayılabilir.
- Izgara havuzu 10.000 seviye için gevşetilmiş bir eşikle (kısa sözcükler daha sıkı) kurulduğundan 6-7 harfli seviyelerde nadir TDK sözcükleri çıkabilir; aynı harf kümesi 10.000 seviye içinde birkaç kez geri gelir.
- Argo filtresi elle seçilmiş küçük bir listedir; kapsamlı bir hakaret sözlüğü değildir.
- Ses efektleri nicel analizle (süre, seviye, temel frekans, atak, kısmi sayısı, perde yönü) denetlendi, kulakla dinlenerek ayarlanmadı; geri bildirime açığız.
