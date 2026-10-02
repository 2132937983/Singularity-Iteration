package com.miophas.singularity_iteration.core.prefab.inventory;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.prefab.item.EmergencyChargePolicy;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

@SuppressWarnings("null")
public class MachineItemHandler extends ItemStackHandler {

    private final SlotLayout layout;
    private ISlotValidator validator;

    public MachineItemHandler(SlotLayout layout) {
        super(layout.getTotalSlots());
        this.layout = layout;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Size", getSlots());
        ListTag list = new ListTag();
        for (int i = 0; i < getSlots(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                itemTag.put("Item", stack.save(provider));
                list.add(itemTag);
            }
        }
        tag.put("Items", list);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        for (int i = 0; i < getSlots(); i++) {
            setStackInSlot(i, ItemStack.EMPTY);
        }
        int savedSize = tag.getInt("Size");
        ListTag list = tag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
        int loadCount = Math.min(savedSize, getSlots());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot >= 0 && slot < loadCount) {
                ItemStack stack = ItemStack.parse(provider, itemTag.getCompound("Item")).orElse(ItemStack.EMPTY);
                setStackInSlot(slot, stack);
            }
        }
    }

    public void setValidator(ISlotValidator validator) {
        this.validator = validator;
    }

    public SlotLayout getLayout() {
        return layout;
    }

    /**
     * Container callbacks can arrive after a block entity has been removed or
     * while a stale menu is closing.  Keep the handler boundary total so an
     * invalid slot cannot turn a harmless client/server race into a crash.
     */
    private boolean isValidSlotIndex(int slot) {
        return slot >= 0 && slot < stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return isValidSlotIndex(slot) ? stacks.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (!isValidSlotIndex(slot)) return;
        super.setStackInSlot(slot, stack == null ? ItemStack.EMPTY : stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        if (!isValidSlotIndex(slot)) return stack;
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!isValidSlotIndex(slot) || amount <= 0) return ItemStack.EMPTY;
        return super.extractItem(slot, amount, simulate);
    }

    /** Owner-side slot commit: validate all snapshots and publish all values before notifying observers. */
    public boolean scexCommitSlots(int[] indices, ItemStack[] expected, ItemStack[] replacement) {
        return scexCommitSlots(indices, expected, replacement, () -> {});
    }

    /** Owner state is published before inventory-change notifications can observe the commit. */
    public boolean scexCommitSlots(int[] indices, ItemStack[] expected, ItemStack[] replacement, Runnable publishOwnerState) {
        java.util.Objects.requireNonNull(publishOwnerState, "owner state publication");
        if (indices.length != expected.length || indices.length != replacement.length) return false;
        for (int i = 0; i < indices.length; i++) {
            int slot = indices[i];
            if (slot < 0 || slot >= getSlots() || expected[i] == null || replacement[i] == null
                    || !ItemStack.matches(stacks.get(slot), expected[i])) return false;
            for (int j = 0; j < i; j++) if (indices[j] == slot) return false;
        }
        for (int i = 0; i < indices.length; i++) stacks.set(indices[i], replacement[i].copy());
        publishOwnerState.run();
        for (int slot : indices) onContentsChanged(slot);
        return true;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!isValidSlotIndex(slot) || stack == null) return false;
        if (stack.isEmpty()) return true;
        SlotType type = layout.getInternalSlotType(slot);
        if (type == null) return false;

        switch (type) {
            case INPUT -> {
                if (isBattery(stack)) return false;
                if (isUpgrade(stack)) return false;
                return validator == null || validator.isValidForSlot(slot, stack, type);
            }
            case OUTPUT -> {
                return false;
            }
            case BATTERY -> {
                return isBattery(stack);
            }
            case FLUID_INPUT, FLUID_OUTPUT -> {
                return validator == null || validator.isValidForSlot(slot, stack, type);
            }
            case UPGRADE -> {
                return isUpgrade(stack) && (validator == null || validator.acceptsUpgrade(slot, stack));
            }
            case REACTOR -> {
                return isReactorComponent(stack);
            }
            case EXTRA -> {
                return validator == null || validator.isValidForSlot(slot, stack, type);
            }
            case RTG_PELLET -> {
                return isRTGPellet(stack);
            }
            case TURBINE -> {
                return isTurbine(stack);
            }
            default -> {
                return validator == null || validator.isValidForSlot(slot, stack, type);
            }
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        if (!isValidSlotIndex(slot)) return 0;
        SlotType type = layout.getInternalSlotType(slot);
        if (type == SlotType.REACTOR) return 1;
        if (type == SlotType.RTG_PELLET) return 1;  // RTG靶丸槽只能放1个
        if (type == SlotType.HEAT_CONDUCTOR) return 1;  // 热交换器槽只能放1个（IC2 setStackSizeLimit(1)）
        if (type == SlotType.TURBINE) return 1;  // 涡轮槽只能放1个
        if (type == SlotType.BATTERY) {
            return EmergencyChargePolicy.stackLimitFor(getStackInSlot(slot), 1);
        }
        if (type == SlotType.COIL) return 1;  // 线圈槽只能放1个
        if (type == SlotType.SCANNER) return 1;  // 扫描槽只能放1个
        if (type == SlotType.MEMORY) return 1;  // 记忆槽只能放1个
        if (type == SlotType.TOOL) return 1;  // 工具槽只能放1个
        if (type == SlotType.UPGRADE) return 64;
        return 64;
    }

    protected boolean isBattery(ItemStack stack) {
        return MioIcifAPI.instance().getItemAPI().isBattery(stack)
            || EmergencyChargePolicy.applies(stack)
            || com.miophas.singularity_iteration.core.runtime.energy.FeMachineBridge.dischargeable(stack);
    }

    protected boolean isUpgrade(ItemStack stack) {
        return MioIcifAPI.instance().getItemAPI().isUpgrade(stack);
    }

    protected boolean isReactorComponent(ItemStack stack) {
        return SlotItemPredicates.reactor(stack);
    }

    protected boolean isRTGPellet(ItemStack stack) {
        return SlotItemPredicates.pellet(stack);
    }

    protected boolean isTurbine(ItemStack stack) {
        return SlotItemPredicates.turbine(stack);
    }

    public static boolean canInsertFromSide(MachineItemHandler handler, int slot, ItemStack stack, SlotType allowedType) {
        if (handler == null || !handler.isValidSlotIndex(slot) || stack == null) return false;
        SlotType type = (SlotType) handler.layout.getType(slot);
        if (type != allowedType) return false;
        return handler.isItemValid(slot, stack);
    }

    public static boolean canExtractFromSide(MachineItemHandler handler, int slot, SlotType... allowedTypes) {
        if (handler == null || !handler.isValidSlotIndex(slot) || allowedTypes == null) return false;
        SlotType type = (SlotType) handler.layout.getType(slot);
        for (SlotType allowed : allowedTypes) {
            if (type == allowed) return true;
        }
        return false;
    }
}
