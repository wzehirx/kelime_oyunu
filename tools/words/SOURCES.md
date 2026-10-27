# TDK Güncel Türkçe Sözlük - kelime listesi kaynakları

Amaç: bir kelimenin gerçek (TDK sözlüğünde yer alan) olup olmadığını doğrulamak. Anlam gerekmez. Hedef: en az 50.000 kelime.

Araştırma tarihi: 2026-10-25. Geçici dosyalar repoya konmadı: `/tmp/claude-0/-home-user-kelime-oyunu/8738d2b8-2244-5bc4-8edc-4bc5cda0eb10/scratchpad/words-research/`


## v1.4: kabul seti ve ızgara havuzu (Zemberek + sıklık listesi)

v1.3'e kadar `words.txt` yalnızca TDK madde başlarıydı. Gerçek oyuncu üç sorun bildirdi: fiil kökleri yok ("sat" kabul edilmiyordu, TDK mastarları `satmak` tutar), "ast" gibi nadir sözcükler ızgaraya giriyordu, "am" gibi argo kabul ediliyordu. v1.4'te liste iki katmana ayrıldı. Araştırma ve denemeler: [`RESEARCH_V2.md`](RESEARCH_V2.md).

| Dosya | Anlam | Boyut |
|---|---|---|
| `tdk_words.txt` | `fetch_words.py` çıktısı (TDK madde başları, eski `words.txt`), boru hattının girdisi | 58.074 (2-15 harf) |
| `app/src/main/assets/words.txt` | **Kabul seti**: oyuncunun yazdığı sözcüğün geçerli sayıldığı liste, 2-7 harf | **55.210** (2 harf 130, 3 harf 961, 4 harf 3.148, 5 harf 9.489, 6 harf 15.948, 7 harf 25.534) |
| `grid_pool.txt` | **Izgara havuzu**: bulmacaya girebilen sözcükler, 3-7 harf, sıklık sırasıyla (uygulamaya girmez) | **9.780** (3 harf 631, 4 harf 1.464, 5 harf 3.211, 6 harf 2.506, 7 harf 1.968) |
| `blocklist.txt` | Argo/küfür filtresi (92 sözcük) | 92 |
| `blocklist_extra_forms.txt` | Elle eklenen engelli türemiş/çekimli biçimler (+ ırkçı/aşağılayıcı birkaç TDK sözcüğü) | 22 |
| `pool_exclude.txt` | Kabul setinde kalan ama ızgaraya sorulmayan sözcükler ("ast" + cinsellik/anatomi/hakaret) | 29 |
| `accept_exclude.txt` | Kabul setinden çıkarılan kısaltma/yabancı sözcükler | 5 |

### Yeniden üretim

```
tools/words/build_wordlists.sh        # indir (önbelleğe), derle, çözümle, seç; words.txt ve grid_pool.txt yazar
python3 -m unittest discover -s tools/words
```

`build_wordlists.sh`: Zemberek jar'larını ve sıklık listesini `tools/words/.cache/` altına indirir (**repoya konmaz**, `.gitignore`'da), her dosyanın SHA-256'sını `tools/words/zemberek/SHA256SUMS` ile doğrular, `tools/words/zemberek/WordTool.java`'yı `javac` ile derler (Maven yok), Zemberek sözlüğünü ve aday sözcüklerin çözümlemelerini TSV'ye döker, sonra `build_wordlists.py` politikayı uygular. TLS doğrulaması kapatılmaz (ortam CA paketi kullanılır); Maven Central 429 verirse 20 sn bekleyip 6 kez dener. Tam çalışma ~15 sn (önbellek doluyken). Çıktılar deterministiktir; commit edilir.

