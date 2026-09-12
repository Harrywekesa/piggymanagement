package com.phms.app.data.local.dao

import androidx.room.*
import com.phms.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PigDao {
    @Query("SELECT * FROM pigs ORDER BY id DESC")
    fun getAllPigs(): Flow<List<PigEntity>>

    @Query("SELECT * FROM pigs WHERE status = 'Active' ORDER BY id DESC")
    fun getActivePigs(): Flow<List<PigEntity>>

    @Query("SELECT * FROM pigs WHERE status = 'Active'")
    suspend fun getActivePigsSync(): List<PigEntity>

    @Query("SELECT * FROM pigs WHERE id = :id")
    suspend fun getPigById(id: Long): PigEntity?

    @Query("SELECT * FROM pigs WHERE tag_number = :tagNumber LIMIT 1")
    suspend fun getPigByTag(tagNumber: String): PigEntity?

    @Query("SELECT * FROM pigs WHERE batch_id = :batchId AND status = 'Active'")
    suspend fun getPigsByBatchSync(batchId: Long): List<PigEntity>

    @Query("SELECT * FROM pigs WHERE pen_id = :penId AND status = 'Active'")
    suspend fun getPigsByPenSync(penId: Long): List<PigEntity>

    @Query("SELECT * FROM pigs WHERE dam_id = :sowId ORDER BY id DESC")
    suspend fun getOffspringForSowSync(sowId: Long): List<PigEntity>

    @Query("SELECT * FROM pigs WHERE sire_id = :boarId ORDER BY id DESC")
    suspend fun getOffspringForBoarSync(boarId: Long): List<PigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPig(pig: PigEntity): Long

    @Update
    suspend fun updatePig(pig: PigEntity)

    @Query("UPDATE pigs SET status = :status WHERE id = :pigId")
    suspend fun updatePigStatus(pigId: Long, status: String)

    @Query("UPDATE pigs SET current_stage_id = :stageId, pen_id = COALESCE(:newPenId, pen_id) WHERE id = :pigId")
    suspend fun promotePig(pigId: Long, stageId: Long, newPenId: Long?)

    // Weight Records
    @Query("SELECT * FROM weight_records WHERE pig_id = :pigId ORDER BY date ASC")
    fun getWeightsForPig(pigId: Long): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records WHERE pig_id = :pigId ORDER BY date DESC")
    suspend fun getWeightsForPigSync(pigId: Long): List<WeightRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightRecord(record: WeightRecordEntity): Long
}

@Dao
interface PenDao {
    @Query("SELECT * FROM pens ORDER BY name ASC")
    fun getAllPens(): Flow<List<PenEntity>>

    @Query("SELECT * FROM pens ORDER BY name ASC")
    suspend fun getAllPensSync(): List<PenEntity>

    @Query("SELECT * FROM pens WHERE id = :id")
    suspend fun getPenById(id: Long): PenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPen(pen: PenEntity): Long

    @Update
    suspend fun updatePen(pen: PenEntity)
}

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches ORDER BY created_date DESC")
    fun getAllBatches(): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches ORDER BY created_date DESC")
    suspend fun getAllBatchesSync(): List<BatchEntity>

    @Query("SELECT * FROM batches WHERE id = :id")
    suspend fun getBatchById(id: Long): BatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: BatchEntity): Long

    @Update
    suspend fun updateBatch(batch: BatchEntity)
}

@Dao
interface GrowthStageDao {
    @Query("SELECT * FROM growth_stages ORDER BY display_order ASC")
    fun getAllStages(): Flow<List<GrowthStageEntity>>

    @Query("SELECT * FROM growth_stages ORDER BY display_order ASC")
    suspend fun getAllStagesSync(): List<GrowthStageEntity>

