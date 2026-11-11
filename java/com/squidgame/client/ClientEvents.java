package com.squidgame.client;

import com.squidgame.SquidGameMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SquidGameMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    private static final long HOLD_MS = 3000L;
    private static final long TOTAL_MS = 4200L;

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        SoundLibrary.ensureFolder(); // crea .minecraft/sound
        ClientConfig.load();
        ControlConfig.load();
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("result_border", (gui, graphics, partialTick, width, height) ->
                renderResult(gui.getFont(), graphics, width, height));
        event.registerAboveAll("game_timer", (gui, graphics, partialTick, width, height) -> {
            int s = ClientHooks.timerSeconds;
            if (s < 0) return;
            String text = s >= 3600
                    ? String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
                    : String.format("%02d:%02d", s / 60, s % 60);
            graphics.drawCenteredString(gui.getFont(), text, width / 2, 4, 0xFFFFFF);
        });
        event.registerAboveAll("final_hud", (gui, graphics, partialTick, width, height) ->
                FinalHud.render(graphics, width, height));
    }

    /** Bordes de la pantalla en verde (pasaste) o rojo (perdiste) + título y texto editable. */
    private static void renderResult(net.minecraft.client.gui.Font font, GuiGraphics g, int w, int h) {
        long start = ClientHooks.resultStart;
        if (start < 0) return;
        long t = System.currentTimeMillis() - start;
        if (t >= TOTAL_MS) {
            ClientHooks.resultStart = -1L;
            return;
        }
        float fade = t > HOLD_MS ? 1.0F - (float) (t - HOLD_MS) / (float) (TOTAL_MS - HOLD_MS) : 1.0F;
        boolean pass = ClientHooks.resultPass;
        int rgb = pass ? 0x1EC83C : 0xDC1E1E;

        // bordes: franjas que se van desvaneciendo hacia el centro
        int edge = Math.max(24, Math.min(w, h) / 4);
        int steps = 24;
        int step = Math.max(1, edge / steps);
        for (int i = 0; i < steps; i++) {
            float k = 1.0F - (float) i / steps;
            int a = Math.max(0, Math.min(255, (int) (190.0F * k * k * fade)));
            int col = (a << 24) | rgb;
            int o = i * step;
            g.fill(0, o, w, o + step, col);          // arriba
            g.fill(0, h - o - step, w, h - o, col);  // abajo
            g.fill(o, 0, o + step, h, col);          // izquierda
            g.fill(w - o - step, 0, w - o, h, col);  // derecha
        }

        int ta = Math.max(4, Math.min(255, (int) (255.0F * fade)));
        String title = pass ? "¡Felicidades!" : "¡Perdiste!";
        int titleColor = (ta << 24) | (pass ? 0x55FF55 : 0xFF5555);
        int subColor = (ta << 24) | (pass ? 0xB8FFB8 : 0xFFB0B0);
        g.pose().pushPose();
        g.pose().translate(w / 2.0F, 28.0F, 0.0F);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        g.drawCenteredString(font, title, 0, 0, titleColor);
        g.pose().popPose();
        String sub = ClientHooks.resultText;
        if (!sub.isEmpty()) g.drawCenteredString(font, sub, w / 2, 48, subColor);
    }
}
