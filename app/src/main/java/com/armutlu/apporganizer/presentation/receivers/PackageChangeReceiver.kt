package com.armutlu.apporganizer.presentation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.data.repository.SearchRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class PackageChangeReceiver : BroadcastReceiver() {

    @EntryPoint
    @dagger.hilt.InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun appRepository(): AppRepository
        fun searchRepository(): SearchRepository
        fun packageManagerHelper(): com.armutlu.apporganizer.utils.PackageManagerHelper
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        Timber.d("Package event: ${intent.action} for $packageName")

        // EXTRA_REPLACING=true → mevcut uygulamanın güncellenmesi (gerçek yeni yükleme değil).
        // Yeni-uygulama bildirimini sadece gerçek ilk yüklemede göster.
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        // goAsync() ile Android'e "henuz bitmedi" sinyali verilir; coroutine bitince finish() cagrilir
        val pendingResult = goAsync()
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED   -> onPackageAdded(context, packageName, isReplacing, pendingResult)
            Intent.ACTION_PACKAGE_REMOVED -> onPackageRemoved(context, packageName, pendingResult)
            Intent.ACTION_PACKAGE_CHANGED -> onPackageChanged(context, packageName, pendingResult)
            else                          -> pendingResult.finish()
        }
    }

    private fun onPackageAdded(
        context: Context,
        packageName: String,
        isReplacing: Boolean,
        pendingResult: BroadcastReceiver.PendingResult
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = getRepository(context)
                val searchRepo = getSearchRepository(context)
                val helper = getPackageManagerHelper(context)
                // Kök neden (EX01 bug): PACKAGE_ADDED bazi OEM/Android surumlerinde PackageManager
                // paket bilgisini TAM commit etmeden mikro-saniyeler once tetiklenebilir —
                // getAppInfo() bu durumda NameNotFoundException yutup null doner ve eskiden
                // burada sessizce return ediliyordu: uygulama DB'ye hic yazilmiyor, cekmecede
                // hicbir zaman gorunmuyordu (kalici kayip — bir sonraki reconcile'a kadar).
                // Kisa, sinirli backoff ile 3 deneme bu yarisi onler.
                var appInfo = helper.getAppInfo(packageName)
                var attempt = 0
                while (appInfo == null && attempt < 2) {
                    kotlinx.coroutines.delay(150L * (attempt + 1))
                    appInfo = helper.getAppInfo(packageName)
                    attempt++
                }
                if (appInfo == null) {
                    Timber.w("onPackageAdded: getAppInfo $attempt denemeden sonra hala null, $packageName atlandi")
                    return@launch
                }
                repo.insertApps(listOf(appInfo))
                // insertApps kategori atar; taze kaydı DB'den çek (categoryId dolu gelir)
                val stored = repo.getAppByPackageName(packageName) ?: appInfo
                searchRepo.indexApp(stored)
                Timber.d("Added new app to DB: $packageName")

                // Sadece GERÇEK yeni yüklemede (güncelleme değil) kullanıcıya kategori bildirimi göster
                if (!isReplacing) {
                    val categoryId = stored.categoryId
                    val categoryName = repo.getCategoryById(categoryId)?.categoryName ?: categoryId
                    com.armutlu.apporganizer.utils.NewAppNotifier.notifyCategorized(
                        context = context,
                        packageName = packageName,
                        appName = stored.appName,
                        categoryId = categoryId,
                        categoryName = categoryName
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding app: $packageName")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun onPackageRemoved(context: Context, packageName: String, pendingResult: BroadcastReceiver.PendingResult) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = getRepository(context)
                val searchRepo = getSearchRepository(context)
                repo.deleteApp(packageName)
                searchRepo.removeApp(packageName)
                // Favori listesinden de temizle — silinen uygulama favorilerde kalmasın
                com.armutlu.apporganizer.utils.AppPrefs.removeFavorite(context, packageName)
                Timber.d("Removed app from DB: $packageName")
            } catch (e: Exception) {
                Timber.e(e, "Error removing app: $packageName")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun onPackageChanged(context: Context, packageName: String, pendingResult: BroadcastReceiver.PendingResult) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = getRepository(context)
                val searchRepo = getSearchRepository(context)
                if (!repo.appExists(packageName)) return@launch
                // Mevcut kaydı koru (kategori, gizlilik), sadece PM'den gelen taze veriyi güncelle
                val existing = repo.getAppByPackageName(packageName) ?: return@launch
                val helper = getPackageManagerHelper(context)
                val fresh = helper.getAppInfo(packageName) ?: return@launch
                val merged = fresh.copy(
                    categoryId   = existing.categoryId,
                    isHidden     = existing.isHidden,
                    usageCount   = existing.usageCount,
                    launchCount  = existing.launchCount,
                    lastUsedTimestamp = existing.lastUsedTimestamp,
                    notificationCount = existing.notificationCount
                )
                repo.insertApps(listOf(merged))
                searchRepo.indexApp(merged)
                Timber.d("App updated (category preserved): $packageName")
            } catch (e: Exception) {
                Timber.e(e, "Error handling app change: $packageName")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun getRepository(context: Context): AppRepository {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverEntryPoint::class.java
        )
        return entryPoint.appRepository()
    }

    private fun getPackageManagerHelper(context: Context): com.armutlu.apporganizer.utils.PackageManagerHelper {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverEntryPoint::class.java
        )
        return entryPoint.packageManagerHelper()
    }

    private fun getSearchRepository(context: Context): SearchRepository {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverEntryPoint::class.java
        )
        return entryPoint.searchRepository()
    }
}
