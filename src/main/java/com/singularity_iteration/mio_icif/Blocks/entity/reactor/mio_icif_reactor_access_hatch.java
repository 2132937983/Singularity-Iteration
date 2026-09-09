package com.singularity_iteration.mio_icif.Blocks.entity.reactor;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator;
import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_reactor_mode;
import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Access_Hatch;
import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Fluid_Port;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 流体反应堆访问接口方块实体
 * 
 * 功能：
 * - 用于访问流体反应堆多方块结构中的核反应堆
 * - 自身没有 GUI，右键显示所连接的核反应堆的 GUI
 * - 只有在有效的流体反应堆结构下才能正常工作
 * - 会强制将连接的核反应堆切换到流体模式
 */
@SuppressWarnings("null")
public class mio_icif_reactor_access_hatch extends BlockEntity implements MenuProvider, com.singularity_iteration.mio_icif.api.reactor.IAccessHatch {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 存储连接的核反应堆位置
    private BlockPos connectedReactorPos = null;
    
    // 标记是否已验证连接
    private boolean connectionValidated = false;
    
    // 上次验证的时间（tick 计数）
    private long lastValidationTick = 0;
    
    // 验证间隔（每 20 tick 验证一次，约1 秒）
    private static final int VALIDATION_INTERVAL = 20;
    
    public mio_icif_reactor_access_hatch(BlockPos pos, BlockState state) {
        super(com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities.REACTOR_ACCESS_HATCH_ENTITY_TYPE.get(), pos, state);
    }
    
    /**
     * 每tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_reactor_access_hatch hatch) {
        if (level.isClientSide()) {
            return;
        }

        // 定期验证连接
        hatch.validateConnection(level, pos);

        // 如果连接有效，确保核反应堆处于流体模式
        hatch.ensureFluidMode(level);

        // 根据连接的核反应堆堆温显示粒子效果
        hatch.spawnHeatParticles(level, pos);
    }

    /**
     * 根据核反应堆堆温产生粒子效果
     */
    private void spawnHeatParticles(Level level, BlockPos pos) {
        if (level.isClientSide()) return;

        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor == null) return;

        int currentHeat = (int) reactor.getCurrentHeat();
        int maxHeat = (int) reactor.getMaxHeat();
        double heatPercentage = (double) currentHeat / maxHeat;

        // 堆温超过 20%：产生烟灰粒子
        if (heatPercentage >= 0.20) {
            if (level.getGameTime() % 10 == 0) {
                spawnParticlesAtHatch(level, pos, net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, 2);
            }
        }

        // 堆温超过 40%：更多烟灰粒子
        if (heatPercentage >= 0.40) {
            if (level.getGameTime() % 8 == 0) {
                spawnParticlesAtHatch(level, pos, net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, 3);
            }
        }

        // 堆温超过 60%：产生火焰粒子
        if (heatPercentage >= 0.60) {
            if (level.getGameTime() % 5 == 0) {
                spawnParticlesAtHatch(level, pos, net.minecraft.core.particles.ParticleTypes.FLAME, 3);
            }
        }

