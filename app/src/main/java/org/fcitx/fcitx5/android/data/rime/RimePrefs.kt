/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.rime

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference

/**
 * User-facing Rime configuration.
 *
 * Only the things the graphical config page edits live here; everything else stays in the Rime
 * YAML files. The prefs are the source of truth and [RimeManager] renders them into
 * `default.custom.yaml` / `wanxiang.custom.yaml` on every change.
 */
class RimePrefs(sharedPreferences: SharedPreferences) {

    /** Schemas the user wants available in Rime's schema list, in order. */
    val enabledSchemas = ManagedPreference.PStringSet(
        sharedPreferences, KEY_ENABLED_SCHEMAS, DEFAULT_SCHEMAS
    )

    /** Schema Rime starts with. */
    val defaultSchema = ManagedPreference.PString(
        sharedPreferences, KEY_DEFAULT_SCHEMA, DEFAULT_SCHEMA
    )

    /** Double-pinyin layout used by the 万象 schema. */
    val doublePinyin = ManagedPreference.PString(
        sharedPreferences, KEY_DOUBLE_PINYIN, DoublePinyin.自然码.id
    )

    /** Candidate page size written into the generated custom config. */
    val pageSize = ManagedPreference.PInt(sharedPreferences, KEY_PAGE_SIZE, 6)

    /** Fuzzy pinyin toggles, written as algebra patches. */
    val fuzzyZhZ = ManagedPreference.PBool(sharedPreferences, KEY_FUZZY_ZH_Z, false)
    val fuzzyChC = ManagedPreference.PBool(sharedPreferences, KEY_FUZZY_CH_C, false)
    val fuzzyShS = ManagedPreference.PBool(sharedPreferences, KEY_FUZZY_SH_S, false)
    val fuzzyNL = ManagedPreference.PBool(sharedPreferences, KEY_FUZZY_N_L, false)
    val fuzzyInIng = ManagedPreference.PBool(sharedPreferences, KEY_FUZZY_IN_ING, false)
    val fuzzyAnAng = ManagedPreference.PBool(sharedPreferences, KEY_FUZZY_AN_ANG, false)

    /** Set once the generated custom config has been written to the Rime user directory. */
    val deployed = ManagedPreference.PBool(sharedPreferences, KEY_DEPLOYED, false)

    /** Schema whose config was last written, so a stale file can be detected. */
    val deployedSignature = ManagedPreference.PString(sharedPreferences, KEY_DEPLOYED_SIGNATURE, "")

    /**
     * Double-pinyin layouts offered by 万象拼音's `wanxiang_algebra`.
     * The [id] is the exact key used in the algebra patch.
     */
    enum class DoublePinyin(val id: String, val label: String) {
        自然码("自然码", "自然码"),
        小鹤双拼("小鹤双拼", "小鹤双拼"),
        搜狗双拼("搜狗双拼", "搜狗双拼"),
        微软双拼("微软双拼", "微软双拼"),
        智能ABC("智能ABC", "智能ABC"),
        紫光双拼("紫光双拼", "紫光双拼"),
        国标双拼("国标双拼", "国标双拼"),
        拼音加加("拼音加加", "拼音加加"),
        全拼("全拼", "全拼（不启用双拼）");

        companion object {
            fun fromId(id: String): DoublePinyin = entries.find { it.id == id } ?: 自然码
        }
    }

    companion object {
        const val KEY_ENABLED_SCHEMAS = "rime_enabled_schemas"
        const val KEY_DEFAULT_SCHEMA = "rime_default_schema"
        const val KEY_DOUBLE_PINYIN = "rime_double_pinyin"
        const val KEY_PAGE_SIZE = "rime_page_size"
        const val KEY_FUZZY_ZH_Z = "rime_fuzzy_zh_z"
        const val KEY_FUZZY_CH_C = "rime_fuzzy_ch_c"
        const val KEY_FUZZY_SH_S = "rime_fuzzy_sh_s"
        const val KEY_FUZZY_N_L = "rime_fuzzy_n_l"
        const val KEY_FUZZY_IN_ING = "rime_fuzzy_in_ing"
        const val KEY_FUZZY_AN_ANG = "rime_fuzzy_an_ang"
        const val KEY_DEPLOYED = "rime_deployed"
        const val KEY_DEPLOYED_SIGNATURE = "rime_deployed_signature"

        /** The 万象拼音 schema, shipped by the rime plugin. */
        const val DEFAULT_SCHEMA = "wanxiang"

        val DEFAULT_SCHEMAS: Set<String> = setOf(
            "wanxiang", "luna_pinyin_simp", "luna_pinyin", "stroke"
        )

        private var instance: RimePrefs? = null

        fun ensureInitialized(context: Context): RimePrefs {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                return RimePrefs(
                    PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
                ).also { instance = it }
            }
        }
    }
}
