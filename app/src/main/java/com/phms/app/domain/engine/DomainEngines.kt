package com.phms.app.domain.engine

import com.phms.app.data.local.dao.*
import com.phms.app.data.local.entity.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WithdrawalTracker {
    /**
     * Checks if a pig is currently under active drug withdrawal.
     * Hard blocks sales if active!
     */
    suspend fun isWithdrawalActive(pigId: Long, healthDao: HealthDao, currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        val events = healthDao.getEventsForPigSync(pigId)
        for (event in events) {
            if (event.withdrawal_days > 0) {
                val withdrawalEndMs = event.date + TimeUnit.DAYS.toMillis(event.withdrawal_days.toLong())
                if (currentTimeMs < withdrawalEndMs) {
                    return true
                }
            }
        }
        return false
    }

    suspend fun getWithdrawalEndDateMs(pigId: Long, healthDao: HealthDao): Long? {
        val events = healthDao.getEventsForPigSync(pigId)
        var maxEndMs: Long? = null
        val now = System.currentTimeMillis()
        for (event in events) {
            if (event.withdrawal_days > 0) {
                val endMs = event.date + TimeUnit.DAYS.toMillis(event.withdrawal_days.toLong())
                if (endMs > now) {
                    if (maxEndMs == null || endMs > maxEndMs) {
                        maxEndMs = endMs
                    }
                }
            }
        }
        return maxEndMs
    }
}

object OutbreakDetector {
    /**
     * Outbreak detection rule:
     * Flag critical alert if >= 2 pigs in the same pen show symptoms within a 48-hour window.
     */
    suspend fun detectOutbreaks(healthDao: HealthDao, currentTimeMs: Long = System.currentTimeMillis()): List<Long> {
        val windowStartMs = currentTimeMs - TimeUnit.HOURS.toMillis(48)
        val recentSymptoms = healthDao.getSymptomsSince(windowStartMs)

        // Group by pen_id
        val penGroupMap = recentSymptoms.groupBy { it.pen_id }
        val outbreakPenIds = mutableListOf<Long>()

        for ((penId, symptomsInPen) in penGroupMap) {
            val distinctPigCount = symptomsInPen.map { it.pig_id }.distinct().size
            if (distinctPigCount >= 2) {
                outbreakPenIds.add(penId)
            }
        }
        return outbreakPenIds
    }
}

