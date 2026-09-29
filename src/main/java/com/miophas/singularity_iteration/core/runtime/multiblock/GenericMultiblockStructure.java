package com.miophas.singularity_iteration.core.runtime.multiblock;

import com.miophas.singularity_iteration.core.api.event.MultiblockBrokenEvent;
import com.miophas.singularity_iteration.core.api.event.MultiblockFormedEvent;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidator;
import com.miophas.singularity_iteration.core.api.machine.IMultiblockValidationResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 通用多方块结构生命周期实现，供构建器与内置管理器共用。
 *
 * <p>负责验证、成形注册、快速/完整重验、拆除，并在成形与拆除时依次
 * 执行验证器钩子、构建器回调和公开事件。内置机器不再在此写死，
 * 控制方块到验证器的映射见 {@link MultiblockControllers}。
 */
public class GenericMultiblockStructure implements IMultiblockStructure {

    private final BlockPos controllerPos;
    private final IMultiblockValidator validator;
    private final Set<BlockPos> structureBlocks = new HashSet<>();
    private final Map<String, Object> structureData = new HashMap<>();
    @Nullable private final BiConsumer<Level, BlockPos> onFormedCallback;
    @Nullable private final BiConsumer<Level, BlockPos> onBrokenCallback;
    private boolean isValid = false;
    private long formationTime = 0;

    public GenericMultiblockStructure(BlockPos controllerPos, IMultiblockValidator validator,
            @Nullable BiConsumer<Level, BlockPos> onFormedCallback,
            @Nullable BiConsumer<Level, BlockPos> onBrokenCallback) {
        if (controllerPos == null || validator == null) {
            throw new IllegalArgumentException("Controller position and validator are required");
        }
        this.controllerPos = controllerPos;
        this.validator = validator;
        this.onFormedCallback = onFormedCallback;
        this.onBrokenCallback = onBrokenCallback;
    }

    public boolean tryForm(Level level) {
        if (isValid) {
            return true;
        }

        IMultiblockValidationResult result = validator.validate(level, controllerPos);
        if (!result.isValid()) {
            return false;
        }

        structureBlocks.clear();
        structureBlocks.addAll(result.getStructureBlocks());
        structureData.clear();
        structureData.putAll(result.getStructureData());
        isValid = true;
        formationTime = level.getGameTime();
        ActiveMultiblocks.register(level, this);
        onStructureFormed(level);
        return true;
    }

    public boolean validateStructure(Level level) {
        return isValid && quickValidationCheck(level) || invalidateAndReport(level);
    }

    public boolean validateStructureFull(Level level) {
        if (!isValid) {
            return false;
        }

        for (BlockPos pos : structureBlocks) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (level.getBlockState(pos).isAir()) {
                return invalidateAndReport(level);
            }
        }

        if (!validator.validate(level, controllerPos).isValid()) {
            return invalidateAndReport(level);
        }

        return true;
    }

    private boolean invalidateAndReport(Level level) {
        invalidate(level);
        return false;
    }

    private boolean quickValidationCheck(Level level) {
        BlockState controllerState = level.getBlockState(controllerPos);
        if (controllerState.isAir()) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = controllerPos.relative(direction);
            if (!structureBlocks.contains(neighborPos)) {
                continue;
            }
            if (!level.isLoaded(neighborPos)) {
                continue;
            }
            if (level.getBlockState(neighborPos).isAir()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean invalidate(Level level) {
        if (!isValid) {
            return false;
        }

        isValid = false;
        Set<BlockPos> formerBlocks = new HashSet<>(structureBlocks);
        ActiveMultiblocks.unregister(level, this);
        onStructureBroken(level, formerBlocks);
        structureBlocks.clear();
        structureData.clear();
        return true;
    }

    protected void onStructureFormed(Level level) {
        validator.onStructureFormed(level, controllerPos, this);
        if (onFormedCallback != null) {
            onFormedCallback.accept(level, controllerPos);
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
            new MultiblockFormedEvent(level, controllerPos, this, validator.getStructureName()));
    }

    protected void onStructureBroken(Level level, Set<BlockPos> formerBlocks) {
        validator.onStructureBroken(level, controllerPos, this);
        if (onBrokenCallback != null) {
            onBrokenCallback.accept(level, controllerPos);
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
            new MultiblockBrokenEvent(level, controllerPos, validator.getStructureName(), formerBlocks));
    }

    public IMultiblockValidator getValidator() {
        return validator;
    }

    @Override
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    @Override
    public Set<BlockPos> getStructureBlocks() {
        return Collections.unmodifiableSet(structureBlocks);
    }

    @Override
    public boolean isPartOfStructure(BlockPos pos) {
        return structureBlocks.contains(pos);
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public long getFormationTime() {
        return formationTime;
    }

    @Override
    public Map<String, Object> getStructureData() {
        return Collections.unmodifiableMap(structureData);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<BlockPos> getRedstonePorts() {
        Object positions = structureData.get("redstonePortPositions");
        if (positions instanceof Set) {
            return (Set<BlockPos>) positions;
        }
        return Collections.emptySet();
    }

    public void setStructureData(String key, Object value) {
        structureData.put(key, value);
    }
}
