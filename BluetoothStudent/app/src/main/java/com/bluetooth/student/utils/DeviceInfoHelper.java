package com.bluetooth.student.utils;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class DeviceInfoHelper {
    public static String getDeviceUniqueId(Context context) {
        StringBuilder uniqueId = new StringBuilder();

        uniqueId.append("AndroidID:")
                .append(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID))
                .append("_");

        uniqueId.append("SerialNumber:")
                .append(Build.SERIAL)
                .append("_");

        uniqueId.append("Model:")
                .append(Build.MODEL)
                .append("_");

        uniqueId.append("Manufacturer:")
                .append(Build.MANUFACTURER)
                .append("_");

        return uniqueId.toString();
    }

}
