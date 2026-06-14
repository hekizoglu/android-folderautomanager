# ============================================================
# AppOrganizer — ProGuard Kuralları (Play Store Release Build)
# Son güncelleme: Loop 85 — 2026-06-14
# ============================================================

# Genel: annotation'lar korunsun (Hilt, Room, Compose)
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Stack trace için satır numaralarını koru
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================
# Hilt / Dagger
# ============================================================
-keepclassmembers class * {
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <init>(...);
}
-keep class dagger.hilt.** { *; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-dontwarn dagger.hilt.**

# ============================================================
# Room DB
# ============================================================
-keep class com.armutlu.apporganizer.domain.models.** { *; }
-keep class com.armutlu.apporganizer.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract !synthetic *;
}

# ============================================================
# Kotlin Coroutines
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================================
# WorkManager
# ============================================================
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# ============================================================
# Notification Listener Service
# ============================================================
-keep class * extends android.service.notification.NotificationListenerService { *; }
-keep class android.service.notification.** { *; }
-keep class android.app.Notification { *; }
-keep class android.app.NotificationChannel { *; }

# ============================================================
# Launcher API (LauncherApps, Shortcuts, AppWidget)
# ============================================================
-keep class android.content.pm.LauncherApps { *; }
-keep class android.content.pm.LauncherActivityInfo { *; }
-keep class android.content.pm.ShortcutInfo { *; }
-keep class android.content.pm.ShortcutManager { *; }
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class android.appwidget.** { *; }
-keep class android.app.role.RoleManager { *; }

# ============================================================
# Coil (image loading)
# ============================================================
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ============================================================
# DataStore / SharedPreferences
# ============================================================
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ============================================================
# Compose (reflection koruması)
# ============================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class kotlin.reflect.** { *; }

# ============================================================
# Timber
# ============================================================
-dontwarn org.jetbrains.annotations.**
-keep class timber.log.** { *; }

# ============================================================
# JSON (org.json — DeepSeek API çağrıları)
# ============================================================
-keep class org.json.** { *; }

# ============================================================
# Serializable / Parcelable
# ============================================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# ============================================================
# Enum
# ============================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================
# R8 / genel uyarılar
# ============================================================
-dontwarn sun.misc.**
-dontwarn java.lang.invoke.**
-dontwarn **$$serializer
