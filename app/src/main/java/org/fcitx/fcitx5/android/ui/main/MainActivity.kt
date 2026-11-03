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
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.databinding.ActivityMainBinding
import org.fcitx.fcitx5.android.ui.design.KsuBottomBar
import org.fcitx.fcitx5.android.ui.design.bottomBarFloating
import org.fcitx.fcitx5.android.ui.design.bottomBarGlass
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.ui.pages.AppearancePageFragment
import org.fcitx.fcitx5.android.ui.pages.FeaturesPageFragment
import org.fcitx.fcitx5.android.ui.pages.HomePageFragment
import org.fcitx.fcitx5.android.ui.pages.SettingsPageFragment
import org.fcitx.fcitx5.android.ui.setup.SetupActivity
import org.fcitx.fcitx5.android.utils.navigateWithAnim
import org.fcitx.fcitx5.android.utils.parcelable
import org.fcitx.fcitx5.android.utils.startActivity
import splitties.dimensions.dp
import splitties.views.topPadding

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var shareReceiveManager: ShareReceiveManager

    private var navBarInset = 0
    private var lastGlassRefresh = 0L

    /** `true` while a detail route is covering the pager. */
    private val detailVisible: Boolean
        get() = binding.navHostContainer.visibility == View.VISIBLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = navBars.left
                rightMargin = navBars.right
            }
            binding.toolbar.topPadding = statusBars.top
            navBarInset = navBars.bottom
            binding.pageContainer.updatePadding(bottom = navBars.bottom)
            applyBottomBarInsets()
            windowInsets
        }
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        // always show toolbar back arrow icon
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        navController = binding.navHostFragment.getFragment<NavHostFragment>().navController
        navController.graph = SettingsRoute.createGraph(navController)
        shareReceiveManager = ShareReceiveManager(this) { message, action ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).apply {
                action?.let { callback ->
                    setAction(R.string.edit) { callback() }
                }
            }.show()
        }
        binding.toolbar.setNavigationOnClickListener {
            // A detail screen may have an active OnBackPressedCallback (for example "save before
            // leaving"), so route through the dispatcher first.
            if (detailVisible) {
                onBackPressedDispatcher.onBackPressed()
            } else if (binding.pagePager.currentItem != 0) {
                binding.pagePager.setCurrentItem(0, true)
            } else {
                // "minimize" the activity if we can't go back
                moveTaskToBack(false)
            }
        }
        // Modern back handling: register a callback instead of overriding onBackPressed(), so the
        // system default (finish) still runs once we are done.
        onBackPressedDispatcher.addCallback(
            this, object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (navigateBackInternal()) return
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        )
        viewModel.toolbarTitle.observe(this) {
            supportActionBar!!.title = it
        }
        viewModel.toolbarShadow.observe(this) {
            binding.toolbar.elevation = dp(if (it) 4f else 0f)
        }
        navController.addOnDestinationChangedListener { _, dest, _ ->
            if (dest.hasRoute<SettingsRoute.Index>()) {
                hideDetailHost()
            } else {
                showDetailHost()
                dest.label?.let { viewModel.setToolbarTitle(it.toString()) }
            }
            if (dest.hasRoute<SettingsRoute.Theme>()) {
                viewModel.disableToolbarShadow()
            } else {
                viewModel.enableToolbarShadow()
            }
        }
        setupPages()
        processIntent(intent, savedInstanceState)
        checkNotificationPermission()
    }

    // ---- Four-page shell --------------------------------------------------------

    private fun setupPages() {
        binding.pagePager.adapter = PagesAdapter()
        binding.pagePager.offscreenPageLimit = 1
        binding.bottomBar.setItems(
            listOf(
                KsuBottomBar.Item(R.drawable.ic_baseline_home_24, getString(R.string.ksu_tab_home)),
                KsuBottomBar.Item(
                    R.drawable.ic_baseline_palette_24, getString(R.string.ksu_tab_appearance)
                ),
                KsuBottomBar.Item(
                    R.drawable.ic_baseline_extension_24, getString(R.string.ksu_tab_features)
                ),
                KsuBottomBar.Item(
                    R.drawable.ic_baseline_settings_24, getString(R.string.ksu_tab_settings)
                )
            )
        )
        binding.bottomBar.floating = bottomBarFloating()
        binding.bottomBar.glassEnabled = bottomBarGlass()
        binding.bottomBar.onSelect = { index ->
            binding.pagePager.setCurrentItem(index, true)
        }
        binding.pagePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomBar.select(position)
                binding.bottomBar.refreshBackdrop(binding.pageContainer)
                if (!detailVisible) {
                    viewModel.setToolbarTitle(tabTitle(position))
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    binding.bottomBar.refreshBackdrop(binding.pageContainer)
                }
            }
        })
        viewModel.setToolbarTitle(tabTitle(0))
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        binding.bottomBar.post { binding.bottomBar.refreshBackdrop(binding.pageContainer) }

        // Keep the blurred strip in sync while any page scrolls.
        binding.pageContainer.viewTreeObserver.addOnScrollChangedListener {
            val now = System.currentTimeMillis()
            if (now - lastGlassRefresh < 120L) return@addOnScrollChangedListener
            lastGlassRefresh = now
            binding.bottomBar.refreshBackdrop(binding.pageContainer)
        }
    }

    private fun tabTitle(position: Int): String = getString(
        when (position) {
            0 -> R.string.ksu_tab_home
            1 -> R.string.ksu_tab_appearance
            2 -> R.string.ksu_tab_features
            else -> R.string.ksu_tab_settings
        }
    )

    private fun applyBottomBarInsets() {
        val params = binding.bottomBar.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val target = navBarInset + if (bottomBarFloating()) dp(12) else 0
        if (params.bottomMargin != target) {
            params.bottomMargin = target
            binding.bottomBar.layoutParams = params
        }
    }

    /**
     * Opens a settings route on top of the pager. The four pages live outside the navigation host,
     * so they cannot call `findNavController()` themselves.
     */
    fun openRoute(route: SettingsRoute) {
        showDetailHost()
        navController.popBackStack(SettingsRoute.Index, false)
        navController.navigateWithAnim(route)
    }

    private fun showDetailHost() {
        binding.navHostContainer.visibility = View.VISIBLE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun hideDetailHost() {
        binding.navHostContainer.visibility = View.GONE
        binding.bottomBar.select(binding.pagePager.currentItem)
        viewModel.setToolbarTitle(tabTitle(binding.pagePager.currentItem))
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    /**
     * Returns true when the back press was handled inside the app.
     *
     * Order: close a detail screen, otherwise return to the home tab.
     */
    private fun navigateBackInternal(): Boolean {
        if (detailVisible) {
            return navController.navigateUp()
        }
        if (binding.pagePager.currentItem != 0) {
            binding.pagePager.setCurrentItem(0, true)
            return true
        }
        return false
    }

    private inner class PagesAdapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = PAGE_COUNT

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> HomePageFragment()
            1 -> AppearancePageFragment()
            2 -> FeaturesPageFragment()
            else -> SettingsPageFragment()
        }
    }

    // ---- Existing behaviour -----------------------------------------------------

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent, null)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (detailVisible) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        if (binding.pagePager.currentItem != 0) {
            binding.pagePager.setCurrentItem(0, true)
            return true
        }
        return super.onSupportNavigateUp() || moveTaskToBack(false)
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

    private var needNotifications by AppPrefs.getInstance().internal.needNotifications

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                needNotifications = true
                return
            }
            // do not ask again if user denied the request
            if (!needNotifications) return
            // always show a dialog to explain why we need notification permission,
            // regardless of `shouldShowRequestPermissionRationale(...)`
            AlertDialog.Builder(this)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.notification_permission_title)
                .setMessage(R.string.notification_permission_message)
                .setNegativeButton(R.string.i_do_not_need_it) { _, _ ->
                    // do not ask again if user denied the request
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
        // do not ask again if user denied the request
        needNotifications = grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        // Appearance prefs may have changed while another screen was in front.
        binding.bottomBar.floating = bottomBarFloating()
        binding.bottomBar.glassEnabled = bottomBarGlass()
        binding.bottomBar.refreshChrome()
        applyBottomBarInsets()
        binding.bottomBar.post { binding.bottomBar.refreshBackdrop(binding.pageContainer) }
    }

    override fun onStop() {
        viewModel.fcitx.runIfReady {
            save()
        }
        super.onStop()
    }

    companion object {
        const val EXTRA_SETTINGS_ROUTE = "${BuildConfig.APPLICATION_ID}.EXTRA_SETTINGS_ROUTE"
        private const val PAGE_COUNT = 4
    }
}
