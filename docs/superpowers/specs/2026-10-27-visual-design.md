# Görsel Tasarım Belirtimi (v2.0)

Durum: kullanıcı isteğiyle başlatıldı ("görsellik sıfır, daha güzel bir menü, daha şık tasarım, daha oynanabilir oyun"). Anthropic `frontend-design` ve `Design` (erişilebilirlik incelemesi) skill'lerinin yayımlanmış yönergeleri çerçeve olarak kullanıldı.

## Amaç
Oyun görme engelliler için TalkBack ile tam erişilebilir kalacak; **bunun üstüne** gören/az gören kullanıcılar ve oyunu birlikte kullananlar için cilalı, ayırt edilebilir bir görünüm. Görsellik erişilebilirliği ASLA bozmaz: tüm etiketler, okuma sırası ve odak davranışı aynı kalır (mevcut testler bunun kanıtıdır).

## Kimlik: "Seyahat defteri / pasaport"
Oyunun konusu dünya turu. Her ülke bir damga, her seviye bir adım. Görsel dil: harita ve pasaport damgası; kağıt-mürekkep kontrastı, bölgeye göre renk kodu.
Kaçınılacaklar: krem + kiremit klişesi, koyu zemin + tek parlak vurgu, aynı yuvarlak SaaS kartlarının tekrarı, dekoratif süs. "Cesaret" tek yerde harcanır: **harf çarkı ve ülke damgası**.

## Renk (adlandırılmış belirteçler, hex)
Açık tema "Gündüz": zemin `#EEF3F6`, yüzey `#FFFFFF`, mürekkep `#12202B`, birincil "Okyanus" `#0B6E8A`, vurgu "Pusula" `#F2B63D`, başarı `#1F8A5B`, hata `#C23B3B`.
Koyu tema "Gece uçuşu": zemin `#0E1A22`, yüzey `#16262F`, mürekkep `#E8F0F4`, birincil `#4FC3DD`, vurgu `#F2B63D`, başarı `#4CC38A`, hata `#FF8080`.
Bölge renkleri (damga/kart vurgusu): avrupa `#5B7CFA`, asya `#E0527A`, ortadogu `#E08A2E`, afrika `#2E9E6B`, amerika `#9B5DE5`, okyanusya `#1FB5C9`. Her metin/arka plan çifti WCAG 2.1 AA (metin ≥ 4.5:1, bileşen/grafik ≥ 3:1); koyu ve açık temada ayrı ayrı doğrulanır (mevcut `ThemeContrastTest` genişletilir). Bilgi yalnızca renkle verilmez.

## Tipografi
Tek aile: **Atkinson Hyperlegible** (OFL; düşük görme için tasarlandı), Regular ve Bold; uygulamaya gömülür (`res/font`), Türkçe harfleri (ç ğ ı İ ö ş ü) içerdiği betikle doğrulanır; lisans metni `assets/licenses/` altında ve README'de. Ölçek: harf karoları 30sp bold, başlık 28sp, bölüm başlığı 20sp, gövde ≥ 18sp, ince etiket ≥ 14sp. sp kullanılır (sistem yazı boyutuna uyar), 200% ölçekte taşma olmaz.

## Ekranlar
**Ana menü:** üstte karolardan oluşan "KELİME OYUNU" işareti; "Devam et" kahraman kartı (ülke damgası, seviye N, ülke ilerleme çubuğu, büyük birincil düğme); altında 2×2 yerine tek sütun ikincil satırlar: Dünya turu (halka ilerleme "x / 100 ülke"), Nasıl oynanır, Ayarlar; altbilgi: jeton ve toplam seviye. Her kart TEK anlamsal düğümdür (mevcut etiketler korunur).
**Oyun:** üst çubuk (Menü, ülke hapı "Japonya 12 / 50" bölge renginde, jeton hapı); **gerçek bir çapraz bulmaca ızgarası** (kareler, bulunan harfler, ipucuyla açılanlar farklı tonda; ekran okuyucudan gizlenir, kelime listesi bilgiyi taşır); bu ızgaranın altında kompakt kelime listesi (erişilebilir satırlar, mevcut etiketler); ortada seçilen harflerden oluşan "oluşturulan kelime" hapı; **daire düzeninde harf çarkı** (harfler daire üzerinde büyük dokunma hedefleri, ortada Karıştır; seçilince karo soluk + sıra rozeti); çarkın altında eylemler: Sil, **Gönder (büyük, birincil)**, Temizle; ikinci satırda İpucu ve Kelime aç (maliyet göstergeli). Odak/okuma sırası mevcut sırayla BİREBİR aynı: başlık, kelime listesi, oluşturulan kelime, harf düğmeleri, eylem düğmeleri.
**Dünya turu:** başlık + halka; ülke kartları: sol yuvarlak damga (bölge renginde, ülke baş harfleri, dekoratif), ad, ilerleme çubuğu, durum hapı (Kilitli/Devam/Tamamlandı — metin + simge, yalnız renk değil); mevcut ülke vurgulu; tamamlananlar açılınca bilgiler. 
**Seviye/ülke bitişi:** kutlama kartı, kazanımlar, ülke bitişinde "damga" animasyonu; birincil düğme.
**Ayarlar:** bölüm kartları, anahtar satırları: Ses efektleri, **Titreşim** (yeni), **Animasyonlar** (yeni), İlerlemeyi sıfırla (tehlike stili, onay diyaloğu).

## Hareket ve dokunsal geri bildirim
Hareket yalnızca kullanıcı eylemine yanıt verir: karo basma ölçek yayı, doğru kelimede kareler sırayla belirir, seviye bitişinde kısa konfeti (Canvas, ≤ 1,5 sn), ülke bitişinde damga. **Hareket azaltma:** sistem `ANIMATOR_DURATION_SCALE == 0` ya da ayarlardaki Animasyonlar kapalı ise tüm hareket kapanır (anlık geçiş). Hiçbir animasyon odak, anlamsal ağaç veya duyuru zamanlamasını değiştirmez.
**Titreşim:** `View.performHapticFeedback` (izin gerekmez): harf seçimi hafif tık, doğru kelime onay (CONFIRM, eski sürümlerde KEYBOARD_TAP), geçersiz ret (REJECT, eski sürümlerde LONG_PRESS yerine hafif); ayarlardan kapatılır, varsayılan açık. Bu, görme engelli oyuncu için ses ve konuşmaya ek bir dokunsal kanaldır.

## Kısıtlar
Ağ izni yok; yeni bağımlılık yalnızca AndroidX/Compose BOM içinde (çevrimdışı önbellekte bulunan) olsun, `material-icons-extended` KULLANMA (simgeler `Canvas`/`ImageVector` ile çizilir ya da core simgeler). Tüm tıklanabilirlerde Türkçe etiket, dokunma hedefleri ≥ 48dp, görünür odak göstergesi, fontScale 2.0'da kaydırılabilir düzen. Mevcut 413+ test anlamsal sözleşmeyi korur; yalnızca görsel yapıya bağlı olanlar güncellenir. Release: R8 kapalı kalır.

## Sürüm planı
v1.4: kelime listesi (kabul seti + ızgara havuzu), argo filtresi, hızlı yükselen ve zor seviye eğrisi, jeton dengesi. v2.0: bu belirtimdeki görsel yenileme.
