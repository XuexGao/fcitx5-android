/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.rime

/**
 * Where the 万象拼音 dictionary packages come from.
 *
 * The schema package used to be a git submodule bundled into the APK, but the submodule was never
 * actually wired into the build (no gitlink in the tree), so 万象拼音 was never deployed and the
 * settings page reported it as missing. It is downloaded instead now.
 *
 * `base` is the standard package; the "Pro" packages are the same release's 辅助码 (auxiliary code)
 * variants. The language model is a separate, much larger file published by RIME-LMDG.
 */
object RimeDictPackages {

    const val REPO = "amzxyz/rime-wanxiang"
    const val GRAM_REPO = "amzxyz/RIME-LMDG"
    const val GRAM_TAG = "LTS"

    /** Used when the "latest release" lookup fails, so an install still works offline-ish. */
    const val FALLBACK_TAG = "v18.1.10"

    /** Mirror prefixes that are tried in order. Empty string means "github.com directly". */
    val MIRRORS = listOf("https://gh-proxy.org/", "")

    /** 辅助码 variants: asset prefix -> label shown in the UI. */
    val PRO_SCHEMES: List<Pair<String, String>> = listOf(
        "flypy" to "小鹤双拼辅助码",
        "zrm" to "自然码辅助码",
        "hanxin" to "汉心辅助码",
        "wubi" to "五笔辅助码",
        "moqi" to "墨奇辅助码",
        "shouyou" to "手游辅助码",
        "shyplus" to "双拼加加辅助码",
        "tiger" to "虎码辅助码",
        "wx" to "万象辅助码",
    )

    enum class Variant(val id: String, val label: String) {
        Base("base", "Base（标准版）"),
        Pro("pro", "Pro（双拼 / 辅助码增强）");

        companion object {
            fun fromId(id: String): Variant = entries.find { it.id == id } ?: Base
        }
    }

    /** File name of the schema package inside a release. */
    fun assetName(variant: Variant, scheme: String): String = when (variant) {
        Variant.Base -> "rime-wanxiang-base.zip"
        Variant.Pro -> "rime-wanxiang-${scheme.ifBlank { PRO_SCHEMES.first().first }}-fuzhu.zip"
    }

    fun packageUrl(tag: String, variant: Variant, scheme: String): String =
        "https://github.com/$REPO/releases/download/$tag/${assetName(variant, scheme)}"

    fun gramUrl(): String =
        "https://github.com/$GRAM_REPO/releases/download/$GRAM_TAG/wanxiang-lts-zh-hans.gram"

    /** The asset name of the language model, as it is written into the Rime user directory. */
    const val GRAM_FILE = "wanxiang-lts-zh-hans.gram"

    val GRAM_SIZE_HINT = 418_080_812L
}
