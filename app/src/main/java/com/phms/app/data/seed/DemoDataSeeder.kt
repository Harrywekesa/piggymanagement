package com.phms.app.data.seed

import com.phms.app.data.local.database.AppDatabase
import com.phms.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DemoDataSeeder {

    suspend fun seedDatabase(db: AppDatabase) = withContext(Dispatchers.IO) {
        // Only seed essential lookup data - no demo pigs, pens, or transactions
        val stages = listOf(
            GrowthStageEntity(id = 1, name = "Piglet", min_age_weeks = 0, max_age_weeks = 4, min_weight_kg = 1.5, max_weight_kg = 7.0, display_order = 1),
            GrowthStageEntity(id = 2, name = "Weaner", min_age_weeks = 4, max_age_weeks = 10, min_weight_kg = 7.0, max_weight_kg = 25.0, display_order = 2),
            GrowthStageEntity(id = 3, name = "Grower", min_age_weeks = 10, max_age_weeks = 16, min_weight_kg = 25.0, max_weight_kg = 60.0, display_order = 3),
            GrowthStageEntity(id = 4, name = "Finisher", min_age_weeks = 16, max_age_weeks = 24, min_weight_kg = 60.0, max_weight_kg = 90.0, display_order = 4),
            GrowthStageEntity(id = 5, name = "Market Ready", min_age_weeks = 24, max_age_weeks = 40, min_weight_kg = 90.0, max_weight_kg = 110.0, display_order = 5),
            GrowthStageEntity(id = 6, name = "Sow", min_age_weeks = 32, max_age_weeks = 200, min_weight_kg = 120.0, max_weight_kg = 250.0, display_order = 6),
            GrowthStageEntity(id = 7, name = "Boar", min_age_weeks = 32, max_age_weeks = 200, min_weight_kg = 130.0, max_weight_kg = 300.0, display_order = 7),
            GrowthStageEntity(id = 8, name = "Gilt", min_age_weeks = 24, max_age_weeks = 32, min_weight_kg = 90.0, max_weight_kg = 120.0, display_order = 8)
        )
        db.growthStageDao().insertStages(stages)
    }
}
