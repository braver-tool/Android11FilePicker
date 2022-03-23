/*
 *
 *  * Created by https://github.com/braver-tool on 12/10/21, 08:30 PM
 *  * Copyright (c) 2021 . All rights reserved.
 *  * Last modified 23/03/22, 09:45 AM
 *
 */
package com.braver.tool.picker.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageUtil {
    /**
     * @param imageFile       - source file
     * @param destinationPath - design path of compressed image
     * @return - file path
     * @throws IOException - error message
     * Method used to compress image
     */
    @JvmStatic
    @Throws(IOException::class)
    fun compressImage(imageFile: File, destinationPath: String): File {
        var fileOutputStream: FileOutputStream? = null
        val file = File(destinationPath).parentFile
        if (!file.exists()) {
            file.mkdirs()
        }
        try {
            fileOutputStream = FileOutputStream(destinationPath)
            // write the compressed bitmap at the destination specified by destinationPath.
            decodeSampledBitmapFromFile(imageFile).compress(
                Bitmap.CompressFormat.JPEG,
                80,
                fileOutputStream
            )
        } finally {
            if (fileOutputStream != null) {
                fileOutputStream.flush()
                fileOutputStream.close()
            }
        }
        return File(destinationPath)
    }

    /**
     * @param imageFile - source file
     * @return - image as bitmap
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decodeSampledBitmapFromFile(imageFile: File): Bitmap {
        //Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getPath());
        //int reqWidth = bitmap.getWidth();
        //int reqHeight = bitmap.getHeight();
        val reqWidth = 612
        val reqHeight = 816
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imageFile.absolutePath, options)
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        var scaledBitmap = BitmapFactory.decodeFile(imageFile.absolutePath, options)
        //check the rotation of the image and display it properly
        val exif = ExifInterface(imageFile.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
        val matrix = Matrix()
        when (orientation) {
            6 -> {
                matrix.postRotate(90f)
            }
            3 -> {
                matrix.postRotate(180f)
            }
            8 -> {
                matrix.postRotate(270f)
            }
        }
        scaledBitmap = Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrix,
            true
        )
        return scaledBitmap
    }

    /**
     * @param options   - BitmapFactory
     * @param reqWidth  - image width
     * @param reqHeight - image height
     * @return - image size
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}