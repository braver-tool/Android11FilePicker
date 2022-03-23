/*
 *
 *  * Created by https://github.com/braver-tool on 12/10/21, 08:30 PM
 *  * Copyright (c) 2021 . All rights reserved.
 *  * Last modified 23/03/22, 09:45 AM
 *
 */
package com.braver.tool.picker

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.*

class BraveFilePath {
    companion object {

        /**
         * Get a file path from a Uri. This will get the the path for Storage Access
         * Framework Documents, as well as the _data field for the MediaStore and
         * other file-based ContentProviders.
         *
         * @param context The context.
         * @param uri     The Uri to query.
         * @author paulburke
         */
        @SuppressLint("NewApi")
        fun getRealPathFromURI(context: Context, uri: Uri): String? {
            when {
                DocumentsContract.isDocumentUri(context, uri) -> {
                    when {
                        isExternalStorageDocument(uri) -> {
                            val docId = DocumentsContract.getDocumentId(uri)
                            val split = docId.split(":".toRegex()).toTypedArray()
                            val type = split[0]
                            return when {
                                "primary".equals(type, ignoreCase = true) -> {
                                    Environment.getExternalStorageDirectory()
                                        .toString() + "/" + split[1]
                                }
                                isGoogleDriveUri(uri) -> {
                                    getDriveFilePath(uri, context)
                                }
                                else -> {
                                    BraveFileUtils.getFilePathFromContentUri(uri, context)
                                }
                            }
                        }
                        isGoogleDriveUri(uri) -> {
                            return getDriveFilePath(uri, context)
                        }
                        isDownloadsDocument(uri) -> {
                            val fileName = getFilePath(context, uri)
                            if (fileName != null) {
                                return Environment.getExternalStorageDirectory()
                                    .toString() + "/Download/" + fileName
                            }
                            var id = DocumentsContract.getDocumentId(uri)
                            if (id.startsWith("raw:")) {
                                id = id.replaceFirst("raw:".toRegex(), "")
                                val file = File(id)
                                if (file.exists()) return id
                            }
                            val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"),
                                id.toLong()
                            )
                            return getDataColumn(context, contentUri, null, null)
                        }
                        isMediaDocument(uri) -> {
                            // MediaProvider
                            try {
                                val docId = DocumentsContract.getDocumentId(uri)
                                val split = docId.split(":".toRegex()).toTypedArray()
                                val type = split[0]
                                var contentUri: Uri? = null
                                if ("image" == type) {
                                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                }
                                val selection = "_id=?"
                                val selectionArgs = arrayOf(split[1])
                                return getDataColumn(context, contentUri, selection, selectionArgs)
                            } catch (e: Exception) {
                                Log.d("##RealDocPathUtil", "--------->" + e.message)
                                val fileName = getFilePath(context, uri)
                                if (fileName != null) {
                                    var file = Environment.getExternalStorageDirectory()
                                        .toString() + "/Download/" + fileName
                                    val file1 = File(file)
                                    return if (file1.exists()) {
                                        file
                                    } else {
                                        file = copyFileToInternalStorage(context, uri, "userfiles")
                                        file
                                    }
                                }
                            }
                        }
                        else -> {
                            return BraveFileUtils.getFilePathFromContentUri(uri, context)
                        }
                    }
                }
                "content".equals(uri.scheme, ignoreCase = true) -> {
                    return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                        context,
                        uri,
                        null,
                        null
                    )
                }
                "file".equals(uri.scheme, ignoreCase = true) -> {
                    return uri.path
                }
            }
            return null
        }

        fun getFilePath(context: Context, uri: Uri?): String? {
            var cursor: Cursor? = null
            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME
            )
            try {
                cursor = context.contentResolver.query(uri!!, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    return cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        /**
         * Get the value of the data column for this Uri. This is useful for
         * MediaStore Uris, and other file-based ContentProviders.
         *
         * @param context       The context.
         * @param uri           The Uri to query.
         * @param selection     (Optional) Filter used in the query.
         * @param selectionArgs (Optional) Selection arguments used in the query.
         * @return The value of the _data column, which is typically a file path.
         */
        fun getDataColumn(
            context: Context,
            uri: Uri?,
            selection: String?,
            selectionArgs: Array<String>?
        ): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(column)
            try {
                cursor =
                    context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is Google Photos.
         */
        fun isGooglePhotosUri(uri: Uri): Boolean {
            return "com.google.android.apps.photos.content" == uri.authority
        }

        fun isGoogleDriveUri(uri: Uri): Boolean {
            return "com.google.android.apps.docs.storage" == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority
        }

        private fun getDriveFilePath(uri: Uri, context: Context): String {
            //String[] split2;
            val returnCursor = context.contentResolver.query(uri, null, null, null, null)
            /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
            val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
            returnCursor.moveToFirst()
            val name = returnCursor.getString(nameIndex)
            val size = java.lang.Long.toString(returnCursor.getLong(sizeIndex))
            //split2 = name.split("\\.");
            //File file = new File(context.getCacheDir(), name);
            val designPath = getRandomDocFileName(context, name)
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(designPath)
                var read = 0
                val maxBufferSize = 1024 * 1024
                val bytesAvailable = inputStream!!.available()

                //int bufferSize = 1024;
                val bufferSize = Math.min(bytesAvailable, maxBufferSize)
                val buffers = ByteArray(bufferSize)
                while (inputStream.read(buffers).also { read = it } != -1) {
                    outputStream.write(buffers, 0, read)
                }
                inputStream.close()
                outputStream.close()
            } catch (e: Exception) {
                BraveFileUtils.printLogConsole("##isGoogleDriveUri", "Exception------>" + e.message)
            }
            return designPath!!.path
        }

        /**
         * Declaration  getExternalSdCardPath(Context context)
         * Description   This method used  save local path file and return file path name
         * Declared In  RealDocPathUtil.java
         *
         * @param context - Contains Context
         */
        @JvmStatic
        fun getExternalSdCardPath(context: Context?): String {
            val dirs = ContextCompat.getExternalFilesDirs(
                context!!, null
            )
            val path = dirs[1]
            val pathSD = dirs[1].toString()
            val firstOpen = pathSD.indexOf("/")
            val secondOpen = pathSD.indexOf("/", firstOpen + 1)
            val thirdOpen = pathSD.indexOf("/", secondOpen + 1)
            return pathSD.substring(firstOpen, thirdOpen + 1)
            //path = new File(filename);
        }

        /**
         * @return - file path
         * method used to generate full file path using selected file name
         */
        fun getRandomDocFileName(context: Context, fileName: String): File? {
            val file: File? = null
            val mediaStorageDir = context.filesDir
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return file
                }
            }
            val random = Random().nextInt(6997)
            val mImageName = "Braver_DOC_$random$fileName"
            return File(mediaStorageDir.path + "/" + mImageName)
        }

        /***
         * Used for Android Q+
         * @param uri - ~
         * @param newDirName if you want to create a directory, you can set this variable
         * @return - ~
         */
        private fun copyFileToInternalStorage(
            context: Context,
            uri: Uri,
            newDirName: String
        ): String {
            val returnCursor = context.contentResolver.query(
                uri, arrayOf(
                    OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
                ), null, null, null
            )
            /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
            val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
            returnCursor.moveToFirst()
            val name = returnCursor.getString(nameIndex)
            val size = java.lang.Long.toString(returnCursor.getLong(sizeIndex))
            val output: File = if (newDirName != "") {
                val dir = File(context.filesDir.toString() + "/" + newDirName)
                if (!dir.exists()) {
                    dir.mkdir()
                }
                File(context.filesDir.toString() + "/" + newDirName + "/" + name)
            } else {
                File(context.filesDir.toString() + "/" + name)
            }
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(output)
                var read = 0
                val bufferSize = 1024
                val buffers = ByteArray(bufferSize)
                while (inputStream!!.read(buffers).also { read = it } != -1) {
                    outputStream.write(buffers, 0, read)
                }
                inputStream.close()
                outputStream.close()
            } catch (e: Exception) {
                BraveFileUtils.printLogConsole(
                    "##copyFileToInternalStorage",
                    "Exception------>" + e.message
                )
            }
            return output.path
        }
    }
}