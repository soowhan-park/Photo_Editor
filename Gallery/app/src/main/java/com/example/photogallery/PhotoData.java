package com.example.photogallery;

/*
사진 데이터를 데이터베이스에 저장하기 위한 클래스.
 */

public class PhotoData {

    private int id;
    private String path;
    private String name;

    public PhotoData(int id, String path, String name) {
        this.id = id;
        this.path = path;
        this.name = name;
    }

    public PhotoData() {
    }

    @Override
    public String toString() {
        return "PhotoData{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
