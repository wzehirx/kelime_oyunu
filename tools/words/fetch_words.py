"""TDK kaynakli kelime listesini indirir, temizler, words.txt yazar.

Kaynak: https://github.com/bilalozdemir/tr-word-list (files/words.json)
Kullanim: python3 tools/words/fetch_words.py [--json yerel.json] [--out yol]

HTTPS icin ortamdaki proxy ve CA paketi (HTTPS_PROXY, SSL_CERT_FILE /
REQUESTS_CA_BUNDLE / CURL_CA_BUNDLE) kullanilir; TLS dogrulamasi kapatilmaz.
"""
import argparse
import hashlib
import json
import os
import ssl
import sys
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from clean_words import clean  # noqa: E402

URL = "https://raw.githubusercontent.com/bilalozdemir/tr-word-list/master/files/words.json"
REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
DEFAULT_OUT = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "words.txt")
MIN_EXPECTED = 50_000


def download(url: str = URL) -> bytes:
    ctx = ssl.create_default_context()
    for var in ("SSL_CERT_FILE", "REQUESTS_CA_BUNDLE", "CURL_CA_BUNDLE"):
        path = os.environ.get(var)
        if path and os.path.exists(path):
            ctx.load_verify_locations(path)
            break
    opener = urllib.request.build_opener(urllib.request.HTTPSHandler(context=ctx))
    with opener.open(url, timeout=120) as resp:
        return resp.read()


def main(argv=None) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--json", help="indirmek yerine yerel words.json kullan")
    ap.add_argument("--out", default=DEFAULT_OUT)
    args = ap.parse_args(argv)

    if args.json:
        with open(args.json, "rb") as f:
            raw = f.read()
    else:
        raw = download()
    print(f"kaynak: {len(raw)} bayt, md5 {hashlib.md5(raw).hexdigest()}")
    entries = json.loads(raw.decode("utf-8"))
    words = clean(e["word"] for e in entries)

    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(words) + "\n")
    print(f"ham kayit: {len(entries)}, yazilan kelime: {len(words)} -> {args.out}")
    if len(words) < MIN_EXPECTED:
        print(f"UYARI: {MIN_EXPECTED} altinda", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
