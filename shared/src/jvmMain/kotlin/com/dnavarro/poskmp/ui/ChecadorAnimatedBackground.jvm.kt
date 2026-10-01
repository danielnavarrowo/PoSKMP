package com.dnavarro.poskmp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
        id = "glacier",
        name = "glacier",
        color1 = Color(0xFF489aea), // #3b82f6
        color2 = Color(0xFF2ed4e3), // #a855f7
        color3 = Color(0xFF8BDEFF), // #22d3ee
        color4 = Color(0xFF3c67be)  // #f43f5e
    ),
    ChecadorShaderTheme(
        id = "verdigris",
        name = "verdigris",
        color1 = Color(0xFF3c9e8f), // #8b5cf6
        color2 = Color(0xFF61c4ab), // #ec4899
        color3 = Color(0xFFd4bc73), // #4cbbe8
        color4 = Color(0xFF236767)  // #fbbf24
    ),
    ChecadorShaderTheme(
        id = "ember",
        name = "Ember",
        color1 = Color(0xFFFB7322), // #fb7322
        color2 = Color(0xFFF43F5E), // #f43f5e
        color3 = Color(0xFFF69E0B), // #f69e0b
        color4 = Color(0xFF8F264E)  // #8f264e
    ),
    ChecadorShaderTheme(
        id = "nocturne",
        name = "nocturne",
        color1 = Color(0xFF2d51ab), // #129cb1
        color2 = Color(0xFF4184d5), // #2d73cd
        color3 = Color(0xFF73aeea), // #8f5bcf
        color4 = Color(0xFF1c2d64)  // #f6bc49
    ),
    ChecadorShaderTheme(
        id = "coral",
        name = "coral",
        color1 = Color(0xFFfa7367), // #12f3d4
        color2 = Color(0xFFfd9a73), // #f639ae
        color3 = Color(0xFFfebe9c), // #aef73c
        color4 = Color(0xFFbe3f50)  // #396bf6
    ),
    ChecadorShaderTheme(
        id = "wine",
        name = "Wine",
        color1 = Color(0xFFA82649), // #a82649
        color2 = Color(0xFFD43F5B), // #d43f5b
        color3 = Color(0xFF8A2E73), // #8a2e73
        color4 = Color(0xFF611732)  // #611732
    ),
    ChecadorShaderTheme(
        id = "abyssal",
        name = "Abyssal",
        color1 = Color(0xFF0A848D), // #0a848d
        color2 = Color(0xFF1FBCAA), // #1fbcaa
        color3 = Color(0xFF9DEAC6), // #9deac6
        color4 = Color(0xFF104973)  // #104973
    ),
    ChecadorShaderTheme(
        id = "dusk",
        name = "dusk",
        color1 = Color(0xFF6b79ab), // #38d4d9
        color2 = Color(0xFFb08594), // #61c47b
        color3 = Color(0xFFdeaa7f), // #9c6bea
        color4 = Color(0xFF4a5179)  // #4679f2
    ),
    ChecadorShaderTheme(
        id = "amethyst",
        name = "Amethyst",
        color1 = Color(0xFF7E51D4), // #7e51d4
        color2 = Color(0xFFA86FF2), // #a86ff2
        color3 = Color(0xFF6194F2), // #6194f2
        color4 = Color(0xFF4A2E8F)  // #4a2e8f
    ),
    ChecadorShaderTheme(
        id = "prism",
        name = "Prism",
        color1 = Color(0xFF94c6f2), // #60a5f2
        color2 = Color(0xFFc4b0f2), // #58d89c
        color3 = Color(0xFFf2bed5), // #f6ca51
        color4 = Color(0xFFaeeade)  // #f26878
    ),
    ChecadorShaderTheme(
        id = "midnight",
        name = "Midnight",
        color1 = Color(0xFF3B82F6), // #3b82f6
        color2 = Color(0xFFA855F7), // #a855f7
        color3 = Color(0xFF22D3EE), // #22d3ee
        color4 = Color(0xFFF43F5E)  // #f43f5e
    ),
    ChecadorShaderTheme(
        id = "witchlight",
        name = "Witchlight",
        color1 = Color(0xFF8B5CF6), // #8b5cf6
        color2 = Color(0xFFEC4899), // #ec4899
        color3 = Color(0xFF4CBBE8), // #4cbbe8
        color4 = Color(0xFFFBBF24)  // #fbbf24
    ),
    ChecadorShaderTheme(
        id = "peacock",
        name = "Peacock",
        color1 = Color(0xFF129CB1), // #129cb1
        color2 = Color(0xFF2D73CD), // #2d73cd
        color3 = Color(0xFF8F5BCF), // #8f5bcf
        color4 = Color(0xFFF6BC49)  // #f6bc49
    ),
    ChecadorShaderTheme(
        id = "neon",
        name = "Neon",
        color1 = Color(0xFF12F3D4), // #12f3d4
        color2 = Color(0xFFF639AE), // #f639ae
        color3 = Color(0xFFAEF73C), // #aef73c
        color4 = Color(0xFF396BF6)  // #396bf6
    ),
    ChecadorShaderTheme(
        id = "spectral",
        name = "Spectral",
        color1 = Color(0xFF38D4D9), // #38d4d9
        color2 = Color(0xFF61C47B), // #61c47b
        color3 = Color(0xFF9C6BEA), // #9c6bea
        color4 = Color(0xFF4679F2)  // #4679f2
    ),
    ChecadorShaderTheme(
        id = "fuchsia",
        name = "Fuchsia",
        color1 = Color(0xFFE3389E), // #e3389e
        color2 = Color(0xFFF964BD), // #f964bd
        color3 = Color(0xFFA849E3), // #a849e3
        color4 = Color(0xFF841C67)  // #841c67
    ),
    ChecadorShaderTheme(
        id = "viridian",
        name = "Viridian",
        color1 = Color(0xFF22AF71), // #22af71
        color2 = Color(0xFF58D885), // #58d885
        color3 = Color(0xFFB8F3AA), // #b8f3aa
        color4 = Color(0xFF11675A)  // #11675a
    ),
    ChecadorShaderTheme(
        id = "foundry",
        name = "Foundry",
        color1 = Color(0xFFF98326), // #f98326
        color2 = Color(0xFFF6B02E), // #f6b02e
        color3 = Color(0xFFFED56B), // #fed56b
        color4 = Color(0xFF9E3C1C)  // #9e3c1c
    ),
    ChecadorShaderTheme(
        id = "tide",
        name = "Tide",
        color1 = Color(0xFF06B6B0), // #06b6b0
        color2 = Color(0xFF4CD9B4), // #4cd9b4
        color3 = Color(0xFFA8F3D5), // #a8f3d5
        color4 = Color(0xFF136F84)  // #136f84
    ),
    ChecadorShaderTheme(
        id = "daybreak",
        name = "Daybreak",
        color1 = Color(0xFFFB9C67), // #fb9c67
        color2 = Color(0xFFFDC671), // #fdc671
        color3 = Color(0xFFF6828D), // #f6828d
        color4 = Color(0xFFAE8ECE)  // #ae8ece
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

const float uDriftSpeed = 0.04;
const float uSoftness   = 0.85;
const float uWarp       = 0.95;
const float uHeat       = 0.70;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float s = 0.0;
    float a = 0.55;
    for (int i = 0; i < 4; i++) {
        s += a * vnoise(p);
        p = vec2(0.80 * p.x - 0.60 * p.y, 0.60 * p.x + 0.80 * p.y) * 2.03 + vec2(11.3, 7.1);
        a *= 0.5;
    }
    return s;
}

