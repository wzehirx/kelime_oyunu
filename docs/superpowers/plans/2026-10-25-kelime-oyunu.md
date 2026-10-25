# Kelime Oyunu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** TalkBack ile tam erişilebilir, Words of Wonders tarzı, 10.000+ seviyeli, çevrimdışı Android kelime oyunu (v1.0).

**Architecture:** Native Kotlin + Jetpack Compose, MVVM. Saf Kotlin `domain` (motor, sözlük), `assets` içinde önceden üretilmiş sözlük/seviye/ses verisi, `a11y` katmanında tek merkezli duyuru yönetimi. Veri ve seviyeler Python araçlarıyla geliştirme zamanında üretilir.

**Tech Stack:** Kotlin 2.x, Compose, AGP 8.9+, Gradle 8.14 (wrapper), DataStore, SoundPool, JUnit4, Robolectric + Compose UI test, Python 3 (araçlar).

**Spec:** `docs/superpowers/specs/2026-10-25-kelime-oyunu-design.md`

## Global Constraints
- Paket: `com.kelimeoyunu`. `minSdk 26`, `targetSdk/compileSdk 35`. Tek aktivite, Compose.
- Dil: Türkçe arayüz ve duyurular. Harfler: `abcçdefgğhıijklmnoöprsştuüvyz` (29). Küçük harfe `Locale("tr")` ile çevrilir (İ→i, I→ı). Ekranda büyük harf gösterilir.
- Tüm etkileşimli öğeler ≥ 48dp, Türkçe `contentDescription`/semantics, bilgi yalnızca renkle verilmez.
- Çevrimdışı çalışır, ağ izni yok.
- Seviye: 3–7 harf; v1.0'da **en az 10.000** seviye, hepsi doğrulanmış.
- Ekonomi: başlangıç 50 jeton; seviye bitişi +10; her 5 bonus kelime +20; harf ipucu 10; kelime aç 30.
- Anahtar dosyası ve şifresi **depoya girmez** (`/home/user/secrets/` , `.gitignore`'da `*.jks`, `*.keystore`).
- Commit mesajları şu iki satırla biter: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` ve `Claude-Session: https://claude.ai/code/session_015T6AezsmCqgfU4mWJYyaJ1`. Geliştirme dalı: `ccr-b0baec48-v0gwgl`. PR açılmaz.
- Lisansı belirsiz ses/veri kullanılmaz; her kaynak `SOURCES.md` / `SOUNDS.md` içinde belgelenir.

## Review Focus
1. Türkçe İ/ı: `"IŞIK"`→`"ışık"`, `"İZMİR"`→`"izmir"`; `ısı` ve `isi` farklı kelimeler (Task 2 testi).
2. Tekrarlı harf: havuzda tek `A` varken `ALA` kabul edilmemeli (Task 2 testi).
3. Boş gönderim, hızlı çift dokunuş, aynı kelimeyi iki kez gönderme (Task 5 ve 6 testi).
4. Uygulama ortasında kapanıp açılma: seviye, bulunan kelimeler, jeton korunur (Task 5 testi).
5. Son seviyeden sonrası (10.000. seviye bitince) ve bozuk/eksik asset: çökmeden "tebrikler"/hata mesajı (Task 4, 5 testi).
6. TalkBack duyuru çakışması: arka arkaya hızlı harf seçiminde duyuruların kaybolmaması/sıralanması (Task 6 testi).

---

## Çalışma düzeni (tüm görevler)
Her görev: **Sonnet-Uygulayıcı** yazar (TDD) → **ayrı Sonnet-Test** ajanı bağımsız doğrular (testleri çalıştırır, kenar durumlar ekler, kodu okur) → geçmezse bir kez Sonnet-Uygulayıcı düzeltir → hâlâ geçmezse **Opus** düzeltir. Araştırma/okuma işleri **Haiku**. Ajanlar commit atmaz; commit/push/tag koordinatörde (bu oturum). Her görev sonunda push.

Sürüm kapıları: v0.1 (Task 0–2), v0.2 (3–4), v0.3 (5–6), v0.4 (7), v0.5 (8), v1.0 (9). Her sürümde debug APK üretilip yayınlanır.

### Task 0: Araç zinciri ve iskelet
**Files:** Create `tools/setup-android-sdk.sh`, `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/kelimeoyunu/MainActivity.kt`, gradle wrapper, `.gitignore`.
**Interfaces:** Produces: `./gradlew :app:assembleDebug` ve `:app:testDebugUnitTest` çalışır.
- [ ] Android cmdline-tools'u `dl.google.com` üzerinden kur (platform 35, build-tools 35.0.0), `ANDROID_HOME` ayarla, lisansları kabul et.
- [ ] Gradle 8.14.3 ile wrapper üret, AGP/Kotlin/Compose sürümlerini uyumlu seç.
- [ ] "Merhaba" gösteren `MainActivity` ve tek bir geçen birim test; `assembleDebug` başarılı olmalı. Beklenen: `BUILD SUCCESSFUL`, `app-debug.apk` var.
- [ ] Commit.

### Task 1: Türkçe metin yardımcıları ve sözlük
**Files:** `app/src/main/java/com/kelimeoyunu/domain/TurkishText.kt`, `WordDictionary.kt`; test `app/src/test/java/com/kelimeoyunu/domain/TurkishTextTest.kt`, `WordDictionaryTest.kt`.
**Interfaces:** Produces: `object TurkishText { fun lower(s: String): String; fun upper(s: String): String; val ALPHABET: String; fun canSpell(word: String, letters: List<Char>): Boolean }` · `class WordDictionary(words: Set<String>) { fun contains(word: String): Boolean; fun spellable(letters: List<Char>, min: Int = 2): Set<String> }` · `WordDictionary.fromLines(lines: Sequence<String>)`.
- [ ] Testler: `lower("IŞIK")=="ışık"`, `lower("İZMİR")=="izmir"`, `canSpell("ala", listOf('a','l'))==false`, `canSpell("ala", listOf('a','l','a'))==true`, `contains("kalem")` büyük/küçük harf duyarsız.
- [ ] Gerçekle, testleri geçir, commit.

### Task 2: Seviye modeli ve oyun motoru
**Files:** `domain/Level.kt`, `domain/GameEngine.kt`; test `GameEngineTest.kt`.
**Interfaces:** Consumes: `WordDictionary`, `TurkishText`. Produces:
`data class PlacedWord(val word: String, val row: Int, val col: Int, val horizontal: Boolean)` · `data class Level(val id: Int, val letters: List<Char>, val words: List<PlacedWord>)` · `sealed interface SubmitResult { Correct(word), Bonus(word), AlreadyFound(word), Invalid(word), Empty }` · `class GameEngine(level: Level, dictionary: WordDictionary, found: Set<String> = emptySet(), bonusFound: Set<String> = emptySet(), revealed: Set<Pair<Int,Int>> = emptySet()) { fun submit(raw: String): SubmitResult; fun isComplete(): Boolean; fun cellState(row: Int, col: Int): CellState; fun revealLetter(): Pair<Int,Int>?; fun revealWord(): PlacedWord?; val remainingWords: Int; val bonusCount: Int; fun snapshot(): EngineState }`.
- [ ] Testler: ızgara kelimesi→`Correct`; sözlükte olup ızgarada olmayan→`Bonus` (bir kez), ikinci→`AlreadyFound`; olmayan→`Invalid`; `""`→`Empty`; tüm kelimeler bulununca `isComplete()`; `revealLetter()` kapalı bir kutuyu açar, hepsi açıksa `null`; kesişen kutu bir kelimeyle dolunca diğerinde de görünür.
- [ ] Gerçekle, geçir, commit. **v0.1 etiketi + debug APK** (Task 11 yayın yöntemiyle).

### Task 3: Kelime listesi (Haiku araştırma → Sonnet araç)
**Files:** `tools/words/fetch_words.py`, `tools/words/clean_words.py`, `tools/words/test_clean_words.py`, `app/src/main/assets/words.txt`, `tools/words/SOURCES.md`.
**Interfaces:** Produces: `words.txt` (UTF-8, satır başına bir küçük harfli Türkçe kelime, sıralı, benzersiz) ve `clean_words.clean(lines: Iterable[str]) -> list[str]`.
- [ ] **Haiku:** TDK kaynaklı, lisansı belgelenebilir en iyi kelime listesi kaynaklarını bul (önce `sozluk.gov.tr` API erişimi dene; erişilemezse GitHub/açık veri setleri). Sonucu `tools/words/SOURCES.md` taslağına yaz.
- [ ] Test: `clean(["Ağaç","ağaç","at  yarışı","Ankara","a-b","İzmir","kitap"])` çıktısı yalnızca `["ağaç","kitap"]` (çok sözcüklü, tireli, harf dışı karakterli satırlar atılır; özel isim sezgisi uygulanıyorsa yalnızca kaynak işaretliyorsa). Uzunluk 2–15, yalnızca ALPHABET harfleri.
- [ ] Aracı yaz, listeyi üret (hedef ≥ 50.000 kelime), sayıyı ve kaynağı SOURCES.md'ye işle. Beklenen: dosya var, `kalem`, `ışık`, `çiçek` içeriyor, `xyz` içermiyor.
- [ ] Commit.

### Task 4: Seviye üretici, doğrulayıcı ve yükleyici
**Files:** `tools/levels/generate_levels.py`, `validate_levels.py`, `test_generate_levels.py`, `app/src/main/assets/levels.txt.gz`; `app/src/main/java/com/kelimeoyunu/data/LevelRepository.kt`; test `LevelRepositoryTest.kt`.
**Interfaces:** Format: satır başına bir seviye, `id|HARFLER|kelime,satır,sütun,y/d;kelime,satır,sütun,y/d;...` (`y`=yatay, `d`=dikey), gzip. Produces: `generate(words, count, seed) -> list[Level]`, `validate(level, words) -> list[str]` (hata listesi, boş = geçerli). Kotlin: `class LevelRepository(open: () -> InputStream) { val count: Int; fun get(index: Int): Level? }`.
- [ ] Test: üretilen her seviyede tüm kelimeler `canSpell` ile havuzdan yazılabilir, yerleşimde kesişen kutular aynı harfi taşır, hiçbir iki kelime çakışıp bitişik kutuları yanlış harfe zorlamaz, seviye başına 3–7 harf, en az 2 kelime.
- [ ] Zorluk eğrisi (v1.2'de güncellendi): harf sayısı seviye numarasıyla hiç düşmez (önce 3, sonra 4, 5, 6, 7 harfliler), kelime sayısı da küresel olarak düşmez (2'den 16'ya). `seed` ile yeniden üretilebilir.
- [ ] 10.000 seviye üret, `validate` ile hepsi geçmeli, benzersiz (harf kümesi + kelime kümesi) olmalı, `levels.txt.gz` boyutu < 6 MB.
- [ ] Kotlin: `LevelRepository.get(10000)` → `null` (aralık dışı), bozuk satır atlanır ve sayılmaz. Testle. Commit. **v0.2 etiketi + debug APK**.

### Task 5: İlerleme deposu ve oyun ViewModel'i
**Files:** `data/ProgressRepository.kt`, `ui/game/GameViewModel.kt`, `ui/game/GameUiState.kt`; testler `ProgressRepositoryTest.kt`, `GameViewModelTest.kt`.
**Interfaces:** Consumes: `LevelRepository`, `GameEngine`, `WordDictionary`. Produces: `data class Progress(val levelIndex: Int, val coins: Int, val found: Set<String>, val bonusFound: Set<String>, val revealed: Set<Pair<Int,Int>>, val bonusMeter: Int)` · `class ProgressRepository(dataStore) { val progress: Flow<Progress>; suspend fun save(p: Progress) }` · `class GameViewModel { val state: StateFlow<GameUiState>; fun onLetter(index: Int); fun onDelete(); fun onClear(); fun onSubmit(); fun onShuffle(); fun onHintLetter(); fun onHintWord(); fun onNextLevel(); val events: SharedFlow<GameEvent> }` · `sealed interface GameEvent` (letter/duyuru/ses için: `LetterAdded(char,index)`, `Deleted`, `Cleared`, `Submitted(SubmitResult)`, `Shuffled`, `HintUsed`, `NotEnoughCoins`, `CoinsEarned(n)`, `BonusMeterFull`, `LevelCompleted`, `AllLevelsDone`).
- [ ] Testler: aynı harf düğmesi iki kez seçilemez (index bazlı), hızlı çift `onSubmit` tek sonuç üretir, yetersiz jetonla ipucu `NotEnoughCoins`, kapanıp açılınca `Progress` geri yüklenir, son seviye sonrası `AllLevelsDone` (çökmez), seviye bitince +10 jeton, 5. bonus kelimede +20.
- [ ] Gerçekle, geçir, commit.

### Task 6: Arayüz ve erişilebilirlik
**Files:** `a11y/Announcements.kt`, `a11y/Announcer.kt`, `ui/game/GameScreen.kt`, `ui/menu/MainMenuScreen.kt`, `ui/level/LevelCompleteScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/theme/Theme.kt`, `MainActivity.kt` (navigasyon); testler `AnnouncementsTest.kt`, `GameScreenA11yTest.kt`.
**Interfaces:** Consumes: `GameViewModel` olayları. Produces: `object Announcements { fun gridSummary(level: Level, state: EngineState): List<String>; fun result(r: SubmitResult): String; fun letterAdded(c: Char): String; ... }` (saf, test edilebilir) · `class Announcer(view: View) { fun say(text: String, urgent: Boolean = false) }` (sıralı kuyruk, `announceForAccessibility`/live region).
- [ ] Testler (`AnnouncementsTest`): `gridSummary` → `"Kelime 2, 5 harfli, açık harfler: _ A _ E _"`; `result(Correct("kalem"))` → `"Doğru, KALEM"`; `result(Bonus(..))` → `"Bonus kelime, ..."`; `result(AlreadyFound(..))` → `"Bu kelime zaten bulundu"`; `result(Invalid(..))` → `"Geçersiz kelime"`; `result(Empty)` → `"Önce harf seç"`.
- [ ] Compose testleri: her harf düğmesi `"Harf A"` etiketli, tıklanabilir, ≥ 48dp; eylem düğmeleri sırasıyla Gönder, Sil, Temizle, Karıştır, İpucu; "oluşturulan kelime" alanı live region; ızgara kelime listesi olarak sunulur (`Role`/`stateDescription`).
- [ ] Hızlı arka arkaya 5 harf seçiminde duyurular kaybolmadan sıraya girer (Announcer testi).
- [ ] Ana menü: Devam et / Seviye N, Ayarlar (ses, titreşim, konuşma hızı bildirimi yok, TalkBack'e bırakılır). Seviye bitiş ekranı sonuç ve jeton duyurur. Commit. **v0.3 etiketi + debug APK**.

### Task 7: Ses efektleri veri seti ve SoundManager
**Files:** `tools/sounds/research.md` (Haiku çıktısı), `tools/sounds/build_sounds.py`, `app/src/main/assets/sounds/*.ogg|wav`, `app/src/main/assets/sounds/SOUNDS.md`, `audio/Sfx.kt`, `audio/SoundManager.kt`; testler `tools/sounds/test_sounds.py`, `SfxMappingTest.kt`.
**Interfaces:** Produces: `enum class Sfx { LETTER, DELETE, CLEAR, CORRECT, BONUS, DUPLICATE, INVALID, SHUFFLE, HINT, COIN, BONUS_FULL, LEVEL_COMPLETE, NAV, ERROR }` · `class SoundManager(context) { fun play(sfx: Sfx, letterIndex: Int = 0); fun release() }` (LETTER perdesi `letterIndex`'e göre 0.8–1.6 arası; aynı anda en fazla 4 ses).
- [ ] **Haiku:** CC0/serbest lisanslı kaynakları (Kenney, OpenGameArt vb.; Freesound erişilemez) tara; hangi paketlerin ne sağladığını ve lisansını `research.md`'ye yaz.
- [ ] Test (`test_sounds.py`): her `Sfx` için dosya var, mono, 44.1 kHz, süre 0.05–2.5 sn, tepe seviyesi −1 dBFS'i aşmıyor, ortalama ses seviyesi hedef aralıkta (tutarlılık), `SOUNDS.md` her dosya için kaynak+lisans içeriyor.
- [ ] `build_sounds.py`: uygun CC0 sesleri indirip normalize eder, eksik olayları numpy ile prosedürel sentezler (sinüs/üçgen dalga + zarf, harmonik; doğru kelime yükselen, geçersiz alçalan, seviye bitişi arpej). Ayırt edilebilirlik: her olay farklı perde/ritim imzası.
- [ ] `SfxMappingTest`: `GameEvent` → `Sfx` eşlemesi eksiksiz. `GameScreen` olaylarını ses ve duyuruya bağla. Commit. **v0.4 etiketi + debug APK**.

### Task 8: Bütünleşik test ve cila
**Files:** `app/src/androidTest` yerine `app/src/test/.../FullLevelFlowTest.kt`, hata düzeltmeleri, `README.md` taslağı.
- [ ] Uçtan uca (Robolectric): menüden başla, harflere dokunarak bir seviyenin tüm kelimelerini gönder, seviye bitişi ve ilerleme kaydını doğrula; 10.000 seviyenin ilk/son/rastgele 200 tanesi motorda çözülebilir.
- [ ] Review Focus 1–6'nın hepsinin testi mevcut ve yeşil. Eksik kalan varsa ekle.
- [ ] `./gradlew test assembleDebug` temiz; lint kritik hata yok. Commit. **v0.5 etiketi + debug APK**.

### Task 9: Yayın, imza ve README (v1.0)
**Files:** `README.md`, `RELEASES.md`, `app/build.gradle.kts` (signingConfigs: debug ve release; release anahtarı ortam değişkeni/yerel dosyadan, depoda yok), `.github/workflows/build.yml` (isteğe bağlı CI).
- [ ] `keytool` ile oturuma özel anahtar üret: `/home/user/secrets/kelime-oyunu-release.jks` (depo dışı, `.gitignore`'da desen). Şifre yalnızca oturumda.
- [ ] v1.0: `assembleDebug` (debug imzalı) ve `assembleRelease` (özel anahtarla imzalı, minify açık, `apksigner verify` ile doğrulanmış). SHA-256 parmak izi README'ye.
- [ ] Yayın yöntemi: önce `gh release create vX --prerelease` ile APK yükle; olmazsa APK'ları depoda `apk/` klasörüne `kelime-oyunu-vX-debug.apk` (v1.0'da `-release.apk` de) olarak commit et ve tag'le. Hangi yöntemin kullanıldığı README'de yazar.
- [ ] README: oyun tanımı, TalkBack kullanım kılavuzu, kurulum, sürüm tablosu (v0.1–v1.0 + APK bağlantıları), seviye/ses/kelime kaynak ve lisansları, anahtarın gizli tutulduğu notu, geliştirme/derleme talimatı.
- [ ] Anahtar dosyasını kullanıcıya `SendUserFile` ile teslim et. Son push, **v1.0** etiketi.
