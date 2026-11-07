/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.IconTheme
import org.fcitx.fcitx5.android.data.theme.IconThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import androidx.compose.material3.MaterialTheme
import org.fcitx.fcitx5.android.ui.compose.LocalUiMode
import org.fcitx.fcitx5.android.ui.compose.UiMode
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.PickerDialog
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.main.settings.behavior.share.JsonFileQrShareManager
import org.fcitx.fcitx5.android.ui.main.settings.behavior.share.LayoutQrBitmapUtil
import org.fcitx.fcitx5.android.ui.main.settings.behavior.share.LayoutQrTransferCodec
import org.fcitx.fcitx5.android.ui.main.settings.behavior.share.QrChunkCollector
import org.fcitx.fcitx5.android.ui.main.settings.behavior.share.QrScanOptions
import org.fcitx.fcitx5.android.ui.main.settings.icon.IconThemeEditorActivity
import org.fcitx.fcitx5.android.ui.main.settings.icon.IconThemeQrTransferCodec
import org.fcitx.fcitx5.android.utils.toast
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private const val QR_PREVIEW_ICON_SIZE = 96

/**
 * 图标主题: the icon skins the keyboard can use, plus the import / share entry points.
 *
 * This replaces `IconThemeListActivity`. The list, the create/duplicate menu and the editor entry
 * are ported; the QR / zip / JSON transfer keeps using the same managers, but feedback is a status
 * line on this screen instead of a modal progress dialog, and the QR scanner is launched through
 * the Activity Result API the same way.
 */
