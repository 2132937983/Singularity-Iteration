// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Explicit observed eligibility, independently bounded from quote exclusion and preserving components. */
public final class UuScanEligibility {
    private static final int MAX_BYTES=65536, MAX_ITEMS=1024;
    private UuScanEligibility() { }

    @SuppressWarnings("deprecation")
    public static Set<IndependentUuValueIndex.Key> read(Reader input, HolderLookup.Provider registries,
            Set<IndependentUuValueIndex.Key> denied) throws IOException {
        var text=new StringBuilder();char[] buffer=new char[4096];
        for(int count;(count=input.read(buffer))>=0;){
            text.append(buffer,0,count);
            if(text.length()>MAX_BYTES)throw new IllegalArgumentException("Scanner eligibility too large");
        }
        if(text.toString().getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)
            throw new IllegalArgumentException("Scanner eligibility too large");
        var fields=new HashSet<String>();var ids=new HashSet<String>();
        var result=new HashSet<IndependentUuValueIndex.Key>();
        var items=registries.lookupOrThrow(Registries.ITEM);
        int schema=-1;
        try(var reader=new JsonReader(new StringReader(text.toString()))){
            reader.setLenient(false);
            if(reader.peek()!=JsonToken.BEGIN_OBJECT)throw new IllegalArgumentException("Eligibility object required");
            reader.beginObject();
            while(reader.hasNext()){
                String field=reader.nextName();
                if(!fields.add(field))throw new IllegalArgumentException("Duplicate eligibility field");
                switch(field){
                    case "schema" -> {
                        if(reader.peek()!=JsonToken.NUMBER)
                            throw new IllegalArgumentException("Unsupported scanner eligibility schema");
                        String version=reader.nextString();
                        if(!version.equals("1")&&!version.equals("2"))throw new IllegalArgumentException("Unsupported scanner eligibility schema");
                        schema=Integer.parseInt(version);
                    }
                    case "basis" -> {
                        if(reader.peek()!=JsonToken.STRING)throw new IllegalArgumentException("Eligibility basis must be text");
                        reader.nextString();
                    }
                    case "denied_scan_items" -> {
                        if(reader.peek()!=JsonToken.BEGIN_ARRAY)throw new IllegalArgumentException("Eligibility array required");
                        reader.beginArray();
                        while(reader.hasNext()){
                            if(reader.peek()!=JsonToken.STRING || ids.size()>=MAX_ITEMS)
                                throw new IllegalArgumentException("Invalid bounded eligibility identity");
                            String value=reader.nextString();
                            if(!ids.add(value) || value.indexOf(':')<=0)
                                throw new IllegalArgumentException("Unique explicit item ID required");
                            var id=ResourceLocation.parse(value);
                            var holder=items.get(ResourceKey.create(Registries.ITEM,id));
                            if(holder.isEmpty())throw new IllegalArgumentException("Unknown scanner eligibility item");
                            var item=new ItemStack(holder.get().value());
                            if(item.isEmpty())throw new IllegalArgumentException("Empty scanner eligibility item");
                            var key=IndependentUuValueIndex.keyOf(item);
                            if(!denied.contains(key) || !result.add(key))
                                throw new IllegalArgumentException("Eligible identity must be a unique known denial");
                        }
                        reader.endArray();
                    }
                    case "denied_scan_stacks" -> {
                        reader.beginArray();
                        while(reader.hasNext()){
                            if(result.size()>=MAX_ITEMS)throw new IllegalArgumentException("Eligibility count limit");
                            reader.beginObject();var rowFields=new HashSet<String>();String target=null,saved=null;
                            while(reader.hasNext()){
                                String key=reader.nextName();
                                if(!rowFields.add(key)||reader.peek()!=JsonToken.STRING)
                                    throw new IllegalArgumentException("Explicit eligibility fields must be unique strings");
                                switch(key){
                                    case "target_item" -> target=reader.nextString();
                                    case "target_stack" -> saved=reader.nextString();
                                    default -> throw new IllegalArgumentException("Unknown explicit eligibility field");
                                }
                            }
                            reader.endObject();
                            if(!rowFields.equals(Set.of("target_item","target_stack"))||!ids.add(target))
                                throw new IllegalArgumentException("Unique complete eligibility identity required");
                            var item=UuMappedCatalog.explicitStack(saved,target,registries);
                            var key=IndependentUuValueIndex.keyOf(item);
                            if(!denied.contains(key)||!result.add(key))
                                throw new IllegalArgumentException("Explicit eligible identity must be a unique known denial");
                        }
                        reader.endArray();
                    }
                    default -> throw new IllegalArgumentException("Unknown scanner eligibility field");
                }
            }
            reader.endObject();
            if(reader.peek()!=JsonToken.END_DOCUMENT || !fields.containsAll(Set.of("schema","denied_scan_items"))
                    || schema==1 && fields.contains("denied_scan_stacks") || result.size()>MAX_ITEMS)
                throw new IllegalArgumentException("Incomplete scanner eligibility document");
        }
        return Set.copyOf(result);
    }
}