    @Query("SELECT * FROM growth_stages WHERE id = :id")
    suspend fun getStageById(id: Long): GrowthStageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStage(stage: GrowthStageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStages(stages: List<GrowthStageEntity>)

    @Update
    suspend fun updateStage(stage: GrowthStageEntity)

    // Stage History
    @Query("SELECT * FROM pig_stage_history WHERE pig_id = :pigId ORDER BY promotion_date DESC")
    fun getStageHistoryForPig(pigId: Long): Flow<List<PigStageHistoryEntity>>

    @Query("SELECT * FROM pig_stage_history WHERE id = :id")
    suspend fun getStageHistoryById(id: Long): PigStageHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStageHistory(history: PigStageHistoryEntity): Long

    @Update
    suspend fun updateStageHistory(history: PigStageHistoryEntity)

    // Pen Occupancy
    @Query("SELECT * FROM pen_occupancy_history WHERE pen_id = :penId ORDER BY start_date DESC")
    fun getOccupancyForPen(penId: Long): Flow<List<PenOccupancyHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccupancy(occupancy: PenOccupancyHistoryEntity): Long

    // Growth Targets
    @Query("SELECT * FROM growth_targets WHERE stage_id = :stageId ORDER BY week_number ASC")
    suspend fun getTargetsForStage(stageId: Long): List<GrowthTargetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrowthTargets(targets: List<GrowthTargetEntity>)
}

@Dao
interface FeedDao {
    @Query("SELECT * FROM feed_ingredients ORDER BY name ASC")
    fun getAllIngredients(): Flow<List<FeedIngredientEntity>>

    @Query("SELECT * FROM feed_ingredients ORDER BY name ASC")
    suspend fun getAllIngredientsSync(): List<FeedIngredientEntity>

    @Query("SELECT * FROM feed_ingredients WHERE id = :id")
    suspend fun getIngredientById(id: Long): FeedIngredientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: FeedIngredientEntity): Long

    @Update
    suspend fun updateIngredient(ingredient: FeedIngredientEntity)

    @Query("UPDATE feed_ingredients SET stock_kg = stock_kg + :addedKg WHERE id = :ingredientId")
    suspend fun addStock(ingredientId: Long, addedKg: Double)

    @Query("UPDATE feed_ingredients SET stock_kg = MAX(0.0, stock_kg - :deductedKg) WHERE id = :ingredientId")
    suspend fun deductStock(ingredientId: Long, deductedKg: Double)

    // Formulas
    @Query("SELECT * FROM feed_formulas ORDER BY name ASC")
    fun getAllFormulas(): Flow<List<FeedFormulaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormula(formula: FeedFormulaEntity): Long

    // Feeding Logs
    @Query("SELECT * FROM feeding_logs ORDER BY date DESC")
    fun getAllFeedingLogs(): Flow<List<FeedingLogEntity>>

    @Query("SELECT * FROM feeding_logs WHERE date >= :startDate ORDER BY date DESC")
    suspend fun getFeedingLogsSince(startDate: Long): List<FeedingLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedingLog(log: FeedingLogEntity): Long

    // Purchases
    @Query("SELECT * FROM feed_purchases ORDER BY date DESC")
    fun getAllPurchases(): Flow<List<FeedPurchaseEntity>>

    @Query("SELECT * FROM feed_purchases WHERE date >= :startDate")
    suspend fun getPurchasesSince(startDate: Long): List<FeedPurchaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: FeedPurchaseEntity): Long
}

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_events WHERE pig_id = :pigId ORDER BY date DESC")
    fun getEventsForPig(pigId: Long): Flow<List<HealthEventEntity>>

    @Query("SELECT * FROM health_events ORDER BY date DESC")
    fun getAllEvents(): Flow<List<HealthEventEntity>>

    @Query("SELECT * FROM health_events ORDER BY date DESC")
    suspend fun getAllEventsSync(): List<HealthEventEntity>

    @Query("SELECT * FROM health_events WHERE pig_id = :pigId ORDER BY date DESC")
    suspend fun getEventsForPigSync(pigId: Long): List<HealthEventEntity>

    @Query("SELECT * FROM health_events WHERE withdrawal_days > 0 ORDER BY date DESC")
    suspend fun getEventsWithWithdrawal(): List<HealthEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthEvent(event: HealthEventEntity): Long

    // Symptoms
    @Query("SELECT * FROM symptoms ORDER BY date DESC")
    fun getAllSymptoms(): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptoms WHERE date >= :startDate ORDER BY date DESC")
    suspend fun getSymptomsSince(startDate: Long): List<SymptomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: SymptomEntity): Long

    // Quarantine
    @Query("SELECT * FROM quarantine WHERE status = 'Active' ORDER BY start_date DESC")
    fun getActiveQuarantines(): Flow<List<QuarantineEntity>>

    @Query("SELECT * FROM quarantine WHERE pig_id = :pigId AND status = 'Active' LIMIT 1")
    suspend fun getActiveQuarantineForPig(pigId: Long): QuarantineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuarantine(quarantine: QuarantineEntity): Long

    @Query("UPDATE quarantine SET status = 'Cleared' WHERE id = :quarantineId")
    suspend fun clearQuarantine(quarantineId: Long)
}

