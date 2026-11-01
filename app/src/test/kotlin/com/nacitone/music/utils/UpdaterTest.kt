package com.nacitone.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test
    fun comparesVersions() {
        assertEquals(1, Updater.compareVersions("v1.2.0", "1.1.9"))
        assertEquals(-1, Updater.compareVersions("1.0", "1.0.1"))
        assertEquals(0, Updater.compareVersions("v2.0.0", "2.0"))
    }

    @Test
    fun detectsAvailableUpdate() {
        assertTrue(Updater.isUpdateAvailable("1.0.0", "1.0.1"))
        assertTrue(!Updater.isUpdateAvailable("1.0.1", "1.0.1"))
    }
}