Girdiler:
- **Zemberek NLP 0.17.1** (`zemberek-core`, `-morphology`, `-tokenization`, `-lm`; bağımlılıklar caffeine 2.6.2, protobuf-java 3.6.1, guava 28.2-jre, antlr4-runtime 4.7). Lisans **Apache-2.0** (ahmetaa/zemberek-nlp). Jar'lar GitHub `ahmetaa/maven-repo` ve Maven Central'dan indirilir.
- **FrequencyWords tr_full** (hermitdave/FrequencyWords, `content/2018/tr/tr_full.txt`, 2.035.629 satır `sözcük sayı`). Kod/depo lisansı **MIT** (Hermit Dave). **Dürüst not:** liste OpenSubtitles altyazılarından derlenmiştir; altyazı metinlerinin kendisinin hak durumu bu depo tarafından belirtilmemiştir ve doğrulanamadı. Depoya listenin **kendisi konmaz**; yalnızca ondan süzülmüş tekil sözcükler (`words.txt`, `grid_pool.txt`) girer. Sözcük listesi (olgusal veri) olarak düşük risklidir, ama ticari yayın öncesi hukuki kontrol önerilir.
- **TDK** madde başları (`tdk_words.txt`): bkz. aşağıdaki "Lisans durumu"; belirsizlik değişmedi.

### Kabul seti nasıl kurulur (`build_wordlists.py`, `select`)

Birleşim, sırayla (hepsi 2-7 harf, Türkçe alfabe, en az bir ünlü):
1. TDK madde başları (2-7 harf): 21.629 aday.
2. **Fiil kökleri**: TDK mastarlarından `-mak/-mek` atılarak (`satmak` → `sat`) ve Zemberek sözlüğündeki fiil maddelerinin `root` alanı (informal olmayan). 4.595 kök, 4.134'ü TDK madde başlarında yoktu (`sat`, `gel`, `yaz`, `yıkıl` ...).
3. **Çekimli/yaygın biçimler**: sıklık listesinde en az **20** geçişi olan, Zemberek'in geçerli saydığı ve en az bir çözümlemesi *özel isim / kısaltma / informal DEĞİL* ve türü isim, sıfat, zarf, fiil, zamir, bağlaç, ilgeç, belirteç, soru, sayı olan sözcükler (ünlem, tekrarlayıcı, noktalama hariç). 29.504 sözcük eklendi (`evler`, `kitapta`, `okullar` ...). Ünlüsüz diziler (`kg`, `tv`, `dvd`) kısaltma sayılıp alınmaz.
4. Argo filtresi çıkarılır (aşağıda); `accept_exclude.txt` çıkarılır.

**Eşik gerekliliği (örneklemeyle).** 10-15 geçişlik bandda rastgele 60 sözcüğün tamamı Zemberek'e göre geçerli çekimlerdi ama çoğu çok nadir (`omurum`, `kudurup`, `nonoşum`); 20+ bandında nadir ama geçerli çekimler ağırlıklıydı, bariz hata (yazım hatası vb.) görülmedi. Kabul seti için amaç oyuncuyu haksız yere reddetmemek olduğundan 20 seçildi; daha yükseği "evler/kitapta" benzeri yaygın çekimleri kaçırmaz ama daha çok geçerli sözcüğü reddederdi. Bilinen kusur: Zemberek bazı nadir/yazım hatası biçimleri de kabul eder; kabul setine girmesi zararsızdır.

### Argo filtresi

`blocklist.txt` = `blocklist_candidates.txt` (85 sözcük) + `blocklist_ambiguous.txt`'den **yalnızca** `seks, oral, anal, kerhane, travesti, gey, fuhuş`. Hafif hakaretler (aptal, salak, öküz, eşek ...) ve `meme, it, karı, kıç, döl, kaka` bilinçli olarak **kalır**. Uygulama:
- Sözcüğün kendisi listede ya da listedeki bir mastarın köküyse (`sikmek` → `sik`, `siktirmek` → `siktir`) engellidir.
- Zemberek çözümlemelerinden **hepsi** engelli bir lemma/köke gidiyorsa engellidir (`amlar`, `götü`, `sikti`, `boku`, `piçler`); temiz bir çözümlemesi de varsa engellenmez (`ama` bağlaçtır, `am`+yönelme değil). Bu, "amlar" gibi çekimli biçimleri yakalarken `ama`, `amca`, `sıçan` gibi temiz sözcükleri korur.
- Zemberek'in türetme çözümlemesi yüzünden kaçan biçimler (`zenciye` ← `zen+ci`, `kancığa`, `sikle`) elle `blocklist_extra_forms.txt`'e eklendi (denetim: kök içeren tüm sözcükler listelenip tek tek bakıldı).
- Sonuç: kabul setinde `am`, `sik`, `göt`, `bok`, `piç`, `çük`, `ibne`, `puşt`, `yarak` ve `blocklist.txt`'in tamamı **yoktur** (test: `tools/words/test_build_wordlists.py`, `WordQualityTest.kt`). TDK'dan 52, kökten 12, sıklık listesinden 295 sözcük engel nedeniyle düştü. Bilinen sınır: liste elle seçilmiş küçük bir listedir; kapsamlı bir hakaret sözlüğü değildir.

