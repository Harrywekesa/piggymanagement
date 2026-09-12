package com.phms.app.data.repository

import com.phms.app.data.local.database.AppDatabase
import com.phms.app.data.local.entity.*
import com.phms.app.domain.engine.OutbreakDetector
import com.phms.app.domain.engine.PromotionEngine
import com.phms.app.domain.engine.WithdrawalTracker
import kotlinx.coroutines.flow.Flow

class PHMSRepository(private val db: AppDatabase) {

    val pigDao = db.pigDao()
    val penDao = db.penDao()
    val batchDao = db.batchDao()
    val growthStageDao = db.growthStageDao()
    val feedDao = db.feedDao()
    val healthDao = db.healthDao()
    val breedingDao = db.breedingDao()
    val alertDao = db.alertDao()
    val marketDao = db.marketDao()
    val expenseDao = db.expenseDao()

    val promotionEngine = PromotionEngine(pigDao, growthStageDao, alertDao)

    // Pig Flow
    val allActivePigs: Flow<List<PigEntity>> = pigDao.getActivePigs()
    val allPens: Flow<List<PenEntity>> = penDao.getAllPens()
    val allBatches: Flow<List<BatchEntity>> = batchDao.getAllBatches()
    val allStages: Flow<List<GrowthStageEntity>> = growthStageDao.getAllStages()
    val activeAlerts: Flow<List<AlertEntity>> = alertDao.getActiveAlerts()
    val criticalAlerts: Flow<List<AlertEntity>> = alertDao.getCriticalAlerts()
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val allBuyers: Flow<List<BuyerEntity>> = marketDao.getAllBuyers()
    val allSales: Flow<List<SaleEntity>> = marketDao.getAllSales()
    val allFormulas: Flow<List<FeedFormulaEntity>> = feedDao.getAllFormulas()
    val allHealthEvents: Flow<List<HealthEventEntity>> = healthDao.getAllEvents()
    val allFeedingLogs: Flow<List<FeedingLogEntity>> = feedDao.getAllFeedingLogs()

    suspend fun logFeedingAndDeductStock(
        penId: Long?,
        batchId: Long?,
        pigId: Long?,
        ingredientId: Long?,
        feedingTime: String,
        feedType: String,
        quantityPerPigKg: Double,
        numPigs: Int
    ): Long {
        val now = System.currentTimeMillis()
        val totalConsumedKg = quantityPerPigKg * numPigs

        val logId = feedDao.insertFeedingLog(
            FeedingLogEntity(
                pen_id = penId,
                batch_id = batchId,
                pig_id = pigId,
                ingredient_id = ingredientId,
                date = now,
                feeding_time = feedingTime,
                feed_type = feedType,
                quantity_kg = totalConsumedKg,
                num_pigs = numPigs
            )
        )

        // Deduct from stock if linked to ingredient/premix
        if (ingredientId != null) {
            feedDao.deductStock(ingredientId, totalConsumedKg)
        }

        return logId
    }

    suspend fun getOffspringForSow(sowId: Long) = pigDao.getOffspringForSowSync(sowId)
    suspend fun getOffspringForBoar(boarId: Long) = pigDao.getOffspringForBoarSync(boarId)

    suspend fun getPigById(id: Long) = pigDao.getPigById(id)

    suspend fun savePig(pig: PigEntity): Long {
        val pigId = pigDao.insertPig(pig)
        // Log purchase expense if applicable
        if (pig.source.equals("Purchased", true) && (pig.purchase_price ?: 0.0) > 0.0) {
            val totalCost = (pig.purchase_price ?: 0.0) + (pig.transport_cost ?: 0.0)
            expenseDao.insertExpense(
                ExpenseEntity(
                    category = "Purchase",
                    amount = totalCost,
                    date = System.currentTimeMillis(),
                    target_scope = "Single Pig",
                    target_id = pig.tag_number,
                    payee = pig.origin_farm ?: "Breeder",
                    notes = "Purchase of pig #${pig.tag_number} from ${pig.origin_farm ?: "Unknown farm"}"
                )
            )
        }
        return pigId
    }

    suspend fun updatePigStatus(pigId: Long, status: String) = pigDao.updatePigStatus(pigId, status)

    suspend fun addWeightRecord(pigId: Long, weightKg: Double): Long {
        val id = pigDao.insertWeightRecord(WeightRecordEntity(pig_id = pigId, date = System.currentTimeMillis(), weight_kg = weightKg))
        // Auto stage promotion evaluation
        promotionEngine.autoEvaluateWeightStage(pigId, weightKg)
        return id
    }

