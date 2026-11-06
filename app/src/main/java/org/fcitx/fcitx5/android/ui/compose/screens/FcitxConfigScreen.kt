/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import android.net.Uri
import androidx.compose.foundation.layout.padding
import arrow.core.getOrElse
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.core.Key
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.ui.compose.component.DropdownRow
import org.fcitx.fcitx5.android.ui.compose.component.SwitchRowRaw
import org.fcitx.fcitx5.android.ui.compose.component.SliderRowRaw
import org.fcitx.fcitx5.android.ui.compose.component.ListConfigDialog
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.utils.buildDocumentsProviderIntent
import org.fcitx.fcitx5.android.utils.buildPrimaryStorageIntent
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor
import org.fcitx.fcitx5.android.utils.config.ConfigType
import org.fcitx.fcitx5.android.utils.toast
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.SmallTitle

/**
 * Compose replacement for `PreferenceScreenFactory`.
 *
 * Fcitx describes its configuration as a tree of [RawConfig] plus a parallel tree of
 * [ConfigDescriptor]s. This renders that pair with miuix components: switches, sliders, dropdowns,
 * text dialogs and grouping, and dispatches "external" entries to the screens that own them
 * (dictionary managers, punctuation editor, add-on configs, …).
 *
 * Values are written straight back into the [RawConfig] tree and pushed to fcitx after every edit,
 * which is simpler than the old "save when leaving" behaviour and cannot lose an edit.
 */
@Composable
fun FcitxConfigScreen(
    title: String,
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    fcitx: FcitxConnection,
    loadConfig: suspend FcitxAPI.() -> RawConfig,
    saveConfig: suspend FcitxAPI.(RawConfig) -> Unit,
    onOpenRoute: (SettingsRoute) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var root by remember { mutableStateOf<RawConfig?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var listDialog by remember { mutableStateOf<Pair<RawConfig, ConfigDescriptor<*, *>>?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            fcitx.runOnReady { loadConfig() }
        }.onSuccess { root = it }
            .onFailure {
                Timber.w(it, "Failed to load config")
                failure = it.localizedMessage ?: it.toString()
            }
    }

    val loaded = root
    val descriptor = remember(loaded) {
        loaded?.findByName("desc")?.let { node ->
            ConfigDescriptor.parseTopLevel(node).getOrElse { error ->
                Timber.w(error, "Unable to parse config descriptor")
                null
            }
        }
    }

    fun persist() {
        val snapshot = root ?: return
        scope.launch {
            runCatching { fcitx.runOnReady { saveConfig(snapshot) } }
                .onFailure { Timber.w(it, "Failed to save config") }
        }
    }

    PageScaffold(
        title = title,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        if (loaded == null || descriptor == null) {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(
                        title = failure ?: stringResource(R.string.loading),
                        icon = Icons.Rounded.Tune,
                    )
                }
            }
            return@PageScaffold
        }
        cfgRows(
            cfg = loaded.findByName("cfg") ?: RawConfig(),
            descriptors = descriptor.values,
            onChanged = { persist() },
            onOpenRoute = onOpenRoute,
            onEditList = { listCfg, listDescriptor -> listDialog = listCfg to listDescriptor },
        )
    }

    val editingList = listDialog
    if (editingList != null) {
        val (listCfg, listDescriptor) = editingList
        ListConfigDialog(
            title = listDescriptor.description ?: listDescriptor.name,
            descriptor = listDescriptor,
            cfg = listCfg,
            onDismiss = { listDialog = null },
            onChanged = { persist() },
        )
    }
}

