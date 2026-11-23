package example.sicore;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.166: the TESU top electrode is gone. The middle of the cap is casing, and the electrode's id is an alias of the casing:
 * an electrode saved in a chunk, a structure or an inventory loads as casing, so a TESU built before stays formed.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111166GameTests {
    private Round111166GameTests() {}

    private static final ResourceLocation ELECTRODE = ResourceLocation.fromNamespaceAndPath("mio_icif", "wiring/block_tesu_electrode");
    private static final ResourceLocation CASING = ResourceLocation.fromNamespaceAndPath("mio_icif", "wiring/block_tesu_casing");

    @GameTest(template = "empty", batch = "round111166_tesu")
    public static void anOldElectrodeLoadsAsCasing(GameTestHelper h) {
        Block casing = BuiltInRegistries.BLOCK.get(CASING);
        h.assertTrue(BuiltInRegistries.BLOCK.get(ELECTRODE) == casing, "the electrode id gives " + BuiltInRegistries.BLOCK.get(ELECTRODE));
        h.assertTrue(BuiltInRegistries.ITEM.get(ELECTRODE) == casing.asItem(), "the electrode item id gives " + BuiltInRegistries.ITEM.get(ELECTRODE));
        CompoundTag saved = new CompoundTag();
        saved.putString("Name", ELECTRODE.toString());
        // a chunk's block palette
        BlockState chunk = BlockState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        h.assertTrue(chunk.is(casing), "a saved electrode in a chunk loads as " + chunk);
        // a structure file or a falling block
        BlockState structure = NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK), saved);
        h.assertTrue(structure.is(casing), "a saved electrode in a structure loads as " + structure);
        // an inventory
        CompoundTag item = new CompoundTag();
        item.putString("id", ELECTRODE.toString());
        item.putInt("count", 3);
        ItemStack stack = ItemStack.CODEC.parse(h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), item).getOrThrow();
        h.assertTrue(stack.is(casing.asItem()) && stack.getCount() == 3, "3 saved electrodes load as " + stack);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111166_tesu")
    public static void theCapIsAllCasing(GameTestHelper h) {
        Class<?> structure = Round33GameTests.common("multiblock.TesuStructure");
        for (Object part : Round33GameTests.common("multiblock.TesuStructure$Part").getEnumConstants())
            h.assertTrue(!String.valueOf(part).equals("ELECTRODE"), "the structure still has an electrode part");
        @SuppressWarnings("unchecked")
        List<Object> cells = (List<Object>) Round33GameTests.call(Round33GameTests.call(structure, "get"), "cells");
        int top = Integer.MIN_VALUE;
        for (Object cell : cells) top = Math.max(top, (Integer) Round33GameTests.call(cell, "y"));
        int cap = 0;
        for (Object cell : cells) {
            if ((Integer) Round33GameTests.call(cell, "y") != top) continue;
            String part = String.valueOf(Round33GameTests.call(cell, "part"));
            h.assertTrue(part.equals("CASING"), "the cap holds " + part);
            cap++;
        }
        h.assertTrue(cap == 49, "the top layer has " + cap + " blocks, not 49");   // 0.1.11.168: the structure is a 7 x 7 box
        h.succeed();
    }
}
