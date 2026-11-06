package com.dnavarro.poskmp.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import poskmp.shared.generated.resources.GoogleSansFlex
import poskmp.shared.generated.resources.Res

val TYPOGRAPHY = Typography()

/**
 * Builds a [FontFamily] from the Google Sans Flex variable font (`ROND`, `opsz`, `slnt`, `wdth`, `wght`),
 * mapping all 9 standard weights (`W100`–`W900`) in both Normal and Italic styles to custom variable axes.
 *
 * Supported axis ranges in Google Sans Flex:
 * - `wght` (Weight): 1..1000 (default 400)
 * - `wdth` (Width): 25f..151f (default 100f)
 * - `opsz` (Optical Size): 1f..144f (default 18f)
 * - `ROND` (Roundness): 0f..100f (default 0f)
 * - `slnt` (Slant): -10f..0f (default 0f)
 */
@Composable
fun createGoogleFlexFontFamily(
    width: Float = 100f,
    roundness: Float = 0f,
    opticalSize: Float = 18f,
    slant: Float = 0f,
    weightOffset: Int = 0
): FontFamily {
    val clampedWidth = width.coerceIn(25f, 151f)
    val clampedRoundness = roundness.coerceIn(0f, 100f)
    val clampedOpsz = opticalSize.coerceIn(1f, 144f)
    val clampedSlant = slant.coerceIn(-10f, 0f)

    fun settingsFor(baseWeight: Int, styleSlant: Float) = FontVariation.Settings(
        FontVariation.weight((baseWeight + weightOffset).coerceIn(1, 1000)),
        FontVariation.width(clampedWidth),
        FontVariation.slant(styleSlant.coerceIn(-10f, 0f)),
        FontVariation.Setting("ROND", clampedRoundness),
        FontVariation.Setting("opsz", clampedOpsz)
    )

    val weights = listOf(
        FontWeight.W100 to 100,
        FontWeight.W200 to 200,
        FontWeight.W300 to 300,
        FontWeight.W400 to 400,
        FontWeight.W500 to 500,
        FontWeight.W600 to 600,
        FontWeight.W700 to 700,
        FontWeight.W800 to 800,
        FontWeight.W900 to 900
    )

    return FontFamily(
        weights.flatMap { (fontWeight, numericWeight) ->
            listOf(
                Font(
                    resource = Res.font.GoogleSansFlex,
                    weight = fontWeight,
                    style = FontStyle.Normal,
                    variationSettings = settingsFor(numericWeight, clampedSlant)
                ),
                Font(
                    resource = Res.font.GoogleSansFlex,
                    weight = fontWeight,
                    style = FontStyle.Italic,
                    variationSettings = settingsFor(numericWeight, -10f)
                )
            )
        }
    )
}

/** Wide, rounded, high-optical-size display voice for hero headers and large banners. */
val googleFlexDisplay: FontFamily
    @Composable get() = createGoogleFlexFontFamily(
        width = 104f,
        roundness = 85f,
        opticalSize = 96f,
        slant = 0f
    )

/** Extra-expressive hero display voice: expanded width, full roundness, and subtle forward slant. */
val googleFlexHero: FontFamily
    @Composable get() = createGoogleFlexFontFamily(
        width = 144f,
        roundness = 100f,
        opticalSize = 120f,
        slant = -5f,
        weightOffset = 60
    )

/** Confident, open-width headline voice with warm rounded terminals for screen & dialog headers. */
val googleFlexHeadline: FontFamily
    @Composable get() = createGoogleFlexFontFamily(
        width = 115f,
        roundness = 65f,
        opticalSize = 42f,
        slant = 0f
    )

/** Emphasized headline voice with expanded proportions and fuller roundness. */
val googleFlexHeadlineEmphasized: FontFamily
    @Composable get() = createGoogleFlexFontFamily(
        width = 126f,
        roundness = 90f,
        opticalSize = 48f,
        slant = -3f,
        weightOffset = 50
    )

