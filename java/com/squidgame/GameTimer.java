package com.squidgame;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Manda a todos los jugadores el tiempo que queda del juego en marcha (contador arriba de la pantalla). */
@Mod.EventBusSubscriber(modid = SquidGameMod.MODID)
public class GameTimer {
    private static int last = Integer.MIN_VALUE;
    private static int tickCount = 0;

    static int currentSeconds(MinecraftServer server) {
        if (!SGData.get(server).showTimer) return -1;
        int t = GameManager.remainingTicks();
        if (t < 0) t = BridgeGame.remainingTicks();
        if (t < 0) t = CookieGame.remainingTicks();
        return t < 0 ? -1 : (t + 19) / 20;
    }

    public static void refresh(MinecraftServer server) {
        last = Integer.MIN_VALUE;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        tickCount++;
        int secs = currentSeconds(server);
        if (secs != last || tickCount % 100 == 0) {
            last = secs;
            Net.CHANNEL.send(PacketDistributor.ALL.noArg(), new GamePackets.Timer(secs));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                    new GamePackets.Timer(currentSeconds(p.server)));
        }
    }
}
