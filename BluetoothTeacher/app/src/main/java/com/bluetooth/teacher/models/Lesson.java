package com.bluetooth.teacher.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Lesson {
    private Date date;
    private String lesson_name;
    private List<Student> students;
    public Lesson(String lesson_name) {
        this.date = new Date();
        this.lesson_name = lesson_name;
        this.students = new ArrayList<>();
    }
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getLesson_name() {
        return lesson_name;
    }

    public void setLesson_name(String lesson_name) {
        this.lesson_name = lesson_name;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }



}
