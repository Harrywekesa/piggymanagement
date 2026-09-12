package com.phms.app.domain.calculator

import com.phms.app.data.local.dao.*
import com.phms.app.data.local.entity.*
import java.util.concurrent.TimeUnit

object FeedCalculator {
    /**
     * Calculates FCR (Feed Conversion Ratio):
     * FCR = total feed consumed (kg) / total weight gain (kg)
     */
    fun calculateFCR(totalFeedConsumedKg: Double, totalWeightGainKg: Double): Double {
        if (totalWeightGainKg <= 0.0) return 0.0
        return totalFeedConsumedKg / totalWeightGainKg
    }

    /**
     * Calculates Average Daily Gain (ADG):
     * ADG = (latest_weight - previous_weight) / days_between
     */
    fun calculateADG(weightRecords: List<WeightRecordEntity>): Double {
        if (weightRecords.size < 2) return 0.0
        val sorted = weightRecords.sortedByDescending { it.date }
        val latest = sorted[0]
        val previous = sorted[1]

        val daysBetween = (latest.date - previous.date) / (1000.0 * 60 * 60 * 24)
        if (daysBetween <= 0) return 0.0

        val weightGain = latest.weight_kg - previous.weight_kg
        return weightGain / daysBetween
    }
}

data class PnLSummary(
    val totalRevenue: Double,
    val feedCost: Double,
    val healthCost: Double,
    val otherExpensesCost: Double,
    val estimatedLaborCost: Double,
    val netProfit: Double,
    val activePigCount: Int,
    val costPerPig: Double
)

object PnLCalculator {
    suspend fun computePnL(
        startDateMs: Long,
        feedDao: FeedDao,
        healthDao: HealthDao,
        marketDao: MarketDao,
        pigDao: PigDao,
        expenseDao: ExpenseDao? = null,
        laborEstimate: Double = 5000.0 // Default farm monthly labor estimate
    ): PnLSummary {
        // Sales revenue
        val sales = marketDao.getSalesSince(startDateMs)
        val totalRevenue = sales.sumOf { it.total_amount }

        // Feed cost — real purchases from DB
        val feedPurchases = feedDao.getPurchasesSince(startDateMs)
        val feedCost = feedPurchases.sumOf { it.total_cost }

        // Health cost — real events from DB (sum cost field where present)
        val healthEvents = healthDao.getAllEventsSync()
        val healthCost: Double = healthEvents
            .filter { it.date >= startDateMs }
            .sumOf { it.cost ?: 0.0 }

        // Other non-feed non-health expenses (e.g. equipment, labor)
        val otherExpensesCost: Double = expenseDao?.getExpensesSince(startDateMs)
            ?.filter { it.category != "Feed" && it.category != "Health" }
            ?.sumOf { it.amount } ?: 0.0

        val netProfit = totalRevenue - (feedCost + healthCost + otherExpensesCost + laborEstimate)

        val activePigs = pigDao.getActivePigsSync()
        val activeCount = activePigs.size
        val totalCosts = feedCost + healthCost + otherExpensesCost + laborEstimate
        val costPerPig = if (activeCount > 0) totalCosts / activeCount else 0.0

        return PnLSummary(
            totalRevenue = totalRevenue,
            feedCost = feedCost,
            healthCost = healthCost,
            otherExpensesCost = otherExpensesCost,
            estimatedLaborCost = laborEstimate,
            netProfit = netProfit,
            activePigCount = activeCount,
            costPerPig = costPerPig
        )
    }
}
