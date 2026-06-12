package com.upnvj.compstolife.entities;

public class Player {
    private String username;
    private int totalScore;

    public Player(String username) {
        this.username = username;
        this.totalScore = 0;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public void addScore(int points) {
        this.totalScore += points;
    }
}
