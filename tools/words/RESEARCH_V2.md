# Kelime listesi araştırması v2 (kabul seti ve ızgara havuzu)

Araştırma tarihi: 2026-10-27. Geçici dosyalar: `/tmp/claude-0/-home-user-kelime-oyunu/8738d2b8-2244-5bc4-8edc-4bc5cda0eb10/scratchpad/wordlist-research/` (repoya konmadı). Kod, commit veya push yapılmadı.

Sorun özeti (kullanıcı): (1) fiil kökleri eksik ("sat" kabul edilmiyor, "satmak" var), (2) "ast" gibi nadir/kısaltma kelimeler ızgaraya giriyor, (3) çekimli biçimler ("evler", "kitaplar") yok. Oyun için 2–7 harf gerekiyor.

## 1. Kaynak tablosu

| Kaynak | URL / koordinat | İndirme | Boyut / içerik | Lisans | Erişilebilirlik |
|---|---|---|---|---|---|
| Zemberek NLP 0.17.1 (jar'lar) | `https://raw.githubusercontent.com/ahmetaa/maven-repo/master/zemberek-nlp/<artifact>/0.17.1/` (groupId `zemberek-nlp`) | `curl -L -o` ile jar + pom | morphology 2.3 MB (içinde `tr/lexicon.bin` 1.27 MB), core 0.34 MB, tokenization, lm, normalization | Apache-2.0 (`ahmetaa/zemberek-nlp` LICENSE, Copyright 2018 Akın) | Erişilebilir. Maven Central'da `zemberek-nlp` grubu 404 döndü; resmi dağıtım GitHub maven-repo'su. Central'da 429 da geldi |
| Zemberek bağımlılıkları | Maven Central: `caffeine-2.6.2`, `protobuf-java-3.6.1`, `guava-28.2-jre`, `antlr4-runtime-4.7` | `repo1.maven.org` üzerinden; 429 gelirse 10-60 sn bekleyerek yeniden denendi | caffeine 0.66 MB, protobuf 1.4 MB, guava 2.8 MB, antlr 0.4 MB | Apache-2.0 / BSD (standart) | İndi (429 için yeniden deneme yeterli) |
| FrequencyWords tr_50k | `https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/tr/tr_50k.txt` | `curl -L` | 50.000 satır, `kelime sayı` | Kod MIT (Hermit Dave). İçerik OpenSubtitles altyazılarından; içerik lisansı README'de ayrıca belirtilmiş olabilir, doğrulanmalı | İndi |
| FrequencyWords tr_full | `.../tr/tr_full.txt` | `curl -L` | 2.035.629 satır (sıklık sıralı, çoğu gürültü/özel isim) | Aynı | İndi (29,7 MB) |
| Hunspell tr (wooorm/dictionaries) | `https://raw.githubusercontent.com/wooorm/dictionaries/main/dictionaries/tr/index.{dic,aff}` | `curl -L` | `.dic` 371.057 satır (9,1 MB), `.aff` 13.254 SFX kuralı, `FLAG num` | Paket MIT (Titus Wormer). Kaynak: extensions.openoffice.org tr_TR; orijinal sözlüğün lisansı paketin README'sinde açıkça yok, **doğrulanmalı** | İndi |
| Hunspell tr_TR (LibreOffice) | `https://raw.githubusercontent.com/LibreOffice/dictionaries/master/tr_TR/tr_TR.{dic,aff}` | `curl -L` | `.dic` 36 MB, `.aff` 2,3 MB | README 404; lisansı `dictionaries.xcu` dışında doğrulanamadı | İndi (yedek) |
| TDK türevi: bilalozdemir/tr-word-list | `https://raw.githubusercontent.com/bilalozdemir/tr-word-list/master/files/words.json` | Mevcut `tools/words/fetch_words.py` | 92.406 kayıt, mevcut `words.txt` kaynağı (58.074 kelime) | README CC BY-SA 4.0 bağlantısı; repoda LICENSE yok. TDK veri hakkı belirsiz (SOURCES.md'de zaten not edilmiş) | Önceki oturumda indirildi |
| erogluegemen/TDK-Dataset (HF `erogluegemen/TDK_Turkish_Words`) | HF / Kaggle | Gated, anonim indirme 401 | 77 MB CSV (tahmini) | Arama özeti HF'de cc0-1.0 gösteriyor; indirilemedi, doğrulanamadı | Erişilemiyor (oturum açma gerekli) |
| tdk-py (PyPI) | `pypi.org/project/tdk-py` | pip | API sarmalayıcı, veri değil | MIT (kod) | Erişilebilir ama kelime listesi sağlamıyor |
| sozluk.gov.tr | `https://sozluk.gov.tr/` | - | Birincil kaynak | - | **Erişilemiyor** (TLS el sıkışması kesiliyor, bkz. SOURCES.md bölüm 1). Değişmedi |
| GitHub arama (`gh search`, `api.github.com/search`) | - | - | - | - | 403: oturum yalnızca yapılandırılmış depolara bağlı. Fiil kökü içeren TDK dökümü bulunamadı |

## 2. Zemberek deneme sonuçları

Ortam: Java 21 (`/usr/bin/java`), `JAVA_TOOL_OPTIONS` proxy truststore'u ayarlıyor. Sınıf yolu: 7 jar (morphology, core, tokenization, lm, normalization, caffeine, protobuf, guava, antlr). Derleme ve çalıştırma `javac`/`java` ile, Maven olmadan. Yükleme süresi ~1,5 sn (`TurkishMorphology.createWithDefaults()`).

Program: `zemberek/demo/ZDemo.java`, çıktı `zemberek/demo/output.txt`.

| Kelime | Kabul (`isCorrect`) | Analiz (lemma / root / POS / özellik) | Yorum |
|---|---|---|---|
| sat | evet | satmak / sat / Verb (Imp) | Fiil kökü, kök = `root`. **Doğru** |
| yat | evet | yatmak / yat / Verb; yat / yat / Noun | Fiil ve isim |
| tas | evet | tas / tas / Noun; tas / Adj | İsim/sıfat |
| taş | evet | taş / Noun; taşmak / taş / Verb; taş / Adj | İsim, fiil kökü, sıfat |
| ast | evet | ast / ast / Noun (özellik: NoVoicing, sekonder POS yok) | **Sorun:** nadir sözcük, isim olarak kabul ediliyor. Zemberek sıklık/nadirlik bilmiyor |
| evler | evet | ev / Noun, Noun+A3pl (çekimli) | Çekim çözümlemesi çalışıyor |
| kitaplar | evet | kitap / Noun+A3pl (ayrıca "Kitap" özel isim bölümü) | Çekim çözümlemesi çalışıyor |
| am | evet | am / Noun | Kabul ediliyor. Argo filtresi Zemberek'te yok |
| gelmek | evet | gelmek / gel / Verb (Inf) | Mastar ve kök eşleşiyor |
| yazdı | evet | yazmak / yaz / Verb+Past+A3sg; yaz / Noun (Time) | Çekimli fiil çözümleniyor |

Sözlük istatistiği (`RootLexicon.getDefault()`):

- Toplam 67.755 madde. Lemma tekil: 65.288; kök tekil: 62.725.
- POS: Noun 56.691, Adjective 5.504, Adverb 1.300, Verb 3.455, Interjection 277, Duplicator 223, diğerleri küçük.
- Sekonder POS: **ProperNoun 33.009** (özel isim, çoğu "PronunciationGuessed"), Abbreviation 1.478, None 33.052. Yani sözlüğün yarısı özel isim; filtre şart.
- Fiil maddeleri lemma olarak mastar tutuluyor (`satmak`), kök ayrı alanda (`sat`). 3.454/3.455 fiil lemması `-mak/-mek` ile bitiyor. Kökü `d.root` alanından okumak yeterli.
- Kısıt: Zemberek sözlüğünde `gel`, `git`, `ver` gibi kökler `lemma` olarak aranınca bulunmuyor, ama analiz (`analyze("gel")`) doğru fiil kökünü veriyor. Fiil doğrulaması `analyze` + `root` ile yapılmalı.

Toplu tarama (`zemberek/demo/ZCheck.java`, çıktılar `zcheck_repo.tsv`, `zcheck_freq50k.tsv`):

| Liste | Satır | Zemberek kabul | İçerik + özel isim değil | Fiil kökü eşleşmesi |
|---|---|---|---|---|
| Repo `words.txt` | 58.074 | 56.884 (%98,0) | 56.540 (%97,4) | 405 |
| FrequencyWords tr_50k | 50.000 | 48.408 (%96,8) | 42.754 (%85,5) | 660 |

- Repo listesinde 2–7 harf, yalnızca Türkçe harf, Zemberek kabul ve içerik/özel-değil: **21.048** (2–7 harf toplam 21.629).
- Repo listesinde Zemberek'in reddettiği: 1.190 kelime (örn. "abartık", "absent", "advertorial", "acve"). Bunlar yabancı sözcük, yazım hatası ya da birleşik kelimeler.
- Repo listesinde içerik olmayan / sadece özel isim olan: 1.534.

Hedef kelimeler (sıklık sırası: tr_50k rank; repoda var mı):

| Kelime | tr_50k rank | Zemberek | Repoda |
|---|---|---|---|
| sat | 10116 | kabul, fiil kökü | **yok** (sorunun kaynağı) |
| yat | 2272 | kabul, fiil kökü | var |
| tas | 26325 | kabul, isim/sıfat | var |
| taş | 1779 | kabul, fiil kökü/isim | var |
| ast | **50 bin dışı** | kabul, isim (nadir) | var (kalite sorunu) |
| am | 9830 | kabul, isim | var (argo riski) |
| evler | 10876 | kabul, çekim | **yok** |
| kitaplar | 6745 | kabul, çekim | **yok** |

Sonuç: Zemberek çalışıyor ve üç sorunu da çözebilir: fiil kökü (`root`), çekimli biçim (`isCorrect`), özel isim/kısaltma filtresi (`secondaryPos`). **Ancak** "ast" gibi nadir ama sözlükte olan isimleri eleyemez; bu iş sıklık listesine bırakılmalı.

## 3. Hunspell (yalnızca olabilirlik)

- `tr.dic` + `tr.aff` indi; `unmunch` ve `hunspell` araçları yok, Python `hunspell` modülü de yok.
- Basit bir Python açıcı (`hunspell/mini_unmunch.py`, yalnızca SFX, `FLAG num`, çapraz ürünü yok sayar) denemesi:
  - `sat` (118 bayrak): sat, sata, satın, sattı, satar, satsa, satmış ...
  - `kitap`: kitap, kitaplar, kitapta, kitaptan, kitaplara ...
  - `ev`: ev, eve, evi, evler (ev+ler), evden, evine ...
  - `ast`: ast, astla, astlar, asttan ... (yani bir isim gibi çekilir, sözlükte yer alması kalite sorununu değiştirmez)
- Sonuç: çekimli biçim üretimi mümkün. Ancak bu sürüm üretim için yeterli değil: çapraz ürün (`Y` kuralları), ünlü uyumu koşulları ve gereksiz çekimler (ör. `tasla`, `amdan`) için doğrulama gerekir. Zemberek zaten aynı işi (çözümleme yönünde) daha güvenilir yapıyor; Hunspell daha çok "üretim" için yedek.
- Lisans: paket MIT; orijinal tr_TR sözlüğünün lisansı net değil. Ürünle dağıtmadan önce doğrulanmalı.

## 4. TDK'nın diğer yansımaları (D)

- Kesin bir fiil kökü içeren TDK dökümü bulunamadı. GitHub API bu oturumda kapalı, web aramasında da net bir veri seti çıkmadı.
- `tr-word-list` (mevcut kaynak): mastar biçimi, kök yok; lisans belirsiz.
- erogluegemen TDK-Dataset: gated, indirilemedi; arama özetinde CC0 etiketi görünüyor ama doğrulanamadı.
- tdk-py / tdk.gts / lugat: API sarmalayıcıları; sozluk.gov.tr erişilemediği için bu ortamda işe yaramıyor. Lisanslar (MIT) kod için geçerli, veri için değil.
- Sonuç: Fiil kökleri için en güvenilir yol Zemberek'in `root` alanı (sözlüğü TDK maddelerinden türetilmiş, Apache-2.0).

## 5. Öneri: iki katmanlı boru hattı

Amaç: (i) geniş, geçerli bir "kabul seti" ve (ii) ızgara için yalnızca yaygın kelimelerden oluşan "havuz".

### (i) Kabul seti (oyuncu girdisini doğrulama)

Kaynaklar, birleşim sırasıyla:

1. Mevcut `words.txt` (TDK madde başları, 58.074) — mevcut davranış korunur.
2. Zemberek fiil kökleri: `analyze(w)` içinde `Verb` ve `root == w` olan her kök (örn. `sat`, `yat`). Tahmini kazanç: 2–7 harfli fiil kökleri, 3.455 fiil maddesinin büyük kısmı; kesin sayı kökleri benzersiz çıkarınca belirlenir.
3. Zemberek çekimli biçimleri: isim/sıfat/fiil maddesinden üretilen yüzey biçimleri, ancak yalnızca bu tip sınırlar içinde (örn. isim + çoğul/iyelik/hal) ve 7 harf üst sınırı ile. Büyük patlama riski var; kabul kontrolü için *çözümleme* (analyze) tercih edilmeli, liste üretmek yerine.

Doğrulama: kullanıcı girdisi için `isCorrect` + (içerik POS ve sekonder POS ≠ ProperNoun) + (Informal/Abbreviation değil). Bu sayede "sat", "evler", "kitaplar" kabul edilir, "ast" kabul edilmeye devam eder (kabul setinde kalması sorun değil).

### (ii) Izgara havuzu (ızgaraya seçilen kelimeler)

Filtreler, sırayla:

1. Biçim: yalnızca küçük harf Türkçe alfabe, uzunluk 2–7.
2. Sıklık: FrequencyWords `tr_50k` (veya tr_full'un ilk 40.000 satırı) içinde olmak; rank eşiği ~25.000–40.000.
3. Zemberek: `isCorrect`, POS ∈ {Noun, Verb-root, Adj, Adv}, sekonder POS ∉ {ProperNoun, Abbreviation}, `Informal` yok.
4. Argo/kötü kelime: `blocklist_candidates.txt` ve `blocklist_ambiguous.txt` (repoda zaten var) uygulanır.
5. Fiil: ızgarada yalnızca kök formu (`sat`, `yat`), mastar değil. Çekimli biçimler ızgaraya girmesin (kullanıcı kararı; çekimli yalnızca kabul setinde).

Tahmini havuz büyüklükleri (ölçüm: tr_50k üzerinde Zemberek + filtre):

| Sıklık eşiği | Alpha 2–7 | Zemberek + içerik + özel değil |
|---|---|---|
| top 10.000 | 6.306 | 5.254 (ölçüm) |
| top 20.000 | 11.018 | 8.863 (ölçüm) |
| top 30.000 | — | 11.850 (ölçüm) |
| top 40.000 | 18.955 | 14.416 (ölçüm) |
| top 50.000 | 49.188 (tüm alpha) | 16.643 (ölçüm) |

Öneri: ızgara havuzu için **top 30.000 ile yaklaşık 11.800 kelime** (filtre sonrası). Bu, mevcut oyunun gereksinimlerini (2–7 harf, yaygın kelime) karşılar. İstenirse 40.000 ile ~14.400.

### Önerilen sıra

1. Kabul seti = `words.txt` ∪ Zemberek fiil kökleri ∪ Zemberek ile doğrulanan yüzey biçimleri (analiz zamanında, liste üretmeden).
2. Havuz = `tr_50k`/`tr_full` (ilk 30–40 bin) ∩ Zemberek (içerik, özel değil, Informal değil) − blocklist.
3. Kelime doğrulamasında "ast" gibi nadirler kabul setinde kalır; havuza girmezler (sıklık filtresi).

## 6. Riskler

- **Lisans:** FrequencyWords içerik lisansı (OpenSubtitles) ve Hunspell tr_TR orijinal lisansı net değil. Zemberek Apache-2.0 temiz. TDK türevi (`words.txt`) CC BY-SA 4.0 beyanı ve TDK veri hakkı ile belirsiz (SOURCES.md'de zaten not var).
- **Zemberek güvenilirliği:** Lexicon TDK madde başlarına dayanıyor; nadir sözcükleri (`ast`) ve argo (`am`) ayırt etmiyor. Sıklık ve blocklist katmanı şart.
- **Yabancı/özel isim:** Sözlükte 33.009 ProperNoun var; filtre atlanırsa havuz özel isimlerle dolar.
- **Çekim patlaması:** Zemberek ile biçim üretmek kabul setini çok büyütebilir; havuz yalnızca kökten üretilmeli.
- **Ağ:** Maven Central 429 verdi; GitHub arama API'si kapalı. Boru hattı deterministik olsun diye jar ve sözlük dosyaları sürümle sabitlenip SHA256 ile kaydedilmeli.
- **Derleme bağımlılığı:** Zemberek jar'ları ~8 MB (guava, protobuf dahil); repoya eklenmemeli, betik indirmeli. Şu an repo dışı `scratchpad` altında.
- **sozluk.gov.tr erişimi:** Hâlâ kapalı; TDK ile çapraz doğrulama yapılamıyor.

## 7. Yeniden üretim

Deneme dosyaları (repo dışı): `zemberek/demo/ZDemo.java`, `zemberek/demo/ZCheck.java`, `zemberek/demo/output.txt`, `zcheck_repo.tsv`, `zcheck_freq50k.tsv`, `hunspell/mini_unmunch.py`. Jar'lar: `zemberek/jars/`.
