package com.openlauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.openlauncher.app.ui.theme.LocalDayMode
import com.openlauncher.app.ui.theme.LocalUiScales

/**
 * Confirmation dialog styled like the rest of the launcher (same rounded panel,
 * background/border colours and typography as the other dialogs).
 *
 * Scales with the user's Text Scale (font sizes) and UI Scale (panel sizes,
 * spacing, corners) so it stays consistent with the interface scale settings.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDayMode = LocalDayMode.current
    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    val dialogBg     = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF1E1E1E)
    val titleColor   = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)
    val subColor     = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)
    val confirmColor = if (isDayMode) Color(0xFFE03131) else Color(0xFFFF5252)
    val cancelColor  = if (isDayMode) Color(0xFF666666) else Color(0xFFAAAAAA)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = (400 * us).dp)
                .clip(RoundedCornerShape((10 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((10 * us).dp))
                .padding((18 * us).dp)
        ) {
            Text(
                title,
                color = titleColor,
                fontSize = (15 * ts).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (1 * ts).sp
            )
            Spacer(Modifier.height((10 * us).dp))
            Text(
                message,
                color = subColor,
                fontSize = (12 * ts).sp,
                lineHeight = (18 * ts).sp
            )
            Spacer(Modifier.height((16 * us).dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CANCEL",
                    color = cancelColor,
                    fontSize = (11 * ts).sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (1 * ts).sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape((6 * us).dp))
                        .tapClickable(onClick = onDismiss)
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp)
                )
                Spacer(Modifier.width((8 * us).dp))
                Text(
                    confirmLabel.uppercase(),
                    color = confirmColor,
                    fontSize = (11 * ts).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1 * ts).sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape((6 * us).dp))
                        .tapClickable(onClick = onConfirm)
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp)
                )
            }
        }
    }
}
