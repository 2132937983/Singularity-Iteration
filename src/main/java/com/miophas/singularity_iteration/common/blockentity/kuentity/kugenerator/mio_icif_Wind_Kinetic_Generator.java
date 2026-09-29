package com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractKineticGeneratorBlockEntity;
import com.miophas.singularity_iteration.core.runtime.world.WindSim;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.item.IKineticRotor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * 风力动能发生机方块实体
 * 通过风力产生动能（KU），需要放置转子才能工作
 *
 * 转子属性（IC2兼容）：
 * | 转子材质        | 直径 | 最小风力MCU | 最大风力MCU | 效率  | 耐久    |
 * |----------------|------|------------|------------|-------|---------|
 * | 木             | 5    | 10         | 60         | 0.25  | 10,800  |
 * | 铁/青铜        | 7    | 14         | 75         | 0.50  | 86,400  |
 * | 钢(精炼铁)     | 9    | 17         | 90         | 0.75  | 172,800 |
 * | 碳             | 11   | 20         | 110        | 1.00  | 604,800 |
 * | 钛铁合金       | 9    | 12         | 100        | 0.91  | 192,800 |
 * | 超级铱合金     | 11   | 18         | 155        | 1.00  | MAX_INT |
 *
 * 空间需求：直径×直径×1（前方），深度=直径×3
 * 过载损伤：风力>最大风力时，耐久损伤×4
 */
@SuppressWarnings("null")
public class mio_icif_Wind_Kinetic_Generator extends AbstractKineticGeneratorBlockEntity {

    // 槽位数量：1个转子槽
    public static final int SLOT_COUNT = 1;
    // 转子槽索引
    public static final int ROTOR_SLOT = 0;

    // 最大转速
    public static final int MAX_RPM = 10000;

    // 刷新间隔（IC2 的 getTickRate = 32 刻）
    public static final int UPDATE_INTERVAL = 32;
    // 转子损伤间隔（与刷新同步，IC2 为每 32 刻损伤 1 点）
    public static final int ROTOR_DAMAGE_INTERVAL = 32;

    // 风力强度范围：由全局风场 WindSim 决定（保留常量以兼容旧引用，不再用作硬上限）
    @SuppressWarnings("unused")
    public static final int MAX_WIND_STRENGTH = 130;

    // IC2动能输出修正系数
    public static final float OUTPUT_MODIFIER = 10.0f;

    public static final int STATUS_MISSING_ROTOR = 0;
    public static final int STATUS_BLOCKED_ROTOR = 1;
    public static final int STATUS_WEAK_WIND = 2;
    public static final int STATUS_GENERATING = 3;
    public static final int STATUS_OVERLOAD = 4;

