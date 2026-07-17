package com.armutlu.apporganizer.domain.usecase.missions

import com.armutlu.apporganizer.R
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MissionValueFormatter — Dongu M03. Gercek string cozumlemesi Android Context gerektirdiginden
 * (bkz. MissionsViewModel.resolveTextSpec), burada resId + arguman DOGRULUGU test edilir —
 * "90 dk -> 1 sa. 30 dk." gibi bicimler resId+args ciftinin dogru uretildigini kanitlar.
 */
class MissionValueFormatterTest {

    @Test
    fun `90 minutes formats as hours and minutes`() {
        val spec = MissionValueFormatter.durationSpec(90L)
        assertEquals(R.string.mission_duration_hours_minutes, spec.resId)
        assertEquals(listOf(1L, 30L), spec.args)
    }

    @Test
    fun `45 minutes formats as minutes only`() {
        val spec = MissionValueFormatter.durationSpec(45L)
        assertEquals(R.string.mission_duration_minutes_only, spec.resId)
        assertEquals(listOf(45L), spec.args)
    }

    @Test
    fun `120 minutes formats as hours only`() {
        val spec = MissionValueFormatter.durationSpec(120L)
        assertEquals(R.string.mission_duration_hours_only, spec.resId)
        assertEquals(listOf(2L), spec.args)
    }

    @Test
    fun `zero minutes formats as zero minutes only`() {
        val spec = MissionValueFormatter.durationSpec(0L)
        assertEquals(R.string.mission_duration_minutes_only, spec.resId)
        assertEquals(listOf(0L), spec.args)
    }

    @Test
    fun `currentDurationSpec nests durationSpec as single argument`() {
        val spec = MissionValueFormatter.currentDurationSpec(90L)
        assertEquals(R.string.mission_progress_current_duration, spec.resId)
        assertEquals(1, spec.args.size)
        val nested = spec.args[0] as MissionTextSpec
        assertEquals(R.string.mission_duration_hours_minutes, nested.resId)
    }

    @Test
    fun `percentUsedSpec converts fraction to whole percent`() {
        val spec = MissionValueFormatter.percentUsedSpec(0.5f)
        assertEquals(R.string.mission_progress_percent_used, spec.resId)
        assertEquals(listOf(50), spec.args)
    }
}
