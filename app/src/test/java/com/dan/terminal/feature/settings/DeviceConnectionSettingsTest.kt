package com.dan.terminal.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceConnectionSettingsTest {
    @Test fun fullBattery() {
        assertEquals(100, batteryPercent(60, 60))
        assertEquals("100%", batteryDisplay(batteryPercent(60, 60)))
    }

    @Test fun halfBattery() {
        assertEquals(50, batteryPercent(25, 50))
    }

    @Test fun unavailableOrInvalidBattery() {
        assertNull(batteryPercent(-1, 100))
        assertNull(batteryPercent(40, 0))
        assertNull(batteryPercent(110, 100))
        assertEquals("Not reported", batteryDisplay(null))
    }
}
