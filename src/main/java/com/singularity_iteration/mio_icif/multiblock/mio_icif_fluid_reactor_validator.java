package com.singularity_iteration.mio_icif.multiblock;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_reactor_mode;
import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Access_Hatch;
import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Fluid_Port;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 流体反应堆多方块结构验证�? *
 * 验证流体反应堆的结构要求�?x5x5结构）：
 * - 中心3x3x3空间隔? *   - 最中心(0,0,0)：核反应�? *   - 六个面各一个：核反应仓
 * - 棱与顶点：压力容器框架方法? * - 六个3x3的面：至少包含一个功能方块（流体端口、访问接口、红石端口）
 */
@SuppressWarnings("null")
public class mio_icif_fluid_reactor_validator implements mio_icif_multiblock_validator {
    private static final Logger LOGGER = LogUtils.getLogger();

    // 结构半径（从中心到边缘）
    public static final int STRUCTURE_RADIUS = 2;

    @Override
    public mio_icif_multiblock_validation_result validate(Level level, BlockPos controllerPos) {
        LOGGER.info("[流体反应堆验证] 开始验证位置? " + controllerPos);

        // 检查中心位置是否是核反应堆
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator)) {
            LOGGER.info("[流体反应堆验证] 失败: 中心方块不是核反应堆");
            return mio_icif_multiblock_validation_result.failure("中心方块必须是核反应�?");
        }

        Set<BlockPos> structureBlocks = new HashSet<>();
        structureBlocks.add(controllerPos);

        // 验证中心3x3x3区域（核反应�?+ 6个反应仓�
    LOGGER.info("[流体反应堆验证] 检查中�?x3x3区域...");
        mio_icif_multiblock_validation_result centerResult = validateCenter3x3x3(level, controllerPos, structureBlocks);
        if (!centerResult.isValid()) {
            LOGGER.info("[流体反应堆验证] 失败: " + centerResult.getErrorMessage());
            return centerResult;
        }
        LOGGER.info("[流体反应堆验证] 中心3x3x3区域验证通过");

        // 验证棱与顶点（压力容器框架）
        LOGGER.info("[流体反应堆验证] 检查压力容器框�?..");
        mio_icif_multiblock_validation_result frameResult = validateFrame(level, controllerPos, structureBlocks);
        if (!frameResult.isValid()) {
            LOGGER.info("[流体反应堆验证] 失败: " + frameResult.getErrorMessage());
            return frameResult;
        }
        LOGGER.info("[流体反应堆验证] 压力容器框架验证通过");

        // 验证六个3x3的面（至少有一个功能方块）
        LOGGER.info("[流体反应堆验证] 检查功能方法?..");
        mio_icif_multiblock_validation_result faceResult = validateFaces(level, controllerPos, structureBlocks);
        if (!faceResult.isValid()) {
            LOGGER.info("[流体反应堆验证] 失败: " + faceResult.getErrorMessage());
            return faceResult;
        }
        LOGGER.info("[流体反应堆验证] 功能方块验证通过");

        // 统计功能方块数量
        int fluidPortCount = countBlocksInFaces(level, controllerPos, mio_icif_Block_Reactor_Fluid_Port.class);
        int accessHatchCount = countBlocksInFaces(level, controllerPos, mio_icif_Block_Reactor_Access_Hatch.class);
        Set<BlockPos> redstonePortPositions = getBlocksInFaces(level, controllerPos, com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_block_reactor_redstone_port.class);
        int redstonePortCount = redstonePortPositions.size();

        LOGGER.info("[流体反应堆验证] 验证成功! 流体端口: " + fluidPortCount + ", 访问接口: " + accessHatchCount + ", 红石端口: " + redstonePortCount);

        // 验证通过，创建结果
    mio_icif_multiblock_validation_result.Builder builder = mio_icif_multiblock_validation_result.builder()
            .setValid(true)
            .addBlocks(structureBlocks)
            .setStructureData("fluidPortCount", fluidPortCount)
            .setStructureData("accessHatchCount", accessHatchCount)
            .setStructureData("redstonePortCount", redstonePortCount)
            .setStructureData("redstonePortPositions", redstonePortPositions);

        return builder.build();
    }

    /**
     * 验证中心3x3x3区域
     * - 中心：核反应�
 * - 六个面：核反应仓
     */
    private mio_icif_multiblock_validation_result validateCenter3x3x3(Level level, BlockPos center, Set<BlockPos> structureBlocks) {
        // 检查六个面是否是核反应�
    for (Direction direction : Direction.values()) {
            BlockPos chamberPos = center.relative(direction);
            BlockState chamberState = level.getBlockState(chamberPos);

            if (!(chamberState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Chamber)) {
                return mio_icif_multiblock_validation_result.failure(
                    String.format("核反应堆%s面必须是核反应仓", direction.getName()));
            }
            structureBlocks.add(chamberPos);
        }

        return mio_icif_multiblock_validation_result.success(structureBlocks);
    }

    /**
     * 验证棱与顶点（压力容器框架）
     * 5x5x5结构的边缘位置必须是压力容器框架或功能方法
 */
    private mio_icif_multiblock_validation_result validateFrame(Level level, BlockPos center, Set<BlockPos> structureBlocks) {
        // 遍历5x5x5区域
        for (int x = -STRUCTURE_RADIUS; x <= STRUCTURE_RADIUS; x++) {
            for (int y = -STRUCTURE_RADIUS; y <= STRUCTURE_RADIUS; y++) {
                for (int z = -STRUCTURE_RADIUS; z <= STRUCTURE_RADIUS; z++) {
                    // 跳过中心3x3x3区域
                    if (Math.abs(x) <= 1 && Math.abs(y) <= 1 && Math.abs(z) <= 1) {
                        continue;
                    }

                    BlockPos pos = center.offset(x, y, z);

                    // 检查是否是棱或顶点（至少两个坐标的绝对值等�?�
                // 棱或顶点：至少两个坐标在边缘
                    boolean isEdge = (Math.abs(x) == STRUCTURE_RADIUS && Math.abs(y) == STRUCTURE_RADIUS) ||
                                     (Math.abs(x) == STRUCTURE_RADIUS && Math.abs(z) == STRUCTURE_RADIUS) ||
                                     (Math.abs(y) == STRUCTURE_RADIUS && Math.abs(z) == STRUCTURE_RADIUS);

                    if (isEdge) {
                        BlockState state = level.getBlockState(pos);
                        Block block = state.getBlock();

                        // 棱和顶点可以是压力容器框架或功能方块
                        boolean isValidBlock = block instanceof com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_reactorvessel ||
                                               isFunctionalBlock(block);

                        if (!isValidBlock) {
                            return mio_icif_multiblock_validation_result.failure(
                                String.format("位置(%d,%d,%d)必须是压力容器框架或功能方块", x, y, z));
                        }
                        structureBlocks.add(pos);
                    }
                }
            }
        }

        return mio_icif_multiblock_validation_result.success(structureBlocks);
    }

    /**
     * 验证六个3x3的面
     * 六个面必须使用压力容器框架或功能方块填充，且至少有一个功能方法
 */
    private mio_icif_multiblock_validation_result validateFaces(Level level, BlockPos center, Set<BlockPos> structureBlocks) {
        // 六个方向的面
        Direction[] faceDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};

        int totalFunctionalBlocks = 0;

        for (Direction faceDir : faceDirections) {
            // 获取该面�?x3区域
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    BlockPos facePos = getFacePosition(center, faceDir, i, j);

                    // 跳过中心3x3x3区域（已经验证过程
                if (isInCenter3x3x3(center, facePos)) {
                        continue;
                    }

                    // 跳过棱和顶点位置（已经在validateFrame中验证过程
                int dx = Math.abs(facePos.getX() - center.getX());
                    int dy = Math.abs(facePos.getY() - center.getY());
                    int dz = Math.abs(facePos.getZ() - center.getZ());
                    boolean isEdge = (dx == STRUCTURE_RADIUS && dy == STRUCTURE_RADIUS) ||
                                     (dx == STRUCTURE_RADIUS && dz == STRUCTURE_RADIUS) ||
                                     (dy == STRUCTURE_RADIUS && dz == STRUCTURE_RADIUS);
                    if (isEdge) {
                        continue;
                    }

                    BlockState state = level.getBlockState(facePos);
                    Block block = state.getBlock();

                    // 检查是否是有效方块（压力容器框架或功能方方块
                boolean isValidBlock = block instanceof com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_reactorvessel ||
                                           isFunctionalBlock(block);

                    if (!isValidBlock) {
                        return mio_icif_multiblock_validation_result.failure(
                            String.format("%s面的位置(%d,%d,%d)必须使用压力容器框架或功能方块填充，不能是空气或其他方块",
                                faceDir.getName(), dx, dy, dz));
                    }

                    if (isFunctionalBlock(block)) {
                        totalFunctionalBlocks++;
                    }
                    structureBlocks.add(facePos);
                }
            }
        }

        // 六个面总共至少有一个功能方法
    if (totalFunctionalBlocks < 1) {
            return mio_icif_multiblock_validation_result.failure(
                "六个面必须至少有一个功能方块（流体端口、访问接口或红石端口�?");
        }

        return mio_icif_multiblock_validation_result.success(structureBlocks);
    }

    /**
     * 获取面的位置
     */
    private BlockPos getFacePosition(BlockPos center, Direction faceDir, int offset1, int offset2) {
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();

        switch (faceDir) {
            case NORTH:
                return new BlockPos(x + offset1, y + offset2, z - STRUCTURE_RADIUS);
            case SOUTH:
                return new BlockPos(x + offset1, y + offset2, z + STRUCTURE_RADIUS);
            case EAST:
                return new BlockPos(x + STRUCTURE_RADIUS, y + offset1, z + offset2);
            case WEST:
                return new BlockPos(x - STRUCTURE_RADIUS, y + offset1, z + offset2);
            case UP:
                return new BlockPos(x + offset1, y + STRUCTURE_RADIUS, z + offset2);
            case DOWN:
                return new BlockPos(x + offset1, y - STRUCTURE_RADIUS, z + offset2);
            default:
                return center;
        }
    }

    /**
     * 检查位置是否在中心3x3x3区域�
 */
    private boolean isInCenter3x3x3(BlockPos center, BlockPos pos) {
        return Math.abs(pos.getX() - center.getX()) <= 1 &&
               Math.abs(pos.getY() - center.getY()) <= 1 &&
               Math.abs(pos.getZ() - center.getZ()) <= 1;
    }

    /**
     * 检查是否是功能方块
     */
    private boolean isFunctionalBlock(Block block) {
        return block instanceof mio_icif_Block_Reactor_Fluid_Port
            || block instanceof mio_icif_Block_Reactor_Access_Hatch
            || block instanceof com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_block_reactor_redstone_port;
    }

    /**
     * 统计六个面中特定类型的方块数据
 */
    private int countBlocksInFaces(Level level, BlockPos center, Class<? extends Block> blockClass) {
        return getBlocksInFaces(level, center, blockClass).size();
    }

    /**
     * 获取六个面中特定类型的方块位置
 */
    private Set<BlockPos> getBlocksInFaces(Level level, BlockPos center, Class<? extends Block> blockClass) {
        Set<BlockPos> positions = new HashSet<>();
        Direction[] faceDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};

        for (Direction faceDir : faceDirections) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    BlockPos facePos = getFacePosition(center, faceDir, i, j);

                    // 跳过中心3x3x3区域
                    if (isInCenter3x3x3(center, facePos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(facePos);
                    if (blockClass.isInstance(state.getBlock())) {
                        positions.add(facePos);
                    }
                }
            }
        }

        return positions;
    }

    @Override
    public void onStructureFormed(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        // 结构形成时，将核反应堆切换到流体模式
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof mio_icif_nuclear_reactor_generator reactor) {
            @SuppressWarnings("unchecked")
            mio_icif_multiblock_manager<mio_icif_fluid_reactor_validator> typedManager = 
                (mio_icif_multiblock_manager<mio_icif_fluid_reactor_validator>) manager;
            reactor.setFluidReactorMultiblock(typedManager);
            reactor.setReactorMode(mio_icif_reactor_mode.FLUID);
        }
    }

    @Override
    public void onStructureBroken(Level level, BlockPos controllerPos, mio_icif_multiblock_manager<?> manager) {
        // 结构破坏时，将核反应堆切换回发电模式
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof mio_icif_nuclear_reactor_generator reactor) {
            reactor.onMultiblockBroken();
        }
    }

    @Override
    public String getStructureName() {
        return "FluidReactor5x5x5";
    }

    /**
     * 检查指定位置是否是有效的流体反应堆结构方块
     */
    public static boolean isValidStructureBlock(Block block) {
        return block instanceof mio_icif_Block_Reactor_Fluid_Port
            || block instanceof mio_icif_Block_Reactor_Access_Hatch
            || block instanceof com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_block_reactor_redstone_port
            || block instanceof com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Chamber
            || block instanceof com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_reactorvessel
            || block instanceof com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator;
    }
}

