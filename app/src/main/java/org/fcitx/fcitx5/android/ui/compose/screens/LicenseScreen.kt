/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.compose.component.PickerDialog
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.utils.toast
import timber.log.Timber

/**
 * 许可证: the libraries bundled into the APK, read from the `aboutlibraries` JSON that the
 * aboutlibraries plugin generates at build time.
 *
 * One row per library; tapping opens the license text directly when there is exactly one, and asks
 * which one otherwise. Libraries with a `native` tag are sorted after the JVM ones, matching the
 * order the old screen used.
 */
@Composable
fun LicenseScreen(bottomInnerPadding: Dp, onBack: () -> Unit) {
    val context = LocalContext.current
    val libraries = remember { mutableStateListOf<Library>() }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) {
                val json = context.resources.openRawResource(R.raw.aboutlibraries)
                    .bufferedReader()
                    .use { it.readText() }
                Libs.Builder()
                    .withJson(json)
                    .build()
                    .libraries
                    .sortedBy {
                        if (it.tag == "native") it.uniqueId.uppercase() else it.uniqueId.lowercase()
                    }
            }
        }.onSuccess {
            libraries.clear()
            libraries.addAll(it)
        }.onFailure { Timber.w(it, "Unable to read the aboutlibraries metadata") }
        loaded = true
    }

    fun open(url: String?) {
        if (url.isNullOrBlank()) return
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { context.toast(it) }
    }

    PageScaffold(
        title = stringResource(R.string.license),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        if (!loaded) {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(
                        title = stringResource(R.string.loading),
                        icon = Icons.AutoMirrored.Rounded.Article,
                    )
                }
            }
            return@PageScaffold
        }
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                libraries.forEach { library ->
                    NavRow(
                        title = "${library.uniqueId}:${library.artifactVersion}",
                        summary = library.licenses.joinToString { license ->
                            license.spdxId ?: license.name
                        },
                        onClick = {
                            val licenses = library.licenses.toList()
                            when (licenses.size) {
                                0 -> Unit
                                1 -> open(licenses[0].url)
                                else -> PickerDialog(
                                    title = library.uniqueId,
                                    items = licenses.map { it.spdxId ?: it.name },
                                    onDismiss = { },
                                    onSelected = { index -> open(licenses[index].url) },
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

/** Kept for symmetry with the old screen's helper; the URL is what actually gets opened. */
private val License.displayName: String
    get() = spdxId ?: name
