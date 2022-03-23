/*
 *
 *  * Created by https://github.com/braver-tool on 12/10/21, 08:30 PM
 *  * Copyright (c) 2021 . All rights reserved.
 *  * Last modified 23/03/22, 09:45 AM
 *
 */
package com.braver.tool.picker


import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.braver.tool.picker.BraveFilePath.Companion.getExternalSdCardPath
import com.braver.tool.picker.image.ImageUtil.compressImage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

class BraveFileUtils {
    companion object {
        const val IS_IMAGE_FILE = "is_image_file"
        const val IMAGE_PATH = "image_path"
        const val VIDEO_PATH = "video_path"
        private val TAG = BraveFileUtils::class.java.simpleName
        var IS_LOG_ENABLED = true
        const val PREVIEW_IMAGE_FILE_PATH = "preview_image_file_path"
        const val PREVIEW_VIDEO_FILE_PATH = "preview_video_file_path"

        /**
         * @param tag - Contains class name
         * @param msg - Log message as String
         * Method used to print log in console for development
         */
        fun printLogConsole(tag: String, msg: String) {
            if (IS_LOG_ENABLED) {
                Log.d("##@@##$tag", "--------->$msg")
            }
        }

        @JvmStatic
        fun getRandomImageFileName(context: Context): File? {
            val file: File? = null
            val mediaStorageDir = context.filesDir
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return file
                }
            }
            val random = Random().nextInt(8997)
            val mImageName = "Braver_IMG_$random.jpg"
            return File(mediaStorageDir.path + "/" + mImageName)
        }

        @JvmStatic
        fun getRandomRecordedVideoFileName(context: Context): File? {
            val file: File? = null
            val mediaStorageDir = context.filesDir
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return file
                }
            }
            return File(mediaStorageDir.path)
        }

        @JvmStatic
        fun saveBitmapToExternalStorage(context: Context?, bitmap: Bitmap, myPath: File?) {
            var fileOutputStream: FileOutputStream? = null
            try {
                fileOutputStream = FileOutputStream(myPath)
                //String fileName = Utils.getMessageFileName(myPath.getAbsolutePath());
                //fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            } catch (e: Exception) {
                printLogConsole(
                    "$TAG##saveBitmapToExternalStorage",
                    "Exception------>" + e.message
                )
            } finally {
                try {
                    fileOutputStream?.close()
                } catch (e: IOException) {
                    printLogConsole(
                        "$TAG##saveBitmapToExternalStorage",
                        "Exception------>" + e.message
                    )
                }
            }
        }


        /**
         * Method used to get the real path from uri file
         *
         * @param context context
         * @return filepath
         */
        fun getFilePathFromContentUri(uri: Uri?, context: Context?): String {
            val filePath: String
            var contentUri: Uri? = null
            var id: String
            try {
                id = DocumentsContract.getDocumentId(uri)
            } catch (e: IllegalArgumentException) {
                id = ""
                printLogConsole(TAG, "Uri Null")
            }
            if (id.isNotEmpty()) {
                val numeric = id.matches("-?\\d+(\\.\\d+)?".toRegex())
                if (numeric) {
                    contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        id.toLong()
                    )
                }
            } else {
                contentUri = uri
            }
            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
            var cursor: Cursor? = null
            if (contentUri != null) {
                cursor = context?.contentResolver?.query(contentUri, projection, null, null, null)
            }
            if (cursor != null) {
                cursor.moveToFirst()
                val columnIndex = cursor.getColumnIndexOrThrow(projection[0])
                filePath = cursor.getString(columnIndex)
                cursor.close()
            } else {
                filePath = uri?.path.toString()
            }
            return filePath
        }

        /**
         * Declaration checkIsBitMapRotated(File imageFile)
         * Some devices returns wrong(auto - rotated) images
         * Description   This method used  to get actual captured image from device .
         * Declared In  Utils.java
         *
         * @param imageFile - Contains image file for the particular image path
         */
        @JvmStatic
        fun checkIsBitMapRotated(context: Context?, isCompress: Boolean, imageFile: File): Bitmap? {
            return try {
                var isComp = true
                val compressedImageFile: File?
                val bytes = imageFile.length()
                if (bytes > 5242880 || isCompress) { // Size must below 5MB
                    compressedImageFile = compressImage(
                        imageFile, context?.let { getRandomImageFileName(it) }!!
                            .absolutePath
                    )
                } else {
                    isComp = false
                    compressedImageFile = imageFile
                }
                printLogConsole(
                    TAG,
                    "Image File Size----->" + getFileSizeFromFilePath(
                        compressedImageFile.length()
                    )
                )
                var photoBitmap = BitmapFactory.decodeFile(compressedImageFile.path)
                val imageRotation = getImageRotation(compressedImageFile)
                if (imageRotation != 0) {
                    photoBitmap = getBitmapRotatedByDegree(photoBitmap, imageRotation)
                }
                if (!isComp) {
                    photoBitmap = Bitmap.createScaledBitmap(
                        photoBitmap,
                        photoBitmap.width,
                        photoBitmap.height,
                        true
                    )
                }
                photoBitmap
            } catch (e: IOException) {
                printLogConsole(TAG, "checkIsBitMapRotated()------>Exception----->" + e.message)
                null
            }
        }

        /**
         * Declaration getImageRotation(final File imageFile
         * Description   This method used  to set ExifInterface
         * Declared In  Utils.java
         *
         * @param imageFile - Contains image file for the particular image path
         */
        private fun getImageRotation(imageFile: File): Int {
            var exif: ExifInterface? = null
            var exifRotation = 0
            try {
                exif = ExifInterface(imageFile.path)
                exifRotation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return if (exif == null) 0 else exifToDegrees(exifRotation)
        }

        /**
         * Declaration exifToDegrees(int rotation)
         * Description   This method used  to set ExifInterface with rotation degree
         * Declared In  Utils.java
         *
         * @param rotation - Contains rotation degree for the captured image
         */
        private fun exifToDegrees(rotation: Int): Int {
            if (rotation == ExifInterface.ORIENTATION_ROTATE_90) return 90 else if (rotation == ExifInterface.ORIENTATION_ROTATE_180) return 180 else if (rotation == ExifInterface.ORIENTATION_ROTATE_270) return 270
            return 0
        }

        /**
         * Declaration  Bitmap getBitmapRotatedByDegree(Bitmap bitmap, int rotationDegree)
         * Description   This method used  to set bitmap height and width
         * Declared In  Utils.java
         *
         * @param bitmap         - Contains captured image bitmap
         * @param rotationDegree - Contains captured image rotation degree
         */
        private fun getBitmapRotatedByDegree(bitmap: Bitmap, rotationDegree: Int): Bitmap {
            val matrix = Matrix()
            matrix.preRotate(rotationDegree.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        /**
         * @param size - long data
         * @return - String data file size
         * Method used to get ile size
         */
        private fun getFileSizeFromFilePath(size: Long): String {
            return if (size <= 0L) {
                "0"
            } else {
                val units = arrayOf("B", "KB", "MB", "GB", "TB")
                var var5 = size.toDouble()
                val var10000 = log10(var5)
                var5 = 1024.0
                val digitGroups = (var10000 / log10(var5)).toInt()
                val var10 = StringBuilder()
                val var10001 = DecimalFormat("#,##0.#")
                val var10002 = size.toDouble()
                var5 = 1024.0
                var10.append(var10001.format(var10002 / var5.pow(digitGroups.toDouble())))
                    .append(" ").append(units[digitGroups]).toString()
            }
        }
    }
}