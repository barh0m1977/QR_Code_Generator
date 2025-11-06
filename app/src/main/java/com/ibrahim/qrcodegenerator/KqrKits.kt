package com.ibrahim.qrcodegenerator

import android.graphics.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlin.math.sqrt

object KqrKits {

    // --- Style Interfaces ---
    interface QrRenderer {
        fun draw(canvas: Canvas, paint: Paint, rect: RectF)
    }

    interface EyeRenderer {
        fun draw(canvas: Canvas, paint: Paint, rect: RectF, innerColor: Int)
    }

    // --- Body Styles ---
    object SquareBody : QrRenderer {
        override fun draw(canvas: Canvas, paint: Paint, rect: RectF) {
            canvas.drawRect(rect, paint)
        }
    }

    object CircleBody : QrRenderer {
        override fun draw(canvas: Canvas, paint: Paint, rect: RectF) {
            canvas.drawOval(rect, paint)
        }
    }

    object RoundedBody : QrRenderer {
        override fun draw(canvas: Canvas, paint: Paint, rect: RectF) {
            canvas.drawRoundRect(rect, rect.width() / 3f, rect.height() / 3f, paint)
        }
    }

    object DiamondBody : QrRenderer {
        override fun draw(canvas: Canvas, paint: Paint, rect: RectF) {
            val path = Path().apply {
                moveTo(rect.centerX(), rect.top)
                lineTo(rect.right, rect.centerY())
                lineTo(rect.centerX(), rect.bottom)
                lineTo(rect.left, rect.centerY())
                close()
            }
            canvas.drawPath(path, paint)
        }
    }

    // --- Finder (eye) Styles ---
    object SquareEye : EyeRenderer {
        override fun draw(canvas: Canvas, paint: Paint, rect: RectF, innerColor: Int) {
            val unit = rect.width() / 7f
            canvas.drawRect(rect, paint) // Outer 7x7
            val innerPaint = Paint(paint).apply { color = innerColor }
            canvas.drawRect(rect.left + unit, rect.top + unit, rect.right - unit, rect.bottom - unit, innerPaint) // Inner 5x5
            canvas.drawRect(rect.left + 2 * unit, rect.top + 2 * unit, rect.right - 2 * unit, rect.bottom - 2 * unit, paint) // Center 3x3
        }
    }

    object RoundedEye : EyeRenderer {
        override fun draw(canvas: Canvas, paint: Paint, rect: RectF, innerColor: Int) {
            val unit = rect.width() / 7f
            val radius = unit * 2.5f
            canvas.drawRoundRect(rect, radius, radius, paint) // Outer 7x7
            val innerPaint = Paint(paint).apply { color = innerColor }
            canvas.drawRect(rect.left + unit, rect.top + unit, rect.right - unit, rect.bottom - unit, innerPaint) // Inner 5x5 clear
            canvas.drawRoundRect(RectF(rect.left + 2 * unit, rect.top + 2 * unit, rect.right - 2 * unit, rect.bottom - 2 * unit), unit, unit, paint) // Center 3x3
        }
    }

    object CircleEye : EyeRenderer {
        override fun draw(canvas: Canvas, paint: Paint, rect: RectF, innerColor: Int) {
            val unit = rect.width() / 7f
            val centerX = rect.centerX()
            val centerY = rect.centerY() // Use the rect's center Y

            // Outer circle
            canvas.drawCircle(centerX, centerY, 3.5f * unit, paint)

            // Middle ring (clear)
            val innerPaint = Paint(paint).apply { color = innerColor }
            canvas.drawCircle(centerX, centerY, 2.5f * unit, innerPaint)

            // Inner circle
            canvas.drawCircle(centerX, centerY, 1.5f * unit, paint)
        }
    }


    private val bodyRenderers = mapOf(
        "square" to SquareBody,
        "circle" to CircleBody,
        "rounded" to RoundedBody,
        "diamond" to DiamondBody
    )

