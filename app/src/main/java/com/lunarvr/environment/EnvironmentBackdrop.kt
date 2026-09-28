package com.lunarvr.environment

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.lunarvr.ui.VRPanel

class EnvironmentBackdrop(val type: VREnvironmentType) {

    // 3D Billboard panel located at a scenic distance (Z = -3.2f, width = 6.0f, height = 3.6f)
    var panel: VRPanel = VRPanel("env_backdrop_" + type.name, 0.0f, 0.40f, -3.2f, 6.0f, 3.6f, 1024, 614)

    fun renderBackdrop() {
        panel.drawCustom { canvas, paint ->
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            when (type) {
                VREnvironmentType.BEACH_PARADISE -> drawBeachParadise(canvas, paint)
                VREnvironmentType.LUNAR_EARTH_VIEW -> drawLunarEarthView(canvas, paint)
                VREnvironmentType.CYBER_SYNTHWAVE -> drawCyberSynthwave(canvas, paint)
                VREnvironmentType.ZEN_FOREST -> drawZenForest(canvas, paint)
                VREnvironmentType.PASSTHROUGH_CAM -> {
                    // Transparent backdrop
                }
            }
        }
    }

    private fun drawLunarEarthView(canvas: Canvas, paint: Paint) {
        val w = 1024f
        val h = 614f

        // Star nebula background glow
        val bgGrad = RadialGradient(w * 0.5f, h * 0.4f, w * 0.6f, Color.parseColor("#301A2744"), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        paint.shader = bgGrad
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Majestic Planet Earth floating in space (upper right horizon)
        val earthCx = w * 0.72f
        val earthCy = h * 0.28f
        val earthRadius = 90f

        // Earth atmosphere glow
        val atmoGrad = RadialGradient(earthCx, earthCy, earthRadius * 1.35f, Color.parseColor("#6000E5FF"), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        paint.shader = atmoGrad
        canvas.drawCircle(earthCx, earthCy, earthRadius * 1.35f, paint)
        paint.shader = null

        // Earth sphere (deep ocean blue with clouds & continents)
        val earthGrad = RadialGradient(earthCx - 25f, earthCy - 25f, earthRadius * 1.1f, Color.parseColor("#38BDF8"), Color.parseColor("#0C2340"), Shader.TileMode.CLAMP)
        paint.shader = earthGrad
        canvas.drawCircle(earthCx, earthCy, earthRadius, paint)
        paint.shader = null

        // Continents / Green & White swirled clouds
        paint.color = Color.parseColor("#A010B981")
        canvas.drawCircle(earthCx - 20f, earthCy + 10f, 32f, paint)
        canvas.drawCircle(earthCx + 25f, earthCy - 15f, 26f, paint)
        paint.color = Color.parseColor("#80FFFFFF")
        canvas.drawCircle(earthCx - 10f, earthCy - 30f, 36f, paint)
        canvas.drawCircle(earthCx + 15f, earthCy + 25f, 28f, paint)

        // Moon Surface Horizon at the bottom with craters
        val moonPath = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.68f)
            quadTo(w * 0.25f, h * 0.64f, w * 0.50f, h * 0.67f)
            quadTo(w * 0.78f, h * 0.62f, w, h * 0.65f)
            lineTo(w, h)
            close()
        }
        val moonGrad = LinearGradient(0f, h * 0.62f, 0f, h, Color.parseColor("#5A6B82"), Color.parseColor("#1B222E"), Shader.TileMode.CLAMP)
        paint.shader = moonGrad
        canvas.drawPath(moonPath, paint)
        paint.shader = null

        // Moon craters
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#252F3F")
        canvas.drawOval(RectF(120f, h * 0.74f, 260f, h * 0.82f), paint)
        canvas.drawOval(RectF(480f, h * 0.78f, 680f, h * 0.88f), paint)
        canvas.drawOval(RectF(780f, h * 0.72f, 890f, h * 0.79f), paint)
    }

