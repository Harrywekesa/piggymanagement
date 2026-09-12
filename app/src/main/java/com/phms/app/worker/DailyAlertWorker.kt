package com.phms.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phms.app.data.local.database.AppDatabase
import com.phms.app.data.local.entity.AlertEntity
import com.phms.app.domain.engine.OutbreakDetector
import com.phms.app.domain.engine.PromotionEngine

class DailyAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val now = System.currentTimeMillis()

        // 1. Run Promotion Engine checks
        val promotionEngine = PromotionEngine(db.pigDao(), db.growthStageDao(), db.alertDao())
        promotionEngine.checkAndGeneratePromotionAlerts(now)

        // 2. Low Feed Stock Check
        val ingredients = db.feedDao().getAllIngredientsSync()
        for (ing in ingredients) {
            if (ing.stock_kg <= ing.reorder_level) {
                db.alertDao().insertAlert(
                    AlertEntity(
                        type = "Low Feed Stock",
                        priority = "High",
                        message = "${ing.name} stock is LOW (${ing.stock_kg} kg remaining, reorder level: ${ing.reorder_level} kg).",
                        created_date = now
                    )
                )
            }
        }

        // 3. Outbreak Detection
        val outbreakPenIds = OutbreakDetector.detectOutbreaks(db.healthDao(), now)
        for (penId in outbreakPenIds) {
            val pen = db.penDao().getPenById(penId)
            val penName = pen?.name ?: "Pen #$penId"
            db.alertDao().insertAlert(
                AlertEntity(
                    type = "Mortality Spike",
                    priority = "Critical",
                    message = "DISEASE OUTBREAK DETECTED: 2 or more pigs in $penName show symptoms within 48 hours!",
                    created_date = now
                )
            )
        }

        return Result.success()
    }
}
