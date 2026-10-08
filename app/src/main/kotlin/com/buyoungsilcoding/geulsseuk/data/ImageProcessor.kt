package com.buyoungsilcoding.geulsseuk.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.buyoungsilcoding.geulsseuk.core.Part
import java.io.ByteArrayOutputStream
import java.io.IOException

/** 사진을 AI·네이버 업로드에 알맞게 줄입니다: 긴 변 1200px, JPEG 품질 82, EXIF 회전 반영. */
object ImageProcessor {
    private const val MAX_SIDE = 1200
    private const val QUALITY = 82

    fun process(context: Context, uri: Uri): Part.Image {
        val cr = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val s1 = cr.openInputStream(uri) ?: throw IOException("사진을 열 수 없어요.")
        s1.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("사진을 읽을 수 없어요.")

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val s2 = cr.openInputStream(uri) ?: throw IOException("사진을 열 수 없어요.")
        var bmp = s2.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IOException("사진을 읽을 수 없어요.")

        val orientation = cr.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        }
        val longest = maxOf(bmp.width, bmp.height)
        if (longest > MAX_SIDE) {
            val f = MAX_SIDE.toFloat() / longest
            m.postScale(f, f)
        }
        if (!m.isIdentity) {
            val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (out !== bmp) bmp.recycle()
            bmp = out
        }

        val bos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, QUALITY, bos)
        bmp.recycle()
        return Part.Image(bos.toByteArray(), "image/jpeg")
    }
}
