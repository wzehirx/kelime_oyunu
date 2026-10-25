# TDK Güncel Türkçe Sözlük - kelime listesi kaynakları

Amaç: bir kelimenin gerçek (TDK sözlüğünde yer alan) olup olmadığını doğrulamak. Anlam gerekmez. Hedef: en az 50.000 kelime.

Araştırma tarihi: 2026-10-25. Geçici dosyalar repoya konmadı: `/tmp/claude-0/-home-user-kelime-oyunu/8738d2b8-2244-5bc4-8edc-4bc5cda0eb10/scratchpad/words-research/`

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
