package com.dnavarro.poskmp.data.source.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLQueryComponent
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class GoogleCustomSearchResponse(
    val items: List<GoogleCustomSearchItem>? = null,
    val error: GoogleCustomSearchError? = null
)

@Serializable
data class GoogleCustomSearchItem(
    val title: String? = null,
    val snippet: String? = null,
    val link: String? = null
)

@Serializable
data class GoogleCustomSearchError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

sealed class GoogleCustomSearchResult {
    data class Success(val snippets: List<String>) : GoogleCustomSearchResult()
    data class Failure(val errorMessage: String, val httpCode: Int? = null) : GoogleCustomSearchResult()
}

interface GoogleCustomSearchService {
    suspend fun search(
        query: String,
        apiKey: String,
        searchEngineId: String
    ): GoogleCustomSearchResult
}

class GoogleCustomSearchServiceImpl(
    private val httpClient: HttpClient = createDefaultClient()
) : GoogleCustomSearchService {

    companion object {
        fun createDefaultClient(): HttpClient {
            return HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = false
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    })
                }
            }
        }

        private val jsonParser = Json {
            prettyPrint = false
            isLenient = true
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }

    override suspend fun search(
        query: String,
        apiKey: String,
        searchEngineId: String
    ): GoogleCustomSearchResult {
        val trimmedQuery = query.trim()
        val cleanKey = apiKey.trim()
        val cleanCx = searchEngineId.trim()

        if (trimmedQuery.isBlank()) {
            return GoogleCustomSearchResult.Failure("Consulta vacía.")
        }
        if (cleanKey.isBlank() || cleanCx.isBlank()) {
            return GoogleCustomSearchResult.Failure("Clave de API o ID de buscador (cx) no configurados.")
        }

        println("[PoSKMP-Search] ==================================================")
        println("[PoSKMP-Search] Consultando Google Custom Search JSON API")
        println("[PoSKMP-Search] Query: '$trimmedQuery'")
        println("[PoSKMP-Search] Search Engine ID (cx): '$cleanCx'")

        return try {
            val encodedQuery = trimmedQuery.encodeURLQueryComponent()
            val url = "https://www.googleapis.com/customsearch/v1?key=$cleanKey&cx=$cleanCx&q=$encodedQuery&hl=es&gl=mx&num=5"

            val response = withTimeoutOrNull(8000L.milliseconds) {
                httpClient.get(url)
            }

            if (response == null) {
                println("[PoSKMP-Search] ⏱️ Timeout esperando respuesta de Google Custom Search (8s)")
                return GoogleCustomSearchResult.Failure("Tiempo de espera agotado al consultar Google Search.")
            }

            val status = response.status.value
            val rawBody = response.bodyAsText()

            println("[PoSKMP-Search] Código HTTP: $status")

            if (!response.status.isSuccess()) {
                val errorDetail = try {
                    jsonParser.decodeFromString<GoogleCustomSearchResponse>(rawBody).error
                } catch (_: Exception) {
                    null
                }
                val msg = errorDetail?.message ?: "Error HTTP $status de Google Custom Search"
                println("[PoSKMP-Search] ❌ Error: $msg")
                return GoogleCustomSearchResult.Failure(msg, status)
            }

            val parsed = jsonParser.decodeFromString<GoogleCustomSearchResponse>(rawBody)
            val items = parsed.items.orEmpty()

            if (items.isEmpty()) {
                println("[PoSKMP-Search] ℹ️ Sin resultados en Google Search para '$trimmedQuery'.")
                return GoogleCustomSearchResult.Success(emptyList())
            }

            val snippets = items.mapNotNull { item ->
                val title = item.title?.trim().orEmpty()
                val snippet = item.snippet?.trim().orEmpty()
                if (snippet.isNotBlank()) {
                    if (title.isNotBlank()) "$title: $snippet" else snippet
                } else title.ifBlank { null }
            }

            println("[PoSKMP-Search] ✅ ${snippets.size} resultados obtenidos de Google Search:")
            snippets.forEachIndexed { i, s ->
                println("[PoSKMP-Search]   [$i] ${s.take(100)}...")
            }
            println("[PoSKMP-Search] ==================================================")

            GoogleCustomSearchResult.Success(snippets)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("[PoSKMP-Search] 💥 Excepción al consultar Google Search: ${e::class.simpleName}: ${e.message}")
            GoogleCustomSearchResult.Failure("Error de conexión con Google Search: ${e.message ?: e::class.simpleName}")
        }
    }
}
