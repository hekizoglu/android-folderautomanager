package com.armutlu.apporganizer.telemetry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryEventValidatorTest {
    @Test fun `yasakli anahtar reddedilir`() {
        assertFalse(TelemetryEventValidator.isValidPayload("search_performed", mapOf("query" to "ali")))
    }

    @Test fun `serbest metin ve bilinmeyen event reddedilir`() {
        assertFalse(TelemetryEventValidator.isValidPayload("app_launched", mapOf("source" to "kullanici metni")))
        assertFalse(TelemetryEventValidator.isValidPayload("custom_event", emptyMap()))
    }

    @Test fun `bilinen enum degeri kabul edilir`() {
        assertTrue(TelemetryEventValidator.isValid(TelemetryEvent.AppLaunched(TelemetryEvent.Source.HOME)))
    }

    @Test fun `cok uzun deger kirpilmadan reddedilir`() {
        assertFalse(TelemetryEventValidator.isValidPayload("app_launched", mapOf("source" to "x".repeat(41))))
    }

    @Test fun `sayilar guvenli kovalara donusur`() {
        assertTrue(TelemetryEventValidator.isValid(TelemetryEvent.SearchPerformed(
            QueryLengthBucket.NINE_PLUS, CountBucket.ONE_HUNDRED_ONE_PLUS
        )))
        assertTrue(CountBucket.from(500) == CountBucket.ONE_HUNDRED_ONE_PLUS)
    }
}
