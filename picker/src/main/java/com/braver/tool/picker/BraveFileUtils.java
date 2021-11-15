package com.braver.tool.picker;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import com.braver.tool.picker.image.ImageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.Random;

import static com.braver.tool.picker.BraveFilePath.getExternalSdCardPath;

public class BraveFileUtils {
    public static final String IS_IMAGE_FILE = "is_image_file";
    public static final String IMAGE_PATH = "image_path";
    public static final String VIDEO_PATH = "video_path";
    private static final String TAG = BraveFileUtils.class.getSimpleName();
    public static boolean IS_LOG_ENABLED = true;
    public static final String PREVIEW_IMAGE_FILE_PATH = "preview_image_file_path";
    public static final String PREVIEW_VIDEO_FILE_PATH = "preview_video_file_path";

    /**
     * @param tag - Contains class name
     * @param msg - Log message as String
     *            Method used to print log in console for development
     */
    public static void printLogConsole(String tag, String msg) {
        if (IS_LOG_ENABLED) {
            Log.d("##@@##" + tag, "--------->" + msg);
        }
    }

    public static File getRandomDocFileName(Context context, String extension) {
        File file = null;
        File mediaStorageDir = context.getFilesDir();
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return file;
            }
        }
        int random = new Random().nextInt(7997);
        String mImageName = "Braver_DOC".concat("_") + random + "." + extension;
        return new File(mediaStorageDir.getPath() + "/" + mImageName);
    }

    public static File getRandomImageFileName(Context context) {
        File file = null;
        File mediaStorageDir = context.getFilesDir();
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return file;
            }
        }
        int random = new Random().nextInt(8997);
        String mImageName = "Braver_IMG".concat("_") + random + ".jpg";
        return new File(mediaStorageDir.getPath() + "/" + mImageName);
    }

    public static File getRandomRecordedVideoFileName(Context context) {
        File file = null;
        File mediaStorageDir = context.getFilesDir();
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return file;
            }
        }
        return new File(mediaStorageDir.getPath());
    }


    public static void saveBitmapToExternalStorage(Context context, Bitmap bitmap, File myPath) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(myPath);
            //String fileName = Utils.getMessageFileName(myPath.getAbsolutePath());
            //fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
        } catch (Exception e) {
            printLogConsole(TAG + "##saveBitmapToExternalStorage", "Exception------>" + e.getMessage());
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                printLogConsole(TAG + "##saveBitmapToExternalStorage", "Exception------>" + e.getMessage());
            }
        }
    }

    /**
     * Declaration  saveDocToLocalPath(Context context, String filePath)
     * Description   This method used  save local path file and return name
     * Declared In  RealDocPathUtil.java
     *
     * @param filePath - Contains document file for the particular image path
     */

    public static String saveDocToExternalStorage(Context context, String filePath) {
        try {
            String splitName;
            if (filePath.contains(":")) {
                final String[] split = filePath.split(":");
                filePath = getExternalSdCardPath(context) + split[1];
            }
            splitName = filePath.substring(filePath.lastIndexOf('/') + 1);
            String extension = splitName.substring(splitName.lastIndexOf("."));
            File designPath = getRandomDocFileName(context, extension);
            File sourcePath = new File(filePath);
            FileChannel in = new FileInputStream(sourcePath).getChannel();
            FileChannel out = new FileOutputStream(designPath).getChannel();
            try {
                in.transferTo(0, in.size(), out);
            } catch (Exception e) {
                printLogConsole(TAG + "##saveDocToExternalStorage", "Exception------>" + e.getMessage());
            } finally {
                if (in != null) in.close();
                if (out != null) out.close();
            }
            return designPath.getAbsolutePath();
        } catch (Exception ioe) {
            printLogConsole(TAG + "##saveDocToExternalStorage", "Exception------>" + ioe.getMessage());
            return "";
        }
    }

    /**
     * Method used to get the real path from uri file
     *
     * @param context context
     * @return filepath
     */

    public static String getFilePathFromContentUri(Uri uri, Context context) {
        String filePath;
        Uri contentUri = null;
        String id;
        try {
            id = DocumentsContract.getDocumentId(uri);
        } catch (IllegalArgumentException e) {
            id = "";
            printLogConsole(TAG, "Uri Null");
        }
        if (!id.isEmpty()) {
            boolean numeric = id.matches("-?\\d+(\\.\\d+)?");
            if (numeric) {
                contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
            }
        } else {
            contentUri = uri;
        }

        String[] projection = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = null;
        if (contentUri != null) {
            cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
        }
        if (cursor != null) {
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            filePath = cursor.getString(column_index);
            cursor.close();
        } else {
            filePath = uri.getPath();
        }
        return filePath;
    }


    /**
     * Declaration checkIsBitMapRotated(File imageFile)
     * Some devices returns wrong(auto - rotated) images
     * Description   This method used  to get actual captured image from device .
     * Declared In  Utils.java
     *
     * @param imageFile - Contains image file for the particular image path
     */

    public static Bitmap checkIsBitMapRotated(Context context, boolean isCompress, File imageFile) {
        try {
            boolean isComp = true;
            File compressedImageFile;
            long bytes = imageFile.length();
            if (bytes > 5242880 || isCompress) { // Size must below 5MB
                compressedImageFile = ImageUtil.compressImage(imageFile, getRandomImageFileName(context).getAbsolutePath());
            } else {
                isComp = false;
                compressedImageFile = imageFile;
            }
            printLogConsole(TAG, "Image File Size----->" + getFileSizeFromFilePath(compressedImageFile.length()));
            Bitmap photoBitmap = BitmapFactory.decodeFile(compressedImageFile.getPath());
            int imageRotation = getImageRotation(compressedImageFile);
            if (imageRotation != 0) {
                photoBitmap = getBitmapRotatedByDegree(photoBitmap, imageRotation);
            }
            if (!isComp) {
                photoBitmap = Bitmap.createScaledBitmap(photoBitmap, photoBitmap.getWidth(), photoBitmap.getHeight(), true);
            }
            return photoBitmap;
        } catch (IOException e) {
            printLogConsole(TAG, "checkIsBitMapRotated()------>Exception----->" + e.getMessage());
            return null;
        }
    }


    /**
     * Declaration getImageRotation(final File imageFile
     * Description   This method used  to set ExifInterface
     * Declared In  Utils.java
     *
     * @param imageFile - Contains image file for the particular image path
     */
    private static int getImageRotation(final File imageFile) {
        ExifInterface exif = null;
        int exifRotation = 0;
        try {
            exif = new ExifInterface(imageFile.getPath());
            exifRotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (exif == null) return 0;
        else return exifToDegrees(exifRotation);
    }

    /**
     * Declaration exifToDegrees(int rotation)
     * Description   This method used  to set ExifInterface with rotation degree
     * Declared In  Utils.java
     *
     * @param rotation - Contains rotation degree for the captured image
     */
    private static int exifToDegrees(int rotation) {
        if (rotation == ExifInterface.ORIENTATION_ROTATE_90) return 90;
        else if (rotation == ExifInterface.ORIENTATION_ROTATE_180) return 180;
        else if (rotation == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        return 0;
    }

    /**
     * Declaration  Bitmap getBitmapRotatedByDegree(Bitmap bitmap, int rotationDegree)
     * Description   This method used  to set bitmap height and width
     * Declared In  Utils.java
     *
     * @param bitmap         - Contains captured image bitmap
     * @param rotationDegree - Contains captured image rotation degree
     */
    private static Bitmap getBitmapRotatedByDegree(Bitmap bitmap, int rotationDegree) {
        Matrix matrix = new Matrix();
        matrix.preRotate(rotationDegree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * @param size - long data
     * @return - String data file size
     * Method used to get ile size
     */
    public static String getFileSizeFromFilePath(long size) {
        if (size <= 0L) {
            return "0";
        } else {
            String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
            double var5 = (double) size;
            double var10000 = Math.log10(var5);
            var5 = 1024.0D;
            int digitGroups = (int) (var10000 / Math.log10(var5));
            StringBuilder var10 = new StringBuilder();
            DecimalFormat var10001 = new DecimalFormat("#,##0.#");
            double var10002 = (double) size;
            var5 = 1024.0D;
            return var10.append(var10001.format(var10002 / Math.pow(var5, digitGroups))).append(" ").append(units[digitGroups]).toString();
        }
    }
}
