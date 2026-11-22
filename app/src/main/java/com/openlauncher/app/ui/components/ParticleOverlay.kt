package com.openlauncher.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.openlauncher.app.data.ParticleEffect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ambient particle overlay for the home background:
 *
 *  - SNOW  — six-armed flakes in four size tiers (small → extra-large). The two
 *    large tiers are rarer and fall noticeably slower with more horizontal sway,
 *    and the big flakes grow a 12-point secondary-arm structure.
 *  - RAIN  — reworked with three depth layers (far / mid / near) for parallax:
 *    near drops are longer, thicker, faster and brighter; far drops are short,
 *    thin, slow and faint. All drops share one consistent wind slant (with a
 *    touch of jitter) so the rain reads as weather, not random lines. Near drops
 *    also splash as small expanding ripples at the bottom edge.
 *  - STARS — small twinkling stars, a rare population of large "breathing" stars
 *    that slowly shine and fade with a soft halo, plus occasional meteorites
 *    (fast streaks with a fading tail) that cross the sky every so often.
 *  - CONSTELLATION — the classic "particle network": dots of several sizes drift
 *    calmly in straight lines (smaller dots move faster, larger drift slower),
 *    bounce off the edges, and draw soft links to nearby dots whose opacity fades
 *    with distance. No gravity — this is the proven smooth, non-clumping look.
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
    val isRain          = style == ParticleEffect.RAIN
    val isStars         = style == ParticleEffect.STARS
    val isSnow          = style == ParticleEffect.SNOW

    val count = when (style) {
        ParticleEffect.SNOW         -> (150 * amount.coerceIn(0.2f, 1.5f)).toInt().coerceIn(30, 280)
        ParticleEffect.RAIN         -> (170 * amount.coerceIn(0.2f, 1.5f)).toInt().coerceIn(40, 320)
        ParticleEffect.STARS        -> (110 * amount.coerceIn(0.2f, 1.5f)).toInt().coerceIn(25, 200)
        ParticleEffect.CONSTELLATION -> (60 * amount.coerceIn(0.2f, 1.5f)).toInt().coerceIn(16, 90)
        ParticleEffect.NONE         -> 0
    }

    val particles = remember(style, count) {
        when (style) {
            ParticleEffect.SNOW         -> List(count) { Particle.randomSnow() }
            ParticleEffect.RAIN         -> List(count) { Particle.randomRain() }
            ParticleEffect.STARS        -> List(count) { Particle.randomStars() } + List(3) { Particle.meteor() }
            ParticleEffect.CONSTELLATION -> List(count) { Particle.randomConstellation() }
            ParticleEffect.NONE         -> emptyList()
        }
    }

    // Rain splash ripples live outside the particle list (they are ground FX).
    val splashes = remember { mutableStateListOf<Splash>() }
    var frame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(style, count) {
        var last = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.1f)
                last = now
                if (isConstellation) {
                    Particle.updateConstellation(particles, dt)
                } else {
                    for (p in particles) {
                        val resetX = p.update(dt, style)
                        if (isRain && resetX != null && p.size > 0.7f && splashes.size < 14) {
                            if (Random.nextFloat() < 0.55f) splashes.add(Splash(resetX, 0f))
                        }
                    }
                }
                if (isRain && splashes.isNotEmpty()) {
                    for (s in splashes) s.age += dt
                    splashes.removeAll { it.age > 0.45f }
                }
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
                        val k = 1f - dist / maxDist
                        drawLine(
                            color = accent.copy(alpha = (k * k * 0.5f).coerceIn(0f, 1f)),
                            start = Offset(a.x * w, a.y * h),
                            end = Offset(b.x * w, b.y * h),
                            strokeWidth = (0.6f + (a.size + b.size) * 0.15f) * unit
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
                    // Six-armed flake, rotating slowly while it falls. Large flakes
                    // add a second set of shorter arms for a fuller 12-point shape.
                    val size = (2f + p.size * 4f) * unit
                    val rot = p.phase * p.rotSpeed
                    val alpha = p.alpha
                    for (k in 0..2) {
                        val ang = rot + k * (PI / 3f).toFloat()
                        val dx = cos(ang) * size
                        val dy = sin(ang) * size
                        drawLine(
                            color = Color.White.copy(alpha = alpha),
                            start = Offset(x - dx, y - dy),
                            end = Offset(x + dx, y + dy),
                            strokeWidth = (0.8f + p.size * 0.15f) * unit
                        )
                    }
                    if (p.size > 1.4f) {
                        for (k in 0..5) {
                            val ang = rot + (PI / 6f).toFloat() + k * (PI / 3f).toFloat()
                            val s2 = size * 0.42f
                            val dx = cos(ang) * s2
                            val dy = sin(ang) * s2
                            drawLine(
                                color = Color.White.copy(alpha = alpha * 0.7f),
                                start = Offset(x - dx, y - dy),
                                end = Offset(x + dx, y + dy),
                                strokeWidth = 0.5f * unit
                            )
                        }
                    }
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.95f),
                        radius = (0.7f + p.size * 0.9f) * unit,
                        center = Offset(x, y)
                    )
                }
                ParticleEffect.RAIN -> {
                    // Streak along the drop's own (slanted) velocity vector, with
                    // length/width driven by its depth layer.
                    val len = (8f + p.size * 30f) * unit
                    val mag = maxOf(hypot(p.vx, p.vy), 0.001f)
                    val dirX = p.vx / mag
                    val dirY = p.vy / mag
                    drawLine(
                        color = Color(0xFFA8C8FF).copy(alpha = p.alpha * 0.9f),
                        start = Offset(x, y),
                        end = Offset(x - dirX * len, y - dirY * len),
                        strokeWidth = (0.5f + p.size * 1.6f) * unit
                    )
                }
                ParticleEffect.STARS -> {
                    when {
                        p.isMeteor -> {
                            if (p.meteorLife >= 0f) {
                                val lifeFade = sin(minOf(p.meteorLife, 1f) * PI.toFloat()).coerceIn(0f, 1f)
                                val head = Offset(x, y)
                                val dirMag = maxOf(hypot(p.meteorVx, p.meteorVy), 0.001f)
                                val dx = -p.meteorVx / dirMag
                                val dy = -p.meteorVy / dirMag
                                val tailLen = 0.16f * minOf(w, h)
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.95f * lifeFade),
                                    radius = 1.6f * unit,
                                    center = head
                                )
                                val segs = 7
                                for (i in 1..segs) {
                                    val t0 = (i - 1) / segs.toFloat()
                                    val t1 = i / segs.toFloat()
                                    val a = ((1f - t0) * 0.7f * lifeFade).coerceIn(0f, 1f)
                                    drawLine(
                                        color = Color(0xFFBFD8FF).copy(alpha = a),
                                        start = Offset(head.x + dx * tailLen * t0, head.y + dy * tailLen * t0),
                                        end = Offset(head.x + dx * tailLen * t1, head.y + dy * tailLen * t1),
                                        strokeWidth = (2.6f * (1f - t0)) * unit + 0.3f
                                    )
                                }
                            }
                        }
                        p.isGlow -> {
                            // Rare large star: slow "breathing" shine-and-fade halo.
                            val pulse = 0.5f + 0.5f * sin(t * p.twinkleSpeed + p.phase)
                            val coreR = (1.0f + p.size * 1.3f) * unit
                            val haloR = coreR * (2.0f + 1.4f * pulse)
                            val coreA = (0.35f + 0.6f * pulse).coerceIn(0f, 1f)
                            drawCircle(Color.White.copy(alpha = 0.10f * pulse), haloR, Offset(x, y))
                            drawCircle(Color.White.copy(alpha = 0.20f * pulse), haloR * 0.62f, Offset(x, y))
                            drawCircle(Color.White.copy(alpha = coreA), coreR, Offset(x, y))
                            val ray = coreR * (1.6f + 1.2f * pulse)
                            val rayA = 0.5f * pulse
                            drawLine(Color.White.copy(alpha = rayA), Offset(x - ray, y), Offset(x + ray, y), 0.8f * unit)
                            drawLine(Color.White.copy(alpha = rayA), Offset(x, y - ray), Offset(x, y + ray), 0.8f * unit)
                        }
                        else -> {
                            val twinkle = 0.55f + 0.45f * sin(t * p.twinkleSpeed + p.phase)
                            if (p.size > 0.55f) {
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
                    }
                }
                ParticleEffect.CONSTELLATION -> {
                    val r = (0.8f + p.size * 2.4f) * unit
                    if (p.size > 2.0f) {
                        // Large slow anchors get a faint halo (purely visual).
                        drawCircle(accent.copy(alpha = 0.07f), r * 2.0f, Offset(x, y))
                    }
                    drawCircle(
                        color = accent.copy(alpha = 0.55f + p.alpha * 0.4f),
                        radius = r,
                        center = Offset(x, y)
                    )
                }
                ParticleEffect.NONE -> {}
            }
        }

        // Rain splash ripples at the bottom edge
        if (isRain) {
            for (s in splashes) {
                val cx = s.x * w
                val cy = h - 3f * unit
                val prog = (s.age / 0.45f).coerceIn(0f, 1f)
                val r = (2f + prog * 9f) * unit
                val a = (1f - prog) * 0.45f
                if (a > 0f) {
                    drawOval(
                        color = Color(0xFFA8C8FF).copy(alpha = a),
                        topLeft = Offset(cx - r * 1.7f, cy - r * 0.55f),
                        size = Size(r * 3.4f, r * 1.1f),
                        style = Stroke(width = (1f * unit).coerceAtLeast(0.6f))
                    )
                }
            }
        }
    }
}

