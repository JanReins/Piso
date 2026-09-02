package com.janreins.piso.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.janreins.piso.ui.theme.ExpenseRed
import com.janreins.piso.ui.theme.TealPrimary
import com.janreins.piso.ui.theme.WarningAmber

/**
 * Clean animated progress bar with optional percentage text and tone.
 */
@Composable
fun ProgressBarWithPercent(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    showLabel: Boolean = true,
    labelLeading: String? = null,
    customColor: Color? = null
) {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = clampedFraction, label = "progress")

    val barColor = customColor ?: when {
        fraction > 1.0f -> ExpenseRed
        fraction >= 0.8f -> WarningAmber
        else -> TealPrimary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showLabel || labelLeading != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (labelLeading != null) {
                    Text(
                        text = labelLeading,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (showLabel) {
                    val percentInt = (fraction * 100).toInt()
                    Text(
                        text = "$percentInt%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = barColor
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(barColor)
            )
        }
    }
}
