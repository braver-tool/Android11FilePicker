/* * *  * Created by https://github.com/braver-tool on 12/10/21, 08:30 PM *  * Copyright (c) 2021 . All rights reserved. *  * Last modified 15/11/21, 05:50 PM * */package com.braver.tool.filepicker;import android.Manifest;import android.app.Activity;import android.app.Dialog;import android.content.Context;import android.content.Intent;import android.content.pm.PackageManager;import android.net.Uri;import android.os.Build;import android.os.Environment;import android.provider.Settings;import android.view.Gravity;import android.view.View;import android.view.Window;import android.view.WindowManager;import android.widget.ImageView;import android.widget.TextView;import androidx.core.app.ActivityCompat;import java.util.ArrayList;import java.util.Arrays;import java.util.Collections;import java.util.List;import java.util.Objects;/** * Class for android 6.0 and above live permission handling */public class PermissionUtils {    public static final String PERMISSION_CAMERA = "Camera";    public static final String PERMISSION_GALLERY = "Gallery";    public static final String PERMISSION_MIC = "Audio";    public static final int MANDATORY_GALLERY_PERMISSION_REQ_CODE = 3646;    public static final String READ_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;    public static final String IS_PERMISSION_DIALOG_ANDROID_11 = "is_permission_dialog_android_11";    /**     * Method helps to add a permission into permission list     */    private static void checkPermissionGrant(Context context, ArrayList<String> permissionList, String permission) {        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {            permissionList.add(permission);        }    }    /**     * Method handling the permissions to getting camera and storage access     *     * @param context activity context     * @return boolean variable     */    public static boolean isFileAccessPermissionGranted(Context context) {        boolean status = true;        ArrayList<String> locationPermissionList = new ArrayList<>();        checkPermissionGrant(context, locationPermissionList, Manifest.permission.CAMERA);        checkPermissionGrant(context, locationPermissionList, Manifest.permission.RECORD_AUDIO);        checkPermissionGrant(context, locationPermissionList, Manifest.permission.READ_EXTERNAL_STORAGE);        //checkPermissionGrant(context, locationPermissionList, Manifest.permission.WRITE_EXTERNAL_STORAGE);        if (locationPermissionList.size() > 0) {            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && locationPermissionList.contains(Manifest.permission.READ_EXTERNAL_STORAGE) && locationPermissionList.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) && Environment.isExternalStorageManager()) {                locationPermissionList.remove(Manifest.permission.READ_EXTERNAL_STORAGE);                //locationPermissionList.remove(Manifest.permission.WRITE_EXTERNAL_STORAGE);            }        }        if (locationPermissionList.size() > 0) {            status = false;        }        return status;    }    /**     * Method to display the dialog alert for camera and storage permission     *     * @param context activity context     */    public static void showFileAccessPermissionRequestDialog(Context context) {        ArrayList<String> locationPermissionList = new ArrayList<>();        checkPermissionGrant(context, locationPermissionList, Manifest.permission.CAMERA);        checkPermissionGrant(context, locationPermissionList, Manifest.permission.RECORD_AUDIO);        checkPermissionGrant(context, locationPermissionList, Manifest.permission.READ_EXTERNAL_STORAGE);        //checkPermissionGrant(context, locationPermissionList, Manifest.permission.WRITE_EXTERNAL_STORAGE);        if (locationPermissionList.size() > 0) {            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && locationPermissionList.contains(Manifest.permission.READ_EXTERNAL_STORAGE) && locationPermissionList.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) && Environment.isExternalStorageManager()) {                locationPermissionList.remove(Manifest.permission.READ_EXTERNAL_STORAGE);                //locationPermissionList.remove(Manifest.permission.WRITE_EXTERNAL_STORAGE);            }        }        if (locationPermissionList.size() > 0) {            ActivityCompat.requestPermissions((Activity) context, locationPermissionList.toArray(new String[locationPermissionList.size()]), MANDATORY_GALLERY_PERMISSION_REQ_CODE);        }    }    /**     * Method handling the permissions to isCameraAccessed     *     * @param context activity context     * @return boolean variable     */    public static boolean isCameraAccessed(Context context) {        boolean status = true;        ArrayList<String> locationPermissionList = new ArrayList<>();        checkPermissionGrant(context, locationPermissionList, Manifest.permission.CAMERA);        if (locationPermissionList.size() > 0) {            status = false;        }        return status;    }    /**     * Method handling the permissions to isAudioAccessed     *     * @param context activity context     * @return boolean variable     */    public static boolean isAudioAccessed(Context context) {        boolean status = true;        ArrayList<String> locationPermissionList = new ArrayList<>();        checkPermissionGrant(context, locationPermissionList, Manifest.permission.RECORD_AUDIO);        if (locationPermissionList.size() > 0) {            status = false;        }        return status;    }    /**     * @param context        - Dialog call from     * @param permissionType - Permission for ?     */    public static void showAlertForAppInfoScreen(Context context, String permissionType) {        if (!isValidPermissionDialog(context, permissionType)) {            return;        }        final Dialog dialog = new Dialog(context);        dialog.setContentView(R.layout.permission_alert_popup);        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);        Window window = dialog.getWindow();        WindowManager.LayoutParams wlp;        if (window != null) {            wlp = window.getAttributes();            wlp.gravity = Gravity.CENTER;            window.setAttributes(wlp);        }        ImageView permissionAlertTypeImageView = dialog.findViewById(R.id.permissionAlertTypeImageView);        TextView permissionAlertContentTextView = dialog.findViewById(R.id.permissionAlertContentTextView);        TextView permissionAlertNotNowTextView = dialog.findViewById(R.id.permissionAlertNotNowTextView);        TextView permissionAlertSettingsTextView = dialog.findViewById(R.id.permissionAlertSettingsTextView);        permissionAlertContentTextView.setText(getSpecialPermissionContent(context, permissionType));        permissionAlertTypeImageView.setImageResource(getSpecialPermissionVector(permissionType));        permissionAlertNotNowTextView.setOnClickListener(v -> dialog.dismiss());        permissionAlertSettingsTextView.setOnClickListener(v -> {            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);            Uri uri = Uri.fromParts("package", context.getPackageName(), null);            intent.setData(uri);            context.startActivity(intent);            dialog.dismiss();        });        dialog.show();    }    /**     * @param context        - context     * @param permissionType - String data     * @return - boolean data     * Method used to return isPermissionDialog is shown for Android 11     */    private static boolean isValidPermissionDialog(Context context, String permissionType) {        boolean isValid = false;        CustomPreferencesManager customPreferencesManager = new CustomPreferencesManager(context);        String callType = customPreferencesManager.getPermissionDialogData();        callType = callType.isEmpty() ? permissionType : callType.concat(",").concat(permissionType);        customPreferencesManager.setPermissionDialogData(callType);        List<String> permissionTypeList = Arrays.asList(callType.split(","));        int count = Collections.frequency(permissionTypeList, permissionType);        if (count > 1) {            isValid = true;        }        return isValid;    }    /**     * @param context        - current activity     * @param permissionType - String data     * @return -     */    private static String getSpecialPermissionContent(Context context, String permissionType) {        String msg = "";        if (permissionType.equalsIgnoreCase(PERMISSION_CAMERA)) {            msg = context.getResources().getString(R.string.camera_video_permission);        } else if (permissionType.equalsIgnoreCase(PERMISSION_GALLERY)) {            msg = context.getResources().getString(R.string.storage_permission);        } else if (permissionType.equalsIgnoreCase(PERMISSION_MIC)) {            msg = context.getResources().getString(R.string.call_permission);        }        return msg;    }    /**     * @param permissionType - String data     * @return -     */    private static int getSpecialPermissionVector(String permissionType) {        int vector;        if (permissionType.equalsIgnoreCase(PERMISSION_CAMERA)) {            vector = R.drawable.ic_permission_camera;            return vector;        } else if (permissionType.equalsIgnoreCase(PERMISSION_GALLERY)) {            vector = R.drawable.ic_permission_storage;            return vector;        }        return 0;    }}