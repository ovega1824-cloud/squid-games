package com.squidgame;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class Net {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SquidGameMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void init() {
        CHANNEL.registerMessage(0, GamePackets.Overlay.class,
                GamePackets.Overlay::encode, GamePackets.Overlay::decode, GamePackets.Overlay::handle);
        CHANNEL.registerMessage(1, SoundPackets.BeginPacket.class,
                SoundPackets.BeginPacket::encode, SoundPackets.BeginPacket::decode, SoundPackets.BeginPacket::handle);
        CHANNEL.registerMessage(2, SoundPackets.ChunkPacket.class,
                SoundPackets.ChunkPacket::encode, SoundPackets.ChunkPacket::decode, SoundPackets.ChunkPacket::handle);
        CHANNEL.registerMessage(3, SoundPackets.StopPacket.class,
                SoundPackets.StopPacket::encode, SoundPackets.StopPacket::decode, SoundPackets.StopPacket::handle);
        CHANNEL.registerMessage(4, FinalPackets.State.class,
                FinalPackets.State::encode, FinalPackets.State::decode, FinalPackets.State::handle);
        CHANNEL.registerMessage(5, ControlPackets.Sequence.class,
                ControlPackets.Sequence::encode, ControlPackets.Sequence::decode, ControlPackets.Sequence::handle);
        CHANNEL.registerMessage(6, ControlPackets.NameData.class,
                ControlPackets.NameData::encode, ControlPackets.NameData::decode, ControlPackets.NameData::handle);
        CHANNEL.registerMessage(7, GamePackets.Timer.class,
                GamePackets.Timer::encode, GamePackets.Timer::decode, GamePackets.Timer::handle);
        CHANNEL.registerMessage(8, GamePackets.CookieSync.class,
                GamePackets.CookieSync::encode, GamePackets.CookieSync::decode, GamePackets.CookieSync::handle);
        CHANNEL.registerMessage(9, GamePackets.CookieDone.class,
                GamePackets.CookieDone::encode, GamePackets.CookieDone::decode, GamePackets.CookieDone::handle);
        CHANNEL.registerMessage(10, GamePackets.Settings.class,
                GamePackets.Settings::encode, GamePackets.Settings::decode, GamePackets.Settings::handle);
    }
}
