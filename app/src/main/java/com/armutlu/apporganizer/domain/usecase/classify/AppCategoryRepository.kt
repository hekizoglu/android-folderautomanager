package com.armutlu.apporganizer.domain.usecase.classify

/**
 * Online uygulama kategorisi sorgulama arayüzü.
 * Şu an local stub; ileride 2M+ uygulamalık online havuza bağlanacak.
 */
interface AppCategoryRepository {
    /**
     * Paket adına göre kategori döndürür.
     * Online havuzda bulunamazsa null döner → local sınıflandırma devreye girer.
     */
    suspend fun getCategoryForPackage(packageName: String): String?

    /**
     * Uygulama isminden arama yapar.
     * Bilinmeyen uygulamalar için kullanılır.
     */
    suspend fun searchByAppName(appName: String): String?
}

/** Henüz online DB kurulmadığında kullanılan no-op implementasyon */
class LocalOnlyAppCategoryRepository : AppCategoryRepository {
    override suspend fun getCategoryForPackage(packageName: String): String? = null
    override suspend fun searchByAppName(appName: String): String? = null
}
