package com.samarth.logsapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.samarth.logsapp.ui.theme.MutedColor
import com.samarth.logsapp.ui.theme.TextColor

/**
 * Reached by double-tapping the top-right corner of the log or month-grid
 * screens. "back" is the primary action (bold, up top); "sign out" is a
 * secondary, faint action below it - mirrors Flow Timer's account screen.
 */
@Composable
fun AccountScreen(
    email: String?,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "account",
            color = TextColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = email ?: "",
            color = MutedColor,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "back",
            color = TextColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onBack() }
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "sign out",
            color = MutedColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSignOut() }
                .padding(8.dp)
        )
    }
}
