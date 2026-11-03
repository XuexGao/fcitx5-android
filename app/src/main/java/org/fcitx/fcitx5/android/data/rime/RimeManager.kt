/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.rime

import android.content.Context
import org.fcitx.fcitx5.android.data.rime.RimePrefs.Companion.DEFAULT_SCHEMA
import org.fcitx.fcitx5.android.utils.appContext
import timber.log.Timber
import java.io.File

/**
 * Reads and writes Rime configuration on disk.
 *
 * Directory layout (set up by `native-lib.cpp`):
 *  * shared data: `<appData>/usr/share/rime-data`, deployed from the rime plugin's assets,
 *    contains the 万象拼音 schemas and dictionaries next to the stock luna_pinyin ones
 *  * user data:   `<externalFilesDir>/data/fcitx5/rime`, this is where `*.custom.yaml` goes
 *
 * fcitx5-rime passes the latter to librime as `user_data_dir`, so writing our generated
 * `default.custom.yaml` / `wanxiang.custom.yaml` there is enough to change the schema list,
 * the default schema and the double-pinyin layout — no build step required.
 */
object RimeManager {

    private const val SHARED_RIME_SUBDIR = "usr/share/rime-data"
    private const val USER_RIME_SUBDIR = "data/fcitx5/rime"

    private const val DEFAULT_CUSTOM = "default.custom.yaml"
    private const val WANXIANG_CUSTOM = "wanxiang.custom.yaml"
    private const val BACKUP_PREFIX = "rime-config-backup"

    /** `<extData>/data/fcitx5/rime` — librime's user_data_dir. */
    val userDir: File
        get() = File(appContext.getExternalFilesDir(null), USER_RIME_SUBDIR)

    /** `<appData>/usr/share/rime-data` — deployed shared data. */
    val sharedDir: File
        get() = File(appContext.applicationInfo.dataDir, SHARED_RIME_SUBDIR)

    /** All schemas visible to librime, as `id -> display name`. */
    fun availableSchemas(context: Context): List<RimeSchema> {
        val prefs = RimePrefs.ensureInitialized(context)
        val found = linkedMapOf<String, RimeSchema>()
        listOf(sharedDir, userDir).forEach { dir ->
            dir.listFiles { f -> f.isFile && f.name.endsWith(".schema.yaml") }
                ?.forEach { file ->
                    val id = file.name.removeSuffix(".schema.yaml")
                    val display = readSchemaName(file) ?: id
                    // user dir wins over shared data
                    found[id] = RimeSchema(id, display, file.parentFile == userDir)
                }
        }
        // Anything the user enabled but that is not on disk yet stays visible, so the list does not
        // silently drop a schema while the plugin is still deploying.
        prefs.enabledSchemas.getValue().forEach { id ->
            found.getOrPut(id) { RimeSchema(id, id, false) }
        }
        return found.values.sortedBy { it.id }
    }

