package com.lunarvr.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

object ModernIcons {

    // Meta Quest 3S inspired Battery Pill Indicator
    fun drawBatteryIcon(canvas: Canvas, paint: Paint, x: Float, y: Float, percent: Int) {
        val oldColor = paint.color
        val oldStyle = paint.style
        val oldWidth = paint.strokeWidth

        val bw = 38f
        val bh = 18f
        val rect = RectF(x, y - bh / 2f, x + bw, y + bh / 2f)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        val nub = RectF(x + bw + 1f, y - bh * 0.22f, x + bw + 3.5f, y + bh * 0.22f)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(nub, 1.5f, 1.5f, paint)

        val fillWidth = (bw - 6f) * (percent.coerceIn(0, 100) / 100f)
        val fillRect = RectF(x + 3f, y - bh / 2f + 3f, x + 3f + fillWidth, y + bh / 2f - 3f)
        paint.color = when {
            percent > 30 -> Color.parseColor("#10B981")
            percent > 15 -> Color.parseColor("#F59E0B")
            else -> Color.parseColor("#EF4444")
        }
        canvas.drawRoundRect(fillRect, 2f, 2f, paint)

        paint.textSize = 20f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawText("$percent%", x + bw + 10f, y + 7f, paint)

        paint.color = oldColor
        paint.style = oldStyle
        paint.strokeWidth = oldWidth
    }

    // Home / Início icon
    fun drawHomeIcon(canvas: Canvas, paint: Paint, cx: Float, cy: Float, size: Float, color: Int) {
        val oldColor = paint.color
        val oldStyle = paint.style
        val oldWidth = paint.strokeWidth

        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        val half = size / 2f
        val roofPeakY = cy - half
        val roofBottomY = cy - 0.05f * size
        val wallBottomY = cy + half

        val roof = Path().apply {
            moveTo(cx, roofPeakY)
            lineTo(cx - half * 0.95f, roofBottomY)
            lineTo(cx - half * 0.65f, roofBottomY)
            lineTo(cx - half * 0.65f, wallBottomY)
            lineTo(cx - half * 0.2f, wallBottomY)
            lineTo(cx - half * 0.2f, cy + 0.15f * size)
            lineTo(cx + half * 0.2f, cy + 0.15f * size)
            lineTo(cx + half * 0.2f, wallBottomY)
            lineTo(cx + half * 0.65f, wallBottomY)
            lineTo(cx + half * 0.65f, roofBottomY)
            lineTo(cx + half * 0.95f, roofBottomY)
            close()
        }
        canvas.drawPath(roof, paint)

        paint.color = oldColor
        paint.style = oldStyle
        paint.strokeWidth = oldWidth
    }

    // Globe / Browser icon
    fun drawGlobeIcon(canvas: Canvas, paint: Paint, cx: Float, cy: Float, size: Float, color: Int) {
        val oldColor = paint.color
        val oldStyle = paint.style
        val oldWidth = paint.strokeWidth

        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.2f

        val radius = size / 2f
        canvas.drawCircle(cx, cy, radius, paint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, paint)

        val ovalRect = RectF(cx - radius * 0.45f, cy - radius, cx + radius * 0.45f, cy + radius)
        canvas.drawOval(ovalRect, paint)

        canvas.drawLine(cx - radius * 0.85f, cy - radius * 0.5f, cx + radius * 0.85f, cy - radius * 0.5f, paint)
        canvas.drawLine(cx - radius * 0.85f, cy + radius * 0.5f, cx + radius * 0.85f, cy + radius * 0.5f, paint)

        paint.color = oldColor
        paint.style = oldStyle
        paint.strokeWidth = oldWidth
    }

    // Mountains / Landscape / Cenários icon
    fun drawEnvironmentIcon(canvas: Canvas, paint: Paint, cx: Float, cy: Float, size: Float, color: Int) {
        val oldColor = paint.color
        val oldStyle = paint.style
        val oldWidth = paint.strokeWidth

        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.2f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        val half = size / 2f

        // Sun / Moon in sky
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx + half * 0.45f, cy - half * 0.45f, half * 0.25f, paint)

        // Mountains path
        paint.style = Paint.Style.STROKE
        val path = Path().apply {
            moveTo(cx - half * 0.95f, cy + half * 0.70f)
            lineTo(cx - half * 0.30f, cy - half * 0.35f)
            lineTo(cx + half * 0.20f, cy + half * 0.30f)
            lineTo(cx + half * 0.55f, cy - half * 0.05f)
            lineTo(cx + half * 0.95f, cy + half * 0.70f)
            close()
        }
        canvas.drawPath(path, paint)

