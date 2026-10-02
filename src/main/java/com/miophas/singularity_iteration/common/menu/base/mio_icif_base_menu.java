package com.miophas.singularity_iteration.common.menu.base;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrades;
import com.miophas.singularity_iteration.common.util.mio_icif_gui_global_variables;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

@SuppressWarnings("null")
public abstract class mio_icif_base_menu extends AbstractContainerMenu {

    /**
     * Vanilla sends container data as signed 16-bit values, so any energy, capacity or
     * progress above 32767 wrapped on the client and GUI read-outs jumped back and
     * forth while charging. Every logical value is sent as two 16-bit halves instead;
     * the client reassembles the full int before the screen reads it.
     */
    @Override
    protected void addDataSlots(net.minecraft.world.inventory.ContainerData data) {
        if (data instanceof com.miophas.singularity_iteration.core.api.menu.WordContainerData) {
            super.addDataSlots(data);
            return;
        }
        for (int i = 0; i < data.getCount(); i++) {
            final int index = i;
            addDataSlot(new net.minecraft.world.inventory.DataSlot() {
                @Override public int get() { return data.get(index) & 0xFFFF; }
                @Override public void set(int value) { data.set(index, (data.get(index) & 0xFFFF0000) | (value & 0xFFFF)); }
            });
            addDataSlot(new net.minecraft.world.inventory.DataSlot() {
                @Override public int get() { return data.get(index) >>> 16; }
                @Override public void set(int value) { data.set(index, (data.get(index) & 0xFFFF) | ((value & 0xFFFF) << 16)); }
            });
        }
    }


    /** 标准能量条的像素宽度 */
    protected static final int ENERGY_BAR_WIDTH = mio_icif_gui_global_variables.ENERGY_BAR_WIDTH;

    /** 动能发电机能量条的像素宽度 */
    protected static final int KINETIC_ENERGY_BAR2_WIDTH = mio_icif_gui_global_variables.KINETIC_ENERGY_BAR2_WIDTH;

    protected final int machineSlotCount;

    protected final int playerInventoryStart;

    private final List<SlotMoveRule> customMoveRules = new ArrayList<>();
    private MachineBinding machineBinding;

    /**
     * Upgrade slots are recorded as they are added, so every menu exposes its
     * upgrade range from the single {@code addUpgradeSlot} call site instead of
     * each subclass re-declaring the same start/count constants.  Menus that
     * still override {@link #getUpgradeSlotStart()} keep their value.
     */
    private int trackedUpgradeSlotStart = -1;
    private int trackedUpgradeSlotEnd = -1;

    /**
     * 标记槽位是否属于机器槽位（非玩家物品栏）
     * 用于处理有显示槽混入的情况（如BatBoxMenu、MagnetizerMenu�?
     */
    private final List<Boolean> machineSlotFlags = new ArrayList<>();

    protected mio_icif_base_menu(net.minecraft.world.inventory.MenuType<?> type, int containerId,
                                  int machineSlotCount, int playerInventoryStart) {
        super(type, containerId);
        this.machineSlotCount = machineSlotCount;
        this.playerInventoryStart = playerInventoryStart;
    }

    /**
     * 新版构造函数：不再需要手动传入machineSlotCount和playerInventoryStart
     * 子类应在注册完所有槽位后调用finishSlotRegistration()
     */
    protected mio_icif_base_menu(net.minecraft.world.inventory.MenuType<?> type, int containerId) {
        super(type, containerId);
        this.machineSlotCount = -1;
        this.playerInventoryStart = -1;
    }

    /**
     * 标记接下来的n个槽位为机器槽位
     */
    protected void markMachineSlots(int count) {
        for (int i = 0; i < count; i++) {
            machineSlotFlags.add(true);
        }
    }

    /**
     * 标记接下来的n个槽位为玩家物品栏槽位?
     */
    protected void markPlayerSlots(int count) {
        for (int i = 0; i < count; i++) {
            machineSlotFlags.add(false);
        }
    }

