package com.phms.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// --- MODULE 1: PIGS, WEIGHTS, PENS, BATCHES ---

@Entity(
    tableName = "pigs",
    indices = [Index(value = ["tag_number"], unique = true)]
)
data class PigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tag_number: String,
    val breed: String,
    val sex: String, // "M" or "F"
    val birth_date: Long, // Epoch timestamp in ms
    val source: String, // "Born" or "Purchased"
    val pen_id: Long?,
    val batch_id: Long?,
    val current_stage_id: Long = 1, // Default Piglet stage
    val status: String = "Active", // "Active", "Sold", "Dead", "Culled"
    val photo_path: String? = null,
    val notes: String? = null,
    val origin_farm: String? = null,
    val seller_contact: String? = null,
    val purchase_price: Double? = null,
    val transport_cost: Double? = null,
    val dam_id: Long? = null, // Mother sow pig ID
    val sire_id: Long? = null  // Father boar pig ID
)

@Entity(tableName = "weight_records")
data class WeightRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pig_id: Long,
    val date: Long,
    val weight_kg: Double
)

@Entity(tableName = "pens")
data class PenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val capacity: Int,
    val notes: String? = null
)

@Entity(tableName = "batches")
data class BatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val stage_id: Long,
    val created_date: Long
)

// --- MODULE 8: GROWTH STAGES & PROMOTION ---

@Entity(tableName = "growth_stages")
data class GrowthStageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val min_age_weeks: Int,
    val max_age_weeks: Int,
    val min_weight_kg: Double,
    val max_weight_kg: Double,
    val feed_formula_id: Long? = null,
    val health_protocol_id: Long? = null,
    val display_order: Int
)

@Entity(tableName = "pig_stage_history")
data class PigStageHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pig_id: Long,
    val batch_id: Long? = null,
    val old_stage_id: Long,
    val new_stage_id: Long,
    val promotion_date: Long,
    val weight_at_promotion: Double,
    val age_at_promotion_weeks: Int,
    val old_pen_id: Long? = null,
    val new_pen_id: Long? = null,
    val promoted_by: String = "System",
    val notes: String? = null,
    val is_reversed: Boolean = false,
    val reversal_date: Long? = null
)

@Entity(tableName = "pen_occupancy_history")
data class PenOccupancyHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pen_id: Long,
    val pig_id: Long? = null,
    val batch_id: Long? = null,
    val start_date: Long,
    val end_date: Long? = null,
    val reason: String // "promotion", "space", "quarantine"
)

@Entity(tableName = "growth_targets")
data class GrowthTargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stage_id: Long,
    val week_number: Int,
    val target_weight_kg: Double,
    val target_feed_kg_per_day: Double
)

// --- MODULE 4: FEED & NUTRITION ---

@Entity(tableName = "feed_ingredients")
data class FeedIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val stock_kg: Double,
    val cost_per_kg: Double,
    val reorder_level: Double,
    val type: String = "Raw", // "Raw Ingredient" or "Commercial Premix"
    val brand_name: String? = null, // e.g. "Unga Farmcare", "Pembe"
    val bag_size_kg: Double? = null, // e.g. 50.0
    val price_per_bag: Double? = null, // e.g. 3200.0
    val stage_category: String? = null // e.g. "Weaner", "Grower", "Finisher", "Sow/Gilt", "Creep/Starter"
)

@Entity(tableName = "feed_formulas")
data class FeedFormulaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val stage_id: Long?,
    val ingredients_json: String, // Format: {"ingredient_id": proportion_pct}
    val cost_per_kg: Double
)

@Entity(tableName = "feeding_logs")
data class FeedingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pen_id: Long?,
    val batch_id: Long?,
    val pig_id: Long? = null,
    val ingredient_id: Long? = null,
    val date: Long,
    val feeding_time: String = "Morning", // "Morning", "Afternoon", "Evening"
    val feed_type: String,
    val quantity_kg: Double,
    val num_pigs: Int = 1
)

@Entity(tableName = "feed_purchases")
data class FeedPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ingredient_id: Long,
    val date: Long,
    val quantity_kg: Double,
    val total_cost: Double
)

// --- MODULE 3: HEALTH & VETERINARY ---

