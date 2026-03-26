package com.cityscape.geoszabaduloszobabackend.model.enums;

import lombok.Getter;

@Getter
public enum Difficulty {
    EASY, MEDIUM, HARD;

    public String getDisplayName() {
        return switch (this) {
            case EASY -> "Könnyű";
            case MEDIUM -> "Közepes";
            case HARD -> "Nehéz";
            default -> "Ismeretlen";
        };
    }
    public static Difficulty fromIndex(int index) {
        if (index >= 0 && index < values().length) {
            return values()[index];
        }
        return MEDIUM;
    }
}
