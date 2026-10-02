package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_Semifluid_Generator;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 半流质发电机方块实体
 * 通过燃烧半流质燃料（沼气）发电
 * 槽位结构：1燃料桶输入槽 + 1空桶输出槽 + 1电池充电槽 = 3
 * 燃料槽容量：10000mb
 * 输出等级：LV，发电 16 EU/t
 * 数值对齐原版 IC2 半流质发电机（沼气 32 EU/mB、输出 16 EU/t、内部储能 32000 EU）
 */
@SuppressWarnings("null")
public class mio_icif_Semifluid_generator extends AbstractGeneratorBlockEntity {

    // 槽位数量：3个槽位
    public static final int SLOT_COUNT = 3;
    // 燃料桶输入槽索引
    public static final int FUEL_BUCKET_SLOT = 0;
    // 空桶输出槽索引
    public static final int EMPTY_BUCKET_SLOT = 1;
    // 电池充电槽索引
    public static final int BATTERY_SLOT = 2;

    // 流体配置（对齐原版 IC2 半流质发电机）
    public static final int FUEL_CAPACITY = 10000; // 燃料槽 10000 mB（10 桶）
    public static final int FUEL_PER_BUCKET = 1000; // 每桶 1000 mB
    public static final long ENERGY_PER_BUCKET = 32000L; // 沼气 32 EU/mB → 每桶 32000 EU

    // 发电速率 (EU/tick) - 与原版 IC2 沼气一致 16 EU/t
    public static final long ENERGY_GENERATION_RATE = 16L; // 16 EU/tick

    // 电能容量 - 对齐原版 IC2 半流质发电机内部储能 32000 EU
    public static final long ENERGY_CAPACITY = 32000L;
    public static final long MAX_RECEIVE = 0L; // 发电机不接受外部能量输入
    public static final long MAX_EXTRACT = 32L; // 输出等级 LV，原版电网上限 32 EU/tick

    // 流体存储
    protected final FluidTank fuelTank;
    private long scexFuelCredit;

    // 当前正在发电的燃料量（mb）
    private int currentFuelBurning = 0;
    // 当前燃料已发电量
    private int currentFuelEnergyGenerated = 0;

