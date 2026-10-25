# R8 kuralları. Compose, AndroidX (lifecycle, DataStore, activity) kendi consumer kurallarını getirir.
# Uygulama kodu yansıma kullanmaz: ViewModel fabrikası GameViewModel'i doğrudan kurar
# (AppContainer.gameViewModelFactory), Sfx/GameEvent sınıflarını yansımayla bulan kod yalnızca testtedir.
# Sesler ve veri assets/ altından dosya adıyla yüklenir (Sfx.fileName); assets R8'e dokunulmaz.

# Hata ayıklanabilir yığın izleri (satır numaraları korunur, kaynak adı gizlenir).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin enum'larının values()/valueOf() ve entries kullanımı (Sfx.entries) R8 tarafından zaten korunur;
# yine de enum ad tabanlı dosya eşlemesi (name.lowercase() + ".wav") için adların değişmemesi şarttır.
-keepclassmembers enum com.kelimeoyunu.audio.Sfx {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
