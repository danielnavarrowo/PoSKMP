package com.dnavarro.poskmp.util

import com.dnavarro.poskmp.db.Products

/**
 * Intelligent local category recommender for retail/convenience stores.
 *
 * Utilizes a 3-tier strategy:
 * 1. Store Catalog Memory: Analyzes existing catalog products to discover store-specific patterns.
 * 2. Retail & Brand Taxonomy Dictionary: Fast, offline knowledge base for standard Mexican/Latin retail brands & keywords.
 * 3. Store Category Resolver: Maps predicted categories to existing store categories to prevent duplicates and fragmented naming.
 */
object CategorySuggester {

    private val STOPWORDS = setOf(
        "de", "del", "la", "el", "los", "las", "un", "una", "unos", "unas",
        "con", "sin", "para", "en", "por", "y", "o", "al", "a", "sabor", "tipo"
    )

    private val GENERIC_MODIFIERS = setOf(
        "blanco", "blanca", "negro", "negra", "rojo", "roja", "azul", "verde", "amarillo",
        "chico", "chica", "mediano", "mediana", "grande", "extra", "mini", "maxi", "plus",
        "natural", "original", "clasico", "clasica", "tradicional", "especial", "premium",
        "familiar", "individual", "nuevo", "nueva", "rico", "rica", "fresco", "fresca"
    )

