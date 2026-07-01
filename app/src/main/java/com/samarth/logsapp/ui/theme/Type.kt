package com.samarth.logsapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System default fonts for now — can be swapped for Inter/Source Serif 4
// (matching the web journal) by adding font resources later.
val NotesTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp
    )
)

/**
 * Standalone styles matching the web Notes editor's header exactly
 * (.date / .time CSS rules), kept separate from Material3's Typography
 * slots above since neither maps cleanly onto "serif date heading" or
 * "small uppercase muted timestamp."
 */
object EntryHeaderStyle {
    // Web: font-family Source Serif 4/Georgia/serif, weight 600, 1.9rem (~30sp)
    val Date = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        letterSpacing = (-0.2).sp
    )

    // Web: 0.78rem (~13sp), muted, uppercase, letter-spacing 0.06em
    val Time = TextStyle(
        fontSize = 13.sp,
        letterSpacing = 0.8.sp
    )

    // Web placeholder color is a dim near-background gray, distinct from
    // the brighter MutedColor used for the time label / status text.
    val PlaceholderColor = androidx.compose.ui.graphics.Color(0xFF3A3F48)
}
