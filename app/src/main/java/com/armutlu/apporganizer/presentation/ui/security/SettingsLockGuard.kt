package com.armutlu.apporganizer.presentation.ui.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.navigation.Routes
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.BiometricHelper

/**
 * F2 (P0 güvenlik): Biyometrik Ayarlar Kilidi artık yalnızca SettingsScreen
 * composable'ında değil, tüm hassas ayar rotalarında (route-guard seviyesinde)
 * uygulanır. MainActivity'nin `open_route` extra'sı ile bir deep-link doğrudan
 * SETTINGS_SECURITY gibi bir alt route'a atlayabildiği için kilidin SADECE
 * SettingsScreen'de olması yetersizdi — bu dosya kilidi NavHost seviyesine taşır.
 *
 * SettingsLockSession: process ömrü boyunca (composable dışı) tek seferlik
 * unlocked flag'i tutar — aynı oturumda ayarlara her dönüşte tekrar biyometrik
 * istenmez (ROADMAP [27] fix mantığı korunur).
 */
object SettingsLockSession {
    @Volatile
    var unlocked: Boolean = false

    fun reset() {
        unlocked = false
    }
}

/**
 * MainActivity EXTRA_OPEN_ROUTE ile deep-link edilebilecek ve/veya kullanıcı
 * verisi/ayarları içeren tüm hassas route'lar. AppNavigation.kt bu setteki her
 * route'un içeriğini SettingsLockGate ile sarmalı.
 */
val SENSITIVE_ROUTES: Set<String> = setOf(
    Routes.SETTINGS,
    Routes.SETTINGS_APPEARANCE,
    Routes.SETTINGS_LAUNCHER,
    Routes.SETTINGS_NOTIFICATIONS,
    Routes.SETTINGS_APPS,
    Routes.SETTINGS_STATS,
    Routes.SETTINGS_USAGE_DATA,
    Routes.SETTINGS_SECURITY,
    Routes.SETTINGS_ABOUT,
    Routes.SEARCH_SETTINGS,
    Routes.SETTINGS_SMART_TICKER,
    Routes.PRIVACY_REPORT,
)

/**
 * Hassas bir ayar rotasının içeriğini biyometrik kilit arkasına alır.
 * Kilit kapalıysa veya bu oturumda zaten doğrulandıysa content doğrudan çizilir.
 * Cihazda biyometrik doğrulama yoksa kullanıcı asla kilitli kalmaz — kilit
 * sessizce atlanır (BiometricHelper.isAvailable == false durumu).
 */
@Composable
fun SettingsLockGate(content: @Composable () -> Unit) {
    val context = LocalContext.current

    var unlocked by remember { mutableStateOf(SettingsLockSession.unlocked) }
    var retryTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTick) {
        if (SettingsLockSession.unlocked) {
            unlocked = true
            return@LaunchedEffect
        }
        val lockEnabled = AppPrefs.isBiometricSettingsLockEnabled(context)
        if (!lockEnabled) {
            unlocked = true
            SettingsLockSession.unlocked = true
            return@LaunchedEffect
        }
        val activity = context as? FragmentActivity
        if (activity == null || !BiometricHelper.isAvailable(activity)) {
            // Cihazda biyometrik yok — kullanıcı kilitlenmemeli.
            unlocked = true
            SettingsLockSession.unlocked = true
            return@LaunchedEffect
        }
        BiometricHelper.authenticate(
            activity = activity,
            onSuccess = {
                unlocked = true
                SettingsLockSession.unlocked = true
            },
            onFailure = { /* kullanıcı "Tekrar dene" ile yeniden deneyebilir */ }
        )
    }

    if (unlocked) {
        content()
    } else {
        SettingsLockPlaceholder(onRetry = { retryTick++ })
    }
}

@Composable
private fun SettingsLockPlaceholder(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_lock_waiting_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.settings_lock_retry_button))
        }
    }
}
