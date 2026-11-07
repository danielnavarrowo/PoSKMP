package com.dnavarro.poskmp.ui.venta

import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.model.ProductSalesStats
import com.dnavarro.poskmp.util.isBarcodeMatch
import com.dnavarro.poskmp.util.normalizeBarcode
import com.dnavarro.poskmp.util.normalizeForSearch
import com.dnavarro.poskmp.util.parseBarcodes
import kotlin.math.ln
import kotlin.math.min

/**
 * Deterministic relevance-scoring algorithm for CatalogSection search.
 *
 * Implements match quality tiers (Exact match > Full query prefix > Word-boundary prefix >
 * Multi-token all-words > Substring match) and boosts favorite and best-selling products
 * within tiers without allowing weak matches to outrank strong prefix/exact matches.
 */
object CatalogSearchRanker {

    // Splits on whitespace, slashes, hyphens, underscores, parentheses, brackets, braces,
    // commas, colons, semicolons, quotes, smart quotes, exclamation, question marks, plus, equals, ampersand,
    // asterisk, pipe, tilde, hash, angle brackets, currency ($), percent (%), at (@), caret (^), degrees/ordinals (°, º, ª),
    // Spanish inverted marks, and periods when not directly between digits (to preserve decimals like 1.5L or 2.5kg).
    private val WORD_SPLIT_REGEX = """[-\s/_(),+=:;!?'"\[\]{}*&|#~<>¡¿\\$%@^°ºª“”‘’«»]+|(?<!\d)\.|\.(?!\d)""".toRegex()

    data class ParsedQuery(
        val rawQuery: String,
        val trimmedQuery: String,
        val normQuery: String,
        val cleanQuery: String,
        val queryTokens: List<String>,
        val normBarcodeQuery: String
    ) {
        val isEmpty: Boolean get() = trimmedQuery.isEmpty()
    }

