package com.miophas.singularity_iteration.common.menu.storage;

import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** 把 core 的独立变压器实体适配成界面数据源。 */
public final class IndependentTransformerView implements TransformerView {

    private final IndependentTransformerBlockEntity entity;

    public IndependentTransformerView(IndependentTransformerBlockEntity entity) {
        this.entity = entity;
    }

    @Override public int modeOrdinal() { return entity.savedMode(); }
    @Override public boolean hasRedstoneSignal() { return entity.hasNeighborRedstoneSignal(); }
    @Override public long storedEnergy() { return (long) entity.storedEnergy(); }
    @Override public long bufferCapacity() { return (long) entity.bufferCapacity(); }
    @Override public long lowSideLimit() { return entity.lowPacket(); }
    @Override public long highSideLimit() { return entity.highPacket(); }
    @Override public void setMode(int ordinal) { entity.setSavedMode(ordinal); }
    @Override public BlockPos viewPos() { return entity.getBlockPos(); }
    @Override public BlockState viewState() { return entity.getBlockState(); }
    @Override public @Nullable Level viewLevel() { return entity.getLevel(); }
    @Override public BlockEntity asBlockEntity() { return entity; }
}
