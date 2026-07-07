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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.presentation.viewmodel.SearchSettingsViewModel
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.SearchHistoryPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SearchSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val sourceOpInFlight by viewModel.sourceOpInFlight.collectAsState()
    var homeAppSearchEnabled by remember { mutableStateOf(AppPrefs.isHomeAppSearchEnabled(context)) }
    var homeSearchEnabled by remember { mutableStateOf(AppPrefs.isHomeSearchEnabled(context)) }
    var doubleTapSearchEnabled by remember { mutableStateOf(AppPrefs.isDoubleTapSearchEnabled(context)) }
    var searchHistoryEnabled by remember { mutableStateOf(AppPrefs.isSearchHistoryEnabled(context)) }
    val appsSourceEnabled = true
    var categoriesSourceEnabled by remember { mutableStateOf(AppPrefs.isSearchSourceCategoriesEnabled(context)) }
    var contactsSourceEnabled by remember { mutableStateOf(AppPrefs.isSearchSourceContactsEnabled(context)) }
    var filesSourceEnabled by remember { mutableStateOf(AppPrefs.isSearchSourceFilesEnabled(context)) }
    var rankingProfile by remember { mutableStateOf(AppPrefs.getSearchRankingProfile(context)) }
    var searchBarPosition by remember { mutableStateOf(AppPrefs.getSearchBarPosition(context)) }
    var pendingPermission by remember { mutableStateOf<ContextualPermission?>(null) }

    // Gelişmiş arama ayarları
    var fuzzyEnabled      by remember { mutableStateOf(AppPrefs.isSearchFuzzyEnabled(context)) }
    var phoneticEnabled   by remember { mutableStateOf(AppPrefs.isSearchPhoneticEnabled(context)) }
    var instantEnabled    by remember { mutableStateOf(AppPrefs.isSearchInstantEnabled(context)) }
    var sortByUsage       by remember { mutableStateOf(AppPrefs.isSearchSortByUsage(context)) }
    var maxResults        by remember { mutableStateOf(AppPrefs.getSearchMaxResults(context)) }
    var historyLimit      by remember { mutableStateOf(AppPrefs.getSearchHistoryLimit(context)) }
    var showIcons         by remember { mutableStateOf(AppPrefs.isSearchShowIcons(context)) }
    var showContactAvatar by remember { mutableStateOf(AppPrefs.isSearchShowContactAvatar(context)) }

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
                        title = "Ana Ekran Uygulama Aramasi",
                        subtitle = "Premium arama cubugu icinde uygulama sonuclari gosterilir",
                        checked = homeAppSearchEnabled,
                        onCheckedChange = {
                            homeAppSearchEnabled = it
                            AppPrefs.setHomeAppSearchEnabled(context, it)
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Category,
                        title = "Klasor ve Kategori Aramasi",
                        subtitle = "Ana ekranda klasor filtresi ve kategori eslesmeleri acik kalir",
                        checked = homeSearchEnabled,
                        onCheckedChange = {
                            homeSearchEnabled = it
                            AppPrefs.setHomeSearchEnabled(context, it)
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.TouchApp,
                        title = "Cift Tikla Arama",
                        subtitle = "Bos ana ekranda cift tiklama tum uygulamalari arama ile acsin",
                        checked = doubleTapSearchEnabled,
                        onCheckedChange = {
                            doubleTapSearchEnabled = it
                            AppPrefs.setDoubleTapSearchEnabled(context, it)
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.History,
                        title = "Arama Gecmisi",
                        subtitle = "Son sorgular kaydedilsin ve onerilsin",
                        checked = searchHistoryEnabled,
                        onCheckedChange = {
                            searchHistoryEnabled = it
                            AppPrefs.setSearchHistoryEnabled(context, it)
                            if (!it) SearchHistoryPrefs.clear(context)
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    // Geçmişi Temizle — her zaman görünür (spec Risk 10: geçmiş sadece cihazda)
                    SettingsButtonRow(
                        icon = Icons.Default.Delete,
                        title = "Geçmişi Temizle",
                        subtitle = "Kayıtlı tüm arama sorgularını siler — geçmiş yalnızca cihazda tutulur",
                        onClick = {
                            SearchHistoryPrefs.clear(context)
                            android.widget.Toast.makeText(context, "Arama geçmişi temizlendi", android.widget.Toast.LENGTH_SHORT).show()
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.SwapVert,
                        title = "Arama Cubugu Konumu",
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
                        subtitle = "Temel arama kaynagi; kapatilamaz",
                        checked = appsSourceEnabled,
                        onCheckedChange = { AppPrefs.setSearchSourceAppsEnabled(context, true) },
                        enabled = false,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Category,
                        title = "Kategoriler",
                        subtitle = "Kategori ve klasor isimleri aramaya dahil edilir",
                        checked = categoriesSourceEnabled,
                        onCheckedChange = {
                            categoriesSourceEnabled = it
                            AppPrefs.setSearchSourceCategoriesEnabled(context, it)
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Person,
                        title = "Kisiler",
                        subtitle = "Izin verilmisse ana ekran aramasinda kisiler de listelenir",
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
                        title = "Dosya Adlari",
                        subtitle = "Varsayilan kapali. Acilirsa cihaz dosya adlari arka planda indekslenir",
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
                        title = "Varsayilan Sonuc Profili",
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
                        subtitle = "Eşit sonuçlarda daha sık açılan uygulama üste çıkar",
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
                        title = "Fuzzy Arama",
                        subtitle = "\"ytb\" → YouTube, \"wtsp\" → WhatsApp — yakın eşleşmeleri bul",
                        checked = fuzzyEnabled,
                        onCheckedChange = {
                            fuzzyEnabled = it
                            AppPrefs.setSearchFuzzyEnabled(context, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Translate,
                        title = "Türkçe Fonetik Toleransı",
                        subtitle = "\"sube\" ile \"şube\" bulunur — ş→s, ü→u, ö→o, ç→c, ğ→g",
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
                        subtitle = "Her tuşta sonuç güncellenir — kapalıysa Enter'da arar",
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
                        title = "Uygulama İkonları",
                        subtitle = "Arama sonuçlarında uygulama simgelerini göster",
                        checked = showIcons,
                        onCheckedChange = {
                            showIcons = it
                            AppPrefs.setSearchShowIcons(context, it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.AccountCircle,
                        title = "Kişi Avatarları",
                        subtitle = "Rehber sonuçlarında profil fotoğraflarını göster",
                        checked = showContactAvatar,
                        onCheckedChange = {
                            showContactAvatar = it
                            AppPrefs.setSearchShowContactAvatar(context, it)
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
                        subtitle = "Şu an: $maxResults sonuç — dokunarak değiştir (4 / 6 / 8 / 10)",
                        onClick = {
                            maxResults = when (maxResults) {
                                4 -> 6; 6 -> 8; 8 -> 10; else -> 4
                            }
                            AppPrefs.setSearchMaxResults(context, maxResults)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.History,
                        title = "Arama Geçmişi Limiti",
                        subtitle = "Şu an: $historyLimit sorgu saklanır — dokunarak değiştir (5 / 8 / 12 / 20)",
                        onClick = {
                            historyLimit = when (historyLimit) {
                                5 -> 8; 8 -> 12; 12 -> 20; else -> 5
                            }
                            AppPrefs.setSearchHistoryLimit(context, historyLimit)
                        }
                    )
                }
            }

            item {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)) {
                    Text(
                        text = "Not: Kişi kaynağı ilk açılışta izin ister. Dosya araması varsayılan kapalıdır ve açıldığında yalnızca ad/yol bilgisi yerel indekse eklenir.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