    // 当前有效风力强度（经全局风场、高度系数与遮挡比例修正后的 MCU 值）
    private int windStrength = 0;
    // 上一次空间扫描的遮挡截面数（-1 表示被其它风力动能发生机完全阻塞）
    private int lastObstruction = 0;
    // 距离下次刷新的刻数
    private int ticksUntilUpdate = UPDATE_INTERVAL;
    // 距离下次转子损伤的刻数
    private int ticksUntilRotorDamage = ROTOR_DAMAGE_INTERVAL;
    // 当前是否正在产生动能
    private boolean isGenerating = false;
    // 当前计算的动能输出
    private int currentKineticOutput = 0;
    // 当前MCU输出（用于显示）
    private int currentMCU = 0;
    private int generatorStatus = STATUS_MISSING_ROTOR;
    private long lastVisualSyncTick = Long.MIN_VALUE;
    private ItemStack lastVisualRotor = ItemStack.EMPTY;
    private int lastVisualMCU;
    private int lastVisualOutput;
    private boolean lastVisualGenerating;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> generatorStatus;
                case 1 -> currentKineticOutput;
                case 2 -> getRotorHealthPercent();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Menu data is server-authoritative and read-only.
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    /**
     * 构造函数（用于 BlockEntityType.Builder）
     */
    public mio_icif_Wind_Kinetic_Generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    /**
     * 完整构造函数
     */
    public mio_icif_Wind_Kinetic_Generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(type != null ? type : com.miophas.singularity_iteration.common.registry.mio_icif_block_entities.WIND_KINETIC_GENERATOR_ENTITY_TYPE.get(),
              pos, state, SlotLayout.builder().rotor().build(), 0, 0, MAX_RPM);
    }

    /**
     * 获取转子物品
     */
    public ItemStack getRotorStack() {
        return itemHandler.getStackInSlot(ROTOR_SLOT);
    }

    /**
     * 设置转子物品
     */
    public void setRotorStack(ItemStack stack) {
        itemHandler.setStackInSlot(ROTOR_SLOT, stack);
        setChanged();
    }

    /**
     * 获取转子对象（如果槽位中有有效转子）
     */
    @Nullable
    private IKineticRotor getRotor() {
        ItemStack stack = getRotorStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof IKineticRotor rotor)) {
            return null;
        }
        return rotor;
    }

    /**
     * 检查前方空间是否满足转子需求
     * 参考IC2逻辑：
     * - 检查前方一个平面区域（深度1格），看是否有其他方块
     * - 如果前方有其他风能机，返回-1表示完全阻塞
     * @return 如果空间满足需求返回0，发现其他风能机返回-1，有阻塞返回正数
     */
    public int checkSpace(boolean onlyRotor) {
        if (level == null) return -1;

        IKineticRotor rotor = getRotor();
        if (rotor == null) return -1;

        int diameter = rotor.getDiameter(getRotorStack());
        int box = diameter / 2;
        int length = onlyRotor ? 1 : diameter * 3;
        if (!onlyRotor) box *= 2;

        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        // 与朝向垂直的两个基准轴：水平朝向以世界向上为“上”，垂直朝向以北方为“上”
        Direction upDir = facing.getAxis().isVertical() ? Direction.NORTH : Direction.UP;
        Direction rightDir = switch (facing) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case EAST -> Direction.SOUTH;
            case UP -> Direction.WEST;
            case DOWN -> Direction.EAST;
        };

        int ret = 0;

        for (int up = -box; up <= box; up++) {

            for (int right = -box; right <= box; right++) {
                boolean occupied = false;

                for (int fwd = 0; fwd <= length; fwd++) {
                    if (up == 0 && right == 0 && fwd == 0) continue;

                    BlockPos checkPos = worldPosition.relative(facing, fwd)
                            .relative(upDir, up)
                            .relative(rightDir, right);

                    // Do not force-load adjacent chunks during the rotor scan. An unloaded
                    // slice is conservatively treated as blocked until it is loaded.
                    if (!level.hasChunkAt(checkPos)) {
                        occupied = true;
                        continue;
                    }
                    BlockState checkState = level.getBlockState(checkPos);
                    if (!checkState.isAir() && checkState.isSolidRender(level, checkPos)) {
                        occupied = true;

                        if (!onlyRotor && level.getBlockEntity(checkPos) instanceof mio_icif_Wind_Kinetic_Generator) {
                            return -1;
                        }
                    }
                }

                if (occupied) ret++;
            }
        }

        return ret;
    }

    /**
     * 检查前方空间是否满足转子需求（简化版）
     */
    public boolean checkSpaceRequirement() {
        return checkSpace(false) == 0;
    }

    /**
     * 更新风力强度（IC2 机制）
     * 每UPDATE_INTERVAL刻刷新一次：
     * 风力取自全局风场 WindSim（含高度三次多项式系数与天气加成），
     * 再按转子遮挡截面比例衰减：wind *= 1 - (遮挡截面/总截面)^2，
     * 少量遮挡（不超过 (直径+1)/2 列）被忽略；
     * 被其它风力动能发生机完全阻塞时风力为 0。
     */
    private void updateWindStrength() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) {
            windStrength = 0;
            lastObstruction = 0;
            return;
        }

        IKineticRotor rotor = getRotor();
        if (rotor == null) {
            windStrength = 0;
            lastObstruction = 0;
            return;
        }

        double simWind = WindSim.get(server).getWindAt(worldPosition.getY() + 0.5);
        int diameter = rotor.getDiameter(getRotorStack());
        int obstruction = checkSpace(false);
        lastObstruction = obstruction;

        if (obstruction < 0) {
            // 前方扫描范围内发现了其它风力动能发生机，完全阻塞
            windStrength = 0;
            return;
        }

        // IC2：小遮挡直接忽略
        if (obstruction > 0 && obstruction <= (diameter + 1) / 2) {
            obstruction = 0;
        }

        // IC2 的转子总截面：(2*直径 + 1)^2
        int crossSection = (diameter * 2 + 1) * (diameter * 2 + 1);
        double obstructionFactor = Math.max(0.0, 1.0 - Math.pow((double) obstruction / crossSection, 2.0));
        windStrength = (int) (simWind * obstructionFactor);
    }

    /**
     * 计算动能输出（IC2兼容逻辑）
     * 输出 = windStrength * efficiency * OUTPUT_MODIFIER
     * 风力低于最小需求时不发电
     * 风力超过最大承受时仍发电但转子过载损伤
     */
    private int calculateKineticOutput() {
        IKineticRotor rotor = getRotor();
        ItemStack rotorStack = getRotorStack();

        if (rotor == null) {
            currentMCU = 0;
            generatorStatus = STATUS_MISSING_ROTOR;
            return 0;
        }

        // 空间需求已在 updateWindStrength 中扫描（-1 = 被其它风力机完全阻塞）
        if (lastObstruction < 0) {
            currentMCU = 0;
            generatorStatus = STATUS_BLOCKED_ROTOR;
            return 0;
        }

        int minWind = rotor.getMinWindStrength(rotorStack);
        int maxWind = rotor.getMaxWindStrength(rotorStack);

        // 检查最小风力需求
        if (windStrength < minWind) {
            currentMCU = 0;
            generatorStatus = STATUS_WEAK_WIND;
            return 0;
        }

        // 当前有效MCU值（用于显示）
        this.currentMCU = windStrength;

        // 检查是否过载
        if (windStrength > maxWind) {
            generatorStatus = STATUS_OVERLOAD;
        } else {
            generatorStatus = STATUS_GENERATING;
        }

        // IC2风格输出：windStrength * efficiency * outputModifier
        int kineticOutput = (int)(windStrength * rotor.getEfficiency(rotorStack) * OUTPUT_MODIFIER);
        return Math.max(0, kineticOutput);
    }

    /**
     * 对转子造成耐久损伤（IC2兼容逻辑）
     * 正常发电：1点损伤/秒
     * 过载（风力>最大承受）：4点损伤/秒
     */
    private void damageRotor() {
        ItemStack rotorStack = getRotorStack();
        if (rotorStack.isEmpty()) return;

        IKineticRotor rotor = getRotor();
        if (rotor == null) return;

        // IC2过载逻辑：风力超过最大承受时4倍损伤
        int damage = (windStrength > rotor.getMaxWindStrength(rotorStack)) ? 4 : 1;
        rotor.damageRotor(rotorStack, damage);

        if (rotorStack.isEmpty()) {
            itemHandler.setStackInSlot(ROTOR_SLOT, ItemStack.EMPTY);
        }

        setChanged();
    }

    /**
     * 每tick更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_Wind_Kinetic_Generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.applyFrictionLoss();

        blockEntity.ticksUntilUpdate--;

        // 每UPDATE_INTERVAL刻更新一次风力强度和动能输出
        if (blockEntity.ticksUntilUpdate <= 0) {
            blockEntity.ticksUntilUpdate = UPDATE_INTERVAL;
            blockEntity.updateWindStrength();
            blockEntity.currentKineticOutput = blockEntity.calculateKineticOutput();
        }

        // 每ROTOR_DAMAGE_INTERVAL刻（1秒）对转子造成一次损伤
        blockEntity.ticksUntilRotorDamage--;
        if (blockEntity.ticksUntilRotorDamage <= 0) {
            blockEntity.ticksUntilRotorDamage = ROTOR_DAMAGE_INTERVAL;
            if (blockEntity.currentKineticOutput > 0) {
                blockEntity.damageRotor();
            }
        }

        boolean wasGenerating = blockEntity.isGenerating;
        // IC2 机制：不主动推送动能。KU 由下游的动能发电机按需从背面拉取
        // （见 extractKinetic），风力条件满足时转子保持运转并损耗耐久。
        blockEntity.isGenerating = blockEntity.currentKineticOutput > 0;

        if (wasGenerating != blockEntity.isGenerating) {
            blockEntity.setChanged();
        }
        blockEntity.syncVisualState();
    }

    // ==================== IC2 式按需动能输出（IKineticSource 语义） ====================
    // 与 IC2 的 drawKineticEnergy 对应：只应答背面（FACING 反方向）的拉取请求，
    // 应答量 = min(请求量, 当前 KU 输出)，不落盘、不缓存，超出当前输出的部分不存在。

    @Override
    public boolean canExtractKinetic() {
        return currentKineticOutput > 0;
    }

    @Override
    public long extractKinetic(long toExtract, boolean simulate) {
        if (toExtract <= 0 || currentKineticOutput <= 0) return 0;
        long served = Math.min(toExtract, currentKineticOutput);
        if (!simulate && served > 0) setChanged();
        return served;
    }

    @Override
    public long getMaxExtract() {
        return Math.max(0, currentKineticOutput);
    }

    /**
     * 只在背面暴露可拉取的动能视图；其余面暴露内部存储（容量 0，不可提取），
     * 与 IC2 中只有背面能输出 KU 的行为一致。
     */
    @Nullable
    @Override
    public IMioIcifCapabilities.IKineticStorage getKineticStorageCapability(@Nullable Direction side) {
        if (side == null) return this;
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        return side == facing.getOpposite() ? this : kineticStorage;
    }

    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        return 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("WindStrength", windStrength);
        tag.putInt("LastObstruction", lastObstruction);
        tag.putInt("TicksUntilUpdate", ticksUntilUpdate);
        tag.putInt("TicksUntilRotorDamage", ticksUntilRotorDamage);
        tag.putInt("CurrentKineticOutput", currentKineticOutput);
        tag.putInt("CurrentMCU", currentMCU);
        tag.putBoolean("IsGenerating", isGenerating);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        windStrength = tag.getInt("WindStrength");
        lastObstruction = tag.getInt("LastObstruction");
        ticksUntilUpdate = tag.getInt("TicksUntilUpdate");
        ticksUntilRotorDamage = tag.getInt("TicksUntilRotorDamage");
        if (ticksUntilRotorDamage == 0) ticksUntilRotorDamage = ROTOR_DAMAGE_INTERVAL;
        currentKineticOutput = tag.getInt("CurrentKineticOutput");
        currentMCU = tag.getInt("CurrentMCU");
        isGenerating = tag.getBoolean("IsGenerating");
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // Only the single rotor (including appearance components) and public
        // visual state belong on the wire. Persistence keeps its complete NBT.
        CompoundTag tag = new CompoundTag();
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.putInt("CurrentMCU", currentMCU);
        tag.putInt("CurrentKineticOutput", currentKineticOutput);
        tag.putBoolean("IsGenerating", isGenerating);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        // The parent applies saved inventory/timers; a compact update must not.
        if (tag.contains("Items")) itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        if (tag.contains("CurrentMCU")) currentMCU = tag.getInt("CurrentMCU");
        if (tag.contains("CurrentKineticOutput")) currentKineticOutput = tag.getInt("CurrentKineticOutput");
        if (tag.contains("IsGenerating")) isGenerating = tag.getBoolean("IsGenerating");
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) handleUpdateTag(tag, registries);
    }

    /** Called once after the server tick; setChanged itself only marks saving dirty. */
    private void syncVisualState() {
        if (level == null || level.isClientSide()) return;
        long now = level.getGameTime();
        if (now == lastVisualSyncTick) return;
        ItemStack rotor = getRotorStack();
        boolean discrete = rotor.isEmpty() != lastVisualRotor.isEmpty()
            || rotor.getItem() != lastVisualRotor.getItem() || isGenerating != lastVisualGenerating;
        if (!discrete && lastVisualSyncTick != Long.MIN_VALUE && now - lastVisualSyncTick < UPDATE_INTERVAL) return;
        boolean changed = discrete || currentMCU != lastVisualMCU || currentKineticOutput != lastVisualOutput
            || !ItemStack.matches(rotor, lastVisualRotor);
        if (!changed) return;
        lastVisualSyncTick = now;
        lastVisualRotor = rotor.copy();
        lastVisualMCU = currentMCU;
        lastVisualOutput = currentKineticOutput;
        lastVisualGenerating = isGenerating;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    // Getter方法供GUI使用
    public int getWindStrength() { return windStrength; }
    public int getCurrentKineticOutput() { return currentKineticOutput; }
    public int getCurrentMCU() { return currentMCU; }
    public boolean isGenerating() { return isGenerating; }

    @Override public int getKineticOutput() { return isGenerating ? currentKineticOutput : 0; }
    @Override public int getBurnTime() { return 0; }
    @Override public int getBurnDuration() { return 0; }
    @Override public int getKineticGenerationRate() { return currentKineticOutput; }
    @Override public int getRotorRPM() { return kineticStorage != null ? kineticStorage.getRPM() : 0; }

    public int getRotorDurability() {
        ItemStack rotorStack = getRotorStack();
        if (rotorStack.isEmpty()) return 0;
        IKineticRotor rotor = getRotor();
        if (rotor == null) return 0;
        return rotor.getDurability(rotorStack);
    }

    public int getRotorMaxDurability() {
        ItemStack rotorStack = getRotorStack();
        if (rotorStack.isEmpty()) return 0;
        IKineticRotor rotor = getRotor();
        if (rotor == null) return 0;
        return rotor.getMaxDurability();
    }

    public int getRotorHealthPercent() {
        int maxDurability = getRotorMaxDurability();
        if (maxDurability <= 0) return 0;
        // 用 long 运算避免超级铱合金转子(INT_MAX 耐久) 的 int 溢出
        int percent = (int) ((long) getRotorDurability() * 100L / maxDurability);
        return Math.max(0, Math.min(100, percent));
    }

    public ContainerData getContainerData() {
        return dataAccess;
    }

    public net.neoforged.neoforge.items.IItemHandler createItemHandler() {
        return new net.neoforged.neoforge.items.IItemHandlerModifiable() {
            @Override
            public int getSlots() { return itemHandler.getSlots(); }

            @Override
            public @org.jetbrains.annotations.NotNull ItemStack getStackInSlot(int slot) {
                return itemHandler.getStackInSlot(slot);
            }

            @Override
            public @org.jetbrains.annotations.NotNull ItemStack insertItem(int slot, @org.jetbrains.annotations.NotNull ItemStack stack, boolean simulate) {
                if (slot != ROTOR_SLOT || !(stack.getItem() instanceof IKineticRotor)) return stack;
                ItemStack existing = itemHandler.getStackInSlot(slot);
                if (existing.isEmpty()) {
                    int limit = Math.min(stack.getCount(), getMaxStackSize());
                    if (!simulate) {
                        itemHandler.setStackInSlot(slot, stack.copyWithCount(limit));
                        setChanged();
                    }
                    return stack.getCount() > limit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
                }
                return stack;
            }

            @Override
            public @org.jetbrains.annotations.NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot != ROTOR_SLOT) return ItemStack.EMPTY;
                ItemStack existing = itemHandler.getStackInSlot(slot);
                if (existing.isEmpty()) return ItemStack.EMPTY;
                int toExtract = Math.min(amount, existing.getCount());
                ItemStack extracted = existing.copyWithCount(toExtract);
                if (!simulate) {
                    existing.shrink(toExtract);
                    if (existing.isEmpty()) itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                    setChanged();
                }
                return extracted;
            }

            @Override
            public int getSlotLimit(int slot) { return getMaxStackSize(); }

            @Override
            public boolean isItemValid(int slot, @org.jetbrains.annotations.NotNull ItemStack stack) {
                return slot == ROTOR_SLOT && stack.getItem() instanceof IKineticRotor;
            }

            @Override
            public void setStackInSlot(int slot, @org.jetbrains.annotations.NotNull ItemStack stack) {
                itemHandler.setStackInSlot(slot, stack);
            }
        };
    }

    @Nullable
    public net.neoforged.neoforge.items.IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return createItemHandler();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.wind_kinetic_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.WindKineticGeneratorMenu(
            containerId, playerInventory, this, this.getItemHandler(), this.dataAccess);
    }
}
