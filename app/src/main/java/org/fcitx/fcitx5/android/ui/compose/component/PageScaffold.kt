/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.ui.compose.theme.LocalEnableBlur
import org.fcitx.fcitx5.android.ui.compose.util.BlurredBar
import org.fcitx.fcitx5.android.ui.compose.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * The shared page skeleton: a large-title top bar that blurs as you scroll, and a list of cards.
 *
 * Every page in the settings app uses this so the spacing, corner radii and blur behaviour stay
 * identical across 主页 / 界面 / 功能 / 设置 and any detail screen.
 */
@Composable
fun PageScaffold(
    title: String,
    bottomInnerPadding: Dp,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface

    Scaffold(
        modifier = modifier,
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = title,
                    scrollBehavior = scrollBehavior,
                    largeTitle = title,
                    navigationIcon = {
                        if (onBack != null) {
                            top.yukonga.miuix.kmp.basic.IconButton(
                                onClick = onBack,
                            ) {
                                top.yukonga.miuix.kmp.basic.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Rounded.ArrowBack,
                                    contentDescription = null,
                                    tint = colorScheme.onSurface,
                                )
                            }
                        }
                    },
                    actions = actions,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                content()
                item { androidx.compose.foundation.layout.Spacer(Modifier.padding(top = bottomInnerPadding)) }
            }
        }
    }
}
