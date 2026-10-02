package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_Geo_Generator;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 地热发电机方块实体类
 * 通过燃烧岩浆发电，每桶岩浆（1000mb）发电量10,000EU（10 EU/mB）
 * 槽位结构：1岩浆桶输入槽 + 1空桶输出槽 + 1电池充电槽 = 3
 * 岩浆槽容量：8000mb
 * 输出等级：LV，20 EU/t（32 EU/packet）
 * 周围岩浆方块额外发电：1 EU/t/面（SI 自有扩展，IC2 原版无此机制）
 *
 * <p>与原版 IC2 1.12.2 {@code TileEntityGeoGenerator}（{@code super(20.0D, 1, 2400)}、
 * 8000 mB 储罐、{@code fluidPerTick = 2}）的对应关系：2 mB/tick × 20 EU/t = 10 EU/mB，
 * 即每桶 1000 mB = 10,000 EU、持续 500 tick；管网输出上限 LV = 32 EU/packet。
 */
@SuppressWarnings("null")
public class mio_icif_geo_generator extends AbstractGeneratorBlockEntity {

    // 槽位数量：3个槽位
    public static final int SLOT_COUNT = 3;
    // 岩浆桶输入槽索引
    public static final int LAVA_BUCKET_SLOT = 0;
    // 空桶输出槽索引
    public static final int EMPTY_BUCKET_SLOT = 1;
    // 电池充电槽索引
    public static final int BATTERY_SLOT = 2;

    // 流体配置
    public static final int LAVA_CAPACITY = 8000; // 8000 mb = 8桶岩浆
    public static final int LAVA_PER_BUCKET = 1000; // 每桶岩浆1000mb
    public static final int ENERGY_PER_BUCKET = 10000; // 每桶岩浆发电10000EU

    // 发电速率 (EU/tick) - LV 级 20 EU/t
    public static final long ENERGY_GENERATION_RATE = 20L; // 20 EU/tick

    // 单位换算：10 EU/mB，等价于原版 2 mB/tick × 20 EU/t
    public static final int EU_PER_MB = ENERGY_PER_BUCKET / LAVA_PER_BUCKET;
    // 满速发电时每 tick 消耗的岩浆：20 EU/t ÷ 10 EU/mB = 2 mB/t，对应原版 fluidPerTick = 2
    public static final int LAVA_MB_PER_TICK = (int) (ENERGY_GENERATION_RATE / EU_PER_MB);

 // 电能容量 - LV 级
    public static final long ENERGY_CAPACITY = 2400L; // 2,400 EU
    public static final long MAX_RECEIVE = 0L; // 发电机不接受外部能量输入
    public static final long MAX_EXTRACT = 32L; // LV 级最大输出速率 32 EU/tick

    // 流体存储
    protected final FluidTank lavaTank;

    // 当前这批已取出、尚未发电的岩浆量（mb）
    private int currentLavaBurning = 0;
    // 当前这批岩浆已发出的电量的零头（不足 1 mB 的部分）
    // 记账关系：剩余可发电量 = currentLavaBurning * EU_PER_MB - currentLavaEnergyGenerated。
    // 两条 tick 路径（接管／未接管）共用这两个保存到 NBT 的字段，切换路径不会丢失余料。
    private long currentLavaEnergyGenerated = 0L;

