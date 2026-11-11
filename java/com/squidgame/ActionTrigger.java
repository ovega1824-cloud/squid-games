package com.squidgame;

/** Eventos que pueden disparar un comando (acciones condicionales del panel). */
public enum ActionTrigger {
    JUMP("Saltar", true),
    SPRINT("Correr", true),
    SNEAK("Agacharse", true),
    HURT("Recibir daño", true),
    PASS("Pasar un juego", false),
    LOSE("Perder un juego", false),
    DEATH("Ser eliminado", false);

    public final String label;
    /** true = movimiento/daño: por defecto solo cuenta durante un juego. */
    public final boolean ingameDefault;

    ActionTrigger(String label, boolean ingameDefault) {
        this.label = label;
        this.ingameDefault = ingameDefault;
    }

    public static ActionTrigger byName(String name) {
        for (ActionTrigger t : values()) if (t.name().equalsIgnoreCase(name)) return t;
        return JUMP;
    }

    public ActionTrigger next() {
        ActionTrigger[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
