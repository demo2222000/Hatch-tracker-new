package com.example.data.repository

import android.content.Context
import com.example.data.dao.AlertDao
import com.example.data.dao.BatchDao
import com.example.data.dao.SensorDao
import com.example.data.dao.TurningDao
import com.example.data.model.AlertLog
import com.example.data.model.IncubationBatch
import com.example.data.model.SensorReading
import com.example.data.model.SpeciesPreset
import com.example.data.model.TurningLog
import com.example.data.model.TurningSchedule
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import com.example.pdf.PdfReportGenerator
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class IncubatorRepository(
    private val context: Context,
    private val batchDao: BatchDao,
    private val sensorDao: SensorDao,
    private val turningDao: TurningDao,
    private val alertDao: AlertDao,
    private val alarmScheduler: AlarmScheduler,
    private val notificationHelper: NotificationHelper,
    private val pdfGenerator: PdfReportGenerator
) {
    val allBatches: Flow<List<IncubationBatch>> = batchDao.getAllBatches()
    val activeBatchFlow: Flow<IncubationBatch?> = batchDao.getActiveBatchFlow()
    val allAlerts: Flow<List<AlertLog>> = alertDao.getAllAlertsFlow()
    val unacknowledgedAlertCount: Flow<Int> = alertDao.getUnacknowledgedCountFlow()
    val recentSensorReadings: Flow<List<SensorReading>> = sensorDao.getRecentReadingsFlow(150)

    suspend fun getActiveBatch(): IncubationBatch? = batchDao.getActiveBatch()

    fun getBatchByIdFlow(id: Long): Flow<IncubationBatch?> = batchDao.getBatchByIdFlow(id)

    suspend fun getBatchById(id: Long): IncubationBatch? = batchDao.getBatchById(id)

    fun getTurningScheduleFlow(batchId: Long): Flow<TurningSchedule?> =
        turningDao.getScheduleFlow(batchId)

    fun getTurningLogsFlow(batchId: Long): Flow<List<TurningLog>> =
        turningDao.getLogsForBatchFlow(batchId)

    fun getReadingsSinceFlow(sinceTimestamp: Long): Flow<List<SensorReading>> =
        sensorDao.getReadingsSinceFlow(sinceTimestamp)

    suspend fun createBatch(batch: IncubationBatch, intervalHours: Int = 2): Long {
        val newId = batchDao.insertBatch(batch)
        if (batch.isActive) {
            batchDao.deactivateOtherBatches(newId)
        }

        // Initialize default turning schedule
        val preset = SpeciesPreset.getById(batch.species)
        val initialSchedule = TurningSchedule(
            batchId = newId,
            enabled = preset.requiresTurning,
            intervalHours = intervalHours,
            is24Hours = true,
            startHour = 6,
            endHour = 22,
            soundEnabled = true,
            vibrateEnabled = true,
            nextTurnTimestamp = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(intervalHours.toLong())
        )
        turningDao.insertOrUpdateSchedule(initialSchedule)

        if (batch.isActive && preset.requiresTurning) {
            alarmScheduler.scheduleNextTurn(newId, initialSchedule.nextTurnTimestamp)
        }

        return newId
    }

    suspend fun updateBatch(batch: IncubationBatch) {
        batchDao.updateBatch(batch)
    }

    suspend fun setActiveBatch(id: Long) {
        val batch = batchDao.getBatchById(id) ?: return
        batchDao.deactivateOtherBatches(id)
        batchDao.updateBatch(batch.copy(isActive = true))

        val schedule = turningDao.getSchedule(id)
        if (schedule != null && schedule.enabled && !batch.isInLockdown()) {
            val nextTurn = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(schedule.intervalHours.toLong())
            turningDao.insertOrUpdateSchedule(schedule.copy(nextTurnTimestamp = nextTurn))
            alarmScheduler.scheduleNextTurn(id, nextTurn)
        }
    }

    suspend fun deleteBatch(id: Long) {
        batchDao.deleteBatch(id)
        alarmScheduler.cancelTurnAlarm()
    }

    suspend fun updateTurningSchedule(schedule: TurningSchedule) {
        turningDao.insertOrUpdateSchedule(schedule)
        if (schedule.enabled) {
            val triggerTime = if (schedule.nextTurnTimestamp > System.currentTimeMillis()) {
                schedule.nextTurnTimestamp
            } else {
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(schedule.intervalHours.toLong())
            }
            turningDao.insertOrUpdateSchedule(schedule.copy(nextTurnTimestamp = triggerTime))
            alarmScheduler.scheduleNextTurn(schedule.batchId, triggerTime)
        } else {
            alarmScheduler.cancelTurnAlarm()
            notificationHelper.cancelTurningNotification()
        }
    }

    suspend fun recordTurn(
        batchId: Long,
        orientation: String,
        action: String = "COMPLETED",
        notes: String = ""
    ) {
        turningDao.insertLog(
            TurningLog(
                batchId = batchId,
                action = action,
                orientation = orientation,
                notes = notes
            )
        )

        notificationHelper.cancelTurningNotification()

        val schedule = turningDao.getSchedule(batchId)
        if (schedule != null && schedule.enabled) {
            val nextTurn = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(schedule.intervalHours.toLong())
            turningDao.insertOrUpdateSchedule(
                schedule.copy(
                    lastTurnTimestamp = System.currentTimeMillis(),
                    nextTurnTimestamp = nextTurn,
                    totalTurnsCompleted = schedule.totalTurnsCompleted + 1
                )
            )
            alarmScheduler.scheduleNextTurn(batchId, nextTurn)
        }
    }

    suspend fun acknowledgeAlert(id: Long) = alertDao.acknowledgeAlert(id)
    suspend fun acknowledgeAllAlerts() = alertDao.acknowledgeAllAlerts()
    suspend fun clearAllAlerts() = alertDao.clearAllAlerts()

    suspend fun generatePdfReport(batchId: Long): android.content.Intent? {
        val batch = batchDao.getBatchById(batchId) ?: return null
        val readings = sensorDao.getReadingsForBatch(batchId)
        val turnings = turningDao.getLogsForBatch(batchId)
        val alerts = alertDao.getAlertsForBatch(batchId)
        return pdfGenerator.generateAndShareBatchReport(batch, readings, turnings, alerts)
    }

    suspend fun seedInitialDataIfEmpty() {
        val count = batchDao.getActiveBatch()
        if (count == null) {
            val now = System.currentTimeMillis()
            // Seed a sample chicken batch on Day 9 of 21
            val sampleStartDate = now - TimeUnit.DAYS.toMillis(9)
            val sampleBatch = IncubationBatch(
                name = "Spring Heritage Flock #1",
                species = "Chicken",
                startDate = sampleStartDate,
                totalEggs = 24,
                incubationDays = 21,
                lockdownDay = 18,
                targetTempC = 37.5,
                minTempC = 37.2,
                maxTempC = 38.3,
                targetHumidityPct = 50.0,
                minHumidityPct = 45.0,
                maxHumidityPct = 55.0,
                lockdownHumidityPct = 65.0,
                notes = "Barnyard mix from pasture flock. Eggs were gathered over 5 days and stored at 15°C prior to setting.",
                isActive = true,
                hatchedEggs = 0,
                infertileEggs = 2,
                earlyQuitEggs = 1,
                status = "INCUBATING"
            )
            val batchId = createBatch(sampleBatch, intervalHours = 2)

            // Seed historical readings
            for (i in 20 downTo 0) {
                val time = now - (i * 15 * 60 * 1000L) // every 15 min
                val temp = 37.5 + (Math.sin(i.toDouble()) * 0.18)
                val hum = 50.0 + (Math.cos(i.toDouble()) * 1.5)
                sensorDao.insertReading(
                    SensorReading(
                        batchId = batchId,
                        timestamp = time,
                        temperatureC = ((temp * 10).toInt()) / 10.0,
                        humidityPct = ((hum * 10).toInt()) / 10.0,
                        isSimulated = true,
                        sensorSource = "DEMO",
                        deviceName = "Demo Precision Sensor"
                    )
                )
            }

            // Seed initial turning logs
            turningDao.insertLog(
                TurningLog(
                    batchId = batchId,
                    timestamp = now - TimeUnit.HOURS.toMillis(4),
                    action = "COMPLETED",
                    orientation = "LEFT",
                    notes = "Morning rotation"
                )
            )
            turningDao.insertLog(
                TurningLog(
                    batchId = batchId,
                    timestamp = now - TimeUnit.HOURS.toMillis(2),
                    action = "COMPLETED",
                    orientation = "RIGHT",
                    notes = "Scheduled turn"
                )
            )

            // Seed one informational alert
            alertDao.insertAlert(
                AlertLog(
                    batchId = batchId,
                    timestamp = now - TimeUnit.DAYS.toMillis(2),
                    alertType = "MILESTONE_CANDLING",
                    title = "Day 7 Candling Window Open",
                    message = "First candling check suggested: Look for spiderweb blood vessels and embryo eye spot.",
                    isAcknowledged = true
                )
            )
        }
    }
}
