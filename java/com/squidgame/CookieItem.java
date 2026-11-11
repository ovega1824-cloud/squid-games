package com.squidgame;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;


import java.util.List;

/** Galleta: clic derecho abre la pantalla para recortar la figura. */
public class CookieItem extends Item {
    private final CookieColor color;

    public CookieItem(CookieColor color, Properties props) {
        super(props);
        this.color = color;
    }

    public CookieColor color() {
        return color;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            final int idx = color.ordinal();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.squidgame.client.ClientHooks.openCookie(idx));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tips, TooltipFlag flag) {
        tips.add(Component.literal("Figura: " + color.shape).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
