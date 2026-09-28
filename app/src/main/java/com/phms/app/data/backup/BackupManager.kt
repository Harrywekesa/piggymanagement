package com.phms.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.phms.app.data.local.database.AppDatabase
import com.phms.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// PHMS BACKUP / RESTORE  (v1 schema)
// ─────────────────────────────────────────────────────────────────────────────

data class PHMSBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val exportedAtReadable: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        .format(Date()),
    val pigs: List<PigEntity> = emptyList(),
    val pens: List<PenEntity> = emptyList(),
    val batches: List<BatchEntity> = emptyList(),
    val weightRecords: List<WeightRecordEntity> = emptyList(),
    val feedIngredients: List<FeedIngredientEntity> = emptyList(),
    val feedingLogs: List<FeedingLogEntity> = emptyList(),
    val feedPurchases: List<FeedPurchaseEntity> = emptyList(),
    val healthEvents: List<HealthEventEntity> = emptyList(),
    val breedingEvents: List<BreedingEventEntity> = emptyList(),
    val pregnancies: List<PregnancyEntity> = emptyList(),
    val farrowingRecords: List<FarrowingRecordEntity> = emptyList(),
    val weaningRecords: List<WeaningRecordEntity> = emptyList(),
    val giltHeatRecords: List<GiltHeatRecordEntity> = emptyList(),
    val alerts: List<AlertEntity> = emptyList(),
    val buyers: List<BuyerEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList()
)

sealed class BackupResult {
    data class Success(val message: String) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

object BackupManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    // ─── EXPORT ──────────────────────────────────────────────────────────────

