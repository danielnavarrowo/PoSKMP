package com.dnavarro.poskmp.ui.venta

import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.model.ProductSalesStats
import com.dnavarro.poskmp.ui.ProductSortField
import com.dnavarro.poskmp.ui.ProductSortOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogSearchRelevanceRankingTest {

    private fun createProduct(
        id: String,
        nombre: String,
        codigos: String = "[]",
        precio: Double = 10.0,
        categoria: String = "General",
        esFavorito: Long = 0L,
        activo: Long = 1L,
        piezas: Double = 10.0
    ): Products = Products(
        id = id,
        codigos = codigos,
        nombre = nombre,
        precio = precio,
        costo = 5.0,
        categoria = categoria,
        activo = activo,
        por_peso = 0L,
        precio_mayoreo = 8.0,
        es_favorito = esFavorito,
        piezas = piezas,
        precio_delivery = 12.0,
        created_at = 1000L,
        updated_at = 1000L,
        sync_state = "synced"
    )

    @Test
    fun testRelevanceTierOrdering_ExactOverPrefixOverWordBoundaryOverSubstring() {
        val exactMatch = createProduct("1", "Coca")
        val prefixMatch = createProduct("2", "Coca Cola 600ml")
        val wordBoundaryMatch = createProduct("3", "Jarabe de Coca")
        val midWordMatch = createProduct("4", "Refresco Chocococa")
        val weakSubstringFavorite = createProduct(
            id = "5",
            nombre = "Aceite de Coco y Cacao (coca)",
            esFavorito = 1L
        )
        val salesStats = mapOf(
            "5" to ProductSalesStats(productId = "5", totalVentas = 500.0)
        )

        val products = listOf(
            weakSubstringFavorite,
            midWordMatch,
            wordBoundaryMatch,
            prefixMatch,
            exactMatch
        )

        val result = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "coca",
            salesStats = salesStats
        )

        assertEquals("1", result[0].id, "Exact match must rank first")
        assertEquals("2", result[1].id, "Full query name prefix must rank second")
        assertEquals("3", result[2].id, "Word-boundary prefix must rank third")
        // Both prefix and word-boundary matches must outrank the weak substring favorite
        val resultIds = result.map { it.id }
        assertTrue(resultIds.indexOf("2") < resultIds.indexOf("5"), "Prefix match must outrank weak substring match even if favorite")
        assertTrue(resultIds.indexOf("3") < resultIds.indexOf("5"), "Word-boundary match must outrank weak substring match even if favorite")
    }

    @Test
    fun testWordBoundary_CocaColaRanksAboveColaderaAndChocolate_ForCola() {
        val cocaCola = createProduct("1", "Coca Cola")
        val coladera = createProduct("2", "Coladera de Plástico")
        val chocolate = createProduct("3", "Chocolate Abuelita", esFavorito = 1L)
        val salesStats = mapOf(
            "3" to ProductSalesStats(productId = "3", totalVentas = 200.0)
        )

        val products = listOf(chocolate, coladera, cocaCola)

        val result = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "cola",
            salesStats = salesStats
        )

        val resultIds = result.map { it.id }
        // "Coca Cola" contains the exact word "Cola" at word boundary, ranking above partial prefix "Coladera" and mid-word "Chocolate"
        assertEquals("1", result[0].id, "Coca Cola must rank above Coladera and Chocolate for query 'cola'")
        assertTrue(resultIds.indexOf("1") < resultIds.indexOf("2"), "Coca Cola must rank above Coladera")
        assertTrue(resultIds.indexOf("2") < resultIds.indexOf("3"), "Coladera must rank above Chocolate")
    }

    @Test
    fun testBarcodeMatch_ExactAndPrefixRanking() {
        val exactRaw = createProduct("1", "Producto Exacto", codigos = "[\"7501000123\"]")
        val exactNormalized = createProduct("2", "Producto Con Ceros", codigos = "[\"007501000123\"]")
        val prefixBarcode = createProduct("3", "Producto Prefijo", codigos = "[\"7501000123999\"]")
        val substringBarcode = createProduct("4", "Producto Interior", codigos = "[\"1117501000123222\"]")

        val products = listOf(substringBarcode, prefixBarcode, exactNormalized, exactRaw)

        val result = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "7501000123"
        )

        val topTwoIds = setOf(result[0].id, result[1].id)
        assertTrue(topTwoIds.contains("1") && topTwoIds.contains("2"), "Exact raw and leading-zero normalized barcode matches must be in top tier")
        assertEquals("3", result[2].id, "Barcode prefix match must rank above interior substring")
        assertEquals("4", result[3].id, "Barcode interior substring must rank lowest")
    }

    @Test
    fun testMultiTokenQueries_AllWordBoundariesRankAbovePartial() {
        val productBothWordBoundaries = createProduct("1", "Coca Cola 600ml")
        val productOneWordBoundary = createProduct("2", "Refresco Chocococa 600ml")
        val productNeither = createProduct("3", "Bebida Chocococa Mini600")

        val products = listOf(productNeither, productOneWordBoundary, productBothWordBoundaries)

        val result = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "coca 600"
        )

        assertEquals("1", result[0].id, "Product with all tokens at word boundaries must rank first")
        assertEquals("2", result[1].id, "Product with one word boundary token must rank second")
        assertEquals("3", result[2].id, "Product with all mid-word tokens must rank third")
    }

    @Test
    fun testAccentAndCaseInsensitivity() {
        val pinata = createProduct("1", "Piñata Tradicional")
        val cafe = createProduct("2", "Café de Altura")
        val other = createProduct("3", "Otró Artículo")

        val products = listOf(other, cafe, pinata)

        val resultPinata = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "pinata"
        )
        assertEquals("1", resultPinata.first().id, "'pinata' must match 'Piñata'")

        val resultCafe = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "cafe"
        )
        assertEquals("2", resultCafe.first().id, "'cafe' must match 'Café'")

        val resultUpper = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "PIÑATA"
        )
        assertEquals("1", resultUpper.first().id, "'PIÑATA' must match 'Piñata Tradicional'")
    }

    @Test
    fun testFavoriteAndBestSellerBoosting_WithinSameTier() {
        val p1 = createProduct("1", "Coca Cola Regular", esFavorito = 0L)
        val p2 = createProduct("2", "Coca Cola Light", esFavorito = 1L)
        val p3 = createProduct("3", "Coca Cola Zero", esFavorito = 1L)
        val p4 = createProduct("4", "Coca Cola Cherry", esFavorito = 0L)

        val salesStats = mapOf(
            "1" to ProductSalesStats(productId = "1", totalVentas = 0.0),
            "2" to ProductSalesStats(productId = "2", totalVentas = 10.0),
            "3" to ProductSalesStats(productId = "3", totalVentas = 150.0),
            "4" to ProductSalesStats(productId = "4", totalVentas = 50.0)
        )

        val products = listOf(p1, p4, p2, p3)

        val result = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "coca",
            salesStats = salesStats
        )

        // All 4 are Tier 2 prefix matches.
        // Ranking should boost favorites and higher sales:
        // p3 (favorite + 150 sales) > p2 (favorite + 10 sales) > p4 (non-favorite + 50 sales) > p1 (non-favorite + 0 sales)
        assertEquals("3", result[0].id, "p3 (favorite + 150 sales) should rank #1")
        assertEquals("2", result[1].id, "p2 (favorite + 10 sales) should rank #2")
        assertEquals("4", result[2].id, "p4 (non-favorite + 50 sales) should rank #3")
        assertEquals("1", result[3].id, "p1 (non-favorite + 0 sales) should rank #4")
    }

    @Test
    fun testBlankQuery_PreservesFavoritesFirst_AndConfiguredSort() {
        val pA = createProduct("A", "Zanahoria", esFavorito = 1L)
        val pB = createProduct("B", "Manzana", esFavorito = 1L)
        val pC = createProduct("C", "Aguacate", esFavorito = 0L)
        val pD = createProduct("D", "Plátano", esFavorito = 0L)

        val products = listOf(pA, pC, pD, pB)

        val result = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = ""
        )

        // Favorites first: B ("Manzana"), A ("Zanahoria")
        // Then non-favorites: C ("Aguacate"), D ("Plátano")
        val ids = result.map { it.id }
        assertEquals(listOf("B", "A", "C", "D"), ids)
    }

    @Test
    fun testTop100Truncation_PreservesHighRelevanceItemsLaterInAlphabet() {
        val manyProducts = mutableListOf<Products>()
        for (i in 1..120) {
            val numStr = i.toString().padStart(3, '0')
            manyProducts.add(createProduct("arr-$numStr", "Arroz Tipo $numStr"))
        }
        val highRelevanceZ = createProduct("z-exact", "Zapato Deportivo")
        manyProducts.add(highRelevanceZ)

        val result = computeDisplayedCatalogProducts(
            productsList = manyProducts,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "zapato"
        )

        assertEquals("z-exact", result[0].id, "High relevance match 'Zapato' must not be cut off by earlier alphabetical items")
        assertTrue(result.size <= 100, "Result list must not exceed 100 items")
    }

    @Test
    fun testTableColumnSortOverride_WhenSearchActive() {
        val pExpensive = createProduct("1", "Coca Cola 2L", precio = 40.0)
        val pCheap = createProduct("2", "Coca Cola Lata", precio = 15.0)
        val pMedium = createProduct("3", "Coca Cola 600ml", precio = 25.0)

        val products = listOf(pExpensive, pCheap, pMedium)

        // When user explicitly sorts by PRECIO ASC with active search, respect PRECIO sort
        val resultByPrice = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.PRECIO,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "coca"
        )

        assertEquals("2", resultByPrice[0].id, "Cheapest Coca Cola should be first when sorting by PRECIO ASC")
        assertEquals("3", resultByPrice[1].id, "Medium price Coca Cola should be second")
        assertEquals("1", resultByPrice[2].id, "Most expensive Coca Cola should be third")
    }

    @Test
    fun testTableColumnSortOverride_TiesResolvedByRelevance() {
        val pWeak = createProduct("1", "Refresco Chocococa", precio = 20.0)
        val pStrong = createProduct("2", "Coca Cola 600ml", precio = 20.0)

        val products = listOf(pWeak, pStrong)

        val result = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.PRECIO,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "coca"
        )

        // Both products have the same price ($20). Relevance tie-breaker should place pStrong ("Coca Cola") before pWeak ("Refresco Chocococa")
        assertEquals("2", result[0].id, "Ties in explicit column sort must be broken by search relevance")
        assertEquals("1", result[1].id)
    }

    @Test
    fun testPunctuationDelimitersInQueryAndProductName() {
        val pSpaced = createProduct("1", "Coca Cola 600ml")
        val pHyphen = createProduct("2", "Coca-Cola Zero 600ml")
        val pSlash = createProduct("3", "Aceite 1-2-3 (Original)")
        val pOther = createProduct("4", "Chocolatera Tradicional")

        val products = listOf(pOther, pSlash, pHyphen, pSpaced)

        // 1. Hyphenated query "coca-cola" should match both spaced and hyphenated products in top prefix tier
        val resultHyphenQuery = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "coca-cola"
        )
        val topTwoIds = setOf(resultHyphenQuery[0].id, resultHyphenQuery[1].id)
        assertTrue(topTwoIds.contains("1") && topTwoIds.contains("2"), "Both Coca Cola and Coca-Cola should rank at top for 'coca-cola'")

        // 2. Query with slash and hyphen "coca-cola/600"
        val resultSlashQuery = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "coca-cola/600"
        )
        val slashTopIds = setOf(resultSlashQuery[0].id, resultSlashQuery[1].id)
        assertTrue(slashTopIds.contains("1") && slashTopIds.contains("2"), "Both should match for multi-delimiter query 'coca-cola/600'")

        // 3. Spaced query "1 2 3" matching hyphenated product "Aceite 1-2-3"
        val resultNumQuery = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "aceite 1-2-3"
        )
        assertEquals("3", resultNumQuery.first().id, "'aceite 1-2-3' should match 'Aceite 1-2-3 (Original)'")
    }

    @Test
    fun testProgressiveTypingPrefixMatchMaintainsHighRelevanceTier() {
        val pTarget = createProduct("1", "Coca Cola 600ml")
        val pColadera = createProduct("2", "Coladera de Plastico", esFavorito = 1L)
        val salesStats = mapOf("2" to ProductSalesStats("2", totalVentas = 500.0))

        val products = listOf(pColadera, pTarget)

        // As user types: "coca", "coca col", "coca cola", "coca cola 600"
        val queries = listOf("coca", "coca col", "coca cola", "coca cola 600")
        for (q in queries) {
            val result = computeDisplayedCatalogProducts(
                productsList = products,
                sortField = ProductSortField.NOMBRE,
                sortOrder = ProductSortOrder.ASC,
                searchQuery = q,
                salesStats = salesStats
            )
            assertEquals("1", result[0].id, "Target product 'Coca Cola 600ml' must rank #1 during progressive query '$q'")
        }
    }

    @Test
    fun testDecimalQuantitiesInQueriesAndProductNames() {
        val p1 = createProduct("1", "Coca Cola 1.5L")
        val p2 = createProduct("2", "Coca Cola 2.5kg")
        val p3 = createProduct("3", "Coca Cola Lata 355ml")

        val products = listOf(p3, p2, p1)

        val result1 = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "coca 1.5"
        )
        assertEquals("1", result1.first().id, "'coca 1.5' should rank 'Coca Cola 1.5L' first")

        val result2 = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "1.5L"
        )
        assertEquals("1", result2.first().id, "'1.5L' should rank 'Coca Cola 1.5L' first")
    }

    @Test
    fun testDeterministicOrderingByProductIdWhenAllAttributesEqual() {
        val pB = createProduct("id-b", "Mismo Nombre", precio = 10.0)
        val pA = createProduct("id-a", "Mismo Nombre", precio = 10.0)
        val pC = createProduct("id-c", "Mismo Nombre", precio = 10.0)

        val products = listOf(pB, pC, pA)

        // When search is active and default sort
        val resultSearch = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "mismo"
        )
        assertEquals(listOf("id-a", "id-b", "id-c"), resultSearch.map { it.id }, "Ties must resolve by product ID ascending")

        // When search is blank and default sort
        val resultBlank = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = ""
        )
        assertEquals(listOf("id-a", "id-b", "id-c"), resultBlank.map { it.id }, "Blank query ties must resolve by product ID ascending")
    }

    @Test
    fun testPunctuationDelimitersExpanded_BracketsQuotesSymbolsSpanishMarks() {
        val pBracket = createProduct("1", "Coca Cola [600ml]")
        val pQuote = createProduct("2", "Galletas \"Oreo\"")
        val pSpanish = createProduct("3", "¡Promoción! Coca Cola")
        val pAmpersand = createProduct("4", "Aceite & Vinagre")
        val pColon = createProduct("5", "Promo: Coca Cola")
        val pOther = createProduct("6", "Producto General")

        val products = listOf(pOther, pColon, pAmpersand, pSpanish, pQuote, pBracket)

        // 1. Bracket word "600ml" matches pBracket in word-boundary tier
        val resBracket = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "600ml"
        )
        assertEquals("1", resBracket.first().id, "'600ml' should match 'Coca Cola [600ml]'")

        // 2. Quoted word "oreo" matches pQuote
        val resQuote = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "oreo"
        )
        assertEquals("2", resQuote.first().id, "'oreo' should match 'Galletas \"Oreo\"'")

        // 3. Query "promo" matches both Spanish inverted mark "¡Promoción!" and colon "Promo:"
        val resSpanish = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "promo"
        )
        val topIdsSpanish = setOf(resSpanish[0].id, resSpanish[1].id)
        assertTrue(topIdsSpanish.contains("3") && topIdsSpanish.contains("5"), "'promo' should match both '¡Promoción!' and 'Promo:'")

        // 4. Ampersand token "vinagre" matches pAmpersand
        val resAmpersand = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "vinagre"
        )
        assertEquals("4", resAmpersand.first().id, "'vinagre' should match 'Aceite & Vinagre'")
    }

    @Test
    fun testLeadingZeroBarcodePrefixMatching() {
        val pBarcodeWithZeroes = createProduct("1", "Galletas", codigos = "[\"00750123456\"]")
        val pBarcodeWithoutZeroes = createProduct("2", "Chocolates", codigos = "[\"750123456\"]")
        val pOther = createProduct("3", "Papas", codigos = "[\"999000111\"]")

        val products = listOf(pOther, pBarcodeWithoutZeroes, pBarcodeWithZeroes)

        // Query with leading zeroes "00750" matches both
        val res1 = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "00750"
        )
        val topTwoIds1 = setOf(res1[0].id, res1[1].id)
        assertTrue(topTwoIds1.contains("1") && topTwoIds1.contains("2"), "Both should match barcode prefix '00750'")

        // Query without leading zeroes "750" also matches both
        val res2 = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "750"
        )
        val topTwoIds2 = setOf(res2[0].id, res2[1].id)
        assertTrue(topTwoIds2.contains("1") && topTwoIds2.contains("2"), "Both should match barcode prefix '750'")
    }

    @Test
    fun testExtremeQueryLengthsStrictTierSeparation() {
        val pExact = createProduct("1", "Agua")
        val pPrefix = createProduct("2", "Agua Natural Ciel")
        val pWordBoundary = createProduct("3", "Botella de Agua Bonafont")
        val pSubstring = createProduct("4", "Desaguador Industrial", esFavorito = 1L)
        val salesStats = mapOf("4" to ProductSalesStats("4", totalVentas = 1000.0))

        val products = listOf(pSubstring, pWordBoundary, pPrefix, pExact)

        // Extremely long query containing "agua" + many tokens should still strictly respect tier boundaries
        val parsedQuery = CatalogSearchRanker.parseQuery("agua")
        val scoreExact = CatalogSearchRanker.computeRelevanceScore(pExact, parsedQuery)
        val scorePrefix = CatalogSearchRanker.computeRelevanceScore(pPrefix, parsedQuery)
        val scoreWordBoundary = CatalogSearchRanker.computeRelevanceScore(pWordBoundary, parsedQuery)
        val scoreSubstring = CatalogSearchRanker.computeRelevanceScore(pSubstring, parsedQuery, salesStats)

        assertTrue(scoreExact >= 100_000.0, "Exact score should be >= 100,000")
        assertTrue(scorePrefix in 50_000.0..55_000.0, "Prefix score should be in [50,000, 55,000]")
        assertTrue(scoreWordBoundary in 20_000.0..25_000.0, "Word boundary score should be in [20,000, 25,000]")
        assertTrue(scoreSubstring in 2_000.0..2_500.0, "Substring score should be in [2,000, 2,500]")

        // Multi-token query with 20 tokens matching word boundaries (Tier 3) stays bounded below Tier 2
        val longTokens = (1..20).joinToString(" ") { "tok$it" }
        val parsedLong = CatalogSearchRanker.parseQuery("agua $longTokens")
        val pWordBoundariesLong = createProduct("5", "Bebida $longTokens Agua")
        val scoreTier3 = CatalogSearchRanker.computeRelevanceScore(pWordBoundariesLong, parsedLong)
        assertTrue(scoreTier3 in 20_000.0..26_000.0, "20-token Tier 3 score should stay in [20,000, 26,000] well below Tier 2")

        // Multi-token query with mid-word tokens (Tier 4) stays bounded below Tier 3
        val pMidWordTokensLong = createProduct("6", "Bebidaguax ${(1..20).joinToString(" ") { "pre${it}suf" }}")
        val parsedMidWord = CatalogSearchRanker.parseQuery("agua ${(1..20).joinToString(" ") { "$it" }}")
        val scoreTier4 = CatalogSearchRanker.computeRelevanceScore(pMidWordTokensLong, parsedMidWord)
        assertTrue(scoreTier4 in 10_000.0..14_000.0, "Multi-token Tier 4 score should stay in [10,000, 14,000] well below Tier 3")
    }

    @Test
    fun testMarginSortingInCatalog() {
        // p1: Cost 10, Price 20 -> margin 100%
        val p1 = createProduct("1", "Prod A", precio = 20.0).copy(costo = 10.0)
        // p2: Cost 10, Price 15 -> margin 50%
        val p2 = createProduct("2", "Prod B", precio = 15.0).copy(costo = 10.0)
        // p3: Cost 10, Price 30 -> margin 200%
        val p3 = createProduct("3", "Prod C", precio = 30.0).copy(costo = 10.0)

        val products = listOf(p1, p2, p3)

        val resAsc = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.MARGEN_VENTA,
            sortOrder = ProductSortOrder.ASC
        )
        assertEquals(listOf("2", "1", "3"), resAsc.map { it.id }, "MARGEN_VENTA ASC should sort lowest to highest margin")

        val resDesc = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.MARGEN_VENTA,
            sortOrder = ProductSortOrder.DESC
        )
        assertEquals(listOf("3", "1", "2"), resDesc.map { it.id }, "MARGEN_VENTA DESC should sort highest to lowest margin")
    }

    @Test
    fun testCurrencySmartQuotesPercentAndOrdinalDelimiters() {
        val pCurrency = createProduct("p-curr", "Coca Cola $15 600ml")
        val pSmartQuotes = createProduct("p-smart", "Galletas “Oreo” Clásicas")
        val pPercent = createProduct("p-perc", "Aceite 10% Gratis")
        val pOrdinal = createProduct("p-ord", "Harina 1° Calidad")

        val products = listOf(pOrdinal, pPercent, pSmartQuotes, pCurrency)

        // 1. Currency query "15" and "$15" should match pCurrency
        val res15 = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "15"
        )
        assertEquals("p-curr", res15.first().id, "'15' should match 'Coca Cola $15 600ml'")

        val resDollar15 = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "$15"
        )
        assertEquals("p-curr", resDollar15.first().id, "'$15' should match 'Coca Cola $15 600ml'")

        // 2. Smart quotes query "oreo" should match pSmartQuotes
        val resOreo = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "oreo"
        )
        assertEquals("p-smart", resOreo.first().id, "'oreo' should match 'Galletas “Oreo” Clásicas'")

        // 3. Percent query "10" and "10%" should match pPercent
        val res10 = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "10"
        )
        assertEquals("p-perc", res10.first().id, "'10' should match 'Aceite 10% Gratis'")

        // 4. Ordinal degree symbol "1" should match pOrdinal
        val res1 = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "1"
        )
        assertEquals("p-ord", res1.first().id, "'1' should match 'Harina 1° Calidad'")
    }

    @Test
    fun testMultipleBarcodesPerProductExactAndPrefixMatching() {
        val pMulti = createProduct(
            id = "p-multi",
            nombre = "Producto Con Multiples Codigos",
            codigos = "[\"750100000001\", \"750200000002\", \"750300000003\"]"
        )
        val pOther = createProduct(
            id = "p-other",
            nombre = "Otro Producto",
            codigos = "[\"999999999999\"]"
        )

        val products = listOf(pOther, pMulti)

        // Exact match on 2nd barcode with leading zero normalization
        val resExact = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "00750200000002"
        )
        assertEquals("p-multi", resExact.first().id, "Should exact match on second barcode")

        // Prefix match on 3rd barcode
        val resPrefix = computeDisplayedCatalogProducts(
            productsList = products,
            sortField = ProductSortField.NOMBRE,
            sortOrder = ProductSortOrder.ASC,
            searchQuery = "750300"
        )
        assertEquals("p-multi", resPrefix.first().id, "Should prefix match on third barcode")
    }

    @Test
    fun testSalesStatsRobustness_NegativeAndZeroSales() {
        val p1 = createProduct("1", "Coca Cola Regular")
        val p2 = createProduct("2", "Coca Cola Zero")

        val salesStats = mapOf(
            "1" to ProductSalesStats("1", totalVentas = -10.0),
            "2" to ProductSalesStats("2", totalVentas = 0.0)
        )

        val score1 = CatalogSearchRanker.computeRelevanceScore(p1, "coca", salesStats)
        val score2 = CatalogSearchRanker.computeRelevanceScore(p2, "coca", salesStats)

        assertTrue(score1 > 0.0 && !score1.isNaN() && !score1.isInfinite(), "Score 1 must be finite and valid")
        assertTrue(score2 > 0.0 && !score2.isNaN() && !score2.isInfinite(), "Score 2 must be finite and valid")
    }
}

