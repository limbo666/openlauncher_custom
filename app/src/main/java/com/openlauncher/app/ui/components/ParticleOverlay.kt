package com.openlauncher.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.openlauncher.app.data.ParticleEffect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ambient particle overlay for the home background:
 *
 *  - SNOW  — six-armed snowflake shapes, slowly rotating as they drift
 *  - RAIN  — slanted streaks whose direction matches each drop's fall vector
 *  - STARS — fixed stars; the larger ones blink and send expanding cross-rays
 *  - CONSTELLATION — drifting dots of various sizes joined by lines when close
 *    (the classic particles-network look), tinted with the accent color
 *
 * Draw-only and touch-transparent, so it can never intercept input. Positions
 * live in 0..1 space and are scaled to the canvas each frame, surviving resizes.
 */
@Composable
fun ParticleOverlay(
    style: ParticleEffect,
    amount: Float,
    accent: Color = Color(0xFF9FC7FF),
    modifier: Modifier = Modifier
) {
    if (style == ParticleEffect.NONE) return

    val isConstellation = style == ParticleEffect.CONSTELLATION
    val count = ((if (isConstellation) 60 else 120) * amount.coerceIn(0.2f, 1.5f))
        .toInt()
        .coerceIn(if (isConstellation) 16 else 20, if (isConstellation) 90 else 240)

    val particles = remember(style, count) {
        List(count) {
            if (isConstellation) Particle.randomConstellation() else Particle.random()
        }
    }
    var frame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(style, count) {
        var last = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.1f)
                last = now
                for (p in particles) p.update(dt, style)
                frame = now
            }
        }
    }

    Canvas(modifier = modifier) {
        val f = frame // state read → redraw each frame
        val w = size.width
        val h = size.height
        val t = f / 1_000_000_000f
        val unit = (minOf(w, h) / 800f).coerceAtLeast(0.55f)

        // Constellation first (connections under the dots)
        if (isConstellation) {
            val threshold = 0.17f
            for (i in particles.indices) {
                val a = particles[i]
                for (j in i + 1 until particles.size) {
                    val b = particles[j]
                    val dx = (a.x - b.x) * w
                    val dy = (a.y - b.y) * h
                    val dist = hypot(dx, dy)
                    val maxDist = threshold * minOf(w, h)
                    if (dist < maxDist) {
                        drawLine(
                            color = accent.copy(alpha = (1f - dist / maxDist) * 0.35f),
                            start = Offset(a.x * w, a.y * h),
                            end = Offset(b.x * w, b.y * h),
                            strokeWidth = 0.8f * unit
                        )
                    }
                }
            }
        }

        for (p in particles) {
            val x = p.x * w
            val y = p.y * h
            when (style) {
                ParticleEffect.SNOW -> {
                    // Six-armed flake, slowly rotating while it falls
                    val size = (2f + p.size * 4f) * unit
                    val rot = p.phase * 0.5f
                    val alpha = p.alpha
                    for (k in 0..2) {
                        val ang = rot + k * (PI / 3f).toFloat()
                        val dx = cos(ang) * size
                        val dy = sin(ang) * size
                        drawLine(
                            color = Color.White.copy(alpha = alpha),
                            start = Offset(x - dx, y - dy),
                            end = Offset(x + dx, y + dy),
                            strokeWidth = 0.9f * unit
                        )
                    }
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.9f),
                        radius = (0.8f + p.size * 1.2f) * unit,
                        center = Offset(x, y)
                    )
                }
                ParticleEffect.RAIN -> {
                    // Streak drawn along the drop's own velocity vector
                    val len = (10f + p.size * 20f) * unit
                    val mag = maxOf(p.vy, 0.001f)
                    val dirX = p.vx / mag
                    val dirY = p.vy / mag
                    drawLine(
                        color = Color(0xFFA8C8FF).copy(alpha = p.alpha * 0.85f),
                        start = Offset(x, y),
                        end = Offset(x - dirX * len, y - dirY * len),
                        strokeWidth = (0.9f + p.size * 1.2f) * unit
                    )
                }
                ParticleEffect.STARS -> {
                    // Larger stars blink and push expanding cross-rays
                    val twinkle = 0.55f + 0.45f * sin(t * (1.2f + p.size * 2f) + p.phase)
                    val isBig = p.size > 0.55f
                    if (isBig) {
                        val ray = (2.5f + 8f * twinkle * p.size) * unit
                        val a = (p.alpha * 0.75f * twinkle).coerceIn(0f, 1f)
                        drawLine(Color.White.copy(alpha = a), Offset(x - ray, y), Offset(x + ray, y), 0.8f * unit)
                        drawLine(Color.White.copy(alpha = a), Offset(x, y - ray), Offset(x, y + ray), 0.8f * unit)
                    }
                    drawCircle(
                        color = Color.White.copy(alpha = (p.alpha * twinkle).coerceIn(0f, 1f)),
                        radius = (0.6f + p.size * 1.6f) * unit,
                        center = Offset(x, y)
                    )
                }
                ParticleEffect.CONSTELLATION -> {
                    drawCircle(
                        color = accent.copy(alpha = 0.55f + p.alpha * 0.4f),
                        radius = (0.8f + p.size * 2.4f) * unit,
                        center = Offset(x, y)
                    )
                }
                ParticleEffect.NONE -> {}
            }
        }
    }
}