### Izgara havuzu nasıl kurulur

Kabul setinin alt kümesi, ve:
- sıklık listesinde en az **N** geçiş: **3 harf 50, 4 harf 20, 5-7 harf 10** (kısa sözcükler daha yaygın olmalı çünkü "ast" gibi nadir kısa kayıtlar çok fazla; uzun sözcük sayısı ise düzey sayısını belirler),
- **çekimsiz** içerik sözcüğü: Zemberek çözümlemesinde isim/sıfat/zarf için `lemma == sözcük`, fiil için `root == sözcük` (`kitap`, `sat`, `yat` evet; `evler`, `satar` hayır); özel isim, kısaltma, informal değil,
- **TDK kapısı**: TDK madde başı ya da TDK mastarının kökü (Zemberek'in bildiği ama TDK'da olmayan `charter`, `sticker` gibi yabancı yazımlar elenir; 500 sözcük),
- engelli listesi ve `pool_exclude.txt` (örneğin `ast`) çıkarılır.

**Eşik neden gevşetildi (dürüst).** Önce istenen yaklaşım denendi: sıklıkta ilk ~40.000 satır → 4.759 sözcük (3 harf 455, 4 harf 861, 5 harf 1.619, 6 harf 1.026, 7 harf 798). Bu havuz 10.000 seviyeyi karşılamaz: seviye başına bir harf kümesi (anagram sınıfı) gerekir; 6 ve 7 harfli kümeler yalnızca ~1.000 ve ~800 tane. Geçiş sayısı eşikleri ve sonuç (kısa sözcük eşiği olmadan): ≥100 → 6.956; ≥50 → 8.141; ≥30 → 8.940; ≥20 → 9.585; ≥10 → 10.578 (TDK kapısı öncesi). 10.000 seviye ancak 7 harfli ~2.000 ve 6 harfli ~2.500 harf kümesiyle, her küme en çok 3 kez kullanılarak çıkıyor; bu yüzden uzun sözcüklerde eşik 10'a kadar gevşetildi. Kısa sözcükler (oyuncunun en çok gördüğü) daha sıkı tutuldu (3 harf 50, 4 harf 20), bunun bedeli 5 harfli bandın uzamasıdır (README ve `tools/levels`'a bakın). Havuzun 3.088 sözcüğü (%32) 100'den az, 6.819'u (%70) 1.000'den az geçişlidir: yani havuz yaygın sözcükler ağırlıklı ama nadir TDK sözcükleri de içerir. "ast" (107 geçiş) yalnızca `pool_exclude.txt` ile dışarıda; benzer birkaç nadir sözcük kalmış olabilir.

`pool_exclude.txt`'te ayrıca cinsellik/anatomi/hakaret içeren (vagina, makat, tecavüz, pedofil, avrat ...) 28 sözcük ızgaraya sorulmaz ama kabul setinde kalır (oyuncu yazarsa geçersiz sayılmaz).

---

## 0. Nihai kaynak ve uretim (Task 3)

- **Kaynak:** `bilalozdemir/tr-word-list`, `files/words.json` (`https://raw.githubusercontent.com/bilalozdemir/tr-word-list/master/files/words.json`). README'ye gore icerik TDK sozlugunden cekilmistir.
- **Indirme tarihi:** 2026-10-25 (`tools/words/fetch_words.py` ile, proxy CA paketiyle, TLS dogrulamasi acik).
- **Ham dosya:** 9.396.926 bayt, MD5 `e78da352ff3d2efb207a4458574df700`, 92.406 kayit; her kayit `{"word", "meanings"}`. Ozel isim alani yok; tek isaret, ozel isimlerin buyuk harfle baslamasidir.
- **Cikti:** `app/src/main/assets/words.txt`: **58.074 benzersiz kelime**, UTF-8, LF, satir basina bir kelime, Turkce alfabe sirasinda (`abcçdefgğhıijklmnoöprsştuüvyz`).
- **Uretim:** `python3 tools/words/fetch_words.py` (yerel dosyadan: `--json yol`). Testler: `python3 -I tools/words/test_clean_words.py`.

