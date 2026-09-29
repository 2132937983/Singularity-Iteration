package com.miophas.singularity_iteration.common.block.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_pattern_storage;
import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 模式存储机方块类
 * 用于存储模式扫描机的扫描结果
 */
@SuppressWarnings("null")
public class mio_icif_block_pattern_storage extends mio_icif_entity_block {

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public static final MapCodec<mio_icif_block_pattern_storage> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_pattern_storage::new));

    public mio_icif_block_pattern_storage(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof mio_icif_pattern_storage storage
                && com.miophas.singularity_iteration.core.runtime.processing.MachineMenuAccess.valid(player, storage)) player.openMenu(storage);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        var drops = super.getDrops(state, params);
        var tile = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (tile instanceof mio_icif_pattern_storage storage) {
            for (var stack : drops) if (stack.is(this.asItem())) {
                var data = stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                data.putString("id", net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(storage.getType()).toString());
                data.putLong("energy", storage.getEnergyStorageInternal().getAmount());
                data.putLong("scex_energy_fraction", storage.getEnergyStorageInternal().scexSavedFraction());
                storage.savePatternData(data, params.getLevel().registryAccess());
                // The memory slot drops separately through the normal Container removal path.
                stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(data));
            }
        }
        return drops;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            // The actual memory Container is dropped by the base removal path.
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof mio_icif_pattern_storage storage) {
            // 根据存储数量返回信号强度
            int count = storage.getStoredCount();
            int max = mio_icif_pattern_storage.MAX_PATTERNS;
            return (count * 15) / max;
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_pattern_storage(pos, state, mio_icif_block_entities.PATTERN_STORAGE_ENTITY_TYPE.get());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof mio_icif_pattern_storage storage) {
                mio_icif_pattern_storage.tick(lvl, pos, blockState, storage);
                // 更新方块状态
            boolean hasData = storage.getStoredCount() > 0;
                if (blockState.getValue(LIT) != hasData) {
                    lvl.setBlock(pos, blockState.setValue(LIT, hasData), 3);
                }
            }
        };
    }
}