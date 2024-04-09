package com.bluetooth.teacher.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;

import com.bluetooth.teacher.utils.FileHelper;
import com.bluetooth.teacher.models.Student;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
/*
 * Этот класс содержит утилиты для работы с SharedPreferences и сохранения/загрузки данных студентов.
 * Записывает в память в формате JSON, по запросу десериализует в GSON объекты по моделям.
 */
public class SharedPreferencesHelper {

    private static final String PREFS_NAME = "StudentsFile";
    private static final String JSON_KEY = "students_json";

    /*
     * Добавляет данные о студенте в SharedPreferences.
     */
    public static void appendStudent(Context context, String deviceId, String name) {
        List<Student> studentList = getStudentList(context);

        if (!containsDeviceId(studentList, deviceId)) {
            studentList.add(new Student(deviceId, name));
            saveStudentList(context, studentList);
        }
    }

    /*
     * Возвращает список студентов из SharedPreferences.
     */
    public static List<Student> getStudentList(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(JSON_KEY, "[]");

        Type listType = new TypeToken<ArrayList<Student>>(){}.getType();
        return new Gson().fromJson(json, listType);
    }

    /*
     * Сохраняет список студентов в SharedPreferences.
     */
    public static void saveStudentList(Context context, List<Student> studentList) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = new Gson().toJson(studentList);
        editor.putString(JSON_KEY, json);
        editor.apply();
    }

    /*
     * Проверяет наличие deviceId в списке студентов.
     */
    public static boolean containsDeviceId(List<Student> studentList, String deviceId) {
        for (Student student : studentList) {
            if (student.getDeviceId().equals(deviceId)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Чистит все SharedPreferences.
     */
    public static void clearPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}
