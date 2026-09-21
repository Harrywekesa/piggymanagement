package com.phms.app.data.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val latestVersion: String = "",
    val currentVersion: String = "v${com.phms.app.BuildConfig.VERSION_NAME}",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val releaseUrl: String = "",
    val isChecking: Boolean = false,
    val checkError: String? = null
)

object GitHubUpdateChecker {
    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/Harrywekesa/piggymanagement/releases/latest"
    val CURRENT_VERSION: String get() = com.phms.app.BuildConfig.VERSION_NAME

    suspend fun checkForUpdates(): AppUpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASES_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "PHMS-Android-App")
            }

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)

                val rawTag = json.optString("tag_name", "").trim()
                val tagName = rawTag.removePrefix("v").removePrefix("V")
                val releaseNotes = json.optString("body", "New features and performance improvements available.")
                val releaseUrl = json.optString("html_url", "https://github.com/Harrywekesa/piggymanagement/releases")

                var downloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }
                if (downloadUrl.isBlank()) {
                    downloadUrl = releaseUrl
                }

                val isNewer = isVersionNewer(CURRENT_VERSION, tagName)

                AppUpdateInfo(
                    isUpdateAvailable = isNewer,
                    latestVersion = if (rawTag.isNotBlank()) rawTag else "v$tagName",
                    currentVersion = "v$CURRENT_VERSION",
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    releaseUrl = releaseUrl,
                    isChecking = false,
                    checkError = null
                )
            } else {
                AppUpdateInfo(currentVersion = "v$CURRENT_VERSION", isChecking = false, checkError = "HTTP ${connection.responseCode}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AppUpdateInfo(currentVersion = "v$CURRENT_VERSION", isChecking = false, checkError = e.localizedMessage)
        }
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        if (latest.isBlank()) return false
        val cleanCurr = current.removePrefix("v").removePrefix("V").trim()
        val cleanLate = latest.removePrefix("v").removePrefix("V").trim()
        val currParts = cleanCurr.split(".").mapNotNull { it.toIntOrNull() }
        val lateParts = cleanLate.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(currParts.size, lateParts.size)
        for (i in 0 until length) {
            val currVal = currParts.getOrElse(i) { 0 }
            val lateVal = lateParts.getOrElse(i) { 0 }
            if (lateVal > currVal) return true
            if (currVal > lateVal) return false
        }
        return false
    }

    fun openUpdateLink(context: Context, updateInfo: AppUpdateInfo) {
        val targetUrl = if (updateInfo.downloadUrl.isNotBlank()) updateInfo.downloadUrl else updateInfo.releaseUrl
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
