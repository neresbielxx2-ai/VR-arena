package com.lunarvr.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

object ModernIcons {

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

        // Roof path
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

        // Reset
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
        // Outer circle
        canvas.drawCircle(cx, cy, radius, paint)

        // Equator horizontal line
        canvas.drawLine(cx - radius, cy, cx + radius, cy, paint)

        // Prime meridian ellipse
        val ovalRect = RectF(cx - radius * 0.45f, cy - radius, cx + radius * 0.45f, cy + radius)
        canvas.drawOval(ovalRect, paint)

        // North latitude line
        canvas.drawLine(cx - radius * 0.85f, cy - radius * 0.5f, cx + radius * 0.85f, cy - radius * 0.5f, paint)
        // South latitude line
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
        // Outer target circle
        canvas.drawCircle(cx, cy, radius * 0.82f, paint)

        // Inner bullseye
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, radius * 0.22f, paint)

        // 4 crosshair ticks
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(cx, cy - radius, cx, cy - radius * 0.45f, paint)
        canvas.drawLine(cx, cy + radius * 0.45f, cx, cy + radius, paint)
        canvas.drawLine(cx - radius, cy, cx - radius * 0.45f, cy, paint)
        canvas.drawLine(cx + radius * 0.45f, cy, cx + radius, cy, paint)

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
        // Central ring
        canvas.drawCircle(cx, cy, radius * 0.45f, paint)

        // 6 gear cogs
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

    // Draw a sleek Move / Drag Bar icon
    fun drawDragHandle(canvas: Canvas, paint: Paint, cx: Float, cy: Float, width: Float, height: Float, isLocked: Boolean) {
        val oldColor = paint.color
        val oldStyle = paint.style

        // Draw pill handle
        val halfW = width / 2f
        val halfH = height / 2f
        val rect = RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)

        paint.style = Paint.Style.FILL
        paint.color = if (isLocked) Color.parseColor("#00E5FF") else Color.parseColor("#384869")
        canvas.drawRoundRect(rect, halfH, halfH, paint)

        // Draw grip dots
        paint.color = if (isLocked) Color.parseColor("#060A14") else Color.parseColor("#8E9DB8")
        val dotRadius = halfH * 0.35f
        canvas.drawCircle(cx - 20f, cy, dotRadius, paint)
        canvas.drawCircle(cx, cy, dotRadius, paint)
        canvas.drawCircle(cx + 20f, cy, dotRadius, paint)

        paint.color = oldColor
        paint.style = oldStyle
    }
}
