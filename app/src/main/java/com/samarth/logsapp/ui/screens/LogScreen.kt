package com.samarth.logsapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.samarth.logsapp.ui.components.SaveStatusIndicator
import com.samarth.logsapp.ui.components.TripleTapBox
import com.samarth.logsapp.ui.theme.EntryHeaderStyle

/**
 * The home screen — always today's log. Opens blank the first time you
 * write on a given day; reopening the app later the same day loads what
 * you already wrote so far, cursor ready to keep appending. Triple-tapping
 * anywhere empty switches to the month-grid history.
 */
@Composable
fun LogScreen(
    viewModel: LogViewModel,
    onTripleTapToHistory: () -> Unit
) {
    val body by viewModel.body.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    TripleTapBox(onTripleTap = onTripleTapToHistory) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(text = viewModel.dateLabel, style = EntryHeaderStyle.Date)
                SaveStatusIndicator(status = saveStatus)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 36.dp)
            ) {
                if (body.isEmpty()) {
                    Text(
                        text = "Start writing…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = EntryHeaderStyle.PlaceholderColor
                    )
                }
                BasicTextField(
                    value = body,
                    onValueChange = viewModel::onBodyChanged,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
