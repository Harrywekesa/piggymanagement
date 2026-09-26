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
        numPigs: Int,
        targetScope: String = "Full Herd",
        category: String? = null,
        notes: String? = null
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
                num_pigs = numPigs,
                target_scope = targetScope,
                category = category,
                notes = notes
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
        withdrawalDays: Int = 0,
        diseaseName: String? = null,
        ageWeeks: Int? = null,
        weightKg: Double? = null
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
                            notes = notes,
                            disease_name = diseaseName,
                            age_weeks = ageWeeks,
                            weight_kg = weightKg
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
                            notes = notes,
                            disease_name = diseaseName,
                            age_weeks = ageWeeks,
                            weight_kg = weightKg
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
                            notes = notes,
                            disease_name = diseaseName,
                            age_weeks = ageWeeks,
                            weight_kg = weightKg
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

    // Community Buyers Directory — seed once per fresh install
    suspend fun seedCommunityBuyersIfNeeded() {
        val existing = marketDao.getAllBuyersSync()
        if (existing.any { it.is_community }) return  // already seeded

        val communityBuyers = listOf(
            BuyerEntity(name = "Kitale Quality Butchery", phone = "+254712345678", type = "Butchery", county = "Trans Nzoia", sub_county = "Kitale", notes = "Regular buyer of 90kg+ finishers. Mon–Sat.", is_community = true),
            BuyerEntity(name = "Western Meat Wholesalers", phone = "+254722987654", type = "Wholesaler", county = "Trans Nzoia", sub_county = "Kitale", notes = "Prefers Duroc crossbreds. Bulk orders only.", is_community = true),
            BuyerEntity(name = "Eldoret Pork Centre", phone = "+254733111222", type = "Butchery", county = "Uasin Gishu", sub_county = "Eldoret East", notes = "Accepts live & dressed pigs. Pays cash.", is_community = true),
            BuyerEntity(name = "Kakamega Pig Farmers Coop", phone = "+254711333444", type = "Cooperative", county = "Kakamega", sub_county = "Kakamega Central", notes = "Group buying — better prices for 5+ pigs.", is_community = true),
            BuyerEntity(name = "Kisumu Pork Traders Ltd", phone = "+254700456789", type = "Wholesaler", county = "Kisumu", sub_county = "Kisumu Central", notes = "Lakeside market. Weekly collection Fridays.", is_community = true),
            BuyerEntity(name = "Bungoma Meat Packers", phone = "+254721567890", type = "Processor", county = "Bungoma", sub_county = "Bungoma", notes = "Processes carcasses. Needs 80kg+ weight.", is_community = true),
            BuyerEntity(name = "Trans Nzoia Pork Dealers", phone = "+254700678901", type = "Wholesaler", county = "Trans Nzoia", sub_county = "Kiminini", notes = "Serves Kitale–Webuye route. Daily purchases.", is_community = true),
            BuyerEntity(name = "Nandi Butchery Supplies", phone = "+254722789012", type = "Butchery", county = "Nandi", sub_county = "Nandi Hills", notes = "Retail butchery. Buys 3–5 pigs per week.", is_community = true),
            BuyerEntity(name = "Siaya Hog Market", phone = "+254733890123", type = "Wholesaler", county = "Siaya", sub_county = "Ugenya", notes = "Saturday market only. Large volumes.", is_community = true),
            BuyerEntity(name = "Webuye Pork House", phone = "+254711901234", type = "Butchery", county = "Bungoma", sub_county = "Webuye West", notes = "Family-run. Good relationship price possible.", is_community = true),
            BuyerEntity(name = "Kitale Hotel & Pork Supplies", phone = "+254700123456", type = "Hotel Buyer", county = "Trans Nzoia", sub_county = "Kitale", notes = "Supplies restaurants. Prefers pork cuts.", is_community = true),
            BuyerEntity(name = "Eldoret Hotel Buyers Group", phone = "+254722234567", type = "Hotel Buyer", county = "Uasin Gishu", sub_county = "Eldoret East", notes = "Consortium of 8 hotels. Monthly contracts.", is_community = true),
            BuyerEntity(name = "Western Kenya Abattoir", phone = "+254733345678", type = "Slaughterhouse", county = "Kakamega", sub_county = "Shinyalu", notes = "Licensed abattoir. Accepts all breeds.", is_community = true),
            BuyerEntity(name = "Mumias Pork Dealers", phone = "+254711456789", type = "Wholesaler", county = "Kakamega", sub_county = "Mumias West", notes = "Sugarcane belt market. Good demand.", is_community = true),
            BuyerEntity(name = "Turbo Livestock Traders", phone = "+254721567891", type = "Wholesaler", county = "Uasin Gishu", sub_county = "Turbo", notes = "Highway market. Buys live pigs.", is_community = true),
            BuyerEntity(name = "Kimilili Pork Sellers", phone = "+254700678902", type = "Butchery", county = "Bungoma", sub_county = "Kimilili", notes = "Active on Tuesday and Friday market days.", is_community = true),
            BuyerEntity(name = "Webuye Open Market Buyers", phone = "+254722789013", type = "Wholesaler", county = "Bungoma", sub_county = "Webuye East", notes = "Thursday market. Competitive cash price.", is_community = true),
            BuyerEntity(name = "Vihiga Pork Supply Chain", phone = "+254700345678", type = "Wholesaler", county = "Vihiga", sub_county = "Emuhaya", notes = "Connects farms to Kisumu hotels.", is_community = true),
            BuyerEntity(name = "Uasin Gishu Hog Market", phone = "+254733456789", type = "Wholesale Market", county = "Uasin Gishu", sub_county = "Moiben", notes = "Large livestock auction. Monthly events.", is_community = true),
            BuyerEntity(name = "Nandi Hills Butchery Network", phone = "+254711567890", type = "Butchery", county = "Nandi", sub_county = "Nandi Hills", notes = "Network of 4 butcheries — consolidated orders.", is_community = true)
        )
        communityBuyers.forEach { marketDao.insertBuyer(it) }
    }
}