    private fun readSchemaName(file: File): String? = runCatching {
        file.useLines { lines ->
            lines.take(40).firstOrNull { it.trimStart().startsWith("schema_name:") }
                ?.substringAfter("schema_name:")?.trim()?.trim('"', '\'')
        }
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    data class RimeSchema(val id: String, val displayName: String, val inUserDir: Boolean)

    /** `true` when the 万象拼音 schema is deployed and therefore usable. */
    fun isWanxiangAvailable(): Boolean =
        File(sharedDir, "$DEFAULT_SCHEMA.schema.yaml").isFile

    /**
     * Renders the current preferences into the Rime user directory.
     *
     * Returns the list of files written. Safe to call repeatedly; it is a no-op when nothing
     * changed and the signature still matches.
     */
    fun applyConfiguration(context: Context, force: Boolean = false): List<File> {
        val prefs = RimePrefs.ensureInitialized(context)
        val signature = configurationSignature(prefs)
        if (!force && prefs.deployed.getValue() && prefs.deployedSignature.getValue() == signature) {
            return emptyList()
        }
        val dir = userDir
        if (!dir.isDirectory && !dir.mkdirs()) {
            Timber.w("Cannot create Rime user dir %s", dir)
            return emptyList()
        }
        val written = mutableListOf<File>()
        runCatching {
            File(dir, DEFAULT_CUSTOM).writeText(buildDefaultCustom(prefs))
            written += File(dir, DEFAULT_CUSTOM)
            File(dir, WANXIANG_CUSTOM).writeText(buildWanxiangCustom(prefs))
            written += File(dir, WANXIANG_CUSTOM)
        }.onFailure {
            Timber.w(it, "Failed to write Rime custom config")
            return emptyList()
        }
        prefs.deployed.setValue(true)
        prefs.deployedSignature.setValue(signature)
        return written
    }

    private fun configurationSignature(prefs: RimePrefs): String = buildString {
        append(prefs.enabledSchemas.getValue().sorted().joinToString(","))
        append('|').append(prefs.defaultSchema.getValue())
        append('|').append(prefs.doublePinyin.getValue())
        append('|').append(prefs.pageSize.getValue())
        append('|').append(fuzzyOptions(prefs).joinToString(","))
    }

    private fun fuzzyOptions(prefs: RimePrefs): List<String> = buildList {
        if (prefs.fuzzyZhZ.getValue()) add("模糊音_z_zh")
        if (prefs.fuzzyChC.getValue()) add("模糊音_c_ch")
        if (prefs.fuzzyShS.getValue()) add("模糊音_s_sh")
        if (prefs.fuzzyNL.getValue()) add("模糊音_nl")
        if (prefs.fuzzyInIng.getValue()) add("模糊音_in_ing")
        if (prefs.fuzzyAnAng.getValue()) add("模糊音_an_ang")
    }

    private fun buildDefaultCustom(prefs: RimePrefs): String {
        val enabled = prefs.enabledSchemas.getValue().toMutableSet()
        val default = prefs.defaultSchema.getValue().takeIf { it.isNotBlank() } ?: DEFAULT_SCHEMA
        // The default schema must be present in the list, otherwise Rime falls back to the first.
        enabled += default
        enabled += DEFAULT_SCHEMA
        val order = listOf(default) + enabled.filter { it != default }.sorted()
        return buildString {
            appendLine("# Generated by fcitx5-android (xuexgao). Do not edit by hand;")
            appendLine("# use 功能 → Rime 方案管理 instead.")
            appendLine("patch:")
            appendLine("  schema_list:")
            order.forEach { appendLine("    - schema: $it") }
            appendLine("  menu/page_size: ${prefs.pageSize.getValue()}")
        }
    }

    private fun buildWanxiangCustom(prefs: RimePrefs): String {
        val layout = RimePrefs.DoublePinyin.fromId(prefs.doublePinyin.getValue())
        return buildString {
            appendLine("# Generated by fcitx5-android (xuexgao). Do not edit by hand;")
            appendLine("# use 功能 → Rime 方案管理 instead.")
            appendLine("patch:")
            appendLine("  speller/algebra:")
            appendLine("    __patch:")
            fuzzyOptions(prefs).forEach { appendLine("      - wanxiang_algebra:/$it") }
            // 万象拼音 selects the layout through `wanxiang_algebra:/base/<name>`; 全拼 means
            // "no double pinyin".
            appendLine("      - wanxiang_algebra:/base/${layout.id}")
            appendLine("      - wanxiang_algebra:/26jian")
            appendLine("  menu/page_size: ${prefs.pageSize.getValue()}")
        }
    }

    /**
     * Restores the shipped defaults: the built-in schema set with 万象拼音 as default and 自然码
     * as the double-pinyin layout. Removes the generated files first so a partially written state
     * cannot survive.
     */
    fun resetToDefaults(context: Context) {
        val prefs = RimePrefs.ensureInitialized(context)
        File(userDir, DEFAULT_CUSTOM).delete()
        File(userDir, WANXIANG_CUSTOM).delete()
        prefs.enabledSchemas.setValue(RimePrefs.DEFAULT_SCHEMAS)
        prefs.defaultSchema.setValue(DEFAULT_SCHEMA)
        prefs.doublePinyin.setValue(RimePrefs.DoublePinyin.自然码.id)
        prefs.pageSize.setValue(6)
        listOf(
            prefs.fuzzyZhZ, prefs.fuzzyChC, prefs.fuzzyShS,
            prefs.fuzzyNL, prefs.fuzzyInIng, prefs.fuzzyAnAng
        ).forEach { it.setValue(false) }
        prefs.deployed.setValue(false)
        prefs.deployedSignature.setValue("")
        applyConfiguration(context, force = true)
    }

    /** Copies the generated config into [target] so the user can keep or share it. */
    fun exportConfiguration(context: Context, target: File): Boolean {
        applyConfiguration(context, force = true)
        return runCatching {
            target.parentFile?.mkdirs()
            target.bufferedWriter().use { out ->
                listOf(DEFAULT_CUSTOM, WANXIANG_CUSTOM).forEach { name ->
                    val f = File(userDir, name)
                    out.appendLine("#### $name")
                    if (f.isFile) out.append(f.readText()) else out.appendLine("# (missing)")
                }
            }
            true
        }.getOrElse {
            Timber.w(it, "Failed to export Rime config")
            false
        }
    }

    /**
     * Imports a previously exported bundle. Unknown keys are ignored; the file is parsed leniently
     * because it is a concatenation of two YAML documents wrapped in `#### <name>` markers.
     */
    fun importConfiguration(context: Context, source: File): Boolean {
        val prefs = RimePrefs.ensureInitialized(context)
        val text = runCatching { source.readText() }.getOrNull() ?: return false
        val sections = text.split(Regex("^#### ", RegexOption.MULTILINE)).filter { it.isNotBlank() }
        var applied = false
        val dir = userDir.apply { if (!isDirectory) mkdirs() }
        sections.forEach { section ->
            val name = section.lineSequence().first().trim()
            val body = section.lineSequence().drop(1).joinToString("\n")
            if (name.endsWith(".yaml") && body.isNotBlank()) {
                runCatching { File(dir, name).writeText(body) }
                    .onSuccess { applied = true }
                    .onFailure { Timber.w(it, "Failed to import $name") }
            }
        }
        if (!applied) return false
        // Re-read the imported YAML so the preference UI matches what was written.
        prefs.deployed.setValue(true)
        prefs.deployedSignature.setValue(configurationSignature(prefs))
        return true
    }

    /** Creates a timestamped backup of the Rime user directory. */
    fun backupUserDir(context: Context): File? = runCatching {
        val src = userDir
        if (!src.isDirectory) return null
        val dest = File(appContext.filesDir, "$BACKUP_PREFIX-${System.currentTimeMillis()}")
        if (!dest.mkdirs()) return null
        src.listFiles()?.forEach { f ->
            if (f.isFile) runCatching { f.copyTo(File(dest, f.name), overwrite = true) }
        }
        dest
    }.getOrNull()
}
