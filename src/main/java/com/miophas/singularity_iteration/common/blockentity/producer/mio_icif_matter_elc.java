// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.block.producer.mio_icif_block_matter_elc;
import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats;
import com.miophas.singularity_iteration.common.menu.producer.MatterElcMenu;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;

/** R115 independent implementation from R114 normal gameplay observations and public SI ABI.
 * Fabrication progress is not electrical storage. Only newly received EU can consume amplifier
 * credit. Pending EU, completed work and fractional credit each survive saves independently. */
public class mio_icif_matter_elc extends AbstractProcessingMachineBlockEntity {
    public static final int SLOT_COUNT=7,AMPLIFIER_SLOT=0,OUTPUT_SLOT=1,CONTAINER_SLOT=2,UPGRADE_SLOT_START=3,UPGRADE_SLOT_COUNT=4;
    public static final long DEFAULT_CAPACITY=1000000,DEFAULT_MAX_RECEIVE=8192,DEFAULT_MAX_EXTRACT=0,DEFAULT_ENERGY_PER_TICK=0,EU_PER_MB=1000000;
    public static final int DEFAULT_WORK_TIME=1,UUMATTER_CAPACITY=8000,SCRAP_BONUS=5000,SCRAPBOX_BONUS=45000,THORIUM_SCRAP_BONUS=360000,UUMATTER_OUTPUT_AMOUNT=1;
    private static final SlotLayout LAYOUT=SlotLayout.builder().input(1).output(1).fluidInput(1).upgrade(4).build();
    private static final EnergyAmount UNIT=EnergyAmount.of(EU_PER_MB),PRELOAD=EnergyAmount.of(10000);
    private EnergyAmount work=EnergyAmount.ZERO,amplifier=EnergyAmount.ZERO;
    private CompoundTag hold=new CompoundTag();
    private final FluidTank matter;
    private long capacity=DEFAULT_CAPACITY;
    private boolean changing,amplifying,ready;
    private double legacyLastEnergy;

