package com.v7lthronyx.v7lpanel.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QRGenerator {
    fun generateQRBitmap(content: String, size: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.Q,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val paint = Paint().apply { isAntiAlias = false }
            val cellW = size.toFloat() / matrix.width
            val cellH = size.toFloat() / matrix.height
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    if (matrix[x, y]) {
                        paint.color = Color.BLACK
                        canvas.drawRect(x * cellW, y * cellH, (x + 1) * cellW, (y + 1) * cellH, paint)
                    }
                }
            }
            bitmap
        } catch (_: Exception) { null }
    }
}