    private val eyeRenderers = mapOf(
        "square" to SquareEye,
        "rounded" to RoundedEye,
        "circle" to CircleEye
    )

    fun generate(
        text: String,
        size: Int = 1024,
        margin: Int = 80,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE,
        body: String = "circle",
        eye: String = "rounded",
        bodyScale: Float = 0.9f,
        errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.H,
        logo: Bitmap? = null
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to errorCorrection,
            EncodeHintType.MARGIN to 0 // We handle margin manually
        )

        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
        val n = bitMatrix.width

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bgColor)

        val paint = Paint().apply {
            color = fgColor
            style = Paint.Style.FILL
            isAntiAlias = true // Enable for smooth shapes
        }

        val availableSize = size - 2 * margin
        val moduleSize = availableSize / n.toFloat()
        val scaledModuleSize = moduleSize * bodyScale
        val padding = (moduleSize - scaledModuleSize) / 2f

        val bodyRenderer = bodyRenderers[body] ?: SquareBody
        val eyeRenderer = eyeRenderers[eye] ?: SquareEye

        // Draw all data modules first
        for (y in 0 until n) {
            for (x in 0 until n) {
                if (bitMatrix.get(x, y)) {
                    val left = margin + x * moduleSize + padding
                    val top = margin + y * moduleSize + padding
                    val rect = RectF(left, top, left + scaledModuleSize, top + scaledModuleSize)
                    bodyRenderer.draw(canvas, paint, rect)
                }
            }
        }

        // Draw finder patterns on top to ensure they are crisp
        val eyeModuleSize = 7 * moduleSize
        val eyePositions = listOf(
            RectF(margin.toFloat(), margin.toFloat(), margin + eyeModuleSize, margin + eyeModuleSize),
            RectF(size - margin - eyeModuleSize, margin.toFloat(), size - margin.toFloat(), margin + eyeModuleSize),
            RectF(margin.toFloat(), size - margin - eyeModuleSize, margin + eyeModuleSize, size - margin.toFloat())
        )
        eyePositions.forEach { rect ->
            if (isInEyeArea(rect.left.toInt(), rect.top.toInt(), n, moduleSize.toInt(), margin)) {
                eyeRenderer.draw(canvas, paint, rect, bgColor)
            }
        }

        logo?.let {
            val maxLogoSize = availableSize * getLogoSafetyMargin(errorCorrection)
            val logoSize = maxLogoSize.coerceAtMost(size / 4f) // Cap logo size
            val left = (size - logoSize) / 2f
            val top = (size - logoSize) / 2f
            val rect = RectF(left, top, left + logoSize, top + logoSize)

            // Clear area behind logo for better scanning
            val clearPaint = Paint().apply { color = bgColor; style = Paint.Style.FILL }
            canvas.drawRoundRect(rect, logoSize / 5f, logoSize / 5f, clearPaint)

            canvas.drawBitmap(it, null, rect, null)
        }

        return bmp
    }

    private fun getLogoSafetyMargin(level: ErrorCorrectionLevel): Float {
        return when (level) {
            ErrorCorrectionLevel.L -> 0.15f
            ErrorCorrectionLevel.M -> 0.25f
            ErrorCorrectionLevel.Q -> 0.30f
            ErrorCorrectionLevel.H -> 0.35f
        }
    }

    private fun isInEyeArea(x: Int, y: Int, n: Int, moduleSize: Int, margin: Int): Boolean {
        val eyePhysicalSize = 7 * moduleSize
        val inTopLeft = x < margin + eyePhysicalSize && y < margin + eyePhysicalSize
        val inTopRight = x > n * moduleSize + margin - eyePhysicalSize && y < margin + eyePhysicalSize
        val inBottomLeft = x < margin + eyePhysicalSize && y > n * moduleSize + margin - eyePhysicalSize
        return inTopLeft || inTopRight || inBottomLeft
    }
}
