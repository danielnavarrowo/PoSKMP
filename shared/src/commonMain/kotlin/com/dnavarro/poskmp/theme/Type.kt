package com.dnavarro.poskmp.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import org.jetbrains.compose.resources.Font
import poskmp.shared.generated.resources.GoogleSansFlex
import poskmp.shared.generated.resources.Res

val TYPOGRAPHY = Typography()

@OptIn(ExperimentalTextApi::class)
@Composable
private fun createGoogleFlexFontFamily(
    width: Float = 100f,
    slant: Float = 0f,
    grade: Int = 0
): FontFamily = FontFamily(
    Font(Res.font.GoogleSansFlex, weight = FontWeight.W100, variationSettings = FontVariation.Settings(FontVariation.weight(100), FontVariation.width(width), FontVariation.slant(slant), FontVariation.grade(grade))),
    Font(Res.font.GoogleSansFlex, weight = FontWeight.W200, variationSettings = FontVariation.Settings(FontVariation.weight(200), FontVariation.width(width), FontVariation.slant(slant), FontVariation.grade(grade))),
    Font(Res.font.GoogleSansFlex, weight = FontWeight.W300, variationSettings = FontVariation.Settings(FontVariation.weight(300), FontVariation.width(width), FontVariation.slant(slant), FontVariation.grade(grade))),
    Font(Res.font.GoogleSansFlex, weight = FontWeight.W400, variationSettings = FontVariation.Settings(FontVariation.weight(400), FontVariation.width(width), FontVariation.slant(slant), FontVariation.grade(grade))),
    Font(Res.font.GoogleSansFlex, weight = FontWeight.W500, variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.width(width), FontVariation.slant(slant), FontVariation.grade(grade))),
    Font(Res.font.GoogleSansFlex, weight = FontWeight.W600, variationSettings = FontVariation.Settings(FontVariation.weight(600), FontVariation.width(width), FontVariation.slant(slant), FontVariation.grade(grade))),
    Font(Res.font.GoogleSansFlex, weight = FontWeight.W700, variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.width(width), FontVariation.slant(slant), FontVariation.grade(grade))),
    Font(Res.font.GoogleSansFlex, weight = FontWeight.W800, variationSettings = FontVariation.Settings(FontVariation.weight(800), FontVariation.width(width), FontVariation.slant(slant), FontVariation.grade(grade))),
    Font(Res.font.GoogleSansFlex, weight = FontWeight.W900, variationSettings = FontVariation.Settings(FontVariation.weight(900), FontVariation.width(width), FontVariation.slant(slant), FontVariation.grade(grade))),
)

val googleFlexDisplay: FontFamily
    @Composable get() = createGoogleFlexFontFamily(width = 150f, slant = -10f, grade = 100)

val googleFlexHeadline: FontFamily
    @Composable get() = createGoogleFlexFontFamily(width = 130f, slant = 0f, grade = 50)

val googleFlexTitle: FontFamily
    @Composable get() = createGoogleFlexFontFamily(width = 95f, slant = 0f, grade = 50)

val googleFlexBody: FontFamily
    @Composable get() = createGoogleFlexFontFamily(width = 100f, slant = 0f, grade = 0)

val googleFlexLabel: FontFamily
    @Composable get() = createGoogleFlexFontFamily(width = 95f, slant = 0f, grade = 10)

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