    // Health Event Logging (Individual, Category, Herd)
    suspend fun logHealthEvent(
        targetScope: String,
        targetPigId: Long?,
        targetCategory: String?,
        type: String,
        product: String,
        dosage: String,
        cost: Double,
        vetName: String,
        notes: String?,
        withdrawalDays: Int = 0
    ) {
        val now = System.currentTimeMillis()
        when (targetScope) {
            "Single Pig" -> {
                if (targetPigId != null) {
                    healthDao.insertHealthEvent(
                        HealthEventEntity(
                            pig_id = targetPigId,
                            date = now,
                            type = type,
                            product = product,
                            dosage = dosage,
                            route = "Oral/Inj",
                            withdrawal_days = withdrawalDays,
                            vet_name = vetName,
                            target_scope = "Single Pig",
                            cost = cost,
                            notes = notes
                        )
                    )
                }
            }
            "Category" -> {
                val pigs = pigDao.getActivePigsSync()
                val targetPigs = when (targetCategory) {
                    "Piglets" -> pigs.filter { it.current_stage_id == 1L }
                    "Weaners" -> pigs.filter { it.current_stage_id == 2L }
                    "Growers" -> pigs.filter { it.current_stage_id == 3L }
                    "Finishers" -> pigs.filter { it.current_stage_id == 4L }
                    "Sows" -> pigs.filter { it.sex.equals("F", true) }
                    "Boars" -> pigs.filter { it.sex.equals("M", true) }
                    else -> pigs
                }
                for (pig in targetPigs) {
                    healthDao.insertHealthEvent(
                        HealthEventEntity(
                            pig_id = pig.id,
                            date = now,
                            type = type,
                            product = product,
                            dosage = dosage,
                            route = "Group Treatment",
                            vet_name = vetName,
                            target_scope = "Category",
                            target_category = targetCategory,
                            cost = if (targetPigs.isNotEmpty()) cost / targetPigs.size else cost,
                            notes = notes
                        )
                    )
                }
            }
            "Herd" -> {
                val pigs = pigDao.getActivePigsSync()
                for (pig in pigs) {
                    healthDao.insertHealthEvent(
                        HealthEventEntity(
                            pig_id = pig.id,
                            date = now,
                            type = type,
                            product = product,
                            dosage = dosage,
                            route = "Herd Mass Admin",
                            vet_name = vetName,
                            target_scope = "Herd",
                            cost = if (pigs.isNotEmpty()) cost / pigs.size else cost,
                            notes = notes
                        )
                    )
                }
            }
        }
        // Log financial expense for health event
        if (cost > 0.0) {
            expenseDao.insertExpense(
                ExpenseEntity(
                    category = "Health & Vet",
                    amount = cost,
                    date = now,
                    target_scope = targetScope,
                    target_id = targetCategory ?: targetPigId?.toString() ?: "Herd",
                    payee = vetName.ifBlank { "Veterinary Supplier" },
                    notes = "$type - $product ($dosage)"
                )
            )
        }
    }

    // Expense Logging
    suspend fun logExpense(expense: ExpenseEntity) = expenseDao.insertExpense(expense)

    // Complete Sale Recording with Buyer Details
    suspend fun recordSaleWithBuyer(
        buyerName: String,
        buyerPhone: String,
        buyerEmail: String?,
        buyerLocation: String?,
        buyerType: String,
        pigIds: List<Long>,
        totalWeightKg: Double,
        pricePerKg: Double,
        paymentStatus: String
    ): Long {
        val now = System.currentTimeMillis()
        val totalAmount = totalWeightKg * pricePerKg

        // 1. Create or Find Buyer
        val existingBuyers = marketDao.getAllBuyersSync()
        var buyerId = existingBuyers.find { it.phone == buyerPhone || it.name.equals(buyerName, true) }?.id
        if (buyerId == null) {
            buyerId = marketDao.insertBuyer(
                BuyerEntity(
                    name = buyerName,
                    phone = buyerPhone,
                    email = buyerEmail,
                    location = buyerLocation,
                    type = buyerType
                )
            )
        }

        // 2. Insert Sale
        val pigIdsJson = pigIds.joinToString(prefix = "[", postfix = "]")
        val saleId = marketDao.insertSale(
            SaleEntity(
                buyer_id = buyerId,
                date = now,
                pig_ids_json = pigIdsJson,
                total_weight = totalWeightKg,
                price_per_kg = pricePerKg,
                total_amount = totalAmount,
                payment_status = paymentStatus
            )
        )

        // 3. Update Pigs status to "Sold"
        for (pigId in pigIds) {
            pigDao.updatePigStatus(pigId, "Sold")
        }

        return saleId
    }

    // Feed Formulation Saving
    suspend fun saveFeedFormula(name: String, stageId: Long?, ingredientsJson: String, costPerKg: Double): Long {
        return feedDao.insertFormula(
            FeedFormulaEntity(
                name = name,
                stage_id = stageId,
                ingredients_json = ingredientsJson,
                cost_per_kg = costPerKg
            )
        )
    }

    // Withdrawal & Safety Check
    suspend fun isPigWithdrawalActive(pigId: Long): Boolean = WithdrawalTracker.isWithdrawalActive(pigId, healthDao)

    // Outbreak Trigger Check
    suspend fun runOutbreakCheck(): List<Long> = OutbreakDetector.detectOutbreaks(healthDao)

    // Promotion Execution
    suspend fun promotePig(pigId: Long, newStageId: Long, newPenId: Long?) = promotionEngine.promotePig(pigId, newStageId, newPenId)
    suspend fun undoPromotion(historyId: Long) = promotionEngine.undoPromotion(historyId)

    // Seed Demo Data
    suspend fun seedDemoData() {
        com.phms.app.data.seed.DemoDataSeeder.seedDatabase(db)
    }
}

