// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_memory;
import com.miophas.singularity_iteration.common.menu.producer.ReplicatorElcMenu;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import com.miophas.singularity_iteration.core.runtime.processing.IndependentUuBuffer;
import com.miophas.singularity_iteration.common.processing.StoredPattern;
import com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/** Independent R94 paid-copy implementation; predecessor body was not used. */
public class mio_icif_replicator_elc extends AbstractProcessingMachineBlockEntity {
    public enum WorkMode { STOPPED, SINGLE, LOOP }
    public static final int SLOT_COUNT=9,UU_CELL_SLOT=0,EMPTY_CELL_SLOT=1,MEMORY_SLOT=2,OUTPUT_SLOT=3,BATTERY_SLOT=4,UPGRADE_SLOT_START=5;
    public static final long DEFAULT_CAPACITY=2000000,DEFAULT_MAX_RECEIVE=8192,DEFAULT_MAX_EXTRACT=0,DEFAULT_ENERGY_PER_TICK=512;
    public static final int DEFAULT_WORK_TIME=100,UUMATTER_CAPACITY=64000;
    private static final double BASE_RATE=.0001;
    private static final String SAVE_KEY="scex_replication_v1";
    private static final SlotLayout LAYOUT=SlotLayout.builder().fluidInput(1).output(1).memory().output(1).battery().upgrade(4).build();
    private final FluidTank tank;
    private final IFluidHandler fluidPort;
    private final IndependentUuBuffer credit;
    private ItemStack selected=ItemStack.EMPTY;
    private double price,processed,pendingUu;
    private long paidEu,pendingEu,completed,lastTick=Long.MIN_VALUE;
    private long quotedGeneration=-1;
    private long migratedGeneration=Long.MIN_VALUE;
    private WorkMode mode=WorkMode.STOPPED;
    private boolean changing,selectionDirty=true;
    private int selectionCooldown;
    private CompoundTag held;
    private final int[] clientData=new int[8];
    private final net.neoforged.neoforge.items.IItemHandlerModifiable guardedItems=new net.neoforged.neoforge.items.IItemHandlerModifiable(){
        public int getSlots(){return itemHandler.getSlots();}
        public ItemStack getStackInSlot(int slot){return itemHandler.getStackInSlot(slot).copy();}
        public int getSlotLimit(int slot){return itemHandler.getSlotLimit(slot);}
        public boolean isItemValid(int slot,ItemStack stack){return !changing&&!hasHeldReplicationData()&&itemHandler.isItemValid(slot,stack);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return changing||hasHeldReplicationData()?stack:itemHandler.insertItem(slot,stack,simulate);}
        public ItemStack extractItem(int slot,int count,boolean simulate){return changing||hasHeldReplicationData()?ItemStack.EMPTY:itemHandler.extractItem(slot,count,simulate);}
        public void setStackInSlot(int slot,ItemStack stack){
            if(changing||hasHeldReplicationData())return;
            var before=itemHandler.getStackInSlot(slot);
            boolean taking=stack.isEmpty()||ItemStack.isSameItemSameComponents(before,stack)&&stack.getCount()<=before.getCount();
            if(taking||isItemValid(slot,stack)&&stack.getCount()<=Math.min(getSlotLimit(slot),stack.getMaxStackSize()))itemHandler.setStackInSlot(slot,stack.copy());
        }
    };

