package com.armutlu.apporganizer.presentation.ui.launcher.hero

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.R

@Composable
internal fun HeroSearchCard(
    spec: HomeHeroLayoutSpec,
    onOpenSearch: () -> Unit,
    onOpenSources: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumGlassSurface(
        modifier = modifier
            .testTag("hero_search_card")
            .fillMaxWidth()
            .height(spec.searchHeightDp.dp)
            .clickable(onClick = onOpenSearch)
            .semantics { role = Role.Button },
        cornerRadius = HomeHeroTokens.SearchCorner,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = stringResource(R.string.search_overlay_title),
                color = Color.White.copy(alpha = .92f),
                fontWeight = FontWeight.SemiBold,
                fontSize = HomeHeroTokens.CardTitleTextSize,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = .72f), modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.hero_search_placeholder),
                    color = Color.White.copy(alpha = .58f),
                    fontSize = HomeHeroTokens.BodyTextSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.hero_search_sources),
                    tint = Color.White.copy(alpha = .78f),
                    modifier = Modifier
                        .testTag("hero_search_sources")
                        .size(48.dp)
                        .clickable(onClick = onOpenSources)
                        .padding(13.dp),
                )
            }
        }
    }
}