    /**
     * 构造函数（用于 BlockEntityType.Builder）
     */
    public mio_icif_geo_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    /**
     * 构造函数
     */
    public mio_icif_geo_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.GEO_GENERATOR_ENTITY_TYPE.get(),
            SlotLayout.builder().extra(2).battery().build(), ENERGY_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, CableTier.LV);

        // 初始化岩浆存储，只接受岩浆
        this.lavaTank = new FluidTank(LAVA_CAPACITY, fluidStack ->
            fluidStack.getFluid() == Fluids.LAVA);
    }

    /**
     * 检查物品是否是岩浆桶或岩浆单元
     */
    private boolean isLavaBucket(ItemStack stack) {
        return stack.is(Items.LAVA_BUCKET) || mio_icif_cells.isCellContainingFluid(stack, Fluids.LAVA);
    }

    /**
     * 获取燃料的燃烧时间（地热发电机使用岩浆）
     *
     * <p>仅供父类查找式逻辑与外部判定「是否为可用燃料」；本类的 tick 不会用到这里返回的
     * 时长，实际燃烧时长由 {@code currentLavaBurning} 按 10 EU/mB 换算得到。
     *
     * @param fuel 燃料物品
     * @return 燃烧时间 (tick)，如果不是岩浆桶返回0
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        if (isLavaBucket(fuel)) {
            // 返回一个非 0 值表示可当燃料；实际燃烧时长由岩浆存量与发电速率换算
            return Integer.MAX_VALUE;
        }
        return 0;
    }

    /**
     * 处理岩浆桶输入槽
     * 将岩浆桶/单元中的岩浆转移到流体存储，空桶/空单元放入空桶输出槽
     */
    private void handleLavaBucketSlot() {
        ItemStack lavaBucketStack = itemHandler.getStackInSlot(LAVA_BUCKET_SLOT);
        if (lavaBucketStack.isEmpty() || !isLavaBucket(lavaBucketStack)) {
            return;
        }

        // 检查流体存储是否还有空位
        if (lavaTank.getFluidAmount() >= lavaTank.getCapacity()) {
            return;
        }

        // 判断输入是桶还是单元
        boolean isCell = mio_icif_cells.isFluidCell(lavaBucketStack);
        ItemStack emptyContainer = isCell ? mio_icif_cells.getEmptyCellForStack(lavaBucketStack) : new ItemStack(Items.BUCKET);
        if (isCell && emptyContainer.isEmpty()) emptyContainer = new ItemStack(mio_icif_cells.CELL_EMPTY.get());

        // 检查空桶输出槽是否可以容纳
        ItemStack emptyBucketStack = itemHandler.getStackInSlot(EMPTY_BUCKET_SLOT);
        if (!emptyBucketStack.isEmpty()) {
            // 检查槽位中是否已有相同类型的空容器，且未达到最大堆叠数
            if (!ItemStack.isSameItem(emptyBucketStack, emptyContainer) || emptyBucketStack.getCount() >= emptyBucketStack.getMaxStackSize()) {
                return;
            }
        }

        // 容器只能整桶转移；容量不足时不能先写入部分岩浆。
        FluidStack bucketFluid = new FluidStack(Fluids.LAVA, LAVA_PER_BUCKET);
        if (lavaTank.fill(bucketFluid, IFluidHandler.FluidAction.SIMULATE) != LAVA_PER_BUCKET) {
            return;
        }
        int filled = lavaTank.fill(bucketFluid, IFluidHandler.FluidAction.EXECUTE);
        if (filled >= LAVA_PER_BUCKET) {
            // 消耗岩浆桶/单元
            lavaBucketStack.shrink(1);
            // 添加空桶/空单元到空桶输出槽
            if (emptyBucketStack.isEmpty()) {
                itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, emptyContainer);
            } else {
                emptyBucketStack.grow(1);
            }
            setChanged();
        }
    }

    /**
     * 消耗岩浆进行发电
     * 重写父类方法，使用岩浆而不是物品燃烧
     *
     * <p>一次取出整桶（1000 mB）作为「当前这一份」待发电燃料，实际消耗量由
     * {@link #generateEnergy()} 按 10 EU/mB 记账，未发电的部分留在
     * {@code currentLavaBurning} 中；因此这里只在此前那份彻底用完时才被 tick 调用。
     */
    @Override
    protected void consumeFuel() {
        // 检查能量存储是否已满
        boolean isEnergyFull = getEnergyStorage().getAmount() >=
                              getEnergyStorage().getCapacity();

        // 如果能量已满，不消耗岩浆（对齐原版：needsFuel() 要求 freeEnergy >= production）
        if (isEnergyFull) {
            return;
        }

        // 检查是否有足够的岩浆（至少1000mb = 1桶）
        if (lavaTank.getFluidAmount() >= LAVA_PER_BUCKET) {
            // 消耗1000mb岩浆
            FluidStack drained = lavaTank.drain(LAVA_PER_BUCKET, IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() >= LAVA_PER_BUCKET) {
                // 开始燃烧这桶岩浆
                this.currentLavaBurning = LAVA_PER_BUCKET;
                this.currentLavaEnergyGenerated = 0;
                this.burnDuration = (int) (ENERGY_PER_BUCKET / energyGenerationRate); // 燃烧持续的tick数
                this.burnTime = this.burnDuration;
                setChanged();
            }
        }
    }

    // 基于周围岩浆方块的发电速率（每面每tick）
    private static final int LAVA_BLOCK_ENERGY_RATE = 1; // 每面每tick 1 EU

    // 当前接触的岩浆方块数量
    private int nearbyLavaBlocks = 0;

    /**
     * 生成能量
     * 重写父类方法，根据岩浆消耗计算发电量
     *
     * <p>仅当储能还有空间时才由 tick 调用（储能满时完全跳过），因此在这里被扣除的
     * 岩浆一定会换算成电能，不会产生「扣了燃料却没发电」的损耗。
     */
    @Override
    protected void generateEnergy() {
        long totalEnergyGenerated = 0L;
        
        // 1. 基于岩浆桶的发电
        if (currentLavaBurning > 0) {
            // 生成能量（每tick至多 20 EU；剩余空间不足时只发部分，差额留到后续 tick）
            long lavaBucketEnergy = Math.min(energyGenerationRate,
                getEnergyStorage().getCapacity() - getEnergyStorage().getAmount());
            
            if (lavaBucketEnergy > 0) {
                totalEnergyGenerated += lavaBucketEnergy;
                currentLavaEnergyGenerated += lavaBucketEnergy;
                
                // 按 10 EU/mB 折算扣除岩浆；不足 1 mB 的零头累计到下一次发电
                int mbToRemove = (int) Math.min((long) currentLavaBurning,
                    currentLavaEnergyGenerated / EU_PER_MB);
                if (mbToRemove > 0) {
                    currentLavaBurning -= mbToRemove;
                    currentLavaEnergyGenerated = currentLavaEnergyGenerated % EU_PER_MB;
                    
                    if (currentLavaBurning < 0) {
                        currentLavaBurning = 0;
                    }
                }
            }
        }
        
        // 2. 基于周围岩浆方块的发电
        if (nearbyLavaBlocks > 0) {
            long lavaBlockEnergy = Math.min((long) nearbyLavaBlocks * LAVA_BLOCK_ENERGY_RATE,
                Math.max(0L, getEnergyStorage().getCapacity() - getEnergyStorage().getAmount() - totalEnergyGenerated));
            
            if (lavaBlockEnergy > 0) {
                totalEnergyGenerated += lavaBlockEnergy;
            }
        }
        
        // 实际存储生成的能量（使用generateEnergyInternal绕过maxReceive限制）
        if (totalEnergyGenerated > 0) {
            apiGenerateEnergy(totalEnergyGenerated, false);
        }
    }

    /**
     * 给充电槽中的物品充电
     * 重写父类方法，使用 BATTERY_SLOT
     */
    @Override
    protected void chargeItems() {
        ItemStack chargeStack = itemHandler.getStackInSlot(BATTERY_SLOT);
        if (chargeStack.isEmpty()) {
            return;
        }

        // 检查物品是否是电池
        if (getItemAPI().isBattery(chargeStack)) {
            var api = getItemAPI();
            long currentEnergy = api.getBatteryStored(chargeStack);
            long batteryMaxEnergy = api.getBatteryCapacity(chargeStack);
            long batteryChargeRate = api.getChargeRate(chargeStack);

            if (currentEnergy >= batteryMaxEnergy) {
                return;
            }

            long availableEnergy = getEnergyStorage().getAmount();
            if (availableEnergy <= 0) {
                return;
            }

            long energyToCharge = Math.min(batteryChargeRate, batteryMaxEnergy - currentEnergy);
            energyToCharge = Math.min(energyToCharge, availableEnergy);
            energyToCharge = Math.min(energyToCharge, Math.max(0, api.chargeBattery(chargeStack, energyToCharge, true)));
            long energyExtracted = energyToCharge > 0 ? getEnergyStorageInternal().extract(energyToCharge, false) : 0;
            long energyStored = energyExtracted > 0 ? api.chargeBattery(chargeStack, energyExtracted, false) : 0;
            if (energyStored < energyExtracted) getEnergyStorageInternal().generateEnergyInternal(energyExtracted - energyStored, false);
            setChanged();
        }
    }

    /**
     * 每tick更新逻辑（未接管路径，交由 SI 自身电网与相邻方块输出）
     *
     * <p>执行顺序对齐原版 {@code TileEntityBaseGenerator.updateEntityServer()}：
     * 先用岩浆桶补液 → 尝试把能量分出去（电池、相邻方块）→ 判定储能是否还有空间 →
     * 只有还有空间才发电并消耗岩浆。先分配再判定，是为了避免内部缓存没能及时输出就停机。
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_geo_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        if (blockEntity.energyStorage.scexNetworkControlled()) {
            blockEntity.tickControlledGeothermal(level, pos);
            return;
        }

        // 检测周围的岩浆方块数量
        blockEntity.checkNearbyLavaBlocks(level, pos);

        // 记录之前的燃烧状态
        boolean wasBurning = blockEntity.isBurning();

        // 处理岩浆桶输入槽
        blockEntity.handleLavaBucketSlot();

        // 先给充电槽中的物品充电，再分配能量到相邻方块
        // 这样可以确保电池和外部电器同时得到能量
        blockEntity.chargeItems();
        
        // 只有当发电机需要直接向相邻方块输出能量时，才调用distributeEnergy()
        if (blockEntity.shouldDirectlyDistributeEnergy()) {
            blockEntity.distributeEnergy();
        }

        // 分配完能量后再检查能量存储是否已满
        // 只有在能量真正无法输出时（存储已满且无法分配），才停止燃烧
        boolean isEnergyFull = blockEntity.getEnergyStorage().getAmount() >=
                              blockEntity.getEnergyStorage().getCapacity();

        // 燃料推进与发电：对齐原版 TileEntityBaseGenerator.updateEntityServer() 的顺序
        // （先 needsFuel() → gainFuel()，再 gainEnergy()），以及原版的判定条件：
        // 只有储能还放得下这一次发电所需的空间时才取用并消耗岩浆。
        // 储能满时完全冻结 —— 不发电、不推进燃烧进度、也不丢弃已取出但尚未发电的岩浆。
        // （旧实现在储能满时让 burnTime 空转递减并在归零时清空 currentLavaBurning，
        //  会把已从储罐扣除的岩浆无声丢弃，与原版「满仓不烧燃料」不符。）
        if (!isEnergyFull) {
            // 上一份岩浆彻底用完（或从未取过）时才领下一份；仍有余料则继续使用，
            // 不覆盖尚未发电的余量。
            if (blockEntity.currentLavaBurning <= 0) {
                blockEntity.consumeFuel();
            }
            // 领到岩浆的同一 tick 即开始发电（包括邻接岩浆方块的额外发电）
            blockEntity.generateEnergy();
            // UI 与方块状态使用的燃烧计时器直接由真实燃料余量派生，满速时为 500 tick，
            // 避免 burnTime 倒计时与岩浆实际扣除两套账脱节后提前补给燃料。
            blockEntity.burnTime = blockEntity.currentLavaBurning > 0
                ? (blockEntity.currentLavaBurning + LAVA_MB_PER_TICK - 1) / LAVA_MB_PER_TICK
                : 0;
        }

        // 检查状态是否改变：燃着（有正在发电的岩浆）或邻接岩浆在供电时都视为激活
        // （储能满、燃烧被冻结时这里仍为 true，这是 SI 原有行为，原版此时会转为 inactive）
        boolean isBurning = blockEntity.isBurning() || blockEntity.nearbyLavaBlocks > 0;
        if (wasBurning != isBurning) {
            // 更新方块状态
            BlockState newState = level.getBlockState(pos);
            // 使用 ACTIVE 属性更新状态
            if (newState.hasProperty(mio_icif_Block_Geo_Generator.ACTIVE)) {
                newState = newState.setValue(mio_icif_Block_Geo_Generator.ACTIVE, isBurning);
                level.setBlock(pos, newState, 3);
            }
        }

        // 标记方块实体已更新
        blockEntity.setChanged();
    }

    /**
     * 接管（独立电网）路径 Public-game R28 contract：2 mB pays for a complete 20 EU step.
     *
     * <p>每个自然 tick 在储能还剩得下完整 20 EU、且燃料足够时消耗 2 mB 岩浆并生成 20 EU，
     * 等价于原版 {@code TileEntityGeoGenerator.fluidPerTick = 2} 配 {@code production = 20}。
     * 余料不足一次发电时保留（{@code currentLavaBurning} / {@code currentLavaEnergyGenerated} 记账），
     * 不随等待时间过期；桶在本 tick 发电后才注入，下一 tick 起可用于发电。
     *
     * <p>注意：本方法中的邻接岩浆方块发电与 {@code SCEX-R28.md} 记录的
     * 「开启接管时相邻世界熔岩不发电」不一致，属于后续改动。若要回到 R28 契约，
     * 删除 {@code checkNearbyLavaBlocks()} 调用与下方 {@code nearbyLavaBlocks} 发电段，
     * 并让 {@code isActive} 仅等于 {@code generated} 即可。
     */
    private void tickControlledGeothermal(Level level, BlockPos pos) {
        chargeItems();
        checkNearbyLavaBlocks(level, pos);
        boolean generated = false;
        if (energyStorage.scexExactAmount().roomBelow(energyStorage.getCapacity()).whole() >= ENERGY_GENERATION_RATE) {
            // 旧存档里可能残留已扣除但未发电的岩浆，这里先用它的剩余能量抵扣本 tick 的需求
            long credit = Math.max(0L, (long) EU_PER_MB * currentLavaBurning - currentLavaEnergyGenerated);
            // 满速时换算结果恒为 2 mB；向上取整以保留不足 1 mB 的尾数
            int needed = (int) ((Math.max(0L, ENERGY_GENERATION_RATE - credit) + (EU_PER_MB - 1L)) / EU_PER_MB);
            if (lavaTank.getFluidAmount() >= needed) {
                if (needed > 0) lavaTank.drain(needed, IFluidHandler.FluidAction.EXECUTE);
                credit += (long) needed * EU_PER_MB - ENERGY_GENERATION_RATE;
                currentLavaBurning = (int) ((credit + (EU_PER_MB - 1L)) / EU_PER_MB);
                currentLavaEnergyGenerated = (long) currentLavaBurning * EU_PER_MB - credit;
                apiGenerateEnergy(ENERGY_GENERATION_RATE, false);
                generated = true;
            }
        }
        // 邻接岩浆方块额外发电（SI 扩展，见本方法 Javadoc 中与 R28 契约的差异说明）
        if (nearbyLavaBlocks > 0) {
            long room = Math.max(0L, energyStorage.getCapacity() - energyStorage.getAmount());
            long lavaBlockEnergy = Math.min((long) nearbyLavaBlocks * LAVA_BLOCK_ENERGY_RATE, room);
            if (lavaBlockEnergy > 0) {
                apiGenerateEnergy(lavaBlockEnergy, false);
                generated = true;
            }
        }
        handleLavaBucketSlot();
        burnDuration = 1;
        burnTime = generated ? 1 : 0;
        boolean isActive = generated || nearbyLavaBlocks > 0;
        BlockState current = level.getBlockState(pos);
        if (current.hasProperty(mio_icif_Block_Geo_Generator.ACTIVE)
            && current.getValue(mio_icif_Block_Geo_Generator.ACTIVE) != isActive) {
            level.setBlock(pos, current.setValue(mio_icif_Block_Geo_Generator.ACTIVE, isActive), 3);
        }
        setChanged();
    }

    /**
     * 检测周围的岩浆方块数量
     */
    private void checkNearbyLavaBlocks(Level level, BlockPos pos) {
        int count = 0;
        // 检查六个方向的方块
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            // 检查是否是岩浆方块（包括流动岩浆）
            if (adjacentState.getBlock() == Blocks.LAVA) {
                count++;
            }
        }
        this.nearbyLavaBlocks = count;
    }

    // ==================== WorldlyContainer 接口实现 ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        // 所有方向都可以访问所有槽位
        return new int[]{LAVA_BUCKET_SLOT, EMPTY_BUCKET_SLOT, BATTERY_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // 岩浆桶输入槽：只接受岩浆桶
        if (slot == LAVA_BUCKET_SLOT) {
            return isLavaBucket(stack);
        }
        // 空桶输出槽：不允许放入
        if (slot == EMPTY_BUCKET_SLOT) {
            return false;
        }
        // 电池槽：只接受电池类物品
        if (slot == BATTERY_SLOT) {
            return isBattery(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // 空桶输出槽可以提取
        if (slot == EMPTY_BUCKET_SLOT) {
            return true;
        }
        // 电池槽可以提取（电池充满后可以取出）
        if (slot == BATTERY_SLOT) {
            return true;
        }
        // 岩浆桶输入槽可以提取（允许玩家取回未使用的岩浆桶）
        if (slot == LAVA_BUCKET_SLOT) {
            return true;
        }
        return false;
    }

    // ==================== 流体处理 ====================

    /**
     * 获取岩浆处理器
     */
    public IFluidHandler getLavaHandler() {
        return lavaTank;
    }

    /**
     * 获取岩浆处理器（用于特定方向）
     */
    @Nullable
    public IFluidHandler getLavaHandlerCapability(@Nullable Direction side) {
        return lavaTank;
    }

    /**
     * 获取当前岩浆储量（mb）
     */
    public int getLavaAmount() {
        return lavaTank.getFluidAmount();
    }

    /**
     * 获取最大岩浆容量（mb）
     */
    public int getLavaCapacity() {
        return lavaTank.getCapacity();
    }

    /**
     * 获取当前岩浆
     */
    public FluidStack getLava() {
        return lavaTank.getFluid();
    }

    /**
     * 获取岩浆储量百分比（用于GUI显示）
     */
    public int getLavaProgress() {
        if (lavaTank.getCapacity() <= 0) {
            return 0;
        }
        return (lavaTank.getFluidAmount() * 100) / lavaTank.getCapacity();
    }

    /**
     * 获取岩浆桶数量（用于GUI显示）
     */
    public int getLavaBuckets() {
        return lavaTank.getFluidAmount() / LAVA_PER_BUCKET;
    }

    // ==================== NBT 数据保存 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 保存岩浆数据
        tag.put("LavaTank", lavaTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CurrentLavaBurning", currentLavaBurning);
        tag.putLong("CurrentLavaEnergyGenerated", currentLavaEnergyGenerated);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 加载岩浆数据
        if (tag.contains("LavaTank")) {
            lavaTank.readFromNBT(registries, tag.getCompound("LavaTank"));
        }
        currentLavaBurning = tag.getInt("CurrentLavaBurning");
        currentLavaEnergyGenerated = tag.contains("CurrentLavaEnergyGenerated", Tag.TAG_LONG)
            ? tag.getLong("CurrentLavaEnergyGenerated")
            : tag.getInt("CurrentLavaEnergyGenerated");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("LavaTank", lavaTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CurrentLavaBurning", currentLavaBurning);
        tag.putLong("CurrentLavaEnergyGenerated", currentLavaEnergyGenerated);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("LavaTank")) {
            lavaTank.readFromNBT(registries, tag.getCompound("LavaTank"));
        }
        currentLavaBurning = tag.getInt("CurrentLavaBurning");
        currentLavaEnergyGenerated = tag.contains("CurrentLavaEnergyGenerated", Tag.TAG_LONG)
            ? tag.getLong("CurrentLavaEnergyGenerated")
            : tag.getInt("CurrentLavaEnergyGenerated");
    }

    // ==================== MenuProvider 接口实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.geo_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.GeoGeneratorMenu(containerId, playerInventory, this, this.getItemHandler(), null);
    }
}