package com.singularity_iteration.mio_icif.Blocks.entity.slot;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
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

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
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
                return isUpgrade(stack);
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
        SlotType type = layout.getInternalSlotType(slot);
        if (type == SlotType.REACTOR) return 1;
        if (type == SlotType.RTG_PELLET) return 1;  // RTG靶丸槽只能放1个
        if (type == SlotType.TURBINE) return 1;  // 涡轮槽只能放1个
        if (type == SlotType.BATTERY) {
            ItemStack stack = getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() == net.minecraft.world.item.Items.REDSTONE) return 64;
            return 1;
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
            || stack.getItem() == net.minecraft.world.item.Items.REDSTONE;
    }

    protected boolean isUpgrade(ItemStack stack) {
        return MioIcifAPI.instance().getItemAPI().isUpgrade(stack);
    }

    protected boolean isReactorComponent(ItemStack stack) {
        return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactor;
    }

    protected boolean isRTGPellet(ItemStack stack) {
        return stack.is(com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources.RTG_PELLET.get());
    }

    protected boolean isTurbine(ItemStack stack) {
        return stack.is(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.STEAM_TURBINE_BLADE.get());
    }

    public static boolean canInsertFromSide(MachineItemHandler handler, int slot, ItemStack stack, SlotType allowedType) {
        SlotType type = (SlotType) handler.layout.getType(slot);
        if (type != allowedType) return false;
        return handler.isItemValid(slot, stack);
    }

    public static boolean canExtractFromSide(MachineItemHandler handler, int slot, SlotType... allowedTypes) {
        SlotType type = (SlotType) handler.layout.getType(slot);
        for (SlotType allowed : allowedTypes) {
            if (type == allowed) return true;
        }
        return false;
    }
}