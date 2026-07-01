package com.samarth.logsapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.samarth.logsapp.ui.screens.SaveStatus
import com.samarth.logsapp.ui.theme.AccentColor
import com.samarth.logsapp.ui.theme.ErrorColor
import com.samarth.logsapp.ui.theme.MutedColor

/**
 * Small dot + label, matching the web Notes editor's corner status
 * indicator exactly: muted dot while saved locally, pulsing accent dot
 * while syncing, solid accent dot once synced, error-colored if a sync
 * attempt failed (the .md file itself is still safe either way).
 */
@Composable
fun SaveStatusIndicator(status: SaveStatus, modifier: Modifier = Modifier) {
    val (dotColor, label) = when (status) {
        SaveStatus.IDLE -> MutedColor to ""
        SaveStatus.SAVING -> MutedColor to "saving…"
        SaveStatus.SAVED_LOCALLY -> MutedColor to "saved on device"
        SaveStatus.SYNCING -> AccentColor to "syncing…"
        SaveStatus.SYNCED -> AccentColor to "synced"
        SaveStatus.SYNC_FAILED -> ErrorColor to "saved on device · will sync later"
    }

    if (label.isEmpty()) return

    val labelColor = if (status == SaveStatus.SYNC_FAILED) ErrorColor else MutedColor

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (status == SaveStatus.SYNCING) {
            PulsingDot(color = dotColor)
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

/** Matches the web editor's `@keyframes pulse` — 1.1s ease-in-out, opacity 1 -> ~0.3 -> 1, infinite. */
@Composable
private fun PulsingDot(color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "sync_pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sync_pulse_alpha"
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
            .alpha(alpha)
    )
}
