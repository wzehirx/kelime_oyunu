#!/usr/bin/env bash
# Kelime listelerini (kabul seti ve ızgara havuzu) yeniden üretir.
#
#   tools/words/build_wordlists.sh
#
# Girdiler (repoda):  tools/words/tdk_words.txt (fetch_words.py çıktısı, TDK madde başları),
#                     tools/words/blocklist*.txt
# Girdiler (indirilir, repoya KONMAZ): Zemberek 0.17.1 jar'ları ve FrequencyWords tr_full.txt
#   -> önbellek dizini: $WORDS_CACHE (varsayılan tools/words/.cache, .gitignore'da)
# Çıktılar (commit edilir): app/src/main/assets/words.txt, tools/words/grid_pool.txt
#
# TLS doğrulaması ASLA kapatılmaz; ortamdaki proxy CA paketi (SSL_CERT_FILE / CURL_CA_BUNDLE)
# kullanılır. Maven Central 429 verirse bekleyip yeniden denenir. İndirilen her dosyanın SHA-256'sı
# tools/words/zemberek/SHA256SUMS ile karşılaştırılır; uyuşmazlık hatadır.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CACHE="${WORDS_CACHE:-$HERE/.cache}"
JARS="$CACHE/jars"
CLASSES="$CACHE/classes"
ZVER=0.17.1
CA="${CURL_CA_BUNDLE:-${SSL_CERT_FILE:-/root/.ccr/ca-bundle.crt}}"
CURL=(curl -sS -L --fail --retry 0)
[ -f "$CA" ] && CURL+=(--cacert "$CA")

mkdir -p "$JARS" "$CLASSES"

expected_sha() { awk -v f="$1" '$2 == f { print $1 }' "$HERE/zemberek/SHA256SUMS"; }

fetch() { # fetch <url> <hedef dosya adı> <hedef dizin>
  local url="$1" name="$2" dir="$3" want got n
  want="$(expected_sha "$name")"
  [ -n "$want" ] || { echo "SHA256SUMS içinde $name yok" >&2; exit 1; }
  if [ -f "$dir/$name" ] && [ "$(sha256sum "$dir/$name" | cut -d' ' -f1)" = "$want" ]; then return 0; fi
  for n in 1 2 3 4 5 6; do
    if "${CURL[@]}" -o "$dir/$name.part" "$url"; then
      got="$(sha256sum "$dir/$name.part" | cut -d' ' -f1)"
      if [ "$got" != "$want" ]; then
        echo "SHA-256 uyuşmuyor: $name (beklenen $want, bulunan $got)" >&2; exit 1
      fi
      mv "$dir/$name.part" "$dir/$name"; return 0
    fi
    echo "indirme başarısız ($n/6), 20 sn sonra yeniden: $url" >&2; sleep 20
  done
  echo "indirilemedi: $url" >&2; exit 1
}

ZBASE="https://raw.githubusercontent.com/ahmetaa/maven-repo/master/zemberek-nlp"
for art in zemberek-core zemberek-morphology zemberek-tokenization zemberek-lm; do
  fetch "$ZBASE/$art/$ZVER/$art-$ZVER.jar" "$art-$ZVER.jar" "$JARS"
done
MC="https://repo1.maven.org/maven2"
fetch "$MC/com/github/ben-manes/caffeine/caffeine/2.6.2/caffeine-2.6.2.jar" caffeine-2.6.2.jar "$JARS"
fetch "$MC/com/google/protobuf/protobuf-java/3.6.1/protobuf-java-3.6.1.jar" protobuf-java-3.6.1.jar "$JARS"
fetch "$MC/com/google/guava/guava/28.2-jre/guava-28.2-jre.jar" guava-28.2-jre.jar "$JARS"
fetch "$MC/org/antlr/antlr4-runtime/4.7/antlr4-runtime-4.7.jar" antlr4-runtime-4.7.jar "$JARS"
fetch "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/tr/tr_full.txt" tr_full.txt "$CACHE"

CP="$(ls "$JARS"/*.jar | tr '\n' ':')"
javac -encoding UTF-8 -cp "$CP" -d "$CLASSES" "$HERE"/zemberek/*.java

java -cp "$CLASSES:$CP" WordTool lexicon "$CACHE/lexicon.tsv" >/dev/null
python3 "$HERE/build_wordlists.py" prepare --cache "$CACHE"
java -cp "$CLASSES:$CP" WordTool analyze "$CACHE/candidates.txt" "$CACHE/analysis.tsv" >/dev/null
python3 "$HERE/build_wordlists.py" select --cache "$CACHE"