    fun parseQuery(query: String): ParsedQuery {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return ParsedQuery(
                rawQuery = query,
                trimmedQuery = "",
                normQuery = "",
                cleanQuery = "",
                queryTokens = emptyList(),
                normBarcodeQuery = ""
            )
        }
        val norm = trimmed.normalizeForSearch()
        val tokens = norm.split(WORD_SPLIT_REGEX).filter { it.isNotEmpty() }
        val clean = tokens.joinToString(" ")
        val barcodeNorm = normalizeBarcode(trimmed)
        return ParsedQuery(
            rawQuery = query,
            trimmedQuery = trimmed,
            normQuery = norm,
            cleanQuery = clean,
            queryTokens = tokens,
            normBarcodeQuery = barcodeNorm
        )
    }

    fun computeRelevanceScore(
        product: Products,
        parsed: ParsedQuery,
        salesStats: Map<String, ProductSalesStats> = emptyMap()
    ): Double {
        if (parsed.isEmpty) return 0.0

        val trimmedQuery = parsed.trimmedQuery
        val normQuery = parsed.normQuery
        val cleanQuery = parsed.cleanQuery
        val queryTokens = parsed.queryTokens
        val normBarcodeQuery = parsed.normBarcodeQuery

        val normName = product.nombre.normalizeForSearch()
        val nameWords = normName.split(WORD_SPLIT_REGEX).filter { it.isNotEmpty() }
        val cleanName = nameWords.joinToString(" ")
        val parsedBarcodes = product.parseBarcodes()
        val sales = salesStats[product.id]?.totalVentas ?: 0.0
        val isFavorite = product.es_favorito == 1L

        val favoriteBonus = if (isFavorite) 100.0 else 0.0
        val salesBonus = min(100.0, if (sales > 0.0) ln(1.0 + sales) * 15.0 else 0.0)

        // ---------------------------------------------------------
        // Tier 1: Exact Match (Highest Priority)
        // Exact barcode match (raw or leading-zero normalized),
        // exact full product name match (case-, accent-, and delimiter-tolerant),
        // or exact product ID match.
        // ---------------------------------------------------------
        val isExactBarcode = parsedBarcodes.any { isBarcodeMatch(it, trimmedQuery) }
        val isExactName = normName == normQuery || (cleanQuery.isNotEmpty() && cleanName == cleanQuery)
        val isExactId = product.id.equals(trimmedQuery, ignoreCase = true)

        if (isExactBarcode || isExactName || isExactId) {
            val tierBase = 100_000.0
            val subBonus = when {
                isExactBarcode -> 1_500.0
                isExactName -> 1_000.0
                else -> 500.0
            }
            val density = (1.0 / normName.length.coerceAtLeast(1)) * 50.0
            return tierBase + subBonus + favoriteBonus + salesBonus + density
        }

        // ---------------------------------------------------------
        // Tier 2: Full Query Prefix Match
        // Product name starts with full query at clean word boundaries
        // (e.g. "Coca Cola" for "coca", "coca col", "coca cola", "coca-cola"),
        // or barcode starts with the query.
        // ---------------------------------------------------------
        val barcodeStartsWithQuery = parsedBarcodes.any {
            it.startsWith(trimmedQuery, ignoreCase = true) ||
                (normBarcodeQuery.isNotEmpty() && normalizeBarcode(it).startsWith(normBarcodeQuery))
        }

        val isNamePrefixMatch = run {
            if (queryTokens.isEmpty() || nameWords.size < queryTokens.size) return@run false
            for (i in 0 until queryTokens.size - 1) {
                if (nameWords[i] != queryTokens[i]) return@run false
            }
            val lastQueryToken = queryTokens.last()
            val targetWord = nameWords[queryTokens.size - 1]
            if (!targetWord.startsWith(lastQueryToken)) return@run false

            if (queryTokens.size == 1) {
                targetWord == lastQueryToken || cleanName == cleanQuery || cleanName.startsWith("$cleanQuery ")
            } else {
                cleanName.startsWith(cleanQuery)
            }
        }

        if (isNamePrefixMatch || barcodeStartsWithQuery) {
            val tierBase = 50_000.0
            val coverage = ((normQuery.length.toDouble() / normName.length.coerceAtLeast(1)).coerceAtMost(1.0)) * 500.0
            val barcodeBonus = if (barcodeStartsWithQuery) 300.0 else 0.0
            return tierBase + coverage + barcodeBonus + favoriteBonus + salesBonus
        }

        // ---------------------------------------------------------
        // Tier 3: Word-Boundary Prefix Match
        // Query (or all query tokens) matches start of words in product name.
        // Higher scores when matching whole words, appearing earlier, or in consecutive order.
        // Also captures first-word partial prefixes (e.g. "Coladera" for "cola").
        // ---------------------------------------------------------
        if (queryTokens.size <= 1) {
            val token = if (queryTokens.isNotEmpty()) queryTokens[0] else normQuery
            if (token.isNotEmpty()) {
                val matchingWordIndices = nameWords.indices.filter { nameWords[it].startsWith(token) }
                if (matchingWordIndices.isNotEmpty()) {
                    val tierBase = 20_000.0
                    val firstWordIndex = matchingWordIndices.first()
                    val positionBonus = (10.0 - firstWordIndex).coerceAtLeast(0.0) * 100.0
                    val hasExactWord = matchingWordIndices.any { nameWords[it] == token }
                    val exactWordBonus = if (hasExactWord) 2_000.0 else 0.0
                    // First-word partial prefix bonus (e.g. "Coladera" starts with "cola" at word 0)
                    val startOfNameBonus = if (cleanName.startsWith(token) || normName.startsWith(normQuery)) 1_000.0 else 0.0
                    val coverage = ((normQuery.length.toDouble() / normName.length.coerceAtLeast(1)).coerceAtMost(1.0)) * 300.0
                    return tierBase + exactWordBonus + startOfNameBonus + positionBonus + coverage + favoriteBonus + salesBonus
                }
            }
        } else {
            // Multi-token: check if all tokens match the start of distinct words
            val matchedWordIndices = mutableListOf<Int>()
            var allTokensMatchWordStart = true
            for (token in queryTokens) {
                val matchIdx = nameWords.indices.firstOrNull { idx ->
                    idx !in matchedWordIndices && nameWords[idx].startsWith(token)
                }
                if (matchIdx != null) {
                    matchedWordIndices.add(matchIdx)
                } else {
                    allTokensMatchWordStart = false
                    break
                }
            }
            if (allTokensMatchWordStart) {
                val tierBase = 20_000.0
                val isConsecutive = matchedWordIndices.zipWithNext().all { (a, b) -> b == a + 1 }
                val isAscending = matchedWordIndices.zipWithNext().all { (a, b) -> b > a }
                val orderBonus = when {
                    isConsecutive -> 1_000.0
                    isAscending -> 500.0
                    else -> 0.0
                }
                val avgPosition = matchedWordIndices.average()
                val positionBonus = (10.0 - avgPosition).coerceAtLeast(0.0) * 80.0
                val exactWordsCount = queryTokens.indices.count { i ->
                    val wIdx = matchedWordIndices[i]
                    nameWords[wIdx] == queryTokens[i]
                }
                val exactWordBonus = min(3_000.0, exactWordsCount * 300.0)
                val coverage = ((queryTokens.sumOf { it.length }.toDouble() / normName.length.coerceAtLeast(1)).coerceAtMost(1.0)) * 300.0
                return tierBase + orderBonus + positionBonus + exactWordBonus + coverage + favoriteBonus + salesBonus
            }
        }

        // ---------------------------------------------------------
        // Tier 4: Multi-Token All-Words Match
        // All query tokens appear in product name or barcodes, with bonus for
        // word-boundary tokens vs mid-word substring tokens.
        // ---------------------------------------------------------
        if (queryTokens.size > 1) {
            val allTokensPresent = queryTokens.all { token ->
                val normToken = normalizeBarcode(token)
                normName.contains(token) || cleanName.contains(token) || parsedBarcodes.any {
                    it.contains(token, ignoreCase = true) ||
                        (normToken.isNotEmpty() && normalizeBarcode(it).contains(normToken))
                }
            }
            if (allTokensPresent) {
                val tierBase = 10_000.0
                val wordBoundaryTokenCount = queryTokens.count { token ->
                    nameWords.any { it.startsWith(token) }
                }
                val wordBoundaryBonus = min(3_000.0, wordBoundaryTokenCount * 400.0)
                val coverage = ((queryTokens.sumOf { it.length }.toDouble() / normName.length.coerceAtLeast(1)).coerceAtMost(1.0)) * 200.0
                return tierBase + wordBoundaryBonus + coverage + favoriteBonus + salesBonus
            }
        }

        // ---------------------------------------------------------
        // Tier 5: Substring Match
        // Query appears as an interior substring inside product name or barcode.
        // ---------------------------------------------------------
        val nameSubIndex = when {
            normQuery.isNotEmpty() && normName.contains(normQuery) -> normName.indexOf(normQuery)
            cleanQuery.isNotEmpty() && cleanName.contains(cleanQuery) -> cleanName.indexOf(cleanQuery)
            else -> -1
        }
        val barcodeSub = (trimmedQuery.isNotEmpty() && parsedBarcodes.any { it.contains(trimmedQuery, ignoreCase = true) }) ||
            (normBarcodeQuery.isNotEmpty() && parsedBarcodes.any { normalizeBarcode(it).contains(normBarcodeQuery) })

        if (nameSubIndex >= 0 || barcodeSub) {
            val tierBase = 2_000.0
            val posBonus = if (nameSubIndex >= 0) {
                (1.0 - (nameSubIndex.toDouble() / normName.length.coerceAtLeast(1))) * 100.0
            } else {
                50.0
            }
            val coverage = ((normQuery.length.toDouble() / normName.length.coerceAtLeast(1)).coerceAtMost(1.0)) * 100.0
            return tierBase + posBonus + coverage + (favoriteBonus * 0.5) + (salesBonus * 0.5)
        }

        // ---------------------------------------------------------
        // Tier 6: Fallback / Category Match / No Direct Match
        // ---------------------------------------------------------
        val normCat = product.categoria?.normalizeForSearch() ?: ""
        val categoryMatch = (normQuery.isNotEmpty() && normCat.contains(normQuery)) || (cleanQuery.isNotEmpty() && normCat.contains(cleanQuery))
        val catBonus = if (categoryMatch) 200.0 else 0.0
        return catBonus + (favoriteBonus * 0.2) + (salesBonus * 0.2)
    }

    fun computeRelevanceScore(
        product: Products,
        searchQuery: String,
        salesStats: Map<String, ProductSalesStats> = emptyMap()
    ): Double {
        return computeRelevanceScore(product, parseQuery(searchQuery), salesStats)
    }
}
