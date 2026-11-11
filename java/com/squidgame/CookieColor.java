package com.squidgame;

/** Los cuatro colores de galleta y su figura. */
public enum CookieColor {
    BLUE("blue", "Azul", "paraguas"),
    GREEN("green", "Verde", "círculo"),
    YELLOW("yellow", "Amarilla", "estrella"),
    RED("red", "Roja", "triángulo");

    public final String id;
    public final String label;
    public final String shape;

    CookieColor(String id, String label, String shape) {
        this.id = id;
        this.label = label;
        this.shape = shape;
    }

    public static CookieColor byIndex(int i) {
        CookieColor[] v = values();
        return v[Math.floorMod(i, v.length)];
    }
}
