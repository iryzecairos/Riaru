
package com.bluetooth.teacher.utils;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import static com.bluetooth.teacher.utils.SharedPreferencesHelper.getStudentList;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.bluetooth.teacher.models.Student;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * Сохраняет данные в текстовый файл.
 */
public class FileHelper {
    public static void saveToFile(Context context, String fileName, ArrayList<String> stringList,
                                  String lessonName) throws FileNotFoundException {
        OutputStream outputStream;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();

            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + ".txt");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Посещаемость");

            Uri extVolumeUri = MediaStore.Files.getContentUri("external");
            Uri fileUri = context.getContentResolver().insert(extVolumeUri, values);

            outputStream = context.getContentResolver().openOutputStream(fileUri);
            Toast.makeText(context, "Сохранено в " + fileUri, Toast.LENGTH_LONG).show();

        } else {
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + "Посещаемость";
            File file = new File(path, fileName + ".txt");
            Toast.makeText(context, "Сохранено в " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

            outputStream = new FileOutputStream(file);
        }

        byte[] bytes = convertToString(stringList, lessonName).getBytes();
        try {
            outputStream.write(bytes);
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @SuppressLint("NewApi")
    public static void saveNamesToFile(Context context, String lessonName) {
        List<Student> studentList = getStudentList(context);
        ArrayList<String> nameList = new ArrayList<>();

        for (Student student : studentList) {
            nameList.add(student.getName());
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());

        String fileName = "Посещаемость_"+lessonName+"_"+timeStamp;

        try {
            FileHelper.saveToFile(context, fileName, nameList, lessonName);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String convertToString(ArrayList<String> stringList, String lessonName) {
        StringBuilder result = new StringBuilder();
        result.append(lessonName).append("\n");
        result.append(getCurrentDate()).append("\n");

        result.append("КОЛИЧЕСТВО ЧЕЛОВЕК: ").append(stringList.size()).append("\n");
        for (int i = 0; i < stringList.size(); i++) {
            result.append(i + 1).append(". ").append(stringList.get(i)).append("\n");
        }
        return result.toString();
    }
    @SuppressLint("NewApi")
    private static String getCurrentDate() {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

}




