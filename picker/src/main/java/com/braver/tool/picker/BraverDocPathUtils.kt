package com.braver.tool.picker

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream


class BraverDocPathUtils {
    companion object {
        @SuppressLint("Range")
        fun getSourceDocPath(
            context: Context,
            uridata: Uri
        ): String? {
            var displayname = ""
            var size = 0
            val contentResolver = context.contentResolver
            if (contentResolver.equals(null)) {
                return null
            }
            contentResolver.query(
                uridata, null, null, null, null, null
            )?.use {
                if (it.moveToFirst()) {
                    displayname = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    size = it.getColumnIndex(OpenableColumns.SIZE)
                }
            }
            //val filepath = context.applicationInfo.dataDir + File.separator + userID + "_" + System.currentTimeMillis() + displayname
            val filepath =
                context.filesDir.absolutePath + File.separator + "_" + System.currentTimeMillis() + displayname
            val splitName = filepath.substring(filepath.lastIndexOf('/') + 1)
            val file = File(filepath)
            try {
                val inputStream = contentResolver.openInputStream(uridata) ?: return null
                val outputStream = FileOutputStream(file)
                val bufferbyte = ByteArray(1024)
                var len: Int
                while (inputStream.read(bufferbyte)
                        .also { len = it } > 0
                ) outputStream.write(bufferbyte, 0, len)
                outputStream.close()
                inputStream.close()
            } catch (e: Exception) {
                return ""
            }
            Log.d("##file.name", "-------->" + file.name)
            Log.d("##file.uridata", "-------->$uridata")
            Log.d("##file.splitName", "-------->$splitName")
            Log.d("##file.size", "-------->$size")
            Log.d("##file.absolutePath", "-------->" + file.absolutePath)
            return file.path
        }

        fun getPath(context: Context, uri: Uri): String? {
            val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    when (type) {
                        "image" -> {
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }
                        "video" -> {
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        }
                        "audio" -> {
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }

                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(
                        split[1]
                    )
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                return getDataColumn(context, uri, null, null)
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }
            return null
        }

        private fun getDataColumn(
            context: Context,
            uri: Uri?,
            selection: String?,
            selectionArgs: Array<String>?
        ): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(
                column
            )
            try {

                cursor = context.contentResolver.query(
                    uri!!, projection, selection, selectionArgs,
                    null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(columnIndex)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

    }
}
