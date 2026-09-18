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

        // Battery outer shell
        val bw = 38f
        val bh = 18f
        val rect = RectF(x, y - bh / 2f, x + bw, y + bh / 2f)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        // Battery positive terminal nub
        val nub = RectF(x + bw + 1f, y - bh * 0.22f, x + bw + 3.5f, y + bh * 0.22f)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(nub, 1.5f, 1.5f, paint)

        // Battery fill level
        val fillWidth = (bw - 6f) * (percent.coerceIn(0, 100) / 100f)
        val fillRect = RectF(x + 3f, y - bh / 2f + 3f, x + 3f + fillWidth, y + bh / 2f - 3f)
        paint.color = when {
            percent > 30 -> Color.parseColor("#10B981") // Crisp neon green
            percent > 15 -> Color.parseColor("#F59E0B") // Warning amber
            else -> Color.parseColor("#EF4444") // Red
        }
        canvas.drawRoundRect(fillRect, 2f, 2f, paint)

        // Percent text
        paint.textSize = 20f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawText("$percent%", x + bw + 10f, y + 7f, paint)

        paint.color = oldColor
        paint.style = oldStyle
        paint.strokeWidth = oldWidth
    }

    // Draw a crisp, vector Home / Início icon
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

        // Roof and wall path
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

    // Draw a crisp, vector Globe / Browser icon
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

    // Draw a crisp Target / Recenter reticle icon
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

    // Draw a crisp Cogwheel / Settings icon
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

    // Meta Quest 3S inspired Drag Bar Handle (Pill capsule with hover glow & lock indication)
    fun drawDragHandle(canvas: Canvas, paint: Paint, cx: Float, cy: Float, width: Float, height: Float, isHovered: Boolean, isLocked: Boolean, progress: Float) {
        val oldColor = paint.color
        val oldStyle = paint.style

        val halfW = width / 2f
        val halfH = height / 2f
        val rect = RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)

        // Pill handle background
        paint.style = Paint.Style.FILL
        paint.color = when {
            isLocked -> Color.parseColor("#00E5FF")
            isHovered -> Color.parseColor("#3B82F6")
            else -> Color.parseColor("#26354D")
        }
        canvas.drawRoundRect(rect, halfH, halfH, paint)

        // Progress bar inside handle
        if (isHovered && progress > 0f && !isLocked) {
            val progW = (width - 8f) * progress
            val progRect = RectF(cx - halfW + 4f, cy - halfH + 3f, cx - halfW + 4f + progW, cy + halfH - 3f)
            paint.color = Color.parseColor("#38BDF8")
            canvas.drawRoundRect(progRect, halfH - 3f, halfH - 3f, paint)
        }

        // Pill border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = if (isLocked) Color.parseColor("#FFFFFF") else Color.parseColor("#475569")
        canvas.drawRoundRect(rect, halfH, halfH, paint)

        // 3 tactile dots
        paint.style = Paint.Style.FILL
        paint.color = if (isLocked) Color.parseColor("#0F172A") else Color.parseColor("#CBD5E1")
        val dotRadius = halfH * 0.35f
        canvas.drawCircle(cx - 24f, cy, dotRadius, paint)
        canvas.drawCircle(cx, cy, dotRadius, paint)
        canvas.drawCircle(cx + 24f, cy, dotRadius, paint)

        paint.color = oldColor
        paint.style = oldStyle
    }
}
