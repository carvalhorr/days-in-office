package com.carvalhorr.daysInOffice.smoke.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carvalhorr.daysInOffice.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * End-to-end smoke walk through every reachable surface in the app.
 *
 * Goal: detect crashes and dead-end navigations (BUG-008, BUG-010, BUG-011 etc.).
 * Assertions are deliberately minimal — each test exercises a flow and asserts only
 * that an anchor screen renders. Anything richer becomes fragile fast and belongs
 * in a per-feature test.
 *
 * Persisted state (Room, DataStore) is NOT reset between tests; instead, helpers
 * adapt to either start screen (Onboarding or Dashboard).
 *
 * Hilt: the test runner is the vanilla `AndroidJUnitRunner` and the real
 * `@HiltAndroidApp DaysInOfficeApp` Application class boots normally, so the
 * production DI graph initialises. No hilt-android-testing dependency is needed
 * (and `app/build.gradle.kts` is a protected file we cannot modify).
 */
@RunWith(AndroidJUnit4::class)
class MainFlowSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // --- finder helpers ----------------------------------------------------

    // On physical devices the Compose hierarchy may not be ready yet when the
    // test rule first polls — returning false lets waitUntil retry instead of crash.
    private fun anyNodeWithText(text: String): Boolean =
        try {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        } catch (e: IllegalStateException) {
            false
        }

    /**
     * Assert a screen anchor is on-screen, tolerating duplicates. Strings like
     * "Settings" and "Calendar" appear on BOTH the bottom-nav tab label and the
     * destination TopAppBar title once you're on that screen, so a strict
     * `onNodeWithText(x).assertIsDisplayed()` is wrong — it fails when the user
     * has actually arrived at the expected screen, which is the opposite of what
     * we want.
     */
    private fun assertOnScreen(anchor: String) {
        composeRule.onAllNodesWithText(anchor).onFirst().assertIsDisplayed()
    }

    private fun waitForStartScreen() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            anyNodeWithText("Days in Office") || anyNodeWithText("Setup")
        }
    }

    private fun ensureAtDashboard() {
        // Wait up to 20s for Dashboard. Walk Onboarding if we land there first.
        // If the nav back-stack was restored from a previous test (e.g. Calendar),
        // click the Home tab to navigate back to Dashboard.
        val deadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < deadline) {
            if (anyNodeWithText("Days in Office")) return
            if (anyNodeWithText("Setup")) {
                // Guard: only walk onboarding when the navigation buttons are
                // also rendered. "Setup" can appear transiently in the semantics
                // tree (e.g. the OnboardingScreen TopAppBar composes before its
                // nav-button Row during a navigation animation). Calling
                // walkOnboarding() in that window fails with "Finish not found".
                if (anyNodeWithText("Next") || anyNodeWithText("Finish")) {
                    walkOnboarding()
                }
                composeRule.waitForIdle()
                continue
            }
            // Not on Dashboard or Onboarding — may be on Calendar/Settings
            if (anyNodeWithText("Home")) {
                composeRule.onNodeWithText("Home").performClick()
            }
            composeRule.waitForIdle()
        }
        // Use waitUntil (existence check) instead of assertIsDisplayed: the TopAppBar
        // title can be in-tree but transiently outside the visible clip during a
        // navigation animation, causing assertIsDisplayed to fail spuriously.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            anyNodeWithText("Days in Office")
        }
    }

    private fun walkOnboarding() {
        // Click Next until Finish appears, then Finish.
        var safetyCounter = 0
        while (anyNodeWithText("Next") && safetyCounter < 10) {
            composeRule.onNodeWithText("Next").performClick()
            composeRule.waitForIdle()
            safetyCounter++
        }
        // Guard: if the app transitioned to Dashboard while we were clicking Next
        // (transient onboarding composing then settling), "Finish" won't be in the
        // tree. Skip the click — ensureAtDashboard()'s loop will detect Dashboard
        // on its next iteration.
        if (anyNodeWithText("Finish")) {
            composeRule.onNodeWithText("Finish").performClick()
            composeRule.waitForIdle()
        }
    }

    private fun openSettingsViaGear() {
        composeRule.onNodeWithText("⚙️").performClick()
        composeRule.waitForIdle()
        assertOnScreen("Settings")
    }

    private fun openSettingsViaTab() {
        // Bottom-nav "Settings" tab — distinct from the gear icon. Both land on
        // the same destination but exercise different nav-stack code paths.
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.waitForIdle()
    }

    private fun goHomeViaTab() {
        composeRule.onNodeWithText("Home").performClick()
        composeRule.waitForIdle()
    }

    private fun goCalendarViaTab() {
        composeRule.onNodeWithText("Calendar").performClick()
        composeRule.waitForIdle()
    }

    /** Open a settings row and immediately Cancel out of its bottom sheet. */
    private fun openAndCancelRow(rowLabel: String) {
        composeRule.onNodeWithText(rowLabel).performClick()
        // ModalBottomSheet animates in; `waitForIdle()` can return before the
        // sheet content composes. Poll for Cancel explicitly (mirrors d02 fix).
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("Cancel").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
    }

    /**
     * Toggle the single `Switch` inside the currently-open bottom sheet. Used to
     * force the Wi-Fi / Geofence pickers to actually compose — they sit behind
     * `if (draftEnabled) { ...Picker(...) }`, so just opening the sheet doesn't
     * exercise them. Without flipping the switch, BUG-011 sleeps.
     */
    private fun toggleEnableSwitchInSheet() {
        composeRule.onNode(isToggleable()).performClick()
        composeRule.waitForIdle()
    }

    // --- launch / onboarding -----------------------------------------------

    @Test
    fun a01_appLaunchesAndReachesAStartScreen() {
        waitForStartScreen()
        val onDashboard = anyNodeWithText("Days in Office")
        val onOnboarding = anyNodeWithText("Setup")
        assert(onDashboard || onOnboarding) {
            "Neither Dashboard nor Onboarding rendered within 10s of launch"
        }
    }

    @Test
    fun a02_onboardingFlowCompletesIfPresent() {
        waitForStartScreen()
        if (!anyNodeWithText("Setup")) {
            // Already onboarded from a previous test run — flow not reachable now.
            return
        }
        walkOnboarding()
        assertOnScreen("Days in Office")
    }

    // --- dashboard ----------------------------------------------------------

    @Test
    fun b01_dashboardCheckInOfficeButtonRespondsToClick() {
        ensureAtDashboard()
        // QuickCheckInButton renders two "🏢 Office" variants; either should
        // dispatch its onClick without crashing and leave us on Dashboard.
        composeRule.onAllNodes(hasText("🏢 Office")).onFirst().performClick()
        composeRule.waitForIdle()
        assertOnScreen("Days in Office")
    }

    @Test
    fun b02_dashboardCheckInRemoteButtonRespondsToClick() {
        ensureAtDashboard()
        composeRule.onAllNodes(hasText("🏠 Remote")).onFirst().performClick()
        composeRule.waitForIdle()
        // Tighter: verify today's record actually flipped to REMOTE (catches regressions
        // where the click fires but RecordRemoteDayUseCase / DashboardViewModel state
        // update never propagates — BUG-022 class).
        composeRule.waitUntil(timeoutMillis = 5_000) {
            anyNodeWithText("Marked as remote day")
        }
        assertOnScreen("Days in Office")
    }

    // --- bottom-nav cycling -------------------------------------------------

    @Test
    fun c01_bottomNavCyclesHomeCalendarSettings() {
        ensureAtDashboard()
        goCalendarViaTab()
        assertOnScreen("Calendar")
        openSettingsViaTab()
        assertOnScreen("Settings")
        goHomeViaTab()
        assertOnScreen("Days in Office")
    }

    /**
     * BUG-008 regression guard. Today: opening Settings via the gear icon performs
     * a plain drill-in push, and tapping the Home tab afterwards does nothing.
     * Expected: tapping Home returns to Dashboard. This test will fail until
     * BUG-008 is fixed.
     */
    @Test
    fun c02_homeTabReturnsToDashboardAfterGearIconSettings() {
        ensureAtDashboard()
        openSettingsViaGear()
        goHomeViaTab()
        assertOnScreen("Days in Office")
    }

    // --- calendar -----------------------------------------------------------

    @Test
    fun d01_calendarMonthNavPrevNextWorks() {
        ensureAtDashboard()
        goCalendarViaTab()
        composeRule.onNodeWithContentDescription("Previous month").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Next month").performClick()
        composeRule.waitForIdle()
        assertOnScreen("Calendar")
    }

    /**
     * BUG-018 regression guard. Marking a day PTO from Calendar must update the
     * Dashboard's compliance calculation reactively (no restart).
     *
     * Flow: Dashboard Office check-in → Calendar PTO override for today →
     * Dashboard "On PTO today" confirms the reactive write propagated.
     */
    @Test
    fun d03_calendarPtoUpdatesDashboardDenominator() {
        ensureAtDashboard()
        // Mark today as Office so today's Calendar cell has a record and is tappable
        composeRule.onAllNodes(hasText("🏢 Office")).onFirst().performClick()
        composeRule.waitForIdle()

        goCalendarViaTab()

        val today = LocalDate.now().toString()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasContentDescription(today, substring = true))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onAllNodes(hasContentDescription(today, substring = true))
            .onFirst()
            .performClick()
        composeRule.waitForIdle()

        // Wait for sheet and mark today as PTO
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Mark as PTO").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Mark as PTO").performClick()
        composeRule.waitForIdle()

        // Return to Dashboard — reactive update must propagate without app restart
        goHomeViaTab()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            anyNodeWithText("On PTO today")
        }
        assertOnScreen("Days in Office")
    }

    /**
     * Tap a day cell to open the day-detail sheet. Day cells expose a
     * contentDescription of the form "YYYY-MM-DD, Status" — BUT only when
     * `isCurrentMonth && record != null` (see MonthCalendarView). So before going
     * to Calendar, do an Office check-in on Dashboard so today gets a record and
     * the cell becomes findable.
     */
    @Test
    fun d02_calendarDayDetailSheetOpens() {
        ensureAtDashboard()
        composeRule.onAllNodes(hasText("🏢 Office")).onFirst().performClick()
        composeRule.waitForIdle()
        goCalendarViaTab()
        val today = LocalDate.now().toString()
        // The Calendar VM's Flow may not emit instantly after the Dashboard
        // check-in writes its record. Poll for today's cell semantics rather than
        // racing the emission.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasContentDescription(today, substring = true))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onAllNodes(hasContentDescription(today, substring = true))
            .onFirst()
            .performClick()
        composeRule.waitForIdle()
        // ModalBottomSheet animates in from the bottom; `assertIsDisplayed()` is
        // unreliable against animated overlay content because the node can be
        // composed but still outside the visible clip during the animation.
        // Checking node existence is sufficient for a smoke test: if the button
        // is in the semantics tree the sheet opened successfully.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Mark as Office").fetchSemanticsNodes().isNotEmpty()
        }
    }

    // --- settings: open + cancel every row ---------------------------------

    @Test
    fun e01_settingsMandateRowsOpenAndCancel() {
        ensureAtDashboard()
        openSettingsViaGear()
        openAndCancelRow("Target")
        openAndCancelRow("Period")
        openAndCancelRow("Working days")
        assertOnScreen("Settings")
    }

    /**
     * BUG-011 regression guard. The Wi-Fi / Geofence sheets gate their picker
     * behind `if (draftEnabled) { ...Picker(...) }`, so just opening the sheet
     * doesn't compose the picker — the first test run had these wrongly passing
     * on fresh emulator state. We must flip the Enable switch — that triggers
     * `WifiSsidPicker` / `GeofencePicker` to compose, which today crashes because
     * it uses `viewModel()` against a `@HiltViewModel`. Each surface gets its
     * own @Test so the failure report names exactly which picker crashed.
     */
    @Test
    fun e02_settingsWifiConnectedRowOpensAndEnablesWithoutCrash() {
        ensureAtDashboard()
        openSettingsViaGear()
        composeRule.onNodeWithText("Wi-Fi (connected)").performClick()
        composeRule.waitForIdle()
        toggleEnableSwitchInSheet()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        assertOnScreen("Settings")
    }

    @Test
    fun e03_settingsWifiScanRowOpensAndEnablesWithoutCrash() {
        ensureAtDashboard()
        openSettingsViaGear()
        composeRule.onNodeWithText("Wi-Fi (scan only)").performClick()
        composeRule.waitForIdle()
        toggleEnableSwitchInSheet()
        // Tighter: assert WifiSsidPicker actually composed (its "Scan for networks"
        // button is the first visible element). Previously this crashed because the
        // picker used viewModel() instead of hiltViewModel() — BUG-011 class.
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("Scan for networks").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        assertOnScreen("Settings")
    }

    @Test
    fun e04_settingsGeofencingRowOpensAndEnablesWithoutCrash() {
        ensureAtDashboard()
        openSettingsViaGear()
        composeRule.onNodeWithText("Geofencing").performClick()
        composeRule.waitForIdle()
        toggleEnableSwitchInSheet()
        // Tighter: assert GeofencePicker actually composed (its "Use current location"
        // button is the first visible element). Previously this crashed because the
        // picker used viewModel() instead of hiltViewModel() — BUG-011 class.
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("Use current location").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        assertOnScreen("Settings")
    }

    @Test
    fun e05_settingsCalendarSyncRowOpensAndCancels() {
        ensureAtDashboard()
        openSettingsViaGear()
        openAndCancelRow("Calendar sync")
        assertOnScreen("Settings")
    }

    @Test
    fun e06_settingsSyncNowButtonRespondsToClick() {
        ensureAtDashboard()
        openSettingsViaGear()
        // "Sync now" lives below the fold in the Settings column. Scroll into view
        // before asserting and clicking.
        composeRule.onNodeWithText("Sync now").performScrollTo()
        composeRule.onNodeWithText("Sync now").assertIsDisplayed()
        composeRule.onNodeWithText("Sync now").performClick()
        composeRule.waitForIdle()
        assertOnScreen("Settings")
    }

    /**
     * BUG-021 regression guard. When Enable is toggled in the Wi-Fi scan sheet the
     * WifiSsidPicker expands inside the sheet body. The Save button must remain
     * visible (not pushed off-screen) regardless of how tall the picker grows.
     */
    @Test
    fun e07_settingsWifiScanSaveReachableWithLongList() {
        ensureAtDashboard()
        openSettingsViaGear()
        composeRule.onNodeWithText("Wi-Fi (scan only)").performClick()
        composeRule.waitForIdle()
        // Flip Enable to expand the WifiSsidPicker inside the sheet body.
        toggleEnableSwitchInSheet()
        // Save must be present and displayed even with the picker composed.
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("Save").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Save").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        assertOnScreen("Settings")
    }
}
