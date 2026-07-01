package com.samarth.logsapp.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

private const val TRIPLE_TAP_WINDOW_MILLIS = 600L

/**
 * Wraps [content] and calls [onTripleTap] when the user taps three times
 * in quick succession anywhere inside this box. Used to toggle between the
 * write screen and the history screen on a tap in empty space, without any
 * visible button — matching the "open, write, leave" minimalism of the app.
 *
 * Taps that land on interactive children (e.g. the text field) still pass
 * through normally; this only adds an additional gesture recognizer on top.
 */
@Composable
fun TripleTapBox(
    onTripleTap: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tapTimestamps = remember { mutableListOf<Long>() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val now = System.currentTimeMillis()
                        tapTimestamps.add(now)
                        // Keep only taps within the rolling window
                        while (tapTimestamps.isNotEmpty() &&
                            now - tapTimestamps.first() > TRIPLE_TAP_WINDOW_MILLIS
                        ) {
                            tapTimestamps.removeAt(0)
                        }
                        if (tapTimestamps.size >= 3) {
                            tapTimestamps.clear()
                            onTripleTap()
                        }
                    }
                )
            }
    ) {
        content()
    }
}
