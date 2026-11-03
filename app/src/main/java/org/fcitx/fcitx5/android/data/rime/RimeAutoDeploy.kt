/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.rime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import timber.log.Timber

/**
 * First-run glue for the Rime integration.
 *
 * Two things have to happen before 万象拼音 is usable:
 *  1. the generated custom config must exist in the user directory (schema list, default schema,
 *     double-pinyin layout);
 *  2. librime has to compile the dictionaries. fcitx5-rime already does a full check on its first
 *     start, so no explicit trigger is needed — but the user can force one from the config page
 *     (see [redeploy]).
 */
object RimeAutoDeploy {

    /** Rime's own "Deploy" action id, registered by fcitx5-rime. */
    private const val DEPLOY_ACTION = "fcitx-rime-deploy"

    /**
     * Writes the custom config if it has not been written yet. Cheap and idempotent: after the
     * first successful run the signature check short-circuits.
     *
     * @return true when files were written on this call
     */
    suspend fun ensureDeployed(context: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val written = RimeManager.applyConfiguration(context)
            if (written.isNotEmpty()) {
                Timber.i("Rime custom config written: ${written.joinToString { it.name }}")
            }
            written.isNotEmpty()
        }.getOrElse {
            Timber.w(it, "Rime auto deploy failed")
            false
        }
    }

    /**
     * Asks the running fcitx instance to redeploy Rime.
     *
     * Falls back to a plain config reload when the Rime engine is not active, so the button always
     * does something observable instead of failing silently.
     */
    suspend fun redeploy(fcitx: FcitxConnection): Boolean {
        RimeManager.applyConfiguration(org.fcitx.fcitx5.android.utils.appContext, force = true)
        var triggered = false
        runCatching {
            fcitx.runOnReady { f ->
                val action = deployAction(f)
                if (action != null) {
                    f.activateAction(action)
                    triggered = true
                } else {
                    f.reloadConfig()
                }
            }
        }.onFailure { Timber.w(it, "Rime redeploy failed") }
        return triggered
    }

    private suspend fun deployAction(fcitx: FcitxAPI): Int? = runCatching {
        fcitx.statusArea().firstOrNull { it.name == DEPLOY_ACTION }?.id
    }.getOrNull()
}
