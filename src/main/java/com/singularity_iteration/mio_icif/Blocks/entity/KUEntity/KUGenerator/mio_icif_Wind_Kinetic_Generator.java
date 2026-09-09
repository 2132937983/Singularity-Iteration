package com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator;

import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.mio_icif_KineticU_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.api.item.IKineticRotor;
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
import net.minecraft.world.level.block.entity.BlockEntity;
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
public class mio_icif_Wind_Kinetic_Generator extends mio_icif_KineticU_Generator {

    // 槽位数量：1个转子槽
    public static final int SLOT_COUNT = 1;
    // 转子槽索引
    public static final int ROTOR_SLOT = 0;

    // 最大转速
    public static final int MAX_RPM = 10000;

    // 刷新间隔（1秒= 20刻）
    public static final int UPDATE_INTERVAL = 20;
    // 转子损伤间隔（1秒= 20刻）
    public static final int ROTOR_DAMAGE_INTERVAL = 20;

    // 风力强度范围：0~130 MCU
    public static final int MIN_WIND_STRENGTH = 0;
    public static final int MAX_WIND_STRENGTH = 130;

    // IC2动能输出修正系数
    public static final float OUTPUT_MODIFIER = 10.0f;

    public static final int STATUS_MISSING_ROTOR = 0;
    public static final int STATUS_BLOCKED_ROTOR = 1;
    public static final int STATUS_WEAK_WIND = 2;
    public static final int STATUS_GENERATING = 3;
    public static final int STATUS_OVERLOAD = 4;

    // 当前风力强度 (0-130 MCU)，基础值10，高度会提供额外加成
    private int baseWindStrength = 10;
    private int windStrength = 10;
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
        super(type != null ? type : com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities.WIND_KINETIC_GENERATOR_ENTITY_TYPE.get(),
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

        Direction facing = getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        Direction rightDir = switch (facing) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case EAST -> Direction.SOUTH;
            default -> Direction.EAST;
        };

        int ret = 0;

        for (int up = -box; up <= box; up++) {
            int y = worldPosition.getY() + up;

            for (int right = -box; right <= box; right++) {
                boolean occupied = false;

                for (int fwd = 0; fwd <= length; fwd++) {
                    if (up == 0 && right == 0 && fwd == 0) continue;

                    int x = worldPosition.getX() + fwd * facing.getStepX() + right * rightDir.getStepX();
                    int z = worldPosition.getZ() + fwd * facing.getStepZ() + right * rightDir.getStepZ();
                    BlockPos checkPos = new BlockPos(x, y, z);

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
     * 计算高度对风力的加成
     */
    private int calculateHeightBonus() {
        if (level == null) return 0;
        int y = worldPosition.getY();
        int bonus = Math.max(0, (y - 64) / 2);
        return Math.min(bonus, 93);
    }

    /**
     * 计算天气对风力的加成
     */
    private int calculateWeatherBonus() {
        if (level == null) return 0;
        if (level.isThundering()) return 35;
        else if (level.isRaining()) return 15;
        return 0;
    }

    /**
     * 更新风力强度
     * 每UPDATE_INTERVAL刻刷新一次
     */
    private void updateWindStrength() {
        if (level == null) return;

        java.util.Random random = new java.util.Random();
        int heightBonus = calculateHeightBonus();
        int weatherBonus = calculateWeatherBonus();
        int baseFluctuation = random.nextInt(11) - 5;
        baseWindStrength = Math.max(5, Math.min(15, 10 + baseFluctuation));
        windStrength = Math.min(MAX_WIND_STRENGTH, baseWindStrength + heightBonus + weatherBonus);
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

        // 检查空间需求
        if (!checkSpaceRequirement()) {
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
        if (blockEntity.currentKineticOutput > 0) {
            blockEntity.isGenerating = true;
            blockEntity.generateAndOutputKineticToBack();
        } else {
            blockEntity.isGenerating = false;
        }

        if (wasGenerating != blockEntity.isGenerating) {
            blockEntity.setChanged();
        }
    }

    /**
     * 产生动能并输出到后方机器
     */
    private void generateAndOutputKineticToBack() {
        if (currentKineticOutput <= 0 || level == null) return;

        Direction facing = getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        Direction backDirection = facing.getOpposite();
        BlockPos backPos = worldPosition.relative(backDirection);

        IMioIcifCapabilities.IKineticStorage backKinetic = level.getCapability(
            IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, backPos, facing);
        if (backKinetic == null) {
            BlockEntity be = level.getBlockEntity(backPos);
            backKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(be);
        }

        if (backKinetic != null && backKinetic.canReceiveKinetic()) {
            long transferred = backKinetic.receiveKinetic(currentKineticOutput, false);
            if (transferred > 0) setChanged();
        }
    }

    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        return 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BaseWindStrength", baseWindStrength);
        tag.putInt("WindStrength", windStrength);
        tag.putInt("TicksUntilUpdate", ticksUntilUpdate);
        tag.putInt("TicksUntilRotorDamage", ticksUntilRotorDamage);
        tag.putInt("CurrentKineticOutput", currentKineticOutput);
        tag.putInt("CurrentMCU", currentMCU);
        tag.putBoolean("IsGenerating", isGenerating);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        baseWindStrength = tag.getInt("BaseWindStrength");
        if (baseWindStrength == 0) baseWindStrength = 10;
        windStrength = tag.getInt("WindStrength");
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
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
        CompoundTag tag = pkt.getTag();
        if (tag != null) loadAdditional(tag, registries);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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
        return maxDurability > 0 ? getRotorDurability() * 100 / maxDurability : 0;
    }

    public ContainerData getContainerData() {
        return dataAccess;
    }

    @Nullable
    public IMioIcifCapabilities.IKineticStorage getKineticStorageCapability(@Nullable Direction side) {
        return kineticStorage;
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
        return new com.singularity_iteration.mio_icif.Menu.Generator.WindKineticGeneratorMenu(
            containerId, playerInventory, this, this.getItemHandler(), this.dataAccess);
    }
}