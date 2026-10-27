# Handoff Spec: Kelime Oyunu görsel yenileme (v2.0)

Kaynak: `2026-10-27-visual-design.md`. Teknoloji: Jetpack Compose, Material3 (yalnızca temel bileşenler), `minSdk 26`. Bu belge tahmin payı bırakmamak içindir; belirtilmeyen bir şey için en erişilebilir seçeneği uygula ve raporda yaz.

## Overview
Oyun görme engelliler için TalkBack ile oynanır; görsel katman, gören/az gören kullanıcı ve oyunu birlikte oynayanlar içindir. **Anlamsal ağaç (etiketler, sıra, odak) bu yenilemeyle DEĞİŞMEZ.** Görsel öğelerin bilgi taşıyanları anlamsal ağaçta zaten başka düğümle temsil edilir; yalnızca süs veya mükerrer olanlar `clearAndSetSemantics {}` ile gizlenir.

## Design Tokens
| Token | Açık (Deniz kâğıdı) | Koyu (Gece haritası) | Kullanım |
|---|---|---|---|
| `color-bg` | `#E8F0EE` | `#0F2229` | ekran zemini |
| `color-surface` | `#FAFCFB` | `#17313A` | panel, diyalog |
| `color-ink` | `#14232B` | `#E6F0EE` | ana metin |
| `color-primary` | `#1F4E79` | `#8DB8E8` | birincil düğme, seçili jeton, vurgu |
| `color-on-primary` | `#FFFFFF` | `#0F2229` | primary üstündeki metin |
| `color-line` | `#B9CBC8` | `#2C4A55` | ince ayraç, ızgara çizgisi |
| `color-tile` | `#F3EEDF` | `#F3EEDF` | harf jetonu zemini |
| `color-tile-ink` | `#14232B` | `#14232B` | jeton harfi |
| `color-success` | `#1F7A52` | `#5BCB93` | doğru, tamamlandı |
| `color-error` | `#B3323A` | `#FF8F94` | geçersiz, tehlike |
| `ink-avrupa/asya/ortadogu/afrika/amerika/okyanusya` | `#3F63D9 #D6406C #C97316 #1F8F5F #8A4FD3 #0F97AA` | her biri +%25 açık varyant | ülke damgası, ülke hapı |
Metin/zemin çiftleri ≥ 4.5:1, bileşen/grafik ≥ 3:1 (iki temada testle).
| Token | Değer |
|---|---|
| `space-xs/sm/md/lg/xl` | 4 / 8 / 16 / 24 / 32 dp |
| `radius-panel` | 14 dp; `radius-pill` 999 dp; `radius-token` 50% (daire); `radius-cell` 6 dp; satırlarda köşe 0 |
| `elevation-token` | 3 dp; `elevation-panel` 2 dp; çark diski 0 (2 dp halka çizgisi) |
| `touch-min` | 48 dp (jetonlar 56–64, birincil düğme 64) |
| Yazı (Atkinson Hyperlegible) | `display` 32sp Bold; `title` 28sp Bold; `section` 20sp Bold; `body` 18sp Regular; `label` 16sp Bold; `small` 14sp Regular; `tile` 30sp Bold; `stamp` 16sp Bold, büyük harf (yalnızca damga) |
Satır uzunluğu < 80 karakter; satır yüksekliği 1.3 (gövde 1.4).

