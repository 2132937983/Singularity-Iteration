package com.singularity_iteration.mio_icif.uu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Objects;

/**
 * 轻量级物品堆栈，用于 UU Graph 计算
 * 与 IC2 1.12.2 原版 LeanItemStack 对应
 */
public class LeanItemStack {
    private final Item item;
    private final CompoundTag nbt;
    private final int size;
    private int hashCode;

    public LeanItemStack(ItemStack stack) {
        this(stack.getItem(), getTagFromStack(stack), stack.getCount());
    }

    public LeanItemStack(ItemStack stack, int size) {
        this(stack.getItem(), getTagFromStack(stack), size);
    }

    public LeanItemStack(Item item, CompoundTag nbt, int size) {
        if (item == null) throw new NullPointerException("null item");
        this.item = item;
        this.nbt = nbt;
        this.size = size;
    }

    public Item getItem() {
        return item;
    }

    public CompoundTag getNbt() {
        return nbt;
    }

    public int getSize() {
        return size;
    }

    public boolean hasSameItem(LeanItemStack o) {
        if (o == null) return false;
        return this.item == o.item && checkNbtEquality(this.nbt, o.nbt);
    }

    public LeanItemStack copy() {
        return copyWithSize(this.size);
    }

    public LeanItemStack copyWithSize(int newSize) {
        LeanItemStack ret = new LeanItemStack(this.item, this.nbt, newSize);
        ret.hashCode = this.hashCode;
        return ret;
    }

    public ItemStack toMcStack() {
        if (this.size <= 0) return ItemStack.EMPTY;
        ItemStack ret = new ItemStack(this.item, this.size);
        if (this.nbt != null) {
            ret.set(DataComponents.CUSTOM_DATA, CustomData.of(this.nbt.copy()));
        }
        return ret;
    }

    /**
     * 从ItemStack获取NBT标签（1.21+使用DataComponents）
     */
    private static CompoundTag getTagFromStack(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : null;
    }

    @Override
    public String toString() {
        return String.format("%dx%s", size, BuiltInRegistries.ITEM.getKey(item));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LeanItemStack o)) return false;
        return this.item == o.item && Objects.equals(this.nbt, o.nbt);
    }

    @Override
    public int hashCode() {
        if (this.hashCode == 0) this.hashCode = calculateHashCode();
        return this.hashCode;
    }

    private int calculateHashCode() {
        int ret = System.identityHashCode(this.item);
        if (this.nbt != null) {
            ret = ret * 61 + this.nbt.hashCode();
        }
        if (ret == 0) ret = -1;
        return ret;
    }

    private static boolean checkNbtEquality(CompoundTag a, CompoundTag b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}