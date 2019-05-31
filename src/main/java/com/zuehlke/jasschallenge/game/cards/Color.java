package com.zuehlke.jasschallenge.game.cards;

import java.io.Serializable;

public enum Color implements Serializable {
    HEARTS("(H)", 0),
    DIAMONDS("(D)", 1),
    CLUBS("(C)", 2),
    SPADES("(S)", 3);


    private final String sign;
    private final int value;

    Color(String sign, int value) {
        this.sign = sign;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return sign;
    }
}