        // 堆温超过 80%：大量火焰粒子
        if (heatPercentage >= 0.80) {
            if (level.getGameTime() % 3 == 0) {
                spawnParticlesAtHatch(level, pos, net.minecraft.core.particles.ParticleTypes.FLAME, 5);
                spawnParticlesAtHatch(level, pos, net.minecraft.core.particles.ParticleTypes.LAVA, 2);
            }
        }
    }

    /**
     * 在访问接口位置产生粒子
     */
    private void spawnParticlesAtHatch(Level level, BlockPos pos, net.minecraft.core.particles.ParticleOptions particleType, int count) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8;
        double y = pos.getY() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8;
        double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8;

        serverLevel.sendParticles(
            particleType,
            x, y, z,
            count, 0.1, 0.1, 0.1, 0.01
        );
    }
    
    /**
     * 验证与核反应堆的连接
     */
    private void validateConnection(Level level, BlockPos pos) {
        long currentTick = level.getGameTime();
        
        // 只在达到验证间隔时才检查
        if (currentTick - lastValidationTick < VALIDATION_INTERVAL) {
            return;
        }
        
        lastValidationTick = currentTick;
        
        // 如果世界正在处理方块更新，跳过验证
        if (level.isClientSide() || !level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return;
        }
        
        if (connectedReactorPos != null) {
            // 检查之前连接的核反应堆是否还存在
            BlockState reactorState = level.getBlockState(connectedReactorPos);
            if (!(reactorState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator)) {
                // 核反应堆不存在了，重置连接
                connectedReactorPos = null;
                connectionValidated = false;
                setChanged();
            } else {
                // 核反应堆还存在，检查是否仍然是有效的流体反应堆结构
                if (!isValidFluidReactorStructure(level, connectedReactorPos)) {
                    // 结构不再有效，断开连接
                    connectedReactorPos = null;
                    connectionValidated = false;
                    setChanged();
                }
                return;
            }
        }
        
        // 没有连接的核反应堆，寻找相邻的核反应堆
        findAdjacentReactor(level, pos);
    }
    
    /**
     * 检查是否为有效的流体反应堆结构
     * 使用多方块结构管理器检查
     */
    private boolean isValidFluidReactorStructure(Level level, BlockPos reactorPos) {
        // 使用多方块结构管理器检查
        mio_icif_multiblock_manager<?> structure = mio_icif_multiblock_manager.getStructureByController(level, reactorPos);
        return structure != null && structure.isValid();
    }
    
    /**
     * 寻找相邻或附近的核反应堆（2格范围内）
     */
    private void findAdjacentReactor(Level level, BlockPos pos) {
        BlockPos reactorPos = null;
        int reactorCount = 0;

        // 首先检查六个面（相邻）
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator) {
                reactorPos = neighborPos;
                reactorCount++;
            }
        }

        // 如果没有找到相邻的核反应堆，检查2格范围内（用于流体反应堆结构中的面位置）
        if (reactorCount == 0) {
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        // 跳过已经检查过的相邻位置
                        if (Math.abs(x) + Math.abs(y) + Math.abs(z) <= 1) {
                            continue;
                        }

                        BlockPos checkPos = pos.offset(x, y, z);
                        BlockState checkState = level.getBlockState(checkPos);

                        if (checkState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator) {
                            reactorPos = checkPos;
                            reactorCount++;
                        }
                    }
                }
            }
        }

        if (reactorCount == 0) {
            // 没有连接的核反应堆
            connectedReactorPos = null;
            connectionValidated = false;
        } else if (reactorCount == 1) {
            // 只连接了一个核反应堆，尝试触发结构验证
            // 通知多方块结构管理器检查这个核反应堆
            mio_icif_multiblock_manager.notifyBlockChanged(level, reactorPos);

            // 检查是否是有效的流体反应堆结构
            if (isValidFluidReactorStructure(level, reactorPos)) {
                connectedReactorPos = reactorPos;
                connectionValidated = true;
            } else {
                connectedReactorPos = null;
                connectionValidated = false;
            }
        } else {
            // 连接了多个核反应堆，会爆炸
            explode(level, pos);
        }
    }
    
    /**
     * 确保核反应堆处于流体模式
     */
    private void ensureFluidMode(Level level) {
        if (connectedReactorPos == null) {
            return;
        }

        BlockEntity be = level.getBlockEntity(connectedReactorPos);
        if (be instanceof mio_icif_nuclear_reactor_generator reactor) {
            // 检查结构是否有效
            mio_icif_multiblock_manager<?> structure = mio_icif_multiblock_manager.getStructureByController(level, connectedReactorPos);
            if (structure != null && structure.isValid()) {
                // 结构有效，确保处于流体模式
                if (reactor.getReactorMode() != mio_icif_reactor_mode.FLUID) {
                    reactor.setReactorMode(mio_icif_reactor_mode.FLUID);
                }
            }
        }
    }
    
    /**
     * 访问接口爆炸
     */
    private void explode(Level level, BlockPos pos) {
        if (level != null && !level.isClientSide()) {
            // 产生爆炸
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 
                1.0f, Level.ExplosionInteraction.BLOCK);
            
            // 移除方块
            level.removeBlock(pos, false);
        }
    }
    
    /**
     * 获取连接的核反应堆
     */
    @Nullable
    public mio_icif_nuclear_reactor_generator getConnectedReactor() {
        if (connectedReactorPos == null || level == null) {
            return null;
        }
        
        BlockEntity be = level.getBlockEntity(connectedReactorPos);
        if (be instanceof mio_icif_nuclear_reactor_generator) {
            return (mio_icif_nuclear_reactor_generator) be;
        }
        
        return null;
    }
    
    /**
     * 获取连接的核反应堆位置
     */
    @Nullable
    public BlockPos getConnectedReactorPos() {
        return connectedReactorPos;
    }
    
