/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 *
 * Ported from KernelSU-Style-UI-Kit (Apache-2.0): the Miuix flavour of 主题设置. Only the screen
 * chrome (PageScaffold), the preference source and the string resources differ from the template;
 * the preview, the tab row and the cards are the template's.
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.bottombar.useNavigationRail
import org.fcitx.fcitx5.android.ui.compose.theme.colorSpecNames
import org.fcitx.fcitx5.android.ui.compose.theme.keyColorOptions
import org.fcitx.fcitx5.android.ui.compose.theme.paletteStyleNames
import org.fcitx.fcitx5.android.utils.AppUtil
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun ColorPaletteScreenMiuix(
    state: ColorPaletteUiState,
    actions: ColorPaletteScreenActions,
    bottomInnerPadding: Dp,
) {
    val currentColorMode = state.currentColorMode
    val isDark = currentColorMode.isDark || currentColorMode.isSystem && isSystemInDarkTheme()

    PageScaffold(
        title = stringResource(R.string.ksu_app_theme_settings),
        bottomInnerPadding = bottomInnerPadding,
        onBack = actions.onBack,
    ) {
        item {
            Spacer(modifier = Modifier.height(32.dp))
            ThemePreviewCardMiuix(
                keyColor = state.keyColor,
                isDark = isDark,
                miuixMonet = state.miuixMonet,
                enableFloatingBottomBar = state.enableFloatingBottomBar,
                enableFloatingBottomBarBlur = state.enableFloatingBottomBarBlur,
                paletteStyle = state.currentPaletteStyle,
                colorSpec = state.currentColorSpec,
            )
            Spacer(modifier = Modifier.height(72.dp))

            val themeItems = listOf(
                stringResource(id = R.string.ksu_app_color_mode_system),
                stringResource(id = R.string.ksu_app_color_mode_light),
                stringResource(id = R.string.ksu_app_color_mode_dark),
            )
            TabRow(
                tabs = themeItems,
                selectedTabIndex = (if (state.colorMode >= 3) state.colorMode - 3 else state.colorMode)
                    .coerceIn(0, 2),
                onTabSelected = { index ->
                    actions.onSetThemeMode(index)
                },
            )

            Card(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
            ) {
                SwitchPreference(
                    title = stringResource(id = R.string.ksu_app_monet),
                    startAction = {
                        Icon(
                            Icons.Rounded.Wallpaper,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = stringResource(id = R.string.ksu_app_monet),
                            tint = colorScheme.onBackground,
                        )
                    },
                    checked = state.miuixMonet,
                    onCheckedChange = {
                        actions.onSetMiuixMonet(it)
                    },
                )

                AnimatedVisibility(
                    visible = state.miuixMonet,
                ) {
                    Column {
                        val colorItems = listOf(
                            stringResource(id = R.string.ksu_app_key_color_default),
                            stringResource(id = R.string.ksu_app_color_red),
                            stringResource(id = R.string.ksu_app_color_pink),
                            stringResource(id = R.string.ksu_app_color_purple),
                            stringResource(id = R.string.ksu_app_color_deep_purple),
                            stringResource(id = R.string.ksu_app_color_indigo),
                            stringResource(id = R.string.ksu_app_color_blue),
                            stringResource(id = R.string.ksu_app_color_cyan),
                            stringResource(id = R.string.ksu_app_color_teal),
                            stringResource(id = R.string.ksu_app_color_green),
                            stringResource(id = R.string.ksu_app_color_yellow),
                            stringResource(id = R.string.ksu_app_color_amber),
                            stringResource(id = R.string.ksu_app_color_orange),
                            stringResource(id = R.string.ksu_app_color_brown),
                            stringResource(id = R.string.ksu_app_color_blue_grey),
                            stringResource(id = R.string.ksu_app_color_sakura),
                        )
                        val colorValues = listOf(0) + keyColorOptions
                        OverlayDropdownPreference(
                            title = stringResource(id = R.string.ksu_app_key_color),
                            items = colorItems,
                            startAction = {
                                Icon(
                                    Icons.Rounded.Colorize,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.ksu_app_key_color),
                                    tint = colorScheme.onBackground,
                                )
                            },
                            selectedIndex = colorValues.indexOf(state.keyColor).takeIf { it >= 0 } ?: 0,
                            onSelectedIndexChange = { index ->
                                actions.onSetKeyColor(colorValues[index])
                            },
                        )

                        AnimatedVisibility(
                            visible = state.keyColor != 0,
                        ) {
                            Column {
                                val paletteStyleTitle = stringResource(R.string.ksu_app_palette_style)
                                OverlayDropdownPreference(
                                    title = paletteStyleTitle,
                                    startAction = {
                                        Icon(
                                            Icons.Rounded.Style,
                                            modifier = Modifier.padding(end = 6.dp),
                                            contentDescription = paletteStyleTitle,
                                            tint = colorScheme.onBackground,
                                        )
                                    },
                                    items = paletteStyleNames,
                                    selectedIndex = paletteStyleNames
                                        .indexOf(state.currentPaletteStyle.name)
                                        .coerceAtLeast(0),
                                    onSelectedIndexChange = { index ->
                                        actions.onSetColorStyle(paletteStyleNames[index])
                                    },
                                )

                                val colorSpecTitle = stringResource(R.string.ksu_app_color_spec)
                                OverlayDropdownPreference(
                                    title = colorSpecTitle,
                                    startAction = {
                                        Icon(
                                            Icons.Rounded.DesignServices,
                                            modifier = Modifier.padding(end = 6.dp),
                                            contentDescription = colorSpecTitle,
                                            tint = colorScheme.onBackground,
                                        )
                                    },
                                    items = colorSpecNames,
                                    selectedIndex = colorSpecNames
                                        .indexOf(state.currentColorSpec.name)
                                        .coerceAtLeast(0),
                                    onSelectedIndexChange = { index ->
                                        actions.onSetColorSpec(colorSpecNames[index])
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
            ) {
                OverlayDropdownPreference(
                    title = stringResource(id = R.string.ksu_app_ui_mode),
                    summary = stringResource(id = R.string.ksu_app_ui_mode_summary),
                    items = listOf("Miuix", "Material"),
                    selectedIndex = if (state.uiMode == "material") 1 else 0,
                    startAction = {
                        Icon(
                            Icons.Rounded.Dashboard,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = stringResource(id = R.string.ksu_app_ui_mode),
                            tint = colorScheme.onBackground,
                        )
                    },
                    onSelectedIndexChange = { index ->
                        actions.onSetUiMode(if (index == 1) "material" else "miuix")
                    },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    SwitchPreference(
                        title = stringResource(id = R.string.ksu_app_enable_blur),
                        summary = stringResource(id = R.string.ksu_app_enable_blur_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.BlurOn,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(id = R.string.ksu_app_enable_blur),
                                tint = colorScheme.onBackground,
                            )
                        },
                        checked = state.enableBlur,
                        onCheckedChange = {
                            actions.onSetEnableBlur(it)
                        },
                    )
                }
                SwitchPreference(
                    title = stringResource(id = R.string.ksu_app_floating_bar),
                    startAction = {
                        Icon(
                            Icons.Rounded.CallToAction,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = stringResource(id = R.string.ksu_app_floating_bar),
                            tint = colorScheme.onBackground,
                        )
                    },
                    checked = state.enableFloatingBottomBar,
                    onCheckedChange = {
                        actions.onSetEnableFloatingBottomBar(it)
                    },
                )
                AnimatedVisibility(
                    visible = state.enableFloatingBottomBar &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                ) {
                    SwitchPreference(
                        title = stringResource(id = R.string.ksu_app_floating_bar_blur),
                        startAction = {
                            Icon(
                                Icons.Rounded.WaterDrop,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.ksu_app_floating_bar_blur),
                                tint = colorScheme.onBackground,
                            )
                        },
                        checked = state.enableFloatingBottomBarBlur,
                        onCheckedChange = {
                            actions.onSetEnableFloatingBottomBarBlur(it)
                        },
                    )
                }
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCardMiuix(
    keyColor: Int,
    isDark: Boolean,
    miuixMonet: Boolean,
    enableFloatingBottomBar: Boolean = false,
    enableFloatingBottomBarBlur: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
) {
    val context = LocalContext.current
    val appLabel = remember(context) { AppUtil.appLabel(context) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight
    val useRail = useNavigationRail(enableFloatingBottomBar)

    val seedColor = if (keyColor == 0) colorScheme.primary else Color(keyColor)
    val effectiveStyle = if (keyColor == 0) PaletteStyle.TonalSpot else paletteStyle
    val effectiveSpec = if (keyColor == 0) ColorSpec.SpecVersion.Default else colorSpec
    val dynamicCs = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        style = effectiveStyle,
        specVersion = effectiveSpec,
    )

    val bgColor = if (miuixMonet) dynamicCs.background else colorScheme.surface
    val textColor = if (miuixMonet) dynamicCs.onSurface else colorScheme.onBackground
    val accentCardColor = when {
        miuixMonet -> dynamicCs.secondaryContainer
        isDark -> Color(0xFF1A3825)
        else -> Color(0xFFDFFAE4)
    }
    val cardColor = if (miuixMonet) dynamicCs.surfaceContainerHighest else colorScheme.surfaceVariant
    val navBarColor = if (miuixMonet) dynamicCs.surfaceContainer else colorScheme.surface
    val iconColor = if (miuixMonet) dynamicCs.primary else colorScheme.primary
    val navSelectedColor = colorScheme.onSurfaceContainer
    val navUnselectedColor = colorScheme.onSurfaceContainer.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio)
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp)),
        ) {
            val content = @Composable {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .height(if (useRail) 36.dp else 48.dp)
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = if (useRail) 12.dp else 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = appLabel,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = textColor,
                        )
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        val navHeight = if (useRail) 0.dp else 36.dp
                        val blockScale = ((maxHeight - navHeight - 12.dp) / 166.dp).coerceIn(0f, 1f)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            PreviewBlockMiuix(
                                color = accentCardColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp * blockScale),
                            )
                            PreviewBlockMiuix(
                                color = cardColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp * blockScale),
                            )
                            PreviewBlockMiuix(
                                color = cardColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp * blockScale),
                            )
                        }
                    }
                }
            }

            if (useRail) {
                Row {
                    Column(
                        modifier = Modifier.fillMaxHeight().width(30.dp).background(navBarColor),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        repeat(2) {
                            PreviewBlockMiuix(
                                color = if (it == 0) navSelectedColor else navUnselectedColor,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) { content() }
                }
            } else {
                content()
            }

            if (!useRail && enableFloatingBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (enableFloatingBottomBarBlur) navBarColor.copy(alpha = 0.5f)
                                else navBarColor,
                            )
                            .border(0.5.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (it == 0) iconColor else textColor),
                            )
                        }
                    }
                }
            } else if (!useRail) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(textColor.copy(alpha = 0.1f)),
                    )
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .fillMaxWidth()
                            .background(navBarColor)
                            .padding(top = 2.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (it == 0) navSelectedColor else navUnselectedColor),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewBlockMiuix(
    color: Color,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color),
    )
}
