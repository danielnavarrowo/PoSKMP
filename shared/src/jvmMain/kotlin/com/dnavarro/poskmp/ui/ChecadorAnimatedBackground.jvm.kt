package com.dnavarro.poskmp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import java.time.LocalDate
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

data class ChecadorShaderTheme(
    val id: String,
    val name: String,
    val color1: Color,
    val color2: Color,
    val color3: Color,
    val color4: Color
)

val CHECADOR_THEMES = listOf(
    ChecadorShaderTheme(
        id = "sunset_glow",
        name = "Sunset Glow",
        color1 = Color(0xFFFC8009), // Electric Mango Orange
        color2 = Color(0xFFFF3366), // Vivid Coral Rose
        color3 = Color(0xFF10A4B3), // Vivid Oceanic Teal
        color4 = Color(0xFF7928CA)  // Electric Violet
    ),
    ChecadorShaderTheme(
        id = "cyber_neon",
        name = "Cyber Neon",
        color1 = Color(0xFFFF007F), // Electric Hot Magenta
        color2 = Color(0xFF7B2CBF), // Vivid Royal Violet
        color3 = Color(0xFF00F0FF), // Neon Cyan
        color4 = Color(0xFF0051FF)  // Electric Cobalt Blue
    ),
    ChecadorShaderTheme(
        id = "aurora_borealis",
        name = "Aurora Borealis",
        color1 = Color(0xFF00F5D4), // Electric Aquamarine
        color2 = Color(0xFF00BBF9), // Vivid Sky Blue
        color3 = Color(0xFF4361EE), // Vivid Indigo Royal
        color4 = Color(0xFF05D550)  // Vivid Emerald Neon
    ),
    ChecadorShaderTheme(
        id = "cosmic_berry",
        name = "Cosmic Berry",
        color1 = Color(0xFF9D4EDD), // Vivid Purple Orchid
        color2 = Color(0xFFF72585), // Vivid Neon Cerise
        color3 = Color(0xFF3A0CA3), // Deep Electric Navy Violet
        color4 = Color(0xFFFF4D6D)  // Vivid Flamingo Pink
    ),
    ChecadorShaderTheme(
        id = "tropical_lagoon",
        name = "Tropical Lagoon",
        color1 = Color(0xFF06D6A0), // Vibrant Tropical Seafoam
        color2 = Color(0xFF118AB2), // Vibrant Ocean Cyan
        color3 = Color(0xFF073B4C), // Deep Marine Navy
        color4 = Color(0xFF00B4D8)  // Vivid Caribbean Azure
    ),
    ChecadorShaderTheme(
        id = "fiery_magma",
        name = "Fiery Magma",
        color1 = Color(0xFFFF4800), // Blazing Flame Orange
        color2 = Color(0xFFFF0054), // Vivid Ruby Crimson
        color3 = Color(0xFF7000FF), // Electric Violet
        color4 = Color(0xFFFF8500)  // Vivid Amber Glow
    ),
    ChecadorShaderTheme(
        id = "electric_twilight",
        name = "Electric Twilight",
        color1 = Color(0xFF6A00F4), // Electric Violet
        color2 = Color(0xFF2D00F7), // Electric Ultramarine
        color3 = Color(0xFFFF007F), // Vivid Neon Fuchsia
        color4 = Color(0xFF00F5D4)  // Neon Mint Glow
    ),
    ChecadorShaderTheme(
        id = "peacock_jewel",
        name = "Peacock Jewel",
        color1 = Color(0xFF0096C7), // Vivid Jewel Blue
        color2 = Color(0xFF00A86B), // Vivid Jade Emerald
        color3 = Color(0xFF6A0DAD), // Vivid Royal Purple
        color4 = Color(0xFF0077B6)  // Deep Cerulean
    ),
    ChecadorShaderTheme(
        id = "vibrant_citrus",
        name = "Vibrant Citrus",
        color1 = Color(0xFFFF6D00), // Vivid Tangerine
        color2 = Color(0xFFFF1493), // Deep Pink
        color3 = Color(0xFF00C896), // Vivid Persian Green
        color4 = Color(0xFF6200EA)  // Vivid Deep Violet
    ),
    ChecadorShaderTheme(
        id = "caribbean_sunset",
        name = "Caribbean Sunset",
        color1 = Color(0xFFFF5E7E), // Vivid Coral Sunset
        color2 = Color(0xFFFF9900), // Vivid Golden Orange
        color3 = Color(0xFF00A8FF), // Vivid Caribbean Blue
        color4 = Color(0xFF5352ED)  // Vivid Royal Periwinkle
    )
)

