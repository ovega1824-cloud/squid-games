package com.squidgame;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/** Bandeja con galleta: clic derecho te da la galleta de su color. */
public class CookieBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);
    private final CookieColor color;
    private final Supplier<Item> item;

    public CookieBlock(CookieColor color, Supplier<Item> item, Properties props) {
        super(props);
        this.color = color;
        this.item = item;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Item it = item.get();
        boolean has = false;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(it)) {
                has = true;
                break;
            }
        }
        if (!has && player.getOffhandItem().is(it)) has = true;
        if (has) {
            player.displayClientMessage(Component.literal("Ya tienes la galleta " + color.label.toLowerCase()
                    + " (" + color.shape + ")."), true);
        } else {
            ItemStack stack = new ItemStack(it);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        return InteractionResult.CONSUME;
    }
}
