package dhanfinix.android.sukun

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppStartupTest {

    // This rule launches MainActivity before each test
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesWithoutCrashing() {
        // If the app crashes on launch (e.g. Gson TypeToken parsing fails due to Proguard),
        // this test will fail because the Activity will crash before we can assert anything.
        
        // Wait for Compose to become idle
        composeTestRule.waitForIdle()

        // Just checking that we've reached a state without a runtime exception.
        // We could assert on a specific text node being displayed here if we want:
        // composeTestRule.onNodeWithText("Sukun").assertExists()
    }
}
