package com.singularity_iteration.mio_icif.Blocks.Crop.Enriched;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * 富集作物基础类
 * 继承原版CropBlock，支持骨粉催熟和正确的掉落物
 */
@SuppressWarnings("null")
public abstract class UniformCropTemplate extends CropBlock {
    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
        Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)
    };

    private final Supplier<Item> seedSupplier;
    private final Supplier<Item> cropSupplier;

    public UniformCropTemplate(Properties properties, Supplier<Item> seedSupplier, Supplier<Item> cropSupplier) {
        super(properties);
        this.seedSupplier = seedSupplier;
        this.cropSupplier = cropSupplier;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[this.getAge(state)];
    }

    public Item getSeed() {
        return this.seedSupplier.get();
    }

    public Item getCrop() {
        return this.cropSupplier.get();
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof net.minecraft.world.level.block.FarmBlock;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return this.getSeed();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(this.getSeed());
    }
}