/** Crisp, slightly compact title voice with balanced roundness for section headers and product names. */
val googleFlexTitle: FontFamily
    @Composable get() = createGoogleFlexFontFamily(
        width = 96f,
        roundness = 45f,
        opticalSize = 24f,
        slant = 0f
    )

/** Emphasized title voice for active navigation items, key card titles, and prominent dialogs. */
val googleFlexTitleEmphasized: FontFamily
    @Composable get() = createGoogleFlexFontFamily(
        width = 105f,
        roundness = 75f,
        opticalSize = 28f,
        slant = 0f,
        weightOffset = 50
    )

/** Balanced, highly legible body voice with subtle terminal softness. */
val googleFlexBody: FontFamily
    @Composable get() = createGoogleFlexFontFamily(
        width = 100f,
        roundness = 25f,
        opticalSize = 16f,
        slant = 0f
    )

/** Emphasized body voice for highlighted table cells and secondary callouts. */
val googleFlexBodyEmphasized: FontFamily
    @Composable get() = createGoogleFlexFontFamily(
        width = 102f,
        roundness = 45f,
        opticalSize = 16f,
        slant = 0f,
        weightOffset = 50
    )

/** Space-efficient, low-optical-size label voice for buttons, table headers, and captions. */
val googleFlexLabel: FontFamily
    @Composable get() = createGoogleFlexFontFamily(
        width = 93f,
        roundness = 60f,
        opticalSize = 11f,
        slant = 0f
    )

/** Expressive pill/badge voice: compact, fully rounded, and subtly slanted for tags and tier chips. */
val googleFlexBadge: FontFamily
    @Composable get() = createGoogleFlexFontFamily(
        width = 88f,
        roundness = 100f,
        opticalSize = 10f,
        slant = -5f,
        weightOffset = 80
    )

/** Sculpted numeric voice tailored for POS currency totals, prices, and KPI metrics. */
val googleFlexNumeric: FontFamily
    @Composable get() = createGoogleFlexFontFamily(
        width = 110f,
        roundness = 80f,
        opticalSize = 36f,
        slant = 0f,
        weightOffset = 30
    )

/** High-density condensed voice for SKUs, barcode lists, folios, and compact data columns. */
val googleFlexCondensed: FontFamily
    @Composable get() = createGoogleFlexFontFamily(
        width = 84f,
        roundness = 30f,
        opticalSize = 13f,
        slant = 0f
    )

