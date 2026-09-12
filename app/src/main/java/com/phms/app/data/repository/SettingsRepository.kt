package com.phms.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FarmSettings(
    val farmName: String = "My Pig Farm",
    val farmerName: String = "",
    val farmLocation: String = "",
    val currencySymbol: String = "KSh",
    val alertTimeHour: Int = 6,
    val alertTimeMinute: Int = 0,
    val snoozeDurationDays: Int = 1,
    val alertVaccination: Boolean = true,
    val alertFarrowing: Boolean = true,
    val alertLowFeed: Boolean = true,
    val alertPromotion: Boolean = true,
    val alertWithdrawal: Boolean = true,
    val alertOvercrowding: Boolean = true,
    val isOnboarded: Boolean = false,
    val showTourOnFirstOpen: Boolean = true
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("phms_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<FarmSettings> = _settings

    private fun load() = FarmSettings(
        farmName = prefs.getString("farm_name", "My Pig Farm") ?: "My Pig Farm",
        farmerName = prefs.getString("farmer_name", "") ?: "",
        farmLocation = prefs.getString("farm_location", "") ?: "",
        currencySymbol = prefs.getString("currency_symbol", "KSh") ?: "KSh",
        alertTimeHour = prefs.getInt("alert_hour", 6),
        alertTimeMinute = prefs.getInt("alert_minute", 0),
        snoozeDurationDays = prefs.getInt("snooze_days", 1),
        alertVaccination = prefs.getBoolean("alert_vaccination", true),
        alertFarrowing = prefs.getBoolean("alert_farrowing", true),
        alertLowFeed = prefs.getBoolean("alert_low_feed", true),
        alertPromotion = prefs.getBoolean("alert_promotion", true),
        alertWithdrawal = prefs.getBoolean("alert_withdrawal", true),
        alertOvercrowding = prefs.getBoolean("alert_overcrowding", true),
        isOnboarded = prefs.getBoolean("is_onboarded", false),
        showTourOnFirstOpen = prefs.getBoolean("show_tour_on_first_open", true)
    )

    fun save(s: FarmSettings) {
        prefs.edit().apply {
            putString("farm_name", s.farmName)
            putString("farmer_name", s.farmerName)
            putString("farm_location", s.farmLocation)
            putString("currency_symbol", s.currencySymbol)
            putInt("alert_hour", s.alertTimeHour)
            putInt("alert_minute", s.alertTimeMinute)
            putInt("snooze_days", s.snoozeDurationDays)
            putBoolean("alert_vaccination", s.alertVaccination)
            putBoolean("alert_farrowing", s.alertFarrowing)
            putBoolean("alert_low_feed", s.alertLowFeed)
            putBoolean("alert_promotion", s.alertPromotion)
            putBoolean("alert_withdrawal", s.alertWithdrawal)
            putBoolean("alert_overcrowding", s.alertOvercrowding)
            putBoolean("is_onboarded", s.isOnboarded)
            putBoolean("show_tour_on_first_open", s.showTourOnFirstOpen)
        }.apply()
        _settings.value = s
    }

    fun setOnboarded(onboarded: Boolean) {
        save(get().copy(isOnboarded = onboarded))
    }

    fun get() = _settings.value
}
