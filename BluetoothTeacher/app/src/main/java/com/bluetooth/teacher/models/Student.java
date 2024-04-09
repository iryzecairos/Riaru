package com.bluetooth.teacher.models;

/*
 * Этот класс представляет модель студента для удобства его записи в JSON через GSON.
 */
public class Student {
    private String deviceId;
    private String name;

    public Student(String deviceId, String name) {
        this.deviceId = deviceId;
        this.name = name;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getName() {
        return name;
    }
}
