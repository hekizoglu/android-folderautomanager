package com.armutlu.apporganizer.domain.usecase.notification

/**
 * P0.5 — Paket bazli "okunmamis" bildirim modeli.
 *
 * Sorun (ROADMAP_AI_AUDIT P0.5): "aktif sistem bildirimi", "son 24 saatte gelen bildirim
 * sayisi" ve "uygulama icinde okundu" ayri kavramlardir ama kod bunlari ayni sayacla
 * (NotificationListenerService.activeNotifications sayisi) temsil ediyordu. Kullanici
 * uygulamayi acip bildirimi sistem tarafinda gorup kapatinca launcher badge'i de sifirlaniyordu
 * (dogru), ama launcher bildirimi SISTEM TARAFINDA iptal etmiyor (kod tarandi, boyle bir
 * cagri yok) — sorun yalnizca "okunmus" ile "hala aktif" ayriminin net olmamasiydi.
 *
 * Bu dosya SAF (Android framework'siz) bir fonksiyon seti sunar: paket basina "son bildirim
 * zamani" (lastPostedAt) ile "kullanicinin en son o uygulamayi actigi zaman" (lastReadAt)
 * karsilastirilir. Badge sayisi = okunmamis bildirim varsa NotificationListenerService'in
 * o an bildirdigi AKTIF bildirim sayisi (sistem tarafinda hala goruntuleniyorsa), yoksa 0.
 *
 * NOT: Bu model NotificationReport'taki 7 gunluk/30 gunluk istatistikleri ETKILEMEZ —
 * o rapor notification_events tablosundan bagimsiz sorgulanir (NotificationAnalyzer).
 * Burada hesaplanan yalnizca "badge'de kac tane goster" sorusunun cevabidir.
 */
object UnreadNotificationModel {

    /**
     * Bir paket icin badge'de gosterilecek "okunmamis" sayisini hesaplar.
     *
     * @param activeCount NotificationListenerService.activeNotifications'tan o an gelen,
     *   sistem tarafinda hala aktif/goruntulenen bildirim sayisi (0 ise zaten gosterilecek yok).
     * @param lastPostedAt bu paket icin en son bildirim POST edilme zamani (epoch ms), yoksa null.
     * @param lastReadAt kullanicinin bu paketi launcher'dan en son actigi zaman (epoch ms),
     *   hic acilmadiysa null.
     * @return badge'de gosterilecek sayi. Kullanici son bildirimden SONRA uygulamayi actiysa
     *   (lastReadAt >= lastPostedAt) okunmus sayilir ve 0 doner — sistem bildirimi hala aktif
     *   olsa bile (orn. kullanici bildirimi sistem tarafinda henuz kapatmadi) launcher badge'i
     *   "okundu" bilgisini yansitir.
     */
    fun unreadCountFor(activeCount: Int, lastPostedAt: Long?, lastReadAt: Long?): Int {
        if (activeCount <= 0) return 0
        // Hic bildirim zamani bilinmiyorsa (beklenmeyen durum) guvenli varsayilan: aktif sayiyi goster.
        val posted = lastPostedAt ?: return activeCount
        val read = lastReadAt ?: return activeCount
        return if (read >= posted) 0 else activeCount
    }

    /**
     * Toplu hesaplama — LauncherViewModel'in badgeCounts akisinda kullanilir.
     *
     * @param activeCounts NotificationListenerService.badgeCounts.value (pkg -> aktif sayi)
     * @param lastPostedAt AppNotificationListenerService.lastPostedAt.value (pkg -> son post zamani)
     * @param lastReadAt NotificationReadPrefs.getAll(context) (pkg -> en son acilis zamani)
     * @return yalnizca okunmamis (>0) sayilar icerir; okunmus paketler haritada YER ALMAZ
     *   (cagiran taraf mevcut sifirlama mantigini korur — bkz. LauncherViewModel toReset).
     */
    fun computeUnreadCounts(
        activeCounts: Map<String, Int>,
        lastPostedAt: Map<String, Long>,
        lastReadAt: Map<String, Long>,
    ): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        activeCounts.forEach { (pkg, active) ->
            val unread = unreadCountFor(active, lastPostedAt[pkg], lastReadAt[pkg])
            if (unread > 0) result[pkg] = unread
        }
        return result
    }
}