val AppTypography: Typography
    @Composable get() {
        val displayFam = googleFlexDisplay
        val heroFam = googleFlexHero
        val headlineFam = googleFlexHeadline
        val headlineEmpFam = googleFlexHeadlineEmphasized
        val titleFam = googleFlexTitle
        val titleEmpFam = googleFlexTitleEmphasized
        val bodyFam = googleFlexBody
        val bodyEmpFam = googleFlexBodyEmphasized
        val labelFam = googleFlexLabel
        val badgeFam = googleFlexBadge

        return Typography(
            displayLarge = TYPOGRAPHY.displayLarge.copy(
                fontFamily = displayFam,
                fontWeight = FontWeight.W800,
                letterSpacing = (-0.5).sp
            ),
            displayMedium = TYPOGRAPHY.displayMedium.copy(
                fontFamily = displayFam,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.25).sp
            ),
            displaySmall = TYPOGRAPHY.displaySmall.copy(
                fontFamily = displayFam,
                fontWeight = FontWeight.W700
            ),
            headlineLarge = TYPOGRAPHY.headlineLarge.copy(
                fontFamily = headlineFam,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.25).sp
            ),
            headlineMedium = TYPOGRAPHY.headlineMedium.copy(
                fontFamily = headlineFam,
                fontWeight = FontWeight.W600
            ),
            headlineSmall = TYPOGRAPHY.headlineSmall.copy(
                fontFamily = headlineFam,
                fontWeight = FontWeight.W600
            ),
            titleLarge = TYPOGRAPHY.titleLarge.copy(
                fontFamily = titleFam,
                fontWeight = FontWeight.W600
            ),
            titleMedium = TYPOGRAPHY.titleMedium.copy(
                fontFamily = titleFam,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.1.sp
            ),
            titleSmall = TYPOGRAPHY.titleSmall.copy(
                fontFamily = titleFam,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.1.sp
            ),
            bodyLarge = TYPOGRAPHY.bodyLarge.copy(
                fontFamily = bodyFam,
                fontWeight = FontWeight.W400
            ),
            bodyMedium = TYPOGRAPHY.bodyMedium.copy(
                fontFamily = bodyFam,
                fontWeight = FontWeight.W400
            ),
            bodySmall = TYPOGRAPHY.bodySmall.copy(
                fontFamily = bodyFam,
                fontWeight = FontWeight.W300
            ),
            labelLarge = TYPOGRAPHY.labelLarge.copy(
                fontFamily = labelFam,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.15.sp
            ),
            labelMedium = TYPOGRAPHY.labelMedium.copy(
                fontFamily = labelFam,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.25.sp
            ),
            labelSmall = TYPOGRAPHY.labelSmall.copy(
                fontFamily = labelFam,
                fontWeight = FontWeight.W500,
                letterSpacing = 0.3.sp
            ),
            displayLargeEmphasized = TYPOGRAPHY.displayLargeEmphasized.copy(
                fontFamily = heroFam,
                fontWeight = FontWeight.W900,
                letterSpacing = (-0.75).sp
            ),
            displayMediumEmphasized = TYPOGRAPHY.displayMediumEmphasized.copy(
                fontFamily = heroFam,
                fontWeight = FontWeight.W800,
                letterSpacing = (-0.5).sp
            ),
            displaySmallEmphasized = TYPOGRAPHY.displaySmallEmphasized.copy(
                fontFamily = heroFam,
                fontWeight = FontWeight.W800
            ),
            headlineLargeEmphasized = TYPOGRAPHY.headlineLargeEmphasized.copy(
                fontFamily = headlineEmpFam,
                fontWeight = FontWeight.W800,
                letterSpacing = (-0.25).sp
            ),
            headlineMediumEmphasized = TYPOGRAPHY.headlineMediumEmphasized.copy(
                fontFamily = headlineEmpFam,
                fontWeight = FontWeight.W700
            ),
            headlineSmallEmphasized = TYPOGRAPHY.headlineSmallEmphasized.copy(
                fontFamily = headlineEmpFam,
                fontWeight = FontWeight.W700
            ),
            titleLargeEmphasized = TYPOGRAPHY.titleLargeEmphasized.copy(
                fontFamily = titleEmpFam,
                fontWeight = FontWeight.W700
            ),
            titleMediumEmphasized = TYPOGRAPHY.titleMediumEmphasized.copy(
                fontFamily = titleEmpFam,
                fontWeight = FontWeight.W700
            ),
            titleSmallEmphasized = TYPOGRAPHY.titleSmallEmphasized.copy(
                fontFamily = titleEmpFam,
                fontWeight = FontWeight.W700
            ),
            bodyLargeEmphasized = TYPOGRAPHY.bodyLargeEmphasized.copy(
                fontFamily = bodyEmpFam,
                fontWeight = FontWeight.W600
            ),
            bodyMediumEmphasized = TYPOGRAPHY.bodyMediumEmphasized.copy(
                fontFamily = bodyEmpFam,
                fontWeight = FontWeight.W600
            ),
            bodySmallEmphasized = TYPOGRAPHY.bodySmallEmphasized.copy(
                fontFamily = bodyEmpFam,
                fontWeight = FontWeight.W500
            ),
            labelLargeEmphasized = TYPOGRAPHY.labelLargeEmphasized.copy(
                fontFamily = badgeFam,
                fontWeight = FontWeight.W700,
                letterSpacing = 0.2.sp
            ),
            labelMediumEmphasized = TYPOGRAPHY.labelMediumEmphasized.copy(
                fontFamily = badgeFam,
                fontWeight = FontWeight.W700,
                letterSpacing = 0.3.sp
            ),
            labelSmallEmphasized = TYPOGRAPHY.labelSmallEmphasized.copy(
                fontFamily = badgeFam,
                fontWeight = FontWeight.W700,
                letterSpacing = 0.35.sp
            )
        )
    }
