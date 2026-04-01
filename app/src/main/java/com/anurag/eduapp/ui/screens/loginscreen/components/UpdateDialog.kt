package com.anurag.eduapp.ui.screens.loginscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anurag.eduapp.ui.theme.BackgroundPrimary
import com.anurag.eduapp.ui.theme.BrandPrimary
import com.anurag.eduapp.ui.theme.LocalDimensions
import com.anurag.eduapp.ui.theme.TextPrimary
import com.anurag.eduapp.ui.theme.TextSecondary

@Composable
fun UpdateAvailableDialog(
    isVisible: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float = 0f,
    modifier: Modifier = Modifier,
    onUpdateClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    if (!isVisible) return

    val dimens = LocalDimensions.current

    Surface(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .background(BackgroundPrimary, RoundedCornerShape(dimens.cornerRadiusLarge)),
        shape = RoundedCornerShape(dimens.cornerRadiusLarge),
        color = BackgroundPrimary,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.spaceLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = "Update available",
                modifier = Modifier
                    .width(56.dp)
                    .height(56.dp),
                tint = BrandPrimary
            )

            Spacer(modifier = Modifier.height(dimens.spaceMedium))

            // Title
            Text(
                text = "Update Available",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(dimens.spaceSmall))

            // Description
            Text(
                text = "A new version of EduAI is available. Please update to the latest version to enjoy new features and improvements.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(dimens.spaceLarge))

            // Progress indicator (only show if downloading)
            if (isDownloading) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = BrandPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(dimens.spaceSmall))

                    Text(
                        text = "${(downloadProgress * 100).toInt()}% Downloaded",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(dimens.spaceMedium))
                }
            }

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceMedium),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(dimens.spaceSmall)
            ) {
                if (!isDownloading) {
                    TextButton(
                        onClick = onDismissClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.buttonHeight)
                    ) {
                        Text("Later", color = BrandPrimary)
                    }

                    Button(
                        onClick = onUpdateClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.buttonHeight),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Text("Update Now", color = MaterialTheme.colorScheme.onPrimary)
                    }
                } else {
                    // Show only a disabled button during download
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.buttonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary.copy(alpha = 0.6f),
                            disabledContainerColor = BrandPrimary.copy(alpha = 0.6f)
                        )
                    ) {
                        Text("Downloading...", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
