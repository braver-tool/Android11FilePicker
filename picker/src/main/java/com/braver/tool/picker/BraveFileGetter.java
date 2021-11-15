/*
 *
 *  * Created by https://github.com/braver-tool on 12/10/21, 08:30 PM
 *  * Copyright (c) 2021 . All rights reserved.
 *  * Last modified 15/11/21, 05:50 PM
 *
 */

package com.braver.tool.picker;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.File;

public class BraveFileGetter {

    public static String getFileType(BraveFileType braveFileType) {
        switch (braveFileType) {
            case IMAGE:
                return "image";
            case VIDEO:
                return "video";
            case DOCS:
                return "docs";
            default:
                return "";
        }
    }

    public static File getSourceFileFromUri(Context context, boolean isCompressImage, boolean isTrimVide, BraveFileType braveFileType, String destinationFilePath, Uri contentUri) {
        File file = null;
        String fileType = getFileType(braveFileType);
        if (fileType.equals("image")) {
            //handle image
            String filepath = BraveFileUtils.getFilePathFromContentUri(contentUri, context);
            Bitmap imageBitMap = BraveFileUtils.checkIsBitMapRotated(context, true, new File(filepath));
            if (imageBitMap != null) {
                file = new File(destinationFilePath);
                BraveFileUtils.saveBitmapToExternalStorage(context, imageBitMap, file);
            }
        }
        return file;
    }
}
