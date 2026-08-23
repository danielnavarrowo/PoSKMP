package com.dnavarro.poskmp.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import poskmp.shared.generated.resources.Manrope
import poskmp.shared.generated.resources.Res

val TYPOGRAPHY = Typography()

@Composable
private fun createGoogleFlexFontFamily(): FontFamily = FontFamily(
    Font(Res.font.Manrope, weight = FontWeight.W100, variationSettings = FontVariation.Settings(FontVariation.weight(100))),
    Font(Res.font.Manrope, weight = FontWeight.W300, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(Res.font.Manrope, weight = FontWeight.W400, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(Res.font.Manrope, weight = FontWeight.W200, variationSettings = FontVariation.Settings(FontVariation.weight(200))),
    Font(Res.font.Manrope, weight = FontWeight.W500, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(Res.font.Manrope, weight = FontWeight.W600, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(Res.font.Manrope, weight = FontWeight.W700, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(Res.font.Manrope, weight = FontWeight.W800, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    Font(Res.font.Manrope, weight = FontWeight.W900, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)

val googleFlexDisplay: FontFamily
    @Composable get() = createGoogleFlexFontFamily()

val googleFlexHeadline: FontFamily
    @Composable get() = createGoogleFlexFontFamily()

val googleFlexTitle: FontFamily
    @Composable get() = createGoogleFlexFontFamily()

val googleFlexBody: FontFamily
    @Composable get() = createGoogleFlexFontFamily()

val googleFlexLabel: FontFamily
    @Composable get() = createGoogleFlexFontFamily()


val AppTypography: Typography
    @Composable get() = Typography(
        displayLarge = TYPOGRAPHY.displayLarge.copy(
            fontFamily = googleFlexDisplay,
        ),
        displayMedium = TYPOGRAPHY.displayMedium.copy(
            fontFamily = googleFlexDisplay,
        ),
        displaySmall = TYPOGRAPHY.displaySmall.copy(
            fontFamily = googleFlexDisplay,
        ),
        headlineLarge = TYPOGRAPHY.headlineLarge.copy(
            fontFamily = googleFlexHeadline,
        ),
        headlineMedium = TYPOGRAPHY.headlineMedium.copy(
            fontFamily = googleFlexHeadline,
        ),
        headlineSmall = TYPOGRAPHY.headlineSmall.copy(
            fontFamily = googleFlexHeadline,
        ),
        titleLarge = TYPOGRAPHY.titleLarge.copy(
            fontFamily = googleFlexTitle,
        ),
        titleMedium = TYPOGRAPHY.titleMedium.copy(
            fontFamily = googleFlexTitle,
        ),
        titleSmall = TYPOGRAPHY.titleSmall.copy(
            fontFamily = googleFlexTitle,
        ),
        bodyLarge = TYPOGRAPHY.bodyLarge.copy(
            fontFamily = googleFlexBody,
        ),
        bodyMedium = TYPOGRAPHY.bodyMedium.copy(
            fontFamily = googleFlexBody,
        ),
        bodySmall = TYPOGRAPHY.bodySmall.copy(
            fontFamily = googleFlexBody,
        ),
        labelLarge = TYPOGRAPHY.labelLarge.copy(
            fontFamily = googleFlexLabel,
        ),
        labelMedium = TYPOGRAPHY.labelMedium.copy(
            fontFamily = googleFlexLabel,
        ),
        labelSmall = TYPOGRAPHY.labelSmall.copy(
            fontFamily = googleFlexLabel,
        )
    )

