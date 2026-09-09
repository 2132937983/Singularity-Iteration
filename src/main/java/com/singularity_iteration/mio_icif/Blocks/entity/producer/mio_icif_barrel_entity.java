package com.singularity_iteration.mio_icif.Blocks.entity.producer;

import com.singularity_iteration.mio_icif.api.item.IStorageBlock;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.Menu.Producer.BarrelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 木桶方块实体类 - 参考IC2原版桶实现
 *
 * 酿造系统设计：
 * - 小麦/甘蔗 + 酒花 + 水桶
 * - 检测详细比例（solidRatio）和酒花比例（hopsRatio）属性
 * - 五个阶段 = Brew -> Youngster -> Beer -> Ale -> Dragonblood -> Black Stuff
 *
 * 朗姆酒酿造：
 * - 甘蔗 + 水桶
 * - 检测单独进度进度（age）属性
 *
 * 交换方式根据酒类在GUI上不同显示
 */
@SuppressWarnings("null")
public class mio_icif_barrel_entity extends BlockEntity implements MenuProvider, IStorageBlock {

    // 桶酒类型定义
    public static final int TYPE_EMPTY = 0;
    public static final int TYPE_BEER = 1;
    public static final int TYPE_RUM = 2;

    // 浓度比例名称
    public static final String[] SOLID_RATIO_NAMES = { "Watery ", "Clear ", "Lite ", "", "Strong ", "Thick ", "Stodge " };
    public static final String[] HOPS_RATIO_NAMES = { "Soup ", "Alcfree ", "White ", "", "Dark ", "Full ", "Black " };
    public static final String[] TIME_RATIO_NAMES = { "Brew", "Youngster", "Beer", "Ale", "Dragonblood", "Black Stuff" };

    // 当前状态
    public int type = TYPE_EMPTY;
    public int boozeAmount = 0;      // 液体数量
    public int age = 0;              // 酿酒时间

    // 详细模式属性，只在 detailed=true 模式下才保存
    public boolean detailed = true;
    public int hopsCount = 0;        // 酒花数量
    public int wheatCount = 0;       // 小麦数量

    // 简化模式属性，在 detailed=false 时计算生成
    public int solidRatio = 0;       // 浓度比例值 (0-6)
    public int hopsRatio = 0;        // 酒花比例值 (0-6)
    public int timeRatio = 0;        // 时间阶段值 (0-5)

    // 树液龙头连接方向(0=无 2-5=北东南西)
    public int treetapSide = 0;

