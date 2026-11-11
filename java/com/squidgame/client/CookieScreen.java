package com.squidgame.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.squidgame.CookieColor;
import com.squidgame.GamePackets;
import com.squidgame.Net;
import com.squidgame.SquidGameMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.io.InputStream;
import java.util.Locale;

/**
 * Pantalla de la galleta: mantén el clic izquierdo y recorta el contorno de la figura (3 capas).
 * Si rozas algo que no sea el borde pierdes medio corazón. Con 0 corazones la galleta se parte.
 * Al terminar (pasar o perder) el clic derecho cierra la pantalla.
 */
public class CookieScreen extends Screen {
    private static final int SIZE = CookieState.SIZE;
    private static final float CARVE_RADIUS = 1.5F;
    private static final long PENALTY_COOLDOWN_MS = 450L;
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");

    private final CookieColor color;
    private final CookieState st;
    private final int[] base = new int[SIZE * SIZE]; // textura original (ABGR)
    private final ResourceLocation baseLoc;
    private final ResourceLocation cracks1, cracks2, cracks3;
    private DynamicTexture tex;
    private ResourceLocation texLoc;
    private boolean dirty = true;

    private float x0, y0, scale; // posición y escala de la galleta en pantalla
    private boolean down = false;
    private double lastX, lastY;
    private boolean haveLast = false;
    private long lastPenalty = 0L;
    private long lastScrape = 0L;
    private long flash = 0L;

