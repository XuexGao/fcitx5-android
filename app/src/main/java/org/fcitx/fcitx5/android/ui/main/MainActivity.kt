/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.snackbar.Snackbar
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.ui.compose.ComposeRouteHost
import org.fcitx.fcitx5.android.ui.compose.FcitxApp
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.ui.setup.SetupActivity
import org.fcitx.fcitx5.android.utils.parcelable
import org.fcitx.fcitx5.android.utils.startActivity

/**
 * Hosts the Compose settings app.
 *
 * The whole settings UI — the four pages and every detail screen — is Compose and lives in
 * [FcitxApp]'s back stack. This activity only provides the window, forwards routes that arrive from
 * outside (deep links, the keyboard's toolbar buttons, share intents) to [ComposeRouteHost] and
 * handles the app-level concerns the settings UI has no business knowing about (notification
 * permission, first-run wizard, share receiving).
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var shareReceiveManager: ShareReceiveManager

    /**
     * Routes that arrive while the Compose host is already composed (or shortly before) are parked
     * here and consumed by [FcitxApp].
     */
    private val routeHost = ComposeRouteHost()

    private lateinit var composeView: ComposeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(buildRootView())

        composeView.setContent {
            FcitxApp(
                prefs = AppearancePrefs.getInstance(),
                viewModel = viewModel,
                onOpenActivity = { clazz -> startActivity(Intent(this, clazz)) },
                onOpenIntent = { intent -> startActivity(intent) },
                routeHost = routeHost,
            )
        }

        shareReceiveManager = ShareReceiveManager(this) { message, action ->
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).apply {
                action?.let { callback -> setAction(R.string.edit) { callback() } }
            }.show()
        }
        processIntent(intent, savedInstanceState)
        checkNotificationPermission()
    }

    private fun buildRootView(): FrameLayout {
        composeView = ComposeView(this)
        return FrameLayout(this).apply {
            addView(
                composeView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }
    }

    /** Opens a settings route, wherever it comes from. */
    fun openRoute(route: SettingsRoute) {
        routeHost.open(route)
    }

    private fun processIntent(intent: Intent?, savedInstanceState: Bundle? = null) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_MAIN -> if (SetupActivity.shouldShowUp()) {
                SetupActivity.markAutoShown()
                startActivity<SetupActivity>()
            }
            Intent.ACTION_VIEW -> intent.data?.let {
                AlertDialog.Builder(this)
                    .setTitle(R.string.pinyin_dict)
                    .setMessage(R.string.whether_import_dict)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        openRoute(SettingsRoute.PinyinDict(it))
                    }
                    .show()
            }
            Intent.ACTION_RUN -> {
                val route = intent.parcelable<SettingsRoute>(EXTRA_SETTINGS_ROUTE) ?: return
                openRoute(route)
            }
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {
                shareReceiveManager.handle(intent, savedInstanceState)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent, null)
    }

    private var needNotifications by AppPrefs.getInstance().internal.needNotifications

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                needNotifications = true
                return
            }
            if (!needNotifications) return
            AlertDialog.Builder(this)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.notification_permission_title)
                .setMessage(R.string.notification_permission_message)
                .setNegativeButton(R.string.i_do_not_need_it) { _, _ ->
                    needNotifications = false
                }
                .setPositiveButton(R.string.grant_permission) { _, _ ->
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
                }
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 0) return
        needNotifications = grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStop() {
        viewModel.fcitx.runIfReady { save() }
        super.onStop()
    }

    companion object {
        const val EXTRA_SETTINGS_ROUTE = "${BuildConfig.APPLICATION_ID}.EXTRA_SETTINGS_ROUTE"
    }
}
