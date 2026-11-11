package com.squidgame;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Acciones condicionales: "cuando pase X (saltar, correr, pasar un juego...) ejecuta este comando".
 * El comando se ejecuta como el jugador al que le pasó (así @s es ese jugador) con permisos de operador.
 */
@Mod.EventBusSubscriber(modid = SquidGameMod.MODID)
public class Actions {
    private static final Map<String, Long> lastFired = new HashMap<>();
    private static final Set<UUID> sprinting = new HashSet<>();
    private static final Set<UUID> sneaking = new HashSet<>();

    // ------------------------------------------------------------------ ajustes desde el panel

    public static void onSettings(ServerPlayer sender, GamePackets.Settings m) {
        if (!sender.hasPermissions(2)) {
            sender.sendSystemMessage(Component.literal("Necesitas ser operador para usar el panel de control."));
            return;
        }
        MinecraftServer server = sender.server;
        SGData d = SGData.get(server);
        d.passText = m.passText;
        d.failText = m.failText;
        d.showTimer = m.showTimer;
        d.rules.clear();
        for (GamePackets.RuleData r : m.rules) {
            String c = r.command().trim();
            if (c.startsWith("/")) c = c.substring(1);
            if (c.isEmpty()) continue;
            d.rules.add(new GamePackets.RuleData(ActionTrigger.byName(r.trigger()).name(), c,
                    r.cooldownTicks(), r.onlyInGame()));
        }
        d.setDirty();
        lastFired.clear();
        GameTimer.refresh(server);
        sender.sendSystemMessage(Component.literal("Ajustes guardados (" + d.rules.size() + " acciones)."));
    }

    // ------------------------------------------------------------------ comando /sg texto y /sg contador

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sg")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("texto")
                        .then(Commands.literal("pasar")
                                .then(Commands.argument("texto", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            SGData d = SGData.get(ctx.getSource().getServer());
                                            d.passText = StringArgumentType.getString(ctx, "texto");
                                            d.setDirty();
                                            ctx.getSource().sendSuccess(() -> Component.literal("Texto de pasar actualizado."), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("perder")
                                .then(Commands.argument("texto", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            SGData d = SGData.get(ctx.getSource().getServer());
                                            d.failText = StringArgumentType.getString(ctx, "texto");
                                            d.setDirty();
                                            ctx.getSource().sendSuccess(() -> Component.literal("Texto de perder actualizado."), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("contador")
                        .then(Commands.literal("on").executes(ctx -> setTimer(ctx.getSource().getServer(), true, ctx.getSource())))
                        .then(Commands.literal("off").executes(ctx -> setTimer(ctx.getSource().getServer(), false, ctx.getSource())))));
    }

    private static int setTimer(MinecraftServer server, boolean on, net.minecraft.commands.CommandSourceStack src) {
        SGData d = SGData.get(server);
        d.showTimer = on;
        d.setDirty();
        GameTimer.refresh(server);
        src.sendSuccess(() -> Component.literal("Contador " + (on ? "activado." : "desactivado.")), true);
        return 1;
    }

    // ------------------------------------------------------------------ disparar

    static boolean anyGameRunning() {
        return GameManager.isRunning() || BridgeGame.isRunning() || CookieGame.isRunning() || FinalGame.isActive();
    }

    public static void fire(ServerPlayer p, ActionTrigger t) {
        SGData d = SGData.get(p.server);
        if (d.rules.isEmpty()) return;
        long now = p.server.overworld().getGameTime();
        List<GamePackets.RuleData> rules = d.rules;
        for (int i = 0; i < rules.size(); i++) {
            GamePackets.RuleData r = rules.get(i);
            if (!r.trigger().equalsIgnoreCase(t.name())) continue;
            if (r.onlyInGame() && !anyGameRunning()) continue;
            String key = i + ":" + p.getUUID();
            Long last = lastFired.get(key);
            if (last != null && now - last < r.cooldownTicks()) continue;
            lastFired.put(key, now);
            String cmd = r.command().replace("%player%", p.getGameProfile().getName());
            try {
                p.server.getCommands().performPrefixedCommand(
                        p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), cmd);
            } catch (Exception e) {
                p.sendSystemMessage(Component.literal("Error en la acción: " + cmd));
            }
        }
    }

    // ------------------------------------------------------------------ eventos

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) fire(p, ActionTrigger.JUMP);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) fire(p, ActionTrigger.HURT);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) fire(p, ActionTrigger.DEATH);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        UUID id = p.getUUID();
        if (p.isSprinting()) {
            if (sprinting.add(id)) fire(p, ActionTrigger.SPRINT);
        } else {
            sprinting.remove(id);
        }
        if (p.isShiftKeyDown()) {
            if (sneaking.add(id)) fire(p, ActionTrigger.SNEAK);
        } else {
            sneaking.remove(id);
        }
    }
}