    // GUI数据同步
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case BarrelMenu.SLOT_RAW_MATERIAL -> stack.is(Items.WHEAT) || stack.is(Items.SUGAR_CANE);
                case BarrelMenu.SLOT_HOPS -> stack.is(mio_icif_normal.HOPS.get());
                case BarrelMenu.SLOT_WATER -> stack.is(Items.WATER_BUCKET);
                default -> false;
            };
        }
    };

    // 数据同步用
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getBrewProgressPercent();
                case 1 -> 100;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端只读取
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public mio_icif_barrel_entity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.BARREL.get(), pos, state);
    }

    // ============ GUI ============

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mio_icif.block_barrel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BarrelMenu(containerId, playerInventory, this);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    // ============ 酿造逻辑：从GUI槽位添加原料开始酿造 ============

    /**
     * 检查是否可以开始酿造：从GUI槽位添加原料
     */
    private boolean tryConsumeIngredients() {
        if (this.type != TYPE_EMPTY) return false;

        ItemStack rawStack = itemHandler.getStackInSlot(BarrelMenu.SLOT_RAW_MATERIAL);
        ItemStack hopsStack = itemHandler.getStackInSlot(BarrelMenu.SLOT_HOPS);
        ItemStack waterStack = itemHandler.getStackInSlot(BarrelMenu.SLOT_WATER);

        // 检查是否有水
        if (waterStack.isEmpty() || !waterStack.is(Items.WATER_BUCKET)) {
            return false;
        }

        // 检查是否有原材料
        boolean hasWheat = !rawStack.isEmpty() && rawStack.is(Items.WHEAT);
        boolean hasSugarCane = !rawStack.isEmpty() && rawStack.is(Items.SUGAR_CANE);

        if (!hasWheat && !hasSugarCane) {
            return false;
        }

        // 检查材料类型：小麦 + 水桶 = 尝试酿造啤酒，砂糖 + 水桶 = 尝试酿造朗姆酒
        if (hasWheat) {
            // 尝试从原料槽中提取所有小麦
            int wheatAmount = rawStack.getCount();
            ItemStack extractedRaw = itemHandler.extractItem(BarrelMenu.SLOT_RAW_MATERIAL, wheatAmount, false);
            if (extractedRaw.isEmpty()) return false;

            // 设置啤酒槽中存在酒花，如果酒花已经存在则不添加新酒花
            int initialHops = 0;
            if (!hopsStack.isEmpty() && hopsStack.is(mio_icif_normal.HOPS.get())) {
                int hopsAmount = hopsStack.getCount();
                ItemStack extractedHops = itemHandler.extractItem(BarrelMenu.SLOT_HOPS, hopsAmount, false);
                if (!extractedHops.isEmpty()) {
                    initialHops = extractedHops.getCount();
                }
            }

            // 水桶替换为空气
            itemHandler.setStackInSlot(BarrelMenu.SLOT_WATER, new ItemStack(Items.BUCKET));

            // 设置桶状态
            this.type = TYPE_BEER;
            this.boozeAmount = 1;
            this.wheatCount = extractedRaw.getCount();
            this.hopsCount = initialHops;
            this.detailed = true;
            this.age = 0;
            this.timeRatio = 0;

            setChanged();
            return true;
        }

        // 检查是否包含甘蔗 + 水桶 = 开始朗姆酿造
        if (hasSugarCane) {
            // 尝试从原料槽中提取所有甘蔗
            int sugarCaneAmount = rawStack.getCount();
            ItemStack extractedRaw = itemHandler.extractItem(BarrelMenu.SLOT_RAW_MATERIAL, sugarCaneAmount, false);
            if (extractedRaw.isEmpty()) return false;

            // 水桶替换为空气
            itemHandler.setStackInSlot(BarrelMenu.SLOT_WATER, new ItemStack(Items.BUCKET));

            // 设置桶状态
            this.type = TYPE_RUM;
            this.boozeAmount = 1;
            this.wheatCount = extractedRaw.getCount(); // 用于计算进度
            this.detailed = true;
            this.age = 0;
            setChanged();
            return true;
        }

        return false;
    }

    /**
     * 酿造过程中尝试从GUI槽位动态添加酒花
     * 添加过程会修改hopsCount而不会触发alterComposition，需要在输出时重新计算密度
     */
    private void tryAddHopsDuringBrewing() {
        if (this.type != TYPE_BEER) return;

        ItemStack hopsStack = itemHandler.getStackInSlot(BarrelMenu.SLOT_HOPS);
        if (hopsStack.isEmpty() || !hopsStack.is(mio_icif_normal.HOPS.get())) {
            return;
        }

        // 修改ItemStack数量不建议直接修改源
        int count = hopsStack.getCount();
        if (count <= 1) {
            itemHandler.setStackInSlot(BarrelMenu.SLOT_HOPS, ItemStack.EMPTY);
        } else {
            hopsStack.setCount(count - 1);
        }

        this.hopsCount++;
        // 修改子composition防止alterComposition，需要在输出时重新计算粘度
        setChanged();
    }

    /**
     * 获取产出物品提供给容器/龙头/GUI按钮
     * 获取产出时会从GUI槽位中读取啤酒的花 hopsCount，再计算密度
     */
    public ItemStack takeOutput() {
        if (isEmpty()) return ItemStack.EMPTY;

        // 获取产出时会从GUI槽位中读取啤酒的花的数量
        consumeAllGuiHops();

        int meta = calculateMetaValue();
        if (meta == 0) return ItemStack.EMPTY;

        ItemStack output = createOutputItem(meta);
        if (output.isEmpty()) return ItemStack.EMPTY;

        drainLiquid(1);
        setChanged();
        return output;
    }

    /**
     * 从GUI槽位中获取啤酒输出的酒花 hopsCount
     */
    private void consumeAllGuiHops() {
        if (this.type != TYPE_BEER) return;

        ItemStack hopsStack = itemHandler.getStackInSlot(BarrelMenu.SLOT_HOPS);
        if (!hopsStack.isEmpty() && hopsStack.is(mio_icif_normal.HOPS.get())) {
            int count = hopsStack.getCount();
            this.hopsCount += count;
            itemHandler.setStackInSlot(BarrelMenu.SLOT_HOPS, ItemStack.EMPTY);
            setChanged();
        }
    }

    /**
     * 根据元数据值构建输出物品
     */
    private ItemStack createOutputItem(int meta) {
        int itemType = getTypeOfValue(meta);

        if (itemType == TYPE_BEER) {
            int time = getTimeRatioOfBeerValue(meta);
            ItemStack output;
            switch (time) {
                case 0: output = new ItemStack(mio_icif_normal.BREW.get()); break;
                case 1: output = new ItemStack(mio_icif_normal.YOUNGSTER.get()); break;
                case 2: output = new ItemStack(mio_icif_normal.BEER.get()); break;
                case 3: output = new ItemStack(mio_icif_normal.ALE.get()); break;
                case 4: output = new ItemStack(mio_icif_normal.BEER.get()); break;
                case 5: output = new ItemStack(mio_icif_normal.BEER.get()); break;
                default: output = new ItemStack(mio_icif_normal.BEER.get()); break;
            }
            // 使用CUSTOM_DATA存储元数据以区分详细属性版本
            CompoundTag tag = new CompoundTag();
            tag.putInt("BoozeMeta", meta);
            output.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return output;
        }

        if (itemType == TYPE_RUM) {
            int progress = getRumProgressPercent(meta);
            ItemStack output;
            if (progress < 100) {
                output = new ItemStack(mio_icif_normal.BREW.get()); // 酿制中返回未完成液体
            } else {
                output = new ItemStack(mio_icif_normal.RUM.get());
            }
            // 使用CUSTOM_DATA存储元数据以区分不同酿造进度
            CompoundTag tag = new CompoundTag();
            tag.putInt("BoozeMeta", meta);
            output.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return output;
        }

        return ItemStack.EMPTY;
    }

    // ============ 桶状态逻辑 ============

    /**
     * 当酒桶内容变化时调整composition修改值
     */
    public void alterComposition() {
        if (this.timeRatio == 0) this.age = 0;

        if (this.timeRatio == 1) {
            if (this.level != null && this.level.random.nextBoolean()) {
                this.timeRatio = 0;
            } else if (this.level != null && this.level.random.nextBoolean()) {
                this.timeRatio = 5;
            }
        }

        if (this.timeRatio == 2 && this.level != null && this.level.random.nextBoolean()) {
            this.timeRatio = 5;
        }

        if (this.timeRatio > 2) this.timeRatio = 5;
    }

    /**
     * 倒出液体
     */
    public boolean drainLiquid(int amount) {
        if (isEmpty()) return false;
        if (amount > this.boozeAmount) return false;

        enforceUndetailed();

        if (this.type == TYPE_RUM) {
            int progress = this.age * 100 / timeNeededForRum(this.boozeAmount);
            this.boozeAmount -= amount;
            this.age = progress / 100 * timeNeededForRum(this.boozeAmount);
        } else {
            this.boozeAmount -= amount;
        }

        if (this.boozeAmount <= 0) {
            reset();
        }

        return true;
    }

    /**
     * 计算桶的各种属性值从详细模式转为简化模式时计算
     * 需要计算hopsRatio和solidRatio，但不要修改timeRatio因为已经完成当前阶段的转换
     */
    public void enforceUndetailed() {
        if (!this.detailed) return;
        this.detailed = false;

        if (this.type == TYPE_BEER) {
            float hops = (this.wheatCount > 0) ? ((float) this.hopsCount / this.wheatCount) : 10.0f;
            if (this.hopsCount <= 0 && this.wheatCount <= 0) hops = 0.0f;

            float solid = (this.boozeAmount > 0) ? ((float)(this.hopsCount + this.wheatCount) / this.boozeAmount) : 10.0f;

            if (hops <= 0.25f) this.hopsRatio = 0;
            else if (hops <= 0.33333334f) this.hopsRatio = 1;
            else if (hops <= 0.5f) this.hopsRatio = 2;
            else if (hops < 2.0f) this.hopsRatio = 3;
            else if (hops < 3.0f) this.hopsRatio = 4;
            else if (hops < 4.0f) this.hopsRatio = 5;
            else if (hops < 5.0f) this.hopsRatio = 6;
            else this.hopsRatio = 6; // 酒花过多hopsRatio 设为最大值

            if (solid <= 0.41666666f) this.solidRatio = 0;
            else if (solid <= 0.5f) this.solidRatio = 1;
            else if (solid < 1.0f) this.solidRatio = 2;
            else if (solid == 1.0f) this.solidRatio = 3;
            else if (solid < 2.0f) this.solidRatio = 4;
            else if (solid < 2.4f) this.solidRatio = 5;
            else if (solid < 4.0f) this.solidRatio = 6;
            else this.solidRatio = 6; // 实体过多solidRatio 设为最大值
        }
    }

    /**
     * 重置木桶
     */
    public void reset() {
        if (this.type == TYPE_BEER) {
            this.hopsCount = 0;
            this.wheatCount = 0;
            this.hopsRatio = 0;
            this.solidRatio = 0;
            this.timeRatio = 0;
        }
        this.type = TYPE_EMPTY;
        this.detailed = true;
        this.boozeAmount = 0;
        this.age = 0;
    }

    // ============ 元数据编码计算 ===========

    public int calculateMetaValue() {
        if (isEmpty()) return 0;

        if (this.type == TYPE_BEER) {
            enforceUndetailed();
            int value = 0;
            value |= this.timeRatio; value <<= 3;
            value |= this.hopsRatio; value <<= 3;
            value |= this.solidRatio; value <<= 5;
            value |= this.boozeAmount - 1; value <<= 2;
            value |= this.type;
            return value;
        }

        if (this.type == TYPE_RUM) {
            enforceUndetailed();
            int value = 0;
            int progress = this.age * 100 / timeNeededForRum(this.boozeAmount);
            if (progress > 100) progress = 100;
            value |= progress; value <<= 5;
            value |= this.boozeAmount - 1; value <<= 2;
            value |= this.type;
            return value;
        }

        return 0;
    }

    public static int getTypeOfValue(int value) {
        return skipGetOfValue(value, 0, 2);
    }

    public static int getAmountOfValue(int value) {
        if (getTypeOfValue(value) == 0) return 0;
        return skipGetOfValue(value, 2, 5) + 1;
    }

    public static int getSolidRatioOfBeerValue(int value) {
        return skipGetOfValue(value, 7, 3);
    }

    public static int getHopsRatioOfBeerValue(int value) {
        return skipGetOfValue(value, 10, 3);
    }

    public static int getTimeRatioOfBeerValue(int value) {
        return skipGetOfValue(value, 13, 3);
    }

    public static int getProgressOfRumValue(int value) {
        return skipGetOfValue(value, 7, 7);
    }

    private static int skipGetOfValue(int value, int bitshift, int take) {
        value >>= bitshift;
        take = (int) Math.pow(2.0, take) - 1;
        return value & take;
    }

    /**
     * 从范围中获取朗姆酒酿进度百分比
     */
    public static int getRumProgressPercent(int meta) {
        if (getTypeOfValue(meta) != TYPE_RUM) return 0;
        return getProgressOfRumValue(meta);
    }

    // ============ 朗姆酒时间换算计算 ===========

    public int timeNeededForRum(int amount) {
        return (int) ((1200 * amount) * Math.pow(0.95, (amount - 1)));
    }

    // ============ 状态校验方法 ============

    public boolean isEmpty() {
        return this.type == TYPE_EMPTY || this.boozeAmount <= 0;
    }

    // ============ Tick更新方法 ============

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_barrel_entity entity) {
        if (level.isClientSide()) return;

        // 检查如果木桶为空时尝试从GUI槽位自动添加原料开始酿造
        if (entity.isEmpty()) {
            entity.tryConsumeIngredients();
            return;
        }

        // 检查酿造过程中自动尝试添加酒花使当前阶段继续酿造
        if (entity.type == TYPE_BEER) {
            entity.tryAddHopsDuringBrewing();
        }

        if (entity.treetapSide >= 2) return; // 如果木龙头连接则不再进行处理

        // 设置酿速倍速器速度为1，玩家使用文字修改器改变
        int brewSpeedMultiplier = 100;
        entity.age += brewSpeedMultiplier;

        // 检查酿造阶段更新
        if (entity.type == TYPE_BEER && entity.timeRatio < 5) {
            int x = entity.timeRatio;
            if (x == 4) x += 2;

            if (entity.age >= 24000.0 * Math.pow(3.0, x)) {
                entity.age = 0;
                entity.timeRatio++;
                entity.setChanged();
            }
        }
    }

    // ============ 状态消息处理 ============

    public String getStatusMessage() {
        if (isEmpty()) return "message.mio_icif.barrel.empty";

        if (type == TYPE_BEER) {
            if (timeRatio == 0 && age == 0) return "message.mio_icif.barrel.beer_brew";
            if (timeRatio < 5) {
                return "message.mio_icif.barrel.beer_fermenting";
            }
            return "message.mio_icif.barrel.beer_ready";
        }

        if (type == TYPE_RUM) {
            int needed = timeNeededForRum(boozeAmount);
            int progress = age * 100 / needed;
            if (progress < 100) return "message.mio_icif.barrel.rum_fermenting";
            return "message.mio_icif.barrel.rum_ready";
        }

        return "message.mio_icif.barrel.empty";
    }

    public int getBrewProgressPercent() {
        if (isEmpty()) return 0;

        if (type == TYPE_BEER && timeRatio < 5) {
            int x = timeRatio;
            if (x == 4) x += 2;
            int needed = (int) (24000.0 * Math.pow(3.0, x));
            return (int) ((double) age / needed * 100);
        }

        if (type == TYPE_RUM) {
            int needed = timeNeededForRum(boozeAmount);
            int progress = age * 100 / needed;
            return Math.min(progress, 100);
        }

        return 100;
    }

    // ============ NBT数据方法 ===========

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.type = tag.getByte("type");
        this.boozeAmount = tag.getByte("waterCount");
        this.age = tag.getInt("age");
        this.treetapSide = tag.getByte("treetapSide");
        this.detailed = tag.getBoolean("detailed");

        if (this.type == TYPE_BEER) {
            if (this.detailed) {
                this.hopsCount = tag.getByte("hopsCount");
                this.wheatCount = tag.getByte("wheatCount");
            }
            this.solidRatio = tag.getByte("solidRatio");
            this.hopsRatio = tag.getByte("hopsRatio");
            this.timeRatio = tag.getByte("timeRatio");
        }

        // 读取物品数据
        if (tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("type", (byte) this.type);
        tag.putByte("waterCount", (byte) this.boozeAmount);
        tag.putInt("age", this.age);
        tag.putByte("treetapSide", (byte) this.treetapSide);
        tag.putBoolean("detailed", this.detailed);

        if (this.type == TYPE_BEER) {
            if (this.detailed) {
                tag.putByte("hopsCount", (byte) this.hopsCount);
                tag.putByte("wheatCount", (byte) this.wheatCount);
            }
            tag.putByte("solidRatio", (byte) this.solidRatio);
            tag.putByte("hopsRatio", (byte) this.hopsRatio);
            tag.putByte("timeRatio", (byte) this.timeRatio);
        }

        // 保存物品数据
        tag.put("Items", itemHandler.serializeNBT(registries));
    }

    /**
     * 方块被移除时实体销毁
     */
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }
}