@Entity(tableName = "health_events")
data class HealthEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pig_id: Long?, // Null if category or herd level
    val date: Long,
    val type: String, // "Vaccination", "Treatment", "Deworming", "Checkup", "Vitamin"
    val product: String,
    val dosage: String,
    val route: String,
    val withdrawal_days: Int = 0,
    val vet_name: String = "",
    val target_scope: String = "Single Pig", // "Single Pig", "Category", "Herd"
    val target_category: String? = null, // e.g. "Piglets", "Weaners", "Sows"
    val cost: Double = 0.0,
    val notes: String? = null,
    val disease_name: String? = null,
    val age_weeks: Int? = null,
    val weight_kg: Double? = null
)

@Entity(tableName = "symptoms")
data class SymptomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pig_id: Long,
    val pen_id: Long,
    val date: Long,
    val symptom: String,
    val severity: String // "Mild", "Moderate", "Severe"
)

@Entity(tableName = "quarantine")
data class QuarantineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pig_id: Long,
    val start_date: Long,
    val end_date: Long,
    val status: String = "Active" // "Active", "Cleared"
)

// --- MODULE 2: BREEDING & REPRODUCTION ---

@Entity(tableName = "breeding_events")
data class BreedingEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sow_id: Long,
    val boar_id: Long?,
    val date: Long,
    val type: String, // "Heat", "Insemination", "Mating"
    val notes: String? = null
)

@Entity(tableName = "pregnancies")
data class PregnancyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sow_id: Long,
    val insemination_date: Long,
    val expected_farrowing_date: Long, // insemination_date + 114 days
    val confirmed_date: Long? = null,
    val status: String = "Active" // "Active", "Completed", "Lost"
)

@Entity(tableName = "farrowing_records")
data class FarrowingRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sow_id: Long,
    val farrowing_date: Long,
    val total_born: Int,
    val born_alive: Int,
    val stillborn: Int,
    val mummified: Int,
    val avg_birth_weight: Double
)

@Entity(tableName = "weaning_records")
data class WeaningRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sow_id: Long,
    val weaning_date: Long,
    val piglets_weaned: Int,
    val avg_weaning_weight: Double
)

// --- MODULE 5: ALERTS ENGINE ---

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "Vaccination", "Heat Check", "Farrowing", "Low Feed", "Mortality Spike", "Withdrawal Active", "Market Ready", "Weaning Due", "Promotion Ready", "Slow Growth", "Overcrowding"
    val priority: String, // "Critical", "High", "Medium", "Low"
    val related_pig_id: Long? = null,
    val related_batch_id: Long? = null,
    val message: String,
    val created_date: Long,
    val status: String = "Active", // "Active", "Snoozed", "Done"
    val snoozed_until: Long? = null
)

// --- MODULE 6: TRADE & MARKET ---

@Entity(tableName = "buyers")
data class BuyerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String? = null,
    val location: String? = null,
    val county: String? = null,
    val sub_county: String? = null,
    val ward: String? = null,
    val type: String, // "Wholesaler", "Retailer", "Butchery", "Slaughterhouse"
    val notes: String? = null,
    val is_synced: Boolean = false
)

@Entity(tableName = "gilt_heat_records")
data class GiltHeatRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pig_id: Long,
    val heat_date: Long,
    val standing_heat_observed: Boolean,
    val symptoms: String, // "Standing Reflex", "Vulva Swelling", "Mucus Discharge", "Restlessness"
    val next_heat_alert_date: Long, // heat_date + 21 days
    val status: String = "Active", // "Active", "Served", "Expired"
    val notes: String? = null
)

// --- MODULE 7: GENERAL EXPENSE TRACKING ---

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "Feed", "Health & Vet", "Purchase", "Equipment", "Labor", "Utilities", "Misc"
    val amount: Double,
    val date: Long,
    val target_scope: String = "Herd", // "Single Pig", "Category", "Herd"
    val target_id: String? = null, // Pig ID or category name
    val payee: String = "",
    val notes: String? = null
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val buyer_id: Long,
    val date: Long,
    val pig_ids_json: String, // JSON array of pig IDs, e.g. "[1, 2, 3]"
    val total_weight: Double,
    val price_per_kg: Double,
    val total_amount: Double,
    val payment_status: String // "Paid", "Partial", "Pending"
)
