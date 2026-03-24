package com.cityscape.geoszabaduloszobabackend.model.enums;

public enum Difficulty {
    EASY, MEDIUM, HARD, UNKNOWN;

    public String getDisplayName() {
        return switch (this) {
            case EASY -> "Könnyű";
            case MEDIUM -> "Közepes";
            case HARD -> "Nehéz";
            default -> "Ismeretlen";
        };
    }
}
