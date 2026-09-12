package com.phms.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.phms.app.data.local.dao.*
import com.phms.app.data.local.entity.*
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
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
        GiltHeatRecordEntity::class
    ],
    version = 5,
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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
