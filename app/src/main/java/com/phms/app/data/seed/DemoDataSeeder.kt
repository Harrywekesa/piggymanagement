package com.phms.app.data.seed

import com.phms.app.data.local.database.AppDatabase
import com.phms.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object DemoDataSeeder {

    suspend fun seedDatabase(db: AppDatabase) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dayMs = TimeUnit.DAYS.toMillis(1)

        // 1. Growth Stages Default Setup
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

        // 2. Pens Setup
        val pens = listOf(
            PenEntity(id = 1, name = "Pen A1 (Nursery)", capacity = 15),
            PenEntity(id = 2, name = "Pen A2 (Growers)", capacity = 12),
            PenEntity(id = 3, name = "Pen B1 (Finishers)", capacity = 10),
            PenEntity(id = 4, name = "Pen B2 (Farrowing Bay)", capacity = 4),
            PenEntity(id = 5, name = "Pen C1 (Quarantine)", capacity = 5)
        )
        for (pen in pens) db.penDao().insertPen(pen)

        // 3. Batches Setup
        val batch1Id = db.batchDao().insertBatch(BatchEntity(name = "Batch 2026-Alpha", stage_id = 3, created_date = now - (60 * dayMs)))
        val batch2Id = db.batchDao().insertBatch(BatchEntity(name = "Batch 2026-Beta", stage_id = 2, created_date = now - (30 * dayMs)))

        // 4. Seed 35 Realistic Pigs across stages
        val breeds = listOf("Landrace", "Large White", "Duroc", "Camborough")
        val pigs = mutableListOf<PigEntity>()

        // Breeding Sows & Boar
        val sow1Id = db.pigDao().insertPig(PigEntity(tag_number = "SOW-001", breed = "Landrace", sex = "F", birth_date = now - (400 * dayMs), source = "Purchased", pen_id = 4, batch_id = null, current_stage_id = 6, status = "Active"))
        val sow2Id = db.pigDao().insertPig(PigEntity(tag_number = "SOW-002", breed = "Camborough", sex = "F", birth_date = now - (350 * dayMs), source = "Purchased", pen_id = 4, batch_id = null, current_stage_id = 6, status = "Active"))
        val boar1Id = db.pigDao().insertPig(PigEntity(tag_number = "BOAR-01", breed = "Duroc", sex = "M", birth_date = now - (450 * dayMs), source = "Purchased", pen_id = 4, batch_id = null, current_stage_id = 7, status = "Active"))

        // Market Ready Pigs
        for (i in 1..5) {
            val tag = "MR-10$i"
            val pId = db.pigDao().insertPig(PigEntity(tag_number = tag, breed = "Duroc", sex = if (i % 2 == 0) "M" else "F", birth_date = now - (180 * dayMs), source = "Born", pen_id = 3, batch_id = batch1Id, current_stage_id = 5, status = "Active"))
            db.pigDao().insertWeightRecord(WeightRecordEntity(pig_id = pId, date = now - (7 * dayMs), weight_kg = 92.5 + (i * 1.2)))
            db.pigDao().insertWeightRecord(WeightRecordEntity(pig_id = pId, date = now, weight_kg = 96.0 + (i * 1.5)))
        }

        // Growers (Batch Alpha)
        for (i in 1..12) {
            val tag = "GR-20$i"
            val pId = db.pigDao().insertPig(PigEntity(tag_number = tag, breed = "Large White", sex = if (i % 2 == 0) "M" else "F", birth_date = now - (90 * dayMs), source = "Born", pen_id = 2, batch_id = batch1Id, current_stage_id = 3, status = "Active"))
            db.pigDao().insertWeightRecord(WeightRecordEntity(pig_id = pId, date = now - (14 * dayMs), weight_kg = 35.0 + i))
            db.pigDao().insertWeightRecord(WeightRecordEntity(pig_id = pId, date = now, weight_kg = 42.5 + i))
        }

        // Weaners (Batch Beta)
        for (i in 1..15) {
            val tag = "WN-30$i"
            val pId = db.pigDao().insertPig(PigEntity(tag_number = tag, breed = "Landrace", sex = if (i % 2 == 0) "M" else "F", birth_date = now - (45 * dayMs), source = "Born", pen_id = 1, batch_id = batch2Id, current_stage_id = 2, status = "Active"))
            db.pigDao().insertWeightRecord(WeightRecordEntity(pig_id = pId, date = now - (7 * dayMs), weight_kg = 12.0 + i))
            db.pigDao().insertWeightRecord(WeightRecordEntity(pig_id = pId, date = now, weight_kg = 15.2 + i))
        }

        // 5. Feed Ingredients & Purchases
        val ingr1 = db.feedDao().insertIngredient(FeedIngredientEntity(name = "Maize Germ", stock_kg = 450.0, cost_per_kg = 38.0, reorder_level = 100.0))
        val ingr2 = db.feedDao().insertIngredient(FeedIngredientEntity(name = "Soya Bean Meal", stock_kg = 35.0, cost_per_kg = 85.0, reorder_level = 50.0)) // Low stock!
        val ingr3 = db.feedDao().insertIngredient(FeedIngredientEntity(name = "Wheat Pollard", stock_kg = 200.0, cost_per_kg = 32.0, reorder_level = 80.0))
        val ingr4 = db.feedDao().insertIngredient(FeedIngredientEntity(name = "Pig Premix", stock_kg = 15.0, cost_per_kg = 250.0, reorder_level = 20.0)) // Low stock!

        db.feedDao().insertPurchase(FeedPurchaseEntity(ingredient_id = ingr1, date = now - (10 * dayMs), quantity_kg = 500.0, total_cost = 19000.0))
        db.feedDao().insertPurchase(FeedPurchaseEntity(ingredient_id = ingr3, date = now - (10 * dayMs), quantity_kg = 300.0, total_cost = 9600.0))

        // 6. Health & Withdrawal Log
        db.healthDao().insertHealthEvent(
            HealthEventEntity(pig_id = 4, date = now - (2 * dayMs), type = "Treatment", product = "Oxytetracycline 20%", dosage = "5ml", route = "IM", withdrawal_days = 7, vet_name = "Dr. Wafula", notes = "Respiratory treatment")
        )
        db.healthDao().insertHealthEvent(
            HealthEventEntity(pig_id = sow1Id, date = now - (30 * dayMs), type = "Vaccination", product = "Parvovirus Vaccine", dosage = "2ml", route = "IM", withdrawal_days = 0, vet_name = "Dr. Wafula")
        )

        // 7. Breeding Events & Pregnancy
        db.breedingDao().insertPregnancy(
            PregnancyEntity(sow_id = sow1Id, insemination_date = now - (100 * dayMs), expected_farrowing_date = now + (14 * dayMs), confirmed_date = now - (70 * dayMs))
        )
        db.breedingDao().insertFarrowingRecord(
            FarrowingRecordEntity(sow_id = sow2Id, farrowing_date = now - (50 * dayMs), total_born = 12, born_alive = 11, stillborn = 1, mummified = 0, avg_birth_weight = 1.4)
        )

        // 8. Alerts Setup (Persistent & Actionable)
        db.alertDao().insertAlert(AlertEntity(type = "Farrowing Expected", priority = "High", related_pig_id = sow1Id, message = "Sow #SOW-001 is due for farrowing in 14 days (Expected: 114 days post-insemination).", created_date = now))
        db.alertDao().insertAlert(AlertEntity(type = "Drug Withdrawal Active", priority = "Critical", related_pig_id = 4, message = "Pig MR-101 has an active drug withdrawal (Oxytetracycline) until 5 days from now. HARD SALES BLOCK ENFORCED.", created_date = now))
        db.alertDao().insertAlert(AlertEntity(type = "Low Feed Stock", priority = "High", message = "Soya Bean Meal stock is LOW (35kg remaining, below 50kg reorder level).", created_date = now))
        db.alertDao().insertAlert(AlertEntity(type = "Promotion Ready", priority = "Medium", message = "5 Finishers in Pen B1 are ready for Market Ready promotion.", created_date = now))

        // 9. Buyers & Sales
        val bId1 = db.marketDao().insertBuyer(BuyerEntity(name = "Kitale Quality Butchery", phone = "+254712345678", type = "Butchery", notes = "Regular purchaser of 90kg+ finishers"))
        val bId2 = db.marketDao().insertBuyer(BuyerEntity(name = "Western Meat Wholesalers", phone = "+254722987654", type = "Wholesaler", notes = "Prefers Duroc crossbreds"))

        db.marketDao().insertSale(
            SaleEntity(buyer_id = bId1, date = now - (15 * dayMs), pig_ids_json = "[991, 992]", total_weight = 195.0, price_per_kg = 340.0, total_amount = 66300.0, payment_status = "Paid")
        )
    }
}
