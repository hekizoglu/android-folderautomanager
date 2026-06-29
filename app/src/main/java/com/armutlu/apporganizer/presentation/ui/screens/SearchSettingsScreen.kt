package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.SearchHistoryPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var homeAppSearchEnabled by remember { mutableStateOf(AppPrefs.isHomeAppSearchEnabled(context)) }
    var homeSearchEnabled by remember { mutableStateOf(AppPrefs.isHomeSearchEnabled(context)) }
    var doubleTapSearchEnabled by remember { mutableStateOf(AppPrefs.isDoubleTapSearchEnabled(context)) }
    var searchHistoryEnabled by remember { mutableStateOf(AppPrefs.isSearchHistoryEnabled(context)) }
    var appsSourceEnabled by remember { mutableStateOf(AppPrefs.isSearchSourceAppsEnabled(context)) }
    var categoriesSourceEnabled by remember { mutableStateOf(AppPrefs.isSearchSourceCategoriesEnabled(context)) }
    var contactsSourceEnabled by remember { mutableStateOf(AppPrefs.isSearchSourceContactsEnabled(context)) }
    var filesSourceEnabled by remember { mutableStateOf(AppPrefs.isSearchSourceFilesEnabled(context)) }
    var rankingProfile by remember { mutableStateOf(AppPrefs.getSearchRankingProfile(context)) }

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
                }
            }

            item { SettingsSectionTitle("Arama Kaynaklari") }
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Uygulamalar",
                        subtitle = "Yuklu uygulamalar aramaya dahil edilir",
                        checked = appsSourceEnabled,
                        onCheckedChange = {
                            appsSourceEnabled = it
                            AppPrefs.setSearchSourceAppsEnabled(context, it)
                        },
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
                        subtitle = "Opsiyonel. Ileride izin verilirse kisi kartlari aranabilir",
                        checked = contactsSourceEnabled,
                        onCheckedChange = {
                            contactsSourceEnabled = it
                            AppPrefs.setSearchSourceContactsEnabled(context, it)
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Description,
                        title = "Dosya Adlari",
                        subtitle = "Varsayilan kapali. Acilirsa cihaz dosya adlari indekslenir",
                        checked = filesSourceEnabled,
                        onCheckedChange = {
                            filesSourceEnabled = it
                            AppPrefs.setSearchSourceFilesEnabled(context, it)
                        },
                    )
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
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)) {
                    Text(
                        text = "Not: Kisi ve dosya kaynaklari privacy-first tasarlanir. Dosya aramasi varsayilan kapali tutulur ve gercek indeksleme ayri turde devreye alinacaktir.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
