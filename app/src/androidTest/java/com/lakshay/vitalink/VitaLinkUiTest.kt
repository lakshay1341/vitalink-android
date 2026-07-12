package com.lakshay.vitalink

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lakshay.vitalink.data.Alert
import com.lakshay.vitalink.data.Encounter
import com.lakshay.vitalink.data.LatestVital
import com.lakshay.vitalink.data.News2Result
import com.lakshay.vitalink.data.Patient
import com.lakshay.vitalink.ui.AlarmRow
import com.lakshay.vitalink.ui.DashboardContent
import com.lakshay.vitalink.ui.DashboardUiState
import com.lakshay.vitalink.ui.EmptyWard
import com.lakshay.vitalink.ui.LoginContent
import com.lakshay.vitalink.ui.LoginUiState
import com.lakshay.vitalink.ui.MonitorContent
import com.lakshay.vitalink.ui.MonitorUiState
import com.lakshay.vitalink.ui.News2Badge
import com.lakshay.vitalink.ui.PatientCard
import com.lakshay.vitalink.ui.PatientRow
import com.lakshay.vitalink.ui.TechnicalAlarm
import com.lakshay.vitalink.ui.VitalTile
import com.lakshay.vitalink.ui.theme.HrGreen
import com.lakshay.vitalink.ui.theme.VitaLinkTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests (androidTest) using the Compose test rule — the Compose-native
 * equivalent of Espresso, on the same androidx.test stack. They render the render-only
 * *Content composables and components in isolation with fake data (no Hilt, no backend)
 * and assert what a clinician sees. Run on a device/emulator:
 *   ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class VitaLinkUiTest {

    @get:Rule
    val rule = createComposeRule()

    private fun encounter() = Encounter(
        id = 1,
        status = "ADMITTED",
        wardLabel = "A",
        bedLabel = "12",
        admittedAt = null,
        patient = Patient(1, "4471", "Eleanor", "Shaw"),
    )

    private fun news2(total: Int, risk: String) =
        News2Result(total, mapOf("spo2" to 3, "respiratoryRate" to 3), true, risk, true)

    @Test
    fun login_showsFormControls() {
        rule.setContent { VitaLinkTheme { LoginContent(LoginUiState(), {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("VitaLink").assertIsDisplayed()
        rule.onNodeWithText("Server URL").assertIsDisplayed()
        rule.onNodeWithText("Username").assertIsDisplayed()
        rule.onNodeWithText("Password").assertIsDisplayed()
        rule.onNodeWithText("Sign in").assertIsDisplayed()
        rule.onNodeWithText("Sign in").assertIsEnabled()
    }

    @Test
    fun dashboardContent_showsPatientRow() {
        val state = DashboardUiState.Content(
            listOf(PatientRow(encounter(), news2(2, "LOW"), listOf(LatestVital("HEART_RATE", 78.0, "bpm", null)))),
        )
        rule.setContent { VitaLinkTheme { DashboardContent(state, onRefresh = {}, onOpen = {}) } }
        rule.onNodeWithText("Eleanor Shaw").assertIsDisplayed()
        rule.onNodeWithText("1 monitored", substring = true).assertIsDisplayed()
    }

    @Test
    fun emptyWard_showsMessageAndRefreshInvokesCallback() {
        var refreshed = false
        rule.setContent { VitaLinkTheme { EmptyWard(onRefresh = { refreshed = true }) } }
        rule.onNodeWithText("No patients monitored").assertIsDisplayed()
        rule.onNodeWithText("Refresh").performClick()
        assertTrue(refreshed)
    }

    @Test
    fun patientCard_showsPatientAndClickOpens() {
        var opened = false
        rule.setContent {
            VitaLinkTheme {
                PatientCard(
                    encounter(),
                    news2(2, "LOW"),
                    listOf(LatestVital("HEART_RATE", 78.0, "bpm", null)),
                ) { opened = true }
            }
        }
        rule.onNodeWithText("Eleanor Shaw").assertIsDisplayed()
        rule.onNodeWithText("4471", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Eleanor Shaw").performClick()
        assertTrue(opened)
    }

    @Test
    fun news2Badge_showsScoreAndLabel() {
        rule.setContent { VitaLinkTheme { News2Badge(news2(5, "MEDIUM")) } }
        rule.onNodeWithText("5").assertIsDisplayed()
        rule.onNodeWithText("NEWS2").assertIsDisplayed()
    }

    @Test
    fun vitalTile_showsValueLabelUnit() {
        rule.setContent { VitaLinkTheme { VitalTile("HR", 125.0, "bpm", HrGreen, 3, Modifier) } }
        rule.onNodeWithText("125").assertIsDisplayed()
        rule.onNodeWithText("HR").assertIsDisplayed()
        rule.onNodeWithText("bpm").assertIsDisplayed()
    }

    @Test
    fun alarmRow_showsMessage() {
        rule.setContent {
            VitaLinkTheme { AlarmRow(Alert(1, "NEW", "CRITICAL", "SpO2 low", "2026-07-12T10:42:00Z")) }
        }
        rule.onNodeWithText("SpO2 low").assertIsDisplayed()
    }

    @Test
    fun monitorContent_rendersEcgHeaderStreaming() {
        rule.setContent {
            VitaLinkTheme { MonitorContent(encounter(), MonitorUiState(streaming = true), {}, {}, {}) }
        }
        rule.onNodeWithText("ECG II · 250 Hz").assertIsDisplayed()
        rule.onNodeWithText("● Streaming").assertIsDisplayed()
    }

    @Test
    fun monitorContent_showsTechnicalAlarm() {
        val state = MonitorUiState(
            technical = listOf(TechnicalAlarm("ECG signal loss", "No waveform received — check lead / sensor")),
        )
        rule.setContent { VitaLinkTheme { MonitorContent(encounter(), state, {}, {}, {}) } }
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("ECG signal loss"))
        rule.onNodeWithText("ECG signal loss").assertIsDisplayed()
    }
}
