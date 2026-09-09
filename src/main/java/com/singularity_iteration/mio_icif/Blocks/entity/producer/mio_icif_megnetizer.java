package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_producer;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Menu.Producer.MagnetizerMenu;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 磁化机方块实体类
 * 使附近铁栏杆可以使用户可以使用玩家吸引到梯子格子间等进行使用
 * 铁栏杆附近会吸引区域上方玩家穿戴金属靴格子的玩家向吸引
 */
@SuppressWarnings("null")
public class mio_icif_megnetizer extends mio_icif_producer {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .battery()
        .extra(4)
        .build();

    // 槽位总数 - 6个槽（输入槽 + 电池槽 + 4个盔甲展示槽）
    public static final int SLOT_COUNT = 6;
    // 输入槽索引：暂不使用，保留给producer超类用
    public static final int INPUT_SLOT = 0;
    // 电池槽索引
    public static final int BATTERY_SLOT = 1;
    // 盔甲展示槽起始索引
    public static final int ARMOR_SLOT_START = 2;
    // 头部展示槽
    public static final int HELMET_SLOT = 2;
    // 盔甲展示槽
    public static final int CHESTPLATE_SLOT = 3;
    // 护腿展示槽
    public static final int LEGGINGS_SLOT = 4;
    // 靴子格子展示槽
    public static final int BOOTS_SLOT = 5;

    // 默认配置
    public static final long DEFAULT_CAPACITY = 100L;      // 对比IC2磁化机
    public static final long DEFAULT_MAX_RECEIVE = 32L;     // LV级最大输入
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    // 每tick消耗的能量（EU）
    public static final long DEFAULT_ENERGY_PER_TICK = 5L;
    // 磁化范围：上下方向上20格
    public static final int MAGNETIZE_RANGE = 20;
    // 吸引玩家的水平距离范围3格，垂直范围20格
    public static final double ATTRACT_HORIZONTAL_RANGE = 3.0;
    public static final double ATTRACT_VERTICAL_RANGE = 20.0;
    // 吸引速度
    public static final double ATTRACT_SPEED = 0.15;

    // 已被磁化的铁栏杆集合（范围上下20格内）
    private final Set<BlockPos> magnetizedFences = new HashSet<>();

