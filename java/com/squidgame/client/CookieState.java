package com.squidgame.client;

import com.squidgame.CookieColor;
import com.squidgame.SquidGameMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/** Progreso de cada galleta (se conserva mientras dura el juego, aunque cierres la pantalla). */
public class CookieState {
    public static final int SIZE = 96;
    public static final int START_HALVES = 16; // 8 corazones = 16 medios corazones
    public static final int LAYERS = 3;

    public static final int PLAYING = 0;
    public static final int PASSED = 1;
    public static final int FAILED = 2;

    /** 0 = fuera, 1 = borde (se raspa), 2 = resto de la galleta (si lo tocas, pierdes medio corazón). */
    public final byte[] kind = new byte[SIZE * SIZE];
    public final byte[] depth = new byte[SIZE * SIZE];
    public int borderCount = 0;
    public int halves = START_HALVES;
    public int layer = 1;
    public int status = PLAYING;
    public long failTime = 0L;
    public boolean reported = false;

    private static final Map<CookieColor, CookieState> STATES = new EnumMap<>(CookieColor.class);
    /** ¿El servidor tiene el juego de la galleta en marcha? */
    public static boolean gameActive = false;

    public static CookieState get(CookieColor c) {
        CookieState s = STATES.get(c);
        if (s == null) {
            s = new CookieState(c);
            STATES.put(c, s);
        }
        return s;
    }

    public static void reset() {
        STATES.clear();
    }

    public static void onSync(boolean active, boolean reset) {
        gameActive = active;
        if (reset) STATES.clear();
    }

    /** Si no hay juego en marcha, una galleta terminada se puede volver a jugar (práctica). */
    public static CookieState forOpen(CookieColor c) {
        CookieState s = get(c);
        if (!gameActive && s.status != PLAYING) {
            STATES.remove(c);
            s = get(c);
        }
        return s;
    }

    private CookieState(CookieColor c) {
        ResourceLocation loc = new ResourceLocation(SquidGameMod.MODID, "textures/gui/cookie/mask_" + c.id + ".png");
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(loc);
             NativeImage img = NativeImage.read(in)) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int v = img.getPixelRGBA(x, y);
                    int r = v & 0xFF, g = (v >> 8) & 0xFF;
                    byte k = (r > 128 && g > 128) ? (byte) 1 : (g > 128 ? (byte) 2 : (byte) 0);
                    kind[y * SIZE + x] = k;
                    if (k == 1) borderCount++;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la máscara de la galleta " + c.id, e);
        }
    }

    public int countAtLeast(int d) {
        int n = 0;
        for (int i = 0; i < depth.length; i++) {
            if (kind[i] == 1 && depth[i] >= d) n++;
        }
        return n;
    }

    /** Porcentaje total de la galleta (0-100). */
    public float percent() {
        if (status == PASSED) return 100.0F;
        if (borderCount == 0) return 0.0F;
        long sum = 0;
        for (int i = 0; i < depth.length; i++) {
            if (kind[i] == 1) sum += depth[i];
        }
        return Math.min(100.0F, 100.0F * sum / (float) (borderCount * LAYERS));
    }
}
