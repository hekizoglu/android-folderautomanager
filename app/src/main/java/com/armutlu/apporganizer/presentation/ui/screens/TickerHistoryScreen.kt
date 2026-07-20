package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.home.TickerAction
import com.armutlu.apporganizer.presentation.viewmodel.TickerHistoryViewModel
import com.armutlu.apporganizer.utils.AppPrefs

/**
 * "Tüm haberler" arşiv ekranı — ana ekran haber şeridinde (ticker) gösterilen tüm öğelerin
 * mail kutusu benzeri kalıcı listesi. Okundu/okunmadı durumu, satıra dokununca ilgili sayfaya
 * gitme (mevcut [com.armutlu.apporganizer.domain.home.TickerActionRouter] YENİDEN KULLANILIR)
 * ve "tümünü okundu işaretle" aksiyonu içerir. Hassas ([sensitive]) satırlar, ana ekran
 * şeridiyle AYNI tercihe (`AppPrefs.isTickerSensitiveVisible`) göre alt başlığı gizler.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickerHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {},
    viewModel: TickerHistoryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val sensitiveVisible = AppPrefs.isTickerSensitiveVisible(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.ticker_history_title))
                        if (unreadCount > 0) {
                            Text(
                                text = stringResource(R.string.ticker_history_unread_badge, unreadCount),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (items.any { !it.isRead }) {
                        IconButton(onClick = { viewModel.markAllRead() }) {
                            Icon(
                                Icons.Filled.DoneAll,
                                contentDescription = stringResource(R.string.ticker_history_mark_all_read),
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.ticker_history_empty),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.ticker_history_empty_desc),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(items, key = { it.id }) { item ->
                TickerHistoryRow(
                    title = item.title,
                    subtitle = if (item.sensitive && !sensitiveVisible) {
                        stringResource(R.string.ticker_history_sensitive_hidden)
                    } else {
                        item.subtitle
                    },
                    icon = item.icon,
                    isRead = item.isRead,
                    createdAt = item.createdAt,
                    onClick = {
                        viewModel.markRead(item.id)
                        if (item.action != TickerAction.None) {
                            viewModel.resolveRoute(item.action)?.let { route -> onNavigateToRoute(route) }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TickerHistoryRow(
    title: String,
    subtitle: String?,
    icon: String,
    isRead: Boolean,
    createdAt: Long,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = icon, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    fontWeight = if (isRead) FontWeight.Normal else FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = relativeTimeLabel(createdAt),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Göreli zaman etiketi ("Az önce" / "5 dk önce" / "3 sa önce" / "2 gün önce"). Saf fonksiyon. */
@Composable
private fun relativeTimeLabel(createdAt: Long): String {
    val diffMs = (System.currentTimeMillis() - createdAt).coerceAtLeast(0L)
    val minutes = diffMs / 60_000
    val hours = diffMs / 3_600_000
    val days = diffMs / 86_400_000
    return when {
        minutes < 1 -> stringResource(R.string.ticker_history_time_just_now)
        minutes < 60 -> stringResource(R.string.ticker_history_time_minutes_ago, minutes.toInt())
        hours < 24 -> stringResource(R.string.ticker_history_time_hours_ago, hours.toInt())
        else -> stringResource(R.string.ticker_history_time_days_ago, days.toInt())
    }
}