    suspend fun exportToUri(
        context: Context,
        db: AppDatabase,
        uri: Uri
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val backup = collectAllData(db)
            val json = gson.toJson(backup)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                    writer.write(json)
                }
            } ?: return@withContext BackupResult.Error("Could not open output stream for file")

            val pigsCount = backup.pigs.size
            val healthCount = backup.healthEvents.size
            val feedCount = backup.feedingLogs.size
            BackupResult.Success(
                "Backup saved!\n" +
                "${pigsCount} pigs · ${healthCount} health events · ${feedCount} feed logs\n" +
                "Exported: ${backup.exportedAtReadable}"
            )
        } catch (e: Exception) {
            BackupResult.Error("Export failed: ${e.message}")
        }
    }

    private suspend fun collectAllData(db: AppDatabase): PHMSBackup {
        val pigDao = db.pigDao()
        val penDao = db.penDao()
        val batchDao = db.batchDao()
        val feedDao = db.feedDao()
        val healthDao = db.healthDao()
        val breedingDao = db.breedingDao()
        val marketDao = db.marketDao()
        val expenseDao = db.expenseDao()

        return PHMSBackup(
            pigs = pigDao.getAllPigsSync(),
            pens = penDao.getAllPensSync(),
            batches = batchDao.getAllBatchesSync(),
            weightRecords = pigDao.getAllWeightRecordsSync(),
            feedIngredients = feedDao.getAllIngredientsSync(),
            feedingLogs = feedDao.getAllFeedingLogsSync(),
            feedPurchases = feedDao.getAllFeedPurchasesSync(),
            healthEvents = healthDao.getAllHealthEventsSync(),
            breedingEvents = breedingDao.getAllBreedingEventsSync(),
            pregnancies = breedingDao.getAllPregnanciesSync(),
            farrowingRecords = breedingDao.getAllFarrowingRecordsSync(),
            weaningRecords = breedingDao.getAllWeaningRecordsSync(),
            giltHeatRecords = breedingDao.getAllGiltHeatRecordsSync(),
            alerts = db.alertDao().getAllAlertsSync(),
            buyers = marketDao.getAllBuyersSync(),
            sales = marketDao.getAllSalesSync(),
            expenses = expenseDao.getAllExpensesSync()
        )
    }

    // ─── IMPORT ──────────────────────────────────────────────────────────────

    suspend fun importFromUri(
        context: Context,
        db: AppDatabase,
        uri: Uri
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { input ->
                InputStreamReader(input, Charsets.UTF_8).readText()
            } ?: return@withContext BackupResult.Error("Could not read backup file")

            val backup = gson.fromJson(json, PHMSBackup::class.java)
                ?: return@withContext BackupResult.Error("Backup file is empty or corrupted")

            if (backup.version != 1) {
                return@withContext BackupResult.Error(
                    "Unsupported backup version (${backup.version}). Please update the app."
                )
            }

            restoreAllData(db, backup)

            BackupResult.Success(
                "Restore complete!\n" +
                "${backup.pigs.size} pigs · ${backup.healthEvents.size} health events\n" +
                "Backup was from: ${backup.exportedAtReadable}"
            )
        } catch (e: Exception) {
            BackupResult.Error("Import failed: ${e.message}")
        }
    }

    private suspend fun restoreAllData(db: AppDatabase, backup: PHMSBackup) {
        val pigDao = db.pigDao()
        val penDao = db.penDao()
        val batchDao = db.batchDao()
        val feedDao = db.feedDao()
        val healthDao = db.healthDao()
        val breedingDao = db.breedingDao()
        val marketDao = db.marketDao()
        val expenseDao = db.expenseDao()
        val alertDao = db.alertDao()

        // Clear and restore all data inside a single Room transaction
        db.withTransaction {
            // Delete existing data
            pigDao.deleteAllWeightRecords()
            feedDao.deleteAllFeedingLogs()
            feedDao.deleteAllFeedPurchases()
            healthDao.deleteAllHealthEvents()
            breedingDao.deleteAllBreedingEvents()
            breedingDao.deleteAllPregnancies()
            breedingDao.deleteAllFarrowingRecords()
            breedingDao.deleteAllWeaningRecords()
            breedingDao.deleteAllGiltHeatRecords()
            marketDao.deleteAllSales()
            expenseDao.deleteAllExpenses()
            alertDao.deleteAllAlerts()
            pigDao.deleteAllPigs()
            batchDao.deleteAllBatches()
            penDao.deleteAllPens()
            feedDao.deleteAllIngredients()
            marketDao.deleteAllBuyers()

            // Restore in safe dependency order
            // 1. Reference/lookup tables first
            if (backup.pens.isNotEmpty()) penDao.insertAllPens(backup.pens)
            if (backup.batches.isNotEmpty()) batchDao.insertAllBatches(backup.batches)

            // 2. Pigs
            if (backup.pigs.isNotEmpty()) pigDao.insertAllPigs(backup.pigs)

            // 3. Weight records
            if (backup.weightRecords.isNotEmpty()) pigDao.insertAllWeightRecords(backup.weightRecords)

            // 4. Feed
            if (backup.feedIngredients.isNotEmpty()) feedDao.insertAllIngredients(backup.feedIngredients)
            if (backup.feedingLogs.isNotEmpty()) feedDao.insertAllFeedingLogs(backup.feedingLogs)
            if (backup.feedPurchases.isNotEmpty()) feedDao.insertAllFeedPurchases(backup.feedPurchases)

            // 5. Health
            if (backup.healthEvents.isNotEmpty()) healthDao.insertAllHealthEvents(backup.healthEvents)

            // 6. Breeding
            if (backup.breedingEvents.isNotEmpty()) breedingDao.insertAllBreedingEvents(backup.breedingEvents)
            if (backup.pregnancies.isNotEmpty()) breedingDao.insertAllPregnancies(backup.pregnancies)
            if (backup.farrowingRecords.isNotEmpty()) breedingDao.insertAllFarrowingRecords(backup.farrowingRecords)
            if (backup.weaningRecords.isNotEmpty()) breedingDao.insertAllWeaningRecords(backup.weaningRecords)
            if (backup.giltHeatRecords.isNotEmpty()) breedingDao.insertAllGiltHeatRecords(backup.giltHeatRecords)

            // 7. Market & Finance
            if (backup.buyers.isNotEmpty()) marketDao.insertAllBuyers(backup.buyers)
            if (backup.sales.isNotEmpty()) marketDao.insertAllSales(backup.sales)
            if (backup.expenses.isNotEmpty()) expenseDao.insertAllExpenses(backup.expenses)

            // 8. Alerts
            if (backup.alerts.isNotEmpty()) alertDao.insertAllAlerts(backup.alerts)
        }
    }
}
