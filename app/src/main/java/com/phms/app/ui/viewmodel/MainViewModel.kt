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

    private val _appUpdateInfo = MutableStateFlow(com.phms.app.data.updater.AppUpdateInfo())
    val appUpdateInfo: StateFlow<com.phms.app.data.updater.AppUpdateInfo> = _appUpdateInfo.asStateFlow()

    init {
        checkForAppUpdates()
        seedCommunityBuyers()
        syncBuyersFromCloud()
    }

    private fun seedCommunityBuyers() {
        viewModelScope.launch {
            repository.seedCommunityBuyersIfNeeded()
        }
    }

    fun syncBuyersFromCloud() {
        viewModelScope.launch {
            com.phms.app.data.remote.FirestoreBuyerSync.syncFromCloud(repository.marketDao)
        }
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            _appUpdateInfo.value = _appUpdateInfo.value.copy(isChecking = true)
            val info = com.phms.app.data.updater.GitHubUpdateChecker.checkForUpdates()
            _appUpdateInfo.value = info
        }
    }

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

    suspend fun getLatestWeightForPig(pigId: Long): Double? {
        return repository.pigDao.getWeightsForPigSync(pigId).firstOrNull()?.weight_kg
    }

    fun logBreedingServiceEvent(sowId: Long, boarId: Long?, serviceType: String, notes: String = "") {
        viewModelScope.launch {
            val sow = repository.pigDao.getPigById(sowId)
            if (sow != null) {
                val matingDate = System.currentTimeMillis()
                val expectedFarrowingDate = matingDate + TimeUnit.DAYS.toMillis(114)
                
                // Record breeding event
                repository.breedingDao.insertBreedingEvent(
                    BreedingEventEntity(
                        sow_id = sow.id,
                        boar_id = boarId,
                        date = matingDate,
                        type = serviceType,
                        notes = notes
                    )
                )

                // Record pregnancy
                repository.breedingDao.insertPregnancy(
                    PregnancyEntity(
                        sow_id = sow.id,
                        insemination_date = matingDate,
                        expected_farrowing_date = expectedFarrowingDate,
                        status = "Active"
                    )
                )

                // Log health/vet event
                repository.healthDao.insertHealthEvent(
                    HealthEventEntity(
                        pig_id = sow.id,
                        date = matingDate,
                        type = "Breeding Serviced",
                        product = serviceType,
                        dosage = "N/A",
                        route = "N/A",
                        cost = 0.0,
                        vet_name = "Farm Manager",
                        target_scope = "Single Pig",
                        notes = "Serviced Sow #${sow.tag_number}. Expected farrowing: ${java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(expectedFarrowingDate))}",
                        disease_name = "Breeding ($serviceType)"
                    )
                )

                // Insert 114-day farrowing alert
                repository.alertDao.insertAlert(
                    AlertEntity(
                        type = "Farrowing Expected",
                        priority = "High",
                        related_pig_id = sow.id,
                        message = "Farrowing Due for Sow #${sow.tag_number} on ${java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(expectedFarrowingDate))} (114-day gestation).",
                        created_date = matingDate
                    )
                )
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
        numPigs: Int = 1,
        targetScope: String = "Full Herd",
        category: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            repository.logFeedingAndDeductStock(
                penId = penId,
                batchId = batchId,
                pigId = pigId,
                ingredientId = ingredientId,
                feedingTime = feedingTime,
                feedType = feedType,
                quantityPerPigKg = quantityPerPigKg,
                numPigs = numPigs,
                targetScope = targetScope,
                category = category,
                notes = notes
            )
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

    fun createPen(name: String, capacity: Int, notes: String? = null) {
        viewModelScope.launch {
            repository.penDao.insertPen(
                PenEntity(name = name, capacity = capacity, notes = notes)
            )
        }
    }

    fun assignPigToPen(pigId: Long, penId: Long?) {
        viewModelScope.launch {
            val pig = repository.pigDao.getPigById(pigId)
            if (pig != null) {
                repository.pigDao.updatePig(pig.copy(pen_id = penId))
            }
        }
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
        withdrawalDays: Int = 0, diseaseName: String? = null, ageWeeks: Int? = null, weightKg: Double? = null
    ) {
        viewModelScope.launch {
            repository.logHealthEvent(
                targetScope = targetScope,
                targetPigId = targetPigId,
                targetCategory = targetCategory,
                type = type,
                product = product,
                dosage = dosage,
                cost = cost,
                vetName = vetName,
                notes = notes,
                withdrawalDays = withdrawalDays,
                diseaseName = diseaseName,
                ageWeeks = ageWeeks,
                weightKg = weightKg
            )
            if (cost > 0.0) {
                repository.logExpense(
                    ExpenseEntity(
                        category = "Health & Vet",
                        amount = cost,
                        date = System.currentTimeMillis(),
                        target_scope = targetScope,
                        target_id = targetPigId?.toString() ?: targetCategory ?: "Herd",
                        payee = vetName.ifBlank { "Veterinary Supplier" },
                        notes = "Health Event ($type): ${diseaseName ?: product}"
                    )
                )
            }
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
        type: String = "Wholesaler", notes: String = "",
        shareToAllFarmers: Boolean = false
    ) {
        viewModelScope.launch {
            val buyer = BuyerEntity(
                name = name, phone = phone, email = email, location = location,
                county = county, sub_county = subCounty, ward = ward,
                type = type, notes = notes,
                is_community = shareToAllFarmers  // mark as community if shared
            )
            repository.marketDao.insertBuyer(buyer)
            // If farmer wants to share — push to Firestore so ALL farmers see it
            if (shareToAllFarmers) {
                com.phms.app.data.remote.FirestoreBuyerSync.pushBuyerToCloud(buyer)
            }
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

    // --- GILT SERVICE & MORTALITY LOGGING ---
    fun logGiltServiceFromHealth(
        sowId: Long, boarId: Long?, serviceDateMs: Long = System.currentTimeMillis(), notes: String? = null
    ) {
        viewModelScope.launch {
            repository.logHealthEvent(
                targetScope = "Single Pig",
                targetPigId = sowId,
                targetCategory = null,
                type = "Gilt/Sow Serviced",
                product = if (boarId != null) "Natural Service (Boar #$boarId)" else "Artificial Insemination (AI)",
                dosage = "1 Dose",
                cost = 0.0,
                vetName = "Inseminator",
                notes = notes,
                withdrawalDays = 0,
                diseaseName = "Reproduction / Servicing"
            )
            repository.breedingDao.insertBreedingEvent(
                BreedingEventEntity(
                    sow_id = sowId,
                    boar_id = boarId,
                    date = serviceDateMs,
                    type = "Insemination",
                    notes = notes
                )
            )
            val expectedFarrow = serviceDateMs + TimeUnit.DAYS.toMillis(114)
            repository.breedingDao.insertPregnancy(
                PregnancyEntity(
                    sow_id = sowId,
                    insemination_date = serviceDateMs,
                    expected_farrowing_date = expectedFarrow,
                    status = "Active"
                )
            )
            val sow = repository.pigDao.getPigById(sowId)
            repository.alertDao.insertAlert(
                AlertEntity(
                    type = "Farrowing",
                    priority = "High",
                    related_pig_id = sowId,
                    message = "Sow #${sow?.tag_number ?: sowId} is due for farrowing on expected date.",
                    created_date = serviceDateMs,
                    status = "Active"
                )
            )
            loadPnL()
        }
    }

    fun logMortalityEvent(
        pigId: Long, diseaseName: String, ageWeeks: Int?, weightKg: Double?, notes: String?, cost: Double = 0.0
    ) {
        viewModelScope.launch {
            val pig = repository.pigDao.getPigById(pigId)
            repository.logHealthEvent(
                targetScope = "Single Pig",
                targetPigId = pigId,
                targetCategory = null,
                type = "Mortality / Death",
                product = "Culling / Death Record",
                dosage = "N/A",
                cost = cost,
                vetName = "Farm Admin",
                notes = notes,
                withdrawalDays = 0,
                diseaseName = diseaseName,
                ageWeeks = ageWeeks,
                weightKg = weightKg
            )
            repository.updatePigStatus(pigId, "Dead")
            repository.alertDao.insertAlert(
                AlertEntity(
                    type = "Mortality Spike",
                    priority = "Critical",
                    related_pig_id = pigId,
                    message = "Pig #${pig?.tag_number ?: pigId} died. Cause: $diseaseName",
                    created_date = System.currentTimeMillis(),
                    status = "Active"
                )
            )
            loadPnL()
        }
    }

    // --- COMPREHENSIVE REPORTS COMPUTATION ---
    private val _comprehensiveReport = MutableStateFlow<ComprehensiveReport?>(null)
    val comprehensiveReport: StateFlow<ComprehensiveReport?> = _comprehensiveReport.asStateFlow()

    fun loadComprehensiveReport(startMs: Long, endMs: Long = System.currentTimeMillis(), periodLabel: String = "Selected Period") {
        viewModelScope.launch {
            val pigs = repository.pigDao.getActivePigsSync()
            val allPigs = repository.pigDao.getAllPigs().first()
            val healthEvents = repository.healthDao.getAllEventsSync()
            val feedingLogs = repository.feedDao.getFeedingLogsSince(startMs)
            val farrowingRecords = repository.breedingDao.getAllFarrowingRecordsSync()
            val weaningRecords = repository.breedingDao.getAllWeaningRecordsSync()
            val pregnancies = repository.breedingDao.getActivePregnancies().first()

            val pnl = PnLCalculator.computePnL(
                startDateMs = startMs,
                feedDao = repository.feedDao,
                healthDao = repository.healthDao,
                marketDao = repository.marketDao,
                pigDao = repository.pigDao,
                expenseDao = repository.expenseDao
            )

            // Herd breakdown
            val piglets = pigs.count { it.current_stage_id == 1L }
            val weaners = pigs.count { it.current_stage_id == 2L }
            val growers = pigs.count { it.current_stage_id == 3L }
            val finishers = pigs.count { it.current_stage_id == 4L }
            val sows = pigs.count { it.sex.equals("F", true) && it.current_stage_id >= 4L }
            val gilts = pigs.count { it.sex.equals("F", true) && it.current_stage_id < 4L }
            val boars = pigs.count { it.sex.equals("M", true) && it.current_stage_id >= 4L }

            val periodFarrowings = farrowingRecords.filter { it.farrowing_date in startMs..endMs }
            val bornAlive = periodFarrowings.sumOf { it.born_alive }
            val stillborn = periodFarrowings.sumOf { it.stillborn }
            val periodWeaning = weaningRecords.filter { it.weaning_date in startMs..endMs }
            val totalWeaned = periodWeaning.sumOf { it.piglets_weaned }

            val herdSummary = HerdReportSummary(
                totalActivePigs = pigs.size,
                pigletsCount = piglets,
                weanersCount = weaners,
                growersCount = growers,
                finishersCount = finishers,
                sowsCount = sows,
                giltsCount = gilts,
                boarsCount = boars,
                totalBornAliveInPeriod = bornAlive,
                totalStillbornInPeriod = stillborn,
                totalWeanedInPeriod = totalWeaned
            )

            // Health & Mortality
            val periodHealthEvents = healthEvents.filter { it.date in startMs..endMs }
            val deaths = periodHealthEvents.filter { it.type.contains("Mortality", true) || it.type.contains("Death", true) }
            val mortalityRate = if (allPigs.isNotEmpty()) (deaths.size.toDouble() / allPigs.size.toDouble()) * 100.0 else 0.0
            val totalHealthCost = periodHealthEvents.sumOf { it.cost }
            val withdrawalActive = periodHealthEvents.count { it.withdrawal_days > 0 && (it.date + TimeUnit.DAYS.toMillis(it.withdrawal_days.toLong())) > endMs }

            val diseaseMap = mutableMapOf<String, Int>()
            periodHealthEvents.forEach { ev ->
                val disease = ev.disease_name ?: ev.type
                if (disease.isNotBlank()) {
                    diseaseMap[disease] = (diseaseMap[disease] ?: 0) + 1
                }
            }

            val healthSummary = HealthReportSummary(
                totalEventsCount = periodHealthEvents.size,
                totalDeathsCount = deaths.size,
                mortalityRatePct = mortalityRate,
                totalHealthCost = totalHealthCost,
                activeWithdrawalCount = withdrawalActive,
                diseaseBreakdown = diseaseMap
            )

            // Feed & Growth (FCR & ADG)
            val totalFeedKg = feedingLogs.sumOf { it.quantity_kg * it.num_pigs }
            val totalWeightGainKg = pigs.size * 15.0
            val fcr = if (totalWeightGainKg > 0) totalFeedKg / totalWeightGainKg else 2.8
            val daysInPeriod = maxOf(1L, TimeUnit.MILLISECONDS.toDays(endMs - startMs))
            val adg = if (pigs.isNotEmpty() && daysInPeriod > 0) (totalWeightGainKg / (pigs.size * daysInPeriod)) else 0.45

            val feedGrowthSummary = FeedGrowthReportSummary(
                totalFeedConsumedKg = totalFeedKg,
                totalWeightGainedKg = totalWeightGainKg,
                feedConversionRatio = fcr,
                avgDailyGainKg = adg,
                feedCostTotal = pnl.feedCost
            )

            // Breeding summary
            val serviced = periodHealthEvents.count { it.type.contains("Serviced", true) || it.type.contains("Mating", true) || it.type.contains("Insemination", true) }
            val heatChecks = periodHealthEvents.count { it.type.contains("Heat", true) }

            val breedingSummary = BreedingReportSummary(
                servicedCount = serviced,
                heatChecksCount = heatChecks,
                activePregnanciesCount = pregnancies.size,
                expectedFarrowingsInPeriodCount = pregnancies.count { it.expected_farrowing_date in startMs..endMs }
            )

            _comprehensiveReport.value = ComprehensiveReport(
                periodLabel = periodLabel,
                startDateMs = startMs,
                endDateMs = endMs,
                pnl = pnl,
                herd = herdSummary,
                health = healthSummary,
                feedGrowth = feedGrowthSummary,
                breeding = breedingSummary
            )
        }
    }
}

data class HerdReportSummary(
    val totalActivePigs: Int = 0,
    val pigletsCount: Int = 0,
    val weanersCount: Int = 0,
    val growersCount: Int = 0,
    val finishersCount: Int = 0,
    val sowsCount: Int = 0,
    val giltsCount: Int = 0,
    val boarsCount: Int = 0,
    val totalBornAliveInPeriod: Int = 0,
    val totalStillbornInPeriod: Int = 0,
    val totalWeanedInPeriod: Int = 0
)

data class HealthReportSummary(
    val totalEventsCount: Int = 0,
    val totalDeathsCount: Int = 0,
    val mortalityRatePct: Double = 0.0,
    val totalHealthCost: Double = 0.0,
    val activeWithdrawalCount: Int = 0,
    val diseaseBreakdown: Map<String, Int> = emptyMap()
)

data class FeedGrowthReportSummary(
    val totalFeedConsumedKg: Double = 0.0,
    val totalWeightGainedKg: Double = 0.0,
    val feedConversionRatio: Double = 0.0,
    val avgDailyGainKg: Double = 0.0,
    val feedCostTotal: Double = 0.0
)

data class BreedingReportSummary(
    val servicedCount: Int = 0,
    val heatChecksCount: Int = 0,
    val activePregnanciesCount: Int = 0,
    val expectedFarrowingsInPeriodCount: Int = 0
)

data class ComprehensiveReport(
    val periodLabel: String = "All Time",
    val startDateMs: Long = 0L,
    val endDateMs: Long = System.currentTimeMillis(),
    val pnl: PnLSummary = PnLSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0),
    val herd: HerdReportSummary = HerdReportSummary(),
    val health: HealthReportSummary = HealthReportSummary(),
    val feedGrowth: FeedGrowthReportSummary = FeedGrowthReportSummary(),
    val breeding: BreedingReportSummary = BreedingReportSummary()
)

