package com.armutlu.apporganizer.domain.usecase.privacy

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.armutlu.apporganizer.domain.models.AppInfo
import timber.log.Timber

/**
 * Gizlilik Analizi — PackageManager'dan yuklu uygulamalarin ISTEDIGI ve VERILMIS
 * hassas izinlerini okur. Tamamen lokal: hicbir veri disari gonderilmez, sadece
 * cihazdaki PackageManager verisi okunur ve gruplanir.
 *
 * QUERY_ALL_PACKAGES izni zaten manifestte var (AppClassifier de kullaniyor) —
 * bu ozellik icin yeni izin eklenmedi.
 */
object PrivacyAnalyzer {

    /** Tek bir hassas izin grubu (ornek: Konum -> ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION). */
    data class PermissionGroup(
        val id: String,
        val label: String,
        val emoji: String,
        val manifestPermissions: List<String>,
    )

    /** Grup icinde izin VERILMIS tek bir uygulama. */
    data class GrantedApp(
        val packageName: String,
        val appName: String,
    )

    /** Bir izin grubu icin nihai rapor satiri. */
    data class PermissionGroupReport(
        val group: PermissionGroup,
        val grantedApps: List<GrantedApp>,
        /** Izni istedi ama sistem/kullanici tarafindan verilmemis uygulama sayisi. */
        val requestedNotGrantedCount: Int,
    ) {
        val grantedCount: Int get() = grantedApps.size
    }

    /** Tum gruplarin toplandigi rapor. */
    data class PrivacyReport(
        val groups: List<PermissionGroupReport>,
    ) {
        /** Hic hassas izin verilmemisse true — bos/kutlama durumu icin. */
        val isAllClear: Boolean get() = groups.all { it.grantedCount == 0 }
    }

    /** Izlenen hassas izin gruplari — sabit tanim, saf veri (test edilebilir). */
    val SENSITIVE_GROUPS: List<PermissionGroup> = listOf(
        PermissionGroup(
            id = "camera",
            label = "Kamera",
            emoji = "📷", // 📷
            manifestPermissions = listOf("android.permission.CAMERA"),
        ),
        PermissionGroup(
            id = "microphone",
            label = "Mikrofon",
            emoji = "🎤", // 🎤
            manifestPermissions = listOf("android.permission.RECORD_AUDIO"),
        ),
        PermissionGroup(
            id = "location",
            label = "Konum",
            emoji = "📍", // 📍
            manifestPermissions = listOf(
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_BACKGROUND_LOCATION",
            ),
        ),
        PermissionGroup(
            id = "contacts",
            label = "Kisiler",
            emoji = "👥", // 👥
            manifestPermissions = listOf("android.permission.READ_CONTACTS"),
        ),
        PermissionGroup(
            id = "sms",
            label = "SMS",
            emoji = "💬", // 💬
            manifestPermissions = listOf(
                "android.permission.READ_SMS",
                "android.permission.RECEIVE_SMS",
            ),
        ),
        PermissionGroup(
            id = "phone",
            label = "Telefon",
            emoji = "📞", // 📞
            manifestPermissions = listOf(
                "android.permission.READ_PHONE_STATE",
                "android.permission.READ_CALL_LOG",
            ),
        ),
        PermissionGroup(
            id = "storage",
            label = "Depolama/Medya",
            emoji = "🗂️", // 🗂️
            manifestPermissions = listOf(
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VIDEO",
                "android.permission.READ_MEDIA_AUDIO",
            ),
        ),
        PermissionGroup(
            id = "calendar",
            label = "Takvim",
            emoji = "📅", // 📅
            manifestPermissions = listOf("android.permission.READ_CALENDAR"),
        ),
    )

    /**
     * Tek bir uygulamanin izin durumunu tasiyan ham veri — PackageManager'dan cikarilir,
     * gruplama/sayim mantigi bundan bagimsiz saf fonksiyonda calisir (test edilebilir).
     */
    data class AppPermissionSnapshot(
        val packageName: String,
        val appName: String,
        /** Uygulamanin manifestinde istedigi (requested) izinler. */
        val requestedPermissions: Set<String>,
        /** Kullanici/sistem tarafindan fiilen verilmis izinler (requested altkumesi). */
        val grantedPermissions: Set<String>,
    )

    /**
     * PackageManager okuma katmani — gizli olmayan (launcher'da gorunen) yuklu
     * uygulamalarin istedigi/verilmis izinlerini cikarir. IO/binder cagrisi icerir,
     * cagiran taraf IO dispatcher'da calistirmali.
     */
    fun readInstalledAppSnapshots(
        context: Context,
        visibleApps: List<AppInfo>,
    ): List<AppPermissionSnapshot> {
        val pm = context.packageManager
        return visibleApps.mapNotNull { app ->
            runCatching {
                val info: PackageInfo = pm.getPackageInfo(
                    app.packageName,
                    PackageManager.GET_PERMISSIONS,
                )
                val requestedPermissions = info.requestedPermissions?.toSet() ?: emptySet()
                if (requestedPermissions.isEmpty()) return@runCatching null

                val flags = info.requestedPermissionsFlags
                val granted = mutableSetOf<String>()
                info.requestedPermissions?.forEachIndexed { index, permission ->
                    val flag = flags?.getOrNull(index) ?: 0
                    if (flag and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) {
                        granted.add(permission)
                    }
                }

                AppPermissionSnapshot(
                    packageName = app.packageName,
                    appName = app.appName,
                    requestedPermissions = requestedPermissions,
                    grantedPermissions = granted,
                )
            }.onFailure { e ->
                Timber.w(e, "Izin bilgisi okunamadi: ${app.packageName}")
            }.getOrNull()
        }
    }

    /**
     * Saf mantik: snapshot listesini hassas izin gruplarina gore siniflandirir ve sayar.
     * PackageManager'a bagimli degildir — unit test edilebilir.
     */
    fun buildReport(
        snapshots: List<AppPermissionSnapshot>,
        groups: List<PermissionGroup> = SENSITIVE_GROUPS,
    ): PrivacyReport {
        val groupReports = groups.map { group ->
            val grantedApps = mutableListOf<GrantedApp>()
            var requestedNotGrantedCount = 0

            snapshots.forEach { snapshot ->
                val requestsGroup = group.manifestPermissions.any { it in snapshot.requestedPermissions }
                if (!requestsGroup) return@forEach

                val isGranted = group.manifestPermissions.any { it in snapshot.grantedPermissions }
                if (isGranted) {
                    grantedApps.add(GrantedApp(snapshot.packageName, snapshot.appName))
                } else {
                    requestedNotGrantedCount++
                }
            }

            PermissionGroupReport(
                group = group,
                grantedApps = grantedApps.sortedBy { it.appName.lowercase() },
                requestedNotGrantedCount = requestedNotGrantedCount,
            )
        }

        return PrivacyReport(groups = groupReports)
    }

    /** Ucdan uca: PackageManager oku + grupla. Cagiran taraf IO dispatcher'da calistirmali. */
    fun analyze(context: Context, visibleApps: List<AppInfo>): PrivacyReport {
        val snapshots = readInstalledAppSnapshots(context, visibleApps)
        return buildReport(snapshots)
    }
}