@Composable
fun IconThemeScreen(
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    onOpenIntent: (Intent) -> Unit,
) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var createMenu by remember { mutableStateOf(false) }
    var sharePicker by remember { mutableStateOf(false) }
    var duplicatePicker by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    val chunkCollector = remember { QrChunkCollector() }

    val themes = remember(revision) { IconThemeManager.iconThemes }
    val activeTheme = remember(revision) { IconThemeManager.activeTheme }
    val builtinDefault = remember { IconTheme.default() }

    // The manager notifies on every list/selection change; re-read on each notification.
    DisposableEffect(Unit) {
        val listListener = IconThemeManager.OnIconThemeListChangeListener { revision++ }
        val themeListener = IconThemeManager.OnIconThemeChangeListener { revision++ }
        val keyboardThemeListener = ThemeManager.OnThemeChangeListener { revision++ }
        IconThemeManager.addOnListChangeListener(listListener)
        IconThemeManager.addOnChangedListener(themeListener)
        ThemeManager.addOnChangedListener(keyboardThemeListener)
        onDispose {
            IconThemeManager.removeOnListChangeListener(listListener)
            IconThemeManager.removeOnChangedListener(themeListener)
            ThemeManager.removeOnChangedListener(keyboardThemeListener)
        }
    }

    fun importJson(uri: Uri) {
        FcitxApplication.getInstance().coroutineScope.launch {
            val json = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                }
            }.getOrNull()
            if (json == null) {
                context.toast(R.string.icon_theme_import_json_failed)
                return@launch
            }
            runCatching { IconThemeManager.importTheme(json) }
                .onSuccess {
                    revision++
                    context.toast(context.getString(R.string.icon_theme_imported, it.name))
                }
                .onFailure { context.toast(R.string.icon_theme_import_json_failed) }
        }
    }

    fun importThemeFromJson(json: String) {
        runCatching {
            IconThemeQrTransferCodec.decodeIconThemeFromJson(json)
                .also { IconThemeManager.saveTheme(it) }
        }.recoverCatching { IconThemeManager.importTheme(json) }
            .onSuccess {
                revision++
                context.toast(context.getString(R.string.icon_theme_imported, it.name))
            }
            .onFailure { context.toast(R.string.icon_theme_decode_qr_failed) }
    }

    fun importFromQrImage(uri: Uri) {
        FcitxApplication.getInstance().coroutineScope.launch {
            busy = context.getString(R.string.loading)
            val chunks = runCatching {
                withContext(Dispatchers.Default) {
                    JsonFileQrShareManager.decodeQrChunksFromImage(context, uri) { current, total ->
                        busy = context.getString(R.string.qr_image_decode_progress, current, total)
                    }
                }
            }.getOrDefault(emptyList())
            busy = null
            if (chunks.isEmpty()) {
                context.toast(R.string.icon_theme_no_qr_found)
                return@launch
            }
            runCatching { LayoutQrTransferCodec.decodeChunksToJson(chunks) }
                .onSuccess { importThemeFromJson(it) }
                .onFailure { context.toast(R.string.icon_theme_decode_qr_failed) }
        }
    }

    fun importZip(uri: Uri) {
        FcitxApplication.getInstance().coroutineScope.launch {
            val theme = runCatching {
                withContext(Dispatchers.Default) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        IconThemeManager.importThemeFromZip(input).getOrThrow()
                    } ?: error("Cannot open ZIP")
                }
            }
            theme.onSuccess {
                revision++
                context.toast(context.getString(R.string.icon_theme_imported, it.name))
            }.onFailure {
                context.toast(
                    context.getString(R.string.icon_theme_import_zip_failed, it.message.orEmpty())
                )
            }
        }
    }

    fun shareTheme(theme: IconTheme) {
        FcitxApplication.getInstance().coroutineScope.launch {
            val hasFiles = IconThemeManager.collectFileReferences(theme).isNotEmpty()
            val intent = runCatching {
                if (hasFiles) shareAsZip(theme) else shareAsQrImage(theme)
            }
            intent.onSuccess {
                context.startActivity(Intent.createChooser(it, context.getString(R.string.icon_theme_share_title)))
            }.onFailure {
                context.toast(
                    context.getString(
                        if (hasFiles) R.string.icon_theme_export_zip_failed
                        else R.string.icon_theme_export_qr_failed,
                        it.message.orEmpty(),
                    )
                )
            }
        }
    }

    fun addScannedChunk(raw: String) {
        val header = JsonFileQrShareManager.parseQrPayload(raw)
        val type = header?.let { LayoutQrTransferCodec.detectTransferType(it.transferId) }
        if (type != null && type != LayoutQrTransferCodec.TRANSFER_TYPE_ICON_THEME) {
            context.toast(
                context.getString(
                    R.string.text_keyboard_layout_qr_type_mismatch,
                    context.getString(R.string.qr_payload_type_icon_theme),
                    context.getString(R.string.qr_payload_type_unknown),
                )
            )
            return
        }
        val progress = runCatching { chunkCollector.addAndMaybeAssemble(raw) }.getOrNull()
        if (progress == null) {
            context.toast(R.string.text_keyboard_layout_qr_invalid_payload)
            return
        }
        if (progress.duplicate) context.toast(R.string.text_keyboard_layout_qr_duplicate_chunk)
        val completed = progress.completedJson
        if (completed != null) {
            importThemeFromJson(completed)
        } else {
            context.toast(
                context.getString(
                    R.string.text_keyboard_layout_qr_scan_progress,
                    progress.current,
                    progress.total,
                )
            )
        }
    }

    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        result?.contents?.let(::addScannedChunk)
    }
    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scanner.launch(
                QrScanOptions.forPrompt(context.getString(R.string.text_keyboard_layout_qr_scan_prompt))
            )
        } else {
            context.toast(R.string.text_keyboard_layout_qr_camera_permission_denied)
        }
    }
    val jsonImporter = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) importJson(uri)
    }
    val qrImageImporter =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) importFromQrImage(uri)
        }
    val zipImporter = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) importZip(uri)
    }

    PageScaffold(
        title = stringResource(R.string.icon_theme),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
        actions = {
            IconButton(onClick = { menu = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null,
                    tint = colorScheme.onSurface,
                )
            }
        },
    ) {
        busy?.let { status ->
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    when (LocalUiMode.current) {
                        UiMode.Miuix -> Text(
                            text = status,
                            style = MiuixTheme.textStyles.body2,
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(16.dp),
                        )

                        UiMode.Material -> androidx.compose.material3.Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                themes.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { theme ->
                            IconThemeTile(
                                modifier = Modifier.weight(1f),
                                theme = theme,
                                active = theme.name == activeTheme.name,
                                isDefault = theme.name == builtinDefault.name,
                                onSelect = {
                                    IconThemeManager.activeTheme = theme
                                    revision++
                                },
                                onEdit = { onOpenIntent(editorIntent(theme.name)) },
                            )
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    NewThemeTile(
                        modifier = Modifier.weight(1f),
                        onClick = { createMenu = true },
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (menu) {
        val customThemes = themes.drop(1)
        val entries = buildList {
            add(context.getString(R.string.icon_theme_import_json))
            add(context.getString(R.string.text_keyboard_layout_qr_import_scan))
            add(context.getString(R.string.icon_theme_import_qr_image))
            add(context.getString(R.string.icon_theme_import_zip))
            add(context.getString(R.string.icon_theme_share_title))
        }
        PickerDialog(
            title = context.getString(R.string.icon_theme),
            items = entries,
            onDismiss = { menu = false },
            onSelected = { which ->
                menu = false
                when (which) {
                    0 -> jsonImporter.launch("application/json")
                    1 -> {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            scanner.launch(
                                QrScanOptions.forPrompt(
                                    context.getString(R.string.text_keyboard_layout_qr_scan_prompt)
                                )
                            )
                        } else {
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    }
                    2 -> qrImageImporter.launch("image/*")
                    3 -> zipImporter.launch("application/zip")
                    4 -> {
                        if (customThemes.isEmpty()) {
                            context.toast(R.string.icon_theme_no_custom_theme)
                        } else {
                            sharePicker = true
                        }
                    }
                }
            },
        )
    }

    if (sharePicker) {
        val customThemes = themes.drop(1)
        PickerDialog(
            title = context.getString(R.string.icon_theme_share_title),
            items = customThemes.map { it.name },
            onDismiss = { sharePicker = false },
            onSelected = { index ->
                sharePicker = false
                shareTheme(customThemes[index])
            },
        )
    }

    if (duplicatePicker) {
        val customThemes = themes.drop(1)
        PickerDialog(
            title = context.getString(R.string.icon_theme_duplicate),
            items = customThemes.map { it.name },
            onDismiss = { duplicatePicker = false },
            onSelected = { index ->
                duplicatePicker = false
                val source = customThemes[index]
                val duplicated = source.copy(name = copyName(source.name, themes))
                IconThemeManager.saveTheme(duplicated)
                revision++
                context.toast(R.string.icon_theme_duplicated)
                onOpenIntent(editorIntent(duplicated.name))
            },
        )
    }

    if (createMenu) {
        PickerDialog(
            title = context.getString(R.string.icon_theme_new),
            items = listOf(
                context.getString(R.string.new_icon_theme),
                context.getString(R.string.icon_theme_duplicate),
            ),
            onDismiss = { createMenu = false },
            onSelected = { which ->
                createMenu = false
                when (which) {
                    0 -> onOpenIntent(editorIntent(null))
                    else -> {
                        val customThemes = themes.drop(1)
                        if (customThemes.isEmpty()) {
                            context.toast(R.string.icon_theme_no_custom_theme)
                        } else {
                            duplicatePicker = true
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun IconThemeTile(
    modifier: Modifier,
    theme: IconTheme,
    active: Boolean,
    isDefault: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable { onSelect() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconThemeThumbnail(theme = theme, size = 44)
            if (active) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        when (LocalUiMode.current) {
            UiMode.Miuix -> Text(
                text = if (isDefault) stringResource(R.string.default_) else theme.name,
                style = MiuixTheme.textStyles.footnote1,
                color = if (active) colorScheme.primary else colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            UiMode.Material -> androidx.compose.material3.Text(
                text = if (isDefault) stringResource(R.string.default_) else theme.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        if (!isDefault) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.edit_icon_theme),
                    tint = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun NewThemeTile(modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = colorScheme.primary,
            modifier = Modifier.size(44.dp),
        )
        when (LocalUiMode.current) {
            UiMode.Miuix -> Text(
                text = stringResource(R.string.icon_theme_new),
                style = MiuixTheme.textStyles.footnote1,
                color = colorScheme.primary,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )

            UiMode.Material -> androidx.compose.material3.Text(
                text = stringResource(R.string.icon_theme_new),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Renders the theme's thumbnail SVG into an image, falling back to a neutral icon. */
@Composable
private fun IconThemeThumbnail(theme: IconTheme, size: Int) {
    val svg = theme.thumbnailSvg?.takeIf { it.isNotBlank() }
        ?: IconThemeManager.buildThemeThumbnailSvg(theme.icons)
    val bitmap = remember(theme, svg) {
        svg?.let { renderSvg(it, size) }
    }
    val image = bitmap?.asImageBitmap()
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.size(size.dp),
        )
    } else {
        Icon(
            imageVector = Icons.Rounded.Palette,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(size.dp),
        )
    }
}

private fun renderSvg(svgContent: String, sizePx: Int): Bitmap? = runCatching {
    val svg = com.caverock.androidsvg.SVG.getFromString(svgContent)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    svg.renderToCanvas(Canvas(bitmap), RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()))
    bitmap
}.getOrNull()

private fun editorIntent(themeName: String?): Intent =
    Intent(org.fcitx.fcitx5.android.utils.appContext, IconThemeEditorActivity::class.java).apply {
        if (themeName != null) putExtra("icon_theme_name", themeName)
    }

private fun copyName(name: String, themes: List<IconTheme>): String {
    val base = "$name (copy)"
    if (themes.none { it.name == base }) return base
    var index = 2
    while (themes.any { it.name == "$base $index" }) index++
    return "$base $index"
}

private suspend fun shareAsZip(theme: IconTheme): Intent {
    val context = org.fcitx.fcitx5.android.utils.appContext
    val uri = withContext(Dispatchers.Default) {
        val dir = java.io.File(context.cacheDir, "shared").also { it.mkdirs() }
        val safeName = theme.name.replace(Regex("[/\\\\:*?\"<>|]"), "_")
        val zipFile = java.io.File(dir, "icon_theme_$safeName.zip")
        java.io.FileOutputStream(zipFile).use { out ->
            IconThemeManager.exportThemeToZip(theme, out).getOrThrow()
        }
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.share.fileprovider",
            zipFile,
        )
    }
    return Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private suspend fun shareAsQrImage(theme: IconTheme): Intent {
    val context = org.fcitx.fcitx5.android.utils.appContext
    val uri = withContext(Dispatchers.Default) {
        val bundle = IconThemeQrTransferCodec.encodeIconThemeToChunks(theme)
        val labels = JsonFileQrShareManager.buildChunkLabels(
            bundle,
            context.getString(R.string.qr_payload_type_icon_theme),
            theme.name,
        )
        val preview = renderSvg(
            theme.thumbnailSvg?.takeIf { it.isNotBlank() }
                ?: IconThemeManager.buildThemeThumbnailSvg(theme.icons).orEmpty(),
            QR_PREVIEW_ICON_SIZE,
        )?.let { icon ->
            val strip = LayoutQrBitmapUtil.composeCenteredPreviewStrip(icon, QR_PREVIEW_ICON_SIZE)
            icon.recycle()
            strip
        }
        val composed = LayoutQrBitmapUtil.composeLongImageStreamingWithPreview(
            bundle.chunks.map { it.encode() },
            labels,
            preview,
        )
        preview?.takeIf { !it.isRecycled }?.recycle()
        JsonFileQrShareManager.saveLongImageToShareCache(
            context,
            composed,
            "icon_theme_${theme.name}",
        ).also { if (!composed.isRecycled) composed.recycle() }
    }
    return Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
