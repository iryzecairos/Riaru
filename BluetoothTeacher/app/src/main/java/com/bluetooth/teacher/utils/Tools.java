package com.bluetooth.teacher.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;
/*
 * Этот класс содержит утилитный метод для проверки наличия разрешений в приложении.
 * Метод hasPermissions проверяет, имеет ли приложение указанные разрешения.
 */
public class Tools {
    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {

                return false;
            }
        }
        return true;
    }
}
