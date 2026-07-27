package com.armutlu.apporganizer.presentation.ui.launcher.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.home.HomeMissionSummary
import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessTab
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessUiState
import com.armutlu.apporganizer.domain.models.HomeSectionId
import com.armutlu.apporganizer.presentation.ui.launcher.HomeMissionCard

/**
 * D240 — SmartAccessCard tek bir sekmeli (NOW/RECENT/NOTIFICATIONS) birim olduğu için editördeki
 * GOOGLE_SEARCH/FAVORITES/SUGGESTIONS/RECENT_NOTIFICATIONS/RECENT_APPS/ANDROID_WIDGETS/
 * ASSISTANT_INSIGHTS/TICKER_OR_STATS section'larının hiçbiri Hero'da ayrı bir composable'a karşılık
 * gelmiyor (grep doğrulandı: hero/ paketinde bu ID'lere referans yok). Bu grup "SmartAccessCard'ın
 * temsil ettiği section'lar" olarak ele alınır: grubun en az bir üyesi görünürse kart gösterilir ve
 * kartın sırası, grup içindeki en önde (en küçük order) yer alan görünür üyenin konumuna göre
 * belirlenir. Küçük/güvenli birleştirme — tam per-tab ayrıştırma büyük refactor gerektirir, bu
 * turun kapsamı dışında bırakıldı (bkz. rapor).
 */
private val SMART_ACCESS_GROUP = setOf(
    HomeSectionId.GOOGLE_SEARCH,
    HomeSectionId.FAVORITES,
    HomeSectionId.SUGGESTIONS,
    HomeSectionId.RECENT_NOTIFICATIONS,
    HomeSectionId.RECENT_APPS,
    HomeSectionId.ANDROID_WIDGETS,
    HomeSectionId.ASSISTANT_INSIGHTS,
    HomeSectionId.TICKER_OR_STATS,
)

/** Hero'nun render edebildiği "sanal" bloklar — contentOrder içindeki gerçek section'lardan türetilir. */
private enum class HeroBlock { CLOCK, MISSIONS_AND_SCORE, MISSIONS, SMART_ACCESS }

/**
 * Editörün gerçek `contentOrder`'ından (dashboardContentOrder(config)) Hero'nun render edebileceği
 * blokların sırasını çıkarır. Gizlenen section'lar contentOrder'da hiç yer almaz (bkz.
 * HomeSectionRenderer.dashboardContentOrder — visible filtresi zaten uygulanmış), bu yüzden burada
 * sadece "bu blok listede var mı" kontrolü yeterli.
 */
private fun heroBlockOrder(contentOrder: List<HomeSectionId>): List<HeroBlock> {
    val result = mutableListOf<HeroBlock>()
    for (sectionId in contentOrder) {
        val block = when {
            sectionId == HomeSectionId.CLOCK -> HeroBlock.CLOCK
            sectionId == HomeSectionId.MISSIONS_AND_SCORE -> HeroBlock.MISSIONS_AND_SCORE
            sectionId == HomeSectionId.MISSIONS -> HeroBlock.MISSIONS
            sectionId in SMART_ACCESS_GROUP -> HeroBlock.SMART_ACCESS
            else -> null
        } ?: continue
        if (block !in result) result += block
    }
    return result
}

@Composable
internal fun HeroDashboardPage(
    pulse: HomePulseSummary?,
    smartAccess: SmartAccessUiState,
    pendingClassificationCount: Int = 0,
    contentOrder: List<HomeSectionId> = HomeSectionId.entries,
    missionSummary: HomeMissionSummary? = null,
    onOpenWeeklyReport: () -> Unit,
    onClockLongPress: () -> Unit,
    onOpenPulse: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onOpenClassificationReview: () -> Unit = {},
    onOpenMissions: () -> Unit = {},
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val spec = HomeHeroLayoutPolicy.resolve(
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
        fontScale = configuration.fontScale,
    )
    var selectedTab by rememberSaveable { mutableStateOf(SmartAccessTab.NOW) }
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val contentWidth = (maxWidth - (spec.horizontalPaddingDp * 2).dp)
            .coerceAtMost(spec.contentMaxWidthDp.dp)
            .coerceAtLeast(0.dp)
        Column(
            modifier = Modifier
                .width(contentWidth)
                .then(if (spec.scrollEnabled) Modifier.verticalScroll(scrollState) else Modifier)
                .padding(top = if (spec.profile == HomeHeroProfile.COMPACT_PHONE) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HomeHeroTokens.SectionGap),
        ) {
            // D240 — statik sıra yerine editör tercihinden (contentOrder) türetilen dinamik blok
            // sırası; gizlenen bloklar heroBlockOrder'da hiç görünmediği için otomatik atlanır.
            heroBlockOrder(contentOrder).forEach { block ->
                when (block) {
                    HeroBlock.CLOCK -> HeroClockCard(
                        spec = spec,
                        onClick = onOpenWeeklyReport,
                        onLongClick = onClockLongPress,
                    )
                    HeroBlock.MISSIONS_AND_SCORE -> HeroDigitalLifeCard(
                        summary = pulse,
                        spec = spec,
                        onClick = onOpenPulse,
                    )
                    HeroBlock.MISSIONS -> HomeMissionCard(
                        summary = missionSummary,
                        onClick = onOpenMissions,
                    )
                    HeroBlock.SMART_ACCESS -> SmartAccessCard(
                        state = smartAccess,
                        spec = spec,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onOpenUsageSettings = onOpenUsageAccessSettings,
                        onOpenNotificationSettings = onOpenNotificationAccessSettings,
                        onLaunchApp = onLaunchApp,
                        onAppLongClick = onAppLongClick,
                    )
                }
            }
        }
    }
}

/**
 * P1.2: Pending classification review badge card.
 * Shows count of apps awaiting category review, tappable to navigate to review screen.
 */
@Composable
private fun PendingClassificationBadge(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .clickable(enabled = count > 0) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Kategori İnceleme",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "$count uygulama kategori onayı bekliyor",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
            }
            // Badge gösterimi
            Text(
                text = count.toString(),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
                    .size(32.dp)
                    .padding(8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiary,
            )
        }
    }
}
