package com.dnavarro.poskmp.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupAndCatalogJourney {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startupAndCatalogScroll() = baselineProfileRule.collect(
        packageName = "com.dnavarro.poskmp",
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()

        val catalog = device.wait(Until.findObject(By.res("catalog_grid")), 5_000)
        if (catalog != null) {
            catalog.setGestureMargin(device.displayWidth / 5)
            catalog.fling(Direction.DOWN)
            device.waitForIdle()
            catalog.fling(Direction.UP)
            device.waitForIdle()
        }
    }
}
