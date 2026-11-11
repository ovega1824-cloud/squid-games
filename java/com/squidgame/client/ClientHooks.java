package com.squidgame.client;

import net.minecraft.client.Minecraft;

public class ClientHooks {
    // resultado (bordes verdes/rojos + texto)
    public static long resultStart = -1L;
    public static boolean resultPass = true;
    public static String resultText = "";
    // contador de tiempo (-1 = oculto)
    public static int timerSeconds = -1;

    public static void showResult(boolean pass, String subtitle) {
        resultStart = System.currentTimeMillis();
        resultPass = pass;
        resultText = subtitle == null ? "" : subtitle;
    }

    public static void setTimer(int seconds) {
        timerSeconds = seconds;
    }

    public static void openCookie(int colorIndex) {
        Minecraft.getInstance().setScreen(new CookieScreen(colorIndex));
    }

    public static void openControl() {
        Minecraft.getInstance().setScreen(new ControlScreen());
    }

    public static void openMenu() {
        Minecraft.getInstance().setScreen(new HeadphonesScreen());
    }
}
