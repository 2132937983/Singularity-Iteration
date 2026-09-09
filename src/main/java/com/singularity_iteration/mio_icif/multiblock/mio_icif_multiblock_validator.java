package com.singularity_iteration.mio_icif.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 多方块结构验证器接口
 * 
 * 用于定义特定多方块结构的验证逻辑
 */
public interface mio_icif_multiblock_validator {
    
    /**
     * 验证多方块结构是否有�
 * @param level 世界
     * @param controllerPos 主控方块位置
     * @return 验证结果
     */
    mio_icif_multiblock_validation_result validate(Level level, BlockPos controllerPos);
    
    /**
     * 结构形成时的回调
     * @param level 世界
     * @param controllerPos 主控方块位置
     * @param manager 多方块结构管理器
     */
    default void onStructureFormed(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        // 默认空实�
}
    
    /**
     * 结构破坏时的回调
     * @param level 世界
     * @param controllerPos 主控方块位置
     * @param manager 多方块结构管理器
     */
    default void onStructureBroken(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        // 默认空实�
}
    
    /**
     * 获取结构名称（用于调试和日志�
 * @return 结构名称
     */
    default String getStructureName() {
        return getClass().getSimpleName();
    }
}


