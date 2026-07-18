package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Döngü P03 — HomeScreen içinden çıkarılan global iskelet.
 *
 * Tüm sayfalarda sabit kalması gereken öğeleri (arama çubuğu üst/alt konum,
 * pager alanı, sayfa göstergesi, dock) tek bir yerde toplar; sayfa içeriği
 * `pager` slot'u içinde kalır. Bu döngüde sayfa davranışı DEĞİŞMEZ — sadece
 * yapısal ayrıştırma yapılır (roadmap satır 502-558).
 *
 * NOT: `indicator` parametresi roadmap imzasına uygunluk için mevcuttur.
 * Sayfa göstergesi (`HomePageIndicator`) bugün hâlâ pager içeriğiyle aynı
 * `BoxWithConstraints` ölçümüne bağımlı (pagerState/pageCount ancak orada
 * hesaplanabiliyor) — bu bağımlılığı kırmak pager'ı `FolderGridPage`'e
 * dönüştüren P04/P05 kapsamındadır. Bu yüzden P03'te `indicator` slotu
 * HomeScreen tarafından boş (`{}`) geçilir; gerçek gösterge hâlâ `pager`
 * slot'unun içinde render edilir — görsel davranış birebir korunur.
 *
 * Sistem bar ve IME padding'i yalnız burada (root shell) uygulanır;
 * sayfa içerikleri bu padding'i tekrar uygulamaz.
 */
@Composable
fun HomeShell(
    modifier: Modifier = Modifier,
    topSearch: (@Composable () -> Unit)? = null,
    pager: @Composable ColumnScope.() -> Unit,
    indicator: @Composable () -> Unit = {},
    bottomSearch: (@Composable () -> Unit)? = null,
    dock: @Composable () -> Unit,
    overlays: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.Top
        ) {
            topSearch?.invoke()
            pager()
            indicator()
            bottomSearch?.invoke()
            dock()
        }
        overlays()
    }
}
