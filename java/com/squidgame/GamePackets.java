package com.squidgame;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Paquetes del resultado (felicidades/perdiste), contador, galleta y ajustes del panel. */
public class GamePackets {

    /** Servidor -> jugador: bordes verdes (pasó) o rojos (perdió) con un texto. */
    public static class Overlay {
        public final boolean pass;
        public final String subtitle;

        public Overlay(boolean pass, String subtitle) {
            this.pass = pass;
            this.subtitle = subtitle;
        }

        public static void encode(Overlay m, FriendlyByteBuf b) {
            b.writeBoolean(m.pass);
            b.writeUtf(m.subtitle, 300);
        }

        public static Overlay decode(FriendlyByteBuf b) {
            return new Overlay(b.readBoolean(), b.readUtf(300));
        }

        public static void handle(Overlay m, Supplier<NetworkEvent.Context> ctxs) {
            NetworkEvent.Context ctx = ctxs.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.squidgame.client.ClientHooks.showResult(m.pass, m.subtitle)));
            ctx.setPacketHandled(true);
        }
    }

    /** Servidor -> clientes: segundos que quedan del juego (-1 = ocultar contador). */
    public static class Timer {
        public final int seconds;

        public Timer(int seconds) {
            this.seconds = seconds;
        }

        public static void encode(Timer m, FriendlyByteBuf b) {
            b.writeInt(m.seconds);
        }

        public static Timer decode(FriendlyByteBuf b) {
            return new Timer(b.readInt());
        }

        public static void handle(Timer m, Supplier<NetworkEvent.Context> ctxs) {
            NetworkEvent.Context ctx = ctxs.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.squidgame.client.ClientHooks.setTimer(m.seconds)));
            ctx.setPacketHandled(true);
        }
    }

    /** Servidor -> clientes: el juego de la galleta empieza/termina y/o hay que reiniciar las galletas. */
    public static class CookieSync {
        public final boolean active;
        public final boolean reset;

        public CookieSync(boolean active, boolean reset) {
            this.active = active;
            this.reset = reset;
        }

        public static void encode(CookieSync m, FriendlyByteBuf b) {
            b.writeBoolean(m.active);
            b.writeBoolean(m.reset);
        }

        public static CookieSync decode(FriendlyByteBuf b) {
            return new CookieSync(b.readBoolean(), b.readBoolean());
        }

        public static void handle(CookieSync m, Supplier<NetworkEvent.Context> ctxs) {
            NetworkEvent.Context ctx = ctxs.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.squidgame.client.CookieState.onSync(m.active, m.reset)));
            ctx.setPacketHandled(true);
        }
    }

    /** Cliente -> servidor: terminó una galleta (pasó o perdió). */
    public static class CookieDone {
        public final int color;
        public final boolean passed;

        public CookieDone(int color, boolean passed) {
            this.color = color;
            this.passed = passed;
        }

        public static void encode(CookieDone m, FriendlyByteBuf b) {
            b.writeVarInt(m.color);
            b.writeBoolean(m.passed);
        }

        public static CookieDone decode(FriendlyByteBuf b) {
            return new CookieDone(b.readVarInt(), b.readBoolean());
        }

        public static void handle(CookieDone m, Supplier<NetworkEvent.Context> ctxs) {
            NetworkEvent.Context ctx = ctxs.get();
            ServerPlayer sender = ctx.getSender();
            if (sender != null) ctx.enqueueWork(() -> CookieGame.onDone(sender, m.color, m.passed));
            ctx.setPacketHandled(true);
        }
    }

    /** Una acción condicional: cuando pasa [trigger], el servidor ejecuta [command]. */
    public record RuleData(String trigger, String command, int cooldownTicks, boolean onlyInGame) {
    }

    /** Cliente -> servidor (solo operadores): textos, contador y acciones condicionales. */
    public static class Settings {
        public final String passText;
        public final String failText;
        public final boolean showTimer;
        public final List<RuleData> rules;

        public Settings(String passText, String failText, boolean showTimer, List<RuleData> rules) {
            this.passText = passText;
            this.failText = failText;
            this.showTimer = showTimer;
            this.rules = rules;
        }

        public static void encode(Settings m, FriendlyByteBuf b) {
            b.writeUtf(m.passText, 200);
            b.writeUtf(m.failText, 200);
            b.writeBoolean(m.showTimer);
            b.writeVarInt(m.rules.size());
            for (RuleData r : m.rules) {
                b.writeUtf(r.trigger(), 32);
                b.writeUtf(r.command(), 300);
                b.writeVarInt(r.cooldownTicks());
                b.writeBoolean(r.onlyInGame());
            }
        }

        public static Settings decode(FriendlyByteBuf b) {
            String pass = b.readUtf(200);
            String fail = b.readUtf(200);
            boolean timer = b.readBoolean();
            int n = Math.min(b.readVarInt(), 50);
            List<RuleData> rules = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                rules.add(new RuleData(b.readUtf(32), b.readUtf(300),
                        Math.max(0, Math.min(b.readVarInt(), 72000)), b.readBoolean()));
            }
            return new Settings(pass, fail, timer, rules);
        }

        public static void handle(Settings m, Supplier<NetworkEvent.Context> ctxs) {
            NetworkEvent.Context ctx = ctxs.get();
            ServerPlayer sender = ctx.getSender();
            if (sender != null) ctx.enqueueWork(() -> Actions.onSettings(sender, m));
            ctx.setPacketHandled(true);
        }
    }
}
