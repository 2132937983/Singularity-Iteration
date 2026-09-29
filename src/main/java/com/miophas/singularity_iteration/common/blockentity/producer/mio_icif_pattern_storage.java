// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_memory;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.processing.MachineMenuAccess;
import com.miophas.singularity_iteration.core.runtime.processing.PatternMenuData;
import com.miophas.singularity_iteration.common.processing.StoredPattern;
import com.miophas.singularity_iteration.core.runtime.uu.UuScanResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** Bounded pattern ownership, registry-aware saves and paid copy operations. */
public class mio_icif_pattern_storage extends AbstractEnergyBlockEntity implements Container {
    @Override protected boolean supportsFeCapability() { return true; }
    public static final int MAX_PATTERNS=64,SLOT_COUNT=1,MEMORY_SLOT=0;
    public static final long ENERGY_PER_OPERATION=100,DEFAULT_CAPACITY=100000,DEFAULT_MAX_RECEIVE=128,DEFAULT_MAX_EXTRACT=0;
    private static final SlotLayout LAYOUT=SlotLayout.builder().memory().build();
    private final List<StoredPattern> storedPatterns=new ArrayList<>();
    private CompoundTag unresolvedPatterns=new CompoundTag();
    private int currentIndex;
    private long revision;
    private boolean changing;
    private long quotedGeneration=Long.MIN_VALUE;
    protected MachineItemHandler itemHandler;
    private final ContainerData dataAccess=new ContainerData(){
        @Override public int getCount(){return PatternMenuData.COUNT;}
        @Override public void set(int index,int value){}
        @Override public int get(int index){
            if(index<0||index>=getCount())throw new IndexOutOfBoundsException(index);
            if(index<2)return PatternMenuData.word(energyStorage.getAmount(),index);
            if(index<4)return PatternMenuData.word(energyStorage.getCapacity(),index-2);
            if(index==4)return currentIndex;
            if(index==5)return storedPatterns.size();
            if(index<10)return PatternMenuData.word(Double.doubleToRawLongBits(getCurrentUuCost()),index-6);
            return PatternMenuData.word(getCurrentEuCost(),index-10);
        }
    };
    public mio_icif_pattern_storage(BlockPos pos,BlockState state){this(pos,state,mio_icif_block_entities.PATTERN_STORAGE_ENTITY_TYPE.get());}
    public mio_icif_pattern_storage(BlockPos pos,BlockState state,BlockEntityType<?> type){
        super(pos,state,type,DEFAULT_CAPACITY,DEFAULT_MAX_RECEIVE,DEFAULT_MAX_EXTRACT,CableTier.LV);
        itemHandler=new MachineItemHandler(LAYOUT){@Override protected void onContentsChanged(int slot){setChanged();}};
        itemHandler.setValidator((slot,stack,kind)->isItemValidForSlot(slot,stack));
    }
    protected boolean operational(){
        return !hasUnresolvedPatterns()&&liveOwner();
    }
    private boolean liveOwner(){
        if(isRemoved()||!(level instanceof ServerLevel world)||!world.getServer().isSameThread())return false;
        var chunk=world.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4);
        return chunk!=null&&world.shouldTickBlocksAt(ChunkPos.asLong(worldPosition))
            &&chunk.getBlockEntity(worldPosition,LevelChunk.EntityCreationType.CHECK)==this;
    }
    protected HolderLookup.Provider patternRegistries(){return level.registryAccess();}
    protected com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.Quote trustedQuote(ItemStack item){
        return level instanceof ServerLevel world?com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.quote(world.getServer(),item):null;
    }
    private void refreshStoredQuotes(){
        if(changing||!liveOwner()||!(level instanceof ServerLevel world))return;
        long generation=com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.generation(world.getServer());
        if(generation<0||generation==quotedGeneration)return;
        quotedGeneration=generation;
        if(hasUnresolvedPatterns()){
            if(!unresolvedPatterns.getAllKeys().equals(java.util.Set.of("patterns"))||!(unresolvedPatterns.get("patterns") instanceof ListTag records))return;
            var recovered=com.miophas.singularity_iteration.common.processing.UuPatternMigration.recover(records,patternRegistries(),this::trustedQuote);
            if(recovered==null)return;
            storedPatterns.clear();storedPatterns.addAll(recovered);unresolvedPatterns=new CompoundTag();clampIndex();revision++;setChanged();
        }
        for(int i=0;i<storedPatterns.size();i++){
            var old=storedPatterns.get(i);var updated=com.miophas.singularity_iteration.common.processing.UuPatternMigration.reprice(old,trustedQuote(old.item()));
            if(updated!=old){storedPatterns.set(i,updated);revision++;setChanged();}
        }
    }
    public boolean hasUnresolvedPatterns(){return !unresolvedPatterns.isEmpty();}
    public boolean isItemValidForSlot(int slot,ItemStack stack){return slot==MEMORY_SLOT&&stack.getItem() instanceof mio_icif_memory;}
    public boolean hasEnoughEnergy(){return energyStorage.getAmount()>=ENERGY_PER_OPERATION;}
    public boolean consumeOperationEnergy(){return !changing&&operational()&&hasEnoughEnergy()&&energyStorage.consumeEnergyInternal(ENERGY_PER_OPERATION,false)==ENERGY_PER_OPERATION;}
    private int findIndex(ItemStack item){for(int i=0;i<storedPatterns.size();i++)if(storedPatterns.get(i).sameItem(item))return i;return -1;}
    public boolean storePattern(UuScanResult result){return storePrepared(StoredPattern.from(result));}
    private boolean storePrepared(StoredPattern prepared){
        if(prepared==null||changing||!operational())return false;
        prepared=com.miophas.singularity_iteration.common.processing.UuPatternMigration.reprice(prepared,trustedQuote(prepared.item()));
        int index=findIndex(prepared.item());
        if(index>=0&&storedPatterns.get(index).same(prepared))return true;
        if(index<0&&isFull()||!hasEnoughEnergy())return false;
        long expectedRevision=revision;var balance=energyStorage.scexNetworkQuote();changing=true;
        try{
            try{prepared.save(patternRegistries());}catch(RuntimeException invalid){return false;}
            if(revision!=expectedRevision||!operational()||!balance.equals(energyStorage.scexNetworkQuote())||!hasEnoughEnergy())return false;
            if(energyStorage.consumeEnergyInternal(ENERGY_PER_OPERATION,false)!=ENERGY_PER_OPERATION)throw new IllegalStateException("Owned pattern payment changed");
            if(index>=0)storedPatterns.set(index,prepared);else storedPatterns.add(prepared);
            revision++;setChanged();return true;
        }finally{changing=false;}
    }
    public boolean removePattern(int index){
        if(changing||!operational()||index<0||index>=storedPatterns.size()||!hasEnoughEnergy())return false;
        if(energyStorage.consumeEnergyInternal(ENERGY_PER_OPERATION,false)!=ENERGY_PER_OPERATION)return false;
        storedPatterns.remove(index);clampIndex();revision++;setChanged();return true;
    }
    public List<UuScanResult> getStoredPatterns(){
        var copy=new ArrayList<UuScanResult>();for(var pattern:storedPatterns)copy.add(pattern.legacyView());return copy;
    }
    @Nullable public UuScanResult getPattern(int index){return index<0||index>=storedPatterns.size()?null:storedPatterns.get(index).legacyView();}
    @Nullable public UuScanResult findPattern(ItemStack item){int index=findIndex(item);return index<0?null:getPattern(index);}
    public boolean hasPattern(ItemStack item){return findIndex(item)>=0;}
    public int getStoredCount(){return storedPatterns.size();}
    public boolean isFull(){return storedPatterns.size()>=MAX_PATTERNS;}
    public void clear(){if(changing||!operational())return;storedPatterns.clear();currentIndex=0;revision++;setChanged();}
    public void receiveSyncData(List<UuScanResult> patterns,int index){
        if(level==null||!level.isClientSide()||patterns.size()>MAX_PATTERNS)return;
        var decoded=new ArrayList<StoredPattern>();
        for(var value:patterns){var pattern=StoredPattern.from(value);if(pattern==null)return;decoded.add(pattern);}
        storedPatterns.clear();storedPatterns.addAll(decoded);currentIndex=index;clampIndex();revision++;
    }
    public int getCurrentIndex(){return currentIndex;}
    public void setCurrentIndex(int index){
        if(changing||!operational())return;int old=currentIndex;currentIndex=index;clampIndex();
        if(old!=currentIndex){revision++;setChanged();}
    }
    private void clampIndex(){currentIndex=storedPatterns.isEmpty()?0:Math.max(0,Math.min(currentIndex,storedPatterns.size()-1));}
    public void previousPattern(){if(!storedPatterns.isEmpty())setCurrentIndex((currentIndex+storedPatterns.size()-1)%storedPatterns.size());}
    public void nextPattern(){if(!storedPatterns.isEmpty())setCurrentIndex((currentIndex+1)%storedPatterns.size());}
    private StoredPattern current(){return currentIndex<0||currentIndex>=storedPatterns.size()?null:storedPatterns.get(currentIndex);}
    public ItemStack getCurrentPattern(){var value=current();return value==null?ItemStack.EMPTY:value.item();}
    public double getCurrentUuCost(){var value=current();return value==null?0:value.buckets();}
    public long getCurrentEuCost(){var value=current();return value==null?0:value.energy();}
    public boolean exportCurrentPattern(){
        refreshStoredQuotes();
        var pattern=current();var original=itemHandler.getStackInSlot(MEMORY_SLOT);
        if(changing||!operational()||pattern==null||!hasEnoughEnergy()||original.getCount()!=1
                ||!(original.getItem() instanceof mio_icif_memory memory)||memory.hasData(original))return false;
        var before=original.copy();var after=before.copy();long expectedRevision=revision;var balance=energyStorage.scexNetworkQuote();changing=true;
        try{
            if(!memory.tryStoreData(after,pattern.item(),pattern.buckets(),pattern.energy()))return false;
            var savedItem=memory.getStoredItemStack(after);
            if(!StoredPattern.valid(savedItem,memory.getUuMatterCost(after),memory.getEnergyCost(after))
                    ||!pattern.same(new StoredPattern(savedItem,memory.getUuMatterCost(after),memory.getEnergyCost(after))))return false;
            if(revision!=expectedRevision||!operational()||!ItemStack.matches(before,itemHandler.getStackInSlot(MEMORY_SLOT))
                    ||!balance.equals(energyStorage.scexNetworkQuote())||!hasEnoughEnergy())return false;
            if(energyStorage.consumeEnergyInternal(ENERGY_PER_OPERATION,false)!=ENERGY_PER_OPERATION)throw new IllegalStateException("Owned export payment changed");
            if(!itemHandler.scexCommitSlots(new int[]{MEMORY_SLOT},new ItemStack[]{before},new ItemStack[]{after}))throw new IllegalStateException("Owned memory slot changed");
            setChanged();return true;
        }finally{changing=false;}
    }
    public boolean importMemoryPattern(){
        if(changing||!operational())return false;
        var before=itemHandler.getStackInSlot(MEMORY_SLOT).copy();
        if(before.getCount()!=1||!(before.getItem() instanceof mio_icif_memory memory))return false;
        var migration=com.miophas.singularity_iteration.common.processing.UuPatternMigration.prepare(before,this::trustedQuote);
        var priced=migration.replacement();
        var item=memory.getStoredItemStack(priced);double buckets=memory.getUuMatterCost(priced);long energy=memory.getEnergyCost(priced);
        if(!StoredPattern.valid(item,buckets,energy)||!ItemStack.matches(before,itemHandler.getStackInSlot(MEMORY_SLOT)))return false;
        if(!storePrepared(new StoredPattern(item,buckets,energy)))return false;
        if(migration.status()==com.miophas.singularity_iteration.common.processing.UuPatternMigration.Status.UPDATED)
            itemHandler.scexCommitSlots(new int[]{MEMORY_SLOT},new ItemStack[]{before},new ItemStack[]{priced});
        return true;
    }
    public IItemHandler getItemHandler(){return itemHandler;}
    public IItemHandler getItemHandlerCapability(@Nullable Direction side){return itemHandler;}
    public ContainerData getContainerData(){return dataAccess;}
    public void savePatternData(CompoundTag tag,HolderLookup.Provider registries){
        var patterns=new ListTag();for(var pattern:storedPatterns)patterns.add(pattern.save(registries));
        tag.put("patterns",patterns);tag.putInt("current_index",currentIndex);
        if(hasUnresolvedPatterns())tag.put("scex_unresolved_patterns",unresolvedPatterns.copy());
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);savePatternData(tag,registries);tag.put("inventory",itemHandler.serializeNBT(registries));
    }
    private void loadPatterns(CompoundTag tag,HolderLookup.Provider registries){
        storedPatterns.clear();unresolvedPatterns=new CompoundTag();quotedGeneration=Long.MIN_VALUE;
        if(tag.contains("scex_unresolved_patterns",Tag.TAG_COMPOUND))unresolvedPatterns=tag.getCompound("scex_unresolved_patterns").copy();
        else if(tag.contains("scex_unresolved_patterns"))unresolvedPatterns.put("unrecognized",tag.get("scex_unresolved_patterns").copy());
        var raw=tag.get("patterns");boolean invalid=raw!=null&&!(raw instanceof ListTag);
        if(raw instanceof ListTag entries){
            invalid=entries.size()>MAX_PATTERNS;
            if(!invalid)for(var entry:entries){
                var value=entry instanceof CompoundTag compound?StoredPattern.load(compound,registries):null;
                if(value==null||findIndex(value.item())>=0){invalid=true;break;}storedPatterns.add(value);
            }
        }
        if(invalid){storedPatterns.clear();unresolvedPatterns.put("patterns",raw.copy());}
        currentIndex=tag.getInt("current_index");clampIndex();revision++;
    }
    @Override public void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);loadPatterns(tag,registries);
        if(tag.contains("inventory",Tag.TAG_COMPOUND))itemHandler.deserializeNBT(registries,tag.getCompound("inventory"));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=super.getUpdateTag(registries);savePatternData(tag,registries);return tag;}
    @Override public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider registries){super.handleUpdateTag(tag,registries);loadPatterns(tag,registries);}
    @Override public Component getDisplayName(){return Component.translatable("container.mio_icif.pattern_storage");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new com.miophas.singularity_iteration.common.menu.producer.PatternStorageMenu(id,inventory,this);}
    public static void tick(Level level,BlockPos pos,BlockState state,mio_icif_pattern_storage storage){if(!level.isClientSide()){AbstractEnergyBlockEntity.tick(level,pos,state,storage);storage.refreshStoredQuotes();}}
    @Override public int getContainerSize(){return SLOT_COUNT;}
    @Override public boolean isEmpty(){return itemHandler.getStackInSlot(0).isEmpty();}
    @Override public ItemStack getItem(int slot){return itemHandler.getStackInSlot(slot);}
    @Override public ItemStack removeItem(int slot,int amount){return itemHandler.extractItem(slot,amount,false);}
    @Override public ItemStack removeItemNoUpdate(int slot){var item=itemHandler.getStackInSlot(slot).copy();itemHandler.setStackInSlot(slot,ItemStack.EMPTY);return item;}
    @Override public void setItem(int slot,ItemStack item){itemHandler.setStackInSlot(slot,item.copy());}
    @Override public boolean stillValid(Player player){return MachineMenuAccess.valid(player,this);}
    @Override public void clearContent(){itemHandler.setStackInSlot(0,ItemStack.EMPTY);}
    @Override public boolean canPlaceItem(int slot,ItemStack item){return isItemValidForSlot(slot,item);}
}
