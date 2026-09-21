package com.phms.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val isUpdateAvailable: Boolean
)

data class InstallTelemetry(
    val installDate: String,
    val totalAppLaunches: Int,
    val currentVersion: String,
    val deviceModel: String
)

class UpdateManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("phms_insights_prefs", Context.MODE_PRIVATE)

    private val _updateState = MutableStateFlow<AppReleaseInfo?>(null)
    val updateState: StateFlow<AppReleaseInfo?> = _updateState

    private val _telemetryState = MutableStateFlow<InstallTelemetry?>(null)
    val telemetryState: StateFlow<InstallTelemetry?> = _telemetryState

    init {
        recordLaunch()
    }

    private fun recordLaunch() {
        val launches = prefs.getInt("launch_count", 0) + 1
        val firstLaunch = prefs.getString("first_launch_date", null) ?: System.currentTimeMillis().toString()
        if (!prefs.contains("first_launch_date")) {
            prefs.edit().putString("first_launch_date", firstLaunch).apply()
        }
        prefs.edit().putInt("launch_count", launches).apply()

        val info = InstallTelemetry(
            installDate = firstLaunch,
            totalAppLaunches = launches,
            currentVersion = "v${com.phms.app.BuildConfig.VERSION_NAME}",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        )
        _telemetryState.value = info
    }

    suspend fun checkForGitHubUpdates() = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/Harrywekesa/piggymanagement/releases/latest")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "PHMS-Pig-Management-App")
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val latestTag = json.optString("tag_name", "v${com.phms.app.BuildConfig.VERSION_NAME}")
                val releaseBody = json.optString("body", "New features & bug fixes.")
                val htmlUrl = json.optString("html_url", "https://github.com/Harrywekesa/piggymanagement/releases")

                val currentVersion = "v${com.phms.app.BuildConfig.VERSION_NAME}"
                val isNewer = latestTag != currentVersion && latestTag.isNotBlank()

                _updateState.value = AppReleaseInfo(
                    version = latestTag,
                    downloadUrl = htmlUrl,
                    releaseNotes = releaseBody,
                    isUpdateAvailable = isNewer
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openDownloadPage(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