    /**
     * 判断指定索引的槽位是否是机器槽位
     * 优先使用显式标记，如果没有标记则回退到旧的逻辑
     */
    protected boolean isMachineSlot(int index) {
        if (index >= 0 && index < machineSlotFlags.size()) {
            return machineSlotFlags.get(index);
        }
        // 回退到旧的逻辑
        return index < this.machineSlotCount;
    }

    /**
     * 获取玩家物品栏起始索引?
     * 优先使用显式标记查找第一个玩家槽位，如果没有则回退到旧的逻辑
     */
    protected int getPlayerInventoryStart() {
        for (int i = 0; i < machineSlotFlags.size(); i++) {
            if (!machineSlotFlags.get(i)) {
                return i;
            }
        }
        // 回退到旧的逻辑
        return this.playerInventoryStart;
    }

    /**
     * 获取所有机器槽位的索引范围（用于快速移动目标）
     */
    protected List<int[]> getMachineSlotRanges() {
        List<int[]> ranges = new ArrayList<>();
        int start = -1;

        for (int i = 0; i < machineSlotFlags.size(); i++) {
            if (machineSlotFlags.get(i)) {
                if (start == -1) start = i;
            } else {
                if (start != -1) {
                    ranges.add(new int[]{start, i});
                    start = -1;
                }
            }
        }
        if (start != -1) {
            ranges.add(new int[]{start, machineSlotFlags.size()});
        }

        // 如果没有显式标记，使用旧的逻辑
        if (ranges.isEmpty() && this.machineSlotCount > 0) {
            ranges.add(new int[]{0, this.machineSlotCount});
        }

        return ranges;
    }

