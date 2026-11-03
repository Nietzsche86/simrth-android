package id.mts.rth.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import id.mts.rth.core.common.Constants
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * Pengelola penyimpanan dan kompresi media foto siswa secara lokal di memori internal perangkat.
 * Mengompresi gambar ke WebP (Lossy pada API 30+, fallback API 26-29) maksimal 300x300 px
 * untuk mencegah CursorWindow error pada Room SQLite dan menjaga hemat storage HP.
 */
class StudentPhotoManager(private val context: Context) {

    private val photosDir: File
        get() {
            val dir = File(context.filesDir, Constants.STUDENT_PHOTO_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Mengembalikan objek File untuk foto siswa berdasarkan nama filenya.
     */
    fun getPhotoFile(fileName: String): File {
        return File(photosDir, fileName)
    }

    /**
     * Memeriksa apakah file foto sudah ada di cache lokal.
     */
    fun hasPhoto(fileName: String?): Boolean {
        if (fileName.isNullOrBlank()) return false
        val file = getPhotoFile(fileName)
        return file.exists() && file.length() > 0
    }

    /**
     * Menyimpan ByteArray foto yang diunduh dari server langsung ke file lokal.
     */
    fun savePhotoBytes(fileName: String, bytes: ByteArray): File {
        val file = getPhotoFile(fileName)
        file.outputStream().use { it.write(bytes) }
        return file
    }

    /**
     * Mengompresi Bitmap dari galeri/kamera ke format WebP 300x300 px (kualitas 80%)
     * dan menyimpannya ke direktori students/.
     */
    fun compressAndSaveBitmap(sourceBitmap: Bitmap, outputFileName: String): File {
        val scaledBitmap = scaleBitmapToMaxDimensions(
            sourceBitmap,
            Constants.STUDENT_PHOTO_MAX_WIDTH,
            Constants.STUDENT_PHOTO_MAX_HEIGHT
        )

        val outputFile = getPhotoFile(outputFileName)

        val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        FileOutputStream(outputFile).use { out ->
            scaledBitmap.compress(compressFormat, Constants.STUDENT_PHOTO_QUALITY, out)
        }

        if (scaledBitmap != sourceBitmap) {
            scaledBitmap.recycle()
        }

        return outputFile
    }

    /**
     * Skalasi proporsional agar dimensi tidak melebihi maxWidth x maxHeight.
     */
    private fun scaleBitmapToMaxDimensions(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val scaleRatio = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val targetWidth = (width * scaleRatio).toInt()
        val targetHeight = (height * scaleRatio).toInt()

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