/**
 * 检查是否已连接到有效的流体反应堆
     */
    public boolean isConnected() {
        return connectionValidated && connectedReactorPos != null;
    }

/**
 * 调试多方块结构
 * 输出详细的结构诊断信息到控制台和玩家聊天
     */
    public void debugMultiblockStructure(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (level.isClientSide()) {
            return;
        }

        LOGGER.info("========== 流体反应堆多方块结构诊断 ==========");
        LOGGER.info("访问接口位置: " + pos);

        // 1. 检查相邻或附近的核反应堆
        LOGGER.info("\n1. 检查相邻核反应堆");
        BlockPos foundReactorPos = null;
        int reactorCount = 0;

        // 首先检查六个面（相邻）
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            String blockName = neighborState.getBlock().getDescriptionId();

            boolean isReactor = neighborState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator;
            LOGGER.info("  " + direction.getName() + " 方向: " + blockName + (isReactor ? " [核反应堆]" : ""));

            if (isReactor) {
                foundReactorPos = neighborPos;
                reactorCount++;
            }
        }

        // 如果没有找到相邻的，检查2格范围内
        if (reactorCount == 0) {
            LOGGER.info("  未找到相邻核反应堆，检查2格范围内...");
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        // 跳过已经检查过的相邻位置
                        if (Math.abs(x) + Math.abs(y) + Math.abs(z) <= 1) {
                            continue;
                        }

                        BlockPos checkPos = pos.offset(x, y, z);
                        BlockState checkState = level.getBlockState(checkPos);

                        if (checkState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator) {
                            foundReactorPos = checkPos;
                            reactorCount++;
                            LOGGER.info("  在偏移(" + x + "," + y + "," + z + ")发现核反应堆: " + checkPos);
                        }
                    }
                }
            }
        }

        LOGGER.info("  找到的核反应堆数量: " + reactorCount);

        if (reactorCount == 0) {
            LOGGER.info("  [错误: 没有找到相邻的核反应堆");
            player.sendSystemMessage(Component.translatable("message.mio_icif.reactor.no_reactor_found"));
            player.sendSystemMessage(Component.translatable("message.mio_icif.reactor.hint_placement"));
            return;
        } else if (reactorCount > 1) {
            LOGGER.info("  [错误: 连接了多个核反应堆，这会导致爆炸!");
            player.sendSystemMessage(Component.translatable("message.mio_icif.reactor.multiple_reactors"));
            return;
        }

        LOGGER.info("  [找到一个核反应堆，位置: " + foundReactorPos);

        // 2. 检查核反应堆的六个面是否是反应仓
        LOGGER.info("\n2. 检查核反应堆周围的反应仓");
        boolean allChambersValid = true;
        for (Direction direction : Direction.values()) {
            BlockPos chamberPos = foundReactorPos.relative(direction);
            BlockState chamberState = level.getBlockState(chamberPos);
            boolean isChamber = chamberState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_Reactor_Chamber;

            if (isChamber) {
                LOGGER.info("  [" + direction.getName() + " 方向: 反应仓");
            } else {
                LOGGER.info("  [" + direction.getName() + " 方向: " + chamberState.getBlock().getDescriptionId() + " (需要反应仓)");
                allChambersValid = false;
            }
        }

        if (!allChambersValid) {
            LOGGER.info("  [错误: 核反应堆的六个面必须都是反应仓");
            player.sendSystemMessage(Component.translatable("message.mio_icif.reactor.chambers_required"));
        }

        // 3. 检查压力容器框架（棱与顶点）
        LOGGER.info("\n3. 检查压力容器框架（棱与顶点）");
        int frameRadius = 2;
        int frameErrors = 0;

        for (int x = -frameRadius; x <= frameRadius; x++) {
            for (int y = -frameRadius; y <= frameRadius; y++) {
                for (int z = -frameRadius; z <= frameRadius; z++) {
                    // 跳过中心3x3x3区域
                    if (Math.abs(x) <= 1 && Math.abs(y) <= 1 && Math.abs(z) <= 1) {
                        continue;
                    }

                    // 检查是否是棱或顶点
                    boolean isEdge = (Math.abs(x) == frameRadius && Math.abs(y) == frameRadius) ||
                                     (Math.abs(x) == frameRadius && Math.abs(z) == frameRadius) ||
                                     (Math.abs(y) == frameRadius && Math.abs(z) == frameRadius);

                    if (isEdge) {
                        BlockPos checkPos = foundReactorPos.offset(x, y, z);
                        BlockState state = level.getBlockState(checkPos);
                        boolean isFrame = state.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Block_reactorvessel;

                        if (!isFrame) {
                            if (frameErrors < 5) {
                                LOGGER.info("  [" + "位置(" + x + "," + y + "," + z + "): " + state.getBlock().getDescriptionId() + " (需要压力容器框架)");
                            }
                            frameErrors++;
                        }
                    }
                }
            }
        }

        if (frameErrors > 0) {
            LOGGER.info("  [错误: 发现 " + frameErrors + " 个位置缺少压力容器框架");
            if (frameErrors > 5) {
                LOGGER.info("  ... 还有 " + (frameErrors - 5) + " 个错误未显示");
            }
            player.sendSystemMessage(Component.translatable("message.mio_icif.reactor.frame_errors", frameErrors));
        } else {
            LOGGER.info("  [所有棱与顶点都是压力容器框架");
        }

        // 4. 检查六个面的功能方块
        LOGGER.info("\n4. 检查六个面的功能方块");
        Direction[] faceDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};
        int totalFunctionalBlocks = 0;
        StringBuilder faceInfo = new StringBuilder();

        for (Direction faceDir : faceDirections) {
            int faceFunctionalCount = 0;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    BlockPos facePos = getFacePosition(foundReactorPos, faceDir, i, j, frameRadius);

                    // 跳过中心3x3x3区域
                    if (isInCenter3x3x3(foundReactorPos, facePos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(facePos);
                    Block block = state.getBlock();

                    if (isFunctionalBlock(block)) {
                        faceFunctionalCount++;
                        totalFunctionalBlocks++;
                    }
                }
            }
            faceInfo.append("  ").append(faceDir.getName()).append(" [ ").append(faceFunctionalCount).append(" 个功能方块\n");
        }

        LOGGER.info(faceInfo.toString());
        LOGGER.info("  功能方块总数: " + totalFunctionalBlocks);

        if (totalFunctionalBlocks < 1) {
            LOGGER.info("  [错误: 六个面必须至少有一个功能方块（流体端口、访问接口或红石端口）");
            player.sendSystemMessage(Component.translatable("message.mio_icif.reactor.functional_blocks_required"));
        } else {
            LOGGER.info("  [功能方块检查通过");
        }

        // 5. 运行完整的多方块结构验证
        LOGGER.info("\n5. 完整多方块结构验证");
        mio_icif_multiblock_manager<?> structure = mio_icif_multiblock_manager.getStructureByController(level, foundReactorPos);

        if (structure != null) {
            LOGGER.info("  找到已注册的多方块结构");
            LOGGER.info("  结构有效: " + (structure.isValid() ? "§a有效" : "§c无效"));

            if (!structure.isValid()) {
                // 尝试重新验证
                LOGGER.info("  尝试触发重新验证...");
                mio_icif_multiblock_manager.notifyBlockChanged(level, foundReactorPos);
                LOGGER.info("  已发送结构变更通知");
            }
        } else {
            LOGGER.info("  没有找到注册的多方块结构，尝试验证..");
            mio_icif_multiblock_manager.notifyBlockChanged(level, foundReactorPos);
            LOGGER.info("  已发送结构变更通知，请稍后再试");
        }

        // 6. 检查核反应堆方块实体状态
        LOGGER.info("\n6. 核反应堆方块实体状态");
        BlockEntity be = level.getBlockEntity(foundReactorPos);
        if (be instanceof mio_icif_nuclear_reactor_generator reactor) {
            LOGGER.info("  反应堆模式: " + reactor.getReactorMode());
            LOGGER.info("  当前热量: " + reactor.getCurrentHeat());
        } else {
            LOGGER.info("  [无法获取核反应堆方块实体!");
        }

        LOGGER.info("\n========== 诊断结束 ==========");

        // 发送总结到玩家
        player.sendSystemMessage(Component.translatable("message.mio_icif.reactor.diagnostic_complete"));
        player.sendSystemMessage(Component.translatable("message.mio_icif.reactor.reactor_position", foundReactorPos.toShortString()));
        player.sendSystemMessage(Component.translatable("message.mio_icif.reactor.chambers_complete", allChambersValid));
        player.sendSystemMessage(Component.translatable("message.mio_icif.reactor.frame_complete", frameErrors == 0, frameErrors));
        player.sendSystemMessage(Component.translatable("message.mio_icif.reactor.functional_blocks_count", totalFunctionalBlocks));
        player.sendSystemMessage(Component.translatable("message.mio_icif.reactor.multiblock_status", structure != null && structure.isValid()));
    }

    /**
     * 获取面的位置
     */
    private BlockPos getFacePosition(BlockPos center, Direction faceDir, int offset1, int offset2, int radius) {
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();

        switch (faceDir) {
            case NORTH:
                return new BlockPos(x + offset1, y + offset2, z - radius);
            case SOUTH:
                return new BlockPos(x + offset1, y + offset2, z + radius);
            case EAST:
                return new BlockPos(x + radius, y + offset1, z + offset2);
            case WEST:
                return new BlockPos(x - radius, y + offset1, z + offset2);
            case UP:
                return new BlockPos(x + offset1, y + radius, z + offset2);
            case DOWN:
                return new BlockPos(x + offset1, y - radius, z + offset2);
            default:
                return center;
        }
    }

