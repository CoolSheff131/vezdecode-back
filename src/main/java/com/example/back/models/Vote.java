package com.example.back.models;

public class Vote {
    String phone;

    @Override
    public String toString() {
        return "Vote{" +
                "phone='" + phone + '\'' +
                ", artist='" + artist + '\'' +
                '}';
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    String artist;
}
