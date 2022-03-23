/*
 *
 *  * Created by https://github.com/braver-tool on 12/10/21, 08:30 PM
 *  * Copyright (c) 2021 . All rights reserved.
 *  * Last modified 23/03/22, 09:45 AM
 *
 */

package com.braver.tool.filepicker;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import static com.braver.tool.filepicker.PermissionUtils.IS_PERMISSION_DIALOG_ANDROID_11;

public class CustomPreferencesManager {
    private static final String NOTIFICATION_PREFERENCES_NAME = "braver_preferences";
    public final SharedPreferences notificationPrefManager;

    /**
     * Method used to initiate Notification User Defaults
     *
     * @param context - Current Activity
     */
    public CustomPreferencesManager(Context context) {
        Context storageContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final Context deviceContext = context.createDeviceProtectedStorageContext();
            if (!deviceContext.moveSharedPreferencesFrom(context, NOTIFICATION_PREFERENCES_NAME)) {
                AppUtils.printLogConsole("CustomPreferencesManager", "Failed to migrate shared preferences.");
            }
            storageContext = deviceContext;
        } else {
            storageContext = context;
        }
        notificationPrefManager = storageContext.getSharedPreferences(NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Method used to get UserDefault
     */
    public String getPermissionDialogData() {
        String data = "";
        if (notificationPrefManager != null) {
            data = notificationPrefManager.getString(IS_PERMISSION_DIALOG_ANDROID_11, "");
        }
        return data;
    }

    /**
     * Method used to set UserDefault
     *
     * @param permissionType - String data
     */
    public void setPermissionDialogData(String permissionType) {
        if (notificationPrefManager != null) {
            SharedPreferences.Editor editor = notificationPrefManager.edit();
            editor.putString(IS_PERMISSION_DIALOG_ANDROID_11, permissionType);
            editor.apply();
        }
    }
}
