/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.FcitxDaemon

class MainViewModel : ViewModel() {

    val fcitx: FcitxConnection = FcitxDaemon.connect(javaClass.name)

    /**
     * Asks the running fcitx instance to redeploy Rime. Fire-and-forget: the result is reflected by
     * the Rime screen's status section on the next composition.
     */
    fun requestRimeRedeploy() {
        viewModelScope.launch {
            org.fcitx.fcitx5.android.data.rime.RimeAutoDeploy.redeploy(fcitx)
        }
    }

    override fun onCleared() {
        FcitxDaemon.disconnect(javaClass.name)
    }
}