        paint.color = oldColor
        paint.style = oldStyle
        paint.strokeWidth = oldWidth
    }

    // Target / Recenter icon
    fun drawRecenterIcon(canvas: Canvas, paint: Paint, cx: Float, cy: Float, size: Float, color: Int) {
        val oldColor = paint.color
        val oldStyle = paint.style
        val oldWidth = paint.strokeWidth

        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.2f

        val radius = size / 2f
        canvas.drawCircle(cx, cy, radius * 0.82f, paint)

        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, radius * 0.22f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(cx, cy - radius, cx, cy - radius * 0.45f, paint)
        canvas.drawLine(cx, cy + radius * 0.45f, cx, cy + radius, paint)
        canvas.drawLine(cx - radius, cy, cx - radius * 0.45f, cy, paint)
        canvas.drawLine(cx + radius, cy, cx + radius * 0.45f, cy, paint)

        paint.color = oldColor
        paint.style = oldStyle
        paint.strokeWidth = oldWidth
    }

    // Cogwheel / Settings icon
    fun drawSettingsIcon(canvas: Canvas, paint: Paint, cx: Float, cy: Float, size: Float, color: Int) {
        val oldColor = paint.color
        val oldStyle = paint.style
        val oldWidth = paint.strokeWidth

        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.2f

        val radius = size / 2f
        canvas.drawCircle(cx, cy, radius * 0.45f, paint)

        paint.strokeWidth = 4f
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60.0))
            val x1 = cx + (radius * 0.50f * Math.cos(angle)).toFloat()
            val y1 = cy + (radius * 0.50f * Math.sin(angle)).toFloat()
            val x2 = cx + (radius * 0.95f * Math.cos(angle)).toFloat()
            val y2 = cy + (radius * 0.95f * Math.sin(angle)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, paint)
        }

        paint.color = oldColor
        paint.style = oldStyle
        paint.strokeWidth = oldWidth
    }

    // Enterprise Meta Quest / VisionOS Drag Handle Pill
    fun drawDragHandle(canvas: Canvas, paint: Paint, cx: Float, cy: Float, width: Float, height: Float, isHovered: Boolean, isLocked: Boolean, progress: Float) {
        val oldColor = paint.color
        val oldStyle = paint.style

        val halfW = width / 2f
        val halfH = height / 2f
        val rect = RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)

        // Subtle glow / shadow background layer when hovered or locked
        if (isHovered || isLocked) {
            val glowRect = RectF(cx - halfW - 6f, cy - halfH - 6f, cx + halfW + 6f, cy + halfH + 6f)
            paint.style = Paint.Style.FILL
            paint.color = if (isLocked) Color.parseColor("#4400E5FF") else Color.parseColor("#336366F1")
            canvas.drawRoundRect(glowRect, halfH + 6f, halfH + 6f, paint)
        }

        // Base pill background
        paint.style = Paint.Style.FILL
        paint.color = when {
            isLocked -> Color.parseColor("#00E5FF")
            isHovered -> Color.parseColor("#38BDF8")
            else -> Color.parseColor("#222F46")
        }
        canvas.drawRoundRect(rect, halfH, halfH, paint)

        // Smooth dwell progress bar fill
        if (isHovered && progress > 0f && !isLocked) {
            val progW = (width - 8f) * progress
            val progRect = RectF(cx - halfW + 4f, cy - halfH + 3f, cx - halfW + 4f + progW, cy + halfH - 3f)
            paint.color = Color.parseColor("#67E8F9")
            canvas.drawRoundRect(progRect, halfH - 3f, halfH - 3f, paint)
        }

        // Crisp border outline
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (isHovered || isLocked) 2.5f else 1.8f
        paint.color = if (isLocked) Color.parseColor("#FFFFFF") else if (isHovered) Color.parseColor("#E0F2FE") else Color.parseColor("#475569")
        canvas.drawRoundRect(rect, halfH, halfH, paint)

        // Center ergonomic tactile grab lines / dots
        paint.style = Paint.Style.FILL
        paint.color = if (isLocked) Color.parseColor("#0F172A") else Color.parseColor("#CBD5E1")
        val dotRadius = halfH * 0.30f
        val spacing = halfW * 0.22f
        canvas.drawCircle(cx - spacing, cy, dotRadius, paint)
        canvas.drawCircle(cx, cy, dotRadius, paint)
        canvas.drawCircle(cx + spacing, cy, dotRadius, paint)

        paint.color = oldColor
        paint.style = oldStyle
    }

    // YouTube Icon (clean red rounded rectangle with white play triangle)
    fun drawYouTubeIcon(canvas: Canvas, paint: Paint, cx: Float, cy: Float, size: Float) {
        val oldColor = paint.color
        val oldStyle = paint.style

        val halfW = size * 0.85f
        val halfH = size * 0.58f

        // Red badge
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FF0000")
        canvas.drawRoundRect(RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH), 22f, 22f, paint)

        // White Play Triangle
        paint.color = Color.WHITE
        val triPath = Path().apply {
            val triW = size * 0.35f
            val triH = size * 0.32f
            moveTo(cx - triW * 0.45f, cy - triH)
            lineTo(cx + triW * 0.75f, cy)
            lineTo(cx - triW * 0.45f, cy + triH)
            close()
        }
        canvas.drawPath(triPath, paint)

        paint.color = oldColor
        paint.style = oldStyle
    }

    // Circular Close '✕' Button
    fun drawCloseButton(canvas: Canvas, paint: Paint, cx: Float, cy: Float, radius: Float, isHovered: Boolean) {
        val oldColor = paint.color
        val oldStyle = paint.style
        val oldWidth = paint.strokeWidth

        paint.style = Paint.Style.FILL
        paint.color = if (isHovered) Color.parseColor("#EF4444") else Color.parseColor("#334155")
        canvas.drawCircle(cx, cy, radius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = if (isHovered) Color.WHITE else Color.parseColor("#94A3B8")
        canvas.drawCircle(cx, cy, radius, paint)

        // Draw 'X'
        val cross = radius * 0.45f
        paint.strokeWidth = 3f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.WHITE
        canvas.drawLine(cx - cross, cy - cross, cx + cross, cy + cross, paint)
        canvas.drawLine(cx - cross, cy + cross, cx + cross, cy - cross, paint)

        paint.color = oldColor
        paint.style = oldStyle
        paint.strokeWidth = oldWidth
    }
}