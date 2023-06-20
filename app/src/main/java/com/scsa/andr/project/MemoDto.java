package com.scsa.andr.project;

import java.io.Serializable;

public class MemoDto implements Serializable {
    private String title;
    private String contents;
    private String date;
    private boolean completed;

    public MemoDto(String title, String contents, String date, boolean completed) {
        this.title = title;
        this.contents = contents;
        this.date = date;
        this.completed = completed;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        return "MemoDto{" +
                "title='" + title + '\'' +
                ", contents='" + contents + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}