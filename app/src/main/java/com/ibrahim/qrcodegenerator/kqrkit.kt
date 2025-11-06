package com.kqrkit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlin.math.min

/**
 * Kotlin QR Code Generator — Simple & Customizable
 * Author: Indexer (كديشو)
 * License: MIT
 */
object KqrKit {

    enum class ModuleStyle { Square, Circle }

    /**
     * Generate QR using ZXing (scannable)
     */
    fun generateZXing(
        text: String,
        size: Int = 512,
        foreground: Int = Color.BLACK,
        background: Int = Color.WHITE,
        margin: Int = 16,
        style: ModuleStyle = ModuleStyle.Square
    ): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            0, // handle sizing ourselves
            0
        )

        // Convert BitMatrix to Boolean matrix
        val matrix = Array(bitMatrix.height) { y ->
            BooleanArray(bitMatrix.width) { x -> bitMatrix[x, y] }
        }

        return renderMatrix(matrix, size, foreground, background, margin, style)
    }

    /**
     * Custom QR Renderer (Version 1 Alphanumeric)
     */
    fun generateCustom(
        text: String,
        size: Int = 512,
        foreground: Int = Color.BLACK,
        background: Int = Color.WHITE,
        margin: Int = 16,
        style: ModuleStyle = ModuleStyle.Square
    ): Bitmap {
        val matrix = SimpleQrEncoder.encode(text)
        return renderMatrix(matrix, size, foreground, background, margin, style)
    }

    /**
     * Render a Boolean matrix to Bitmap with style
     */
    private fun renderMatrix(
        matrix: Array<BooleanArray>,
        size: Int,
        fg: Int,
        bg: Int,
        margin: Int,
        style: ModuleStyle
    ): Bitmap {
        val n = matrix.size
        val scale = (size - 2 * margin) / n.toFloat()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bg)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fg }

        val gapRatio = if (style == ModuleStyle.Circle) 0.25f else 0f
        val moduleSize = scale * (1 - gapRatio)

        for (y in matrix.indices) {
            for (x in matrix[y].indices) {
                if (matrix[y][x]) {
                    val cx = margin + x * scale + scale / 2
                    val cy = margin + y * scale + scale / 2

                    // Keep finder patterns square for scannability
                    val isFinder = (x < 7 && y < 7) ||
                            (x >= n - 7 && y < 7) ||
                            (x < 7 && y >= n - 7)

                    when {
                        isFinder || style == ModuleStyle.Square -> {
                            canvas.drawRect(
                                margin + x * scale,
                                margin + y * scale,
                                margin + (x + 1) * scale,
                                margin + (y + 1) * scale,
                                paint
                            )
                        }
                        else -> {
                            canvas.drawCircle(cx, cy, moduleSize / 2, paint)
                        }
                    }
                }
            }
        }
        return bmp
    }
}

/**
 * Simple QR Encoder (V1 Alphanumeric)
 */
object SimpleQrEncoder {

    private const val TOTAL_DATA_BITS = 152 // Version 1-L
    private const val TOTAL_DATA_BYTES = 19
    private val ALPHANUMERIC_TABLE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"

    fun encode(text: String): Array<BooleanArray> {
        val bits = BitBuffer()
        bits.put(0b0010, 4)           // Mode: Alphanumeric
        bits.put(text.length, 9)      // Char count

        // Encode characters
        var i = 0
        while (i < text.length) {
            val c1 = ALPHANUMERIC_TABLE.indexOf(text[i])
            if (i + 1 < text.length) {
                val c2 = ALPHANUMERIC_TABLE.indexOf(text[i + 1])
                bits.put(45 * c1 + c2, 11)
                i += 2
            } else {
                bits.put(c1, 6)
                i++
            }
        }

        // Terminator & pad bits
        val remaining = TOTAL_DATA_BITS - bits.size
        bits.put(0, min(4, remaining))
        while (bits.size % 8 != 0) bits.put(false)

        // Pad bytes
        val dataBytes = bits.toBytes().toMutableList()
        var pad: Byte = 0xEC.toByte()
        while (dataBytes.size < TOTAL_DATA_BYTES) {
            dataBytes.add(pad)
            pad = if (pad == 0xEC.toByte()) 0x11.toByte() else 0xEC.toByte()
        }

        val finalBits = BitBuffer()
        dataBytes.forEach { finalBits.put(it.toInt() and 0xFF, 8) }

        // Build matrix
        val builder = MatrixBuilder(21)
        builder.placeFunctionPatterns()
        builder.placeDataBits(finalBits)
        builder.applyMask(0)
        return builder.modules
    }
}

/**
 * BitBuffer for building bit sequences
 */
class BitBuffer {
    private val bits = mutableListOf<Boolean>()
    val size get() = bits.size

    fun put(value: Int, length: Int) {
        for (i in length - 1 downTo 0) bits.add((value shr i) and 1 == 1)
    }

    fun put(bit: Boolean) = bits.add(bit)

    operator fun get(i: Int) = bits[i]

    fun toBytes(): ByteArray {
        val out = ByteArray((bits.size + 7) / 8)
        for (i in bits.indices) {
            if (bits[i]) out[i / 8] = (out[i / 8].toInt() or (1 shl (7 - (i % 8)))).toByte()
        }
        return out
    }
}

/**
 * Minimal QR matrix builder (Version 1, 21x21)
 */
class MatrixBuilder(val size: Int) {
    val modules = Array(size) { BooleanArray(size) }
    private val isFunction = Array(size) { BooleanArray(size) }

    fun placeFunctionPatterns() {
        placeFinder(0, 0)
        placeFinder(size - 7, 0)
        placeFinder(0, size - 7)
        placeTiming()
    }

    private fun placeFinder(x: Int, y: Int) {
        for (dy in 0 until 7) {
            for (dx in 0 until 7) {
                val xx = x + dx
                val yy = y + dy
                val v = dx == 0 || dx == 6 || dy == 0 || dy == 6 || (dx in 2..4 && dy in 2..4)
                modules[yy][xx] = v
                isFunction[yy][xx] = true
            }
        }
    }

    private fun placeTiming() {
        for (i in 8 until size - 8) {
            val v = i % 2 == 0
            modules[6][i] = v
            modules[i][6] = v
            isFunction[6][i] = true
            isFunction[i][6] = true
        }
    }

    fun placeDataBits(bits: BitBuffer) {
        var bitIndex = 0
        var upward = true
        var x = size - 1
        while (x > 0) {
            if (x == 6) x--
            var y = if (upward) size - 1 else 0
            while (y in 0 until size) {
                for (xx in x downTo x - 1) {
                    if (!isFunction[y][xx]) {
                        val bit = if (bitIndex < bits.size) bits[bitIndex++] else false
                        modules[y][xx] = bit
                    }
                }
                y += if (upward) -1 else 1
            }
            upward = !upward
            x -= 2
        }
    }

    fun applyMask(maskIndex: Int) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!isFunction[y][x]) {
                    val invert = when (maskIndex) {
                        0 -> (x + y) % 2 == 0
                        1 -> y % 2 == 0
                        2 -> x % 3 == 0
                        3 -> (x + y) % 3 == 0
                        else -> false
                    }
                    if (invert) modules[y][x] = !modules[y][x]
                }
            }
        }
    }
}
