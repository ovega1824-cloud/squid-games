package com.squidgame;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SquidGameMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SquidGameMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SquidGameMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SquidGameMod.MODID);

    public static final RegistryObject<Block> BLOCK_ONE = BLOCKS.register("block_one",
            () -> new LineBlock(1, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.5F, 6.0F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BLOCK_TWO = BLOCKS.register("block_two",
            () -> new LineBlock(2, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN).strength(1.5F, 6.0F).sound(SoundType.STONE)));

    public static final RegistryObject<Block> FINAL_BLOCK = BLOCKS.register("final_block",
            () -> new FinalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(1.5F, 6.0F).sound(SoundType.STONE)));

    public static final RegistryObject<Block> BRIDGE_GLASS = BLOCKS.register("bridge_glass",
            () -> new BridgeGlassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.GLASS).noOcclusion().noLootTable()
                    .isValidSpawn((s, g, p, e) -> false)
                    .isRedstoneConductor((s, g, p) -> false)
                    .isSuffocating((s, g, p) -> false)
                    .isViewBlocking((s, g, p) -> false)));

    public static final RegistryObject<Item> BRIDGE_GLASS_ITEM = ITEMS.register("bridge_glass",
            () -> new BridgeGlassItem(new Item.Properties()));
    public static final RegistryObject<Item> BRIDGE_WAND = ITEMS.register("bridge_wand",
            () -> new BridgeWandItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> FINAL_BLOCK_ITEM = ITEMS.register("final_block",
            () -> new BlockItem(FINAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> BLOCK_ONE_ITEM = ITEMS.register("block_one",
            () -> new BlockItem(BLOCK_ONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOCK_TWO_ITEM = ITEMS.register("block_two",
            () -> new BlockItem(BLOCK_TWO.get(), new Item.Properties()));

    // ---- galletas (dalgona): 4 bloques con bandeja y 4 ítems jugables
    public static final RegistryObject<Item> COOKIE_ITEM_BLUE = cookieItem(CookieColor.BLUE);
    public static final RegistryObject<Item> COOKIE_ITEM_GREEN = cookieItem(CookieColor.GREEN);
    public static final RegistryObject<Item> COOKIE_ITEM_YELLOW = cookieItem(CookieColor.YELLOW);
    public static final RegistryObject<Item> COOKIE_ITEM_RED = cookieItem(CookieColor.RED);

    public static final RegistryObject<Block> COOKIE_BLUE = cookieBlock(CookieColor.BLUE, COOKIE_ITEM_BLUE);
    public static final RegistryObject<Block> COOKIE_GREEN = cookieBlock(CookieColor.GREEN, COOKIE_ITEM_GREEN);
    public static final RegistryObject<Block> COOKIE_YELLOW = cookieBlock(CookieColor.YELLOW, COOKIE_ITEM_YELLOW);
    public static final RegistryObject<Block> COOKIE_RED = cookieBlock(CookieColor.RED, COOKIE_ITEM_RED);

    public static final RegistryObject<Item> COOKIE_BLOCK_BLUE = cookieBlockItem(COOKIE_BLUE, "cookie_blue");
    public static final RegistryObject<Item> COOKIE_BLOCK_GREEN = cookieBlockItem(COOKIE_GREEN, "cookie_green");
    public static final RegistryObject<Item> COOKIE_BLOCK_YELLOW = cookieBlockItem(COOKIE_YELLOW, "cookie_yellow");
    public static final RegistryObject<Item> COOKIE_BLOCK_RED = cookieBlockItem(COOKIE_RED, "cookie_red");

    public static final RegistryObject<Item> CONTROL_STICK = ITEMS.register("control_stick",
            () -> new ControlStickItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HEADPHONES = ITEMS.register("headphones",
            () -> new HeadphonesItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<SoundEvent> GREEN_LIGHT = sound("green_light");
    public static final RegistryObject<SoundEvent> RED_LIGHT = sound("red_light");
    public static final RegistryObject<SoundEvent> GUNSHOT = sound("gunshot");
    public static final RegistryObject<SoundEvent> PASS = sound("pass");
    public static final RegistryObject<SoundEvent> FAIL = sound("fail");

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("squidgame",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.squidgame"))
                    .icon(() -> new ItemStack(BLOCK_TWO_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(BLOCK_ONE_ITEM.get());
                        output.accept(BLOCK_TWO_ITEM.get());
                        output.accept(HEADPHONES.get());
                        output.accept(BRIDGE_GLASS_ITEM.get());
                        output.accept(BRIDGE_WAND.get());
                        output.accept(FINAL_BLOCK_ITEM.get());
                        output.accept(COOKIE_BLOCK_BLUE.get());
                        output.accept(COOKIE_BLOCK_GREEN.get());
                        output.accept(COOKIE_BLOCK_YELLOW.get());
                        output.accept(COOKIE_BLOCK_RED.get());
                        output.accept(COOKIE_ITEM_BLUE.get());
                        output.accept(COOKIE_ITEM_GREEN.get());
                        output.accept(COOKIE_ITEM_YELLOW.get());
                        output.accept(COOKIE_ITEM_RED.get());
                        output.accept(CONTROL_STICK.get());
                    })
                    .build());

    private static RegistryObject<Item> cookieItem(CookieColor c) {
        return ITEMS.register("cookie_item_" + c.id, () -> new CookieItem(c, new Item.Properties().stacksTo(1)));
    }

    private static RegistryObject<Block> cookieBlock(CookieColor c, RegistryObject<Item> item) {
        return BLOCKS.register("cookie_" + c.id, () -> new CookieBlock(c, item::get,
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.5F).sound(SoundType.STONE)
                        .noOcclusion()));
    }

    private static RegistryObject<Item> cookieBlockItem(RegistryObject<Block> block, String name) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SquidGameMod.MODID, name)));
    }
}
