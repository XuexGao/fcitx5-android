/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 *
 * Ported from KernelSU-Style-UI-Kit (Apache-2.0): the Material 3 flavour of 主题设置. Only the
 * screen chrome (PageScaffold), the preference source and the string resources differ from the
 * template; the preview, the key-colour row and the segmented groups are the template's.
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs.ColorMode
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.bottombar.useNavigationRail
import org.fcitx.fcitx5.android.ui.compose.component.material.SegmentedColumn
import org.fcitx.fcitx5.android.ui.compose.component.material.SegmentedDropdownItem
import org.fcitx.fcitx5.android.ui.compose.component.material.SegmentedSwitchItem
import org.fcitx.fcitx5.android.ui.compose.theme.colorSpecNames
import org.fcitx.fcitx5.android.ui.compose.theme.keyColorOptions
import org.fcitx.fcitx5.android.ui.compose.theme.paletteStyleNames
import org.fcitx.fcitx5.android.utils.AppUtil

@Composable
fun ColorPaletteScreenMaterial(
    state: ColorPaletteUiState,
    actions: ColorPaletteScreenActions,
    bottomInnerPadding: Dp,
) {
    val currentColorMode = state.currentColorMode
    val currentKeyColor = state.keyColor
    val colorStyle = state.currentPaletteStyle
    val colorSpec = state.currentColorSpec
    val haptic = LocalHapticFeedback.current

    PageScaffold(
        title = stringResource(R.string.ksu_app_theme_settings),
        bottomInnerPadding = bottomInnerPadding,
        onBack = actions.onBack,
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val isDark = currentColorMode.isDark || currentColorMode.isSystem && isSystemInDarkTheme()
                ThemePreviewCard(
                    keyColor = currentKeyColor,
                    isDark = isDark,
                    paletteStyle = colorStyle,
                    colorSpec = colorSpec,
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        ColorButtonMaterial(
                            color = Color.Unspecified,
                            isSelected = currentKeyColor == 0,
                            isDark = isDark,
                            paletteStyle = colorStyle,
                            colorSpec = colorSpec,
                            onClick = {
                                actions.onSetKeyColor(0)
                            },
                        )
                    }

                    items(keyColorOptions) { color ->
                        ColorButtonMaterial(
                            color = Color(color),
                            isSelected = currentKeyColor == color,
                            isDark = isDark,
                            paletteStyle = colorStyle,
                            colorSpec = colorSpec,
                            onClick = {
                                actions.onSetKeyColor(color)
                            },
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val options = listOf(
                        listOf(ColorMode.System) to stringResource(R.string.ksu_app_color_mode_system),
                        listOf(ColorMode.Light) to stringResource(R.string.ksu_app_color_mode_light),
                        listOf(ColorMode.Dark) to stringResource(R.string.ksu_app_color_mode_dark),
                        listOf(ColorMode.Amoled) to stringResource(R.string.ksu_app_color_mode_amoled),
                    )

                    options.chunked(4).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                ButtonGroupDefaults.ConnectedSpaceBetween
                            ),
                        ) {
                            rowOptions.forEachIndexed { index, (modes, label) ->
                                ToggleButton(
                                    checked = currentColorMode in modes,
                                    onCheckedChange = {
                                        if (it) {
                                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                            actions.onSetColorMode(modes.first())
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics { role = Role.RadioButton },
                                    shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        rowOptions.lastIndex ->
                                            ButtonGroupDefaults.connectedTrailingButtonShapes()

                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                ) {
                                    Icon(
                                        imageVector = when (modes.first()) {
                                            ColorMode.System -> Icons.Filled.Brightness4
                                            ColorMode.Light -> Icons.Filled.Brightness7
                                            ColorMode.Dark -> Icons.Filled.Brightness3
                                            ColorMode.Amoled -> Icons.Filled.Brightness1
                                            else -> Icons.Filled.Brightness4
                                        },
                                        contentDescription = label,
                                    )
                                }
                            }
                        }
                    }

                    SegmentedColumn(
                        modifier = Modifier.padding(top = 4.dp),
                        content = listOf(
                            {
                                SegmentedDropdownItem(
                                    icon = Icons.Rounded.Dashboard,
                                    title = stringResource(R.string.ksu_app_ui_mode),
                                    items = listOf("Miuix", "Material"),
                                    selectedIndex = if (state.uiMode == "material") 1 else 0,
                                    onItemSelected = { index ->
                                        actions.onSetUiMode(if (index == 1) "material" else "miuix")
                                    },
                                )
                            },
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.Rounded.BlurOn,
                                    title = stringResource(R.string.ksu_app_enable_blur),
                                    summary = stringResource(R.string.ksu_app_enable_blur_summary),
                                    checked = state.enableBlur,
                                    onCheckedChange = { actions.onSetEnableBlur(it) },
                                )
                            },
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.Rounded.CallToAction,
                                    title = stringResource(R.string.ksu_app_floating_bar),
                                    checked = state.enableFloatingBottomBar,
                                    onCheckedChange = { actions.onSetEnableFloatingBottomBar(it) },
                                )
                            },
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.Rounded.WaterDrop,
                                    title = stringResource(R.string.ksu_app_floating_bar_blur),
                                    checked = state.enableFloatingBottomBarBlur,
                                    enabled = state.enableFloatingBottomBar,
                                    onCheckedChange = { actions.onSetEnableFloatingBottomBarBlur(it) },
                                )
                            },
                            {
                                SegmentedDropdownItem(
                                    icon = Icons.Rounded.Style,
                                    title = stringResource(R.string.ksu_app_palette_style),
                                    items = paletteStyleNames,
                                    selectedIndex = paletteStyleNames.indexOf(colorStyle.name),
                                    onItemSelected = { index ->
                                        actions.onSetColorStyle(paletteStyleNames[index])
                                    },
                                )
                            },
                            {
                                SegmentedDropdownItem(
                                    icon = Icons.Rounded.DesignServices,
                                    title = stringResource(R.string.ksu_app_color_spec),
                                    items = colorSpecNames,
                                    selectedIndex = colorSpecNames
                                        .indexOf(colorSpec.name)
                                        .coerceAtLeast(0),
                                    onItemSelected = { index ->
                                        actions.onSetColorSpec(colorSpecNames[index])
                                    },
                                )
                            },
                        ),
                    )
                }
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCard(
    keyColor: Int,
    isDark: Boolean,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
) {
    val context = LocalContext.current
    val appLabel = remember(context) { AppUtil.appLabel(context) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight
    val useRail = useNavigationRail(enableFloatingBottomBar = false)
    val dynamicColor = keyColor == 0

    val colorScheme = if (dynamicColor) {
        val baseScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        rememberDynamicColorScheme(
            seedColor = Color.Unspecified,
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error,
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = Color(keyColor),
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
        )
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio),
            color = colorScheme.background,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.outlineVariant),
        ) {
            val content: @Composable ColumnScope.() -> Unit = {
                // top bar
                Box(
                    modifier = Modifier
                        .height(if (useRail) 36.dp else 48.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, top = if (useRail) 8.dp else 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = appLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colorScheme.onSurface,
                        )
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.TopStart,
                ) {
                    val blockScale = ((maxHeight - 14.dp - 2.dp) / 152.dp).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        PreviewBlock(
                            color = colorScheme.secondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp * blockScale),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(2) {
                                PreviewBlock(
                                    color = colorScheme.surfaceContainerHighest,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp * blockScale),
                                )
                            }
                        }
                        PreviewBlock(
                            color = colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp * blockScale),
                        )
                    }
                }
            }

            if (useRail) {
                Row {
                    Column(
                        modifier = Modifier.fillMaxHeight().width(36.dp)
                            .background(colorScheme.surfaceContainer),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.Home, null, tint = colorScheme.primary)
                        PreviewBlock(
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) { content() }
                }
            } else {
                Column {
                    content()
                    // bottom bar
                    Surface(
                        color = colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Home, null, tint = colorScheme.primary)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(colorScheme.onSurfaceVariant.copy(alpha = 0.45f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewBlock(
    color: Color,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color),
    )
}

@Composable
private fun ColorButtonMaterial(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colorScheme = if (color == Color.Unspecified) {
        val baseScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        rememberDynamicColorScheme(
            seedColor = Color.Unspecified,
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error,
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = color,
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
        )
    }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier.size(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(
                    color = colorScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                )
                drawArc(
                    color = colorScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                )
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f)
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp),
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorScheme.primary, CircleShape),
                    )
                }
            }
        }
    }
}
