# TDK Güncel Türkçe Sözlük - kelime listesi kaynakları

Amaç: bir kelimenin gerçek (TDK sözlüğünde yer alan) olup olmadığını doğrulamak. Anlam gerekmez. Hedef: en az 50.000 kelime.

Araştırma tarihi: 2026-10-25. Geçici dosyalar repoya konmadı: `/tmp/claude-0/-home-user-kelime-oyunu/8738d2b8-2244-5bc4-8edc-4bc5cda0eb10/scratchpad/words-research/`


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
- Liste TDK maddelerinden olustugu icin cekimli bicimler (ornegin `kitaplar`) buyuk olasilikla yoktur; yalnizca madde basi sozcukler vardir. Oyunda bu bilinen bir sinirlamadir.
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
