package com.ncert7.mathandsciencelab.ui.screens.conceptscreen.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ncert7.mathandsciencelab.R
import com.ncert7.mathandsciencelab.ui.theme.HeaderGradientStart
import com.ncert7.mathandsciencelab.ui.theme.LocalDimensions
import com.ncert7.mathandsciencelab.ui.theme.TextOnPrimary

/**
 * simulation header with fixed touch responsiveness.
 * Uses a Box with explicit clickable modifier to ensure touches are captured
 * even when a WebView is active.
 */
@Composable
fun SimulationHeader(
    title: String,
    onBackClick: () -> Unit
) {
    val dimens = LocalDimensions.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1000f) // Extremely high priority for touch events
            .background(HeaderGradientStart)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp) // Slightly taller for better touch targets
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = TextOnPrimary,
                    modifier = Modifier.size(dimens.iconMedium)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextOnPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
        }
    }
}
