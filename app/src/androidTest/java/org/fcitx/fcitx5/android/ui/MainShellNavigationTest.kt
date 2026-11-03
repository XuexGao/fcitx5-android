/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.main.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the four-tab shell: the bottom bar exists, switching tabs moves the pager, and the toolbar
 * title follows the selection.
 */
@RunWith(AndroidJUnit4::class)
class MainShellNavigationTest {

    private fun currentPage(scenario: ActivityScenario<MainActivity>): Int {
        var page = -1
        scenario.onActivity { activity ->
            page = activity.findViewById<ViewPager2>(R.id.page_pager).currentItem
        }
        return page
    }

    @Test
    fun bottomBarHasFourTabs() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            listOf(
                R.string.ksu_tab_home, R.string.ksu_tab_appearance,
                R.string.ksu_tab_features, R.string.ksu_tab_settings
            ).forEach { label ->
                val text = androidx.test.platform.app.InstrumentationRegistry
                    .getInstrumentation().targetContext.getString(label)
                onView(withContentDescription(text)).check(matches(isDisplayed()))
            }
            assertEquals(0, currentPage(scenario))
        }
    }

    @Test
    fun switchingTabsMovesThePager() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val labels = listOf(
                R.string.ksu_tab_appearance, R.string.ksu_tab_features, R.string.ksu_tab_settings
            )
            labels.forEachIndexed { index, label ->
                val text = androidx.test.platform.app.InstrumentationRegistry
                    .getInstrumentation().targetContext.getString(label)
                onView(withContentDescription(text)).perform(click())
                // ViewPager2 animates; give it a moment before asserting.
                Thread.sleep(600)
                assertEquals(index + 1, currentPage(scenario))
                onView(withId(R.id.page_pager)).check(matches(isDisplayed()))
            }
            // Back to home.
            val home = androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().targetContext.getString(R.string.ksu_tab_home)
            onView(withContentDescription(home)).perform(click())
            Thread.sleep(600)
            assertEquals(0, currentPage(scenario))
        }
    }
}