private class Particle(
    var x: Float,
    var y: Float,
    var vy: Float,
    var vx: Float,
    val size: Float,
    var alpha: Float,
    var phase: Float
) {
    fun update(dt: Float, style: ParticleEffect) {
        phase += dt
        when (style) {
            ParticleEffect.SNOW -> {
                y += vy * dt
                x += (vx + sin(phase * 1.4f) * 0.02f) * dt
                if (y > 1.06f) { y = -0.05f; x = Random.nextFloat() }
                if (x > 1.05f) x = -0.04f
                if (x < -0.05f) x = 1.04f
            }
            ParticleEffect.RAIN -> {
                y += vy * dt
                x += vx * dt
                if (y > 1.04f) { y = -0.04f; x = Random.nextFloat() }
                if (x < -0.04f) x = 1.03f
            }
            ParticleEffect.STARS -> { /* fixed; only twinkle via phase */ }
            ParticleEffect.CONSTELLATION -> {
                x += vx * dt
                y += vy * dt
                if (x < 0f) { x = 0f; vx = -vx }
                if (x > 1f) { x = 1f; vx = -vx }
                if (y < 0f) { y = 0f; vy = -vy }
                if (y > 1f) { y = 1f; vy = -vy }
            }
            ParticleEffect.NONE -> {}
        }
    }

    companion object {
        fun random(): Particle = Particle(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            vy = 0.06f + Random.nextFloat() * 0.16f,
            vx = (Random.nextFloat() - 0.5f) * 0.03f,
            size = Random.nextFloat(),
            alpha = 0.25f + Random.nextFloat() * 0.55f,
            phase = Random.nextFloat() * (2 * PI).toFloat()
        )

        /**
         * Constellation variant: a few dots are ~2x and a few ~3x the size of
         * the rest, and speed scales DOWN with size (big = slow). The base speed
         * is much lower than the other effects so the network drifts calmly
         * instead of buzzing around.
         */
        fun randomConstellation(): Particle {
            val roll = Random.nextFloat()
            val jitter = Random.nextFloat()
            val size = when {
                roll < 0.10f -> 2.6f + jitter * 0.4f          // ~10% triple size
                roll < 0.30f -> 1.9f + jitter * 0.3f          // ~20% double size
                else         -> 0.7f + jitter * 0.5f          // the rest, base size
            }
            val speedScale = 1f / (0.5f + size)               // bigger = slower
            val base = 0.012f + Random.nextFloat() * 0.022f   // slow, calm drift
            return Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vy = base * speedScale,
                vx = (Random.nextFloat() - 0.5f) * 2f * base * speedScale,
                size = size,
                alpha = 0.35f + Random.nextFloat() * 0.4f,
                phase = Random.nextFloat() * (2 * PI).toFloat()
            )
        }
    }
}