fun getDailyChecadorTheme(date: LocalDate = LocalDate.now()): ChecadorShaderTheme {
    val epochDay = date.toEpochDay()
    val size = CHECADOR_THEMES.size
    val cycle = Math.floorDiv(epochDay, size.toLong())
    val pos = Math.floorMod(epochDay, size.toLong()).toInt()
    val cyclePermutation = getCyclePermutation(cycle, size)
    return CHECADOR_THEMES[cyclePermutation[pos]]
}

internal fun getCyclePermutation(cycle: Long, size: Int): IntArray {
    val rng = Random(cycle * 31337L)
    val list = (0 until size).toMutableList()
    for (i in list.size - 1 downTo 1) {
        val j = rng.nextInt(i + 1)
        val temp = list[i]; list[i] = list[j]; list[j] = temp
    }
    val prevRng = Random((cycle - 1) * 31337L)
    val prevList = (0 until size).toMutableList()
    for (i in prevList.size - 1 downTo 1) {
        val j = prevRng.nextInt(i + 1)
        val temp = prevList[i]; prevList[i] = prevList[j]; prevList[j] = temp
    }
    val prevLast = prevList.last()
    if (list[0] == prevLast && list.size > 1) {
        val temp = list[0]; list[0] = list[1]; list[1] = temp
    }
    return list.toIntArray()
}

internal const val CHECADOR_SKSL_SHADER = """
uniform float2 uResolution;
uniform float uTime;
uniform vec3 uColor1;
uniform vec3 uColor2;
uniform vec3 uColor3;
uniform vec3 uColor4;

vec4 main(vec2 fragCoord) {
    vec2 uv = fragCoord / uResolution;
    float t = uTime * 0.20;

    // 4 slowly drifting color poles
    vec2 c1 = vec2(0.20 + 0.15 * sin(t), 0.25 + 0.15 * cos(t * 0.8));
    vec2 c2 = vec2(0.80 + 0.12 * cos(t * 0.9), 0.25 + 0.15 * sin(t * 0.7));
    vec2 c3 = vec2(0.25 + 0.15 * cos(t * 0.7), 0.80 + 0.12 * sin(t * 0.9));
    vec2 c4 = vec2(0.85 + 0.12 * sin(t * 0.8), 0.75 + 0.12 * cos(t * 0.6));

    // Fast squared-distance inverse falloffs (no sqrt or pow)
    vec2 d1 = uv - c1;
    vec2 d2 = uv - c2;
    vec2 d3 = uv - c3;
    vec2 d4 = uv - c4;

    float w1 = 1.0 / (0.18 + dot(d1, d1));
    float w2 = 1.0 / (0.18 + dot(d2, d2));
    float w3 = 1.0 / (0.18 + dot(d3, d3));
    float w4 = 1.0 / (0.18 + dot(d4, d4));

    float total = w1 + w2 + w3 + w4;

    vec3 color = (uColor1 * w1 + uColor2 * w2 + uColor3 * w3 + uColor4 * w4) / total;
    return vec4(color, 1.0);
}
"""

@Composable
actual fun ChecadorAnimatedBackground(
    modifier: Modifier
) {
    var time by remember { mutableFloatStateOf(0f) }
    var currentDay by remember { mutableStateOf(LocalDate.now().toEpochDay()) }
    val currentTheme = remember(currentDay) {
        getDailyChecadorTheme(LocalDate.ofEpochDay(currentDay))
    }

    // Low refresh rate (~10 FPS): sleeps 100ms per update to minimize CPU/GPU/battery usage
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(100L.milliseconds)
            time += 0.10f
            val today = LocalDate.now().toEpochDay()
            if (today != currentDay) {
                currentDay = today
            }
        }
    }

    val effect = remember {
        RuntimeEffect.makeForShader(CHECADOR_SKSL_SHADER)
    }
    val builder = remember(effect) {
        RuntimeShaderBuilder(effect)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width > 0f && size.height > 0f) {
            builder.uniform("uResolution", size.width, size.height)
            builder.uniform("uTime", time)
            builder.uniform("uColor1", currentTheme.color1.red, currentTheme.color1.green, currentTheme.color1.blue)
            builder.uniform("uColor2", currentTheme.color2.red, currentTheme.color2.green, currentTheme.color2.blue)
            builder.uniform("uColor3", currentTheme.color3.red, currentTheme.color3.green, currentTheme.color3.blue)
            builder.uniform("uColor4", currentTheme.color4.red, currentTheme.color4.green, currentTheme.color4.blue)
            val shader = builder.makeShader().asComposeShader()
            drawRect(brush = ShaderBrush(shader))
        }
    }
}
