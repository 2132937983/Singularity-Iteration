package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;

/**
 * 多方块结构应实现的接口。
 *
 * <p>API 层通过此接口读取结构信息，而不直接依赖具体实现类
 * {@code mio_icif_multiblock_manager}。
 *
 * <p>使用 {@link #find(Level, BlockPos)} 查找指定位置的结构实例。
 * 使用 {@link #builder()} 创建新的多方块结构。
 */
public interface IMultiblockStructure {

    boolean isValid();

    Set<BlockPos> getStructureBlocks();

    long getFormationTime();

    Map<String, Object> getStructureData();

    Set<BlockPos> getRedstonePorts();

    /**
     * 查找指定位置所属的多方块结构。
     *
     * <p>这是 API 层与内部多块管理器之间的唯一桥接点。
     * 如果内部实现类变更，只需修改此处。
     *
     * @param level 世界
     * @param pos 方块位置（主控或结构内任意方块）
     * @return 结构实例，不存在则返回 null
     */
    static IMultiblockStructure find(Level level, BlockPos pos) {
        return MultiblockBridge.findStructure(level, pos);
    }

    /**
     * 创建多方块结构构建器。
     *
     * <p>附属开发者通过此方法创建自定义多方块结构，无需引用内部实现类。
     *
     * <p>使用示例：
     * <pre>{@code
     * IMultiblockStructure structure = IMultiblockStructure.builder()
     *     .validator(new MyValidator())
     *     .onFormed((level, pos) -> { ... })
     *     .onBroken((level, pos) -> { ... })
     *     .buildAndRegister(level, controllerPos);
     * }</pre>
     *
     * @return 多方块结构构建器
     */
    static IMultiblockBuilder builder() {
        return MultiblockBridge.createBuilder();
    }
}