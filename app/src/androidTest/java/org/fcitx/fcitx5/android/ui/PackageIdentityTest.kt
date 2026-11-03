/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.utils.InputMethodUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the install identity after the `org.fcitx5.xuexgao` rename.
 *
 * Also documents the deliberate split between applicationId (install identity) and namespace (code
 * package / JNI symbol prefix).
 */
@RunWith(AndroidJUnit4::class)
class PackageIdentityTest {

    @Test
    fun applicationIdIsXuexgao() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // The runtime identity must match what the build configured.
        assertEquals(BuildConfig.APPLICATION_ID, context.packageName)
        // Release identity is exactly org.fcitx5.xuexgao; the debug build type appends .debug so it
        // can be installed side by side, which is the pre-existing project convention.
        assertTrue(
            "unexpected applicationId: ${BuildConfig.APPLICATION_ID}",
            BuildConfig.APPLICATION_ID == "org.fcitx5.xuexgao" ||
                BuildConfig.APPLICATION_ID == "org.fcitx5.xuexgao.debug"
        )
        assertTrue(
            "the old package name must not appear: ${BuildConfig.APPLICATION_ID}",
            !BuildConfig.APPLICATION_ID.contains("org.fcitx.fcitx5.android")
        )
    }

    @Test
    fun codeNamespaceIsUnchanged() {
        // The R class still lives in the historical namespace; renaming it would break every
        // JNI entry point and the upstream plugin protocol.
        assertNotNull(org.fcitx.fcitx5.android.R.string.app_name)
        assertTrue(
            "namespace should stay org.fcitx.fcitx5.android",
            org.fcitx.fcitx5.android.R::class.java.name.startsWith("org.fcitx.fcitx5.android")
        )
    }

    @Test
    fun inputMethodServiceIsDeclared() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val component = android.content.ComponentName(context, InputMethodUtil.serviceName)
        val info = context.packageManager.getServiceInfo(component, 0)
        assertNotNull("IME service must be declared in the manifest", info)
        assertEquals(context.packageName, info.packageName)
    }

    @Test
    fun fileProviderAuthoritiesFollowApplicationId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val providers = context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_PROVIDERS
        ).providers.orEmpty()
        val authorities = providers.mapNotNull { it.authority }
        assertTrue(
            "share.fileprovider authority should be derived from applicationId, got $authorities",
            authorities.any { it == "${context.packageName}.share.fileprovider" }
        )
    }
}
