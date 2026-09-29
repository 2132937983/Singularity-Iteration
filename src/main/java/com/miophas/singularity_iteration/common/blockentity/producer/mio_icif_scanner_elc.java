// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_memory;
import com.miophas.singularity_iteration.common.menu.producer.ScannerElcMenu;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import com.miophas.singularity_iteration.common.processing.IndependentScanSession;
import com.miophas.singularity_iteration.core.runtime.processing.PatternMenuData;
import com.miophas.singularity_iteration.common.processing.StoredPattern;
import com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

/** Independent adapter from observed scan work to owned progress, inventory and authoritative prices. */
public class mio_icif_scanner_elc extends AbstractProcessingMachineBlockEntity {
    public static final int SLOT_COUNT=3,SCANNER_SLOT=0,BATTERY_SLOT=1,MEMORY_SLOT=2;
    public static final long DEFAULT_CAPACITY=512000,DEFAULT_MAX_RECEIVE=512,DEFAULT_MAX_EXTRACT=0;
    public static final int DEFAULT_SCAN_TIME=3300;
    public static final long DEFAULT_ENERGY_PER_TICK=256,TOTAL_ENERGY_COST=844800;
    public enum State { IDLE,SCANNING,NO_ENERGY,NO_STORAGE,COMPLETED,FAILED,TRANSFER_ERROR,ALREADY_RECORDED }
    /** Compatibility shell over the core scan-result value model. */
    public static class ScanResult extends com.miophas.singularity_iteration.core.runtime.uu.UuScanResult {
        public ScanResult(ItemStack item,double buckets,long energy){super(item,buckets,energy);}
        /** Compatibility only. Machine persistence always supplies its actual registry provider. */
        public CompoundTag serializeNBT(){return new StoredPattern(item,uuMatterCostBuckets,energyCost).save(compatRegistries());}
        public static ScanResult deserializeNBT(CompoundTag tag){var value=StoredPattern.load(tag,compatRegistries());return value==null?null:new ScanResult(value.item(),value.buckets(),value.energy());}
        private static HolderLookup.Provider compatRegistries(){
            var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            return server==null?RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY):server.registryAccess();
        }
    }
    private static final String SAVE_KEY="scex_scanner_v1";
    private static final SlotLayout LAYOUT=SlotLayout.builder().scanner().battery().memory().build();
    private IndependentScanSession session;
    private StoredPattern result;
    private CompoundTag held;
    private boolean changing;
    private long lastTick=Long.MIN_VALUE;
    private State state=State.IDLE;
    private final int[] clientData=new int[15];
    private final IItemHandlerModifiable guardedItems=new IItemHandlerModifiable(){
        public int getSlots(){return itemHandler.getSlots();}
        public ItemStack getStackInSlot(int slot){return itemHandler.getStackInSlot(slot).copy();}
        public int getSlotLimit(int slot){return itemHandler.getSlotLimit(slot);}
        public boolean isItemValid(int slot,ItemStack stack){return !locked(slot)&&itemHandler.isItemValid(slot,stack);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return locked(slot)?stack:itemHandler.insertItem(slot,stack,simulate);}
        public ItemStack extractItem(int slot,int count,boolean simulate){return locked(slot)?ItemStack.EMPTY:itemHandler.extractItem(slot,count,simulate);}
        public void setStackInSlot(int slot,ItemStack stack){
            if(locked(slot))return;var before=itemHandler.getStackInSlot(slot);
            boolean taking=stack.isEmpty()||ItemStack.isSameItemSameComponents(before,stack)&&stack.getCount()<=before.getCount();
            if(taking||isItemValid(slot,stack)&&stack.getCount()<=Math.min(getSlotLimit(slot),stack.getMaxStackSize()))itemHandler.setStackInSlot(slot,stack.copy());
        }
    };
    public mio_icif_scanner_elc(BlockPos pos,BlockState state){this(pos,state,mio_icif_block_entities.SCANNER_ELC_ENTITY_TYPE.get());}
    public mio_icif_scanner_elc(BlockPos pos,BlockState state,BlockEntityType<?> type){
        super(pos,state,type,DEFAULT_CAPACITY,DEFAULT_MAX_RECEIVE,0,DEFAULT_SCAN_TIME,LAYOUT,DEFAULT_ENERGY_PER_TICK,CableTier.HV);
        session=newSession();
    }
    private IndependentScanSession newSession(){return new IndependentScanSession(()->level==null||serverThread(),DEFAULT_SCAN_TIME,DEFAULT_ENERGY_PER_TICK,()->ContainerToTank.markUnsaved(this));}
    protected boolean serverThread(){return level instanceof ServerLevel s&&s.getServer().isSameThread();}
    protected long gameTime(){return level.getGameTime();}
    protected UuQuoteBook.Quote trustedQuote(ItemStack item){return level instanceof ServerLevel s?UuQuoteBook.quote(s.getServer(),item):null;}
    protected UuQuoteBook.Assessment trustedAssessment(ItemStack item) {
        // Preserve the finite quote port used by existing machine contracts and public adapters.
        var finite = trustedQuote(item);
        if (finite != null) return new UuQuoteBook.Assessment(UuQuoteBook.Disposition.FINITE, finite, finite.generation(), false);
        return level instanceof ServerLevel s ? UuQuoteBook.classify(s.getServer(), item)
                : new UuQuoteBook.Assessment(UuQuoteBook.Disposition.UNAVAILABLE, null, -1, false);
    }
    public boolean isDeniedScanComplete() {
        return session != null && session.completionKind() == IndependentScanSession.CompletionKind.KNOWN_DENIED
                && session.state() == IndependentScanSession.State.COMPLETED;
    }
    public boolean hasHeldScanData(){return held!=null||session!=null&&session.state()==IndependentScanSession.State.FAILED;}
    public boolean hasStoredScanData(){return hasHeldScanData()||result!=null||session!=null&&(session.progress()>0||session.paidTick()>0||session.pendingPayment()>0);}
    private boolean active(){return session!=null&&!session.sourceStack().isEmpty()&&session.state()!=IndependentScanSession.State.CANCELLED;}
    private boolean locked(int slot){return changing||hasHeldScanData()||slot==SCANNER_SLOT&&(active()||result!=null);}
    @Override protected MachineItemHandler createItemHandler(SlotLayout layout){
        var handler=new MachineItemHandler(layout){@Override protected void onContentsChanged(int slot){onInventoryChanged(slot);ContainerToTank.markUnsaved(mio_icif_scanner_elc.this);}};
        handler.setValidator(this);return handler;
    }
    @Override public boolean isItemValidForSlot(int slot,ItemStack item){
        if(item.isEmpty())return false;
        return slot==SCANNER_SLOT?item.getCount()==1:slot==MEMORY_SLOT?item.getCount()==1&&item.getItem() instanceof mio_icif_memory:slot==BATTERY_SLOT&&apiIsBattery(item);
    }
    @Override protected int getBatterySlot(){return BATTERY_SLOT;}
    @Override protected int[] getSlotsForDirection(Direction side){return new int[]{SCANNER_SLOT};}
    @Override protected boolean canInsertItem(int slot,ItemStack stack,@Nullable Direction side){return !locked(slot)&&super.canInsertItem(slot,stack,side);}
    @Override protected boolean canExtractItem(int slot,@Nullable Direction side){return !locked(slot)&&slot==SCANNER_SLOT;}
    @Override public IItemHandler getItemHandler(){return guardedItems;}
    @Override public ItemStack getItem(int slot){return guardedItems.getStackInSlot(slot);}
    @Override public ItemStack removeItem(int slot,int amount){return guardedItems.extractItem(slot,amount,false);}
    @Override public ItemStack removeItemNoUpdate(int slot){return guardedItems.extractItem(slot,Integer.MAX_VALUE,false);}
    @Override public void setItem(int slot,ItemStack stack){guardedItems.setStackInSlot(slot,stack);}
    @Override public void clearContent(){if(!changing&&!hasStoredScanData())super.clearContent();}
    protected java.util.List<mio_icif_pattern_storage> nearbyStorage(){
        var found=new java.util.ArrayList<mio_icif_pattern_storage>();
        if(level instanceof ServerLevel s)for(var side:Direction.values()){
            var pos=worldPosition.relative(side);var chunk=s.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if(chunk!=null&&chunk.getBlockEntity(pos) instanceof mio_icif_pattern_storage storage&&!storage.hasUnresolvedPatterns())found.add(storage);
        }
        return found;
    }
    private boolean storageAvailable(ItemStack item){
        var crystal=itemHandler.getStackInSlot(MEMORY_SLOT);
        if(!crystal.isEmpty()){
            if(crystal.getCount()==1&&crystal.getItem() instanceof mio_icif_memory memory){
                if(!memory.hasData(crystal)&&!crystal.has(net.minecraft.core.component.DataComponents.CONTAINER))return true;
                state=ItemStack.isSameItemSameComponents(item,memory.getStoredItemStack(crystal))?State.ALREADY_RECORDED:State.TRANSFER_ERROR;
            }else state=State.TRANSFER_ERROR;
            return false;
        }
        var storage=nearbyStorage();
        for(var library:storage)if(library.hasPattern(item)){state=State.ALREADY_RECORDED;return false;}
        for(var library:storage)if(!library.isFull())return true;
        state=State.NO_STORAGE;return false;
    }
    @Override protected void tickProduction(){
        isWorking=false;
        if(!serverThread()||changing||gameTime()==lastTick)return;
        lastTick=gameTime();
        if(hasHeldScanData()){state=State.FAILED;return;}
        if(result!=null){state=State.COMPLETED;return;}
        if(isDeniedScanComplete()){state=State.FAILED;return;}
        var input=itemHandler.getStackInSlot(SCANNER_SLOT);
        if(active()&&(input.isEmpty()||!ItemStack.isSameItemSameComponents(input,session.sourceStack()))){hold("Paid input identity changed");return;}
        if(input.isEmpty()){state=State.IDLE;return;}
        var assessment=trustedAssessment(input.copyWithCount(1));
        boolean denied=assessment.disposition()==UuQuoteBook.Disposition.KNOWN_DENIED && assessment.deniedScanEligible();
        var quote=assessment.finite();
        if(quote==null&&!denied){state=State.FAILED;return;}
        // A valid new generation can resume matching work; it cannot reinterpret a paid completion kind.
        if(active()&&denied!=(session.completionKind()==IndependentScanSession.CompletionKind.KNOWN_DENIED)){state=State.FAILED;return;}
        if(!storageAvailable(input))return;
        if(!active()&&!(denied?session.beginDenied(input.copyWithCount(1)):session.begin(input.copyWithCount(1),quote.buckets(),TOTAL_ENERGY_COST))){state=State.FAILED;return;}
        changing=true;
        try{
            long debit=session.tick(lastTick,new IndependentScanSession.EnergyPort(){
                public long available(){return energyStorage.getAmount();}
                public long consume(long amount){return energyStorage.consumeEnergyInternal(amount,false);}
            });
            isWorking=debit>0;updateProgress();
            state=session.state()==IndependentScanSession.State.WAITING_ENERGY?State.NO_ENERGY:session.state()==IndependentScanSession.State.FAILED?State.FAILED:State.SCANNING;
            if(session.state()==IndependentScanSession.State.COMPLETED){
                if(session.completionKind()==IndependentScanSession.CompletionKind.KNOWN_DENIED){
                    // Expected paid negative result: keep the input and terminal work, never create a StoredPattern.
                    state=State.FAILED;progress=DEFAULT_SCAN_TIME;ContainerToTank.markUnsaved(this);return;
                }
                var before=input.copy();var after=input.copy();after.shrink(1);
                var completed=new StoredPattern(session.sourceStack(),quote.buckets(),TOTAL_ENERGY_COST);
                if(!itemHandler.scexCommitSlots(new int[]{SCANNER_SLOT},new ItemStack[]{before},new ItemStack[]{after},()->{
                    result=completed;session.takeStoredPattern();state=State.COMPLETED;progress=DEFAULT_SCAN_TIME;
                }))hold("Completed scan could not commit its input");
            }
            ContainerToTank.markUnsaved(this);
        }finally{changing=false;}
    }
    public boolean storeResult(){
        if(!serverThread()||changing||hasHeldScanData()||result==null)return false;
        var quote=trustedQuote(result.item());if(quote==null)return false;
        var updated=new StoredPattern(result.item(),quote.buckets(),TOTAL_ENERGY_COST);
        changing=true;
        try{
            var before=itemHandler.getStackInSlot(MEMORY_SLOT).copy();
            if(!before.isEmpty()){
                if(before.getCount()!=1||!(before.getItem() instanceof mio_icif_memory memory)||memory.hasData(before)){state=State.TRANSFER_ERROR;return false;}
                var after=before.copy();
                if(!memory.tryStoreData(after,updated.item(),updated.buckets(),updated.energy())){state=State.TRANSFER_ERROR;return false;}
                return itemHandler.scexCommitSlots(new int[]{MEMORY_SLOT},new ItemStack[]{before},new ItemStack[]{after},this::finishStorage);
            }
            for(var storage:nearbyStorage())if(storage.storePattern(updated.legacyView())){finishStorage();return true;}
            state=State.TRANSFER_ERROR;return false;
        }finally{changing=false;ContainerToTank.markUnsaved(this);}
    }
    private void finishStorage(){result=null;session=newSession();progress=0;state=State.IDLE;}
    public void discardResult(){if(serverThread()&&!changing&&!hasHeldScanData()){finishStorage();isWorking=false;ContainerToTank.markUnsaved(this);}}
    public void clearScanResult(){discardResult();}
    public void reset(){discardResult();}
    public boolean isScanComplete(){return result!=null;}
    public ScanResult getScanResult(){return result==null?null:new ScanResult(result.item(),result.buckets(),result.energy());}
    public ItemStack getScannedItem(){return result==null?session.sourceStack():result.item();}
    public double getUUMatterCost(){var quote=trustedQuote(getScannedItem());return quote==null?0:quote.buckets();}
    public long getEnergyCost(){return getScannedItem().isEmpty()?0:TOTAL_ENERGY_COST;}
    public State getScanState(){return state;}
    public int getStateOrdinal(){return state.ordinal();}
    public int getPercentageDone(){return getProgress()*100/DEFAULT_SCAN_TIME;}
    public boolean isDone(){return isScanComplete();}
    @Override protected void checkInputChanged(){/* Session owns the input until explicit cancellation or completion. */}
    @Override protected boolean shouldResetProgress(){return false;}
    @Override protected void doWork(){tickProduction();}
    @Override protected boolean canWork(){return !hasHeldScanData()&&result==null;}
    @Override protected void updateProgress(){progress=result==null?session.progress():DEFAULT_SCAN_TIME;maxProgress=DEFAULT_SCAN_TIME;}
    @Override public int getMaxProgress(){return DEFAULT_SCAN_TIME;}
    private void hold(String reason){held=new CompoundTag();held.putString("reason",reason);state=State.FAILED;isWorking=false;ContainerToTank.markUnsaved(this);}
    @Override public void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);var own=new CompoundTag();own.putInt("version",1);own.put("session",session.save(registries));
        if(result!=null)own.put("result",result.save(registries));if(held!=null)own.put("held",held.copy());tag.put(SAVE_KEY,own);
    }
    @Override public void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);session=newSession();result=null;held=null;isWorking=false;state=State.IDLE;lastTick=Long.MIN_VALUE;
        if(!tag.contains(SAVE_KEY,Tag.TAG_COMPOUND)){if(!tag.isEmpty())held=tag.copy();state=held==null?State.IDLE:State.FAILED;updateProgress();return;}
        var own=tag.getCompound(SAVE_KEY);
        boolean valid=java.util.Set.of("version","session","result","held").containsAll(own.getAllKeys())
            &&own.contains("version",Tag.TAG_INT)&&own.getInt("version")==1&&own.contains("session",Tag.TAG_COMPOUND)
            &&(!own.contains("result")||own.contains("result",Tag.TAG_COMPOUND))&&(!own.contains("held")||own.contains("held",Tag.TAG_COMPOUND));
        if(!valid){held=tag.copy();state=State.FAILED;updateProgress();return;}
        session.load(own.getCompound("session"),registries);
        if(own.contains("result")){result=StoredPattern.load(own.getCompound("result"),registries);if(result==null||session.state()!=IndependentScanSession.State.IDLE){held=tag.copy();result=null;}}
        if(own.contains("held")&&held==null)held=own.getCompound("held").copy();
        if(active()&&(itemHandler.getStackInSlot(SCANNER_SLOT).isEmpty()||!ItemStack.isSameItemSameComponents(itemHandler.getStackInSlot(SCANNER_SLOT),session.sourceStack())))held=tag.copy();
        state=hasHeldScanData()||isDeniedScanComplete()?State.FAILED:result!=null?State.COMPLETED:active()?State.NO_ENERGY:State.IDLE;updateProgress();
    }
    public ContainerData getContainerData(){return new ContainerData(){
        public int getCount(){return 15;}
        public void set(int index,int value){clientData[index]=value;}
        public int get(int i){
            if(i<0||i>=15)throw new IndexOutOfBoundsException(i);if(level!=null&&level.isClientSide())return clientData[i];
            if(i<3)return i==0?progress:i==1?DEFAULT_SCAN_TIME:isWorking?1:0;
            if(i<5)return PatternMenuData.word(energyStorage.getAmount(),i-3);
            if(i<7)return PatternMenuData.word(energyStorage.getCapacity(),i-5);
            if(i==7)return isScanComplete()?1:0;if(i==8)return state.ordinal();
            if(i<13)return PatternMenuData.word(Double.doubleToRawLongBits(getUUMatterCost()),i-9);
            return PatternMenuData.word(getEnergyCost(),i-13);
        }
    };}
    @Override public Component getDisplayName(){return Component.translatable("container.mio_icif.scanner_elc");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new ScannerElcMenu(id,inventory,this);}
    public static void tick(Level level,BlockPos pos,BlockState state,mio_icif_scanner_elc machine){if(!level.isClientSide())AbstractProcessingMachineBlockEntity.tick(level,pos,state,machine);}
}
