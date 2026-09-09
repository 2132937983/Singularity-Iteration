package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 传送机方块实体
 * 使用红石信号在两台传送机之间传送实体
 * 没有GUI，使用权杖右键两台传送机设置配对
 * 只有收到红石信号且配对完成后才会传送
 */
@SuppressWarnings("null")
public class mio_icif_teleporter_elc extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .build();

    // 槽位总数：仅电池槽
    public static final int SLOT_COUNT = 1;
    // 电池槽索引
    public static final int BATTERY_SLOT = 0;

    // 默认配置
    public static final long DEFAULT_CAPACITY = 100000000L; // 100,000,000 EU (1亿)
    public static final long DEFAULT_MAX_RECEIVE = 8192L;   // 最大输入电压8192 EU/t (EV等级)
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_COOLDOWN = 20; // 传送冷却时间 20 ticks (1秒)
    public static final long DEFAULT_ENERGY_PER_TICK = 0L; // 传送机不自动消耗能量

    // 目标传送机位置
    @Nullable
    private BlockPos targetPos;

    // 传送冷却时间
    private int cooldown = 0;

    // 目标检查计时器
    @SuppressWarnings("unused")
    private int targetCheckTicker = 0;

    // 机器是否被红石信号激活且配对完成
    private boolean isActive = false;

    /**
     * 构造 BlockEntityType.Builder 注册用参数构造函数
     */
    public mio_icif_teleporter_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.TELEPORTER_ELC_ENTITY_TYPE.get());
    }

    public mio_icif_teleporter_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_COOLDOWN,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.HV);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.teleporter_elc");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == BATTERY_SLOT && isBattery(stack);
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{BATTERY_SLOT};
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == BATTERY_SLOT;
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == BATTERY_SLOT && isBattery(stack);
    }

    @Override
    public boolean canWork() {
        return false; // 传送机不使用进度工作模式
    }

    @Override
    protected void doWork() {
        // 传送机不使用进度工作模式
    }

    @Override
    protected boolean shouldResetProgress() {
        return false;
    }

    /**
     * 每tick执行工作核心逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_teleporter_elc blockEntity) {
        if (level.isClientSide()) {
            // 客户端：显示粒子效果
            blockEntity.tickClient();
            return;
        }

        // 调用父类tick方法处理升级和充放电等逻辑
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 减少传送冷却时间
        if (blockEntity.cooldown > 0) {
            blockEntity.cooldown--;
        }

        // 检测红石信号
        boolean hasRedstoneSignal = level.hasNeighborSignal(pos);

        if (hasRedstoneSignal && blockEntity.targetPos != null) {
            blockEntity.setActive(true);

            // 冷却结束后尝试传送
            if (blockEntity.cooldown <= 0) {
                if (blockEntity.verifyTarget()) {
                    // 寻找需要传送的实体
                    Entity entityToTeleport = blockEntity.findEntityToTeleport();
                    if (entityToTeleport != null) {
                        double distance = Math.sqrt(pos.distSqr(blockEntity.targetPos));
                        blockEntity.teleport(entityToTeleport, distance);
                    }
                }
            }
        } else {
            blockEntity.setActive(false);
        }
        blockEntity.targetCheckTicker++;
    }

    /**
     * 客户端tick - 显示粒子效果
     */
    private void tickClient() {
        if (!isActive) return;

        Level level = this.level;
        if (level == null) return;

        // 根据状态显示不同粒子效果
        if (cooldown > 0) {
            spawnParticles(level, worldPosition, 0.0f, 1.0f, 0.0f); // 绿色 - 传送冷却中
        } else {
            spawnParticles(level, worldPosition, 0.0f, 0.0f, 1.0f); // 蓝色 - 可以传送就绪
        }
    }

    /**
     * 生成粒子效果
     */
    private void spawnParticles(Level level, BlockPos pos, float r, float g, float b) {
        if (level.random.nextInt(5) != 0) return;

        double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 1.5;
        double y = pos.getY() + 1.0 + level.random.nextDouble();
        double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 1.5;

        level.addParticle(net.minecraft.core.particles.DustParticleOptions.REDSTONE,
            x, y, z, r, g, b);
    }

    /**
     * 验证目标传送机是否仍然有效
     */
    private boolean verifyTarget() {
        if (targetPos == null || level == null) return false;

        if (!level.isLoaded(targetPos)) {
            // 如果目标区块未加载，尝试强制加载
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().getChunk(targetPos.getX() >> 4, targetPos.getZ() >> 4, true);
            }
        }

        if (level.getBlockEntity(targetPos) instanceof mio_icif_teleporter_elc) {
            return true;
        }

        // 目标传送机失效，清除配对
        targetPos = null;
        setActive(false);
        return false;
    }

    /**
     * 寻找需要传送的实体
     * 寻找距离传送机最近的实体
     */
    private @Nullable Entity findEntityToTeleport() {
        if (level == null) return null;

        // 搜索区域：传送机上方中心区域
        AABB searchBox = new AABB(
            worldPosition.getX() + 0.2, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.2,
            worldPosition.getX() + 0.8, worldPosition.getY() + 2.5, worldPosition.getZ() + 0.8
        );

        List<Entity> entities = level.getEntities((Entity) null, searchBox, EntitySelector.NO_SPECTATORS);

        Entity closestEntity = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (Entity entity : entities) {
            // 排除骑乘其他实体的实体，传送骑乘实体连同载具一起传送
            if (entity.isPassenger()) continue;

            // 检查实体是否在传送机上方范围内
            double entityY = entity.getY();
            if (entityY < worldPosition.getY() + 0.5 || entityY > worldPosition.getY() + 2.5) {
                continue; // 实体不在传送机上方有效范围内
            }

            double distSq = entity.distanceToSqr(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5
            );

            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                closestEntity = entity;
            }
        }

        return closestEntity;
    }

    /**
     * 执行传送
     */
    private void teleport(Entity entity, double distance) {
        if (targetPos == null || level == null || !(level instanceof ServerLevel serverLevel)) {
            sendMessageToEntity(entity, "block.mio_icif.teleporter_elc.error.invalid_target");
            return;
        }

        // 计算传送重量
        int weight = calculateWeight(entity);
        if (weight == 0) {
            sendMessageToEntity(entity, "block.mio_icif.teleporter_elc.error.invalid_entity");
            return;
        }

        // 计算传送需要的能量
        int energyCost = calculateEnergyCost(weight, distance);

        // 检查能量是否足够
        if (energyStorage.getAmount() < energyCost) {
            sendMessageToEntity(entity, "block.mio_icif.teleporter_elc.error.insufficient_energy",
                String.format("%,d", energyCost), String.format("%,d", energyStorage.getAmount()));
            return; // 能量不足
        }

        // 消耗能量
        apiUseEnergy(energyCost, false);

        // 计算目标位置
        double targetX = targetPos.getX() + 0.5;
        double targetY = targetPos.getY() + 1.5;
        double targetZ = targetPos.getZ() + 0.5;

        if (entity instanceof ServerPlayer player) {
            player.teleportTo(targetX, targetY, targetZ);
            // 发送传送成功消息
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("block.mio_icif.teleporter_elc.success",
                    String.format("%,d", (int)distance), String.format("%,d", energyCost)),
                true);
        } else {
            // 非玩家实体直接设置位置，不会标记为重生传送
            entity.moveTo(targetX, targetY, targetZ);
            entity.setPos(targetX, targetY, targetZ);
        }

        // 通知目标传送机进入冷却
        if (level.getBlockEntity(targetPos) instanceof mio_icif_teleporter_elc targetTeleporter) {
            targetTeleporter.onTeleportFrom(this);
        }

        // 播放传送粒子效果
        spawnTeleportEffects(serverLevel, worldPosition);
        spawnTeleportEffects(serverLevel, targetPos);

        setChanged();
    }

    /**
     * 向实体发送消息（仅玩家）
     */
    private void sendMessageToEntity(Entity entity, String translationKey, Object... args) {
        if (entity instanceof ServerPlayer player) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(translationKey, args),
                true);
        }
    }

    /**
     * 当有其他传送机传送到此机器时调用
     */
    private void onTeleportFrom(mio_icif_teleporter_elc from) {
        this.cooldown = DEFAULT_COOLDOWN;
        setChanged();
    }

    /**
     * 播放传送粒子效果
     */
    private void spawnTeleportEffects(ServerLevel level, BlockPos pos) {
        // 播放传送声音
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
            net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);

        // 播放传送粒子
        for (int i = 0; i < 20; i++) {
            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0;
            double y = pos.getY() + 1.0 + level.random.nextDouble();
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0;

            level.sendParticles(net.minecraft.core.particles.DustParticleOptions.REDSTONE,
                x, y, z, 1, 0.0, 0.0, 1.0, 0.0);
        }
    }

    /**
     * 计算传送实体的重量
     */
    private int calculateWeight(Entity entity) {
        int weight = 0;

        if (entity instanceof ItemEntity itemEntity) {
            // 掉落物
            ItemStack stack = itemEntity.getItem();
            weight += 100 * stack.getCount() / stack.getMaxStackSize();
        } else if (entity instanceof Animal || entity instanceof Minecart || entity instanceof Boat) {
            // 生物和载具
            weight += 100;
        } else if (entity instanceof Player player) {
            // 玩家基础重量
            weight += 1000;

            // 盔甲重量
            for (ItemStack stack : player.getArmorSlots()) {
                if (!stack.isEmpty()) {
                    weight += 100;
                }
            }

            // 背包物品重量
            Container inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    // 每个物品组重量 = 100 * 物品组 / 最大堆叠数
                    weight += 100 * stack.getCount() / stack.getMaxStackSize();
                }
            }

            // 副手物品重量
            ItemStack offhand = player.getOffhandItem();
            if (!offhand.isEmpty()) {
                weight += 100 * offhand.getCount() / offhand.getMaxStackSize();
            }

            // 玩家重量最大5100
            weight = Math.min(weight, 5100);
        } else if (entity instanceof WitherBoss) {
            weight += 5000;
        } else if (entity instanceof EnderDragon) {
            weight += 10000;
        } else if (entity instanceof Monster) {
            // 怪物
            weight += 500;
        } else if (entity instanceof net.minecraft.world.entity.LivingEntity) {
            // 其他生物
            weight += 500;
        }

        // 计算所有乘客额外重量
        for (Entity passenger : entity.getPassengers()) {
            weight += calculateWeight(passenger);
        }

        return weight;
    }

    /**
     * 计算传送能量消耗
     * 计算公式 y = x * (l + 10)^0.7 * 5
     * x = 重量, l = 距离
     */
    private int calculateEnergyCost(int weight, double distance) {
        return (int) (weight * Math.pow(distance + 10.0, 0.7) * 5.0);
    }

    /**
     * 设置激活状态
     */
    private void setActive(boolean active) {
        if (this.isActive != active) {
            this.isActive = active;
            setChanged();
        }
    }

    /**
     * 获取目标位置
     */
    @Nullable
    public BlockPos getTargetPos() {
        return targetPos;
    }

    /**
     * 设置目标位置
     */
    public void setTargetPos(@Nullable BlockPos pos) {
        this.targetPos = pos;
        setChanged();
    }

    /**
     * 是否有目标
     */
    public boolean hasTarget() {
        return targetPos != null;
    }

    /**
     * 获取激活状态
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * 获取传送冷却时间
     */
    public int getCooldown() {
        return cooldown;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (targetPos != null) {
            tag.putInt("targetX", targetPos.getX());
            tag.putInt("targetY", targetPos.getY());
            tag.putInt("targetZ", targetPos.getZ());
        }
        tag.putInt("cooldown", cooldown);
        tag.putBoolean("isActive", isActive);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("targetX")) {
            targetPos = new BlockPos(
                tag.getInt("targetX"),
                tag.getInt("targetY"),
                tag.getInt("targetZ")
            );
        }
        cooldown = tag.getInt("cooldown");
        isActive = tag.getBoolean("isActive");
    }
}