package com.anurag.eduapp.ui.screens.homescreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import com.anurag.eduapp.R
import com.anurag.eduapp.ui.theme.LocalDimensions
import com.anurag.eduapp.ui.theme.SubjectCardGradientCenter
import com.anurag.eduapp.ui.theme.SubjectCardGradientEnd
import com.anurag.eduapp.ui.theme.SubjectCardGradientStart
import com.anurag.eduapp.ui.theme.White
import com.anurag.eduapp.utils.getLocalizedSubjectName

@Composable
fun HomeScreenSubjectCard(
    subject: String,
    onChangeClick: () -> Unit = {}
) {
    val dimes = LocalDimensions.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        SubjectCardGradientStart,
                        SubjectCardGradientCenter,
                        SubjectCardGradientEnd
                    )
                ),
                shape = RoundedCornerShape(dimes.cornerRadiusRound)
            )
            .clickable(onClick = onChangeClick)
            .padding(dimes.screenPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Default.MenuBook,
            contentDescription = stringResource(R.string.book_icon),
            tint = White,
            modifier = Modifier.size(dimes.iconLarge)
        )

        Spacer(modifier = Modifier.width(dimes.spaceSmall))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.current_subject),
                style = MaterialTheme.typography.titleSmall,
                color = White.copy(alpha = 0.7f)
            )
            Text(
                text = getLocalizedSubjectName(subject),
                style = MaterialTheme.typography.titleLarge,
                color = White
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.change), color = White.copy(alpha = 0.7f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.arrow_icon),
                tint = White.copy(alpha = 0.7f)
            )
        }
    }
}