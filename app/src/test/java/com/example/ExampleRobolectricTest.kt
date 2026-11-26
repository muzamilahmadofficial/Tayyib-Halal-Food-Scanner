package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.AppContainer
import com.example.database.CachedProductEntity
import com.example.database.FavouriteEntity
import com.example.database.ScanHistoryEntity
import com.example.database.TayyibDatabase
import com.example.localization.AppLanguage
import com.example.localization.ProvideAppLocale
import com.example.navigation.TayyibAppRoot
import com.example.ui.theme.TayyibTheme
import com.example.ui.viewmodel.TayyibViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `english and french string resources load accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Tayyib", context.getString(R.string.app_name))

        val frConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale.FRENCH)
        }
        val frContext = context.createConfigurationContext(frConfig)
        assertEquals("Accueil", frContext.getString(R.string.nav_home))
        assertEquals("Douteux", frContext.getString(R.string.verdict_doubtful))
        assertEquals("Nous n'avons pas trouvé ce produit.", frContext.getString(R.string.scanner_not_found_title))
    }

    @Test
    fun `room database persists scan history favourites and offline cache`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TayyibDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            db.scanHistoryDao().upsert(
                ScanHistoryEntity(
                    barcode = "3017620422003",
                    productName = "Fixture Spread",
                    brands = "FixtureBrand",
                    imageUrl = null,
                    verdict = "HALAL",
                    evidenceQuality = "HIGH",
                    scannedAtMillis = 1730000000000L
                )
            )
            val history = db.scanHistoryDao().observeAll().first()
            assertEquals(1, history.size)
            assertEquals("3017620422003", history.first().barcode)

            db.favouriteDao().upsert(
                FavouriteEntity(
                    barcode = "3017620422003",
                    productName = "Fixture Spread",
                    brands = "FixtureBrand",
                    imageUrl = null,
                    lastKnownVerdict = "HALAL",
                    lastVerifiedMillis = 1730000000000L
                )
            )
            assertTrue(db.favouriteDao().isFavourite("3017620422003"))

            db.cachedProductDao().upsert(
                CachedProductEntity(
                    barcode = "3017620422003",
                    rawProductJson = """{"code":"3017620422003","product_name":"Fixture Spread"}""",
                    cachedAtMillis = 1730000000000L
                )
            )
            assertNotNull(db.cachedProductDao().getByBarcode("3017620422003"))
            assertEquals(1, db.cachedProductDao().count())

            db.scanHistoryDao().clearAll()
            assertEquals(0, db.scanHistoryDao().observeAll().first().size)
            assertTrue(db.favouriteDao().isFavourite("3017620422003"))

            db.favouriteDao().deleteByBarcode("3017620422003")
            assertFalse(db.favouriteDao().isFavourite("3017620422003"))
        } finally {
            db.close()
        }
    }

    @Test
    fun `ui navigation covers home ingredient checker e-number guide history and settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)
        val viewModel = TayyibViewModel(container)

        composeTestRule.setContent {
            ProvideAppLocale(language = AppLanguage.ENGLISH) {
                TayyibTheme {
                    TayyibAppRoot(viewModel = viewModel, skipSplash = true)
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()

        // Navigate to Offline Ingredient Checker and analyze French E-number input
        composeTestRule.onNodeWithTag("home_screen").performScrollToNode(hasTestTag("home_tool_checker"))
        composeTestRule.onNodeWithTag("home_tool_checker").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ingredient_checker_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("checker_input_field")
            .performTextInput("Sucre, gélatine, E471, E120")
        composeTestRule.onNodeWithTag("ingredient_checker_screen").performScrollToNode(hasTestTag("checker_analyze_button"))
        composeTestRule.onNodeWithTag("checker_analyze_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ingredient_checker_screen").performScrollToNode(hasTestTag("checker_result_card"))
        composeTestRule.onNodeWithTag("checker_result_card").assertIsDisplayed()

        // Return Home
        composeTestRule.onNodeWithTag("checker_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()

        // Navigate to E-Number Guide
        composeTestRule.onNodeWithTag("home_screen").performScrollToNode(hasTestTag("home_tool_enumbers"))
        composeTestRule.onNodeWithTag("home_tool_enumbers").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("enumber_guide_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("enumber_search_input").performTextInput("E471")
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("E471", substring = true)[0].assertIsDisplayed()

        composeTestRule.onNodeWithTag("enumber_back_button").performClick()
        composeTestRule.waitForIdle()

        // Bottom Navigation -> Alternatives -> History -> Settings
        composeTestRule.onNodeWithTag("nav_tab_alternatives").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("alternatives_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_history").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("history_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    }
}
