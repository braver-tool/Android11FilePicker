/*
 *
 *  * Created by https://github.com/braver-tool on 12/10/21, 08:30 PM
 *  * Copyright (c) 2021 . All rights reserved.
 *  * Last modified 23/03/22, 09:45 AM
 *
 */
package com.braver.tool.picker

import android.content.Context
import android.net.Uri
import java.io.File

object BraveFileGetter {
    private fun getFileType(braveFileType: BraveFileType?): String {
        return when (braveFileType) {
            BraveFileType.IMAGE -> "image"
            BraveFileType.VIDEO -> "video"
            BraveFileType.DOCS -> "docs"
            else -> ""
        }
    }

    @JvmStatic
    fun getSourceFileFromUri(
        context: Context?,
        isCompressImage: Boolean,
        isTrimVide: Boolean,
        braveFileType: BraveFileType?,
        destinationFilePath: String?,
        contentUri: Uri?
    ): File? {
        var file: File? = null
        val fileType = getFileType(braveFileType)
        if (fileType == "image") {
            //handle image
            val filepath = BraveFileUtils.getFilePathFromContentUri(contentUri, context)
            val imageBitMap = BraveFileUtils.checkIsBitMapRotated(context, true, File(filepath))
            if (imageBitMap != null && destinationFilePath != null) {
                file = File(destinationFilePath)
                BraveFileUtils.saveBitmapToExternalStorage(context, imageBitMap, file)
            }
        }
        return file
    }
}