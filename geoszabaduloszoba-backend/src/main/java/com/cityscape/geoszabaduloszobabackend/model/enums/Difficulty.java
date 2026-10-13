package com.cityscape.geoszabaduloszobabackend.model.enums;

import lombok.Getter;

@Getter
public enum Difficulty {
    EASY, MEDIUM, HARD;

    public static Difficulty fromIndex(int index) {
        if (index >= 0 && index < values().length) {
            return values()[index];
        }
        return MEDIUM;
    }
}
