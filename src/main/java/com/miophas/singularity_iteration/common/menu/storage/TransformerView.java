package com.miophas.singularity_iteration.common.menu.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 变压器界面读取的数据源。
 *
 * <p>core 的独立变压器实体与遗留变压器实体是两种不同的 Java 类型，
 * 界面通过这个视图统一读取，避免菜单依赖某个具体实现。
 */
public interface TransformerView {

    int modeOrdinal();

    boolean hasRedstoneSignal();

    long storedEnergy();

    long bufferCapacity();

    long lowSideLimit();

    long highSideLimit();

    void setMode(int ordinal);

    BlockPos viewPos();

    BlockState viewState();

    @Nullable
    Level viewLevel();

    /** 仍由实体自身提供菜单时（遗留档位）返回自身，否则返回 null。 */
    @Nullable
    default BlockEntity asBlockEntity() { return null; }
}