    public CookieScreen(int colorIndex) {
        super(Component.literal("Galleta"));
        this.color = CookieColor.byIndex(colorIndex);
        this.st = CookieState.forOpen(color);
        this.baseLoc = new ResourceLocation(SquidGameMod.MODID, "textures/gui/cookie/cookie_" + color.id + ".png");
        this.cracks1 = new ResourceLocation(SquidGameMod.MODID, "textures/gui/cookie/crack_1.png");
        this.cracks2 = new ResourceLocation(SquidGameMod.MODID, "textures/gui/cookie/crack_2.png");
        this.cracks3 = new ResourceLocation(SquidGameMod.MODID, "textures/gui/cookie/crack_3.png");
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(baseLoc);
             NativeImage img = NativeImage.read(in)) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) base[y * SIZE + x] = img.getPixelRGBA(x, y);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la textura de la galleta", e);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        if (tex == null) {
            tex = new DynamicTexture(new NativeImage(SIZE, SIZE, false));
            texLoc = Minecraft.getInstance().getTextureManager()
                    .register("squidgame_cookie_" + color.id, tex);
            dirty = true;
        }
    }

    @Override
    public void removed() {
        if (texLoc != null) {
            Minecraft.getInstance().getTextureManager().release(texLoc);
            texLoc = null;
            tex = null;
        }
    }

    // ------------------------------------------------------------------ textura

    private static int abgr(int r, int g, int b, int a) {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private void repaint() {
        NativeImage img = tex.getPixels();
        if (img == null) return;
        for (int i = 0; i < base.length; i++) {
            int px = base[i];
            int d = st.depth[i];
            if (st.kind[i] == 1 && d > 0) {
                if (d == 1) px = abgr(214, 160, 92, 255);
                else if (d == 2) px = abgr(244, 214, 150, 255);
                else px = 0; // agujero: ya está recortado del todo
            }
            img.setPixelRGBA(i % SIZE, i / SIZE, px);
        }
        tex.upload();
        dirty = false;
    }

    // ------------------------------------------------------------------ entrada

    private void layout() {
        float top = 64.0F;
        float bottom = this.height - 40.0F;
        float size = Math.max(64.0F, Math.min(bottom - top, this.width - 20.0F));
        scale = size / SIZE;
        x0 = (this.width - size) / 2.0F;
        y0 = top + ((bottom - top) - size) / 2.0F;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (st.status != CookieState.PLAYING) {
            if (button == 1) this.onClose(); // terminó: clic derecho quita la interfaz
            return true;
        }
        if (button == 0) {
            layout();
            down = true;
            haveLast = false;
            process(mx, my);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && down && st.status == CookieState.PLAYING) {
            layout();
            process(mx, my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) {
            down = false;
            haveLast = false;
        }
        return super.mouseReleased(mx, my, button);
    }

    /** Recorre el trazo desde la última posición hasta la actual sin saltarse píxeles. */
    private void process(double mx, double my) {
        double px = (mx - x0) / scale;
        double py = (my - y0) / scale;
        if (haveLast) {
            double dx = px - lastX, dy = py - lastY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            int steps = Math.max(1, (int) Math.ceil(dist / 0.5D));
            for (int i = 1; i <= steps && st.status == CookieState.PLAYING; i++) {
                sample(lastX + dx * i / steps, lastY + dy * i / steps);
            }
        } else {
            sample(px, py);
        }
        lastX = px;
        lastY = py;
        haveLast = true;
    }

    private void sample(double px, double py) {
        int ix = (int) Math.floor(px), iy = (int) Math.floor(py);
        if (ix < 0 || iy < 0 || ix >= SIZE || iy >= SIZE) return;
        int k = st.kind[iy * SIZE + ix];

        // ¿tocó algo que no es el borde? (con 1 píxel de tolerancia)
        if (k == 2 && !nearBorder(ix, iy)) {
            penalty();
            if (st.status != CookieState.PLAYING) return;
        }

        // raspar el borde en la capa actual
        boolean changed = false;
        int r = (int) Math.ceil(CARVE_RADIUS);
        for (int y = iy - r; y <= iy + r; y++) {
            for (int x = ix - r; x <= ix + r; x++) {
                if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) continue;
                double ddx = x + 0.5D - px, ddy = y + 0.5D - py;
                if (ddx * ddx + ddy * ddy > CARVE_RADIUS * CARVE_RADIUS) continue;
                int idx = y * SIZE + x;
                if (st.kind[idx] == 1 && st.depth[idx] == st.layer - 1) {
                    st.depth[idx] = (byte) st.layer;
                    changed = true;
                }
            }
        }
        if (changed) {
            dirty = true;
            long now = System.currentTimeMillis();
            if (now - lastScrape > 90L) {
                lastScrape = now;
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.SAND_HIT, 0.7F + (float) Math.random() * 0.3F, 0.6F));
            }
            checkLayer();
        }
    }

    private boolean nearBorder(int ix, int iy) {
        for (int y = iy - 1; y <= iy + 1; y++) {
            for (int x = ix - 1; x <= ix + 1; x++) {
                if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) continue;
                if (st.kind[y * SIZE + x] == 1) return true;
            }
        }
        return false;
    }

    private void penalty() {
        long now = System.currentTimeMillis();
        if (now - lastPenalty < PENALTY_COOLDOWN_MS) return;
        lastPenalty = now;
        flash = now;
        st.halves--;
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.GLASS_BREAK, 1.6F, 0.6F));
        if (st.halves <= 0) {
            st.halves = 0;
            st.status = CookieState.FAILED;
            st.failTime = now;
            down = false;
            report(false);
        }
    }

    private void checkLayer() {
        int need = (int) Math.ceil(st.borderCount * 0.96D);
        while (st.status == CookieState.PLAYING && st.countAtLeast(st.layer) >= need) {
            if (st.layer >= CookieState.LAYERS) {
                st.status = CookieState.PASSED;
                down = false;
                report(true);
                break;
            }
            st.layer++;
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F, 0.7F));
        }
    }

    private void report(boolean passed) {
        if (st.reported) return;
        st.reported = true;
        Net.CHANNEL.sendToServer(new GamePackets.CookieDone(color.ordinal(), passed));
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        layout();
        if (tex == null) return;
        if (dirty) repaint();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float size = scale * SIZE;
        // sombra detrás (se ve a través del agujero que va quedando)
        g.fill((int) x0, (int) y0, (int) (x0 + size), (int) (y0 + size), 0x40000000);

        // la galleta
        if (st.status == CookieState.FAILED) {
            drawBroken(g);
        } else {
            g.pose().pushPose();
            g.pose().translate(x0, y0, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.blit(texLoc, 0, 0, 0.0F, 0.0F, SIZE, SIZE, SIZE, SIZE);
            drawCracks(g);
            g.pose().popPose();
        }

        // si acabas de perder medio corazón, un destello rojo
        long since = System.currentTimeMillis() - flash;
        if (since < 220L && st.status == CookieState.PLAYING) {
            int a = (int) (90 * (1.0F - since / 220.0F));
            g.fill((int) x0, (int) y0, (int) (x0 + size), (int) (y0 + size), (a << 24) | 0xFF0000);
        }

        drawHearts(g);
        drawBar(g);

        // aguja en el cursor
        if (st.status == CookieState.PLAYING) {
            g.pose().pushPose();
            g.pose().translate(mouseX, mouseY, 0.0F);
            g.pose().mulPose(Axis.ZP.rotationDegrees(45.0F));
            g.fill(0, -26, 1, 0, 0xFFE8E8E8);
            g.fill(-1, -26, 2, -22, 0xFFAAAAAA);
            g.pose().popPose();
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawCracks(GuiGraphics g) {
        ResourceLocation c = null;
        if (st.halves <= 4) c = cracks3;
        else if (st.halves <= 8) c = cracks2;
        else if (st.halves <= 12) c = cracks1;
        if (c != null) g.blit(c, 0, 0, 0.0F, 0.0F, SIZE, SIZE, SIZE, SIZE);
    }

    private void drawBroken(GuiGraphics g) {
        float t = Math.min(1.0F, (System.currentTimeMillis() - st.failTime) / 700.0F);
        float e = t * t;
        float cx = x0 + scale * SIZE / 2.0F, cy = y0 + scale * SIZE / 2.0F;
        for (int side = 0; side < 2; side++) {
            float dir = side == 0 ? -1.0F : 1.0F;
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.pose().translate(dir * 14.0F * t, 34.0F * e, 0.0F);
            g.pose().mulPose(Axis.ZP.rotationDegrees(dir * 16.0F * t));
            int half = SIZE / 2;
            g.blit(texLoc, side == 0 ? -half : 0, -half, side == 0 ? 0.0F : (float) half, 0.0F,
                    half, SIZE, SIZE, SIZE);
            g.pose().popPose();
        }
    }

    private void drawHearts(GuiGraphics g) {
        int total = CookieState.START_HALVES / 2;
        int x = (this.width - total * 9) / 2;
        int y = 15;
        for (int i = 0; i < total; i++) {
            g.blit(ICONS, x + i * 9, y, 16, 0, 9, 9);
            if (st.halves >= i * 2 + 2) g.blit(ICONS, x + i * 9, y, 52, 0, 9, 9);
            else if (st.halves == i * 2 + 1) g.blit(ICONS, x + i * 9, y, 61, 0, 9, 9);
        }
    }

    private void drawBar(GuiGraphics g) {
        int bw = 170, bh = 14;
        int bx = (this.width - bw) / 2;
        int by = this.height - 28;
        float pct = st.percent();

        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF000000);
        g.fill(bx, by, bx + bw, by + bh, 0xFF3A2A1A);
        g.fill(bx, by, bx + (int) (bw * pct / 100.0F), by + bh, 0xFFD9A441);
        String text = String.format(Locale.ROOT, "%.2f %%", pct).replace('.', ',');
        g.drawCenteredString(this.font, text, bx + bw / 2, by + 3, 0xFFFFFF);

        // capa que llevas, a la izquierda de la barra
        String layer = "Capa " + Math.min(st.layer, CookieState.LAYERS) + "/" + CookieState.LAYERS;
        g.drawString(this.font, layer, bx - 6 - this.font.width(layer), by + 3, 0xFFFFFF, true);

        if (st.status != CookieState.PLAYING) {
            g.drawCenteredString(this.font, "Clic derecho para cerrar", this.width / 2, by - 12, 0xCCCCCC);
        }
    }
}
