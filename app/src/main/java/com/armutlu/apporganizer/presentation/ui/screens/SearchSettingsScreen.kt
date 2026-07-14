package com.armutlu.apporganizer.presentation.ui.screens

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.ui.common.rememberBooleanPreferenceState
import com.armutlu.apporganizer.presentation.viewmodel.SearchSettingsViewModel
import com.armutlu.apporganizer.utils.AppPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SearchSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val sourceOpInFlight by viewModel.sourceOpInFlight.collectAsState()
    var homeAppSearchEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_HOME_APP_SEARCH_ENABLED) { AppPrefs.isHomeAppSearchEnabled(context) }
    var homeSearchEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_HOME_SEARCH_ENABLED) { AppPrefs.isHomeSearchEnabled(context) }
    var doubleTapSearchEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_DOUBLE_TAP_SEARCH) { AppPrefs.isDoubleTapSearchEnabled(context) }
    val appsSourceEnabled = true
    var categoriesSourceEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_SEARCH_SOURCE_CATEGORIES) { AppPrefs.isSearchSourceCategoriesEnabled(context) }
    var settingsSourceEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_SEARCH_SOURCE_SETTINGS) { AppPrefs.isSearchSourceSettingsEnabled(context) }
    var contactsSourceEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_SEARCH_SOURCE_CONTACTS) { AppPrefs.isSearchSourceContactsEnabled(context) }
    var filesSourceEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_SEARCH_SOURCE_FILES) { AppPrefs.isSearchSourceFilesEnabled(context) }
    var rankingProfile by remember { mutableStateOf(AppPrefs.getSearchRankingProfile(context)) }
    var searchBarPosition by remember { mutableStateOf(AppPrefs.getSearchBarPosition(context)) }
    var pendingPermission by remember { mutableStateOf<ContextualPermission?>(null) }

    // Gelişmiş arama ayarları
    var fuzzyEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_SEARCH_FUZZY) { AppPrefs.isSearchFuzzyEnabled(context) }
    var phoneticEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_SEARCH_PHONETIC) { AppPrefs.isSearchPhoneticEnabled(context) }
    var instantEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_SEARCH_INSTANT) { AppPrefs.isSearchInstantEnabled(context) }
    var sortByUsage by rememberBooleanPreferenceState(context, AppPrefs.KEY_SEARCH_SORT_BY_USAGE) { AppPrefs.isSearchSortByUsage(context) }
    var maxResults        by remember { mutableStateOf(AppPrefs.getSearchMaxResults(context)) }
    var showIcons by rememberBooleanPreferenceState(context, AppPrefs.KEY_SEARCH_SHOW_ICONS) { AppPrefs.isSearchShowIcons(context) }
    var showContactAvatar by rememberBooleanPreferenceState(context, AppPrefs.KEY_SEARCH_SHOW_CONTACT_AVATAR) { AppPrefs.isSearchShowContactAvatar(context) }
    var searchStatsEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_SEARCH_STATS_ENABLED) { AppPrefs.isSearchStatsEnabled(context) }
    var webFallbackEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_SEARCH_WEB_FALLBACK_ENABLED) { AppPrefs.isSearchWebFallbackEnabled(context) }

    pendingPermission?.let { permission ->
        ContextualPermissionDialog(
            permission = permission,
            onGranted = {
                when (permission) {
                    ContextualPermission.CONTACTS -> {
                        contactsSourceEnabled = true
                        AppPrefs.setSearchSourceContactsEnabled(context, true)
                        viewModel.enableContactsSource()
                    }
                    ContextualPermission.FILES -> {
                        filesSourceEnabled = true
                        AppPrefs.setSearchSourceFilesEnabled(context, true)
                        viewModel.enableFilesSource()
                    }
                    else -> Unit
                }
                pendingPermission = null
            },
            onDismiss = {
                when (permission) {
                    ContextualPermission.CONTACTS -> contactsSourceEnabled = AppPrefs.isSearchSourceContactsEnabled(context)
                    ContextualPermission.FILES -> filesSourceEnabled = AppPrefs.isSearchSourceFilesEnabled(context)
                    else -> Unit
                }
                pendingPermission = null
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Arama", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { SettingsSectionTitle("Arama Davranisi") }
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.Search,
                        title = "Ana Ekran Arama",
                        subtitle = "Ana ekrandaki arama çubuğunda uygulama sonuçları gösterilir",
                        checked = homeAppSearchEnabled,
                        onCheckedChange = {
                            homeAppSearchEnabled = it
                            AppPrefs.setHomeAppSearchEnabled(context, it)
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Category,
                        title = "Klasör ve Kategori Arama",
                        subtitle = "Klasör filtresi ve kategori eşleşmeleri açık kalır",
                        checked = homeSearchEnabled,
                        onCheckedChange = {
                            homeSearchEnabled = it
                            AppPrefs.setHomeSearchEnabled(context, it)
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.TouchApp,
                        title = "Çift Dokunarak Arama",
                        subtitle = "Boş ana ekranda çift dokununca tüm uygulamalar arama ile açılsın",
                        checked = doubleTapSearchEnabled,
                        onCheckedChange = {
                            doubleTapSearchEnabled = it
                            AppPrefs.setDoubleTapSearchEnabled(context, it)
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.SwapVert,
                        title = "Arama Çubuğu Konumu",
                        subtitle = if (searchBarPosition == AppPrefs.SEARCH_BAR_POS_TOP) {
                            "Saat widget'inin altinda"
                        } else {
                            "Google aramasinin altinda"
                        },
                        onClick = {
                            searchBarPosition = if (searchBarPosition == AppPrefs.SEARCH_BAR_POS_TOP) {
                                AppPrefs.SEARCH_BAR_POS_BOTTOM
                            } else {
                                AppPrefs.SEARCH_BAR_POS_TOP
                            }
                            AppPrefs.setSearchBarPosition(context, searchBarPosition)
                        },
                    )
                }
            }

            item { SettingsSectionTitle("Arama Kaynaklari") }
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Uygulamalar",
                        subtitle = "Temel arama kaynağı; sabit açık",
                        checked = appsSourceEnabled,
                        onCheckedChange = { AppPrefs.setSearchSourceAppsEnabled(context, true) },
                        enabled = false,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Category,
                        title = "Kategoriler",
                        subtitle = "Kategori ve klasör adları aramaya dahil edilir",
                        checked = categoriesSourceEnabled,
                        onCheckedChange = {
                            categoriesSourceEnabled = it
                            AppPrefs.setSearchSourceCategoriesEnabled(context, it)
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Settings,
                        title = "Android Ayarları",
                        subtitle = "Wi-Fi, bildirim erişimi ve kullanım erişimi gibi sistem ayarları. Ek izin gerekmez.",
                        checked = settingsSourceEnabled,
                        onCheckedChange = {
                            settingsSourceEnabled = it
                            AppPrefs.setSearchSourceSettingsEnabled(context, it)
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Person,
                        title = "Kişiler",
                        subtitle = "İzin verilmişse kişi sonuçları da listelenir",
                        checked = contactsSourceEnabled,
                        onCheckedChange = {
                            if (it) {
                                val hasContactsPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.READ_CONTACTS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasContactsPermission) {
                                    contactsSourceEnabled = true
                                    AppPrefs.setSearchSourceContactsEnabled(context, true)
                                    viewModel.enableContactsSource()
                                } else {
                                    contactsSourceEnabled = false
                                    pendingPermission = ContextualPermission.CONTACTS
                                }
                            } else {
                                contactsSourceEnabled = false
                                AppPrefs.setSearchSourceContactsEnabled(context, false)
                                viewModel.disableContactsSource()
                            }
                        },
                        enabled = !sourceOpInFlight,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Description,
                        title = "Dosya Adları",
                        subtitle = "Varsayilan kapali. Medya/indirme adlari icin Android dosya izni ister; icerik okunmaz",
                        checked = filesSourceEnabled,
                        onCheckedChange = {
                            if (it) {
                                filesSourceEnabled = false
                                pendingPermission = ContextualPermission.FILES
                            } else {
                                filesSourceEnabled = false
                                AppPrefs.setSearchSourceFilesEnabled(context, false)
                                viewModel.disableFilesSource()
                            }
                        },
                        enabled = !sourceOpInFlight,
                    )
                    // İndeksleme devam ederken kullanıcıya durum göster (spec Risk 4/8)
                    if (sourceOpInFlight) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "İndeks oluşturuluyor… Arka planda taranıyor, uygulamayı kullanmaya devam edebilirsin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item { SettingsSectionTitle("Sonuc Sirasi") }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.SwapVert,
                        title = "Varsayılan Sonuç Profili",
                        subtitle = when (rankingProfile) {
                            AppPrefs.SearchRankingProfile.BALANCED -> "Dengeli"
                            AppPrefs.SearchRankingProfile.CATEGORIES_FIRST -> "Kategoriler once"
                            AppPrefs.SearchRankingProfile.APPS_FIRST -> "Uygulamalar once"
                        },
                        onClick = {
                            rankingProfile = when (rankingProfile) {
                                AppPrefs.SearchRankingProfile.APPS_FIRST -> AppPrefs.SearchRankingProfile.BALANCED
                                AppPrefs.SearchRankingProfile.BALANCED -> AppPrefs.SearchRankingProfile.CATEGORIES_FIRST
                                AppPrefs.SearchRankingProfile.CATEGORIES_FIRST -> AppPrefs.SearchRankingProfile.APPS_FIRST
                            }
                            AppPrefs.setSearchRankingProfile(context, rankingProfile)
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.AutoMirrored.Filled.Sort,
                        title = "Kullanım Sıklığına Göre Sırala",
                        subtitle = "Eşit sonuçlarda daha sık açılan uygulama önce görünür",
                        checked = sortByUsage,
                        onCheckedChange = {
                            sortByUsage = it
                            AppPrefs.setSearchSortByUsage(context, it)
                        }
                    )
                }
            }

            // ── Gelişmiş Arama ────────────────────────────────────────────────
            item { SettingsSectionTitle("Gelişmiş Arama") }
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.Tune,
                        title = "Yakın Eşleşme Arama",
                        subtitle = "\"ytb\" → YouTube, \"wtsp\" → WhatsApp gibi yazımları da yakalar",
                        checked = fuzzyEnabled,
                        onCheckedChange = {
                            fuzzyEnabled = it
                            AppPrefs.setSearchFuzzyEnabled(context, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Translate,
                        title = "Türkçe Yazım Toleransı",
                        subtitle = "\"sube\" ile \"şube\" gibi yakın yazımlar eşleşebilir",
                        checked = phoneticEnabled,
                        onCheckedChange = {
                            phoneticEnabled = it
                            AppPrefs.setSearchPhoneticEnabled(context, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Search,
                        title = "Anlık Arama",
                        subtitle = "Her tuşta sonuç yenilenir; kapalıysa Enter ile arar",
                        checked = instantEnabled,
                        onCheckedChange = {
                            instantEnabled = it
                            AppPrefs.setSearchInstantEnabled(context, it)
                        }
                    )
                }
            }

            // ── Sonuç Görünümü ────────────────────────────────────────────────
            item { SettingsSectionTitle("Sonuç Görünümü") }
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.Image,
                        title = "Uygulama Simgeleri",
                        subtitle = "Arama sonuçlarında uygulama ikonlarını göster",
                        checked = showIcons,
                        onCheckedChange = {
                            showIcons = it
                            AppPrefs.setSearchShowIcons(context, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.AccountCircle,
                        title = "Kişi Fotoğrafları",
                        subtitle = "Rehber sonuçlarında profil fotoğraflarını göster",
                        checked = showContactAvatar,
                        onCheckedChange = {
                            showContactAvatar = it
                            AppPrefs.setSearchShowContactAvatar(context, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.search_settings_web_fallback_title),
                        subtitle = stringResource(R.string.search_settings_web_fallback_desc),
                        checked = webFallbackEnabled,
                        onCheckedChange = {
                            webFallbackEnabled = it
                            AppPrefs.setSearchWebFallbackEnabled(context, it)
                        }
                    )
                }
            }

            // ── Arama İstatistikleri ────────────────────────────────────────
            item { SettingsSectionTitle(stringResource(R.string.search_settings_stats_title)) }
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.search_settings_stats_title),
                        subtitle = stringResource(R.string.search_settings_stats_desc),
                        checked = searchStatsEnabled,
                        onCheckedChange = {
                            searchStatsEnabled = it
                            AppPrefs.setSearchStatsEnabled(context, it)
                        }
                    )
                }
            }

            // ── Limitler ─────────────────────────────────────────────────────
            item { SettingsSectionTitle("Limitler") }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.Numbers,
                        title = "Maksimum Sonuç Sayısı",
                        subtitle = "Şu an: $maxResults sonuç. Dokunarak 4, 6, 8 veya 10 yap",
                        onClick = {
                            maxResults = when (maxResults) {
                                4 -> 6; 6 -> 8; 8 -> 10; else -> 4
                            }
                            AppPrefs.setSearchMaxResults(context, maxResults)
                        }
                    )
                }
            }

            item {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)) {
                    Text(
                        text = "Not: Kisiler ilk kullanimda izin ister. Dosya aramasi kapali baslar; acildiginda Android'in izin verdigi medya/indirme adlari yerel indekse eklenir, dosya icerigi okunmaz.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
