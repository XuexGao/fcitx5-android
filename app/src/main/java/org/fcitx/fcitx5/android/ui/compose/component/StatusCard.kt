/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 *
 * Ported from KernelSU-Style-UI-Kit (Apache-2.0): the big status card on the home page. The
 * template uses it for its permission state; here it reports whether the input method is ready.
 */
package org.fcitx.fcitx5.android.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fcitx.fcitx5.android.ui.compose.LocalUiMode
import org.fcitx.fcitx5.android.ui.compose.UiMode
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * A full-width status card: green when everything is set up, red when something is missing. Tapping
 * it takes the user to wherever the missing piece is configured.
 *
 * The miuix variant keeps the template's fixed status colours (a banner look that works in both
 * light and dark themes); the Material variant uses the theme's container roles instead.
 */
@Composable
fun StatusCard(
    ready: Boolean,
    readyTitle: String,
    missingTitle: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixStatusCard(ready, readyTitle, missingTitle, subtitle, onClick)
        UiMode.Material -> MaterialStatusCard(ready, readyTitle, missingTitle, subtitle, onClick)
    }
}

@Composable
private fun MiuixStatusCard(
    ready: Boolean,
    readyTitle: String,
    missingTitle: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val iconColor = if (ready) Color(0xFF36D167) else Color(0xFFF72727)
    val containerColor = if (ready) Color(0xFFDFFAE4) else Color(0xFFF8E2E2)
    val textColor = Color(0xFF111111)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(color = containerColor),
            onClick = onClick,
            showIndication = true,
            pressFeedbackType = PressFeedbackType.Tilt,
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(27.dp, 31.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Icon(
                        modifier = Modifier.size(110.dp),
                        imageVector = if (ready) Icons.Rounded.CheckCircleOutline
                        else Icons.Rounded.Cancel,
                        tint = iconColor,
                        contentDescription = null,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp, 14.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Column {
                        Text(
                            text = if (ready) readyTitle else missingTitle,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(text = subtitle, fontSize = 15.sp, color = textColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialStatusCard(
    ready: Boolean,
    readyTitle: String,
    missingTitle: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val containerColor = if (ready) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.errorContainer
    val contentColor = contentColorFor(containerColor)
    val title = if (ready) readyTitle else missingTitle

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
    ) {
        ListItem(
            leadingContent = {
                MaterialIcon(
                    imageVector = if (ready) Icons.Rounded.CheckCircleOutline
                    else Icons.Rounded.Cancel,
                    contentDescription = title,
                )
            },
            supportingContent = {
                MaterialText(text = subtitle, style = MaterialTheme.typography.bodyMedium)
            },
            verticalAlignment = Alignment.CenterVertically,
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = contentColor,
                leadingContentColor = contentColor,
                supportingContentColor = contentColor.copy(alpha = 0.7f),
            ),
            content = {
                MaterialText(text = title, style = MaterialTheme.typography.titleMediumEmphasized)
            },
        )
    }
}