/** Renders every descriptor of a config level. */
private fun LazyListScope.cfgRows(
    cfg: RawConfig,
    descriptors: List<ConfigDescriptor<*, *>>,
    onChanged: () -> Unit,
    onOpenRoute: (SettingsRoute) -> Unit,
    onEditList: (RawConfig, ConfigDescriptor<*, *>) -> Unit,
    title: String? = null,
) {
    val plain = descriptors.filterNot { it is ConfigDescriptor.ConfigCustom }
    val customs = descriptors.filterIsInstance<ConfigDescriptor.ConfigCustom>()
    item {
        if (title != null) SmallTitle(text = title)
        SettingsGroup(modifier = Modifier.padding(top = if (title == null) 12.dp else 2.dp)) {
            plain.forEach { descriptor ->
                CfgRow(cfg, descriptor, onChanged, onOpenRoute, onEditList)
            }
        }
    }
    // "Custom" descriptors are just grouping: render their members as their own titled group.
    customs.forEach { custom ->
        val sub = cfg.getOrCreate(custom.name)
        cfgRows(
            cfg = sub,
            descriptors = custom.customTypeDef?.values.orEmpty(),
            onChanged = onChanged,
            onOpenRoute = onOpenRoute,
            onEditList = onEditList,
            title = custom.description ?: custom.name,
        )
    }
}

@Composable
private fun CfgRow(
    cfg: RawConfig,
    descriptor: ConfigDescriptor<*, *>,
    onChanged: () -> Unit,
    onOpenRoute: (SettingsRoute) -> Unit,
    onEditList: (RawConfig, ConfigDescriptor<*, *>) -> Unit,
) {
    // Key bindings are hidden when the user asked for it, same as the old screen.
    if (AppPrefs.getInstance().advanced.hideKeyConfig.getValue() &&
        ConfigType.pretty(descriptor.ty).contains("Key")
    ) {
        return
    }

    val label = descriptor.description ?: descriptor.name
    val summary = descriptor.tooltip

    when (descriptor) {
        is ConfigDescriptor.ConfigBool -> {
            val current = cfg.findByName(descriptor.name)?.value?.let { it == "True" }
                ?: (descriptor.defaultValue ?: false)
            SwitchRowRaw(
                title = label,
                summary = summary,
                checked = current,
                onCheckedChange = {
                    cfg.getOrCreate(descriptor.name).value = if (it) "True" else "False"
                    onChanged()
                },
            )
        }

        is ConfigDescriptor.ConfigEnum -> {
            val entries = descriptor.entries
            val labels = descriptor.entriesI18n ?: entries
            val current = cfg.findByName(descriptor.name)?.value ?: descriptor.defaultValue ?: ""
            DropdownRow(
                title = label,
                summary = summary,
                items = labels,
                selectedIndex = entries.indexOf(current).coerceAtLeast(0),
                onSelectedIndexChange = { index ->
                    cfg.getOrCreate(descriptor.name).value = entries[index]
                    onChanged()
                },
            )
        }

        is ConfigDescriptor.ConfigInt -> {
            val min = descriptor.intMin
            val max = descriptor.intMax
            val current = cfg.findByName(descriptor.name)?.value?.toIntOrNull()
                ?: descriptor.defaultValue ?: 0
            if (min != null && max != null && max - min in 1..100) {
                SliderRowRaw(
                    title = label,
                    summary = summary,
                    value = current,
                    range = min.toFloat()..max.toFloat(),
                    onValueChange = {
                        cfg.getOrCreate(descriptor.name).value = it.toString()
                        onChanged()
                    },
                )
            } else {
                TextRowRaw(
                    title = label,
                    summary = summary,
                    value = current.toString(),
                    numeric = true,
                    onValueChange = {
                        cfg.getOrCreate(descriptor.name).value = it
                        onChanged()
                    },
                )
            }
        }

        is ConfigDescriptor.ConfigString -> {
            val current = cfg.findByName(descriptor.name)?.value ?: descriptor.defaultValue ?: ""
            TextRowRaw(
                title = label,
                summary = summary,
                value = current,
                onValueChange = {
                    cfg.getOrCreate(descriptor.name).value = it
                    onChanged()
                },
            )
        }

        is ConfigDescriptor.ConfigKey -> {
            val current = cfg.findByName(descriptor.name)?.value ?: descriptor.defaultValue ?: ""
            val rendered = runCatching { Key.parse(current).localizedString }.getOrDefault(current)
            TextRowRaw(
                title = label,
                summary = summary,
                value = current,
                displayValue = rendered.ifEmpty { stringResource(R.string.none) },
                onValueChange = {
                    cfg.getOrCreate(descriptor.name).value = it
                    onChanged()
                },
            )
        }

        is ConfigDescriptor.ConfigList, is ConfigDescriptor.ConfigEnumList -> {
            val keys = cfg.findByName(descriptor.name)?.subItems
            NavRow(
                title = label,
                summary = keys?.joinToString(", ") { it.value }.orEmpty()
                    .ifEmpty { stringResource(R.string.none) },
                onClick = { onEditList(cfg, descriptor) },
            )
        }

        is ConfigDescriptor.ConfigExternal -> ExternalRow(
            cfg = cfg,
            descriptor = descriptor,
            label = label,
            summary = summary,
            onOpenRoute = onOpenRoute,
        )

        else -> NavRow(title = label, summary = summary, icon = Icons.Rounded.Tune)
    }
}

