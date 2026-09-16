package com.dnavarro.poskmp.data.source.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val tools: List<GeminiTool>? = null
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiTool(
    val googleSearch: GeminiGoogleSearch? = null
)

@Serializable
class GeminiGoogleSearch

@Serializable
data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiErrorResponse? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

@Serializable
data class GeminiErrorResponse(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

@Serializable
data class GeminiCategoryAnalysis(
    val category: String,
    val confidence: Double = 0.9,
    val productDescription: String? = null,
    val alternativeCategories: List<String> = emptyList()
)

data class GeminiCategoryResult(
    val suggestedCategory: String,
    val alternativeCategories: List<String> = emptyList(),
    val sourceModel: String = ""
)

sealed class GeminiInferenceResult {
    data class Success(val result: GeminiCategoryResult) : GeminiInferenceResult()
    data class Failure(
        val errorMessage: String,
        val httpCode: Int? = null,
        val isApiKeyError: Boolean = false
    ) : GeminiInferenceResult()
}

interface GeminiCategoryService {
    suspend fun inferCategory(
        productName: String,
        existingCategories: List<String> = emptyList(),
        apiKey: String,
        model: String = DEFAULT_MODEL
    ): GeminiCategoryResult?

    suspend fun inferCategoryDetailed(
        productName: String,
        existingCategories: List<String> = emptyList(),
        apiKey: String,
        model: String = DEFAULT_MODEL,
        googleSearchEngineId: String = "",
        googleSearchApiKey: String = ""
    ): GeminiInferenceResult

    companion object {
        const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
        val FALLBACK_MODELS = listOf("gemini-3.5-flash", "gemini-3.6-flash", "gemini-flash-latest", "gemini-flash-lite-latest")
    }
}

class GeminiCategoryServiceImpl(
    private val httpClient: HttpClient = createDefaultClient(),
    private val googleCustomSearchService: GoogleCustomSearchService = GoogleCustomSearchServiceImpl(httpClient)
) : GeminiCategoryService {

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

    private val cache = mutableMapOf<String, GeminiCategoryResult>()

    override suspend fun inferCategory(
        productName: String,
        existingCategories: List<String>,
        apiKey: String,
        model: String
    ): GeminiCategoryResult? {
        return when (val detailed = inferCategoryDetailed(productName, existingCategories, apiKey, model)) {
            is GeminiInferenceResult.Success -> detailed.result
            is GeminiInferenceResult.Failure -> null
        }
    }

    override suspend fun inferCategoryDetailed(
        productName: String,
        existingCategories: List<String>,
        apiKey: String,
        model: String,
        googleSearchEngineId: String,
        googleSearchApiKey: String
    ): GeminiInferenceResult {
        val trimmedName = productName.trim()
        val cleanKey = apiKey.trim()

        if (trimmedName.length < 2) {
            println("[PoSKMP-Gemini] ⚠️ Nombre demasiado corto ('$trimmedName'). Mínimo 2 caracteres requeridos.")
            return GeminiInferenceResult.Failure("El nombre del producto debe tener al menos 2 caracteres.")
        }
        if (cleanKey.isBlank()) {
            println("[PoSKMP-Gemini] ⚠️ API Key vacía.")
            return GeminiInferenceResult.Failure("Clave de API de Gemini no configurada.", isApiKeyError = true)
        }

        val maskedKey = if (cleanKey.length > 10) {
            "${cleanKey.take(6)}...${cleanKey.takeLast(4)} (longitud: ${cleanKey.length})"
        } else {
            "*** (longitud: ${cleanKey.length})"
        }

        println("[PoSKMP-Gemini] ==================================================")
        println("[PoSKMP-Gemini] Consultando categoría con Gemini AI")
        println("[PoSKMP-Gemini] Producto: '$trimmedName'")
        println("[PoSKMP-Gemini] API Key: $maskedKey")
        if (!cleanKey.startsWith("AIzaSy") && !cleanKey.startsWith("AQ.")) {
            println("[PoSKMP-Gemini] ⚠️ ADVERTENCIA: La clave no comienza con 'AQ.' ni con 'AIzaSy'. Las claves de Google AI Studio / Gemini API suelen iniciar con 'AQ.' o 'AIzaSy'.")
        }

        val cacheKey = "${trimmedName.lowercase()}__${existingCategories.sorted().joinToString(",")}__cx:$googleSearchEngineId"
        cache[cacheKey]?.let {
            println("[PoSKMP-Gemini] ⚡ Resultado obtenido directamente de caché local: '${it.suggestedCategory}'")
            return GeminiInferenceResult.Success(it)
        }

        val validExisting = existingCategories
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("Sin categoría", ignoreCase = true) && !it.equals("Sin categoria", ignoreCase = true) }
            .distinct()

        val categoriesContext = if (validExisting.isNotEmpty()) {
            "Categorías existentes en la tienda:\n" +
                validExisting.joinToString(separator = ", ") { "\"$it\"" } + "\n" +
                "Si el producto coincide o encaja de forma razonable con alguna de estas categorías existentes, PRIORIZA y selecciona esa misma categoría para evitar duplicados o sinónimos."
        } else {
            "No hay categorías registradas previamente en la tienda. Sugiere un nombre estándar y conciso en español (ej. Botanas, Bebidas, Galletas, Lácteos, Dulcería, Abarrotes, Higiene, Limpieza)."
        }

        // 1. Google Custom Search integration (Option A)
        var searchSnippets: List<String> = emptyList()
        val cleanCx = googleSearchEngineId.trim()
        val cleanSearchKey = googleSearchApiKey.trim().ifBlank { cleanKey }

        if (cleanCx.isNotBlank()) {
            println("[PoSKMP-Gemini] 🌐 Consultando Google Custom Search API (cx: $cleanCx)...")
            when (val searchRes = googleCustomSearchService.search(trimmedName, cleanSearchKey, cleanCx)) {
                is GoogleCustomSearchResult.Success -> {
                    searchSnippets = searchRes.snippets
                    if (searchSnippets.isNotEmpty()) {
                        println("[PoSKMP-Gemini] ✅ Google Search aportó ${searchSnippets.size} fragmentos de información real.")
                    }
                }
                is GoogleCustomSearchResult.Failure -> {
                    println("[PoSKMP-Gemini] ⚠️ Google Custom Search retornó error: ${searchRes.errorMessage}")
                }
            }
        }

        val searchContextBlock = if (searchSnippets.isNotEmpty()) {
            "RESULTADOS REALES DE BÚSQUEDA WEB EN GOOGLE SOBRE EL PRODUCTO:\n" +
                searchSnippets.joinToString("\n- ", prefix = "- ") + "\n\n" +
                "Instrucción crítica: BÁSATE ESTRICTAMENTE en estos resultados de búsqueda de Google para identificar la categoría exacta y descripción de \"$trimmedName\". NO inventes nada ajeno a la búsqueda."
        } else {
            "Instrucciones obligatorias:\n1. Investiga o deduce la marca, fabricante y tipo de producto que es \"$trimmedName\" (especialmente para marcas comerciales como Bokachitos, Bolichik, Toffino, etc.)."
        }

        val promptText = """
Eres un asistente experto de Punto de Venta (POS) para abarrotes, dulcerías y tiendas minoristas en Latinoamérica.
Tu labor es identificar qué es el producto comercial indicado a continuación y clasificarlo en la categoría más adecuada:
Producto: "$trimmedName"

$searchContextBlock

$categoriesContext

Instrucciones de salida:
Responde ÚNICAMENTE con un objeto JSON válido con la siguiente estructura, sin texto explicativo adicional:
{
  "category": "Nombre de la categoría principal",
  "alternativeCategories": ["Categoría alternativa 1", "Categoría alternativa 2"]
}
        """.trimIndent()

        val requestPayloadWithTools = GeminiGenerateContentRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = promptText))
                )
            ),
            tools = listOf(
                GeminiTool(googleSearch = GeminiGoogleSearch())
            )
        )

        val requestPayloadWithoutTools = GeminiGenerateContentRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = promptText))
                )
            )
        )

        // If we already have real Google search snippets, we do NOT need the paid Grounding tool!
        val hasCustomSearchSnippets = searchSnippets.isNotEmpty()
        val initialPayload = if (hasCustomSearchSnippets) requestPayloadWithoutTools else requestPayloadWithTools

        // Normalize deprecated models (1.5, 2.0) to Gemini 3.5
        val baseModel = if (model.contains("1.5") || model.contains("2.0")) {
            GeminiCategoryService.DEFAULT_MODEL
        } else {
            model
        }

        val modelsToTry = listOf(baseModel) + GeminiCategoryService.FALLBACK_MODELS.filter { it != baseModel }

        var lastErrorMessage = "No se pudo obtener sugerencia de categoría de Gemini."
        var lastHttpCode: Int? = null

        for (targetModel in modelsToTry) {
            println("[PoSKMP-Gemini] Probando modelo: $targetModel...")
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$cleanKey"
                var response = withTimeoutOrNull(10000L.milliseconds) {
                    httpClient.post(url) {
                        contentType(ContentType.Application.Json)
                        header("User-Agent", "PoSKMP/1.0")
                        setBody(initialPayload)
                    }
                }

                if (response == null) {
                    println("[PoSKMP-Gemini] ⏱️ Timeout esperando respuesta de $targetModel (10s)")
                    lastErrorMessage = "Tiempo de espera agotado al conectar con Gemini ($targetModel)."
                    continue
                }

                var status = response.status.value
                var rawBody = response.bodyAsText()

                println("[PoSKMP-Gemini] Código de respuesta HTTP: $status")

                // If Google returns 429 on the Search Grounding tool (free tier limitation), retry without tools
                if (status == 429) {
                    println("[PoSKMP-Gemini] ⚠️ Cuota de Google Search Grounding no disponible o excedida (HTTP 429).")
                    println("[PoSKMP-Gemini] 🔄 Reintentando con conocimiento nativo de $targetModel (sin herramienta de búsqueda)...")
                    val retryResponse = withTimeoutOrNull(8000L.milliseconds) {
                        httpClient.post(url) {
                            contentType(ContentType.Application.Json)
                            header("User-Agent", "PoSKMP/1.0")
                            setBody(requestPayloadWithoutTools)
                        }
                    }
                    if (retryResponse != null) {
                        response = retryResponse
                        status = retryResponse.status.value
                        rawBody = retryResponse.bodyAsText()
                        println("[PoSKMP-Gemini] Código de respuesta reintento HTTP: $status")
                    }
                }

                println("[PoSKMP-Gemini] Cuerpo de respuesta: $rawBody")

                if (status == 400) {
                    val errorDetail = try {
                        jsonParser.decodeFromString<GeminiGenerateContentResponse>(rawBody).error
                    } catch (_: Exception) {
                        null
                    }
                    val msg = errorDetail?.message ?: "Solicitud rechazada (código 400)"
                    val isKeyError = msg.contains("API key", ignoreCase = true) || errorDetail?.status == "INVALID_ARGUMENT"
                    val userFriendlyMsg = if (isKeyError) {
                        "Clave de API de Gemini no válida (HTTP 400). Verifica que tu clave de Google AI Studio sea correcta (suele iniciar con 'AQ.' o 'AIzaSy')."
                    } else {
                        "Error en la solicitud a Gemini (400): $msg"
                    }
                    println("[PoSKMP-Gemini] ❌ Error HTTP 400 detectado: $userFriendlyMsg")
                    return GeminiInferenceResult.Failure(
                        errorMessage = userFriendlyMsg,
                        httpCode = 400,
                        isApiKeyError = isKeyError
                    )
                }

                if (status == 403) {
                    val errorDetail = try {
                        jsonParser.decodeFromString<GeminiGenerateContentResponse>(rawBody).error
                    } catch (_: Exception) {
                        null
                    }
                    val msg = errorDetail?.message ?: "Acceso denegado (HTTP 403)"
                    println("[PoSKMP-Gemini] ❌ Permiso denegado: $msg")
                    return GeminiInferenceResult.Failure(
                        errorMessage = "Acceso denegado (HTTP 403). Verifica los permisos de tu API Key: $msg",
                        httpCode = 403,
                        isApiKeyError = true
                    )
                }

                if (status == 429) {
                    println("[PoSKMP-Gemini] ❌ Límite de cuota alcanzado en Gemini API (HTTP 429)")
                    return GeminiInferenceResult.Failure(
                        errorMessage = "Límite de cuota alcanzado en Gemini API (HTTP 429). Espera un momento antes de reintentar.",
                        httpCode = 429
                    )
                }

                if (status == 404) {
                    println("[PoSKMP-Gemini] ℹ️ Modelo '$targetModel' no encontrado o no disponible (HTTP 404). Intentando siguiente modelo en lista de respaldo...")
                    lastHttpCode = 404
                    lastErrorMessage = "Modelo '$targetModel' no disponible en la API v1beta."
                    continue
                }

                if (!response.status.isSuccess()) {
                    println("[PoSKMP-Gemini] ❌ Error HTTP $status no controlado.")
                    lastHttpCode = status
                    lastErrorMessage = "Error de Gemini API (HTTP $status)."
                    continue
                }

                val body = jsonParser.decodeFromString<GeminiGenerateContentResponse>(rawBody)
                val candidateText = body.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text

                if (candidateText.isNullOrBlank()) {
                    println("[PoSKMP-Gemini] ⚠️ El modelo $targetModel no devolvió texto en candidates.")
                    lastErrorMessage = "Gemini no devolvió texto de respuesta para el producto."
                    continue
                }

                println("[PoSKMP-Gemini] ✅ Respuesta recibida de $targetModel:")
                println("[PoSKMP-Gemini] $candidateText")

                val parsed = parseGeminiResponse(candidateText, validExisting, targetModel)
                if (parsed != null) {
                    val finalModel = if (hasCustomSearchSnippets) "Google Search + $targetModel" else targetModel
                    val finalResult = parsed.copy(sourceModel = finalModel)
                    println("[PoSKMP-Gemini] 🎉 Categoría identificada: '${finalResult.suggestedCategory}' modelo: ${finalResult.sourceModel})")
                    if (finalResult.alternativeCategories.isNotEmpty()) {
                        println("[PoSKMP-Gemini] ℹ️ Alternativas: ${finalResult.alternativeCategories}")
                    }
                    cache[cacheKey] = finalResult
                    return GeminiInferenceResult.Success(finalResult)
                } else {
                    println("[PoSKMP-Gemini] ⚠️ No se pudo parsear la categoría de la respuesta.")
                    lastErrorMessage = "No se pudo interpretar el formato JSON devuelto por Gemini."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("[PoSKMP-Gemini] 💥 Excepción de red/parseo con $targetModel: ${e::class.simpleName}: ${e.message}")
                lastErrorMessage = "Error al conectar con Gemini: ${e.message ?: e::class.simpleName}"
            }
        }

        println("[PoSKMP-Gemini] ❌ Inferencia fallida en todos los modelos. Último error: $lastErrorMessage")
        println("[PoSKMP-Gemini] ==================================================")
        return GeminiInferenceResult.Failure(lastErrorMessage, lastHttpCode)
    }

    private fun parseGeminiResponse(
        rawText: String,
        existingCategories: List<String>,
        modelUsed: String
    ): GeminiCategoryResult? {
        val extractedJson = extractJsonPayload(rawText)
        var categoryName = ""
        var alternatives = emptyList<String>()

        try {
            val analysis = jsonParser.decodeFromString<GeminiCategoryAnalysis>(extractedJson)
            categoryName = analysis.category.trim()
            alternatives = analysis.alternativeCategories.map { it.trim() }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            // Fallback: simple line or text extraction
            val firstLine = extractedJson.lines()
                .map { it.trim().trim('"', '\'', '{', '}', ':', ',') }
                .firstOrNull { it.isNotBlank() && !it.contains("category", ignoreCase = true) }
            if (!firstLine.isNullOrBlank()) {
                categoryName = firstLine
            }
        }

        if (categoryName.isBlank()) return null

        // Align with existing categories if there's a close match
        val matchedExisting = existingCategories.find { it.equals(categoryName, ignoreCase = true) }
        val finalCategory = matchedExisting ?: categoryName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        return GeminiCategoryResult(
            suggestedCategory = finalCategory,
            alternativeCategories = alternatives,
            sourceModel = modelUsed
        )
    }

    private fun extractJsonPayload(rawText: String): String {
        val trimmed = rawText.trim()
        val jsonBlockRegex = Regex("""```(?:json)?\s*(\{[\s\S]*?\})\s*```""", RegexOption.IGNORE_CASE)
        val match = jsonBlockRegex.find(trimmed)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return trimmed.substring(start, end + 1).trim()
        }
        return trimmed
    }
}