    private val UNIT_REGEX = Regex(
        "\\b\\d+([.,]\\d+)?\\s*(kg|kilos?|g|gr|grs|gramos?|l|lt|lts|litros?|ml|milis?|pz|pza|pzas|piezas?|oz|cm|mm|pack|paquete|caja)\\b",
        RegexOption.IGNORE_CASE
    )
    private val DIGITS_REGEX = Regex("\\b\\d+\\b")
    private val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9\\s]")

    // Canonical category names
    const val CAT_BOTANAS = "Botanas"
    const val CAT_BEBIDAS = "Bebidas"
    const val CAT_LACTEOS = "Lácteos"
    const val CAT_PANADERIA = "Galletas"
    const val CAT_DULCERIA = "Dulcería"
    const val CAT_ABARROTES = "Abarrotes"
    const val CAT_CUIDADO_PERSONAL = "Cuidado Personal"
    const val CAT_CARNES = "Carnes y Embutidos"
    const val CAT_CERVEZAS = "Cervezas y Licores"
    const val CAT_FRUTAS_VERDURAS = "Frutas y Verduras"
    const val CAT_MASCOTAS = "Mascotas"

    /**
     * Synonym clusters to prevent duplicating similar categories
     * (e.g. mapping "Botanas" to store's existing "Frituras" or "Snacks").
     */
    private val CATEGORY_SYNONYM_GROUPS: List<Set<String>> = listOf(
        setOf("botanas", "botana", "frituras", "fritura", "snacks", "snack", "papas", "chicharrones"),
        setOf("bebidas", "bebida", "refrescos", "refresco", "jugos", "jugo", "aguas", "agua", "sodas", "soda", "liquidos"),
        setOf("lacteos", "lacteo", "lecheria", "quesos", "queso", "cremeria", "refrigerados", "lacteos y huevos", "huevos"),
        setOf("panaderia", "pan", "panes", "galletas", "galleta", "galleteria", "reposteria", "panaderia y galletas"),
        setOf("dulceria", "dulces", "dulce", "chocolates", "chocolate", "chocolateria", "golosinas"),
        setOf("abarrotes", "despensa", "comestibles", "viveres", "granos", "semillas"),
        setOf("limpieza", "detergentes", "detergente", "quimicos", "hogar", "limpieza del hogar", "aseo", "articulos de aseo", "limpieza y hogar"),
        setOf("cuidado personal", "higiene", "perfumeria", "farmacia", "salud"),
        setOf("carnes", "carniceria", "embutidos", "embutido", "salchichoneria", "cremeria", "carnes y embutidos"),
        setOf("cervezas", "cerveza", "vinos", "licores", "vinos y licores", "licoreria", "alcohol", "bebidas alcoholicas", "cervezas y licores"),
        setOf("frutas y verduras", "frutas", "verduras", "fruteria", "perecederos"),
        setOf("mascotas", "alimento mascotas", "veterinaria"),
        setOf("cigarros", "cigarro", "tabaco", "tabaqueria")
    )

    // Multi-word phrases that give high confidence
    private val MULTIWORD_PHRASES: List<Pair<String, List<String>>> = listOf(
        CAT_BOTANAS to listOf(
            "papas fritas", "papas sabritas", "cacahuates japoneses", "cacahuates salados",
            "cacahuates enchilados", "totopos de maiz", "chicharron de cerdo", "palomitas acarameladas",
            "palomitas mantequilla", "palomitas naturales", "chicharron botanero", "churrumais con limon",
            "doritos nacho"
        ),
        CAT_BEBIDAS to listOf(
            "coca cola", "del valle", "red bull", "agua mineral", "agua natural", "agua purificada",
            "jugo de naranja", "jugo de manzana", "sidral mundet", "sangria senorial", "te helado",
            "suero oral", "cafe soluble", "cafe en grano", "cafe tostado"
        ),
        CAT_LACTEOS to listOf(
            "santa clara", "leche entera", "leche deslactosada", "leche semidescremada",
            "queso panela", "queso oaxaca", "queso manchego", "queso fresco", "queso crema",
            "crema acida", "crema entera", "media crema", "leche condensada", "leche evaporada", "leche en polvo"
        ),
        CAT_PANADERIA to listOf(
            "tia rosa", "pan blanco", "pan integral", "pan tostado", "pan dulce",
            "triki trakes", "galletas marias", "galletas saladas"
        ),
        CAT_DULCERIA to listOf(
            "de la rosa", "carlos v", "pelon pelo rico", "kinder sorpresa", "kinder bueno", "paleta payaso"
        ),
        CAT_ABARROTES to listOf(
            "la costena", "la moderna", "la sierra", "verde valle", "tres estrellas",
            "pure de tomate", "sopa de fideo", "aceite vegetal", "aceite de soya",
            "chiles en vinagre", "frijoles refritos"
        ),
        CAT_CUIDADO_PERSONAL to listOf(
            "papel higienico", "pasta dental", "cepillo dental", "toallas sanitarias",
            "toallitas humedas", "jabon de bano", "crema corporal"
        ),
        CAT_CARNES to listOf(
            "san rafael", "pechuga de pavo", "jamon de pierna", "jamon de pavo",
            "salchicha de pavo", "salchicha viena", "carne molida"
        ),
        CAT_CERVEZAS to listOf(
            "carta blanca", "dos equis", "jose cuervo", "don julio", "vino tinto", "vino blanco"
        ),
        CAT_MASCOTAS to listOf(
            "comida para perro", "comida para gato", "alimento para perro",
            "alimento para gato", "alimento para mascotas", "arena para gato"
        ),
    )

    // Single-word keywords and brand names mapped to category
    private val KEYWORDS_MAP: Map<String, String> = buildMap {
        fun addWords(category: String, words: List<String>) {
            for (word in words) {
                put(word.normalizeForSearch(), category)
            }
        }

        addWords(
            CAT_BOTANAS,
            listOf(
                "botana", "botanas", "fritura", "frituras", "chicharron", "chicharrones", "papa", "papas",
                "cacahuate", "cacahuates", "palomitas", "palomita", "churrito", "churritos", "totopo", "totopos",
                "nacho", "nachos", "tostada", "tostadas", "pistache", "pistaches", "pepita", "pepitas",
                "semilla", "semillas", "sabritas", "doritos", "cheetos", "ruffles", "tostitos", "takis",
                "barcel", "chips", "churrumais", "cazares", "totis", "runners", "crujitos", "pringles",
                "rancheritos", "fritos", "toreadas", "popkaramel", "karameladas", "snack", "snacks"
            )
        )

        addWords(
            CAT_BEBIDAS,
            listOf(
                "refresco", "refrescos", "bebida", "bebidas", "agua", "aguas", "jugo", "jugos",
                "nectar", "nectares", "soda", "sodas", "energizante", "hidratante", "electrolito", "electrolitos",
                "te", "tes", "coca", "pepsi", "jarrito", "jarritos", "penafiel", "boing", "jumex",
                "gatorade", "powerade", "monster", "redbull", "bonafont", "ciel", "epura", "squirt",
                "fanta", "sprite", "mundet", "sangria", "delvalle", "arizona", "ameyal", "mirinda",
                "manzanita", "scheweppes", "electrolit", "suerox", "frutsi", "tang", "zuko"
            )
        )

        addWords(
            CAT_LACTEOS,
            listOf(
                "leche", "leches", "queso", "quesos", "crema", "cremas", "mantequilla", "mantequillas",
                "yogurt", "yogur", "yogurts", "requeson", "cuajada", "huevo", "huevos", "blanquillo",
                "blanquillos", "margarina", "lala", "alpura", "santaclara", "danone", "yoplait",
                "chontalpa", "nochebuena", "covadonga", "philadelphia", "danonino", "yakult",
                "nutrileche", "carnation", "lechera", "svelty"
            )
        )

        addWords(
            CAT_PANADERIA,
            listOf(
                "pan", "panes", "bolillo", "bolillos", "telera", "teleras", "dona", "donas",
                "concha", "conchas", "mantecada", "mantecadas", "cuerno", "cuernos", "pastelito", "pastelitos",
                "galleta", "galletas", "panque", "pay", "polvoron", "polvorones", "bimbo", "marinela",
                "tiarosa", "gamesa", "wonder", "barritas", "canelitas", "emperador", "chokis", "marias",
                "oreo", "principes", "suavicremas", "submarinos", "gansito", "chocotorro", "dalmata",
                "pinguinos", "twinkies", "cuetara", "ritz", "saladitas"
            )
        )

        addWords(
            CAT_DULCERIA,
            listOf(
                "dulce", "dulces", "chocolate", "chocolates", "paleta", "paletas", "chicle", "chicles",
                "caramelo", "caramelos", "gomita", "gomitas", "mazapan", "mazapanes", "tamarindo",
                "tamarindos", "bombon", "bombones", "malvavisco", "oblea", "obleas", "delarosa",
                "ricolino", "vero", "lucas", "pulparindo", "duvalin", "kinder", "ferrero", "skwinkles",
                "bubbaloo", "clorets", "halls", "orbit", "trident", "tutsi", "coronado", "pelon",
                "krankys", "bocadin", "panditas", "bubulubu", "paleton", "snickers", "milkyway"
            )
        )

        addWords(
            CAT_ABARROTES,
            listOf(
                "arroz", "frijol", "frijoles", "azucar", "sal", "aceite", "aceites", "manteca",
                "harina", "harinas", "atun", "sardina", "sardinas", "pasta", "pastas", "sopa", "sopas",
                "mayonesa", "catsup", "mostaza", "vinagre", "consome", "mole", "adobo", "pimienta",
                "canela", "comino", "avena", "lenteja", "lentejas", "garbanzo", "garbanzos",
                "maseca", "minsa", "herdez", "knorr", "nutrioli", "capullo", "dolores", "barilla",
                "yemina", "mccormick", "hellmanns", "costena", "kelloggs", "nestle", "nescafe", "maizena"
            )
        )


        addWords(
            CAT_CUIDADO_PERSONAL,
            listOf(
                "shampoo", "acondicionador", "desodorante", "desodorantes", "talco", "rastrillo",
                "rastrillos", "panal", "panales", "tinte", "colgate", "palmolive", "sedal",
                "pantene", "suavel", "petalo", "kleenex", "kotex", "saba", "gillette", "nivea",
                "dove", "rexona", "axe", "regio", "charmin", "huggies", "pampers"
            )
        )

        addWords(
            CAT_CARNES,
            listOf(
                "jamon", "jamones", "salchicha", "salchichas", "tocino", "tocinos", "chorizo", "chorizos",
                "mortadela", "chuleta", "chuletas", "pechuga", "carne", "bistec", "molida", "longaniza",
                "cecina", "fud", "sanrafael", "chimex", "zwan", "bafar", "kir", "duby", "capistrano"
            )
        )

        addWords(
            CAT_CERVEZAS,
            listOf(
                "cerveza", "cervezas", "caguama", "caguamas", "vino", "vinos", "tequila", "tequilas",
                "ron", "rones", "whisky", "vodka", "brandy", "mezcal", "licor", "corona", "victoria",
                "modelo", "tecate", "pacifico", "indio", "heineken", "bohemia", "michelob", "ultra",
                "smirnoff", "bacardi"
            )
        )

        addWords(
            CAT_FRUTAS_VERDURAS,
            listOf(
                "jitomate", "tomate", "cebolla", "papa", "limon", "aguacate", "platano",
                "manzana", "naranja", "zanahoria", "chile", "cilantro", "perejil", "calabaza",
                "lechuga", "pepino", "mango", "papaya", "sandia", "melon", "uva", "fresa", "pina"
            )
        )

        addWords(
            CAT_MASCOTAS,
            listOf(
                "croquetas", "whiskas", "pedigree", "purina", "dogchow", "catchow", "campeon", "ganador"
            )
        )

    }

    /**
     * Tokenizes a product name:
     * - Strips units, weights and numbers (e.g. "1kg", "500g", "600ml")
     * - Normalizes diacritics / accents
     * - Removes punctuation
     * - Filters out common stopwords and short tokens (< 3 chars)
     */
    fun tokenize(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val withoutUnits = UNIT_REGEX.replace(text, " ")
        val withoutDigits = DIGITS_REGEX.replace(withoutUnits, " ")
        val normalized = withoutDigits.normalizeForSearch()
        val cleaned = NON_ALPHANUMERIC_REGEX.replace(normalized, " ")

        return cleaned.split("\\s+".toRegex())
            .map { it.trim() }
            .filter { it.length >= 3 && !STOPWORDS.contains(it) }
    }

    /**
     * Evaluates a product name and returns a ranked list of suggested categories,
     * ordered from most probable to least probable.
     *
     * @param productName The product title typed by the user.
     * @param existingCategories Categories currently present in the store.
     * @param existingProducts Existing catalog products (for historical pattern learning).
     * @param limit Maximum number of suggestions to return.
     * @return Ranked list of suggested category names.
     */
    fun suggestCategories(
        productName: String,
        existingCategories: List<String> = emptyList(),
        existingProducts: List<Products> = emptyList(),
        limit: Int = 4
    ): List<String> {
        val trimmed = productName.trim()
        if (trimmed.length < 3) return emptyList()

        val tokens = tokenize(trimmed)
        val normalizedRaw = trimmed.normalizeForSearch()
        if (tokens.isEmpty() && normalizedRaw.isEmpty()) return emptyList()

        val rawScores = mutableMapOf<String, Double>()

        // Level 1: Match against existing products in store catalog
        if (existingProducts.isNotEmpty() && tokens.isNotEmpty()) {
            for ((_, _, nombre, _, _, categoria) in existingProducts) {
                val cat = categoria?.trim()
                if (cat.isNullOrBlank() || cat.equals("Sin categoría", ignoreCase = true) || cat.equals("Sin categoria", ignoreCase = true)) {
                    continue
                }

                val prodTokens = tokenize(nombre)
                if (prodTokens.isEmpty()) continue

                var prodScore = 0.0
                var hasDistinctiveMatch = false
                for (token in tokens) {
                    if (prodTokens.contains(token)) {
                        if (GENERIC_MODIFIERS.contains(token)) {
                            prodScore += 0.5
                        } else {
                            prodScore += 2.5
                            hasDistinctiveMatch = true
                        }
                    }
                }

                if (hasDistinctiveMatch && prodScore > 0.0) {
                    val current = rawScores[cat] ?: 0.0
                    rawScores[cat] = minOf(15.0, current + prodScore)
                }
            }
        }

        // Level 2: Match against built-in retail taxonomy dictionary
        // A) Multi-word phrases
        for ((category, phrases) in MULTIWORD_PHRASES) {
            for (phrase in phrases) {
                if (normalizedRaw.contains(phrase)) {
                    rawScores[category] = (rawScores[category] ?: 0.0) + 6.0
                }
            }
        }

        // B) Single-word tokens & brands
        for (token in tokens) {
            val cat = KEYWORDS_MAP[token]
            if (cat != null) {
                rawScores[cat] = (rawScores[cat] ?: 0.0) + 3.0
            }
        }

        if (rawScores.isEmpty()) return emptyList()

        // Level 3: Resolve candidate categories against existing store categories
        val resolvedScores = mutableMapOf<String, Double>()
        for ((rawCat, score) in rawScores) {
            val resolved = resolveToExistingCategory(rawCat, existingCategories)
            resolvedScores[resolved] = (resolvedScores[resolved] ?: 0.0) + score
        }

        // Give a bonus (+1.0) to categories that already exist in the store's category list
        val storeCategoriesNorm = existingCategories
            .filter { it.isNotBlank() && !it.equals("Sin categoría", ignoreCase = true) && !it.equals("Sin categoria", ignoreCase = true) }
            .map { it.trim().lowercase() }
            .toSet()

        val finalScores = resolvedScores.mapValues { (cat, score) ->
            if (storeCategoriesNorm.contains(cat.trim().lowercase())) {
                score + 1.0
            } else {
                score
            }
        }

        return finalScores.entries
            .filter { it.value >= 1.5 }
            .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
            .map { it.key }
            .take(limit)
    }

    /**
     * Level 3: Resolves any predicted category to the store's existing category list.
     * Prevents duplicate/similar categories (e.g. mapping "Botanas" to "Frituras" if the store uses that).
     */
    fun resolveToExistingCategory(
        suggestedCategory: String,
        existingCategories: List<String>
    ): String {
        val validExisting = existingCategories.filter { it.isNotBlank() && !it.equals("Sin categoría", ignoreCase = true) && !it.equals("Sin categoria", ignoreCase = true) }
        if (validExisting.isEmpty()) return suggestedCategory

        val normSuggested = suggestedCategory.normalizeForSearch().trim()

        // 1. Exact match (ignoring accents & case)
        val exact = validExisting.firstOrNull { it.normalizeForSearch().trim() == normSuggested }
        if (exact != null) return exact

        // 2. Direct containment (e.g. "Botanas" matches "Botanas y Frituras")
        val contained = validExisting.firstOrNull {
            val normExist = it.normalizeForSearch().trim()
            normExist.contains(normSuggested) || normSuggested.contains(normExist)
        }
        if (contained != null) return contained

        // 3. Synonym group cluster match
        val synonymGroup = CATEGORY_SYNONYM_GROUPS.firstOrNull { group ->
            group.any { syn ->
                val normSyn = syn.normalizeForSearch()
                normSyn == normSuggested || normSuggested.contains(normSyn)
            }
        }
        if (synonymGroup != null) {
            val matchingExisting = validExisting.firstOrNull { cat ->
                val normCat = cat.normalizeForSearch().trim()
                synonymGroup.any { syn ->
                    val normSyn = syn.normalizeForSearch()
                    normCat == normSyn || normCat.contains(normSyn) || normSyn.contains(normCat)
                }
            }
            if (matchingExisting != null) return matchingExisting
        }

        // 4. Fuzzy Levenshtein match (tolerance <= 2 or similarity >= 0.75)
        val fuzzy = validExisting.firstOrNull { cat ->
            val normCat = cat.normalizeForSearch().trim()
            val dist = levenshteinDistance(normSuggested, normCat)
            val maxLen = maxOf(normSuggested.length, normCat.length)
            dist <= 2 || (maxLen > 0 && (1.0 - dist.toDouble() / maxLen) >= 0.75)
        }
        if (fuzzy != null) return fuzzy

        // 5. Fallback: Return suggested category
        return suggestedCategory
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val a = s1.lowercase()
        val b = s2.lowercase()
        val costs = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            costs[0] = i
            var nw = i - 1
            for (j in 1..b.length) {
                val cj = minOf(
                    1 + minOf(costs[j], costs[j - 1]),
                    if (a[i - 1] == b[j - 1]) nw else nw + 1
                )
                nw = costs[j]
                costs[j] = cj
            }
        }
        return costs[b.length]
    }
}
