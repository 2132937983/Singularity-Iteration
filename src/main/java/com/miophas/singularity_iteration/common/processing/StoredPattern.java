// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.core.runtime.uu.UuScanResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Owned one-item pattern. Costs are supplied by a caller, never computed here. */
public final class StoredPattern {
    private final ItemStack item;
    private final double buckets;
    private final long energy;
    public StoredPattern(ItemStack item,double buckets,long energy) {
        if(!valid(item,buckets,energy))throw new IllegalArgumentException("Invalid one-item scan pattern");
        this.item=item.copy();this.buckets=buckets;this.energy=energy;
    }
    public static boolean valid(ItemStack item,double buckets,long energy) {
        return UuScanResult.valid(item,buckets,energy);
    }
    public static boolean validCosts(double buckets,long energy) {
        return UuScanResult.validCosts(buckets,energy);
    }
    /** 扫描结果统一使用 core 值模型；不再反向依赖内置扫描机类型。 */
    public static StoredPattern from(UuScanResult value) {
        return value==null?null:
            new StoredPattern(value.item,value.uuMatterCostBuckets,value.energyCost);
    }
    public ItemStack item(){return item.copy();}
    public double buckets(){return buckets;}
    public long energy(){return energy;}
    public boolean sameItem(ItemStack other){return ItemStack.isSameItemSameComponents(item,other);}
    public boolean same(StoredPattern other){return other!=null&&sameItem(other.item)&&Double.compare(buckets,other.buckets)==0&&energy==other.energy;}
    /** Preserve the old SI public return type without retaining caller-owned stacks. */
    public UuScanResult legacyView(){return new UuScanResult(item.copy(),buckets,energy);}
    public CompoundTag save(HolderLookup.Provider registries) {
        var tag=new CompoundTag();tag.putInt("scex_pattern_version",1);tag.put("item",item.save(registries));
        tag.putDouble("uu_matter_cost_buckets",buckets);tag.putLong("energy_cost",energy);return tag;
    }
    /** The unversioned keys were observed through the SI R5 public saved-NBT boundary. */
    public static StoredPattern load(CompoundTag tag,HolderLookup.Provider registries) {
        for(var key:tag.getAllKeys())if(!java.util.Set.of("scex_pattern_version","item","uu_matter_cost_buckets","energy_cost").contains(key))return null;
        if(tag.contains("scex_pattern_version")&&(!tag.contains("scex_pattern_version",Tag.TAG_INT)||tag.getInt("scex_pattern_version")!=1))return null;
        if(!tag.contains("item",Tag.TAG_COMPOUND)||!tag.contains("uu_matter_cost_buckets",Tag.TAG_DOUBLE)||!tag.contains("energy_cost",Tag.TAG_LONG))return null;
        try {
            var item=ItemStack.parseOptional(registries,tag.getCompound("item"));
            double buckets=tag.getDouble("uu_matter_cost_buckets");long energy=tag.getLong("energy_cost");
            return valid(item,buckets,energy)?new StoredPattern(item,buckets,energy):null;
        }catch(RuntimeException invalid){return null;}
    }
}
