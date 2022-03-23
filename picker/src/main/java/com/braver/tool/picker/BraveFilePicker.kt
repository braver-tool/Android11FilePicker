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
import com.braver.tool.picker.BraveFileGetter.getSourceFileFromUri
import java.io.File
import java.io.IOException
import kotlin.Throws

class BraveFilePicker {
    private var context: Context? = null
    private var isCompressImage = false
    private var isTrimVide = false

    //private String fileType;
    private var braveFileType: BraveFileType? = null
    private var destinationFilePath: String? = null
    private var contentUri: Uri? = null
    fun setActivity(ctx: Context?): BraveFilePicker {
        context = ctx
        return this
    }

    fun setIsCompressImage(compressImage: Boolean): BraveFilePicker {
        isCompressImage = compressImage
        return this
    }

    fun setIsTrimVide(trimVide: Boolean): BraveFilePicker {
        isTrimVide = trimVide
        return this
    }

    fun setFileType(fileType: BraveFileType?): BraveFilePicker {
        braveFileType = fileType
        return this
    }

    fun setDestinationFilePath(destinationFilePath: String?): BraveFilePicker {
        this.destinationFilePath = destinationFilePath
        return this
    }

    fun setContentUri(uri: Uri?): BraveFilePicker {
        contentUri = uri
        return this
    }

    @get:Throws(IOException::class)
    val sourceFile: File?
        get() = getSourceFileFromUri(
            context,
            isCompressImage,
            isTrimVide,
            braveFileType,
            destinationFilePath,
            contentUri
        )
}