/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import androidx.fragment.app.Fragment

/**
 * Nav graph root.
 *
 * The four top-level pages live outside the navigation host (they are hosted by a ViewPager in
 * [org.fcitx.fcitx5.android.ui.main.MainActivity]), so the graph needs a start destination that is
 * never actually shown. Keeping it empty avoids instantiating a settings screen that would fight
 * with the pager for the toolbar and menu.
 */
class IndexPlaceholderFragment : Fragment()