/** External entries navigate to the screen that owns the data. */
@Composable
private fun ExternalRow(
    cfg: RawConfig,
    descriptor: ConfigDescriptor.ConfigExternal,
    label: String,
    summary: String?,
    onOpenRoute: (SettingsRoute) -> Unit,
) {
    val context = LocalContext.current
    when (descriptor.knownType) {
        ConfigDescriptor.ConfigExternal.ETy.PinyinDict ->
            NavRow(title = label, summary = summary, onClick = { onOpenRoute(SettingsRoute.PinyinDict("")) })

        ConfigDescriptor.ConfigExternal.ETy.Punctuation ->
            NavRow(title = label, summary = summary, onClick = {
                onOpenRoute(
                    SettingsRoute.Punctuation(
                        label, descriptor.uri?.substringAfterLast('/')
                    )
                )
            })

        ConfigDescriptor.ConfigExternal.ETy.QuickPhrase ->
            NavRow(title = label, summary = summary, onClick = { onOpenRoute(SettingsRoute.QuickPhraseList) })

        ConfigDescriptor.ConfigExternal.ETy.Chttrans ->
            NavRow(title = label, summary = summary, onClick = {
                onOpenRoute(SettingsRoute.AddonConfig(label, "chttrans"))
            })

        ConfigDescriptor.ConfigExternal.ETy.TableGlobal ->
            NavRow(title = label, summary = summary, onClick = {
                onOpenRoute(SettingsRoute.AddonConfig(label, "table"))
            })

        ConfigDescriptor.ConfigExternal.ETy.AndroidTable ->
            NavRow(title = label, summary = summary, onClick = { onOpenRoute(SettingsRoute.TableInputMethods) })

        ConfigDescriptor.ConfigExternal.ETy.PinyinCustomPhrase ->
            NavRow(title = label, summary = summary, onClick = { onOpenRoute(SettingsRoute.PinyinCustomPhrase) })

        ConfigDescriptor.ConfigExternal.ETy.MultiSelect -> {
            val route = descriptor.uri?.let { parseMultiSelect(it, label) }
            NavRow(
                title = label,
                summary = summary,
                enabled = route != null,
                onClick = route?.let { r -> { onOpenRoute(r) } },
            )
        }

        ConfigDescriptor.ConfigExternal.ETy.AddonDirProfileManager -> {
            val route = descriptor.uri?.let { parseAddonDirProfile(it, label) }
            NavRow(
                title = label,
                summary = summary,
                enabled = route != null,
                onClick = route?.let { r -> { onOpenRoute(r) } },
            )
        }

        ConfigDescriptor.ConfigExternal.ETy.AddonAction -> {
            val action = descriptor.uri?.let { parseAddonAction(it) }
            NavRow(
                title = label,
                summary = summary,
                enabled = action != null,
                onClick = action?.let { (addon, path) -> { runAddonAction(context, addon, path) } },
            )
        }

        ConfigDescriptor.ConfigExternal.ETy.RimeUserDataDir ->
            NavRow(title = label, summary = summary, onClick = { openRimeDataDir(context) })

        else -> NavRow(title = label, summary = summary, icon = Icons.Rounded.Tune)
    }
}

// ---- route parsers (same semantics as the old PreferenceScreenFactory) --------------------

