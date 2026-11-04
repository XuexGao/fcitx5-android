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
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.ui.compose.FcitxApp
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.ui.setup.SetupActivity
import org.fcitx.fcitx5.android.utils.navigateWithAnim
import org.fcitx.fcitx5.android.utils.parcelable
import org.fcitx.fcitx5.android.utils.startActivity
import splitties.dimensions.dp
import splitties.views.topPadding

/**
 * Hosts the Compose settings app.
 *
 * The four top-level pages are pure Compose. Detail screens that have not been ported to Compose
 * yet are pushed onto the existing navigation graph, which is shown in an overlay layer above the
 * Compose content together with its AppCompat toolbar, so unported screens keep working unchanged
 * while they are migrated one by one.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var navController: NavController
    private lateinit var shareReceiveManager: ShareReceiveManager

    private lateinit var composeView: ComposeView
    private lateinit var detailLayer: ViewGroup
    private lateinit var detailToolbar: Toolbar
    private lateinit var navHostContainer: FragmentContainerView

    private val detailVisible: Boolean
        get() = detailLayer.visibility == View.VISIBLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(buildRootView())
        setSupportActionBar(detailToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        composeView.setContent {
            FcitxApp(
                prefs = AppearancePrefs.getInstance(),
                viewModel = viewModel,
                onOpenLegacyRoute = { route -> openRoute(route) },
                onOpenActivity = { clazz -> startActivity(Intent(this, clazz)) },
            )
        }

        setupNavigation()
        setupInsets()
        processIntent(intent, savedInstanceState)
        checkNotificationPermission()
    }

    private fun buildRootView(): View {
        val root = FrameLayout(this)

        composeView = ComposeView(this)
        root.addView(composeView, matchParentParams())

        detailToolbar = Toolbar(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.toolbar_background))
            elevation = dp(4f)
        }

        navHostContainer = FragmentContainerView(this).apply { id = View.generateViewId() }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                detailToolbar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                navHostContainer,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            )
        }

        detailLayer = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.window_background))
            addView(column, matchParentParams())
        }
        root.addView(detailLayer, matchParentParams())
        return root
    }

    private fun matchParentParams() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
    )

    private fun setupNavigation() {
        val navHost = NavHostFragment()
        supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            add(navHostContainer.id, navHost, "detail-nav-host")
        }
        navController = navHost.navController
        navController.graph = SettingsRoute.createGraph(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.hasRoute<SettingsRoute.Index>()) {
                hideDetail()
            } else {
                showDetail()
                destination.label?.let { viewModel.setToolbarTitle(it.toString()) }
            }
        }
        shareReceiveManager = ShareReceiveManager(this) { message, action ->
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).apply {
                action?.let { callback -> setAction(R.string.edit) { callback() } }
            }.show()
        }
        viewModel.toolbarTitle.observe(this) { supportActionBar?.title = it }
        viewModel.toolbarShadow.observe(this) {
            detailToolbar.elevation = dp(if (it) 4f else 0f)
        }
        detailToolbar.setNavigationOnClickListener {
            if (onBackPressedDispatcher.hasEnabledCallbacks()) {
                onBackPressedDispatcher.onBackPressed()
                return@setNavigationOnClickListener
            }
            if (!navigateBack()) moveTaskToBack(false)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navigateBack()) return
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(detailLayer) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            detailLayer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = navBars.left
                rightMargin = navBars.right
            }
            detailToolbar.topPadding = statusBars.top
            navHostContainer.updatePadding(bottom = navBars.bottom)
            windowInsets
        }
    }

    /** Opens a settings route in the detail layer. */
    fun openRoute(route: SettingsRoute) {
        showDetail()
        navController.popBackStack(SettingsRoute.Index, false)
        navController.navigateWithAnim(route)
    }

    private fun showDetail() {
        detailLayer.visibility = View.VISIBLE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun hideDetail() {
        detailLayer.visibility = View.GONE
    }

    /** Returns true when the back press was handled inside the app. */
    private fun navigateBack(): Boolean {
        if (detailVisible) return navController.navigateUp()
        return false
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

    override fun onSupportNavigateUp(): Boolean {
        if (onBackPressedDispatcher.hasEnabledCallbacks()) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return navigateBack() || super.onSupportNavigateUp() || moveTaskToBack(false)
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
