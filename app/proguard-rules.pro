-keepattributes *Annotation*

# Hilt / Dagger
-keepclassmembers class * {
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <init>(...);
}
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Room entities
-keep class com.armutlu.apporganizer.domain.models.** { *; }
-keep class com.armutlu.apporganizer.data.local.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Accompanist
-keep class com.google.accompanist.** { *; }

# Timber
-dontwarn org.jetbrains.annotations.**

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# DataStore / Preferences
-keep class androidx.datastore.** { *; }

# Kotlin Serialization / JSON (org.json kullanıyoruz)
-keep class org.json.** { *; }

# AppWidget (widget desteği)
-keep class android.appwidget.** { *; }
-keep class * extends android.appwidget.AppWidgetProvider { *; }

# Coil image loader
-keep class coil.** { *; }
-dontwarn coil.**

# Launcher shortcut API
-keep class android.content.pm.ShortcutInfo { *; }
-keep class android.content.pm.ShortcutManager { *; }

# Enum sınıflarının values()/valueOf() korunması
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Stack trace için satır numaralarını koru
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