/**
 * 检查位置是否在中心3x3x3区域内
     */
    private boolean isInCenter3x3x3(BlockPos center, BlockPos pos) {
        int dx = Math.abs(pos.getX() - center.getX());
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = Math.abs(pos.getZ() - center.getZ());
        return dx <= 1 && dy <= 1 && dz <= 1;
    }

    /**
     * 检查是否为功能方块
     */
    private boolean isFunctionalBlock(Block block) {
        return block instanceof mio_icif_Block_Reactor_Fluid_Port ||
               block instanceof mio_icif_Block_Reactor_Access_Hatch ||
               block instanceof com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_block_reactor_redstone_port;
    }

    // ==================== MenuProvider 接口实现 ====================
    
    @Override
    public Component getDisplayName() {
        // 如果连接了核反应堆，使用核反应堆的名称
        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor != null) {
            return reactor.getDisplayName();
        }
        return Component.translatable("container.mio_icif.reactor_access_hatch");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // 如果连接了核反应堆，返回核反应堆的菜单（会自动根据模式显示对应的 GUI）
        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor != null) {
            return reactor.createMenu(containerId, playerInventory, player);
        }
        
        // 没有连接的核反应堆，无法打开 GUI
        return null;
    }
    
    // ==================== 数据保存与加载====================
    
    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        if (connectedReactorPos != null) {
            tag.putInt("ReactorX", connectedReactorPos.getX());
            tag.putInt("ReactorY", connectedReactorPos.getY());
            tag.putInt("ReactorZ", connectedReactorPos.getZ());
        }
        tag.putBoolean("ConnectionValidated", connectionValidated);
        tag.putLong("LastValidationTick", lastValidationTick);
    }
    
    @Override
    public void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        if (tag.contains("ReactorX") && tag.contains("ReactorY") && tag.contains("ReactorZ")) {
            connectedReactorPos = new BlockPos(tag.getInt("ReactorX"), tag.getInt("ReactorY"), tag.getInt("ReactorZ"));
        }
        connectionValidated = tag.getBoolean("ConnectionValidated");
        lastValidationTick = tag.getLong("LastValidationTick");
    }
}