package com.openlauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.openlauncher.app.data.AppDrawerFilter
import com.openlauncher.app.model.AppInfo
import com.openlauncher.app.util.TapSound

/**
 * Unified "pick a target app" dialog used by every app-assignment flow in the
 * launcher (sidebar shortcuts, app-shortcuts widget, floating panel buttons,
 * radio app, gestures, auto-launch). It merges the old per-feature pickers into
 * one window:
 *
 *  - search box (matches app name or package)
 *  - the app drawer's filter chips (Selected / Tools / Installed / System / All)
 *  - an adaptive icon grid with the currently assigned app highlighted
 *  - optional clear action with a custom label ("CLEAR", "AUTO-DETECT", ...)
 *  - tap sound on every control
 *
 * The host decides what happens on [onSelect] / [onClear] and dismisses itself;
 * [clearLabel] = null hides the clear action entirely.
 */
@Composable
fun AppPickerDialog(
    title: String,
    currentPackage: String,
    apps: List<AppInfo>,
    accent: Color,
    isDayMode: Boolean,
    selectedAppPackages: List<String> = emptyList(),
    toolsAppPackages: List<String> = emptyList(),
    clearLabel: String? = "CLEAR",
    onSelect: (String) -> Unit,
    onClear: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg     = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)
    val searchBg     = if (isDayMode) Color(0xFFF1F3F5) else Color(0xFF1A1A1A)
    val textColor    = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)
    val subColor     = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)
    val tileBg       = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF161616)
    val tileBorder   = if (isDayMode) Color(0xFFCED4DA) else Color(0xFF242424)
    val chipContainer = effectiveAccent.copy(alpha = if (isDayMode) 0.12f else 0.22f)
    val chipLabelSelected = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)

    var query  by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppDrawerFilter.ALL) }

    val filtered = remember(apps, query, filter, selectedAppPackages, toolsAppPackages) {
        val byName = if (query.isBlank()) apps
                     else apps.filter {
                         it.appName.contains(query, ignoreCase = true) ||
                         it.packageName.contains(query, ignoreCase = true)
                     }
        when (filter) {
            AppDrawerFilter.SELECTED -> byName.filter { it.packageName in selectedAppPackages }
            AppDrawerFilter.TOOLS    -> byName.filter { it.packageName in toolsAppPackages }
            AppDrawerFilter.USER     -> byName.filter { !it.isSystemApp }
            AppDrawerFilter.SYSTEM   -> byName.filter { it.isSystemApp }
            AppDrawerFilter.ALL      -> byName
        }
    }

    val currentApp = apps.firstOrNull { it.packageName == currentPackage }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 380.dp, max = 720.dp)
                .fillMaxWidth(0.94f)
                .heightIn(max = 560.dp)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(10.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            // ── Header: title + current selection + clear + close ──────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text          = title.uppercase(),
                        color         = effectiveAccent,
                        fontSize      = 11.sp,
                        letterSpacing = 2.sp,
                        fontWeight    = FontWeight.Bold,
                        maxLines      = 1,
                        overflow      = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (currentApp != null) "Current: ${currentApp.appName}"
                               else "No app assigned",
                        color = subColor,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (clearLabel != null && currentPackage.isNotBlank()) {
                    TextButton(
                        onClick = {
                            TapSound.play()
                            onClear()
                            onDismiss()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            clearLabel,
                            color = Color(0xFFE03131),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                IconButton(
                    onClick = { TapSound.play(); onDismiss() },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = subColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ── Search ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(searchBg)
                    .border(1.dp, if (isDayMode) Color(0xFFD0D0D0) else Color(0xFF282828), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = subColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = TextStyle(color = textColor, fontSize = 12.sp),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    "Search by name or package...",
                                    color = if (isDayMode) Color(0xFF999999) else Color(0xFF555555),
                                    fontSize = 12.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { TapSound.play(); query = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "Clear search",
                                tint = subColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── App drawer filter chips ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppDrawerFilter.entries.forEach { f ->
                    TapFilterChip(
                        selected = filter == f,
                        onClick  = { filter = f },
                        label = {
                            Text(
                                when (f) {
                                    AppDrawerFilter.SELECTED -> "Selected"
                                    AppDrawerFilter.TOOLS    -> "Tools"
                                    AppDrawerFilter.USER     -> "Installed"
                                    AppDrawerFilter.SYSTEM   -> "System"
                                    AppDrawerFilter.ALL      -> "All"
                                },
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipContainer,
                            selectedLabelColor     = chipLabelSelected,
                            labelColor             = subColor
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── App grid ────────────────────────────────────────────────────────
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.isBlank()) "No apps in this category" else "No apps match \"$query\"",
                        color = subColor,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 84.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppPickTile(
                            app          = app,
                            isCurrent    = app.packageName == currentPackage,
                            accent       = effectiveAccent,
                            textColor    = textColor,
                            subColor     = subColor,
                            tileBg       = tileBg,
                            tileBorder   = tileBorder,
                            isDayMode    = isDayMode,
                            onPick       = { onSelect(app.packageName) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Footer count ────────────────────────────────────────────────────
            Text(
                text          = "${filtered.size} app${if (filtered.size == 1) "" else "s"}",
                color         = subColor.copy(alpha = 0.8f),
                fontSize      = 9.sp,
                letterSpacing = 1.sp,
                modifier      = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun AppPickTile(
    app: AppInfo,
    isCurrent: Boolean,
    accent: Color,
    textColor: Color,
    subColor: Color,
    tileBg: Color,
    tileBorder: Color,
    isDayMode: Boolean,
    onPick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCurrent) accent.copy(alpha = if (isDayMode) 0.12f else 0.2f) else tileBg)
            .border(1.dp, if (isCurrent) accent else tileBorder, RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { TapSound.play(); onPick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            val bmp = remember(app.icon) {
                runCatching { app.icon.toBitmap(72, 72) }.getOrNull()
            }
            if (bmp != null) {
                Icon(
                    painter = BitmapPainter(bmp.asImageBitmap()),
                    contentDescription = app.appName,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    Icons.Default.Apps,
                    null,
                    tint = accent,
                    modifier = Modifier.size(40.dp)
                )
            }
            if (isCurrent) {
                Icon(
                    Icons.Default.CheckCircle,
                    "Current",
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = app.appName,
            color = textColor,
            fontSize = 9.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp
        )
        Text(
            text = app.packageName,
            color = subColor,
            fontSize = 7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
