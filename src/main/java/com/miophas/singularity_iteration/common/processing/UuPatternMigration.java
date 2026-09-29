// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.common.item.resource.mio_icif_memory;
import com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook;
import java.util.function.Function;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Prepare a crystal replacement without mutating caller-owned inventory or trusting legacy costs. */
public final class UuPatternMigration {
    public enum Status { EMPTY,UNCHANGED,UPDATED,WAITING_QUOTE,HELD_IDENTITY }
    public record Result(Status status,ItemStack replacement){
        public Result{replacement=replacement.copy();}
        @Override public ItemStack replacement(){return replacement.copy();}
    }
    private UuPatternMigration(){}
    public static Result prepare(ItemStack original,Function<ItemStack,UuQuoteBook.Quote> quotes){
        if(original.isEmpty()||!(original.getItem() instanceof mio_icif_memory memory)||!memory.hasData(original))return new Result(Status.EMPTY,original);
        ItemStack item=memory.getStoredItemIdentity(original);
        var data=original.get(DataComponents.CUSTOM_DATA);var tag=data==null?null:data.copyTag();
        if(item.isEmpty()||tag==null||!tag.contains("energy_cost",Tag.TAG_LONG)||tag.getLong("energy_cost")<0)return new Result(Status.HELD_IDENTITY,original);
        var quote=quotes.apply(item.copy());
        if(quote==null)return new Result(Status.WAITING_QUOTE,original);
        if(!StoredPattern.validCosts(quote.buckets(),tag.getLong("energy_cost")))return new Result(Status.WAITING_QUOTE,original);
        var next=original.copy();
        if(!memory.tryStoreData(next,item,quote.buckets(),tag.getLong("energy_cost")))return new Result(Status.HELD_IDENTITY,original);
        return new Result(ItemStack.matches(original,next)?Status.UNCHANGED:Status.UPDATED,next);
    }
    public static StoredPattern reprice(StoredPattern original,UuQuoteBook.Quote quote){
        if(original==null||quote==null||!StoredPattern.validCosts(quote.buckets(),original.energy()))return original;
        return Double.compare(original.buckets(),quote.buckets())==0?original:new StoredPattern(original.item(),quote.buckets(),original.energy());
    }
    /** All-or-nothing recovery of known legacy pattern records; unknown fields or identities retain the whole list. */
    public static java.util.List<StoredPattern> recover(ListTag records,HolderLookup.Provider registries,Function<ItemStack,UuQuoteBook.Quote> quotes){
        if(records.size()>64)return null;
        var result=new java.util.ArrayList<StoredPattern>();
        for(var raw:records){
            if(!(raw instanceof CompoundTag tag)||!java.util.Set.of("scex_pattern_version","item","uu_matter_cost_buckets","energy_cost").containsAll(tag.getAllKeys())
                ||!tag.contains("item",Tag.TAG_COMPOUND)||!tag.contains("uu_matter_cost_buckets",Tag.TAG_DOUBLE)||!tag.contains("energy_cost",Tag.TAG_LONG)
                ||tag.getLong("energy_cost")<0||tag.contains("scex_pattern_version")&&(!tag.contains("scex_pattern_version",Tag.TAG_INT)||tag.getInt("scex_pattern_version")!=1))return null;
            ItemStack item;
            try{item=ItemStack.parse(registries,tag.getCompound("item")).orElse(ItemStack.EMPTY);}catch(RuntimeException invalid){return null;}
            if(item.isEmpty()||item.getCount()!=1)return null;
            for(var prior:result)if(prior.sameItem(item))return null;
            var quote=quotes.apply(item.copy());if(quote==null||!StoredPattern.validCosts(quote.buckets(),tag.getLong("energy_cost")))return null;
            result.add(new StoredPattern(item,quote.buckets(),tag.getLong("energy_cost")));
        }
        return java.util.List.copyOf(result);
    }
}
