package com.singularity_iteration.mio_icif.Blocks.entity.reactor;

import com.singularity_iteration.mio_icif.api.item.IReactorChamber;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import com.singularity_iteration.mio_icif.energy.grid.*;
import com.singularity_iteration.mio_icif.energy.heat.IHeatStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 核反应堆腔室方块实体
 * 
 * 功能：
 * - 用于增加核反应堆的额外槽位数
 * - 每个面接触核反应堆就额外添加6个额外槽位（每个槽位置）
 * - 当腔室完全包围核反应堆时，核反应堆拥有完整的 54 个可用槽
 * - 腔室自身没有 GUI，显示所连接的核反应堆的 GUI
 * - 当核反应堆被移除时接触两个核反应堆会断开连接
 */
@SuppressWarnings("null")
public class mio_icif_reactor_chamber extends BlockEntity implements MenuProvider, IEnergySource, IReactorChamber {
    
    // 存储连接的核反应堆位置
    private BlockPos connectedReactorPos = null;
    
    // 标记是否已验证连接
    private boolean connectionValidated = false;
    
    // 上次验证的时间（tick 计数）
    private long lastValidationTick = 0;
    
    // 验证间隔：每 20 tick 验证一次（约1 秒）
    private static final int VALIDATION_INTERVAL = 20;
    
    public mio_icif_reactor_chamber(BlockPos pos, BlockState state) {
        super(com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities.REACTOR_CHAMBER_ENTITY_TYPE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // 方块实体载入时立即寻找相邻的核反应堆
        // 这样即使NBT 加载没有connectedReactorPos，也能尽快建立连接
        if (level != null && !level.isClientSide && connectedReactorPos == null) {
            findAdjacentReactor(level, worldPosition);
        }
    }

    /**
     * 每tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_reactor_chamber chamber) {
        if (level.isClientSide()) {
            return;
        }

        // 定期验证连接，确保是有效连接
        chamber.validateConnection(level, pos);

        // 如果连接有效，根据堆温显示粒子效果
        chamber.spawnHeatParticles(level, pos);
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
                spawnParticlesAtChamber(level, pos, net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, 2);
            }
        }

        // 堆温超过 40%：更多烟灰粒子
        if (heatPercentage >= 0.40) {
            if (level.getGameTime() % 8 == 0) {
                spawnParticlesAtChamber(level, pos, net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, 3);
            }
        }

        // 堆温超过 60%：产生火焰粒子
        if (heatPercentage >= 0.60) {
            if (level.getGameTime() % 5 == 0) {
                spawnParticlesAtChamber(level, pos, net.minecraft.core.particles.ParticleTypes.FLAME, 3);
            }
        }

        // 堆温超过 80%：大量火焰粒子
        if (heatPercentage >= 0.80) {
            if (level.getGameTime() % 3 == 0) {
                spawnParticlesAtChamber(level, pos, net.minecraft.core.particles.ParticleTypes.FLAME, 5);
                spawnParticlesAtChamber(level, pos, net.minecraft.core.particles.ParticleTypes.LAVA, 2);
            }
        }
    }

    /**
     * 在腔室位置产生粒子
     */
    private void spawnParticlesAtChamber(Level level, BlockPos pos, net.minecraft.core.particles.ParticleOptions particleType, int count) {
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
     * 注意：邻居变化检测已移至事件系统（mio_icif_events.onNeighborNotify）
     * 这里确保连接仍然有效
     */
    private void validateConnection(Level level, BlockPos pos) {
        long currentTick = level.getGameTime();

        // 只在达到验证间隔时才检查
        if (currentTick - lastValidationTick < VALIDATION_INTERVAL) {
            return;
        }

        lastValidationTick = currentTick;

        // 如果世界正在处理方块更新，跳过验证以避免死锁
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
            }
            // 核反应堆还存在，不需要重新寻找
            return;
        }

        // 没有连接的核反应堆，寻找相邻的核反应堆
        findAdjacentReactor(level, pos);
    }

    /**
     * 寻找相邻的核反应堆
     * 找到并记录连接，邻居变化通知已由事件系统处理
     */
    private void findAdjacentReactor(Level level, BlockPos pos) {
        // 遍历六个面
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_Nuclear_Reactor_Generator) {
                // 找到核反应堆，记录连接
                connectedReactorPos = neighborPos;
                connectionValidated = true;
                setChanged();
                return;
            }
        }

        // 没找到连接的核反应堆
        connectedReactorPos = null;
        connectionValidated = false;
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
    
    // ==================== MenuProvider 接口实现 ====================
    
    @Override
    public Component getDisplayName() {
        // 如果有连接的核反应堆，使用它的显示名称
        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor != null) {
            return reactor.getDisplayName();
        }
        return Component.translatable("container.mio_icif.reactor_chamber");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // 如果有连接的核反应堆，打开它的菜单
        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor != null) {
            return reactor.createMenu(containerId, playerInventory, player);
        }
        
        // 没有连接的核反应堆，无法打开 GUI
        return null;
    }
    
    // ==================== NBT 保存与加载 ====================
    
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
    
    // ==================== 能力转发 ====================
    
    /**
     * 获取 EU 能量存储能力，转发到连接的核反应堆
     * @param direction 方向
     * @return 能量存储能力
     */
    @Nullable
    public IEUEnergyStorage getEnergyStorageCapability(@Nullable Direction direction) {
        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor != null) {
            return reactor.getEnergyStorageCapability(direction);
        }
        return null;
    }
    
    /**
     * 获取热存储能力，转发到连接的核反应堆
     * @param direction 方向
     * @return 热存储能力
     */
    @Nullable
    public IHeatStorage getHeatStorageCapability(@Nullable Direction direction) {
        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor != null) {
            return reactor.getHeatStorage();
        }
        return null;
    }
    
    /**
     * 获取物品处理器能力，转发到连接的核反应堆
     * @param direction 方向
     * @return 物品处理器
     */
    @Nullable
    public IItemHandler getItemHandlerCapability(@Nullable Direction direction) {
        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor != null) {
            return reactor.getItemHandlerCapability(direction);
        }
        return null;
    }
    
    // ==================== IEnergyEmitter ====================

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        // 腔室只要有连接的核反应堆位置就可以向任何方向输出能量
        // 注意：在电网系统调用 getConnectedReactor() 获取EU电网节点时 level 可能尚未设置
        // 电网系统）通过 IMetaDelegate.getSubTiles() 遍历每个腔室，需要通过此方法判断连接
        // 只要 connectedReactorPos 不为 null，就说明这个腔室属于某个核反应堆
        return connectedReactorPos != null;
    }

    @Override
    public double getOfferedEnergy() {
        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor != null) {
            return reactor.getOfferedEnergy();
        }
        return 0.0D;
    }

    @Override
    public void drawEnergy(double amount) {
        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor != null) {
            reactor.drawEnergy((long) amount);
        }
    }

    @Override
    public int getSourceTier() {
        // 委托到连接的核反应堆
        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor != null) {
            return reactor.getSourceTier();
        }
        return -1;
    }

    @Override
    public int getPacketCount() {
        // 委托到连接的核反应堆
        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor != null) {
            return reactor.getPacketCount();
        }
        return 1;
    }
}