class PromotionEngine(
    private val pigDao: PigDao,
    private val growthStageDao: GrowthStageDao,
    private val alertDao: AlertDao
) {
    suspend fun checkAndGeneratePromotionAlerts(currentTimeMs: Long = System.currentTimeMillis()) {
        val activePigs = pigDao.getActivePigsSync()
        val stages = growthStageDao.getAllStagesSync().sortedBy { it.display_order }

        for (pig in activePigs) {
            val currentStage = stages.find { it.id == pig.current_stage_id } ?: continue
            val nextStage = stages.find { it.display_order == currentStage.display_order + 1 } ?: continue

            val weights = pigDao.getWeightsForPigSync(pig.id)
            val latestWeight = weights.firstOrNull()?.weight_kg ?: 0.0

            val ageMs = currentTimeMs - pig.birth_date
            val ageWeeks = (ageMs / (1000L * 60 * 60 * 24 * 7)).toInt()

            val ageReady = ageWeeks >= nextStage.min_age_weeks
            val weightReady = latestWeight >= nextStage.min_weight_kg

            if (ageReady || weightReady) {
                // Generate promotion alert
                alertDao.insertAlert(
                    AlertEntity(
                        type = "Promotion Ready",
                        priority = "Medium",
                        related_pig_id = pig.id,
                        message = "Pig Tag #${pig.tag_number} is ready for promotion from ${currentStage.name} to ${nextStage.name} (Weight: ${latestWeight}kg, Age: ${ageWeeks}w).",
                        created_date = currentTimeMs
                    )
                )
            }
        }
    }

    suspend fun promotePig(pigId: Long, newStageId: Long, newPenId: Long?, user: String = "Manager", notes: String? = null): Boolean {
        val pig = pigDao.getPigById(pigId) ?: return false
        val oldStageId = pig.current_stage_id
        val weights = pigDao.getWeightsForPigSync(pigId)
        val latestWeight = weights.firstOrNull()?.weight_kg ?: 0.0

        val now = System.currentTimeMillis()
        val ageWeeks = ((now - pig.birth_date) / (1000L * 60 * 60 * 24 * 7)).toInt()

        // 1. Update Pig
        pigDao.promotePig(pigId, newStageId, newPenId)

        // 2. Log History
        growthStageDao.insertStageHistory(
            PigStageHistoryEntity(
                pig_id = pigId,
                batch_id = pig.batch_id,
                old_stage_id = oldStageId,
                new_stage_id = newStageId,
                promotion_date = now,
                weight_at_promotion = latestWeight,
                age_at_promotion_weeks = ageWeeks,
                old_pen_id = pig.pen_id,
                new_pen_id = newPenId,
                promoted_by = user,
                notes = notes
            )
        )

        // 3. Log Pen Occupancy if pen changed
        if (newPenId != null && newPenId != pig.pen_id) {
            growthStageDao.insertOccupancy(
                PenOccupancyHistoryEntity(
                    pen_id = newPenId,
                    pig_id = pigId,
                    start_date = now,
                    reason = "promotion"
                )
            )
        }
        return true
    }

    suspend fun undoPromotion(historyId: Long): Boolean {
        val history = growthStageDao.getStageHistoryById(historyId) ?: return false
        val now = System.currentTimeMillis()
        // Enforce 24-hour reversal window constraint
        val isWithin24Hours = (now - history.promotion_date) <= TimeUnit.HOURS.toMillis(24)
        if (!isWithin24Hours || history.is_reversed) return false

        // Revert pig stage and pen
        pigDao.promotePig(history.pig_id, history.old_stage_id, history.old_pen_id)

        // Mark history as reversed
        growthStageDao.updateStageHistory(
            history.copy(is_reversed = true, reversal_date = now)
        )
        return true
    }

    suspend fun autoEvaluateWeightStage(pigId: Long, weightKg: Double): Long? {
        val pig = pigDao.getPigById(pigId) ?: return null
        val targetStageId = when {
            weightKg < 15.0 -> 1L // Piglet
            weightKg < 30.0 -> 2L // Weaner
            weightKg < 60.0 -> 3L // Grower
            else -> 4L           // Finisher (Market Ready)
        }

        if (pig.current_stage_id < targetStageId) {
            promotePig(pigId, targetStageId, null, user = "System (Weight Auto)", notes = "Auto-promoted upon reaching ${weightKg}kg")
            if (targetStageId == 4L) {
                alertDao.insertAlert(
                    AlertEntity(
                        type = "Market Ready",
                        priority = "High",
                        related_pig_id = pigId,
                        message = "Pig #${pig.tag_number} reached ${weightKg}kg (Finisher stage) and is ready for market!",
                        created_date = System.currentTimeMillis()
                    )
                )
            }
            return targetStageId
        }
        return pig.current_stage_id
    }
}

object FeedCalculator {
    data class FeedRequirement(
        val dailyKg: Double,
        val feedType: String,
        val description: String
    )

    fun getDailyRequirement(weightKg: Double, stageId: Long, sex: String = "M"): FeedRequirement {
        return when {
            sex.equals("F_Breeding", true) || sex.equals("Sow", true) -> {
                FeedRequirement(3.5, "Lactating & Sow Feed", "High-energy sow ration split into 2 daily feeds")
            }
            weightKg < 15.0 || stageId == 1L -> {
                val kg = (0.2 + (weightKg / 15.0) * 0.4).coerceIn(0.2, 0.6)
                FeedRequirement((kg * 10).toInt() / 10.0, "Creep / Starter Pellets", "High-protein (20-22%) creep feed for young suckling piglets")
            }
            weightKg < 30.0 || stageId == 2L -> {
                val kg = (0.8 + ((weightKg - 15.0) / 15.0) * 0.6).coerceIn(0.8, 1.4)
                FeedRequirement((kg * 10).toInt() / 10.0, "Weaner Starter Feed", "Crude protein 18-20% starter ration for healthy growth")
            }
            weightKg < 60.0 || stageId == 3L -> {
                val kg = (1.5 + ((weightKg - 30.0) / 30.0) * 0.9).coerceIn(1.5, 2.4)
                FeedRequirement((kg * 10).toInt() / 10.0, "Grower Feed", "Balanced 16-18% protein grower mash for muscle development")
            }
            else -> {
                val kg = (2.5 + ((weightKg - 60.0) / 40.0) * 1.0).coerceIn(2.5, 3.5)
                FeedRequirement((kg * 10).toInt() / 10.0, "Finisher Meal", "Energy-dense 14-15% finisher meal for market weight gain")
            }
        }
    }
}

