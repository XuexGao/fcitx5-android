/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.utils.AppUtil
import org.fcitx.fcitx5.android.utils.appContext

class MainViewModel : ViewModel() {
    private val appLabel = AppUtil.appLabel(appContext)

    enum class ButtonMode { NONE, EDIT, DELETE }

    val toolbarTitle = MutableLiveData(appLabel)

    val toolbarShadow = MutableLiveData(true)

    val toolbarButton = MutableLiveData(ButtonMode.NONE)

    val fcitx: FcitxConnection = FcitxDaemon.connect(javaClass.name)

    fun setToolbarTitle(title: String) {
        toolbarTitle.value = title
    }

    fun enableToolbarShadow() {
        toolbarShadow.value = true
    }

    fun disableToolbarShadow() {
        toolbarShadow.value = false
    }

    /**
     * Asks the running fcitx instance to redeploy Rime. Fire-and-forget: the result is reflected by
     * the Rime screen's status section on the next composition.
     */
    fun requestRimeRedeploy() {
        viewModelScope.launch {
            org.fcitx.fcitx5.android.data.rime.RimeAutoDeploy.redeploy(fcitx)
        }
    }

    fun disableToolbarEditButton() {
        toolbarButton.value = ButtonMode.NONE
    }

    override fun onCleared() {
        FcitxDaemon.disconnect(javaClass.name)
    }
}
