package com.dnavarro.poskmp.data.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

class UpdateRepository {

    suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/danielnavarrowo/PoSKMP/releases/latest"
            val url = URI.create(apiUrl).toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "PoSKMP-App")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // No releases published yet
                return@withContext UpdateCheckResult.UpToDate
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateCheckResult.Error("HTTP $responseCode: ${connection.responseMessage}")
            }

            val jsonContent = connection.inputStream.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
            connection.disconnect()

            val release = parseReleaseJson(jsonContent)
                ?: return@withContext UpdateCheckResult.Error("Formato de respuesta inválido")

            val currentVersion = PlatformUpdater.getAppVersion()
            if (isNewerVersion(remoteVersion = release.cleanVersion, currentVersion = currentVersion)) {
                val matchingAsset = PlatformUpdater.findMatchingAsset(release.assets)
                UpdateCheckResult.UpdateAvailable(
                    releaseInfo = release,
                    matchingAsset = matchingAsset
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Error al verificar actualizaciones")
        }
    }

    suspend fun downloadAndInstall(
        asset: ReleaseAsset,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> {
        return PlatformUpdater.downloadAndInstall(asset, onProgress)
    }

    fun getCurrentVersion(): String = PlatformUpdater.getAppVersion()

    internal fun parseVersion(version: String): List<Int> {
        val clean = version.trim().trimStart('v', 'V').substringBefore('-').substringBefore('+')
        return clean.split('.').mapNotNull { it.toIntOrNull() }
    }

    internal fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        val remoteParts = parseVersion(remoteVersion)
        val currentParts = parseVersion(currentVersion)
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun parseReleaseJson(json: String): ReleaseInfo? {
        try {
            val tagName = extractJsonString(json, "tag_name") ?: return null
            val name = extractJsonString(json, "name") ?: tagName
            val body = extractJsonString(json, "body") ?: ""
            val htmlUrl = extractJsonString(json, "html_url") ?: ""
            val publishedAt = extractJsonString(json, "published_at") ?: ""

            val assets = mutableListOf<ReleaseAsset>()
            val assetsSection = extractJsonArray(json, "assets")
            if (assetsSection != null) {
                val assetObjects = splitJsonObjectsInArray(assetsSection)
                for (assetJson in assetObjects) {
                    val assetName = extractJsonString(assetJson, "name") ?: continue
                    val downloadUrl = extractJsonString(assetJson, "browser_download_url") ?: continue
                    val size = extractJsonLong(assetJson, "size") ?: 0L
                    assets.add(
                        ReleaseAsset(
                            name = assetName,
                            downloadUrl = downloadUrl,
                            sizeBytes = size
                        )
                    )
                }
            }

            return ReleaseInfo(
                tagName = tagName,
                name = name,
                releaseNotes = body,
                htmlUrl = htmlUrl,
                publishedAt = publishedAt,
                assets = assets
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"")
        val match = pattern.find(json) ?: return null
        val raw = match.groupValues[1]
        return unescapeJson(raw)
    }

    @Suppress("SameParameterValue")
    private fun extractJsonLong(json: String, key: String): Long? {
        val pattern = Regex("\"$key\"\\s*:\\s*(\\d+)")
        val match = pattern.find(json) ?: return null
        return match.groupValues[1].toLongOrNull()
    }

    @Suppress("SameParameterValue")
    private fun extractJsonArray(json: String, key: String): String? {
        val keyIdx = json.indexOf("\"$key\"")
        if (keyIdx == -1) return null
        val startBracket = json.indexOf('[', keyIdx)
        if (startBracket == -1) return null

        var depth = 0
        var insideString = false
        var isEscaped = false

        for (i in startBracket until json.length) {
            val char = json[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (char == '\\') {
                isEscaped = true
                continue
            }
            if (char == '"') {
                insideString = !insideString
                continue
            }
            if (!insideString) {
                if (char == '[') depth++
                else if (char == ']') {
                    depth--
                    if (depth == 0) {
                        return json.substring(startBracket, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun splitJsonObjectsInArray(arrayJson: String): List<String> {
        val objects = mutableListOf<String>()
        var depth = 0
        var insideString = false
        var isEscaped = false
        var objStart = -1

        for (i in arrayJson.indices) {
            val char = arrayJson[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (char == '\\') {
                isEscaped = true
                continue
            }
            if (char == '"') {
                insideString = !insideString
                continue
            }
            if (!insideString) {
                if (char == '{') {
                    if (depth == 0) objStart = i
                    depth++
                } else if (char == '}') {
                    depth--
                    if (depth == 0 && objStart != -1) {
                        objects.add(arrayJson.substring(objStart, i + 1))
                        objStart = -1
                    }
                }
            }
        }
        return objects
    }

    private fun unescapeJson(input: String): String {
        return input
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\/", "/")
    }
}