    public mio_icif_replicator_elc(BlockPos pos,BlockState state){this(pos,state,mio_icif_block_entities.REPLICATOR_ELC_ENTITY_TYPE.get());}
    public mio_icif_replicator_elc(BlockPos pos,BlockState state,BlockEntityType<?> type){
        super(pos,state,type,DEFAULT_CAPACITY,DEFAULT_MAX_RECEIVE,0,DEFAULT_WORK_TIME,LAYOUT,DEFAULT_ENERGY_PER_TICK,CableTier.EV);
        credit=new IndependentUuBuffer(()->level==null||serverThread(),()->ContainerToTank.markUnsaved(this));
        tank=new FluidTank(UUMATTER_CAPACITY,f->f.getFluid()==uuFluid()){
            @Override protected void onContentsChanged(){ContainerToTank.markUnsaved(mio_icif_replicator_elc.this);}
        };
        fluidPort=new IFluidHandler(){
            public int getTanks(){return 1;}
            public FluidStack getFluidInTank(int index){checkTank(index);return tank.getFluid().copy();}
            public int getTankCapacity(int index){checkTank(index);return UUMATTER_CAPACITY;}
            public boolean isFluidValid(int index,FluidStack f){return index==0&&!f.isEmpty()&&f.getFluid()==uuFluid();}
            public int fill(FluidStack f,FluidAction action){return changing||held!=null?0:tank.fill(f,action);}
            public FluidStack drain(FluidStack f,FluidAction action){return FluidStack.EMPTY;}
            public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
        };
    }
    private static void checkTank(int index){if(index!=0)throw new IndexOutOfBoundsException(index);}
    protected Fluid uuFluid(){return mio_icif_fluids.UUMATTER.get();}
    protected boolean serverThread(){return level instanceof ServerLevel s&&s.getServer().isSameThread();}
    protected UuQuoteBook.Quote trustedQuote(ItemStack item){return level instanceof ServerLevel s?UuQuoteBook.quote(s.getServer(),item):null;}
    protected long priceGeneration(){return level instanceof ServerLevel s?UuQuoteBook.generation(s.getServer()):-1;}
    public boolean hasHeldReplicationData(){return held!=null||credit.blocked();}
    public double getUuCreditBuckets(){return credit.creditBuckets();}
    public double getProcessedUuBuckets(){return processed;}
    public double getCurrentUuCostBuckets(){return price;}
    @Override protected MachineItemHandler createItemHandler(SlotLayout layout){
        var handler=new MachineItemHandler(layout){
            @Override protected void onContentsChanged(int slot){
                if(slot==MEMORY_SLOT)selectionDirty=true;
                mio_icif_replicator_elc.this.onInventoryChanged(slot);
                ContainerToTank.markUnsaved(mio_icif_replicator_elc.this);
            }
        };handler.setValidator(this);return handler;
    }
    @Override public boolean isItemValidForSlot(int slot,ItemStack item){
        if(item.isEmpty())return false;
        if(slot==MEMORY_SLOT)return item.getCount()==1&&item.getItem() instanceof mio_icif_memory;
        if(slot==UU_CELL_SLOT)return !cellContent(item).isEmpty();
        if(slot==BATTERY_SLOT)return apiIsBattery(item);
        if(slot>=UPGRADE_SLOT_START&&slot<SLOT_COUNT){var type=getItemAPI().getUpgradeType(item);return type!=null&&!type.isEmpty();}
        return false;
    }
    @Override protected int[] getSlotsForDirection(Direction side){return new int[]{UU_CELL_SLOT,EMPTY_CELL_SLOT,OUTPUT_SLOT};}
    @Override protected int getBatterySlot(){return BATTERY_SLOT;}
    @Override protected boolean canExtractItem(int slot,@Nullable Direction side){return !changing&&!hasHeldReplicationData()&&(slot==OUTPUT_SLOT||slot==EMPTY_CELL_SLOT);}
    @Override protected boolean canInsertItem(int slot,ItemStack stack,@Nullable Direction side){return !changing&&!hasHeldReplicationData()&&super.canInsertItem(slot,stack,side);}
    @Override public IFluidHandler getFluidHandlerCapability(@Nullable Direction side){return fluidPort;}
    @Override public net.neoforged.neoforge.items.IItemHandler getItemHandler(){return guardedItems;}
    @Override public ItemStack removeItem(int slot,int amount){return guardedItems.extractItem(slot,amount,false);}
    @Override public ItemStack removeItemNoUpdate(int slot){return guardedItems.extractItem(slot,Integer.MAX_VALUE,false);}
    @Override public void setItem(int slot,ItemStack stack){guardedItems.setStackInSlot(slot,stack);}
    @Override public void clearContent(){if(!changing&&!hasHeldReplicationData())super.clearContent();}
    @Override
    protected void handleAutomationUpgrades() {
        // A price reload can place paid work on hold during onTick, before the
        // base machine reaches automation. Keep internal transfers subject to
        // the same custody guard as the public inventory and fluid ports.
        if (changing || hasHeldReplicationData()) return;
        super.handleAutomationUpgrades();
    }
    private FluidStack cellContent(ItemStack item){
        if(item.isEmpty())return FluidStack.EMPTY;
        if(item.is(mio_icif_fluids.UUMATTER_BUCKET.get()))return new FluidStack(uuFluid(),1000);
        var content=mio_icif_cells.getCellFluid(item.copyWithCount(1));
        return !content.isEmpty()&&content.getFluid()==uuFluid()?content:FluidStack.EMPTY;
    }
    @Override protected void onTick(){
        if(changing||hasHeldReplicationData())return;
        long generation=priceGeneration();
        if(generation>=0&&(selectionDirty||generation!=migratedGeneration)){
            migratedGeneration=generation;
            changing=true;
            try{
                var memory=itemHandler.getStackInSlot(MEMORY_SLOT).copy();
                var migration=com.miophas.singularity_iteration.common.processing.UuPatternMigration.prepare(memory,this::trustedQuote);
                if(migration.status()==com.miophas.singularity_iteration.common.processing.UuPatternMigration.Status.UPDATED)
                    itemHandler.scexCommitSlots(new int[]{MEMORY_SLOT},new ItemStack[]{memory},new ItemStack[]{migration.replacement()});
            }finally{changing=false;}
        }
        var input=itemHandler.getStackInSlot(UU_CELL_SLOT);var content=cellContent(input);
        if(!content.isEmpty()){
            var empty=input.is(mio_icif_fluids.UUMATTER_BUCKET.get())?new ItemStack(Items.BUCKET):mio_icif_cells.getEmptyCellForStack(input);
            changing=true;try{ContainerToTank.transfer(itemHandler,UU_CELL_SLOT,EMPTY_CELL_SLOT,tank,content,empty);}finally{changing=false;}
        }
        refreshSelection();
    }
    private void refreshSelection(){
        long generation=priceGeneration();
        if(generation<0)return;
        if(!selected.isEmpty()&&(processed>0||paidEu>0||pendingUu>0)){
            if(generation!=quotedGeneration){
                var next=trustedQuote(selected);
                if(next==null||Double.compare(next.buckets(),price)!=0){hold("Price changed during paid work");return;}
                quotedGeneration=next.generation();
            }
            return;
        }
        if(!selectionDirty&&generation==quotedGeneration&&selectionCooldown-->0)return;
        selectionDirty=false;selectionCooldown=20;
        ItemStack item=selectPatternItem();
        var quote=item.isEmpty()?null:trustedQuote(item);
        selected=quote==null?ItemStack.EMPTY:item.copyWithCount(1);price=quote==null?0:quote.buckets();quotedGeneration=generation;
    }
    protected ItemStack selectPatternItem(){
        ItemStack item=ItemStack.EMPTY;var memory=itemHandler.getStackInSlot(MEMORY_SLOT);
        if(memory.getCount()==1&&memory.getItem() instanceof mio_icif_memory crystal)item=crystal.getStoredItemStack(memory);
        if(item.isEmpty()&&memory.isEmpty()&&level instanceof ServerLevel server){
            for(var side:Direction.values()){
                var at=worldPosition.relative(side);var chunk=server.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4);
                if(chunk!=null&&chunk.getBlockEntity(at) instanceof mio_icif_pattern_storage storage){item=storage.getCurrentPattern();if(!item.isEmpty())break;}
            }
        }
        return item;
    }
    private boolean fitsOutput(){
        if(selected.isEmpty())return false;
        var out=itemHandler.getStackInSlot(OUTPUT_SLOT);int limit=Math.min(itemHandler.getSlotLimit(OUTPUT_SLOT),selected.getMaxStackSize());
        return out.isEmpty()?limit>0:ItemStack.isSameItemSameComponents(out,selected)&&out.getCount()<limit;
    }
    @Override protected boolean canWork(){return !changing&&!hasHeldReplicationData()&&mode!=WorkMode.STOPPED&&!selected.isEmpty()&&price>0
        &&quotedGeneration>=0&&quotedGeneration==priceGeneration()&&completed<Long.MAX_VALUE&&fitsOutput();}
    @Override protected void tickProduction(){
        isWorking=false;
        if(!serverThread()||!canWork())return;
        changing=true;
        try{
            if(processed>=price){commitOutput();return;}
            if(pendingUu==0){
                double next=Math.min(BASE_RATE,price-processed);
                if(credit.creditBuckets()+tank.getFluidAmount()/1000.0<next||energyStorage.getAmount()<DEFAULT_ENERGY_PER_TICK)return;
                pendingUu=next;pendingEu=DEFAULT_ENERGY_PER_TICK;
            }
            long due=pendingEu-paidEu;
            if(credit.creditBuckets()+tank.getFluidAmount()/1000.0<pendingUu||energyStorage.getAmount()<due)return;
            long received=energyStorage.consumeEnergyInternal(due,false);
            if(received<0||received>due){hold("Invalid energy receipt");return;}
            paidEu+=received;ContainerToTank.markUnsaved(this);
            if(paidEu!=pendingEu)return;
            boolean paid=credit.consume(pendingUu,new IndependentUuBuffer.FluidPort(){
                public int availableMilliBuckets(){return tank.getFluidAmount();}
                public int drainMilliBuckets(int requested){return tank.drain(requested,IFluidHandler.FluidAction.EXECUTE).getAmount();}
            });
            if(!paid)return;
            processed=Math.min(price,processed+pendingUu);pendingUu=0;pendingEu=0;paidEu=0;isWorking=true;
            if(processed>=price)commitOutput();
            updateProgress();ContainerToTank.markUnsaved(this);
        }finally{changing=false;}
    }
    private void commitOutput(){
        if(!fitsOutput())return;
        var before=itemHandler.getStackInSlot(OUTPUT_SLOT).copy();
        var after=selected.copyWithCount(before.isEmpty()?1:before.getCount()+1);
        if(itemHandler.scexCommitSlots(new int[]{OUTPUT_SLOT},new ItemStack[]{before},new ItemStack[]{after},()->{
            processed=0;completed++;selectionDirty=true;if(mode==WorkMode.SINGLE)mode=WorkMode.STOPPED;
        })){
            ContainerToTank.markUnsaved(this);
            com.miophas.singularity_iteration.core.api.advancement.MioAchievements
                .at(level, worldPosition, "picard_order", "replicator_used");
        }
    }
    @Override protected void doWork(){tickProduction();}
    @Override protected void checkInputChanged(){/* Paid work owns its immutable item snapshot. */}
    @Override protected boolean shouldResetProgress(){return false;}
    @Override protected void updateProgress(){progress=price>0?(int)Math.min(10000,Math.max(0,processed/price*10000)):0;maxProgress=10000;}
    public void stopGeneration(){setWorkMode(0);}
    public void generateOnce(){setWorkMode(1);}
    public void loopGeneration(){setWorkMode(2);}
    public void setWorkMode(int value){if(serverThread()&&!changing&&!hasHeldReplicationData()&&value>=0&&value<3){mode=WorkMode.values()[value];selectionDirty=true;ContainerToTank.markUnsaved(this);}}
    public WorkMode getWorkMode(){return mode;}
    public boolean isReplicating(){return isWorking;}
    public int getReplicateProgress(){return progress;}
    public int getReplicateMaxProgress(){return 10000;}
    @Override public int getMaxProgress(){return 10000;}
    public FluidTank getUuMatterTank(){return tank;}
    public int getUuMatterAmount(){return tank.getFluidAmount();}
    public int getUuMatterCapacity(){return UUMATTER_CAPACITY;}
    /** Legacy integer display only; payment always uses getCurrentUuCostBuckets. */
    public long getCurrentUuCost(){return (long)Math.ceil(price*1000);}
    public long getCurrentEuCost(){return (long)Math.min(Long.MAX_VALUE,Math.ceil(price/BASE_RATE)*DEFAULT_ENERGY_PER_TICK);}
    @Override public long getTotalProcessed(){return completed;}
    @Override public Component getDisplayName(){return Component.translatable("container.mio_icif.replicator_elc");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new ReplicatorElcMenu(id,inventory,this);}
    public ContainerData getContainerData(){return new ContainerData(){
        public int getCount(){return 8;}
        public int get(int i){if(i<0||i>=8)throw new IndexOutOfBoundsException(i);if(level!=null&&level.isClientSide())return clientData[i];return switch(i){
            case 0->progress;case 1->10000;case 2->isWorking?1:0;case 3->(int)Math.min(Integer.MAX_VALUE,energyStorage.getAmount());
            case 4->(int)Math.min(Integer.MAX_VALUE,energyStorage.getCapacity());case 5->tank.getFluidAmount();case 6->UUMATTER_CAPACITY;default->mode.ordinal();};}
        public void set(int i,int v){if(i<0||i>=8)throw new IndexOutOfBoundsException(i);clientData[i]=v;}
    };}
    private void hold(String why){held=new CompoundTag();held.putString("reason",why);isWorking=false;ContainerToTank.markUnsaved(this);}
    @Override public void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);var own=new CompoundTag();own.putInt("version",1);own.put("credit",credit.save());
        own.put("tank",tank.writeToNBT(registries,new CompoundTag()));if(!selected.isEmpty())own.put("item",selected.save(registries));
        own.putDouble("price",price);own.putDouble("processed",processed);own.putDouble("pending_uu",pendingUu);own.putLong("pending_eu",pendingEu);
        own.putLong("paid_eu",paidEu);own.putLong("completed",completed);own.putInt("mode",mode.ordinal());
        if(held!=null)own.put("held",held.copy());tag.put(SAVE_KEY,own);
    }
    @Override public void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);held=null;selected=ItemStack.EMPTY;mode=WorkMode.STOPPED;price=processed=pendingUu=0;paidEu=pendingEu=completed=0;
        tank.setFluid(FluidStack.EMPTY);selectionDirty=true;quotedGeneration=-1;lastTick=Long.MIN_VALUE;
        if(!tag.contains(SAVE_KEY,Tag.TAG_COMPOUND)){isWorking=false;return;}   // item/pick-block data or empty: fresh machine
        var own=tag.getCompound(SAVE_KEY);
        var allowed=java.util.Set.of("version","credit","tank","item","price","processed","pending_uu","pending_eu","paid_eu","completed","mode","held");
        boolean schema=allowed.containsAll(own.getAllKeys())&&own.contains("mode",Tag.TAG_INT)
            &&(!own.contains("item")||own.contains("item",Tag.TAG_COMPOUND))&&(!own.contains("held")||own.contains("held",Tag.TAG_COMPOUND));
        for(String key:new String[]{"price","processed","pending_uu"})schema&=own.contains(key,Tag.TAG_DOUBLE);
        for(String key:new String[]{"pending_eu","paid_eu","completed"})schema&=own.contains(key,Tag.TAG_LONG);
        if(!schema||!own.contains("version",Tag.TAG_INT)||own.getInt("version")!=1||!own.contains("credit",Tag.TAG_COMPOUND)||!own.contains("tank",Tag.TAG_COMPOUND)){
            held=tag.copy();isWorking=false;return;
        }
        if(!credit.load(own.getCompound("credit"))){held=tag.copy();isWorking=false;return;}
        tank.readFromNBT(registries,own.getCompound("tank"));
        if(!own.getCompound("tank").isEmpty()&&tank.isEmpty()||!tank.isEmpty()&&(tank.getFluid().getFluid()!=uuFluid()||tank.getFluidAmount()>UUMATTER_CAPACITY)){held=tag.copy();isWorking=false;return;}
        selected=own.contains("item",Tag.TAG_COMPOUND)?ItemStack.parseOptional(registries,own.getCompound("item")):ItemStack.EMPTY;
        price=own.getDouble("price");processed=own.getDouble("processed");pendingUu=own.getDouble("pending_uu");pendingEu=own.getLong("pending_eu");paidEu=own.getLong("paid_eu");completed=own.getLong("completed");int modeId=own.getInt("mode");
        if(!Double.isFinite(price)||price<0||!Double.isFinite(processed)||processed<0||processed>price||!Double.isFinite(pendingUu)||pendingUu<0||pendingUu>BASE_RATE
            ||pendingUu>price-processed||pendingEu<0||pendingEu>DEFAULT_ENERGY_PER_TICK||(pendingEu==0)!=(pendingUu==0)||paidEu<0||paidEu>pendingEu||completed<0||modeId<0||modeId>2
            ||!selected.isEmpty()&&!StoredPattern.valid(selected,price,0)||selected.isEmpty()&&(price!=0||processed!=0||pendingUu!=0||paidEu!=0||pendingEu!=0)){
            held=tag.copy();isWorking=false;return;
        }
        mode=WorkMode.values()[modeId];
        if(own.contains("held",Tag.TAG_COMPOUND))com.miophas.singularity_iteration.common.Singularity_Iteration.LOGGER.warn("Replicator at {}: discarding held record {} on load",worldPosition,own.getCompound("held"));
        isWorking=false;updateProgress();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=super.getUpdateTag(registries);saveAdditional(tag,registries);return tag;}
    @Override public void serverTick(){if(level!=null)tick(level,worldPosition,getBlockState(),this);}
    public static void tick(Level level,BlockPos pos,BlockState state,mio_icif_replicator_elc machine){
        if(!machine.serverThread()||machine.hasHeldReplicationData()||machine.lastTick==level.getGameTime())return;
        machine.lastTick=level.getGameTime();AbstractProcessingMachineBlockEntity.tick(level,pos,state,machine);
    }
}
