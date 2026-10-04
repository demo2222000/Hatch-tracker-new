package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

@Entity(tableName = "incubation_batches")
data class IncubationBatch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val species: String, // Species ID or name (e.g. "Chicken", "Duck")
    val startDate: Long, // Epoch timestamp in ms
    val totalEggs: Int,
    val incubationDays: Int = 21,
    val lockdownDay: Int = 18,
    val targetTempC: Double = 37.5,
    val minTempC: Double = 37.0,
    val maxTempC: Double = 38.3,
    val targetHumidityPct: Double = 50.0,
    val minHumidityPct: Double = 45.0,
    val maxHumidityPct: Double = 55.0,
    val lockdownHumidityPct: Double = 65.0,
    val notes: String = "",
    val isActive: Boolean = true,
    
    // Hatch and Candling Outcomes
    val hatchedEggs: Int = 0,
    val infertileEggs: Int = 0,
    val earlyQuitEggs: Int = 0,
    val lateQuitEggs: Int = 0,
    val pipsCount: Int = 0,
    val rottedEggsRemoved: Int = 0,
    val lastEggRottedTimestamp: Long? = null,
    val eggRottedNotes: String = "",
    
    val status: String = "INCUBATING", // "INCUBATING", "LOCKDOWN", "HATCHED", "COMPLETED"
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Current incubation day (1-indexed). Day 1 is the 24 hours following start date.
     */
    fun calculateCurrentDay(now: Long = System.currentTimeMillis()): Int {
        if (now < startDate) return 0
        val diffMs = now - startDate
        val days = TimeUnit.MILLISECONDS.toDays(diffMs).toInt() + 1
        return days.coerceAtLeast(1)
    }

    /**
     * Days remaining until expected hatch date.
     */
    fun calculateRemainingDays(now: Long = System.currentTimeMillis()): Int {
        val currentDay = calculateCurrentDay(now)
        val remaining = incubationDays - currentDay
        return remaining.coerceAtLeast(0)
    }

    /**
     * Epoch timestamp of expected hatch date.
     */
    val expectedHatchDate: Long
        get() = startDate + TimeUnit.DAYS.toMillis(incubationDays.toLong())

    /**
     * Epoch timestamp of lockdown date.
     */
    val lockdownDate: Long
        get() = startDate + TimeUnit.DAYS.toMillis(lockdownDay.toLong())

    /**
     * Whether the batch is currently in lockdown period.
     */
    fun isInLockdown(now: Long = System.currentTimeMillis()): Boolean {
        val currentDay = calculateCurrentDay(now)
        return currentDay >= lockdownDay && currentDay <= incubationDays + 2
    }

    /**
     * Hatching percentage = (hatched eggs / initial eggs) * 100
     */
    val hatchingPercentage: Double
        get() = if (totalEggs > 0) {
            (hatchedEggs.toDouble() / totalEggs.toDouble()) * 100.0
        } else {
            0.0
        }

    /**
     * Viable fertile eggs count = initial - infertile - quits - rotted
     */
    val estimatedViableEggs: Int
        get() = (totalEggs - infertileEggs - earlyQuitEggs - lateQuitEggs - rottedEggsRemoved).coerceAtLeast(0)

    /**
     * Unhatched eggs count
     */
    val unhatchedEggs: Int
        get() = (totalEggs - hatchedEggs).coerceAtLeast(0)
}
