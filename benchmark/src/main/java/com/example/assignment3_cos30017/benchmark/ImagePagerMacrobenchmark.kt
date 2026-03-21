package com.example.assignment3_cos30017.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.MemoryCountersMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class ImagePagerMacrobenchmark {

    companion object {
        private const val PACKAGE = "com.example.assignment3_cos30017"

        // Uses the seeded Firebase Auth accounts documented in `SEED_ACCOUNTS.md`.
        private const val TEST_EMAIL = "buimanhthang286@gmail.com"
        private const val TEST_PASSWORD = "Bmt130920055@@"

        private const val TIMEOUT_MS = 15_000L
        private const val SWIPE_COUNT = 10
    }

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Measures "cold-ish" startup cost until the first fully rendered Home screen.
     * Useful for context, but the main focus of this investigation is swipe jank.
     */
    @Test
    fun startup_to_home() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE,
            metrics = listOf(
                StartupTimingMetric(),
                MemoryUsageMetric(mode = MemoryUsageMetric.Mode.Max),
                MemoryCountersMetric()
            ),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.COLD,
            iterations = 5,
            setupBlock = {
                pressHome()
            }
        ) {
            startActivityAndWait()
            device.loginIfNeeded()
            device.waitForHomeLoaded()
        }
    }

    /**
     * Measures frame timing (jank) while swiping the image ViewPager in Detail screen.
     * Run this for both flavors:
     * - preloadBenchmark
     * - lazyBenchmark
     */
    @Test
    fun detail_image_pager_swipe_jank() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE,
            metrics = listOf(
                FrameTimingMetric(),
                MemoryUsageMetric(mode = MemoryUsageMetric.Mode.Max),
                MemoryCountersMetric()
            ),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = 10,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                device.loginIfNeeded()
                device.openFirstCarDetail()
                device.waitForDetailLoaded()
            }
        ) {
            device.swipeImagePager(times = SWIPE_COUNT)
        }
    }

    private fun UiDevice.loginIfNeeded() {
        // If already on Home (rv_cars exists), skip.
        if (hasObject(By.res(PACKAGE, "rv_cars"))) return

        // If login fields exist, perform login.
        val emailField = wait(Until.findObject(By.res(PACKAGE, "et_email")), TIMEOUT_MS)
        val passwordField = wait(Until.findObject(By.res(PACKAGE, "et_password")), TIMEOUT_MS)
        if (emailField != null && passwordField != null) {
            emailField.text = TEST_EMAIL
            passwordField.text = TEST_PASSWORD
            findObject(By.res(PACKAGE, "btn_login"))?.click()
        }

        waitForHomeLoaded()
    }

    private fun UiDevice.waitForHomeLoaded() {
        wait(Until.hasObject(By.res(PACKAGE, "rv_cars")), TIMEOUT_MS)
        wait(Until.hasObject(By.res(PACKAGE, "tv_car_name")), TIMEOUT_MS)
    }

    private fun UiDevice.openFirstCarDetail() {
        waitForHomeLoaded()
        val firstTitle = wait(Until.findObject(By.res(PACKAGE, "tv_car_name")), TIMEOUT_MS)
        firstTitle?.click()
    }

    private fun UiDevice.waitForDetailLoaded() {
        wait(Until.hasObject(By.res(PACKAGE, "vp_images")), TIMEOUT_MS)
        // Also wait for dots container; helps ensure initial layout pass completed.
        wait(Until.hasObject(By.res(PACKAGE, "layout_dots")), TIMEOUT_MS)
    }

    private fun UiDevice.swipeImagePager(times: Int) {
        val pager = findObject(By.res(PACKAGE, "vp_images")) ?: return
        repeat(times) { i ->
            // Alternate direction to avoid hitting the end forever.
            if (i % 2 == 0) {
                pager.swipe(Direction.LEFT, 0.9f)
            } else {
                pager.swipe(Direction.RIGHT, 0.9f)
            }
            // Let the UI settle a bit between swipes.
            waitForIdle()
        }
    }
}

