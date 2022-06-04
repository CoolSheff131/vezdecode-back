package com.example.back.models;

public class Artist {


    private String name;

    @Override
    public String toString() {
        return "Artist{" +
                "name='" + name + '\'' +
                ", votes=" + votes +
                '}';
    }

    public Artist(String name, int votes) {
        this.name = name;
        this.votes = votes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    private int votes;


}
