package com.phms.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.phms.app.data.local.database.AppDatabase
import com.phms.app.data.repository.PHMSRepository
import com.phms.app.data.repository.SettingsRepository
import com.phms.app.worker.DailyAlertWorker
import java.util.concurrent.TimeUnit

class PHMSApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: PHMSRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = PHMSRepository(database)
        settingsRepository = SettingsRepository(this)
        scheduleDailyAlertWorker()
    }

    private fun scheduleDailyAlertWorker() {
        val dailyRequest = PeriodicWorkRequestBuilder<DailyAlertWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PHMSDailyAlertWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )
    }
}