    /**
     * 构造函数（用于 BlockEntityType.Builder）
     */
    public mio_icif_Semifluid_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    /**
     * 构造函数
     */
    public mio_icif_Semifluid_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            SlotLayout.builder().extra(2).battery().build(), ENERGY_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, CableTier.LV);

        // 初始化燃料存储，接受全部半流质燃料（沼气/生物质/原油/柴油）
        this.fuelTank = new FluidTank(FUEL_CAPACITY, fluidStack ->
            SemifluidFuels.accepts(fluidStack.getFluid())) {
            @Override protected void onContentsChanged() {
                if (energyStorage.scexNetworkControlled()) com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.markUnsaved(mio_icif_Semifluid_generator.this);
                else mio_icif_Semifluid_generator.this.setChanged();
            }
        };
    }

    /**
     * 检查物品是否是任意半流质燃料桶或燃料单元。
     */
    private boolean isFuelBucket(ItemStack stack) {
        return fuelContentOf(stack) != null;
    }

    /**
     * 取出容器内的半流质燃料内容（整桶/整单元），非燃料容器返回 null。
     */
    @Nullable
    private FluidStack fuelContentOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (mio_icif_cells.isFluidCell(stack)) {
            FluidStack content = mio_icif_cells.getCellFluid(stack);
            int capacity = mio_icif_cells.getCellCapacity(stack);
            if (content.isEmpty() || capacity <= 0 || content.getAmount() < capacity
                    || !SemifluidFuels.accepts(content.getFluid())) return null;
            return content.copyWithAmount(capacity);
        }
        if (stack.getItem() instanceof BucketItem bucket && SemifluidFuels.accepts(bucket.content)) {
            return new FluidStack(bucket.content, FUEL_PER_BUCKET);
        }
        return null;
    }

    /** 排空后返回的空容器物品。 */
    private static ItemStack emptyContainerFor(ItemStack stack) {
        if (mio_icif_cells.isFluidCell(stack)) {
            ItemStack empty = mio_icif_cells.getEmptyCellForStack(stack);
            return empty.isEmpty() ? new ItemStack(mio_icif_cells.CELL_EMPTY.get()) : empty;
        }
        return new ItemStack(Items.BUCKET);
    }

    /**
     * 获取燃料的燃烧时间
     * @param fuel 燃料物品
     * @return 燃烧时间 (tick)，如果不是燃料桶返回0
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        if (isFuelBucket(fuel)) {
            // 返回一个较大的值，实际燃烧时间由燃料量控制
            return Integer.MAX_VALUE;
        }
        return 0;
    }

    /**
     * 处理燃料桶输入槽
     * 将燃料桶/单元中的燃料转移到流体存储，空桶/空单元放入空桶输出槽
     */
    private void handleFuelBucketSlot() {
        if (energyStorage.scexNetworkControlled()) { scexFillFuelContainer(); return; }
        ItemStack fuelBucketStack = itemHandler.getStackInSlot(FUEL_BUCKET_SLOT);
        FluidStack content = fuelContentOf(fuelBucketStack);
        if (content == null) {
            return;
        }

        // 检查流体存储是否还有空位
        if (fuelTank.getFluidAmount() >= fuelTank.getCapacity()) {
            return;
        }

        // 判断输入是桶还是单元
        ItemStack emptyContainer = emptyContainerFor(fuelBucketStack);

        // 检查空桶输出槽是否可以容纳
        ItemStack emptyBucketStack = itemHandler.getStackInSlot(EMPTY_BUCKET_SLOT);
        if (!emptyBucketStack.isEmpty()) {
            // 检查槽位中是否已有相同类型的空容器，且未达到最大堆叠数
            if (!ItemStack.isSameItem(emptyBucketStack, emptyContainer) || emptyBucketStack.getCount() >= emptyBucketStack.getMaxStackSize()) {
                return;
            }
        }

        // 转移燃料：按容器实际内容（整桶/整单元 = 1000 mB）
        int filled = fuelTank.fill(content, IFluidHandler.FluidAction.EXECUTE);
        if (filled >= content.getAmount()) {
            // 消耗燃料桶/单元
            fuelBucketStack.shrink(1);
            // 添加空桶/空单元到空桶输出槽
            if (emptyBucketStack.isEmpty()) {
                itemHandler.setStackInSlot(EMPTY_BUCKET_SLOT, emptyContainer);
            } else {
                emptyBucketStack.grow(1);
            }
            setChanged();
            // 通知客户端更新方块实体数据
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    /**
     * 消耗燃料进行发电
     * 重写父类方法，使用燃料而不是物品燃烧
     */
    /** 燃料不走物品燃烧路径，消耗逻辑见 {@link #legacyTickFuel} / {@link #scexTickFuel}。 */
    @Override
    protected void consumeFuel() {
    }

    @Override
    protected void generateEnergy() {
    }

    /**
     * 旧（非独立电网）路径：对齐 IC2 连续耗液——每 tick 按需消耗 mB，
     * 内部储能满时暂停燃烧且不丢弃燃料（余量保留在 {@code scexFuelCredit}）。
     */
    private void legacyTickFuel(Level level, BlockPos pos) {
        boolean wasBurning = burnTime > 0;
        handleFuelBucketSlot();
        chargeItems();
        if (shouldDirectlyDistributeEnergy()) {
            distributeEnergy();
        } else {
            distributeEnergyToCompatSinks();
        }

        long room = energyStorage.getCapacity() - energyStorage.getAmount();
        FluidStack fuel = fuelTank.getFluid();
        SemifluidFuels.GeneratorValue value = fuel.isEmpty() ? null : SemifluidFuels.generatorValue(fuel.getFluid());
        long generated = 0;
        if (room > 0 && value != null) {
            var plan = com.miophas.singularity_iteration.core.runtime.energy.engine.ConsumableGeneration.plan(
                fuelTank.getFluidAmount(), scexFuelCredit, value.energyPerMb(), value.energyPerTick(), room, 0);
            if (plan.fuelConsumed() > 0) {
                fuelTank.drain((int) plan.fuelConsumed(), IFluidHandler.FluidAction.EXECUTE);
            }
            scexFuelCredit = plan.bufferedEnergy();
            generated = plan.generated();
            if (generated > 0) {
                apiGenerateEnergy(generated, false);
            }
        }
        burnTime = generated > 0 ? 1 : 0;
        burnDuration = 1;

        boolean isBurning = burnTime > 0;
        if (wasBurning != isBurning) {
            BlockState newState = level.getBlockState(pos);
            if (newState.hasProperty(mio_icif_Block_Semifluid_Generator.ACTIVE)) {
                level.setBlock(pos, newState.setValue(mio_icif_Block_Semifluid_Generator.ACTIVE, isBurning), 3);
            }
        }
        setChanged();
    }

    /**
     * 给充电槽中的物品充电
     * 重写父类方法，使用 BATTERY_SLOT
     */
    @Override
    protected void chargeItems() {
        if (energyStorage.scexNetworkControlled()) {
            if (com.miophas.singularity_iteration.core.runtime.energy.SolarItemCharging.chargeRange(itemHandler, BATTERY_SLOT, 1, energyStorage, getItemAPI())) setChanged();
            return;
        }
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
     * 每tick更新逻辑。
     * 独立电网路径与旧路径都改为 IC2 的连续耗液模型：储能满时暂停燃烧且不丢弃燃料。
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_Semifluid_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        if (blockEntity.energyStorage.scexNetworkControlled()) { blockEntity.scexTickFuel(level, pos); return; }
        blockEntity.legacyTickFuel(level, pos);
    }

    // ==================== WorldlyContainer 接口实现 ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        // 所有方向都可以访问所有槽位
        return new int[]{FUEL_BUCKET_SLOT, EMPTY_BUCKET_SLOT, BATTERY_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // 燃料桶输入槽：只接受燃料桶
        if (slot == FUEL_BUCKET_SLOT) {
            return isFuelBucket(stack);
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
        // 燃料桶输入槽可以提取（允许玩家取回未使用的燃料桶）
        if (slot == FUEL_BUCKET_SLOT) {
            return true;
        }
        return false;
    }

    // ==================== 流体处理 ====================

    /**
     * 获取燃料处理器
     */
    public IFluidHandler getFuelHandler() {
        return fuelTank;
    }

    /**
     * 获取燃料处理器（用于特定方向）
     */
    @Nullable
    public IFluidHandler getFuelHandlerCapability(@Nullable Direction side) {
        return fuelTank;
    }

    /**
     * 获取当前燃料储量（mb）
     */
    public int getFuelAmount() {
        return fuelTank.getFluidAmount();
    }

    /**
     * 获取最大燃料容量（mb）
     */
    public int getFuelCapacity() {
        return fuelTank.getCapacity();
    }

    /**
     * 获取当前燃料
     */
    public FluidStack getFuel() {
        return fuelTank.getFluid();
    }

    /**
     * 获取燃料储量百分比（用于GUI显示）
     */
    public int getFuelProgress() {
        if (fuelTank.getCapacity() <= 0) {
            return 0;
        }
        return (fuelTank.getFluidAmount() * 100) / fuelTank.getCapacity();
    }

    /**
     * 获取燃料桶数量（用于GUI显示)
     */
    public int getFuelBuckets() {
        return fuelTank.getFluidAmount() / FUEL_PER_BUCKET;
    }

    // ==================== NBT 数据保存 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (energyStorage.scexNetworkControlled() || scexFuelCredit > 0) tag.putLong("scex_fuel_credit_eu", scexFuelCredit);
        // 保存燃料数据
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CurrentFuelBurning", currentFuelBurning);
        tag.putInt("CurrentFuelEnergyGenerated", currentFuelEnergyGenerated);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 加载燃料数据
        if (tag.contains("FuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        }
        currentFuelBurning = tag.getInt("CurrentFuelBurning");
        currentFuelEnergyGenerated = tag.getInt("CurrentFuelEnergyGenerated");
        scexReadFuelCredit(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (energyStorage.scexNetworkControlled() || scexFuelCredit > 0) tag.putLong("scex_fuel_credit_eu", scexFuelCredit);
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("CurrentFuelBurning", currentFuelBurning);
        tag.putInt("CurrentFuelEnergyGenerated", currentFuelEnergyGenerated);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("FuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        }
        currentFuelBurning = tag.getInt("CurrentFuelBurning");
        currentFuelEnergyGenerated = tag.getInt("CurrentFuelEnergyGenerated");
        scexReadFuelCredit(tag);
    }

    // ==================== MenuProvider 接口实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.semifluid_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.SemifluidGeneratorMenu(containerId, playerInventory, this, this.getItemHandler(), null);
    }

    // ==================== 流体能力注册 ====================

    /**
     * 注册流体能力
     */
    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            mio_icif_block_entities.SEMIFLUID_GENERATOR_ENTITY_TYPE.get(),
            (be, side) -> be.getFuelHandlerCapability(side)
        );
    }

    private void scexFillFuelContainer() {
        var input = itemHandler.getStackInSlot(FUEL_BUCKET_SLOT);
        FluidStack contents = fuelContentOf(input);
        if (contents == null) return;
        ItemStack empty = emptyContainerFor(input);
        if (empty.isEmpty()) return;
        if (com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank.transfer(itemHandler, FUEL_BUCKET_SLOT, EMPTY_BUCKET_SLOT, fuelTank, contents, empty)) setChanged();
    }

    private void scexTickFuel(Level level, BlockPos pos) {
        boolean wasBurning = burnTime > 0;
        handleFuelBucketSlot();
        chargeItems();
        FluidStack fuel = fuelTank.getFluid();
        SemifluidFuels.GeneratorValue value = fuel.isEmpty() ? null : SemifluidFuels.generatorValue(fuel.getFluid());
        if (value != null) {
            var step = com.miophas.singularity_iteration.core.runtime.energy.FluidFuelGeneration.tick(fuelTank, energyStorage,
                scexFuelCredit, 1, value.energyPerMb(), value.energyPerTick());
            scexFuelCredit = step.bufferedEnergy();
            burnTime = step.generated() > 0 ? 1 : 0;
        } else {
            burnTime = 0;
        }
        burnDuration = 1;
        if (wasBurning != (burnTime > 0)) {
            var state = level.getBlockState(pos);
            if (state.hasProperty(mio_icif_Block_Semifluid_Generator.ACTIVE)) level.setBlock(pos, state.setValue(mio_icif_Block_Semifluid_Generator.ACTIVE, burnTime > 0), 3);
        }
        setChanged();
    }

    private void scexReadFuelCredit(CompoundTag tag) {
        if (tag.contains("scex_fuel_credit_eu", net.minecraft.nbt.Tag.TAG_LONG)) {
            scexFuelCredit = Math.clamp(tag.getLong("scex_fuel_credit_eu"), 0L, ENERGY_PER_BUCKET);
        } else if (energyStorage.scexNetworkControlled()) {
            scexFuelCredit = com.miophas.singularity_iteration.core.runtime.energy.engine.ConsumableGeneration.legacyCredit(currentFuelBurning,
                currentFuelEnergyGenerated, ENERGY_PER_BUCKET / FUEL_PER_BUCKET, FUEL_PER_BUCKET);
            currentFuelBurning = 0;
            currentFuelEnergyGenerated = 0;
        }
    }
}
