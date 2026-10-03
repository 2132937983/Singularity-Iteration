package com.miophas.singularity_iteration.common.block;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * What a machine keeps when it is picked up with a wrench: its full block-entity data (energy,
 * upgrades and inventory, modes, filters / ghost items, redstone mode, upgrade directions, step
 * mode ...) copied into {@link DataComponents#BLOCK_ENTITY_DATA}. Vanilla's {@code BlockItem}
 * loads it back into the new block entity on placement.
 *
 * <p>World-bound runtime state is stripped first (a miner's dig position and layer, multiblock
 * links, chunk-loader tickets, timers, in-flight transfers, the remote-off switch of a terminal
 * the machine may no longer be wired to), so a machine placed elsewhere starts clean. A tag that
 * would grow past {@link #MAX_BYTES} keeps settings only and lets the inventory spill as before.
 */
public final class MachineItemData {
    private MachineItemData() {}

    /** Safe ceiling for an item component (client packets cap item NBT at 2 MiB). */
    public static final int MAX_BYTES = 256 * 1024;

    /** Runtime keys of every SI machine. */
    private static final Set<String> ALWAYS_STRIP = Set.of(
        "scex_fe_uncertain_output", "is_power_source", "power_output", "cable_tier", "remote_disabled",
        "progress", "is_working", "scex_item_output_pending", "scex_item_input_pending",
        "scex_fluid_output_pending", "scex_fluid_input_pending", "scex_machine_action_owner",
        // engine-side transfer ledgers of pipes / generators
        "scex_pipe_source", "scex_pipe_phase", "scex_pipe_transfer_id", "scex_pipe_uncertain",
        "scex_extract_transfer", "scex_output_transfer");

    /** Per block-entity type: position- or time-bound state (checked with instanceof, most specific first). */
    private static final Map<String, Set<String>> TYPE_STRIP = new LinkedHashMap<>();
    static {
        TYPE_STRIP.put("com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_advanced_miner_elc", Set.of(
            "tipPos", "oresInCurrentLayer", "currentDepth", "currentOreIndex", "workTicker",
            "scex_mining_payment", "scex_pending_mining_drops"));
        TYPE_STRIP.put("com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_miner_elc", Set.of(
            "TipPosX", "TipPosY", "TipPosZ", "OreCount", "CurrentDepth", "CurrentOreIndex", "IsPaused",
            "scex_layer_ready", "scex_mining_route", "scex_pipe_advance", "scex_mining_cost",
            "scex_mining_payment", "scex_pending_mining_drops"));
        TYPE_STRIP.put("com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader", Set.of("loadedChunks", "active"));
        TYPE_STRIP.put("com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyPart", Set.of(
            "coreX", "coreY", "coreZ", "structureCompleted", "structureComplete"));
        TYPE_STRIP.put("com.miophas.singularity_iteration.common.armory.mio_icif_armory", Set.of("ArmoryBusyUntil"));
        TYPE_STRIP.put("com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_tesla", Set.of("LastAttacked", "WorkTimer"));
        TYPE_STRIP.put("com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_teleporter_elc", Set.of("cooldown", "isActive"));
        TYPE_STRIP.put("com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_terra_elc", Set.of("tick_counter", "last_x", "last_z"));
        TYPE_STRIP.put("com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_molecular_transformer", Set.of("currentRecipeEU", "consumedEU"));
        TYPE_STRIP.put("com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_laser_tower", Set.of("TowerCooldown"));
        TYPE_STRIP.put("com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity", Set.of("active", "redstone_signal"));
        TYPE_STRIP.put("com.miophas.singularity_iteration.common.blockentity.transformer.mio_icif_transformer", Set.of("active", "redstone_signal"));
    }

    private static boolean isA(BlockEntity be, String className) {
        for (Class<?> c = be.getClass(); c != null; c = c.getSuperclass()) if (c.getName().equals(className)) return true;
        return false;
    }

    /**
     * Item data for {@code be}. With {@code withInventory=false} item slots are removed (they were
     * or will be spilled). Returns null when the block entity has nothing to keep.
     */
    @Nullable
    public static CompoundTag capture(BlockEntity be, HolderLookup.Provider registries, boolean withInventory) {
        CompoundTag tag = be.saveCustomOnly(registries);
        ALWAYS_STRIP.forEach(tag::remove);
        for (var e : TYPE_STRIP.entrySet()) if (isA(be, e.getKey())) e.getValue().forEach(tag::remove);
        // miner ore list Ore{i}X/Y/Z
        tag.getAllKeys().removeIf(k -> k.matches("Ore\\d+[XYZ]"));
        if (!withInventory) {
            for (String k : new String[]{"inventory", "Inventory", "Items", "items"}) tag.remove(k);
        }
        // energy stays under "energy" for tooltips and for the legacy placement path
        if (be instanceof AbstractEnergyBlockEntity energy) tag.putLong("energy", energy.getEnergyStorage().getAmount());
        var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
        if (type == null) return null;
        tag.putString("id", type.toString());       // BLOCK_ENTITY_DATA requires the id
        return tag;
    }

    /** Serialized size in bytes (compressed-free NBT stream). */
    public static long size(CompoundTag tag) {
        long[] n = {0};
        try (var out = new DataOutputStream(new OutputStream() {
            @Override public void write(int b) { n[0]++; }
            @Override public void write(byte[] b, int off, int len) { n[0] += len; }
        })) {
            NbtIo.write(tag, out);
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
        return n[0];
    }

    /** Puts {@code data} on {@code stack} (no-op for null). */
    public static ItemStack apply(ItemStack stack, @Nullable CompoundTag data) {
        if (data != null) stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(data));
        return stack;
    }

    // ------------------------------------------------------------------ wrench pick-up handshake
    /**
     * Set by the wrench while it removes a machine whose inventory travels inside the item:
     * {@code onRemove} must not spill (and clear) the container then.
     */
    private record Carried(BlockPos pos, CompoundTag data) { }
    private static final ThreadLocal<Carried> CARRIED = new ThreadLocal<>();

    /**
     * Wrench pick-up, step 1 (before the block is removed): captures the full data including the
     * inventory, then empties the block entity's container so that no removal hook - the shared
     * one or a machine's own {@code onRemove} - spills a second copy. Returns false (nothing
     * changed, the inventory spills as usual) when there is no container or the data is too big.
     */
    public static boolean carryContents(BlockEntity be, HolderLookup.Provider registries) {
        if (!(be instanceof net.minecraft.world.Container container)) return false;
        CompoundTag data = capture(be, registries, true);
        if (data == null || size(data) > MAX_BYTES) return false;
        container.clearContent();
        CARRIED.set(new Carried(be.getBlockPos().immutable(), data));
        return true;
    }

    /** Step 3: the drop code takes the captured data for this position (once). */
    @Nullable
    public static CompoundTag takeCarried(BlockPos pos) {
        Carried c = CARRIED.get();
        if (c == null || !c.pos().equals(pos)) return null;
        CARRIED.remove();
        return c.data();
    }

    /** Clears a capture that no drop consumed (removal cancelled, other drop path). */
    public static void endCarry() {
        CARRIED.remove();
    }
}
