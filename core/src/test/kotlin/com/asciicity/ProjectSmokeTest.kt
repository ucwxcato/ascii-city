package com.asciicity

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import kotlin.test.Test
import kotlin.test.assertNotNull

class ProjectSmokeTest {
    @Test
    fun `headless libgdx application can start and stop`() {
        val application = HeadlessApplication(
            AsciiCityGame(loadVisualAssets = false),
            HeadlessApplicationConfiguration(),
        )

        try {
            assertNotNull(application.applicationListener)
        } finally {
            application.exit()
        }
    }
}