float poleW(vec2 pos, vec2 c, float r) {
    vec2 d = pos - c;
    return exp(-dot(d, d) / max(r * r, 0.001));
}

vec4 main(vec2 fragCoord) {
    vec3 c0 = uColor1;
    vec3 c1 = uColor2;
    vec3 c2 = uColor3;
    vec3 c3 = uColor4;

    vec2 uv = fragCoord / uResolution;
    float aspect = uResolution.x / uResolution.y;
    vec2 pos = vec2((uv.x - 0.5) * aspect, uv.y - 0.5);

    float t = uTime * uDriftSpeed;

    // turbulent warp — welter roiling edges
    vec2 wv = vec2(fbm(pos * 1.9 + t * 0.6), fbm(pos * 1.9 + vec2(4.0, 9.0) - t * 0.5)) - 0.5;
    pos += wv * (0.55 * uWarp);

    // five molten poles rising and rolling
    vec2 P0 = vec2(-0.5,  0.55) + vec2(sin(t * 0.9 + 0.0), cos(t * 1.1 + 1.0)) * 0.40;
    vec2 P1 = vec2( 0.55, 0.20) + vec2(sin(t * 0.7 + 2.1), cos(t * 1.3 + 0.4)) * 0.42;
    vec2 P2 = vec2(-0.10,-0.50) + vec2(sin(t * 1.2 + 4.0), cos(t * 0.8 + 3.0)) * 0.44;
    vec2 P3 = vec2( 0.45,-0.30) + vec2(sin(t * 0.8 + 5.5), cos(t * 1.0 + 2.4)) * 0.40;
    vec2 P4 = vec2(-0.45,-0.05) + vec2(sin(t * 1.05 + 1.3), cos(t * 0.9 + 4.2)) * 0.42;

    float r = 0.42 * max(uSoftness, 0.25);
    float w0 = poleW(pos, P0, r);
    float w1 = poleW(pos, P1, r);
    float w2 = poleW(pos, P2, r);
    float w3 = poleW(pos, P3, r);
    float w4 = poleW(pos, P4, r);
    float wsum = w0 + w1 + w2 + w3 + w4 + 0.001;

    vec3 col = (w0 * c0 + w1 * c1 + w2 * c2 + w3 * c3 + w4 * c2) / wsum;

    float hot = clamp((max(max(w0, w1), max(w2, max(w3, w4))) / wsum) * 1.6, 0.0, 1.0);
    col *= mix(0.62, 1.18, pow(hot, mix(1.0, 2.2, uHeat)));
    col += c2 * 0.18 * uHeat * smoothstep(0.55, 1.0, hot);

    return vec4(col, 1.0);
}
"""

@Composable
actual fun ChecadorAnimatedBackground(
    modifier: Modifier
) {
    var time by remember { mutableFloatStateOf(0f) }
    var currentDay by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
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
