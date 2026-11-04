/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */

import org.gradle.api.JavaVersion

object Versions {

    val java = JavaVersion.VERSION_17
    // miuix 0.9.4 (the KernelSU-Style-UI-Kit component library) requires compiling against API 37.
    // minSdk stays at 23 so Android 8+ devices keep working; compileSdk only affects which APIs
    // are available at compile time.
    const val compileSdk = 37
    const val minSdk = 23
    const val targetSdk = 36

    const val defaultCMake = "3.31.6"
    const val defaultNDK = "28.0.13004108"
    const val defaultBuildTools = "37.0.0"

    // NOTE: increase this value to bump version code
    const val baseVersionCode = 11
    const val baseVersionName = "0.1.3"

    val supportedABIs = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    const val fallbackABI = "arm64-v8a"

    fun calculateVersionCode(abi: String = fallbackABI): Int {
        val abiId = when (abi) {
            "armeabi-v7a" -> 1
            "arm64-v8a" -> 2
            "x86" -> 3
            "x86_64" -> 4
            else -> 0
        }
        return baseVersionCode * 10 + abiId
    }
}