private fun parseMultiSelect(uriText: String, title: String): SettingsRoute.MultiSelect? {
    val uri = Uri.parse(uriText)
    val segments = uri.pathSegments
    if (segments.size < 3 || segments.first() != "addon") return null
    val option = uri.getQueryParameter("option")?.takeIf { it.isNotBlank() } ?: return null
    return SettingsRoute.MultiSelect(
        title = title,
        addon = segments[1],
        path = segments.drop(2).joinToString("/"),
        option = option,
        min = uri.getQueryParameter("min")?.toIntOrNull() ?: 0,
    )
}

private fun parseAddonDirProfile(uriText: String, title: String): SettingsRoute.AddonDirProfileManager? {
    val uri = Uri.parse(uriText)
    val segments = uri.pathSegments
    if (segments.size < 3 || segments.first() != "addon") return null
    val addon = segments[1]
    val path = segments.drop(2).joinToString("/")
    if (addon.isBlank() || path.isBlank()) return null
    return SettingsRoute.AddonDirProfileManager(title, addon, path)
}

private fun parseAddonAction(uriText: String): Pair<String, String>? {
    val uri = Uri.parse(uriText)
    val segments = uri.pathSegments
    if (segments.size < 3 || segments.first() != "addon") return null
    val addon = segments[1]
    val path = segments.drop(2).joinToString("/")
    if (addon.isBlank() || path.isBlank()) return null
    return addon to path
}

private fun runAddonAction(context: android.content.Context, addon: String, path: String) {
    FcitxApplication.getInstance().coroutineScope.launch {
        val connectionName = "settings-addon-action-${System.nanoTime()}"
        val conn = FcitxDaemon.connect(connectionName)
        try {
            conn.runOnReady { setAddonSubConfig(addon, path, RawConfig()) }
            context.toast(R.string.done)
        } catch (e: Exception) {
            Timber.w("Addon action failed: addon=$addon path=$path error=$e")
            context.toast(e)
        } finally {
            FcitxDaemon.disconnect(connectionName)
        }
    }
}

private fun openRimeDataDir(context: android.content.Context) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
        runCatching { context.startActivity(buildDocumentsProviderIntent()) }
            .onFailure { context.toast(it) }
        return
    }
    FcitxApplication.getInstance().coroutineScope.launch {
        val connectionName = "settings-rime-user-dir-${System.nanoTime()}"
        val conn = FcitxDaemon.connect(connectionName)
        val currentDir = try {
            conn.runOnReady {
                val raw = getAddonSubConfig("rime", "profile-manager")
                val cfg = raw.findByName("cfg") ?: raw
                cfg.findByName("Current")?.value?.trim().orEmpty().ifBlank { "rime" }
            }
        } catch (e: Exception) {
            "rime"
        } finally {
            FcitxDaemon.disconnect(connectionName)
        }
        runCatching { context.startActivity(buildPrimaryStorageIntent("data/$currentDir")) }
            .onFailure {
                runCatching { context.startActivity(buildDocumentsProviderIntent()) }
                    .onFailure { e -> context.toast(e) }
            }
    }
}

// ---- small raw-config rows ---------------------------------------------------------------

/** Text / number / key editor: a row that opens a dialog. */
@Composable
private fun TextRowRaw(
    title: String,
    summary: String?,
    value: String,
    numeric: Boolean = false,
    displayValue: String? = null,
    onValueChange: (String) -> Unit,
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    NavRow(
        title = title,
        summary = listOfNotNull(summary, displayValue ?: value.ifEmpty { null }).joinToString(" · ")
            .ifEmpty { null },
        onClick = { showDialog = true },
    )
    if (showDialog) {
        val input = remember { mutableStateOf(value) }
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setView(
                android.widget.EditText(context).apply {
                    setText(input.value)
                    if (numeric) inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                    setPadding(48, 24, 48, 24)
                }.also { field ->
                    field.addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                        override fun afterTextChanged(s: android.text.Editable?) {
                            input.value = s?.toString().orEmpty()
                        }
                    })
                }
            )
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onValueChange(input.value)
                showDialog = false
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> showDialog = false }
            .show()
    }
}