private class Splash(var x: Float, var age: Float)

private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    var alpha: Float,
    var phase: Float,
    // Behavior modifiers (defaults keep non-participating effects inert)
    var sway: Float = 0f,
    var rotSpeed: Float = 0.3f,
    var twinkleSpeed: Float = 1.5f,
    var isGlow: Boolean = false,
    var isMeteor: Boolean = false,
    var meteorDelay: Float = 8f,
    var meteorLife: Float = -1f,
    var meteorVx: Float = 0f,
    var meteorVy: Float = 0f
) {
    /** Advances the particle; returns the x where a rain drop reset (for a splash), else null. */
    fun update(dt: Float, style: ParticleEffect): Float? {
        if (isMeteor) {
            if (meteorLife < 0f) {
                meteorDelay -= dt
                if (meteorDelay <= 0f) {
                    meteorLife = 0f
                    x = Random.nextFloat()
                    y = Random.nextFloat() * 0.22f
                    val dir = if (Random.nextBoolean()) 1f else -1f
                    val ang = PI.toFloat() * (0.30f + Random.nextFloat() * 0.35f)
                    val sp = 0.9f + Random.nextFloat() * 0.7f
                    meteorVx = dir * cos(ang) * sp
                    meteorVy = sin(ang) * sp
                }
            } else {
                meteorLife += dt / 0.7f
                x += meteorVx * dt
                y += meteorVy * dt
                if (meteorLife >= 1f || y > 1.1f || x < -0.15f || x > 1.15f) {
                    meteorLife = -1f
                    meteorDelay = 7f + Random.nextFloat() * 14f
                }
            }
            return null
        }

        when (style) {
            ParticleEffect.SNOW -> {
                phase += dt
                y += vy * dt
                x += (vx + sin(phase * 1.4f) * sway) * dt
                if (y > 1.06f) { y = -0.05f; x = Random.nextFloat() }
                if (x > 1.05f) x = -0.04f
                if (x < -0.05f) x = 1.04f
            }
            ParticleEffect.RAIN -> {
                y += vy * dt
                x += vx * dt
                if (y > 1.04f) {
                    y = -0.04f
                    x = Random.nextFloat()
                    return x
                }
                if (x < -0.06f) x = 1.04f
                if (x > 1.06f) x = -0.04f
            }
            ParticleEffect.STARS -> { /* fixed; twinkle handled in draw via time + phase */ }
            ParticleEffect.CONSTELLATION -> { /* handled by updateConstellation() */ }
            ParticleEffect.NONE -> {}
        }
        return null
    }

    companion object {
        fun randomSnow(): Particle {
            val roll = Random.nextFloat()
            val r = Random.nextFloat()
            return when {
                roll < 0.55f -> Particle( // small — the common background flurry
                    x = r, y = Random.nextFloat(),
                    vx = (Random.nextFloat() - 0.5f) * 0.03f,
                    vy = 0.08f + Random.nextFloat() * 0.10f,
                    size = 0.35f + Random.nextFloat() * 0.40f,
                    alpha = 0.30f + Random.nextFloat() * 0.40f,
                    phase = Random.nextFloat() * (2 * PI).toFloat(),
                    sway = 0.020f + Random.nextFloat() * 0.020f,
                    rotSpeed = 0.25f + Random.nextFloat() * 0.35f
                )
                roll < 0.85f -> Particle( // medium
                    x = r, y = Random.nextFloat(),
                    vx = (Random.nextFloat() - 0.5f) * 0.03f,
                    vy = 0.06f + Random.nextFloat() * 0.05f,
                    size = 0.75f + Random.nextFloat() * 0.35f,
                    alpha = 0.35f + Random.nextFloat() * 0.35f,
                    phase = Random.nextFloat() * (2 * PI).toFloat(),
                    sway = 0.035f + Random.nextFloat() * 0.015f,
                    rotSpeed = 0.20f + Random.nextFloat() * 0.20f
                )
                roll < 0.95f -> Particle( // large — rarer, slower, drifts more
                    x = r, y = Random.nextFloat(),
                    vx = (Random.nextFloat() - 0.5f) * 0.03f,
                    vy = 0.035f + Random.nextFloat() * 0.025f,
                    size = 1.50f + Random.nextFloat() * 0.40f,
                    alpha = 0.50f + Random.nextFloat() * 0.30f,
                    phase = Random.nextFloat() * (2 * PI).toFloat(),
                    sway = 0.050f + Random.nextFloat() * 0.020f,
                    rotSpeed = 0.12f + Random.nextFloat() * 0.10f
                )
                else -> Particle( // extra-large — rarest, slowest
                    x = r, y = Random.nextFloat(),
                    vx = (Random.nextFloat() - 0.5f) * 0.03f,
                    vy = 0.018f + Random.nextFloat() * 0.022f,
                    size = 2.10f + Random.nextFloat() * 0.50f,
                    alpha = 0.55f + Random.nextFloat() * 0.30f,
                    phase = Random.nextFloat() * (2 * PI).toFloat(),
                    sway = 0.070f + Random.nextFloat() * 0.030f,
                    rotSpeed = 0.08f + Random.nextFloat() * 0.08f
                )
            }
        }

        fun randomRain(): Particle {
            // Three depth layers with a shared wind slant for natural-looking rain.
            val slant = -0.16f // consistent lean (negative = toward the left)
            val roll = Random.nextFloat()
            return when {
                roll < 0.50f -> { // far layer — many, short, thin, slow, faint
                    val vy = 0.35f + Random.nextFloat() * 0.20f
                    Particle(
                        x = Random.nextFloat(), y = Random.nextFloat(),
                        vx = slant * vy + (Random.nextFloat() - 0.5f) * 0.03f, vy = vy,
                        size = Random.nextFloat() * 0.35f,
                        alpha = 0.15f + Random.nextFloat() * 0.15f,
                        phase = 0f
                    )
                }
                roll < 0.85f -> { // mid layer
                    val vy = 0.60f + Random.nextFloat() * 0.25f
                    Particle(
                        x = Random.nextFloat(), y = Random.nextFloat(),
                        vx = slant * vy + (Random.nextFloat() - 0.5f) * 0.03f, vy = vy,
                        size = 0.35f + Random.nextFloat() * 0.40f,
                        alpha = 0.30f + Random.nextFloat() * 0.25f,
                        phase = 0f
                    )
                }
                else -> { // near layer — few, long, thick, fast, bright
                    val vy = 0.90f + Random.nextFloat() * 0.40f
                    Particle(
                        x = Random.nextFloat(), y = Random.nextFloat(),
                        vx = slant * vy + (Random.nextFloat() - 0.5f) * 0.04f, vy = vy,
                        size = 0.75f + Random.nextFloat() * 0.55f,
                        alpha = 0.55f + Random.nextFloat() * 0.30f,
                        phase = 0f
                    )
                }
            }
        }

        fun randomStars(): Particle {
            val glow = Random.nextFloat() < 0.15f // rare large "breathing" star
            return if (glow) {
                Particle(
                    x = Random.nextFloat(), y = Random.nextFloat(),
                    vx = 0f, vy = 0f,
                    size = 1.3f + Random.nextFloat() * 1.2f,
                    alpha = 0.7f + Random.nextFloat() * 0.3f,
                    phase = Random.nextFloat() * (2 * PI).toFloat(),
                    twinkleSpeed = 0.25f + Random.nextFloat() * 0.50f,
                    isGlow = true
                )
            } else {
                Particle(
                    x = Random.nextFloat(), y = Random.nextFloat(),
                    vx = 0f, vy = 0f,
                    size = Random.nextFloat(),
                    alpha = 0.25f + Random.nextFloat() * 0.55f,
                    phase = Random.nextFloat() * (2 * PI).toFloat(),
                    twinkleSpeed = 1.2f + Random.nextFloat() * 2.0f
                )
            }
        }

        fun meteor(): Particle = Particle(
            x = 0f, y = 0f, vx = 0f, vy = 0f,
            size = 1f, alpha = 1f, phase = 0f,
            isMeteor = true,
            meteorDelay = 4f + Random.nextFloat() * 10f
        )

        /**
         * Classic "particle network" drift: each dot travels in a straight line at
         * its own constant speed and bounces off the edges. Links are drawn in the
         * canvas pass purely from proximity, so the field stays calm, uniform and
         * never clumps or freezes.
         */
        fun updateConstellation(particles: List<Particle>, dt: Float) {
            for (a in particles) {
                a.x += a.vx * dt
                a.y += a.vy * dt
                if (a.x < 0f) { a.x = 0f; a.vx = -a.vx }
                if (a.x > 1f) { a.x = 1f; a.vx = -a.vx }
                if (a.y < 0f) { a.y = 0f; a.vy = -a.vy }
                if (a.y > 1f) { a.y = 1f; a.vy = -a.vy }
            }
        }

        fun randomConstellation(): Particle {
            val roll = Random.nextFloat()
            val jitter = Random.nextFloat()
            // Four size tiers: smaller dots are more common and move faster; the
            // rare large dots drift slowly like calm anchors.
            val size = when {
                roll < 0.05f -> 2.2f + jitter * 0.6f   // ~5% extra-large
                roll < 0.20f -> 1.5f + jitter * 0.5f   // ~15% large
                roll < 0.50f -> 0.9f + jitter * 0.5f   // ~30% medium
                else         -> 0.5f + jitter * 0.4f   // ~50% small
            }
            // Speed falls off with size — bigger = slower, smaller = livelier.
            val speed = (0.008f + Random.nextFloat() * 0.022f) / (0.4f + size)
            val ang = Random.nextFloat() * (2 * PI).toFloat()
            return Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = cos(ang) * speed,
                vy = sin(ang) * speed,
                size = size,
                alpha = 0.45f + Random.nextFloat() * 0.40f,
                phase = Random.nextFloat() * (2 * PI).toFloat()
            )
        }
    }
}
