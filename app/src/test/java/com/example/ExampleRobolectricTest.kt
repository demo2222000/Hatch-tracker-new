package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.IncubationBatch
import com.example.data.model.SpeciesPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HatchMaster", appName)
    }

    @Test
    fun `test hatching percentage calculation`() {
        val batch = IncubationBatch(
            name = "Test Batch",
            species = "Chicken",
            startDate = System.currentTimeMillis(),
            totalEggs = 20,
            hatchedEggs = 17
        )
        // 17 / 20 * 100 = 85.0%
        assertEquals(85.0, batch.hatchingPercentage, 0.01)
    }

    @Test
    fun `test chicken species preset parameters`() {
        val chicken = SpeciesPreset.getById("chicken")
        assertEquals(21, chicken.incubationDays)
        assertEquals(18, chicken.lockdownDay)
        assertTrue(chicken.requiresTurning)
        assertEquals(37.5, chicken.targetTempC, 0.01)
    }
}
