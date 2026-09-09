package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_tesla;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.event.mio_icif_DamageTypes;
import com.singularity_iteration.mio_icif.util.RadiationProtectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 特斯拉线圈方块实体类
 * 激活红石信号后启动，使用电力对区域内实体造成伤害并破坏盔甲
 * 当储备库能量达到5000EU以上时正常工作
 * 每20 ticks（1秒）对范围内实体造成一次攻击，该线圈第一击造成24点伤害
 * 对范围内目标第二次攻击只造成10点~11点伤害
 * 最大输入电压为128EU/t (MV等级)
 */
@SuppressWarnings("null")
public class mio_icif_tesla extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .battery()
        .build();

    // 槽位总数：只有电池槽
    public static final int SLOT_COUNT = 1;
    // 电池槽索引
    public static final int BATTERY_SLOT = 0;

    // 默认配置
    public static final long DEFAULT_CAPACITY = 10000L;  // 最大能量10000EU
    public static final long DEFAULT_MAX_RECEIVE = 128L; // 最大输入128EU/t (MV等级)
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    // 每次攻击消耗的能量
    public static final long ENERGY_PER_ATTACK = 500L;
    // 最小工作能量
    public static final long MIN_WORKING_ENERGY = 5000L;
    // 攻击范围半径
    public static final double ATTACK_RANGE = 9.0;
    // 首次攻击伤害
    public static final float FIRST_ATTACK_DAMAGE = 24.0F;
    // 后续攻击伤害最小值
    
    public static final float FOLLOW_UP_DAMAGE_MIN = 10.0F;
    // 后续攻击伤害最大值
    
    public static final float FOLLOW_UP_DAMAGE_MAX = 11.0F;
    // 盔甲损坏值最小值
    
    public static final int ARMOR_DAMAGE_MIN = 4;
    // 盔甲损坏值最大值
    
    public static final int ARMOR_DAMAGE_MAX = 6;
    // 工作间隔时间tick数，每秒攻击一次
    
    public static final int WORK_INTERVAL = 20;

    // 记录上次被攻击的实体UUID集合
    private final Set<UUID> lastAttackedEntities = new HashSet<>();
    // 工作计时器
    
    private int workTimer = 0;

    public mio_icif_tesla(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.TESLA_ENTITY_TYPE.get());
    }

    public mio_icif_tesla(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            100, // maxProgress (特斯拉线圈不使用工作进度系统
            LAYOUT,
            ENERGY_PER_ATTACK,
            CableTier.MV); // MV等级，最大输入128EU/t
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.tesla");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // 特斯拉线圈没有GUI界面
        return null;
    }

    /**
     * 检查槽位是否存放适合特定槽位
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        // 只有电池槽位可以放电力物品
        return slot == BATTERY_SLOT && isBattery(stack);
    }

    /**
     * 获取特定方向可访问的槽位
     */
    @Override
    protected int[] getSlotsForDirection(Direction side) {
        // 任何方向都只能访问电池槽
        return new int[]{BATTERY_SLOT};
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    /**
     * 检查机器是否可以工作
     */
    @Override
    protected boolean canWork() {
        return getEnergyStorage().getAmount() >= MIN_WORKING_ENERGY;
    }

    /**
     * 执行机器工作
     */
    @Override
    protected void doWork() {
        // 特斯拉线圈不消耗进度工作模式，在tick中执行
    }

    /**
     * 每tick执行工作核心逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_tesla blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类tick方法处理升级和充放电等逻辑
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 检测红石信号和激活状态
        boolean hasRedstoneSignal = level.hasNeighborSignal(pos);
        boolean isActive = state.getValue(mio_icif_block_tesla.ACTIVE);

        // 同步激活状态
        boolean shouldBeActive = hasRedstoneSignal && blockEntity.getEnergyStorage().getAmount() >= MIN_WORKING_ENERGY;
        if (shouldBeActive != isActive) {
            level.setBlock(pos, state.setValue(mio_icif_block_tesla.ACTIVE, shouldBeActive), 3);
        }

        // 如果没有红石信号或能量不足，停止工作
        if (!hasRedstoneSignal || blockEntity.getEnergyStorage().getAmount() < MIN_WORKING_ENERGY) {
            blockEntity.lastAttackedEntities.clear();
            return;
        }

        // 工作计时器递增
        blockEntity.workTimer++;
        if (blockEntity.workTimer < WORK_INTERVAL) {
            return;
        }
        blockEntity.workTimer = 0;

        // 执行攻击
        blockEntity.performAttack(level, pos);
    }

    /**
     * 执行电击攻击
     */
    private void performAttack(Level level, BlockPos pos) {
        // 检查能量是否足够
        if (getEnergyStorage().getAmount() < ENERGY_PER_ATTACK) {
            return;
        }

        // 计算攻击范围的AABB
        AABB attackArea = new AABB(
            pos.getX() - ATTACK_RANGE, pos.getY() - ATTACK_RANGE, pos.getZ() - ATTACK_RANGE,
            pos.getX() + ATTACK_RANGE + 1, pos.getY() + ATTACK_RANGE + 1, pos.getZ() + ATTACK_RANGE + 1
        );

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, attackArea);
        
        // 记录本次攻击的实体UUID
        Set<UUID> currentAttackedEntities = new HashSet<>();

        for (LivingEntity entity : entities) {
            // 跳过死亡的实体
            if (!entity.isAlive()) {
                continue;
            }

            UUID entityId = entity.getUUID();
            
            // 检查是否是首次攻击
            boolean isFirstAttack = !lastAttackedEntities.contains(entityId);
            
            // 计算伤害
            float damage;
            if (isFirstAttack) {
                damage = FIRST_ATTACK_DAMAGE;
            } else {
                damage = FOLLOW_UP_DAMAGE_MIN + level.random.nextFloat() * (FOLLOW_UP_DAMAGE_MAX - FOLLOW_UP_DAMAGE_MIN);
            }

            // 检查防化服 - 穿全套防化服的免疫伤害
            if (RadiationProtectionUtil.isWearingFullHazmat(entity)) {
                continue; // 防化服免疫，不造成伤害
            }

            // 创建固定位置的伤害源 - 使用特斯拉线圈特定的伤害类型
            Vec3 damagePos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            DamageSource damageSource = new DamageSource(
                level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(mio_icif_DamageTypes.TESLA_COIL),
                damagePos
            );

            // 对实体造成伤害
            entity.hurt(damageSource, damage);

            // 对盔甲造成额外损坏，即使是穿全套防化服 also
            damageArmor(entity, level);

            // 记录本次攻击的实体UUID
            currentAttackedEntities.add(entityId);
        }

        // 更新上次攻击记录
        lastAttackedEntities.clear();
        lastAttackedEntities.addAll(currentAttackedEntities);

        // 只有实际攻击到实体时才消耗能量
        if (!currentAttackedEntities.isEmpty()) {
            getEnergyStorageInternal().extract(ENERGY_PER_ATTACK, false);
        }
    }

    /**
     * 对穿全套防化服的实体盔甲造成额外损失
     */
    private void damageArmor(LivingEntity entity, Level level) {
        if (!(entity instanceof Player player)) {
            return;
        }

        // 计算盔甲损坏值
        int armorDamage = ARMOR_DAMAGE_MIN + level.random.nextInt(ARMOR_DAMAGE_MAX - ARMOR_DAMAGE_MIN + 1);

        // 对四件盔甲分别造成损坏
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armorStack = player.getItemBySlot(slot);
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof ArmorItem) {
                armorStack.hurtAndBreak(armorDamage, player, slot);
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("WorkTimer", workTimer);
        
        // 保存上次攻击的实体UUID
        CompoundTag attackedTag = new CompoundTag();
        int i = 0;
        for (UUID uuid : lastAttackedEntities) {
            attackedTag.putUUID("Entity" + i, uuid);
            i++;
        }
        attackedTag.putInt("Count", i);
        tag.put("LastAttacked", attackedTag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workTimer = tag.getInt("WorkTimer");
        
        // 读取上次攻击的实体UUID
        lastAttackedEntities.clear();
        if (tag.contains("LastAttacked")) {
            CompoundTag attackedTag = tag.getCompound("LastAttacked");
            int count = attackedTag.getInt("Count");
            for (int i = 0; i < count; i++) {
                if (attackedTag.hasUUID("Entity" + i)) {
                    lastAttackedEntities.add(attackedTag.getUUID("Entity" + i));
                }
            }
        }
    }
}