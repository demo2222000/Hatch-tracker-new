package com.example

import android.app.Application
import com.example.data.db.HatchDatabase
import com.example.data.repository.IncubatorRepository
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import com.example.pdf.PdfReportGenerator
import com.example.sensor.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HatchMasterApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { HatchDatabase.getInstance(this) }
    val notificationHelper by lazy { NotificationHelper(this) }
    val alarmScheduler by lazy { AlarmScheduler(this) }
    val pdfGenerator by lazy { PdfReportGenerator(this) }

    val sensorManager by lazy {
        SensorManager(
            context = this,
            sensorDao = database.sensorDao(),
            batchDao = database.batchDao(),
            alertDao = database.alertDao(),
            notificationHelper = notificationHelper,
            scope = applicationScope
        )
    }

    val repository by lazy {
        IncubatorRepository(
            context = this,
            batchDao = database.batchDao(),
            sensorDao = database.sensorDao(),
            turningDao = database.turningDao(),
            alertDao = database.alertDao(),
            alarmScheduler = alarmScheduler,
            notificationHelper = notificationHelper,
            pdfGenerator = pdfGenerator
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch(Dispatchers.IO) {
            repository.seedInitialDataIfEmpty()
        }
    }
}