## Components
| Bileşen | Varyant | Özellikler / notlar |
|---|---|---|
| `LetterToken` | default, pressed, selected, disabled | 64 dp daire, fildişi zemin, `tile` harf. selected: `color-primary` dolgu, `color-on-primary` harf, sağ üstte 20 dp sıra rozeti (yalnızca görsel; etiket zaten "Harf K, seçildi"). pressed: ölçek 0.94, 80 ms. disabled (seviye bitti): %40 opaklık, anlamsal olarak devre dışı |
| `LetterWheel` | 3–7 harf | Disk çapı `min(ekranGenişliği − 2·space-lg, 320 dp)`; ince ölçek çizgileri (36 tik, 1 dp, `color-line`); jetonlar halka üzerinde eşit aralıklı, ilk jeton saat 12 yönünden başlar (n harf için açı 360/n); merkezde 56 dp `ShuffleButton` (pusula ibresi simgesi). Odak sırası = havuz sırası (`tiles` sırası), tuval değil. |
| `CurrentWordPill` | boş, dolu | Min yükseklik 56 dp, `radius-pill`, seçilen harfler yanyana mini jeton (24 dp) değil düz `title` metni; boşken "Harf seç" ipucu metni (`small`, soluk, anlamsal olarak yok sayılır). Etiket mevcutla aynı. |
| `CrosswordGrid` | n×m, n,m ≤ 9 | Hücre = `min(36 dp, kullanılabilirGenişlik / sütun)`; boş (ızgara dışı) hücre çizilmez; kapalı hücre `color-line` çerçeveli boş; bulunan hücre fildişi + harf; ipucuyla açılan hücre primary %15 tonlu + harf. **Anlamsal olarak gizli** (`clearAndSetSemantics {}`): bilgi kelime listesinde. |
| `WordList` | satır = kelime | Mevcut satırlar ve etiketler aynen; görsel olarak kompakt (kutucuk dizisi küçük, 28 dp). |
| `ActionBar` | birincil, ikincil | Satır 1: Sil (ikincil), **Gönder (birincil, 64 dp yükseklik, tam genişliğin ~%45'i)**, Temizle (ikincil). Satır 2: "İpucu" ve "Kelime aç" (maliyet metin içinde, ör. "İpucu, 15 jeton"). Boş kelimeyle Gönder **etkin kalır** (duyuru "Önce harf seç"); stili soluk olabilir ama anlamsal devre dışı OLMAZ. |
| `StatPill` | jeton, ülke | `radius-pill`, `small`, simge + metin. Ülke hapı bölge mürekkebi çerçeve + ilerleme çizgisi altta (2 dp). |
| `HeroPanel` | ana menü "Devam et" | `radius-panel`, yüzey, ülke damgası (sol), "Seviye N", ülke adı ve ilerleme çubuğu, altta birincil düğme. Tek anlamsal düğüm (mevcut "Devam et, {ülke}, k / n" etiketi). |
| `ListRow` | menü satırı, ülke satırı | 64 dp min yükseklik, alt çizgi `color-line` 1 dp, soldaki simge/damga, başlık `section`, alt metin `small`; tek anlamsal düğüm. |
| `Stamp` | ülke baş harfleri | 56 dp daire, 2.5 dp bölge mürekkebi halka + iç ince halka, ortada `stamp` iki harf; hafif −4° dönüklük; **dekoratif** (gizli). Kilitli: gri halka, kilit simgesi. |
| `ProgressLine` | ülke / seviye | 4 dp yükseklik, `radius-pill`, `color-line` iz üstünde bölge/primary dolgu; dekoratif (bilgi başlıkta metinle). |
| `SettingRow` | anahtar | `ListRow` + `Switch`; anlamsal: "{ad}: açık/kapalı". |
| `DangerButton` | sıfırla | Anahtar satırlarından ayrı, `color-error` çerçeveli, onay diyaloğu |

## States and Interactions
| Öğe | Durum | Davranış |
|---|---|---|
| LetterToken | dokunma | ölçek 0.94 (80 ms) → seçili görünüm; ses + titreşim (mevcut olay hattı) |
| LetterToken | seçili tekrar dokunma | yok sayılır (motor); görsel değişmez |
| Gönder | doğru kelime | ızgara hücreleri sırayla belirir (60 ms arayla), CONFIRM titreşimi |
| Gönder | bonus | "+bonus" hapı yukarı süzülüp kaybolur (600 ms), CONFIRM |
| Gönder | geçersiz | çark 8 dp yatay sallanır (200 ms), REJECT; seçim korunur (mevcut davranış) |
| Karıştır | dokunma | jetonlar yeni konumlarına kayar (220 ms); seçili jetonlar yerinde kalır (mevcut davranış) |
| Seviye bitişi | — | konfeti ≤ 1.5 sn, ardından bitiş paneli; odak mantığı AYNEN (içerik bölümü) |
| Ülke bitişi | — | damga ekrana "basılır" (ölçek 1.6→1.0 + hafif zıplama, 450 ms) |
| Ülke satırı | kilitli | muted, "Kilitli" metin+kilit simgesi; tıklayınca "Henüz bu ülkeye ulaşmadın" (mevcut) |
| Ülke satırı | devam | primary çerçeve, "Şu an buradasın" metni |
| Ülke satırı | tamamlandı | onay simgesi + "Tamamlandı"; açılınca iki bilgi görünür |
| Düğmeler | odak (klavye/TalkBack) | 3 dp `color-primary` dış halka, 2 dp aralık |

## Responsive Behavior
| Genişlik | Değişiklik |
|---|---|
| < 360 dp | çark çapı = genişlik − 32 dp; ActionBar ikinci satır tek sütun |
| 360–600 dp | varsayılan düzen |
| ≥ 600 dp veya yatay | oyun ekranı iki sütun: solda ızgara + kelime listesi, sağda çark + eylemler; okuma sırası DEĞİŞMEZ (ızgara/liste → kelime → harfler → eylemler) |
| fontScale 2.0 | tüm ekranlar dikey kaydırılabilir; çark ve jetonlar sabit dp (metinle büyümez), etiketli metinler büyür; hiçbir metin kırpılmaz |

## Edge Cases
- Uzun ülke adı ("Demokratik Kongo Cumhuriyeti", "Amerika Birleşik Devletleri"): 2 satıra sarar, hapta `maxLines = 1` + sonuna "…" (tam ad anlamsal etikette zaten var).
- 3 harf: jetonlar 120° aralık; 7 harf: ~51° aralık, jeton 56 dp'ye iner.
- Izgara 9×9 ve dar ekran: hücre 28 dp altına inmez; gerekirse yatay kaydırma YOK, bunun yerine ızgara tuvali ölçeklenir.
- Yükleme: seviye/sözlük yüklenirken yazı + `CircularProgressIndicator` (etiket "Yükleniyor"); hata: açıklama + "Yeniden dene" düğmesi.
- Boş durumlar: Dünya turu verisi yoksa menüde Dünya turu satırı yok (mevcut davranış).
- Hareket azaltma / animasyon kapalı: tüm geçişler anlık, konfeti ve sallanma yok.
- Tema: sistem teması; ayarlarda ayrı seçici yok (YAGNI).

## Animation / Motion
| Öğe | Tetik | Animasyon | Süre | Easing |
|---|---|---|---|---|
| LetterToken | basma | ölçek 1→0.94→1 | 80 + 120 ms | FastOutSlowIn |
| Izgara hücresi | doğru kelime | opaklık 0→1 + ölçek 0.8→1, sırayla | 160 ms / 60 ms aralık | FastOutSlowIn |
| Çark | geçersiz | yatay titreme ±8 dp, 3 salınım | 200 ms | Linear |
| Karıştır | dokunma | konum geçişi | 220 ms | FastOutSlowIn |
| Konfeti | seviye bitişi | 40 parça Canvas, yerçekimi | 1500 ms | doğrusal düşüş |
| Damga | ülke bitişi | ölçek 1.6→1.0, hafif zıplama | 450 ms | spring (dampingRatio 0.55) |
| Ekran geçişi | gezinme | yok (anlık) | — | — |
Süreler `LocalReduceMotion` benzeri tek bir durum nesnesinden okunur (sistem ölçeği 0 ya da ayar kapalı ⇒ 0 ms).

## Haptics
Harf seçimi: `KEYBOARD_TAP`; doğru/bonus: `CONFIRM` (API 30+; altında `KEYBOARD_TAP`); geçersiz/yetersiz jeton/ipucu yok: `REJECT` (API 30+; altında `LONG_PRESS` yerine hafif `KEYBOARD_TAP` iki kez); seviye bitişi: `CONFIRM`. `View.performHapticFeedback`, izin yok. Ayar: "Titreşim" (varsayılan açık).

## Accessibility Notes
- **Odak/okuma sırası (oyun):** başlık düğümü → kelime listesi satırları → oluşturulan kelime → harf düğmeleri (havuz sırası) → Sil/Gönder/Temizle → Karıştır → İpucu → Kelime aç. (Mevcut sırayı koru; Karıştır'ın çark ortasında görsel olması sıra için fark etmez.)
- Görsel-yalnız öğeler (ızgara tuvali, damga, ilerleme çizgisi, konfeti, ölçek çizgileri, sıra rozeti): `clearAndSetSemantics {}` ya da `invisibleToUser`.
- Tüm tıklanabilirlerde Türkçe etiket (mevcut testler `assertNoUnlabeledClickables` ile korur); durum metni (Kilitli/Tamamlandı/Devam) renk dışında metin + simgeyle.
- Duyurular ve odak hedefleri değişmez; animasyonlar duyuru zamanlamasını geciktirmez.
- Kontrast: iki temada ThemeContrastTest (metin ≥ 4.5:1, bileşen ≥ 3:1) bölge mürekkepleri dahil.
- Dokunma hedefi ≥ 48 dp; jetonlar arası boşluk ≥ 8 dp.
- Yazı tipi dosyaları OFL; `assets/licenses/` ve README'de lisans.