    public mio_icif_matter_elc(BlockPos pos,BlockState state){this(pos,state,mio_icif_block_entities.MATTER_ELC_ENTITY_TYPE.get());}
    public mio_icif_matter_elc(BlockPos pos,BlockState state,BlockEntityType<?> type){
        super(pos,state,type,DEFAULT_CAPACITY,DEFAULT_MAX_RECEIVE,0,DEFAULT_WORK_TIME,LAYOUT,0,CableTier.EV);
        matter=new FluidTank(UUMATTER_CAPACITY,f->f.getFluid()==mio_icif_fluids.UUMATTER.get()){
            @Override protected void onContentsChanged(){dirty();}
        };
        energyStorage.setCapacity(0); // Wait for a real server tick before trusting neighbor/redstone state.
    }
    @Override protected MachineItemHandler createItemHandler(SlotLayout layout){return new MachineItemHandler(layout){
        @Override public boolean isItemValid(int slot,ItemStack stack){return isItemValidForSlot(slot,stack);}
        @Override protected void onContentsChanged(int slot){onInventoryChanged(slot); dirty();}
    };}
    private void dirty(){ContainerToTank.markUnsaved(this);}
    private boolean live(){
        if(!(level instanceof ServerLevel s)||!s.getServer().isSameThread()||isRemoved())return false;
        var c=s.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4);
        return c!=null&&c.getBlockEntity(worldPosition,LevelChunk.EntityCreationType.CHECK)==this;
    }
    public boolean hasUnmappedState(){return !hold.isEmpty();}
    public EnergyAmount getFabricationProgress(){return work;}
    public EnergyAmount getAmplifierCredit(){return amplifier;}
    private static int bonus(ItemStack stack){
        if(stack.is(mio_icif_normal.SCRAP.get()))return SCRAP_BONUS;
        if(stack.is(mio_icif_normal.SCRAPBOX.get()))return SCRAPBOX_BONUS;
        // Existing SI extension, not an original-game equivalence claim.
        return stack.is(mio_icif_resources.THORIUM_SCRAP.get())?THORIUM_SCRAP_BONUS:0;
    }
    public boolean isItemValidForSlot(int slot,ItemStack stack){
        if(stack.isEmpty())return false;
        if(slot==AMPLIFIER_SLOT)return bonus(stack)>0;
        if(slot==CONTAINER_SLOT)return stack.is(Items.BUCKET)||mio_icif_cells.isEmptyCell(stack);
        return slot>=UPGRADE_SLOT_START&&slot<SLOT_COUNT&&MioIcifAPI.instance().getItemAPI().isUpgrade(stack);
    }
    @Override protected int getBatterySlot(){return -1;}
    @Override protected int[] getSlotsForDirection(Direction side){return side==Direction.DOWN?new int[]{OUTPUT_SLOT}:new int[]{AMPLIFIER_SLOT,CONTAINER_SLOT};}
    @Override protected boolean canWork(){return !hasUnmappedState()&&canWorkRedstone();}
    @Override protected void doWork(){} // The exact ledger below owns production.
    @Override protected void recalculateUpgradeStats(){
        upgradeStats=MachineUpgradeStats.fromInventory(itemHandler,UPGRADE_SLOT_START,UPGRADE_SLOT_COUNT);
        capacity=Math.addExact(DEFAULT_CAPACITY,Math.max(0,upgradeStats.getEnergyCapacityBonus()));
        energyStorage.setMaxReceive(DEFAULT_MAX_RECEIVE);updateAdmission();
    }
    @Override public long getEffectiveCapacity(){return !ready||work==null||hold==null||hasUnmappedState()||!canWorkRedstone()?0:Math.max(0,capacity-Math.min(capacity,work.whole()));}
    private void updateAdmission(){
        if(work==null||hold==null)return; // Base load/constructor callbacks precede independent fields.
        // Capacity counts paid work as well as pending input, but only real EU lives in storage.
        // A fractional room is rounded up for intake; exact conservation retains any overshoot.
        energyStorage.setCapacity(getEffectiveCapacity());
    }
    public static void tick(Level level,BlockPos pos,BlockState state,mio_icif_matter_elc m){
        if(!m.live()||m.changing)return;
        m.ready=true;
        m.recalculateUpgradeStats();m.isWorking=false;m.amplifying=false;
        if(!m.hasUnmappedState()&&m.canWorkRedstone()){
            m.changing=true;
            try{m.fabricate();m.fillContainer();}finally{m.changing=false;}
            m.handleAutomationUpgrades();
        }
        m.updateAdmission();
        var actual=m.getBlockState();if(actual.getValue(mio_icif_block_matter_elc.LIT)!=m.isWorking)
            level.setBlock(pos,actual.setValue(mio_icif_block_matter_elc.LIT,m.isWorking),3);
    }
    private void fabricate(){
        var offered=energyStorage.scexExactAmount();
        if(!offered.isZero()){
            // Mode-off compatibility consumes only whole EU; exact fractions stay pending there.
            if(!energyStorage.scexNetworkControlled())offered=EnergyAmount.of(offered.whole());
            var boosted=offered.min(amplifier);
            var next=work.add(offered);
            for(int i=0;i<5;i++)next=next.add(boosted);
            var paid=energyStorage.scexNetworkControlled()?energyStorage.scexConsumeEnergy(offered,false)
                :EnergyAmount.of(energyStorage.consumeEnergyInternal(offered.whole(),false));
            if(!paid.equals(offered))throw new IllegalStateException("Owned input changed during fabrication");
            work=next;amplifier=amplifier.subtract(boosted);amplifying=!boosted.isZero();dirty();
        }
        isWorking=!work.isZero();
        if(!work.isZero()&&amplifier.compareTo(PRELOAD)<0){
            var before=itemHandler.getStackInSlot(AMPLIFIER_SLOT).copy();int add=bonus(before);
            if(add>0){var next=amplifier.add(EnergyAmount.of(add));
                itemHandler.scexCommitSlots(new int[]{AMPLIFIER_SLOT},new ItemStack[]{before},
                    new ItemStack[]{before.copyWithCount(before.getCount()-1)},()->{amplifier=next;dirty();});
            }
        }
        if(work.compareTo(UNIT)>=0&&matter.getFluidAmount()<UUMATTER_CAPACITY){
            var output=new FluidStack(mio_icif_fluids.UUMATTER.get(),1);
            if(matter.fill(output,IFluidHandler.FluidAction.SIMULATE)==1){
                matter.fill(output,IFluidHandler.FluidAction.EXECUTE);work=work.subtract(UNIT);dirty();
            }
        }
    }
    private void fillContainer(){
        var input=itemHandler.getStackInSlot(CONTAINER_SLOT);if(input.isEmpty()||matter.isEmpty())return;
        if(input.is(Items.BUCKET)){
            var filled=new ItemStack(mio_icif_fluids.UUMATTER.get().getBucket());filled.applyComponents(input.getComponentsPatch());
            if(matter.getFluidAmount()>=1000)ContainerToTank.drainToContainer(itemHandler,CONTAINER_SLOT,OUTPUT_SLOT,matter,
                new FluidStack(mio_icif_fluids.UUMATTER.get(),1000),filled);
        }else if(mio_icif_cells.isEmptyCell(input)){
            // Cell capacity/content come from the existing public SI item helper.
            var filled=mio_icif_cells.getFilledCellForFluidStack(mio_icif_fluids.UUMATTER.get());
            if(!filled.isEmpty()){
                var content=mio_icif_cells.getCellFluid(filled);filled.applyComponents(input.getComponentsPatch());
                // Refuse an incompatible custom fluid component instead of dropping it or minting contents.
                if(FluidStack.matches(content,mio_icif_cells.getCellFluid(filled)))
                    ContainerToTank.drainToContainer(itemHandler,CONTAINER_SLOT,OUTPUT_SLOT,matter,content,filled);
            }
        }
    }
    public FluidTank getUuMatterTank(){return matter;}
    public int getUuMatterAmount(){return matter.getFluidAmount();}
    public int getUuMatterCapacity(){return UUMATTER_CAPACITY;}
    public int getScrap(){return (int)Math.min(Integer.MAX_VALUE,amplifier.whole());}
    public int getState(){return !isWorking?0:amplifying?2:1;}
    @Override public int getProgress(){return (int)Math.min(Integer.MAX_VALUE,work.whole());}
    @Override public int getMaxProgress(){return (int)EU_PER_MB;}
    public String getProgressAsString(){return String.format(java.util.Locale.ROOT,"%.2f%%",100.0*work.toDouble()/EU_PER_MB);}
    @Override public IItemHandler getItemHandlerCapability(Direction side){return new IItemHandler(){
        public int getSlots(){return SLOT_COUNT;}
        public ItemStack getStackInSlot(int slot){return itemHandler.getStackInSlot(slot).copy();}
        public int getSlotLimit(int slot){return itemHandler.getSlotLimit(slot);}
        public boolean isItemValid(int slot,ItemStack stack){return isItemValidForSlot(slot,stack);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return !live()||changing||hasUnmappedState()?stack:itemHandler.insertItem(slot,stack,simulate);}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return !live()||changing||hasUnmappedState()||slot!=OUTPUT_SLOT?ItemStack.EMPTY:itemHandler.extractItem(slot,amount,simulate);}
    };}
    @Override public IFluidHandler getFluidHandlerCapability(Direction side){return new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int tank){if(tank!=0)throw new IndexOutOfBoundsException(tank);return matter.getFluid().copy();}
        public int getTankCapacity(int tank){if(tank!=0)throw new IndexOutOfBoundsException(tank);return UUMATTER_CAPACITY;}
        public boolean isFluidValid(int tank,FluidStack f){return false;}
        public int fill(FluidStack f,FluidAction a){return 0;}
        public FluidStack drain(FluidStack f,FluidAction a){return !live()||changing||hasUnmappedState()?FluidStack.EMPTY:matter.drain(f,a);}
        public FluidStack drain(int n,FluidAction a){return !live()||changing||hasUnmappedState()?FluidStack.EMPTY:matter.drain(n,a);}
    };}
    @Override public Component getDisplayName(){return Component.translatable("container.mio_icif.matter_elc");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new MatterElcMenu(id,inventory,this);}
    public ContainerData getContainerData(){return new ContainerData(){
        public int getCount(){return 7;}
        public int get(int i){return switch(i){case 0,2->(int)Math.min(Integer.MAX_VALUE,work.whole()+Math.min(Integer.MAX_VALUE,energyStorage.getAmount()));case 1->(int)EU_PER_MB;case 3->(int)Math.min(Integer.MAX_VALUE,capacity);case 4->getUuMatterAmount();case 5->UUMATTER_CAPACITY;case 6->getScrap();default->0;};}
        public void set(int i,int v){}
    };}
    private static void putAmount(CompoundTag tag,String key,EnergyAmount value){var t=new CompoundTag();t.putLong("whole",value.whole());t.putLong("fraction",value.fraction());tag.put(key,t);}
    private static EnergyAmount amount(CompoundTag tag,String key){var t=tag.getCompound(key);return new EnergyAmount(t.getLong("whole"),t.getLong("fraction"));}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){
        super.saveAdditional(tag,provider);tag.put("UuMatterTank",matter.writeToNBT(provider,new CompoundTag()));tag.putInt("Scrap",getScrap());tag.putDouble("LastEnergy",legacyLastEnergy);
        var s=new CompoundTag();s.putInt("version",1);putAmount(s,"work",work);putAmount(s,"amplifier",amplifier);s.put("hold",hold.copy());tag.put("scex_matter",s);
    }
    @Override public void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){
        ready=false;
        super.loadAdditional(tag,provider);matter.readFromNBT(provider,tag.getCompound("UuMatterTank"));
        work=EnergyAmount.ZERO;amplifier=EnergyAmount.ZERO;hold=new CompoundTag();legacyLastEnergy=tag.getDouble("LastEnergy");
        try{
            new EnergyAmount(tag.getLong("energy"),tag.getLong("scex_energy_fraction"));
            if(tag.contains("scex_matter",Tag.TAG_COMPOUND)){
                var s=tag.getCompound("scex_matter");if(s.getInt("version")!=1)hold=s.copy();
                else{work=amount(s,"work");amplifier=amount(s,"amplifier");hold=s.getCompound("hold").copy();}
            }else{
                // Legacy stored EU was already displayed fabrication progress. LastEnergy is
                // retained as provenance, never used to award speculative historical bonuses.
                work=new EnergyAmount(tag.getLong("energy"),tag.getLong("scex_energy_fraction"));
                amplifier=EnergyAmount.of(tag.getInt("Scrap"));energyStorage.setEnergy(0);energyStorage.scexLoadFraction(0);
            }
            if(work.whole()>1000000000L||amplifier.whole()>1000000000L||tag.getLong("energy")>1000000000L
                ||matter.getFluidAmount()>UUMATTER_CAPACITY||!matter.isEmpty()&&!matter.isFluidValid(matter.getFluid())||!Double.isFinite(legacyLastEnergy))throw new IllegalArgumentException("Saved state out of range");
        }catch(IllegalArgumentException ex){hold=tag.copy();hold.putString("reason","Invalid saved matter ledger retained");}
        isWorking=false;amplifying=false;recalculateUpgradeStats();
    }
}
