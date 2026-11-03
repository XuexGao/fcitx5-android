/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.pages

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceProvider
import org.fcitx.fcitx5.android.ui.design.KsuViews
import org.fcitx.fcitx5.android.ui.main.MainActivity
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute

/**
 * Base class for the four top-level pages.
 *
 * Every page is a scrollable column of KernelSU-style cards. The page rebuilds itself whenever an
 * appearance preference changes, which is what keeps the "界面" page preview live and makes the
 * other pages pick up a new corner radius or accent colour without a restart.
 */
abstract class KsuPageFragment : Fragment() {

    protected val appearance: AppearancePrefs
        get() = AppearancePrefs.ensureInitialized(requireContext())

    private var content: LinearLayout? = null

    private val appearanceListener =
        ManagedPreferenceProvider.OnChangeListener { onAppearanceChanged(it) }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val page = KsuViews.page(ctx) {
            content = this
            buildContent(this)
        }
        return page
    }

    override fun onStart() {
        super.onStart()
        appearance.registerOnChangeListener(appearanceListener)
    }

    override fun onStop() {
        appearance.unregisterOnChangeListener(appearanceListener)
        super.onStop()
    }

    /** Rebuilds the whole page body in place. */
    protected fun rebuildPage() {
        val target = content ?: return
        target.removeAllViews()
        buildContent(target)
    }

    /**
     * Called for any appearance preference change. Discrete changes rebuild the page; the
     * appearance page overrides [onAppearanceChanged] to avoid fighting an in-progress slider drag.
     */
    protected open fun onAppearanceChanged(key: String) {
        rebuildPage()
    }

    /** Fills [container] with the page content. Called on create and on every rebuild. */
    protected abstract fun buildContent(container: LinearLayout)

    /** Navigates to an existing settings screen, if the host activity supports it. */
    protected fun openRoute(route: SettingsRoute) {
        (activity as? MainActivity)?.openRoute(route)
    }

    protected fun toast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT)
            .show()
    }

    /** Convenience accessor for the scroll view that hosts this page. */
    protected val pageScroll: ScrollView?
        get() = view as? ScrollView
}
