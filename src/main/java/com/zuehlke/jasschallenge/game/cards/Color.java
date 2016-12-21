package com.zuehlke.jasschallenge.game.cards;

public enum Color {
    HEARTS("(H)"),
    DIAMONDS("(D)"),
    CLUBS("(C)"),
    SPADES("(S)");


    private final String sign;

    Color(String sign) {
        this.sign = sign;
    }

    @Override
    public String toString() {
        return sign;
    }
}
