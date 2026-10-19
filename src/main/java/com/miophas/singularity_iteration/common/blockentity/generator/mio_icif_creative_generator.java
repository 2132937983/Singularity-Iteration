// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.block.generator.mio_icif_block_creative_generator;
import com.miophas.singularity_iteration.common.menu.generator.CreativeGeneratorMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy;
import com.miophas.singularity_iteration.core.api.energy.IEnergyPacketSource;
import com.miophas.singularity_iteration.core.api.energy.IOverloadFreeEnergySource;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.11: Creative Generator, a debug source for creative mode.
 *
 * <ul>
 *   <li>Every tick it offers exactly the set output (EU/t) on all six faces: its buffer is refilled to
 *       the output and holds nothing more.</li>
 *   <li>One packet of the whole output per tick, also below one full packet. The packet size has no
 *       tier limit.</li>
 *   <li>Its deliveries never melt cables, strip insulation, shock or explode receivers
 *       ({@link IOverloadFreeEnergySource}). Receivers still take only what they have room for and
 *       what their rated input allows.</li>
 *   <li>Output 0 to {@link #MAX_RATE} EU/t, switchable off. Only a creative player (or an operator)
 *       can open its screen and change it; see the block.</li>
 * </ul>
 */
public class mio_icif_creative_generator extends AbstractEnergyBlockEntity implements IEnergyPacketSource, IOverloadFreeEnergySource {
    public static final long DEFAULT_RATE = 8192L;
    /** Highest output: one trillion EU/t (exact in double meters, far above any tier). */
    public static final long MAX_RATE = 1_000_000_000_000L;
    /** Data slots: output (4 x 16 bits), switch, EU sent in the last tick (4 x 16 bits). */
    public static final int DATA_COUNT = 9;

    private long rate = DEFAULT_RATE;
    private boolean enabled = true;
    /** Balance after the last refill, and the EU that left the buffer since (shown in the screen). */
    private long refilledTo;
    private long lastSent;

    public mio_icif_creative_generator(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.CREATIVE_GENERATOR_ENTITY_TYPE.get(), DEFAULT_RATE, 0, DEFAULT_RATE, CableTier.MAX);
        setAsPowerSource(DEFAULT_RATE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_creative_generator generator) {
        if (level.isClientSide()) return;
        generator.lastSent = Math.max(0, generator.refilledTo - generator.energyStorage.getAmount());
        generator.refill();
        boolean active = generator.enabled && generator.rate > 0;
        if (state.hasProperty(mio_icif_block_creative_generator.ACTIVE) && state.getValue(mio_icif_block_creative_generator.ACTIVE) != active)
            level.setBlock(pos, state.setValue(mio_icif_block_creative_generator.ACTIVE, active), 3);
    }

    /** Fills the buffer to the output (or empties it while off). */
    private void refill() {
        long target = enabled ? rate : 0;
        long stored = energyStorage.getAmount();
        if (stored > target) {
            energyStorage.setEnergy(target);
        } else if (stored < target) {
            if (energyStorage.scexNetworkControlled()) energyStorage.scexGenerateEnergy(EnergyAmount.of(target - stored), false);
            else energyStorage.generateEnergyInternal(target - stored, false);
        }
        refilledTo = energyStorage.getAmount();
    }

    public long rate() { return rate; }
    public boolean enabled() { return enabled; }
    public long lastSent() { return lastSent; }

    /** Sets the output (clamped to 0 .. {@link #MAX_RATE}) and the switch; server side. */
    public void configure(long newRate, boolean on) {
        long clamped = Math.max(0, Math.min(MAX_RATE, newRate));
        if (clamped == rate && on == enabled) return;
        rate = clamped;
        enabled = on;
        long limit = Math.max(1, rate);
        energyStorage.setCapacity(limit);
        energyStorage.setMaxExtract(limit);
        setAsPowerSource(limit);
        refill();
        setChanged();
    }

    @Override
    public EnergyPacketPolicy getEnergyPacketPolicy() {
        return new EnergyPacketPolicy(Math.max(1, rate), 1, true);
    }

    @Override
    public boolean ignoresOverload() {
        return true;
    }

    @Override
    public long getPowerOutput() {
        return enabled ? rate : 0;
    }

    // ------------------------------------------------------------------ screen

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            if (index < 4) return (int) ((rate >>> (16 * index)) & 0xFFFF);
            if (index == 4) return enabled ? 1 : 0;
            return (int) ((lastSent >>> (16 * (index - 5))) & 0xFFFF);
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    /** Joins four 16-bit data slots (from {@code first}) into a long. */
    public static long join(ContainerData data, int first) {
        long value = 0;
        for (int i = 0; i < 4; i++) value |= (long) (data.get(first + i) & 0xFFFF) << (16 * i);
        return value;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mio_icif.generator.block_creative_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CreativeGeneratorMenu(id, playerInventory, this, data);
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("CreativeRate", rate);
        tag.putBoolean("CreativeEnabled", enabled);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("CreativeRate")) {
            rate = Math.max(0, Math.min(MAX_RATE, tag.getLong("CreativeRate")));
            enabled = !tag.contains("CreativeEnabled") || tag.getBoolean("CreativeEnabled");
            long limit = Math.max(1, rate);
            energyStorage.setCapacity(limit);
            energyStorage.setMaxExtract(limit);
            setAsPowerSource(limit);
        }
    }
}
