package com.squidgame;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

/** Lo que pasa cuando un jugador pasa o pierde cualquier juego. */
public class Results {

    /** Sonido de pasar + bordes verdes + "Jugador #001 Nombre pasa" en el chat + acciones. */
    public static void pass(ServerPlayer p) {
        MinecraftServer server = p.server;
        SGData d = SGData.get(server);
        p.playNotifySound(ModRegistry.PASS.get(), SoundSource.MASTER, 1.0F, 1.0F);
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new GamePackets.Overlay(true, d.passText));
        Integer n = d.numbers.get(p.getUUID());
        String text = "Jugador " + (n != null ? "#" + NameCommands.format(n) + " " : "")
                + p.getGameProfile().getName() + " pasa";
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(text).withStyle(ChatFormatting.GREEN), false);
        Actions.fire(p, ActionTrigger.PASS);
    }

    /** Bordes rojos (+ sonido de perder si playSound) + acciones. */
    public static void fail(ServerPlayer p, boolean playSound) {
        SGData d = SGData.get(p.server);
        if (playSound) p.playNotifySound(ModRegistry.FAIL.get(), SoundSource.MASTER, 1.0F, 1.0F);
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new GamePackets.Overlay(false, d.failText));
        Actions.fire(p, ActionTrigger.LOSE);
    }
}
