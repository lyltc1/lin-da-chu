package com.lyltc1.lindachu.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * 二维码生成与识别，基于 ZXing。
 *
 * 选 ZXing 而不是 ML Kit，是因为 ML Kit 依赖 Google Play services，
 * 在国内大量无 GMS 的设备上不可用；ZXing 是纯 Java 实现，无额外运行时依赖，
 * 一个库同时解决"生成菜单码"和"扫朋友订单码"两件事。
 */

/** 生成正方形二维码位图，失败返回 null */
fun generateQrCode(content: String, size: Int = 720): Bitmap? {
    return runCatching {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1
        )
        val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        repeat(height) { y ->
            repeat(width) { x ->
                pixels[y * width + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }.getOrNull()
}

/**
 * 从相机帧中识别二维码。
 *
 * 只取 Y 分量构造灰度图——二维码本身是黑白图形，亮度信息已足够，
 * 省去 YUV 转 RGB 再转灰度的开销，在低端机上也能保持实时。
 */
fun decodeQrCode(image: ImageProxy): String? {
    return runCatching {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val source = PlanarYUVLuminanceSource(
            bytes,
            plane.rowStride,
            image.height,
            0,
            0,
            image.width,
            image.height,
            false
        )
        val hints = mapOf(DecodeHintType.TRY_HARDER to true)
        MultiFormatReader().apply { setHints(hints) }
            .decode(BinaryBitmap(HybridBinarizer(source)))
            .text
    }.getOrNull()
}