Temizleme kurallari (`tools/words/clean_words.py`):

| Kural | Etki / gerekce |
|---|---|
| Turkce kucuk harf (I→ı, İ→i) | `IŞIK`→`ışık`; `ısı` ve `isi` ayri kelimeler |
| Yalnizca ALPHABET harfleri, uzunluk 2-15 | bosluk, tire, rakam, kesme isareti, q/w/x iceren her sey atilir (30.000'den fazla cok sozcuklu kayit: deyim, atasozu) |
| Ilk harfi buyuk, gerisi kucuk girisler ozel isim sayilir ve atilir (kucuk harfli bicimi ayrica varsa o alinir) | `Ankara`, `İzmir`, `Alevi`, `Cermen` gibi ~1.900 giris dusuyor. Sezgi kaba: `Alevi`, `Duma` gibi ortak ad sayilabilecek birkac yuz kelime de gidiyor; 58 bin kelimede kabul edilebilir bir kayip |
| Sapkali unluler duzlestirilir (â→a, î→i, û→u) | TDK `dükkân`, `bekâr`, `hâlâ` yazar; oyuncu `dukkan`, `bekar`, `hala` girer. ~760 kayit bu yuzden listeye giriyor. Yan etki: `kâr`/`kar` tek kelimede birlesir |
| En az bir unlu zorunlu | 2 harfli sembol/kisaltma kayitlarini (cm, mg, kr, br, cb, cf, md, pb... ) eler |

Kalite notlari:
- 2 harfli 102 kelime kaliyor (ab, aç, ad, ev, su, ... ). Hepsi unlu iceriyor; elle taranan listede kisaltma kalmadi. Bazilari Turkce alfabe harf adlari (`be`, `ce`, `çe`, `de`, `fe`, `ge`, `he`, `je`, `ke`, `le`, `me`, `ne`, `pe`, `re`, `se`, `şe`, `te`, `ve`, `ye`, `ze`) ve nota adlari (`do`, `re`, `mi`, `fa`, `si`, `la`); TDK'da madde oldugundan korundu. Uzunluk 2'den 1'e inmez; tek harfler atilir.
- Liste TDK maddelerinden olustugu icin cekimli bicimler (ornegin `kitapta`) buyuk olasilikla yoktur (v1.4 oncesi durum); yalnizca madde basi sozcukler vardir. Oyunda bu bilinen bir sinirlamadir.
- Kelimeler TDK sozlugundeki yazimla eslesir; sozlukte olmayan gunluk yazimlar (`isi` gibi) girmez.
- Ham veri guncel TDK ile birebir karsilastirilmadi (`sozluk.gov.tr` bu ortamdan TLS ile erisilemiyor, bkz. bolum 1).

### Lisans durumu (dürüst ozet)
- Kaynak deponun tek lisans beyani README'deki **CC BY-SA 4.0** baglantisidir. Depoda `LICENSE` dosyasi yok; beyan dogrulanabilir bir lisans metniyle desteklenmiyor.
- **Share-alike:** `words.txt` bu verinin turevi sayilmalidir; dagitirken CC BY-SA 4.0 ile (atif: bilalozdemir/tr-word-list) paylasilmasi gerekir. Uygulamanin geri kalan kodu bu sart nedeniyle otomatik olarak etkilenir mi (toplu eser mi turev mi) hukuki bir degerlendirme gerektirir; burada karar verilmedi.
- **TDK kullanim kosulu belirsiz:** verinin asil sahibi TDK; kelime listesinin yeniden dagitimina dair TDK'nin acik bir izni/kosulu bu calismada bulunamadi. Tek tek sozcukler telif konusu olmayabilir, ama derleme icin kesin bir iddiada bulunmuyoruz. Yayin (ozellikle ticari) oncesi hukuki kontrol onerilir.
- Bu sorunlar cozulemezse yedek yol: `sozluk.gov.tr` erisimi acildiginda listeyi TDK'dan dogrudan yeniden uretmek.

## 1. sozluk.gov.tr (birincil kaynak): bu ortamdan erişilemiyor

| Uç | Sonuç |
|---|---|
| `https://sozluk.gov.tr/` | TLS el sıkışması sırasında kesiliyor (curl 52, kod 000) |
| `https://sozluk.gov.tr/gts?ara=kitap` | Aynı şekilde kesiliyor |
| `https://sozluk.gov.tr/autocomplete.json` | Aynı şekilde kesiliyor |
| `http://sozluk.gov.tr/` | 301 (nginx/1.28.3, Ubuntu). Sunucu ayakta, HTTPS'e yönlendiriyor |
| `https://www.tdk.gov.tr/` | Aynı TLS kesilmesi (HTTP/2 PROTOCOL_ERROR) |
| `https://tdk.org.tr/` | Proxy `connect_rejected` (egress politikası) |

Proxy durumu (`/__agentproxy/status`): `sozluk.gov.tr:443` için `ws_closed_mid_exchange` (code 1006), 7-12 sn sonra; 517 B gönderildi, 39 B alındı. Tünel kuruluyor ama upstream TLS el sıkışmasını tamamlamadan kesiyor. Birden fazla denemede aynı sonuç. Bu proxy/ortam tarafında bir sorun olabilir; TLS doğrulamasını kapatmadan çözülmüyor.

## 2. GitHub adayları

Not: GitHub arama uçları (`gh search`, `api.github.com`) bu oturumda 403 veriyor (oturum yalnızca yapılandırılmış depolara bağlı). Ham dosya (`raw.githubusercontent.com`) erişimi çalışıyor. Aşağıdaki depolar MCP GitHub araması ile bulundu ve ham dosya üzerinden kontrol edildi.

| Kaynak | Kelime sayısı (yaklaşık) | Biçim | Lisans | TDK kaynaklı mı | Çok sözcüklü / özel isim | Durum |
|---|---|---|---|---|---|---|
| **bilalozdemir/tr-word-list**, `files/words.json` | 92.406 kayıt, 91.631 benzersiz madde (README "92407" diyor) | JSON dizi `[{"word": "...", "meanings": [...]}]`, 9,4 MB, UTF-8 | README'de CC BY-SA 4.0 bağlantısı var. Depoda LICENSE dosyası yok (404) | Evet. README, "Turkish Government's official online dictionary" (TDK) kaynağını ve kelimelerin sözlükten çekildiğini belirtiyor. Anlamlar da içeriyor | 30.293 çok sözcüklü kayıt (deyim, atasözü, "(veya ...)" içeren uzun satırlar); 2.784 büyük harfle başlayan; 83 kesme işaretli. Yalnızca tek sözcük (küçük harf, yalnızca harf): **59.087 benzersiz** | **Önerilen (1. sıra)** |
| **troopcat/tdk-guncel-sozluk-wordlist** | "92406 kelime/deyim/atasözü" (README) | Veri dosyasının adı bulunamadı (yaygın adlar ana dalda 404). Depo 455 KB | WTFPL (LICENSE dosyası var) | README sözluk.gov.tr'den çekildiğini söylüyor, ama kelimeleri bilal deposundan "ayıkladığını" da yazıyor. Yani bilal'in türevi | Bilinmiyor | Veri dosyası çözülemedi. Türev olduğu için ikinci tercih değil |
| **erogluegemen/TDK-Dataset** (HuggingFace `erogluegemen/TDK_Turkish_Words`; Kaggle `erogluegemen/tdk-turkish-words`) | HF etiketi 10K-100K. `tdk_word_data_all.csv` 77 MB, `tdk_word_meaning_data.csv` 5,3 MB | CSV | HF'de lisans etiketi yok. Veri seti "gated" (oturum açma gerekli); anonim indirme 401 | Evet, README TDK API'sinden çekildiğini söylüyor | Bilinmiyor (indirilemedi) | Erişilemiyor (kimlik doğrulama gerekli). Lisans belirsiz |
| **pr0mers/TDKDictionaryWordAndStatistics** | Bilinmiyor | Kendi scraper'ı, ham çıktı dosyası README'de yok | Lisans dosyası yok | Evet, sozluk.gov.tr'den çektiğini belirtiyor | Bilinmiyor | Ham veri yok. Script çalıştırılarak üretilebilir; lisans yok |
| **grandsir/Turkish-Wordlist** | Bilinmiyor | Bilinmiyor (README: "TDK'nin kelime veritabanından çıkartılmış kelime dosyaları") | Yok | README'ye göre TDK kaynaklı | Bilinmiyor | Ham dosya adı bulunamadı, kullanılamadı |
| csariyildiz/turkish-wordlist | 2.510.327 | Küçük harf, sıkıştırılmış | MIT | **Hayır**: Vikipedi metninden üretilmiş | Çok gürültü (yazım hatası, ad, yabancı sözcük) | Elendi (TDK değil) |
| vdemir/hunspell-tr | Hunspell kelime listesi | `.dic`/`.aff` | NOASSERTION | TDK olarak belirtilmemiş | - | Elendi |
| cgurkan/turkish-wordlist | Bilinmiyor | Scrabble için | GPL-3.0 | Belirtilmemiş | - | Elendi |
| xis/tdk, emreozcan/tdk-py, mertssmnoglu/TDK-ApiWrapper | Kelime listesi değil (API sarmalayıcı) | Python kodu | MIT | TDK sitesine istek atıyor | - | Liste sağlamıyor; sozluk.gov.tr erişilemediği için işe yaramaz |

## 3. Öneri

1. **bilalozdemir/tr-word-list, `files/words.json` (birincil öneri).** Tek sözcük filtresinden sonra 59.087 benzersiz kelime (hedef 50.000 üstü). Doğrudan indirilebiliyor, TDK kaynaklı olduğu README'de yazıyor ve README'de açık bir lisans bağlantısı var (CC BY-SA 4.0). Dikkat edilecekler:
   - Depoda LICENSE dosyası yok; tek lisans beyanı README'deki CC BY-SA 4.0 bağlantısı. Veri TDK'ya ait; TDK'nın kendi kullanım koşulları ayrıca geçerli olabilir. Ticari kullanım öncesi hukuki kontrol gerekir.
   - Share-alike (SA) şartı nedeniyle türetilmiş listenin de CC BY-SA ile paylaşılması gerekebilir.
   - Yalnızca tek sözcük kullanın; çok sözcüklü, büyük harfli ve parantezli girişler filtrelenmeli.
   - README "92407" diyor, dosyada 92.406 kayıt var; küçük fark.
2. **Yedek: sozluk.gov.tr'nin kendisi.** Proxy tarafındaki TLS kesilmesi giderildiğinde doğrudan TDK'dan doğrulama yapılabilir ve bilal listesi TDK ile karşılaştırılarak yeniden üretilebilir. Bu, lisans sorununu da çözer.

Elenenler: erogluegemen (HF gated, lisans belirsiz), troopcat (bilal'in türevi, veri dosyası bulunamadı), grandsir (ham dosya bulunamadı).

## 4. İndirme ve filtreleme

Ham dosyayı indirme (proxy CA ile):

```bash
mkdir -p /tmp/tdk-words && cd /tmp/tdk-words
curl -sS -L -o words.json \
  --cacert /root/.ccr/ca-bundle.crt \
  https://raw.githubusercontent.com/bilalozdemir/tr-word-list/master/files/words.json
```

Tek sözcük listesi üretme (Python, `-I` ile):

```bash
python3 -I - /tmp/tdk-words/words.json /tmp/tdk-words/tdk_single_words.txt <<'PYEOF'
import json, re, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))
pat = re.compile(r"^[a-zçğıöşüâîû]+$")
seen, out = set(), []
for e in data:
    w = e["word"]
    if pat.match(w) and w not in seen:
        seen.add(w)
        out.append(w)
open(sys.argv[2], "w", encoding="utf-8").write("\n".join(out) + "\n")
print(len(out))
PYEOF
```

Beklenen çıktı: `59087`. İndirilen `words.json` için MD5 `e78da352ff3d2efb207a4458574df700`, boyut 9.396.926 bayt.
