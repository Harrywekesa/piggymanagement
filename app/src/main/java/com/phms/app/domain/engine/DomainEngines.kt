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
                // Deduplicate: Don't insert if active promotion alert already exists for this pig
                val existingAlert = alertDao.getActiveAlertForPig("Promotion Ready", pig.id)
                if (existingAlert == null) {
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
                val existingAlert = alertDao.getActiveAlertForPig("Market Ready", pigId)
                if (existingAlert == null) {
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
            }
            return targetStageId
        }
        return pig.current_stage_id
    }
}

data class SwineFeedingGuideItem(
    val category: String,
    val stageName: String,
    val ageRange: String,
    val weightRange: String,
    val dailyFeedRationKg: Double,
    val dailyFeedRange: String,
    val feedType: String,
    val crudeProtein: String,
    val dailyMeals: String,
    val waterReqLiters: String,
    val managementGuide: String
)

object SwineFeedingKey {
    val table = listOf(
        SwineFeedingGuideItem(
            category = "Piglets (Suckling)",
            stageName = "Piglet",
            ageRange = "1 – 4 weeks (Day 7 to Weaning)",
            weightRange = "1.5 – 7.0 kg",
            dailyFeedRationKg = 0.3,
            dailyFeedRange = "0.2 – 0.5 kg / pig / day",
            feedType = "Creep Feed / Pre-Starter Pellets",
            crudeProtein = "20% – 22% Crude Protein",
            dailyMeals = "3–4 small fresh feeds daily",
            waterReqLiters = "0.5 – 1.0 Liters / day",
            managementGuide = "Introduce fresh creep feed in small pinches from day 7. High lactose and digestible milk proteins prepare piglet gut for weaning."
        ),
        SwineFeedingGuideItem(
            category = "Weaners",
            stageName = "Weaner",
            ageRange = "4 – 10 weeks",
            weightRange = "7.0 – 25.0 kg",
            dailyFeedRationKg = 1.0,
            dailyFeedRange = "0.8 – 1.4 kg / pig / day",
            feedType = "Weaner Starter Pellets / Mash",
            crudeProtein = "18% – 20% Crude Protein",
            dailyMeals = "Ad libitum or 3 meals daily",
            waterReqLiters = "2.0 – 4.0 Liters / day",
            managementGuide = "Transition gradually over 5 days from creep to starter. Ensure constant clean drinking water to prevent post-weaning scours."
        ),
        SwineFeedingGuideItem(
            category = "Growers",
            stageName = "Grower",
            ageRange = "10 – 16 weeks",
            weightRange = "25.0 – 60.0 kg",
            dailyFeedRationKg = 1.8,
            dailyFeedRange = "1.5 – 2.2 kg / pig / day",
            feedType = "Grower Mash / Pellets",
            crudeProtein = "16% – 18% Crude Protein",
            dailyMeals = "2 equal feeds (Morning 7am, Evening 5pm)",
            waterReqLiters = "5.0 – 7.0 Liters / day",
            managementGuide = "Maximum frame and muscle building phase. Target Average Daily Gain (ADG) 600–750g/day. Keep trough space clean."
        ),
        SwineFeedingGuideItem(
            category = "Finishers (Porkers)",
            stageName = "Finisher",
            ageRange = "16 – 24 weeks",
            weightRange = "60.0 – 90.0 kg",
            dailyFeedRationKg = 2.6,
            dailyFeedRange = "2.4 – 3.0 kg / pig / day",
            feedType = "Finisher Meal",
            crudeProtein = "14% – 16% Crude Protein",
            dailyMeals = "2 feeds daily",
            waterReqLiters = "7.0 – 10.0 Liters / day",
            managementGuide = "High energy ration to achieve market slaughter weight rapidly. Avoid excess fat buildup by adhering to ration limits."
        ),
        SwineFeedingGuideItem(
            category = "Market Ready (Heavy Baconers)",
            stageName = "Market Ready",
            ageRange = "24 – 28 weeks",
            weightRange = "90.0 – 110.0+ kg",
            dailyFeedRationKg = 3.0,
            dailyFeedRange = "2.8 – 3.2 kg / pig / day",
            feedType = "Finisher / Maintenance Mash",
            crudeProtein = "13% – 14% Crude Protein",
            dailyMeals = "2 feeds daily",
            waterReqLiters = "8.0 – 12.0 Liters / day",
            managementGuide = "Controlled feeding to maintain muscle without excessive lard thickness before slaughter."
        ),
        SwineFeedingGuideItem(
            category = "Pregnant / Gestating Sows",
            stageName = "Sow (Pregnant)",
            ageRange = "Mature (Gestation 114 days)",
            weightRange = "140.0 – 250.0 kg",
            dailyFeedRationKg = 2.2,
            dailyFeedRange = "2.0 – 2.5 kg / sow / day (2.8kg in late gestation)",
            feedType = "Sow & Weaner (Gestation Ration)",
            crudeProtein = "13% – 15% Crude Protein (High Fiber)",
            dailyMeals = "1–2 feeds daily",
            waterReqLiters = "12.0 – 18.0 Liters / day",
            managementGuide = "Do not overfeed during early-to-mid pregnancy (fat sows farrow poorly). Increase feed to 2.8kg during final 3 weeks for piglet size."
        ),
        SwineFeedingGuideItem(
            category = "Lactating Sows (Nursing)",
            stageName = "Sow (Lactating)",
            ageRange = "Nursing (Day 0 to Weaning ~28d)",
            weightRange = "150.0 – 250.0 kg",
            dailyFeedRationKg = 5.5,
            dailyFeedRange = "4.0 – 7.0 kg / sow / day",
            feedType = "Lactation Sow Feed",
            crudeProtein = "16% – 18% Crude Protein (High Calcium/Energy)",
            dailyMeals = "3 feeds daily",
            waterReqLiters = "25.0 – 40.0 Liters / day",
            managementGuide = "Golden Rule: 2.5 kg base feed + 0.5 kg for EVERY suckling piglet (e.g. 10 piglets = 7.5 kg/day). Water must never run dry!"
        ),
        SwineFeedingGuideItem(
            category = "Gilts (Breeding Prep)",
            stageName = "Gilt",
            ageRange = "24 – 32 weeks (6 – 8 months)",
            weightRange = "90.0 – 120.0 kg",
            dailyFeedRationKg = 2.4,
            dailyFeedRange = "2.2 – 2.6 kg / day (3.0kg flushing)",
            feedType = "Gilt Developer / Sow & Weaner",
            crudeProtein = "15% – 16% Crude Protein",
            dailyMeals = "2 feeds daily",
            waterReqLiters = "10.0 – 15.0 Liters / day",
            managementGuide = "Flush with 3.0 kg/day 10–14 days before servicing to increase litter ovulation rate, then drop back to 2.2 kg immediately after mating."
        ),
        SwineFeedingGuideItem(
            category = "Breeding Boars",
            stageName = "Boar",
            ageRange = "Mature (8+ months)",
            weightRange = "150.0 – 300.0 kg",
            dailyFeedRationKg = 2.5,
            dailyFeedRange = "2.2 – 2.8 kg / boar / day",
            feedType = "Boar / Breeder Mash",
            crudeProtein = "14% – 15% Crude Protein",
            dailyMeals = "2 feeds daily",
            waterReqLiters = "12.0 – 20.0 Liters / day",
            managementGuide = "Keep boars lean, muscular, and active. Overfeeding causes foot rot and lethargy during mounting. Boost feed during heavy servicing seasons."
        )
    )

