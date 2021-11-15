package com.braver.tool.picker;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

public class BraveFilePicker {
    private Context context;
    private boolean isCompressImage = false;
    private boolean isTrimVide = false;
    //private String fileType;
    private BraveFileType braveFileType;
    private String destinationFilePath;
    private Uri contentUri;

    public BraveFilePicker setActivity(Context ctx) {
        this.context = ctx;
        return this;
    }

    public BraveFilePicker setIsCompressImage(boolean compressImage) {
        this.isCompressImage = compressImage;
        return this;
    }

    public BraveFilePicker setIsTrimVide(boolean trimVide) {
        isTrimVide = trimVide;
        return this;
    }

    public BraveFilePicker setFileType(BraveFileType fileType) {
        this.braveFileType = fileType;
        return this;
    }

    public BraveFilePicker setDestinationFilePath(String destinationFilePath) {
        this.destinationFilePath = destinationFilePath;
        return this;
    }

    public BraveFilePicker setContentUri(Uri uri) {
        this.contentUri = uri;
        return this;
    }

    public File getSourceFile() throws IOException {
        return BraveFileGetter.getSourceFileFromUri(context, isCompressImage, isTrimVide, braveFileType, destinationFilePath, contentUri);
    }

}
