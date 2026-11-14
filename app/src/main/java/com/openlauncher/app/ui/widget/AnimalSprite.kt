package com.openlauncher.app.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Speedometer "animals" sprite sheet — a 4×4 grid stored at
 * `assets/animals_sprite.png` (1024×1024, transparent background, so each
 * cell is 256×256). Cell index = row * 4 + col.
 *
 * Sheet layout (unused cells marked -):
 *   0 Tree      1 Tortoise   2 Mouse      3 Squirrel
 *   4 Bear      5 Boar       6 Turkey     7 (-) man
 *   8 (-) kangaroo 9 (-) ostrich 10 Lion   11 Horse
 *  12 Antelope 13 Cheetah   14 Gyrfalcon 15 Peregrine falcon
 */
object AnimalSprite {

    const val COLUMNS = 4
    const val ROWS = 4

    private const val ASSET_PATH = "animals_sprite.png"

    private var cached: Bitmap? = null
    private var loadFailed = false

    /** Decode the sheet once and keep it for the process lifetime. */
    @Synchronized
    fun bitmap(context: Context): Bitmap? {
        cached?.let { if (!it.isRecycled) return it }
        if (loadFailed) return null
        return runCatching {
            context.assets.open(ASSET_PATH).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull().also {
            if (it != null) cached = it else loadFailed = true
        }
    }

    /**
     * Speed in km/h → sprite cell index.
     * 0 → tree · 1–10 tortoise · 11–20 mouse · 21–30 squirrel · 31–40 bear ·
     * 41–50 boar · 51–60 turkey · 61–70 lion · 71–80 horse · 81–95 antelope ·
     * 96–120 cheetah · 121–140 gyrfalcon · 141+ peregrine falcon.
     */
    fun cellIndexForSpeed(kmh: Float): Int {
        val s = if (kmh.isFinite()) kmh.coerceAtLeast(0f) else 0f
        return when {
            s < 1f    -> 0    // Tree
            s <= 10f  -> 1    // Tortoise
            s <= 20f  -> 2    // Mouse
            s <= 30f  -> 3    // Squirrel
            s <= 40f  -> 4    // Bear
            s <= 50f  -> 5    // Boar
            s <= 60f  -> 6    // Turkey
            s <= 70f  -> 10   // Lion
            s <= 80f  -> 11   // Horse
            s <= 95f  -> 12   // Antelope
            s <= 120f -> 13   // Cheetah
            s <= 140f -> 14   // Gyrfalcon
            else      -> 15   // Peregrine falcon
        }
    }
}
