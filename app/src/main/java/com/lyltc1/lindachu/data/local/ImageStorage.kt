package com.lyltc1.lindachu.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 图片统一存放于 filesDir/images/<dishId>/ 下，对外只暴露相对路径，
 * 这样导出 zip 与同步到 GitHub 时路径保持一致。
 *
 * 写入前统一缩放与压缩，避免原图（数 MB）撑爆备份与仓库。
 */
class ImageStorage(private val context: Context) {

    fun resolve(relativePath: String?): File? =
        relativePath?.let { File(context.filesDir, it) }

    /** 保存图片并返回相对路径，失败返回 null */
    suspend fun save(source: Uri, dishId: String): String? = withContext(Dispatchers.IO) {
        val bitmap = decode(source) ?: return@withContext null
        val target = File(File(context.filesDir, DIR_IMAGES).resolve(dishId).apply { mkdirs() },
            "${UUID.randomUUID()}$EXT")
        val ok = runCatching {
            FileOutputStream(target).use { out ->
                scale(bitmap).compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            }
        }.isSuccess
        if (!ok) return@withContext null
        target.relativeTo(context.filesDir).path
    }

    /** 删除某道菜的全部图片，删除菜谱时调用 */
    fun deleteDishImages(dishId: String) {
        File(context.filesDir, DIR_IMAGES).resolve(dishId).deleteRecursively()
    }

    private fun decode(uri: Uri): Bitmap? {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, null)
        } ?: return null
        return rotateByExif(uri, bitmap)
    }

    /** 相机直出照片常带 EXIF 旋转信息，不处理会躺倒显示 */
    private fun rotateByExif(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scale(bitmap: Bitmap): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= MAX_SIDE) return bitmap
        val ratio = MAX_SIDE.toFloat() / maxSide
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        )
    }

    companion object {
        const val DIR_IMAGES = "images"
        const val EXT = ".jpg"
        const val MAX_SIDE = 1600
        const val QUALITY = 80
    }
}
