package com.phms.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.phms.app.data.local.dao.*
import com.phms.app.data.local.entity.*
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

// --- Safe Migrations (data is NEVER wiped on update) ---

/** v7 → v8: Added repeat_interval_days column to health_events */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE health_events ADD COLUMN repeat_interval_days INTEGER NOT NULL DEFAULT 0")
    }
}

/** v8 → v9: Added mixed_batches table + mixed_batch_id column on feeding_logs */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS mixed_batches (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                formula_name TEXT NOT NULL,
                total_kg REAL NOT NULL,
                remaining_kg REAL NOT NULL,
                cost_per_kg REAL NOT NULL,
                total_cost REAL NOT NULL,
                ingredients_used_json TEXT NOT NULL,
                skipped_ingredients_json TEXT NOT NULL DEFAULT '[]',
                is_complete_formula INTEGER NOT NULL DEFAULT 1,
                created_date INTEGER NOT NULL,
                notes TEXT
            )
        """.trimIndent())
        db.execSQL("ALTER TABLE feeding_logs ADD COLUMN mixed_batch_id INTEGER")
    }
}

@Database(
    entities = [
        PigEntity::class,
        WeightRecordEntity::class,
        PenEntity::class,
        BatchEntity::class,
        GrowthStageEntity::class,
        PigStageHistoryEntity::class,
        PenOccupancyHistoryEntity::class,
        GrowthTargetEntity::class,
        FeedIngredientEntity::class,
        FeedFormulaEntity::class,
        FeedingLogEntity::class,
        FeedPurchaseEntity::class,
        HealthEventEntity::class,
        SymptomEntity::class,
        QuarantineEntity::class,
        BreedingEventEntity::class,
        PregnancyEntity::class,
        FarrowingRecordEntity::class,
        WeaningRecordEntity::class,
        AlertEntity::class,
        BuyerEntity::class,
        SaleEntity::class,
        ExpenseEntity::class,
        GiltHeatRecordEntity::class,
        MixedBatchEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pigDao(): PigDao
    abstract fun penDao(): PenDao
    abstract fun batchDao(): BatchDao
    abstract fun growthStageDao(): GrowthStageDao
    abstract fun feedDao(): FeedDao
    abstract fun healthDao(): HealthDao
    abstract fun breedingDao(): BreedingDao
    abstract fun alertDao(): AlertDao
    abstract fun marketDao(): MarketDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun mixedBatchDao(): MixedBatchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "phms_database"
                )
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
