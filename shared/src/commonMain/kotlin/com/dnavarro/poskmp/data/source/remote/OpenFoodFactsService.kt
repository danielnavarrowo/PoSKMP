package com.dnavarro.poskmp.data.source.remote

import com.dnavarro.poskmp.util.CategorySuggester
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class OpenFoodFactsResponse(
    val code: String? = null,
    val status: Int? = null,
    @SerialName("status_verbose") val statusVerbose: String? = null,
    val product: OpenFoodFactsProductItem? = null
)

@Serializable
data class OpenFoodFactsProductItem(
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_es") val productNameEs: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @SerialName("categories_tags") val categoriesTags: List<String> = emptyList(),
    val categories: String? = null
)

data class OpenFoodFactsProduct(
    val code: String,
    val displayName: String,
    val brand: String?,
    val quantity: String?,
    val suggestedCategory: String?,
    val categoriesTags: List<String> = emptyList()
)

interface OpenFoodFactsService {
    suspend fun lookupProduct(
        barcode: String,
        existingCategories: List<String> = emptyList()
    ): OpenFoodFactsProduct?
}

class OpenFoodFactsServiceImpl(
    private val httpClient: HttpClient = createDefaultClient()
) : OpenFoodFactsService {

    companion object {
        fun createDefaultClient(): HttpClient {
            return HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = false
                        isLenient = true
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    })
                }
            }
        }

        private const val API_BASE_URL = "https://world.openfoodfacts.org/api/v2/product"
        private const val USER_AGENT = "PoSKMP - Multiplatform POS"
    }

    override suspend fun lookupProduct(
        barcode: String,
        existingCategories: List<String>
    ): OpenFoodFactsProduct? {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.length < 6) return null

        return try {
            withTimeoutOrNull(4000.milliseconds) {
                val url = "$API_BASE_URL/$cleanBarcode.json?fields=product_name,product_name_es,brands,quantity,categories_tags,categories"
                val response: OpenFoodFactsResponse = httpClient.get(url) {
                    header("User-Agent", USER_AGENT)
                    header("Accept", "application/json")
                }.body()

                if (response.status != 1 || response.product == null) {
                    return@withTimeoutOrNull null
                }

                val item = response.product
                val rawName = item.productNameEs?.takeIf { it.isNotBlank() }
                    ?: item.productName?.takeIf { it.isNotBlank() }
                    ?: return@withTimeoutOrNull null

                val firstBrand = item.brands?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
                val quantity = item.quantity?.trim()?.takeIf { it.isNotBlank() }
                val formattedName = formatDisplayName(rawName, firstBrand, quantity)

                val rawCategory = mapTagsToCategory(item.categoriesTags)
                val resolvedCategory = if (rawCategory != null) {
                    CategorySuggester.resolveToExistingCategory(rawCategory, existingCategories)
                } else {
                    null
                }

                OpenFoodFactsProduct(
                    code = cleanBarcode,
                    displayName = formattedName,
                    brand = firstBrand,
                    quantity = quantity,
                    suggestedCategory = resolvedCategory,
                    categoriesTags = item.categoriesTags
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    internal fun formatDisplayName(rawName: String, brand: String?, quantity: String?): String {
        val nameWithBrand = if (brand != null && !rawName.contains(brand, ignoreCase = true)) {
            "$brand $rawName"
        } else {
            rawName
        }
        return if (quantity != null && !nameWithBrand.contains(quantity, ignoreCase = true)) {
            "$nameWithBrand $quantity"
        } else {
            nameWithBrand
        }.trim()
    }

    internal fun mapTagsToCategory(tags: List<String>): String? {
        for (tag in tags) {
            val lower = tag.lowercase()
            when {
                lower.contains("beverage") || lower.contains("soda") || lower.contains("drink") || lower.contains("cola") || lower.contains("juice") || lower.contains("water") -> return CategorySuggester.CAT_BEBIDAS
                lower.contains("snack") || lower.contains("chip") || lower.contains("crisp") || lower.contains("botana") || lower.contains("popcorn") || lower.contains("palomita") -> return CategorySuggester.CAT_BOTANAS
                lower.contains("dairy") || lower.contains("milk") || lower.contains("cheese") || lower.contains("yogurt") || lower.contains("egg") || lower.contains("leche") || lower.contains("queso") -> return CategorySuggester.CAT_LACTEOS
                lower.contains("bread") || lower.contains("biscuit") || lower.contains("cake") || lower.contains("cookie") || lower.contains("pastr") || lower.contains("pan") || lower.contains("galleta") -> return CategorySuggester.CAT_PANADERIA
                lower.contains("candy") || lower.contains("candies") || lower.contains("chocolate") || lower.contains("confection") || lower.contains("sweet") || lower.contains("dulce") || lower.contains("chicle") -> return CategorySuggester.CAT_DULCERIA
                lower.contains("sauce") || lower.contains("condiment") || lower.contains("canned") || lower.contains("grocery") || lower.contains("pasta") || lower.contains("rice") || lower.contains("oil") || lower.contains("abarrote") -> return CategorySuggester.CAT_ABARROTES
                lower.contains("clean") || lower.contains("detergent") || lower.contains("soap") || lower.contains("limpieza") -> return CategorySuggester.CAT_ABARROTES
                lower.contains("meat") || lower.contains("sausage") || lower.contains("ham") || lower.contains("carne") || lower.contains("jamon") || lower.contains("embutido") -> return CategorySuggester.CAT_CARNES
                lower.contains("beer") || lower.contains("wine") || lower.contains("liquor") || lower.contains("alcohol") || lower.contains("cerveza") -> return CategorySuggester.CAT_CERVEZAS
                lower.contains("fruit") || lower.contains("vegetable") || lower.contains("fruta") || lower.contains("verdura") -> return CategorySuggester.CAT_FRUTAS_VERDURAS
                lower.contains("pet") || lower.contains("dog") || lower.contains("cat") || lower.contains("mascota") -> return CategorySuggester.CAT_MASCOTAS
            }
        }
        return null
    }
}
