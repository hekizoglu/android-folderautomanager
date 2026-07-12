package com.armutlu.apporganizer.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.ui.MainActivity
import timber.log.Timber

/**
 * Yeni bir uygulama yüklenip otomatik kategoriye atandığında kullanıcıya bilgi bildirimi gösterir.
 *
 * Bildirime tıklanınca ilgili kategori/klasör açılır (MainActivity `open_category` deep-link).
 * "Kategoriyi Değiştir" aksiyonu da aynı kategoriye götürür; kullanıcı oradan yanlış atamayı
 * düzeltebilir (uzun bas → kategori taşı).
 */
object NewAppNotifier {

    private const val CHANNEL_ID = "new_app_category"
    private const val CHANNEL_NAME = "Yeni Uygulama Kategorisi"
    private const val CHANNEL_DESC = "Yeni yüklenen uygulamaların hangi kategoriye eklendiğini bildirir"

    /**
     * Yeni yüklenen ve kategorisi atanan bir uygulama için bildirim gönderir.
     *
     * @param categoryId  Uygulamanın atandığı kategori kimliği (deep-link hedefi)
     * @param categoryName Kullanıcıya gösterilecek kategori adı
     */
    fun notifyCategorized(
        context: Context,
        packageName: String,
        appName: String,
        categoryId: String,
        categoryName: String
    ) {
        // Android 13+ (TIRAMISU) POST_NOTIFICATIONS runtime izni yoksa sessizce çık —
        // izinsiz notify() çağrısı sistemce yok sayılır, log kirliliği yaratmayalım.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Timber.d("NewAppNotifier: POST_NOTIFICATIONS izni yok, bildirim atlandı ($packageName)")
                return
            }
        }

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)

        // Bildirime tıklama → ilgili kategoriyi aç
        val contentPending = openCategoryPendingIntent(
            context = context,
            categoryId = categoryId,
            requestCode = ("open_" + packageName).hashCode()
        )

        // "Kategoriyi Değiştir" aksiyonu → aynı kategoriye götür; kullanıcı oradan düzeltir
        val changePending = openCategoryPendingIntent(
            context = context,
            categoryId = categoryId,
            requestCode = ("change_" + packageName).hashCode()
        )

        val text = "$appName → $categoryName kategorisine eklendi"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Yeni uygulama düzenlendi")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPending)
            .addAction(0, "Kategoriyi Değiştir", changePending)
            .setAutoCancel(true)
            .build()

        // Paket başına benzersiz ID — aynı uygulama tekrar bildirime düşerse eskisini günceller
        manager.notify(packageName.hashCode(), notification)
        Timber.d("NewAppNotifier: '$appName' için '$categoryName' bildirimi gönderildi")
    }

    private fun openCategoryPendingIntent(
        context: Context,
        categoryId: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // MainActivity.applyOpenCategoryIntent() bu extra'yı okuyup kategoriyi seçer
            putExtra("open_category", categoryId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESC
        }
        manager.createNotificationChannel(channel)
    }
}
