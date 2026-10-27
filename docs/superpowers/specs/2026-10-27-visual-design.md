# Görsel Tasarım Belirtimi (v2.0)

Durum: kullanıcı isteğiyle başlatıldı ("görsellik sıfır, daha güzel bir menü, daha şık tasarım, daha oynanabilir oyun"). Anthropic `frontend-design` ve `Design` (erişilebilirlik incelemesi) skill'lerinin yayımlanmış yönergeleri çerçeve olarak kullanıldı.

## Amaç
Oyun görme engelliler için TalkBack ile tam erişilebilir kalacak; **bunun üstüne** gören/az gören kullanıcılar ve oyunu birlikte kullananlar için cilalı, ayırt edilebilir bir görünüm. Görsellik erişilebilirliği ASLA bozmaz: tüm etiketler, okuma sırası ve odak davranışı aynı kalır (mevcut testler bunun kanıtıdır).

## Kimlik: "Seyahat defteri / pasaport"
Oyunun konusu dünya turu. Her ülke bir damga, her seviye bir adım. Görsel dil konunun malzemesinden türer: haritanın deniz kâğıdı, pasaportun mürekkep mavisi, damga mürekkeplerinin renk çeşitliliği, oyun karosu olarak fildişi jetonlar.
**Skill incelemesi sonucu revize edilenler** (`frontend-design`, "jenerik varsayılan" listesine karşı): (1) tek parlak vurgu + koyu lacivert zemin kalıbı yerine renk bölge damga mürekkeplerinden gelir, tek vurgu rengi yoktur; (2) aynı yuvarlak kartların tekrarı yerine hiyerarşiye göre farklı biçimler: kahraman panel, çizgili defter satırları, yuvarlak jetonlar, damga; köşe yarıçapı her yerde aynı değildir; (3) büyük harfli etiket, orta nokta dizileri, "—" etiketleri ve ok eklerinden kaçınılır (tek istisna damga yazısı: damgalar doğası gereği büyük harftir); (4) numaralandırma yalnızca gerçek sıralarda (ülkenin 12 / 50. seviyesi gibi) kullanılır; (5) "cesaret" tek yerde harcanır: **harf çarkı (pusula halkası) ve ülke damgası**; gerisi sakin ve disiplinlidir.

## Renk (adlandırılmış belirteçler, hex)
Açık "Deniz kâğıdı": zemin `#E8F0EE`, yüzey `#FAFCFB`, mürekkep `#14232B`, birincil "Pasaport mavisi" `#1F4E79`, ince çizgi `#B9CBC8`, başarı `#1F7A52`, hata `#B3323A`. Harf jetonu: fildişi `#F3EEDF` üzerinde mürekkep harf; seçili jeton: birincil mavi dolgu + beyaz harf + sıra rozeti.
Koyu "Gece haritası": zemin `#0F2229`, yüzey `#17313A`, mürekkep `#E6F0EE`, birincil açık mürekkep `#8DB8E8`, ince çizgi `#2C4A55`, başarı `#5BCB93`, hata `#FF8F94`. Jeton fildişi, harf koyu.
Damga mürekkepleri (bölge): avrupa `#3F63D9`, asya `#D6406C`, ortadogu `#C97316`, afrika `#1F8F5F`, amerika `#8A4FD3`, okyanusya `#0F97AA`; koyu temada her biri kontrastı korumak için daha açık varyantını kullanır. Her metin/arka plan çifti WCAG 2.1 AA (metin ≥ 4.5:1, bileşen/grafik ≥ 3:1) iki temada ayrı ayrı testle doğrulanır (`ThemeContrastTest` genişletilir). Bilgi yalnızca renkle verilmez (durum hapları metin + simge taşır).

## Tipografi
Tek aile: **Atkinson Hyperlegible** (OFL; düşük görme için tasarlandı), Regular ve Bold; uygulamaya gömülür (`res/font`), Türkçe harfleri (ç ğ ı İ ö ş ü) içerdiği betikle doğrulanır; lisans metni `assets/licenses/` altında ve README'de. Ölçek: harf karoları 30sp bold, başlık 28sp, bölüm başlığı 20sp, gövde ≥ 18sp, ince etiket ≥ 14sp. sp kullanılır (sistem yazı boyutuna uyar), 200% ölçekte taşma olmaz.

