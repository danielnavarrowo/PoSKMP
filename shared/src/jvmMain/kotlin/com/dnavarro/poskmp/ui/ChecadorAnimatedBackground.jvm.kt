package com.dnavarro.poskmp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.time.Duration.Companion.milliseconds

private const val CHECADOR_SKSL_SHADER = """
uniform float2 uResolution;
uniform float uTime;

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

    vec3 col1 = vec3(0.988, 0.502, 0.035); // #FC8009 Rose
    vec3 col2 = vec3(0.988, 0.290, 0.294); // #FC4A4B Plum
    vec3 col3 = vec3(0.063, 0.643, 0.702); // #10A4B3 Indigo
    vec3 col4 = vec3(0.980, 0.804, 0.576); // #FACD93 Teal

    vec3 color = (col1 * w1 + col2 * w2 + col3 * w3 + col4 * w4) / total;
    return vec4(color, 1.0);
}
"""

@Composable
actual fun ChecadorAnimatedBackground(
    modifier: Modifier
) {
    var time by remember { mutableFloatStateOf(0f) }

    // Low refresh rate (~10 FPS): sleeps 100ms per update to minimize CPU/GPU/battery usage
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(100L.milliseconds)
            time += 0.10f
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
            val shader = builder.makeShader().asComposeShader()
            drawRect(brush = ShaderBrush(shader))
        }
    }
}