    private fun drawCyberSynthwave(canvas: Canvas, paint: Paint) {
        val w = 1024f
        val h = 614f

        // Giant Retro Neon Sun at center
        val sunCx = w * 0.5f
        val sunCy = h * 0.44f
        val sunRadius = 120f

        val sunGrad = LinearGradient(sunCx, sunCy - sunRadius, sunCx, sunCy + sunRadius, Color.parseColor("#FFE600"), Color.parseColor("#FF007F"), Shader.TileMode.CLAMP)
        paint.shader = sunGrad
        paint.style = Paint.Style.FILL
        canvas.drawCircle(sunCx, sunCy, sunRadius, paint)
        paint.shader = null

        // Sun horizon horizontal blind stripes
        paint.color = Color.parseColor("#14041F")
        for (i in 1..6) {
            val sy = sunCy + (i * 16f)
            canvas.drawRect(sunCx - sunRadius, sy, sunCx + sunRadius, sy + (i * 2.2f), paint)
        }

        // Mountain silhouettes in foreground
        val mtn = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.54f)
            lineTo(w * 0.18f, h * 0.38f)
            lineTo(w * 0.35f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.42f)
            lineTo(w * 0.68f, h * 0.54f)
            lineTo(w * 0.82f, h * 0.36f)
            lineTo(w, h * 0.52f)
            lineTo(w, h)
            close()
        }
        val mtnGrad = LinearGradient(0f, h * 0.36f, 0f, h, Color.parseColor("#4C0561"), Color.parseColor("#12021A"), Shader.TileMode.CLAMP)
        paint.shader = mtnGrad
        canvas.drawPath(mtn, paint)
        paint.shader = null

        // Perspective Synthwave Grid Floor
        paint.color = Color.parseColor("#A0FF007F")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f

        // Horizontal grid lines
        var gy = h * 0.62f
        var step = 8f
        while (gy < h) {
            canvas.drawLine(0f, gy, w, gy, paint)
            gy += step
            step *= 1.35f
        }

        // Converging perspective lines to vanishing point (sun center)
        for (x in -2..12) {
            val fx = (x * 120f)
            canvas.drawLine(sunCx, h * 0.62f, fx, h, paint)
        }
    }

    private fun drawZenForest(canvas: Canvas, paint: Paint) {
        val w = 1024f
        val h = 614f

        // Aurora Borealis wavy curtains in upper sky
        val auroraGrad = LinearGradient(0f, 0f, w, h * 0.5f, Color.parseColor("#6010B981"), Color.parseColor("#6006B6D4"), Shader.TileMode.CLAMP)
        paint.shader = auroraGrad
        paint.style = Paint.Style.FILL

        val aurora = Path().apply {
            moveTo(0f, h * 0.15f)
            cubicTo(w * 0.3f, h * 0.05f, w * 0.6f, h * 0.35f, w, h * 0.18f)
            lineTo(w, h * 0.45f)
            cubicTo(w * 0.6f, h * 0.55f, w * 0.3f, h * 0.25f, 0f, h * 0.40f)
            close()
        }
        canvas.drawPath(aurora, paint)
        paint.shader = null

        // Mountain ridge in mist
        val ridge = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.50f)
            lineTo(w * 0.30f, h * 0.38f)
            lineTo(w * 0.65f, h * 0.46f)
            lineTo(w, h * 0.35f)
            lineTo(w, h)
            close()
        }
        paint.color = Color.parseColor("#172922")
        canvas.drawPath(ridge, paint)

        // Pine trees skyline silhouettes
        paint.color = Color.parseColor("#0C1713")
        for (i in 0..18) {
            val tx = i * 58f + 10f
            val ty = h * 0.52f + (i % 3) * 12f
            val tree = Path().apply {
                moveTo(tx, ty)
                lineTo(tx - 24f, ty + 120f)
                lineTo(tx + 24f, ty + 120f)
                close()
            }
            canvas.drawPath(tree, paint)
        }

        // Magical Fireflies glow dots
        paint.color = Color.parseColor("#D0A7F3D0")
        val dots = listOf(
            Pair(180f, 380f), Pair(260f, 320f), Pair(410f, 350f),
            Pair(540f, 290f), Pair(710f, 360f), Pair(860f, 310f),
            Pair(920f, 410f), Pair(320f, 440f), Pair(650f, 420f)
        )
        for ((dx, dy) in dots) {
            canvas.drawCircle(dx, dy, 4.5f, paint)
        }
    }

    private fun drawMinimalLoft(canvas: Canvas, paint: Paint) {
        val w = 1024f
        val h = 614f

        // Highrise Penthouse Night City skyline
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#151D2A")

        // Distant Skyscrapers
        val buildings = listOf(
            RectF(40f, h * 0.32f, 130f, h * 0.70f),
            RectF(160f, h * 0.22f, 270f, h * 0.70f),
            RectF(300f, h * 0.38f, 390f, h * 0.70f),
            RectF(420f, h * 0.18f, 550f, h * 0.70f),
            RectF(580f, h * 0.28f, 680f, h * 0.70f),
            RectF(710f, h * 0.15f, 830f, h * 0.70f),
            RectF(860f, h * 0.34f, 980f, h * 0.70f)
        )
        for (b in buildings) {
            canvas.drawRect(b, paint)
        }

        // Tiny warm illuminated skyscraper windows
        paint.color = Color.parseColor("#90FDE047")
        for (b in buildings) {
            var wy = b.top + 20f
            while (wy < b.bottom - 20f) {
                var wx = b.left + 15f
                while (wx < b.right - 15f) {
                    canvas.drawRect(wx, wy, wx + 6f, wy + 10f, paint)
                    wx += 20f
                }
                wy += 25f
            }
        }

        // Sleek Glass Terrace floor with reflections
        val terrace = RectF(0f, h * 0.70f, w, h)
        val terraceGrad = LinearGradient(0f, h * 0.70f, 0f, h, Color.parseColor("#2A384F"), Color.parseColor("#0F1724"), Shader.TileMode.CLAMP)
        paint.shader = terraceGrad
        canvas.drawRect(terrace, paint)
        paint.shader = null

        // Glass Balustrade rail line
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.parseColor("#38BDF8")
        canvas.drawLine(0f, h * 0.70f, w, h * 0.70f, paint)
    }

    private fun drawBeachParadise(canvas: Canvas, paint: Paint) {
        val w = 1024f
        val h = 614f

        // Tropical Sunset Gradient Sky
        val skyGrad = LinearGradient(0f, 0f, 0f, h * 0.65f, Color.parseColor("#FF7E40"), Color.parseColor("#4A0E4E"), Shader.TileMode.CLAMP)
        paint.shader = skyGrad
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h * 0.65f, paint)
        paint.shader = null

        // Golden Sun descending on ocean horizon
        val sunCx = w * 0.48f
        val sunCy = h * 0.52f
        val sunGrad = RadialGradient(sunCx, sunCy, 80f, Color.parseColor("#FFE87C"), Color.parseColor("#FF6B35"), Shader.TileMode.CLAMP)
        paint.shader = sunGrad
        canvas.drawCircle(sunCx, sunCy, 75f, paint)
        paint.shader = null

        // Turquoise Ocean with sunset reflections
        val seaRect = RectF(0f, h * 0.50f, w, h * 0.72f)
        val seaGrad = LinearGradient(0f, h * 0.50f, 0f, h * 0.72f, Color.parseColor("#0284C7"), Color.parseColor("#0D9488"), Shader.TileMode.CLAMP)
        paint.shader = seaGrad
        canvas.drawRect(seaRect, paint)
        paint.shader = null

        // Sun glitter trail across the water
        paint.color = Color.parseColor("#40FDE047")
        paint.strokeWidth = 3f
        for (i in 0..12) {
            val y = h * 0.51f + i * 10f
            val spread = 20f + i * 22f
            canvas.drawLine(sunCx - spread, y, sunCx + spread, y, paint)
        }

        // Golden Sand Beach in foreground
        val sandPath = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.70f)
            quadTo(w * 0.35f, h * 0.66f, w * 0.70f, h * 0.69f)
            quadTo(w * 0.88f, h * 0.72f, w, h * 0.68f)
            lineTo(w, h)
            close()
        }
        val sandGrad = LinearGradient(0f, h * 0.66f, 0f, h, Color.parseColor("#EAB308"), Color.parseColor("#78350F"), Shader.TileMode.CLAMP)
        paint.shader = sandGrad
        canvas.drawPath(sandPath, paint)
        paint.shader = null

        // Gentle Ocean Wave foam line
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.parseColor("#CCF0FDFA")
        val foam = Path().apply {
            moveTo(0f, h * 0.70f)
            quadTo(w * 0.35f, h * 0.66f, w * 0.70f, h * 0.69f)
            quadTo(w * 0.88f, h * 0.72f, w, h * 0.68f)
        }
        canvas.drawPath(foam, paint)

        // Palm Trees silhouettes on the right and left
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1B2A1E")

        // Right Palm Trunk
        val trunk = Path().apply {
            moveTo(w * 0.88f, h)
            quadTo(w * 0.85f, h * 0.62f, w * 0.82f, h * 0.38f)
            lineTo(w * 0.84f, h * 0.38f)
            quadTo(w * 0.87f, h * 0.62f, w * 0.90f, h)
            close()
        }
        canvas.drawPath(trunk, paint)

        // Palm leaves
        val px = w * 0.83f
        val py = h * 0.38f
        for (a in listOf(-70.0, -40.0, -10.0, 20.0, 50.0, 80.0, 110.0)) {
            val rad = Math.toRadians(a)
            val lx = px + (120f * Math.cos(rad)).toFloat()
            val ly = py + (90f * Math.sin(rad)).toFloat()
            val leaf = Path().apply {
                moveTo(px, py)
                quadTo((px + lx) / 2f, py - 40f, lx, ly)
                quadTo((px + lx) / 2f + 10f, py - 10f, px, py)
                close()
            }
            canvas.drawPath(leaf, paint)
        }
    }

}