## Ekranlar
**Ana menü:** üstte karolardan oluşan "KELİME OYUNU" işareti; "Devam et" kahraman kartı (ülke damgası, seviye N, ülke ilerleme çubuğu, büyük birincil düğme); altında kart yerine ince çizgiyle ayrılmış ikincil satırlar: Dünya turu (halka ilerleme "x / 100 ülke"), Nasıl oynanır, Ayarlar; altbilgi: jeton ve toplam seviye. Her kart TEK anlamsal düğümdür (mevcut etiketler korunur).
**Oyun:** üst çubuk (Menü, ülke hapı "Japonya 12 / 50" bölge renginde, jeton hapı); **gerçek bir çapraz bulmaca ızgarası** (kareler, bulunan harfler, ipucuyla açılanlar farklı tonda; ekran okuyucudan gizlenir, kelime listesi bilgiyi taşır); bu ızgaranın altında kompakt kelime listesi (erişilebilir satırlar, mevcut etiketler); ortada seçilen harflerden oluşan "oluşturulan kelime" hapı; **pusula halkası biçiminde harf çarkı** (disk üzerinde ince ölçek çizgileri, harfler halka üzerinde fildişi jetonlar, büyük dokunma hedefleri, ortada Karıştır; seçilen jeton mavi dolgu + sıra rozeti); çarkın altında eylemler: Sil, **Gönder (büyük, birincil)**, Temizle; ikinci satırda İpucu ve Kelime aç (maliyet göstergeli). Odak/okuma sırası mevcut sırayla BİREBİR aynı: başlık, kelime listesi, oluşturulan kelime, harf düğmeleri, eylem düğmeleri.
**Dünya turu:** başlık + halka; **çizgili defter satırları** (kart değil): sol yuvarlak damga (bölge renginde, ülke baş harfleri, dekoratif), ad, ilerleme çubuğu, durum hapı (Kilitli/Devam/Tamamlandı — metin + simge, yalnız renk değil); mevcut ülke vurgulu; tamamlananlar açılınca bilgiler. 
**Seviye/ülke bitişi:** kutlama kartı, kazanımlar, ülke bitişinde "damga" animasyonu; birincil düğme.
**Ayarlar:** bölüm kartları, anahtar satırları: Ses efektleri, **Titreşim** (yeni), **Animasyonlar** (yeni), İlerlemeyi sıfırla (tehlike stili, onay diyaloğu).

## Hareket ve dokunsal geri bildirim
Hareket yalnızca kullanıcı eylemine yanıt verir: karo basma ölçek yayı, doğru kelimede kareler sırayla belirir, seviye bitişinde kısa konfeti (Canvas, ≤ 1,5 sn), ülke bitişinde damga. **Hareket azaltma:** sistem `ANIMATOR_DURATION_SCALE == 0` ya da ayarlardaki Animasyonlar kapalı ise tüm hareket kapanır (anlık geçiş). Hiçbir animasyon odak, anlamsal ağaç veya duyuru zamanlamasını değiştirmez.
**Titreşim:** `View.performHapticFeedback` (izin gerekmez): harf seçimi hafif tık, doğru kelime onay (CONFIRM, eski sürümlerde KEYBOARD_TAP), geçersiz ret (REJECT, eski sürümlerde LONG_PRESS yerine hafif); ayarlardan kapatılır, varsayılan açık. Bu, görme engelli oyuncu için ses ve konuşmaya ek bir dokunsal kanaldır.

## Kısıtlar
Ağ izni yok; yeni bağımlılık yalnızca AndroidX/Compose BOM içinde (çevrimdışı önbellekte bulunan) olsun, `material-icons-extended` KULLANMA (simgeler `Canvas`/`ImageVector` ile çizilir ya da core simgeler). Tüm tıklanabilirlerde Türkçe etiket, dokunma hedefleri ≥ 48dp, görünür odak göstergesi, fontScale 2.0'da kaydırılabilir düzen. Mevcut 413+ test anlamsal sözleşmeyi korur; yalnızca görsel yapıya bağlı olanlar güncellenir. Release: R8 kapalı kalır.

## Sürüm planı
v1.4: kelime listesi (kabul seti + ızgara havuzu), argo filtresi, hızlı yükselen ve zor seviye eğrisi, jeton dengesi. v2.0: bu belirtimdeki görsel yenileme.
