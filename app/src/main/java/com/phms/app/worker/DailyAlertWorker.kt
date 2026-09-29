package com.phms.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phms.app.data.local.database.AppDatabase
import com.phms.app.data.local.entity.AlertEntity
import com.phms.app.data.repository.SettingsRepository
import com.phms.app.domain.engine.OutbreakDetector
import com.phms.app.domain.engine.PromotionEngine
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DailyAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val settings = SettingsRepository(applicationContext).get()
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // 1. Run Promotion Engine checks (honors alertPromotion preference)
        if (settings.alertPromotion) {
            val promotionEngine = PromotionEngine(db.pigDao(), db.growthStageDao(), db.alertDao())
            promotionEngine.checkAndGeneratePromotionAlerts(now)
        }

        // 2. Low Feed Stock Check (honors alertLowFeed preference + deduplication)
        if (settings.alertLowFeed) {
            val ingredients = db.feedDao().getAllIngredientsSync()
            for (ing in ingredients) {
                if (ing.stock_kg <= ing.reorder_level) {
                    val alertType = "Low Feed"
                    val msg = "${ing.name} stock is LOW (${ing.stock_kg} kg remaining, reorder level: ${ing.reorder_level} kg)."
                    val existing = db.alertDao().getActiveAlertByKeyword(alertType, ing.name)
                    if (existing == null) {
                        db.alertDao().insertAlert(
                            AlertEntity(
                                type = alertType,
                                priority = "High",
                                message = msg,
                                created_date = now
                            )
                        )
                    }
                }
            }
        }

        // 3. Outbreak Detection (critical biosecurity check + deduplication)
        val outbreakPenIds = OutbreakDetector.detectOutbreaks(db.healthDao(), now)
        for (penId in outbreakPenIds) {
            val pen = db.penDao().getPenById(penId)
            val penName = pen?.name ?: "Pen #$penId"
            val msg = "DISEASE OUTBREAK DETECTED: 2 or more pigs in $penName show symptoms within 48 hours!"
            val existing = db.alertDao().getActiveAlertByKeyword("Mortality Spike", penName)
            if (existing == null) {
                db.alertDao().insertAlert(
                    AlertEntity(
                        type = "Mortality Spike",
                        priority = "Critical",
                        message = msg,
                        created_date = now
                    )
                )
            }
        }

        // 4. Drug Withdrawal Check (honors alertWithdrawal preference + deduplication)
        if (settings.alertWithdrawal) {
            val withdrawalEvents = db.healthDao().getEventsWithWithdrawal()
            val activePigs = db.pigDao().getActivePigsSync()
            for (event in withdrawalEvents) {
                val withdrawalEndMs = event.date + TimeUnit.DAYS.toMillis(event.withdrawal_days.toLong())
                if (now < withdrawalEndMs) {
                    val pig = activePigs.find { it.id == event.pig_id }
                    val targetName = if (pig != null) "Pig #${pig.tag_number}" else if (event.target_category != null) "Category ${event.target_category}" else "Herd"
                    val endDateStr = dateFormat.format(Date(withdrawalEndMs))
                    val msg = "WITHDRAWAL ACTIVE: $targetName is under ${event.product} withdrawal until $endDateStr. DO NOT SELL OR SLAUGHTER!"

                    val existing = if (event.pig_id != null) {
                        db.alertDao().getActiveAlertForPig("Withdrawal Active", event.pig_id)
                    } else {
                        db.alertDao().getActiveAlertByTypeAndMessage("Withdrawal Active", msg)
                    }
                    if (existing == null) {
                        db.alertDao().insertAlert(
                            AlertEntity(
                                type = "Withdrawal Active",
                                priority = "Critical",
                                related_pig_id = event.pig_id,
                                message = msg,
                                created_date = now
                            )
                        )
                    }
                }
            }
        }

        // 5. Farrowing Alerts (honors alertFarrowing preference + deduplication)
        if (settings.alertFarrowing) {
            val activePregnancies = db.breedingDao().getActivePregnanciesSync()
            val activePigs = db.pigDao().getActivePigsSync()
            for (preg in activePregnancies) {
                val daysRemaining = TimeUnit.MILLISECONDS.toDays(preg.expected_farrowing_date - now).coerceAtLeast(0)
                if (daysRemaining <= 5) {
                    val sow = activePigs.find { it.id == preg.sow_id }
                    val sowTag = sow?.tag_number ?: "${preg.sow_id}"
                    val dueDateStr = dateFormat.format(Date(preg.expected_farrowing_date))
                    val msg = if (daysRemaining == 0L) {
                        "FARROWING DUE TODAY: Sow #$sowTag is expected to farrow today! Prepare farrowing pen and heating lamps."
                    } else {
                        "UPCOMING FARROWING: Sow #$sowTag is due in $daysRemaining days (Expected: $dueDateStr)."
                    }
                    val existing = db.alertDao().getActiveAlertForPig("Farrowing", preg.sow_id)
                    if (existing == null) {
                        db.alertDao().insertAlert(
                            AlertEntity(
                                type = "Farrowing",
                                priority = if (daysRemaining <= 1) "Critical" else "High",
                                related_pig_id = preg.sow_id,
                                message = msg,
                                created_date = now
                            )
                        )
                    }
                }
            }
        }

        // 6. Overcrowding Check (honors alertOvercrowding preference + deduplication)
        if (settings.alertOvercrowding) {
            val pens = db.penDao().getAllPensSync()
            val activePigs = db.pigDao().getActivePigsSync()
            for (pen in pens) {
                val count = activePigs.count { it.pen_id == pen.id }
                if (pen.capacity > 0 && count > pen.capacity) {
                    val msg = "OVERCROWDING: ${pen.name} holds $count pigs (Capacity: ${pen.capacity}). Move excess pigs to reduce stress and disease risk."
                    val existing = db.alertDao().getActiveAlertByKeyword("Overcrowding", pen.name)
                    if (existing == null) {
                        db.alertDao().insertAlert(
                            AlertEntity(
                                type = "Overcrowding",
                                priority = "High",
                                message = msg,
                                created_date = now
                            )
                        )
                    }
                }
            }
        }

        return Result.success()
    }
}
