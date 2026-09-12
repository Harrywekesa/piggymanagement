package com.phms.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phms.app.data.local.entity.*
import com.phms.app.data.repository.FarmSettings
import com.phms.app.data.repository.PHMSRepository
import com.phms.app.data.repository.SettingsRepository
import com.phms.app.domain.calculator.PnLCalculator
import com.phms.app.domain.calculator.PnLSummary
import com.phms.app.domain.engine.WithdrawalTracker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainViewModel(
    val repository: PHMSRepository,
    val settingsRepository: SettingsRepository
) : ViewModel() {

    val activePigs: StateFlow<List<PigEntity>> = repository.allActivePigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pens: StateFlow<List<PenEntity>> = repository.allPens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batches: StateFlow<List<BatchEntity>> = repository.allBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stages: StateFlow<List<GrowthStageEntity>> = repository.allStages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAlerts: StateFlow<List<AlertEntity>> = repository.activeAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val criticalAlerts: StateFlow<List<AlertEntity>> = repository.criticalAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedIngredients: StateFlow<List<FeedIngredientEntity>> =
        repository.feedDao.getAllIngredients()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleEntity>> = repository.marketDao.getAllSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val buyers: StateFlow<List<BuyerEntity>> = repository.marketDao.getAllBuyers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val farmSettings: StateFlow<FarmSettings> = settingsRepository.settings

    val isOnboarded: StateFlow<Boolean> = farmSettings
        .map { it.isOnboarded }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.get().isOnboarded)

    private val _pnlSummary = MutableStateFlow<PnLSummary?>(null)
    val pnlSummary: StateFlow<PnLSummary?> = _pnlSummary.asStateFlow()

    // Selected pig for detail view
    private val _selectedPig = MutableStateFlow<PigEntity?>(null)
    val selectedPig: StateFlow<PigEntity?> = _selectedPig.asStateFlow()

    private val _selectedPigWeights = MutableStateFlow<List<WeightRecordEntity>>(emptyList())
    val selectedPigWeights: StateFlow<List<WeightRecordEntity>> = _selectedPigWeights.asStateFlow()

    private val _selectedPigHealth = MutableStateFlow<List<HealthEventEntity>>(emptyList())
    val selectedPigHealth: StateFlow<List<HealthEventEntity>> = _selectedPigHealth.asStateFlow()

    private val _selectedPigStageHistory = MutableStateFlow<List<PigStageHistoryEntity>>(emptyList())
    val selectedPigStageHistory: StateFlow<List<PigStageHistoryEntity>> = _selectedPigStageHistory.asStateFlow()

    // Breeding flows
    val activePregnancies: StateFlow<List<PregnancyEntity>> = repository.breedingDao.getActivePregnancies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _farrowingRecords = MutableStateFlow<List<FarrowingRecordEntity>>(emptyList())
    val farrowingRecords: StateFlow<List<FarrowingRecordEntity>> = _farrowingRecords.asStateFlow()

    private val _isWithdrawalActive = MutableStateFlow(false)
    val isWithdrawalActive: StateFlow<Boolean> = _isWithdrawalActive.asStateFlow()

    init {
        loadPnL()
        loadFarrowingRecords()
    }

    fun loadPnL(startDateMs: Long? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val from = startDateMs ?: (now - TimeUnit.DAYS.toMillis(30))
            _pnlSummary.value = PnLCalculator.computePnL(
                startDateMs = from,
                feedDao = repository.feedDao,
                healthDao = repository.healthDao,
                marketDao = repository.marketDao,
                pigDao = repository.pigDao,
                expenseDao = repository.expenseDao
            )
        }
    }

    fun selectPig(pigId: Long) {
        viewModelScope.launch {
            val pig = repository.pigDao.getPigById(pigId)
            _selectedPig.value = pig
            pig?.let {
                _selectedPigWeights.value = repository.pigDao.getWeightsForPigSync(pigId)
                _selectedPigHealth.value = repository.healthDao.getEventsForPigSync(pigId)
                _isWithdrawalActive.value = WithdrawalTracker.isWithdrawalActive(pigId, repository.healthDao)
                // Fixed: use first() for a one-shot snapshot instead of an infinite collect
                _selectedPigStageHistory.value = repository.growthStageDao.getStageHistoryForPig(pigId).first()
            }
        }
    }

    fun saveFarmSettings(settings: FarmSettings) {
        settingsRepository.save(settings)
    }

    fun seedDemoData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.seedDemoData()
            loadPnL()
            onComplete()
        }
    }

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val formulas: StateFlow<List<FeedFormulaEntity>> = repository.allFormulas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHealthEvents: StateFlow<List<HealthEventEntity>> = repository.allHealthEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedingLogs: StateFlow<List<FeedingLogEntity>> = repository.allFeedingLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setOnboarded(onboarded: Boolean) {
        settingsRepository.setOnboarded(onboarded)
    }

    fun addFeedIngredient(
        name: String, stockKg: Double, costPerKg: Double, reorderLevel: Double,
        type: String = "Raw Ingredient", brandName: String? = null, bagSizeKg: Double? = null, pricePerBag: Double? = null,
        stageCategory: String? = null
    ) {
        viewModelScope.launch {
            repository.feedDao.insertIngredient(
                FeedIngredientEntity(
                    name = name, stock_kg = stockKg, cost_per_kg = costPerKg, reorder_level = reorderLevel,
                    type = type, brand_name = brandName, bag_size_kg = bagSizeKg, price_per_bag = pricePerBag,
                    stage_category = stageCategory
                )
            )
            // Log expense if shop premix purchase cost entered
            if (type == "Commercial Premix" && (pricePerBag ?: 0.0) > 0.0) {
                val bags = if (bagSizeKg != null && bagSizeKg > 0) stockKg / bagSizeKg else 1.0
                val totalCost = bags * (pricePerBag ?: 0.0)
                repository.logExpense(
                    ExpenseEntity(
                        category = "Feed",
                        amount = totalCost,
                        date = System.currentTimeMillis(),
                        target_scope = "Herd",
                        payee = brandName ?: name,
                        notes = "Purchase of $name (${bags.toInt()} bags)"
                    )
                )
            }
        }
    }

    fun logFeedingAndDeductStock(
        penId: Long? = null,
        batchId: Long? = null,
        pigId: Long? = null,
        ingredientId: Long? = null,
        feedingTime: String = "Morning",
        feedType: String = "Formula Ration",
        quantityPerPigKg: Double = 1.0,
        numPigs: Int = 1
    ) {
        viewModelScope.launch {
            repository.logFeedingAndDeductStock(penId, batchId, pigId, ingredientId, feedingTime, feedType, quantityPerPigKg, numPigs)
            loadPnL()
        }
    }

    fun addPig(
        tag: String, breed: String, sex: String, source: String,
        penId: Long?, batchId: Long?, stageId: Long,
        birthDateMs: Long = System.currentTimeMillis(),
        photoPath: String? = null,
        originFarm: String? = null, sellerContact: String? = null,
        purchasePrice: Double? = null, transportCost: Double? = null,
        damId: Long? = null, sireId: Long? = null
    ) {
        viewModelScope.launch {
            repository.savePig(
                PigEntity(
                    tag_number = tag, breed = breed, sex = sex,
                    birth_date = birthDateMs, source = source,
                    pen_id = penId, batch_id = batchId,
                    current_stage_id = stageId, status = "Active",
                    photo_path = photoPath,
                    origin_farm = originFarm, seller_contact = sellerContact,
                    purchase_price = purchasePrice, transport_cost = transportCost,
                    dam_id = damId, sire_id = sireId
                )
            )
        }
    }

    fun updatePig(pig: PigEntity) {
        viewModelScope.launch {
            repository.pigDao.updatePig(pig)
            _selectedPig.value = pig
        }
    }

    fun logWeight(pigId: Long, weightKg: Double) {
        viewModelScope.launch {
            repository.addWeightRecord(pigId, weightKg)
            _selectedPigWeights.value = repository.pigDao.getWeightsForPigSync(pigId)
            selectPig(pigId) // Refresh selected pig details (e.g. auto stage promotion)
        }
    }

    fun promotePig(pigId: Long, newStageId: Long, newPenId: Long?) {
        viewModelScope.launch { repository.promotePig(pigId, newStageId, newPenId) }
    }

    fun markAlertDone(alertId: Long) {
        viewModelScope.launch { repository.alertDao.markAlertDone(alertId) }
    }

    fun markPigStatus(pigId: Long, status: String) {
        viewModelScope.launch { repository.updatePigStatus(pigId, status) }
    }

    fun logGroupHealthEvent(
        targetScope: String, targetPigId: Long?, targetCategory: String?,
        type: String, product: String, dosage: String, cost: Double, vetName: String, notes: String?,
        withdrawalDays: Int = 0
    ) {
        viewModelScope.launch {
            repository.logHealthEvent(targetScope, targetPigId, targetCategory, type, product, dosage, cost, vetName, notes, withdrawalDays)
            if (targetPigId != null) {
                _selectedPigHealth.value = repository.healthDao.getEventsForPigSync(targetPigId)
            }
            loadPnL()
        }
    }

    fun logExpense(category: String, amount: Double, scope: String, targetId: String?, payee: String, notes: String?) {
        viewModelScope.launch {
            repository.logExpense(
                ExpenseEntity(
                    category = category,
                    amount = amount,
                    date = System.currentTimeMillis(),
                    target_scope = scope,
                    target_id = targetId,
                    payee = payee,
                    notes = notes
                )
            )
            loadPnL()
        }
    }

    fun recordSaleWithBuyer(
        buyerName: String, buyerPhone: String, buyerEmail: String?, buyerLocation: String?, buyerType: String,
        selectedPigIds: List<Long>, totalWeight: Double, pricePerKg: Double, paymentStatus: String
    ) {
        viewModelScope.launch {
            repository.recordSaleWithBuyer(
                buyerName = buyerName,
                buyerPhone = buyerPhone,
                buyerEmail = buyerEmail,
                buyerLocation = buyerLocation,
                buyerType = buyerType,
                pigIds = selectedPigIds,
                totalWeightKg = totalWeight,
                pricePerKg = pricePerKg,
                paymentStatus = paymentStatus
            )
            loadPnL()
        }
    }

    fun saveFeedFormula(name: String, stageId: Long?, ingredientsJson: String, costPerKg: Double) {
        viewModelScope.launch {
            repository.saveFeedFormula(name, stageId, ingredientsJson, costPerKg)
        }
    }

    fun addBuyer(
        name: String, phone: String, email: String? = null, location: String? = null,
        county: String? = null, subCounty: String? = null, ward: String? = null,
        type: String = "Wholesaler", notes: String = ""
    ) {
        viewModelScope.launch {
            repository.marketDao.insertBuyer(
                BuyerEntity(
                    name = name, phone = phone, email = email, location = location,
                    county = county, sub_county = subCounty, ward = ward,
                    type = type, notes = notes
                )
            )
        }
    }

    val giltHeatRecords: StateFlow<List<GiltHeatRecordEntity>> = repository.breedingDao.getAllGiltHeatRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordGiltHeat(pigId: Long, standingHeat: Boolean, symptoms: String, notes: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val nextHeatMs = now + (21L * 24 * 60 * 60 * 1000)
            repository.breedingDao.insertGiltHeatRecord(
                GiltHeatRecordEntity(
                    pig_id = pigId,
                    heat_date = now,
                    standing_heat_observed = standingHeat,
                    symptoms = symptoms,
                    next_heat_alert_date = nextHeatMs,
                    status = "Active",
                    notes = notes
                )
            )
            val pig = repository.pigDao.getPigById(pigId)
            repository.alertDao.insertAlert(
                AlertEntity(
                    type = "Heat Check",
                    priority = "High",
                    related_pig_id = pigId,
                    message = "Gilt #${pig?.tag_number ?: pigId} heat recorded. Next heat check due in 21 days.",
                    created_date = now
                )
            )
        }
    }

    fun recordGiltService(pigId: Long, boarId: Long?, serviceType: String, notes: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            recordMating(pigId, boarId, serviceType, notes)
            val pig = repository.pigDao.getPigById(pigId)
            pig?.let {
                if (it.sex == "F") {
                    repository.pigDao.updatePig(it.copy(current_stage_id = 4)) // Promote to Breeding Sow stage
                }
            }
            val farrowingDateMs = now + (114L * 24 * 60 * 60 * 1000)
            repository.alertDao.insertAlert(
                AlertEntity(
                    type = "Farrowing",
                    priority = "Critical",
                    related_pig_id = pigId,
                    message = "Gilt #${pig?.tag_number ?: pigId} served. Expected Farrowing on ${java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(farrowingDateMs))}. Prepare pen on Day 110.",
                    created_date = now
                )
            )
        }
    }

    fun logSale(
        buyerId: Long, selectedPigIds: List<Long>,
        totalWeight: Double, pricePerKg: Double, paymentStatus: String
    ) {
        viewModelScope.launch {
            val totalAmount = totalWeight * pricePerKg
            repository.marketDao.insertSale(
                SaleEntity(
                    buyer_id = buyerId,
                    date = System.currentTimeMillis(),
                    pig_ids_json = selectedPigIds.toString(),
                    total_weight = totalWeight,
                    price_per_kg = pricePerKg,
                    total_amount = totalAmount,
                    payment_status = paymentStatus
                )
            )
            for (pId in selectedPigIds) {
                repository.updatePigStatus(pId, "Sold")
            }
            loadPnL()
        }
    }

    // ── Breeding & Reproduction ──────────────────────────────────────────────

    fun loadFarrowingRecords() {
        viewModelScope.launch {
            _farrowingRecords.value = repository.breedingDao.getAllFarrowingRecordsSync()
        }
    }

    fun recordMating(sowId: Long, boarId: Long?, type: String, notes: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.breedingDao.insertBreedingEvent(
                BreedingEventEntity(
                    sow_id = sowId,
                    boar_id = boarId,
                    date = now,
                    type = type,
                    notes = notes
                )
            )
            // Auto-create pregnancy (gestation = 114 days)
            val expectedFarrowing = now + (114L * 24 * 60 * 60 * 1000)
            // Only create if no active pregnancy for this sow
            if (repository.breedingDao.getActivePregnancyForSow(sowId) == null) {
                repository.breedingDao.insertPregnancy(
                    PregnancyEntity(
                        sow_id = sowId,
                        insemination_date = now,
                        expected_farrowing_date = expectedFarrowing,
                        status = "Active"
                    )
                )
            }
        }
    }

    fun recordFarrowing(
        sowId: Long, totalBorn: Int, bornAlive: Int,
        stillborn: Int, mummified: Int, avgBirthWeight: Double
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.breedingDao.insertFarrowingRecord(
                FarrowingRecordEntity(
                    sow_id = sowId,
                    farrowing_date = now,
                    total_born = totalBorn,
                    born_alive = bornAlive,
                    stillborn = stillborn,
                    mummified = mummified,
                    avg_birth_weight = avgBirthWeight
                )
            )
            // Mark active pregnancy as completed
            val pregnancy = repository.breedingDao.getActivePregnancyForSow(sowId)
            pregnancy?.let {
                repository.breedingDao.updatePregnancy(it.copy(status = "Completed"))
            }
            loadFarrowingRecords()
        }
    }

    fun recordWeaning(sowId: Long, pigletsWeaned: Int, avgWeaningWeight: Double) {
        viewModelScope.launch {
            repository.breedingDao.insertWeaningRecord(
                WeaningRecordEntity(
                    sow_id = sowId,
                    weaning_date = System.currentTimeMillis(),
                    piglets_weaned = pigletsWeaned,
                    avg_weaning_weight = avgWeaningWeight
                )
            )
        }
    }
}

