package com.squidgame;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Juego de la galleta (dalgona): /sg galleta start|stop|reset.
 * Quien pierde (se queda sin corazones) NO muere hasta que se acaba el tiempo.
 * Al acabar el tiempo mueren todos los que no pasaron.
 */
@Mod.EventBusSubscriber(modid = SquidGameMod.MODID)
public class CookieGame {
    private static boolean running = false;
    private static int timeLeft;
    private static final Set<UUID> players = new HashSet<>();
    private static final Set<UUID> winners = new HashSet<>();
    private static final Set<UUID> failed = new HashSet<>();
    private static final Map<UUID, GameType> startModes = new HashMap<>();

    public static boolean isRunning() {
        return running;
    }

    public static int remainingTicks() {
        return running ? Math.max(0, timeLeft) : -1;
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sg")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("galleta")
                        .then(Commands.literal("start")
                                .executes(ctx -> start(ctx.getSource(), 300))
                                .then(Commands.argument("tiempo", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                new String[]{"60s", "90s", "2m", "5m", "10m"}, b))
                                        .executes(ctx -> {
                                            int secs = GameManager.parseDuration(StringArgumentType.getString(ctx, "tiempo"));
                                            if (secs <= 0) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "Tiempo no válido. Ejemplos: 90, 90s, 2m, 1m30s, 1h"));
                                                return 0;
                                            }
                                            return start(ctx.getSource(), secs);
                                        })))
                        .then(Commands.literal("stop").executes(ctx -> stop(ctx.getSource())))
                        .then(Commands.literal("reset").executes(ctx -> {
                            Net.CHANNEL.send(PacketDistributor.ALL.noArg(), new GamePackets.CookieSync(running, true));
                            ctx.getSource().sendSuccess(() -> Component.literal("Galletas reiniciadas."), true);
                            return 1;
                        }))));
    }

    private static int start(CommandSourceStack src, int seconds) {
        if (running || GameManager.isRunning() || BridgeGame.isRunning() || FinalGame.isActive()) {
            src.sendFailure(Component.literal("Ya hay un juego en curso. Detenlo primero."));
            return 0;
        }
        MinecraftServer server = src.getServer();
        players.clear();
        winners.clear();
        failed.clear();
        startModes.clear();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            GameType gm = p.gameMode.getGameModeForPlayer();
            if (gm == GameType.SURVIVAL || gm == GameType.ADVENTURE) {
                players.add(p.getUUID());
                startModes.put(p.getUUID(), gm);
            }
        }
        if (players.isEmpty()) {
            src.sendFailure(Component.literal("No hay jugadores en supervivencia/aventura para jugar."));
            return 0;
        }
        timeLeft = seconds * 20;
        running = true;
        Net.CHANNEL.send(PacketDistributor.ALL.noArg(), new GamePackets.CookieSync(true, true));
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("¡Juego de la galleta! " + players.size() + " jugadores, tiempo: "
                        + GameManager.format(seconds)).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int stop(CommandSourceStack src) {
        if (!running) {
            src.sendFailure(Component.literal("El juego de la galleta no está en marcha."));
            return 0;
        }
        finish(src.getServer(), false);
        src.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("Juego detenido.").withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    /** Un jugador terminó su galleta (el cliente avisa). */
    public static void onDone(ServerPlayer p, int colorIndex, boolean passed) {
        UUID id = p.getUUID();
        if (running && players.contains(id)) {
            if (winners.contains(id) || failed.contains(id)) return; // ya tenía resultado
            if (passed) winners.add(id);
            else failed.add(id);
        }
        if (passed) Results.pass(p);
        else Results.fail(p, true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !running) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        timeLeft--;
        if (winners.size() >= players.size()) {
            finish(server, false);
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("¡Juego terminado! Todos pasaron.").withStyle(ChatFormatting.GOLD), false);
        } else if (timeLeft <= 0) {
            finish(server, true);
        }
    }

    private static void finish(MinecraftServer server, boolean timedOut) {
        running = false;
        int passed = winners.size();
        if (timedOut) {
            boolean any = false;
            for (UUID id : new ArrayList<>(players)) {
                if (winners.contains(id)) continue;
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p == null) continue;
                GameType gm = p.gameMode.getGameModeForPlayer();
                if (gm != GameType.SURVIVAL && gm != GameType.ADVENTURE) continue;
                GameManager.eliminateExternal(p, startModes.getOrDefault(id, GameType.SURVIVAL));
                any = true;
            }
            if (any) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.playNotifySound(ModRegistry.GUNSHOT.get(), SoundSource.MASTER, 1.0F, 1.0F);
                }
            }
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("¡Se acabó el tiempo! Jugadores que pasaron: " + passed)
                            .withStyle(ChatFormatting.GOLD), false);
        }
        players.clear();
        winners.clear();
        failed.clear();
        Net.CHANNEL.send(PacketDistributor.ALL.noArg(), new GamePackets.CookieSync(false, false));
    }
}