    /** Keep client menu contents independent of the world inventory. */
    protected final IItemHandler createMenuItemHandler(Inventory inventory, int size,
            IItemHandler source, Supplier<?> owner) {
        machineBinding = new MachineBinding(inventory.player.level(), owner);
        if (source != null && !inventory.player.level().isClientSide) return source;
        return new ItemStackHandler(size) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                IItemHandler target = machineBinding.clientHandler();
                return target != null && slot >= 0 && slot < target.getSlots()
                        && target.isItemValid(slot, stack);
            }
            @Override public int getSlotLimit(int slot) {
                IItemHandler target = machineBinding.clientHandler();
                return target != null && slot >= 0 && slot < target.getSlots() ? target.getSlotLimit(slot) : 0;
            }
        };
    }

    /** Append metadata after the existing machine data; its indices and object stay unchanged. */
    protected final void addMachineBindingData() {
        if (machineBinding != null) addDataSlots(machineBinding);
    }

    /** The server authorizes the original loaded machine, never a replacement at its position. */
    protected final boolean isMachineMenuValid(Player player) {
        if (player.level().isClientSide) return true;
        if (machineBinding == null || !(machineBinding.owner.get() instanceof BlockEntity tile)
                || tile.isRemoved() || !player.isAlive() || tile.getLevel() != player.level()
                || !(tile.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        BlockPos pos = tile.getBlockPos();
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return false;
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK) == tile;
    }

    private static final ClassValue<HandlerAccess> HANDLER_ACCESS = new ClassValue<>() {
        @Override protected HandlerAccess computeValue(Class<?> type) {
            java.lang.reflect.Method method = null;
            java.lang.reflect.Field field = null;
            try { method = type.getMethod("getItemHandler"); } catch (NoSuchMethodException ignored) { }
            try { field = type.getField("itemHandler"); } catch (NoSuchFieldException ignored) { }
            return new HandlerAccess(method, field);
        }
    };
    private record HandlerAccess(java.lang.reflect.Method method, java.lang.reflect.Field field) {
        IItemHandler read(Object owner) {
            if (method != null) {
                try { if (method.invoke(owner) instanceof IItemHandler handler) return handler; }
                catch (ReflectiveOperationException ignored) { }
            }
            if (field != null) {
                try { if (field.get(owner) instanceof IItemHandler handler) return handler; }
                catch (ReflectiveOperationException ignored) { }
            }
            return null;
        }
    }
    protected static IItemHandler getItemHandlerFromBlockEntity(Object owner) {
        return owner == null ? null : HANDLER_ACCESS.get(owner.getClass()).read(owner);
    }

    /** Four unsigned position words followed by the exact BE type id plus one as readiness. */
    private static final class MachineBinding implements ContainerData {
        private final Level level;
        private final Supplier<?> owner;
        private final int[] received = new int[5];
        private int receivedMask;
        MachineBinding(Level level, Supplier<?> owner) { this.level = level; this.owner = owner; }
        @Override public int getCount() { return received.length; }
        @Override public int get(int index) {
            if (level.isClientSide) return received[index];
            // Deferred until data is sent, after subclass constructor fields are initialized.
            Object value = owner.get();
            if (!(value instanceof BlockEntity tile) || tile.isRemoved() || tile.getLevel() != level) return 0;
            if (index < 4) return (int)(tile.getBlockPos().asLong() >>> (index * 16)) & 0xffff;
            int id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(tile.getType()) + 1;
            return id > 0 && id <= 0xffff ? id : 0;
        }
        @Override public void set(int index, int value) {
            received[index] = value & 0xffff;
            receivedMask |= 1 << index;
        }
        IItemHandler clientHandler() {
            int type = received[4];
            if (receivedMask != 0x1f || type == 0) return null;
            long packed = 0;
            for (int i = 0; i < 4; i++) packed |= (long)received[i] << (i * 16);
            BlockPos pos = BlockPos.of(packed);
            var chunk = level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
            if (!(chunk instanceof LevelChunk loaded)) return null;
            BlockEntity tile = loaded.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
            if (tile == null || tile.isRemoved() || tile.getLevel() != level
                    || tile.getType() != BuiltInRegistries.BLOCK_ENTITY_TYPE.byId(type - 1)) return null;
            return getItemHandlerFromBlockEntity(tile);
        }
    }

    protected abstract boolean isBattery(ItemStack stack);

    protected boolean isFuel(ItemStack stack) {
        return false;
    }

    protected boolean isChargeable(ItemStack stack) {
        return false;
    }

    protected boolean isUpgrade(ItemStack stack) {
        return MioIcifAPI.instance().getItemAPI().isUpgrade(stack);
    }

    protected int getBatterySlotIndex() {
        return 0;
    }

    protected int getFuelSlotIndex() {
        return 0;
    }

    protected int getChargeSlotIndex() {
        return 1;
    }

    protected int getUpgradeSlotStart() {
        return trackedUpgradeSlotStart;
    }

    protected int getUpgradeSlotCount() {
        return trackedUpgradeSlotStart < 0 ? 0 : trackedUpgradeSlotEnd - trackedUpgradeSlotStart;
    }

    /**
     * Returns whether this menu exposes a real upgrade-slot range.  Screens use
     * this as the single source of truth for the small compatibility hint in
     * the upper-left corner instead of guessing from a texture or title.
     */
    public final boolean hasUpgradeHint() {
        int count = getUpgradeSlotCount();
        return count > 0 && getUpgradeSlotStart() >= 0;
    }

    /**
     * Whether the given menu slot index belongs to the machine upgrade range.
     * Screens use this to attach the default upgrade-slot tooltip to the exact
     * slot the player hovers, instead of guessing from the slot type/position.
     */
    public final boolean isUpgradeSlot(int index) {
        int start = getUpgradeSlotStart();
        int count = getUpgradeSlotCount();
        return start >= 0 && count > 0 && index >= start && index < start + count;
    }

    /**
     * Resolves the upgrade items that the menu can actually accept.  The
     * check deliberately goes through the menu slots, so custom machine
     * validators and the client-side machine binding are both respected.
     * This is a UI query (eight candidates at most), not a machine-tick path.
     */
    public final List<ItemStack> getCompatibleUpgradeHints() {
        if (!hasUpgradeHint()) return List.of();

        int start = getUpgradeSlotStart();
        int end = start + getUpgradeSlotCount();
        List<ItemStack> compatible = new ArrayList<>();
        List<net.minecraft.world.item.Item> candidates = List.of(
            mio_icif_upgrades.OVERCLOCKER_UPGRADE.get(),
            mio_icif_upgrades.ENERGY_STORAGE_UPGRADE.get(),
            mio_icif_upgrades.TRANSFORMER_UPGRADE.get(),
            mio_icif_upgrades.EJECTOR_UPGRADE.get(),
            mio_icif_upgrades.PULLING_UPGRADE.get(),
            mio_icif_upgrades.FLUID_EJECTOR_UPGRADE.get(),
            mio_icif_upgrades.FLUID_PULLING_UPGRADE.get(),
            mio_icif_upgrades.REDSTONE_INVERTER_UPGRADE.get()
        );
        for (net.minecraft.world.item.Item item : candidates) {
            ItemStack candidate = new ItemStack(item);
            for (Slot slot : slots) {
                // Player inventory slots reuse the same numeric indices; only
                // handler-backed machine slots belong to this range.
                if (slot instanceof net.neoforged.neoforge.items.SlotItemHandler
                        && slot.index >= start && slot.index < end && slot.mayPlace(candidate)) {
                    compatible.add(candidate);
                    break;
                }
            }
        }
        return compatible;
    }

    /**
     * 注册自定义快速移动规则?
     * 子类可以在构造函数中调用此方法添加额外的槽位移动规则
     * 规则按注册顺序执行，先注册的先尝�?
     *
     * @param validator 物品验证�?
     * @param slotStart 目标槽位起始索引（包含）
     * @param slotEnd   目标槽位结束索引（不包含�?
     */
    protected void addCustomMoveRule(Predicate<ItemStack> validator, int slotStart, int slotEnd) {
        customMoveRules.add(new SlotMoveRule(validator, slotStart, slotEnd));
    }

    /**
     * 清空自定义快速移动规则?
     */
    protected void clearCustomMoveRules() {
        customMoveRules.clear();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();

            if (isMachineSlot(index)) {
                // 从机器槽位移出到玩家物品栈?
                if (!this.moveItemStackTo(itemStack1, getPlayerInventoryStart(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家物品栏移入到机器槽位?
                boolean moved = false;

                if (!moved && isUpgrade(itemStack1)) {
                    int upgradeStart = getUpgradeSlotStart();
                    int upgradeCount = getUpgradeSlotCount();
                    if (upgradeStart >= 0 && upgradeCount > 0) {
                        if (this.moveItemStackTo(itemStack1, upgradeStart, upgradeStart + upgradeCount, false)) {
                            moved = true;
                        }
                    }
                }

                if (!moved && isChargeable(itemStack1)) {
                    int chargeSlot = getChargeSlotIndex();
                    if (isValidMachineSlot(chargeSlot) && this.moveItemStackTo(itemStack1, chargeSlot, chargeSlot + 1, false)) {
                        moved = true;
                    }
                }

                if (!moved && isBattery(itemStack1)) {
                    int batterySlot = getBatterySlotIndex();
                    if (isValidMachineSlot(batterySlot) && this.moveItemStackTo(itemStack1, batterySlot, batterySlot + 1, false)) {
                        moved = true;
                    }
                }

                if (!moved && isFuel(itemStack1)) {
                    int fuelSlot = getFuelSlotIndex();
                    if (isValidMachineSlot(fuelSlot) && this.moveItemStackTo(itemStack1, fuelSlot, fuelSlot + 1, false)) {
                        moved = true;
                    }
                }

                if (!moved) {
                    for (SlotMoveRule rule : customMoveRules) {
                        if (rule.validator.test(itemStack1)) {
                            if (this.moveItemStackTo(itemStack1, rule.slotStart, rule.slotEnd, false)) {
                                moved = true;
                                break;
                            }
                        }
                    }
                }

                if (!moved) {
                    // 尝试移动到机器槽位（所有机器槽位范围）
                    moved = tryMoveToMachineSlots(itemStack1);
                }

                if (!moved) {
                    // Do not move within the player inventory here: this range contains the
                    // source slot, so an unsupported stack can be reported as a successful
                    // self-merge even though no machine slot accepted it.
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setByPlayer(itemStack1);
            }

            // A non-empty return tells the client that the shift-click succeeded.
            // Never report success when a slot implementation accepted no items.
            if (itemStack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
        }

        return itemstack;
    }

    /**
     * 检查指定索引是否是有效的机器槽位?
     */
    protected boolean isValidMachineSlot(int index) {
        if (index < 0 || index >= this.slots.size()) return false;
        return isMachineSlot(index);
    }

    /**
     * 尝试将物品移动到任意机器槽位
     */
    protected boolean tryMoveToMachineSlots(ItemStack stack) {
        List<int[]> ranges = getMachineSlotRanges();
        for (int[] range : ranges) {
            if (this.moveItemStackTo(stack, range[0], range[1], false)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
        if (stack.isEmpty() || start < 0 || end > slots.size() || start >= end) return false;
        int originalCount = stack.getCount();
        int step = reverse ? -1 : 1;
        int first = reverse ? end - 1 : start;
        if (stack.isStackable()) {
            for (int i = first; i >= start && i < end && !stack.isEmpty(); i += step) {
                Slot target = slots.get(i);
                ItemStack existing = target.getItem();
                if (existing == stack || existing.isEmpty() || !target.mayPlace(stack)
                        || !ItemStack.isSameItemSameComponents(stack, existing)) continue;
                int added = Math.min(stack.getCount(), Math.max(0, target.getMaxStackSize(existing) - existing.getCount()));
                if (added == 0) continue;
                ItemStack merged = existing.copyWithCount(existing.getCount() + added);
                stack.shrink(added);
                // SlotItemHandler.setChanged() only touches its empty dummy container.
                // Publish through the handler so its dirty/cache callbacks observe the merge.
                target.setByPlayer(merged);
            }
        }
        if (!stack.isEmpty()) {
            for (int i = first; i >= start && i < end; i += step) {
                Slot target = slots.get(i);
                if (target.hasItem() || !target.mayPlace(stack)) continue;
                int added = Math.min(stack.getCount(), Math.max(0, target.getMaxStackSize(stack)));
                if (added == 0) continue;
                target.setByPlayer(stack.split(added));
                target.setChanged();
                break;
            }
        }
        return stack.getCount() < originalCount;
    }

    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, startY + i * 18));
            }
        }

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, hotbarY));
        }
    }

    /**
     * 添加玩家物品栏槽位（重载方法，兼容旧代码�?
     */
    protected void addPlayerInventorySlots(Inventory playerInventory, int startY, int hotbarY) {
        addPlayerInventory(playerInventory, startY, hotbarY);
    }

    /**
     * 添加玩家物品栏槽位（默认位置，兼容旧代码�?
     */
    protected void addPlayerInventorySlots(Inventory playerInventory) {
        addPlayerInventory(playerInventory, 84, 142);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
    }

    /**
     * 设置同步数据（子类应重写此方法）
     */
    public void setSyncData(int index, int value) {
        // 基类空实现，有data的子类应重写
    }

    /**
     * 自定义槽位移动规则?
     */
    private record SlotMoveRule(Predicate<ItemStack> validator, int slotStart, int slotEnd) {
    }

    protected void addUpgradeSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int index, int x, int y) {
        // Track the upgrade range automatically; indices may be added in any order.
        if (trackedUpgradeSlotStart < 0) {
            trackedUpgradeSlotStart = index;
            trackedUpgradeSlotEnd = index + 1;
        } else {
            trackedUpgradeSlotStart = Math.min(trackedUpgradeSlotStart, index);
            trackedUpgradeSlotEnd = Math.max(trackedUpgradeSlotEnd, index + 1);
        }
        this.addSlot(new net.neoforged.neoforge.items.SlotItemHandler(itemHandler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isUpgrade(stack) && (machineBinding == null || itemHandler.isItemValid(index, stack));
            }

            @Override
            public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
                ItemStack stack = getItem();
                if (isTransformerUpgrade(stack)) {
                    return com.miophas.singularity_iteration.common.network.mio_icif_KeyboardManager.isSafetyKeyDown(player);
                }
                return super.mayPickup(player);
            }
        });
    }

    protected boolean isTransformerUpgrade(ItemStack stack) {
        if (MioIcifAPI.instance() == null) return false;
        return MioIcifAPI.instance().getUpgradeAPI().getUpgradeType(stack)
                == com.miophas.singularity_iteration.core.api.upgrade.IUpgradeAPI.UpgradeType.TRANSFORMER;
    }
}