    fun getRecommendation(weightKg: Double?, ageWeeks: Int?, categoryName: String?): SwineFeedingGuideItem {
        if (!categoryName.isNullOrBlank()) {
            val catLower = categoryName.lowercase()
            when {
                catLower.contains("piglet") -> return table[0]
                catLower.contains("weaner") -> return table[1]
                catLower.contains("grower") -> return table[2]
                catLower.contains("finisher") -> return table[3]
                catLower.contains("market") -> return table[4]
                catLower.contains("lactat") -> return table[6]
                catLower.contains("sow") -> return table[5]
                catLower.contains("gilt") -> return table[7]
                catLower.contains("boar") -> return table[8]
            }
        }
        if (weightKg != null && weightKg > 0) {
            return when {
                weightKg < 7.0 -> table[0]
                weightKg < 25.0 -> table[1]
                weightKg < 60.0 -> table[2]
                weightKg < 90.0 -> table[3]
                weightKg < 115.0 -> table[4]
                else -> table[5]
            }
        }
        if (ageWeeks != null && ageWeeks > 0) {
            return when {
                ageWeeks < 4 -> table[0]
                ageWeeks < 10 -> table[1]
                ageWeeks < 16 -> table[2]
                ageWeeks < 24 -> table[3]
                ageWeeks < 30 -> table[4]
                else -> table[5]
            }
        }
        return table[2] // default to Grower
    }
}

object FeedCalculator {
    data class FeedRequirement(
        val dailyKg: Double,
        val feedType: String,
        val description: String
    )

    fun getDailyRequirement(weightKg: Double, stageId: Long, sex: String = "M"): FeedRequirement {
        val categoryHint = when {
            sex.equals("F_Breeding", true) || sex.equals("Sow", true) -> "Sow"
            sex.equals("M", true) && weightKg >= 90.0 -> "Boar"
            stageId == 1L || weightKg < 7.0 -> "Piglet"
            stageId == 2L || weightKg < 25.0 -> "Weaner"
            stageId == 3L || weightKg < 60.0 -> "Grower"
            stageId == 4L || weightKg < 90.0 -> "Finisher"
            stageId == 5L -> "Market Ready"
            stageId == 6L -> "Sow"
            stageId == 7L -> "Boar"
            stageId == 8L -> "Gilt"
            else -> "Grower"
        }
        val rec = SwineFeedingKey.getRecommendation(weightKg, null, categoryHint)
        return FeedRequirement(rec.dailyFeedRationKg, rec.feedType, "${rec.crudeProtein} • ${rec.dailyMeals}")
    }
}