    public mio_icif_megnetizer(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.MAGNETIZER_ENTITY_TYPE.get());
    }

    public mio_icif_megnetizer(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            100, // maxProgress (磁化机不使用工作进度，设置为100)
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.magnetizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MagnetizerMenu(containerId, playerInventory, this);
    }

    /**
     * 检查槽位是否存放适合特定槽位
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> false; // 输入槽不使用
            case BATTERY_SLOT -> isBattery(stack); // 电池槽位
            case HELMET_SLOT, CHESTPLATE_SLOT, LEGGINGS_SLOT, BOOTS_SLOT -> false; // 盔甲展示槽位不接受任何物品
            default -> false;
        };
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
     * 检查指定槽位是否可以特定方向提取
     */
    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        // 只有电池槽可以提取
        return slot == BATTERY_SLOT;
    }

    /**
     * 检查机器是否可以工作
     */
    @Override
    protected boolean canWork() {
        // 磁化机只要有能量就可以工作
        return hasEnoughEnergy();
    }

    /**
     * 执行机器工作
     */
    @Override
    protected void doWork() {
        if (level == null || level.isClientSide()) return;

        // 执行磁化栏杆
        magnetizeFences();

        // 吸引玩家
        attractPlayers();
    }

    /**
     * 磁化附近铁栏杆
     * 使用广度优先搜索(BFS)从磁化机器开始搜索连接在一起的铁栏杆链
     * 连接的栏杆会被加入集合中供吸引系统使用，已被磁化的连接重复磁化的栏杆不会被重复添加
     */
    private void magnetizeFences() {
        if (level == null || level.isClientSide()) return;

        magnetizedFences.clear();
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        // 从磁化机器六个面开始搜索
        for (Direction direction : Direction.values()) {
            BlockPos startPos = worldPosition.relative(direction);
            if (isMagnetizableFence(startPos) && !visited.contains(startPos)) {
                // 检查起始位置是否在范围内
                if (Math.abs(startPos.getY() - worldPosition.getY()) <= MAGNETIZE_RANGE) {
                    queue.offer(startPos);
                    visited.add(startPos);
                }
            }
        }

        // BFS搜索连接的铁栏杆
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();

            // 添加到磁化集合
            magnetizedFences.add(currentPos);

            // 遍历四个方向的邻居
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(direction);

                // 检查是否未访问过且不是机器本身
                if (!visited.contains(neighborPos) && !neighborPos.equals(worldPosition)) {
                    // 检查邻居是否在垂直范围内
                    if (Math.abs(neighborPos.getY() - worldPosition.getY()) <= MAGNETIZE_RANGE) {
                        if (isMagnetizableFence(neighborPos)) {
                            queue.offer(neighborPos);
                            visited.add(neighborPos);
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查指定位置是否为可磁化铁栏杆
     */
    private boolean isMagnetizableFence(BlockPos pos) {
        if (level == null) return false;
        BlockState state = level.getBlockState(pos);
        // 检查是否原版铁栏杆
        if (state.is(Blocks.IRON_BARS)) return true;
        // 检查是否自定义铁栏杆
        if (state.is(mio_icif_blocks.BLOCK_FENCE_IRON.get())) return true;
        return false;
    }

    /**
     * 清除磁化栏杆集合
     */
    private void demagnetizeFences() {
        magnetizedFences.clear();
    }

    /**
     * 吸引穿戴金属靴子的玩家
     */
    private void attractPlayers() {
        if (level == null || level.isClientSide()) return;

        // 计算磁化栏杆集合的包围盒，扩大检测范围
        if (magnetizedFences.isEmpty()) return;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : magnetizedFences) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        // 扩大包围盒以检测范围内的玩家
        AABB attractBox = new AABB(
            minX - 2, minY - 2, minZ - 2,
            maxX + 3, maxY + 3, maxZ + 3
        );

        List<Player> players = level.getEntitiesOfClass(Player.class, attractBox);

        for (Player player : players) {
            // 检查玩家是否穿戴金属靴子
            if (hasMetalBoots(player)) {
                // 检查玩家是否在磁化栏杆内部
                if (isInsideMagnetizedFence(player)) {
                    // 向上吸引玩家
                    attractPlayerUp(player);
                }
            }
        }
    }

    /**
     * 检查玩家是否穿戴金属靴子
     */
    private boolean hasMetalBoots(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty()) return false;

        // 检查是否是可用盔甲类靴子
        if (boots.getItem() instanceof ArmorItem armorItem) {
            // 检查是否是脚部装备
            if (armorItem.getEquipmentSlot() == EquipmentSlot.FEET) {
                // 检查材质类型 - 使用 ArmorMaterials 类判断
                return armorItem.getMaterial() == net.minecraft.world.item.ArmorMaterials.IRON ||
                       armorItem.getMaterial() == net.minecraft.world.item.ArmorMaterials.GOLD ||
                       armorItem.getMaterial() == net.minecraft.world.item.ArmorMaterials.NETHERITE;
            }
        }

        // 检查是否是电动靴（IC2电动靴）
        if (getItemAPI().isElectricArmor(boots)) {
            return true;
        }

        return false;
    }

    /**
     * 检查玩家是否在磁化栏杆包围的方块内部
     * 玩家必须在磁化栏杆封闭的空间内才会被吸引
     */
    private boolean isInsideMagnetizedFence(Player player) {
        // 获取玩家的碰撞箱
        AABB playerBox = player.getBoundingBox();

        // 检查玩家是否与任何磁化栏杆方块碰撞
        for (BlockPos fencePos : magnetizedFences) {
            // 构建稍微扩大的栏杆AABB，允许边缘穿行但防止贴边穿行
            AABB fenceBox = new AABB(
                fencePos.getX() - 0.3, fencePos.getY() - 0.5, fencePos.getZ() - 0.3,
                fencePos.getX() + 1.3, fencePos.getY() + 1.5, fencePos.getZ() + 1.3
            );

            // 检查玩家是否与该栏杆碰撞
            if (playerBox.intersects(fenceBox)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 向上吸引玩家
     */
    private void attractPlayerUp(Player player) {
        // 给玩家一个向上的速度
        player.setDeltaMovement(player.getDeltaMovement().x, ATTRACT_SPEED, player.getDeltaMovement().z);
        player.hurtMarked = true; // 标记需要客户端同步
        player.fallDistance = 0; // 重置下落距离，防止摔落伤害
    }

    /**
     * 覆写电池判定方法
     */
    @Override
    protected boolean isBattery(ItemStack stack) {
        return getItemAPI().isBattery(stack) ||
               getItemAPI().isElectricArmor(stack) ||
               stack.getItem() == Items.REDSTONE;
    }

    /**
     * 获取磁化栏杆的位置集合（拷贝）
     */
    public Set<BlockPos> getMagnetizedFences() {
        return new HashSet<>(magnetizedFences);
    }

    /**
     * 每tick执行工作核心逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_megnetizer blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类tick方法处理升级和充放电等逻辑，如果不在工作状态则清除
        mio_icif_producer.tick(level, pos, state, blockEntity);

        // 检查当不在工作状态时，清除磁化
        if (!blockEntity.isWorking()) {
            blockEntity.demagnetizeFences();
        }
    }

    // ==================== 数据持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 保存磁化铁栏杆的位置集合
        int[] magnetizedX = new int[magnetizedFences.size()];
        int[] magnetizedY = new int[magnetizedFences.size()];
        int[] magnetizedZ = new int[magnetizedFences.size()];
        int i = 0;
        for (BlockPos pos : magnetizedFences) {
            magnetizedX[i] = pos.getX();
            magnetizedY[i] = pos.getY();
            magnetizedZ[i] = pos.getZ();
            i++;
        }
        tag.putIntArray("magnetized_x", magnetizedX);
        tag.putIntArray("magnetized_y", magnetizedY);
        tag.putIntArray("magnetized_z", magnetizedZ);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // 读取磁化铁栏杆位置集合
        magnetizedFences.clear();
        if (tag.contains("magnetized_x") && tag.contains("magnetized_y") && tag.contains("magnetized_z")) {
            int[] magnetizedX = tag.getIntArray("magnetized_x");
            int[] magnetizedY = tag.getIntArray("magnetized_y");
            int[] magnetizedZ = tag.getIntArray("magnetized_z");
            for (int i = 0; i < magnetizedX.length; i++) {
                magnetizedFences.add(new BlockPos(magnetizedX[i], magnetizedY[i], magnetizedZ[i]));
            }
        }
    }
}