@Dao
interface BreedingDao {
    @Query("SELECT * FROM breeding_events WHERE sow_id = :sowId ORDER BY date DESC")
    fun getBreedingForSow(sowId: Long): Flow<List<BreedingEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreedingEvent(event: BreedingEventEntity): Long

    // Pregnancies
    @Query("SELECT * FROM pregnancies WHERE status = 'Active' ORDER BY expected_farrowing_date ASC")
    fun getActivePregnancies(): Flow<List<PregnancyEntity>>

    @Query("SELECT * FROM pregnancies WHERE sow_id = :sowId AND status = 'Active' LIMIT 1")
    suspend fun getActivePregnancyForSow(sowId: Long): PregnancyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPregnancy(pregnancy: PregnancyEntity): Long

    @Update
    suspend fun updatePregnancy(pregnancy: PregnancyEntity)

    // Farrowing
    @Query("SELECT * FROM farrowing_records WHERE sow_id = :sowId ORDER BY farrowing_date DESC")
    fun getFarrowingForSow(sowId: Long): Flow<List<FarrowingRecordEntity>>

    @Query("SELECT * FROM farrowing_records ORDER BY farrowing_date DESC")
    suspend fun getAllFarrowingRecordsSync(): List<FarrowingRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarrowingRecord(record: FarrowingRecordEntity): Long

    // Weaning
    @Query("SELECT * FROM weaning_records WHERE sow_id = :sowId ORDER BY weaning_date DESC")
    fun getWeaningForSow(sowId: Long): Flow<List<WeaningRecordEntity>>

    @Query("SELECT * FROM weaning_records ORDER BY weaning_date DESC")
    suspend fun getAllWeaningRecordsSync(): List<WeaningRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeaningRecord(record: WeaningRecordEntity): Long
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts WHERE status = 'Active' ORDER BY CASE priority WHEN 'Critical' THEN 1 WHEN 'High' THEN 2 WHEN 'Medium' THEN 3 ELSE 4 END, created_date DESC")
    fun getActiveAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE status = 'Active' AND priority = 'Critical'")
    fun getCriticalAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts ORDER BY created_date DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Query("UPDATE alerts SET status = 'Done' WHERE id = :alertId")
    suspend fun markAlertDone(alertId: Long)

    @Query("UPDATE alerts SET status = 'Snoozed', snoozed_until = :snoozedUntil WHERE id = :alertId")
    suspend fun snoozeAlert(alertId: Long, snoozedUntil: Long)
}

@Dao
interface MarketDao {
    @Query("SELECT * FROM buyers ORDER BY name ASC")
    fun getAllBuyers(): Flow<List<BuyerEntity>>

    @Query("SELECT * FROM buyers ORDER BY name ASC")
    suspend fun getAllBuyersSync(): List<BuyerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuyer(buyer: BuyerEntity): Long

    @Update
    suspend fun updateBuyer(buyer: BuyerEntity)

    // Sales
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE date >= :startDate ORDER BY date DESC")
    suspend fun getSalesSince(startDate: Long): List<SaleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesSync(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE date >= :startDate ORDER BY date DESC")
    suspend fun getExpensesSince(startDate: Long): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long
}

