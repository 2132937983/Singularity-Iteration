// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.block.producer.mio_icif_block_blast_furnace;
import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.menu.producer.BlastFurnaceMenu;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.common.recipe.blast_furnace.*;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots;
import java.util.List;
import java.util.Optional;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** R113 independent implementation from R112 ordinary observations and SI public integration.
 * Existing SI recipes remain data driven. Only the measured iron recipe is aligned to6000ticks.
 * No predecessor body or original mod API/implementation was used. */
public class mio_icif_blast_furnace extends AbstractHeatBlockEntity {
    // Per-call-site recipe memo: canWork() runs every tick, getRecipeFor is a linear scan.
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.blast_furnace.mio_icif_BlastFurnaceRecipeInput, com.miophas.singularity_iteration.common.recipe.blast_furnace.mio_icif_BlastFurnaceRecipe> recipeCache1 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_BlastFurnaceRecipes.BLAST_FURNACE_TYPE);
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.blast_furnace.mio_icif_BlastFurnaceRecipeInput, com.miophas.singularity_iteration.common.recipe.blast_furnace.mio_icif_BlastFurnaceRecipe> recipeCache2 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_BlastFurnaceRecipes.BLAST_FURNACE_TYPE);
    public static final int INPUT_SLOT=0,OUTPUT_SLOT_1=1,OUTPUT_SLOT_2=2,AIR_CELL_SLOT=3,EMPTY_CELL_SLOT=4,
            UPGRADE_SLOT_START=5,UPGRADE_SLOT_COUNT=2,TOTAL_SLOTS=7;
    public static final int HEAT_CAPACITY=50000,HEAT_STORAGE_CAPACITY=50100,MAX_HEAT_RECEIVE=1000,MAX_HEAT_EXTRACT=0,
            MAX_TEMP=1000,AIR_TANK_CAPACITY=8000,AIR_PER_TICK=1,BASE_OPERATION_TICKS=6000,AIR_PER_OPERATION=6000;
    public static final float HEAT_LOSS_FACTOR=0.0f;
    private static final SlotLayout LAYOUT=SlotLayout.builder().input(1).output(2).fluidInput(1).output(1).upgrade(2).build();
    private final FluidTank air;
    private final HeatPort[] ports=new HeatPort[6];
    private int progress,maxProgress=BASE_OPERATION_TICKS;
    private boolean working,changing,pulling;
    private long frame=Long.MIN_VALUE,received,uncertain;
    private String failure="";
    private CompoundTag plan=new CompoundTag(),hold=new CompoundTag(),computedPlan=new CompoundTag();
    private RecipeHolder<mio_icif_BlastFurnaceRecipe> currentRecipe;
    private ItemStack cachedInput=ItemStack.EMPTY;
    private long recipeChecked=Long.MIN_VALUE;

    public mio_icif_blast_furnace(BlockPos pos,BlockState state){this(pos,state,mio_icif_block_entities.BLAST_FURNACE_ENTITY_TYPE.get());}
    public mio_icif_blast_furnace(BlockPos pos,BlockState state,BlockEntityType<?> type){
        super(type,pos,state,LAYOUT,new com.miophas.singularity_iteration.core.runtime.energy.PlatformHeatStorage(HEAT_STORAGE_CAPACITY,MAX_HEAT_RECEIVE,0,20,MAX_TEMP,0));
        air=new FluidTank(AIR_TANK_CAPACITY,f->f.getFluid()==mio_icif_fluids.AIR.get()){
            @Override protected void onContentsChanged(){dirty();}
        };
        for(var side:Direction.values())ports[side.get3DDataValue()]=new HeatPort(side);
    }
    @Override protected MachineItemHandler createItemHandler(SlotLayout layout){
        return new MachineItemHandler(layout){
            @Override public boolean isItemValid(int slot,ItemStack stack){return isItemValidForSlot(slot,stack);}
            @Override protected void onContentsChanged(int slot){
                if(layout!=null&&layout.isType(slot,com.miophas.singularity_iteration.core.prefab.inventory.SlotType.UPGRADE)){
                    markUpgradeStatsDirty();
                }
                recipeChecked=Long.MIN_VALUE;dirty();
            }
        };
    }
    private void dirty(){ContainerToTank.markUnsaved(this);}
    private boolean live(){
        if(!(level instanceof ServerLevel s)||!s.getServer().isSameThread()||isRemoved())return false;
        var c=s.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4);
        return c!=null&&c.getBlockEntity(worldPosition,LevelChunk.EntityCreationType.CHECK)==this;
    }
    private Direction front(){return getBlockState().getValue(mio_icif_block_blast_furnace.FACING);}
    private boolean held(){return !hold.isEmpty()||uncertain!=0;}
    public boolean hasUnmappedState(){return held();}
    public long getUncertainHeat(){return uncertain;}
    private void frame(){long now=level==null?0:level.getGameTime();if(now!=frame){frame=now;received=0;}}
    private RecipeHolder<mio_icif_BlastFurnaceRecipe> recipe(){
        if(level==null)return null;
        var input=itemHandler.getStackInSlot(INPUT_SLOT);long now=level.getGameTime();
        if(!ItemStack.matches(input,cachedInput)||recipeChecked==Long.MIN_VALUE||now-recipeChecked>=20){
            cachedInput=input.copy();recipeChecked=now;
            computedPlan=new CompoundTag();
            currentRecipe=recipeCache1.find(level, new mio_icif_BlastFurnaceRecipeInput(input)).orElse(null);
        }
        return currentRecipe;
    }
    public boolean isItemValidForSlot(int slot,ItemStack stack){
        if(stack.isEmpty())return false;
        if(slot==INPUT_SLOT)return level==null||recipeCache2.find(level, new mio_icif_BlastFurnaceRecipeInput(stack)).isPresent();
        if(slot==AIR_CELL_SLOT)return mio_icif_cells.isCellContainingFluid(stack,mio_icif_fluids.AIR.get());
        return slot>=UPGRADE_SLOT_START&&slot<TOTAL_SLOTS&&MioIcifAPI.instance().getItemAPI().isUpgrade(stack);
    }
    // R112: a source-fed tick gains heat without ordinary cooling. Other ticks lose one HU.
    @Override public long receiveHeat(long amount,boolean simulate){
        if(!live()||changing||pulling||held()||amount<=0||recipe()==null||getHeatStored()>HEAT_CAPACITY)return 0;
        frame();long accepted=Math.min(amount,Math.min(Math.max(0,100-received),HEAT_STORAGE_CAPACITY-getHeatStored()));
        if(!simulate&&accepted>0){heatStorage.generateHeatInternal(accepted,false);received+=accepted;dirty();}return accepted;
    }
    @Override public boolean canReceiveHeat(){return receiveHeat(1,true)>0;}
    @Override public long extractHeat(long amount,boolean simulate){return 0;}
    @Override public boolean canExtractHeat(){return false;}
    @Override protected void applyHeatCapacityUpgrades(){} // Fixed SI capacity; storage upgrades cannot destroy or manufacture heat.
    private void pullHeat(){
        if(received!=0||getHeatStored()>HEAT_CAPACITY||recipe()==null||held())return;
        var s=(ServerLevel)level;var at=worldPosition.relative(front());
        if(s.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4)==null)return;
        pulling=true;
        try{
            var source=s.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK,at,front().getOpposite());
            if(source==null||source==this||!source.canExtractHeat())return;
            long proposed=source.extractHeat(Math.min(100,HEAT_STORAGE_CAPACITY-getHeatStored()),true);
            if(proposed<=0||proposed>100)return;
            uncertain=proposed;dirty();
            long taken=source.extractHeat(proposed,false);
            if(taken<0||taken>proposed){failure="Invalid external HU receipt";dirty();return;}
            uncertain=0;heatStorage.generateHeatInternal(taken,false);received+=taken;failure="";dirty();
        }catch(RuntimeException e){if(uncertain>0){failure=e.getClass().getName();dirty();}}
        finally{pulling=false;}
    }
    public static void tick(Level level,BlockPos pos,BlockState state,mio_icif_blast_furnace m){
        if(!m.live()||m.changing||m.held())return;
        m.frame();m.handleAutomationUpgrades();m.fillCell();
        var holder=m.recipe();m.pullHeat();if(m.held())return;
        if(m.received==0&&m.getHeatStored()>0){m.heatStorage.consumeHeatInternal(1,false);m.dirty();}
        m.working=holder!=null&&(m.received>0||m.isHot());
        m.process(holder);
        var actual=m.getBlockState();if(actual.getValue(mio_icif_block_blast_furnace.LIT)!=m.working)
            level.setBlock(pos,actual.setValue(mio_icif_block_blast_furnace.LIT,m.working),3);
    }
    private void fillCell(){
        var stack=itemHandler.getStackInSlot(AIR_CELL_SLOT);if(!mio_icif_cells.isCellContainingFluid(stack,mio_icif_fluids.AIR.get()))return;
        ContainerToTank.transfer(itemHandler,AIR_CELL_SLOT,EMPTY_CELL_SLOT,air,mio_icif_cells.getCellFluid(stack),mio_icif_cells.getEmptyCellForStack(stack));
    }
    private CompoundTag describe(RecipeHolder<mio_icif_BlastFurnaceRecipe> holder){
        var r=holder.value();var t=new CompoundTag();t.putString("recipe",holder.id().toString());
        t.put("input",itemHandler.getStackInSlot(INPUT_SLOT).copyWithCount(1).save(level.registryAccess()));
        t.put("primary",r.getResult().save(level.registryAccess()));
        if(!r.getSecondaryResult().isEmpty())t.put("secondary",r.getSecondaryResult().save(level.registryAccess()));
        t.putInt("duration",r.getDuration());t.putInt("air",r.getAirCostPerTick());t.putInt("count",r.getIngredientCount());return t;
    }
    private void process(RecipeHolder<mio_icif_BlastFurnaceRecipe> holder){
        if(holder==null){if(progress!=0||!plan.isEmpty()){progress=0;plan=new CompoundTag();dirty();}return;}
        var r=holder.value();int duration=r.getDuration(),airCost=r.getAirCostPerTick();
        if(duration<=0||duration>200000||airCost<0||airCost>AIR_TANK_CAPACITY||r.getIngredientCount()<1)return;
        if(computedPlan.isEmpty())computedPlan=describe(holder);var next=computedPlan;maxProgress=duration;
        if(!plan.isEmpty()&&!plan.equals(next)){progress=0;dirty();}plan=next;
        if(progress>=duration){hold.putString("reason","Saved progress exceeds current recipe");dirty();return;}
        // 对齐 IC2：热量只作为“是否够热”的开关，加工过程中不再逐 tick 扣热。
        if(!isHot()||air.getFluidAmount()<airCost||!canWorkRedstone())return;
        var prepared=RecipeSlots.prepare(itemHandler,INPUT_SLOT,r.getIngredientCount(),new int[]{OUTPUT_SLOT_1,OUTPUT_SLOT_2},List.of(r.getResult(),r.getSecondaryResult()));
        if(prepared.isEmpty())return;
        changing=true;
        try{
            if(progress+1==duration&&!prepared.get().commit())return;
            if(airCost>0)air.drain(airCost,IFluidHandler.FluidAction.EXECUTE);
            progress=progress+1==duration?0:progress+1;if(progress==0)plan=new CompoundTag();dirty();
        }finally{changing=false;}
    }
    public boolean isHot(){return getHeatStored()>=HEAT_CAPACITY;}
    @Override public ItemStackHandler getItemHandler(){return itemHandler;}
    public IFluidHandler getAirTank(){return air;}
    public int getAirAmount(){return air.getFluidAmount();}
    public int getAirCapacity(){return AIR_TANK_CAPACITY;}
    public int getProgress(){return progress;}
    public int getMaxProgress(){return maxProgress;}
    public int getHeatCapacity(){return HEAT_CAPACITY;}
    public boolean isWorking(){return working;}
    @Override public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(Direction side){return side==null?this:side==front()?ports[side.get3DDataValue()]:null;}
    public IItemHandler getItemHandlerCapability(Direction side){return new IItemHandler(){
        public int getSlots(){return TOTAL_SLOTS;}
        public ItemStack getStackInSlot(int slot){return itemHandler.getStackInSlot(slot).copy();}
        public int getSlotLimit(int slot){return itemHandler.getSlotLimit(slot);}
        public boolean isItemValid(int slot,ItemStack stack){return isItemValidForSlot(slot,stack);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return !live()||held()||changing?stack:itemHandler.insertItem(slot,stack,simulate);}
        public ItemStack extractItem(int slot,int count,boolean simulate){return !live()||held()||changing||!(slot==1||slot==2||slot==4)?ItemStack.EMPTY:itemHandler.extractItem(slot,count,simulate);}
    };}
    @Override public IFluidHandler getFluidHandlerCapability(Direction side){return new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int tank){if(tank!=0)throw new IndexOutOfBoundsException(tank);return air.getFluid().copy();}
        public int getTankCapacity(int tank){if(tank!=0)throw new IndexOutOfBoundsException(tank);return AIR_TANK_CAPACITY;}
        public boolean isFluidValid(int tank,FluidStack f){return tank==0&&air.isFluidValid(f);}
        public int fill(FluidStack f,FluidAction a){return !live()||held()||changing?0:air.fill(f,a);}
        public FluidStack drain(FluidStack f,FluidAction a){return !live()||held()||changing?FluidStack.EMPTY:air.drain(f,a);}
        public FluidStack drain(int n,FluidAction a){return !live()||held()||changing?FluidStack.EMPTY:air.drain(n,a);}
    };}
    @Override public Component getDisplayName(){return Component.translatable("container.mio_icif.blast_furnace");}
    public ContainerData getContainerData(){return new ContainerData(){
        public int getCount(){return 6;}
        public int get(int index){return switch(index){case 0->(int)Math.min(Integer.MAX_VALUE,getHeatStored());case 1->HEAT_CAPACITY;case 2->progress;case 3->maxProgress;case 4->getAirAmount();case 5->AIR_TANK_CAPACITY;default->0;};}
        public void set(int index,int value){} // Client menus use their own SimpleContainerData.
    };}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new BlastFurnaceMenu(id,inventory,this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){
        super.saveAdditional(tag,provider);tag.put("inventory",itemHandler.serializeNBT(provider));tag.put("airTank",air.writeToNBT(provider,new CompoundTag()));
        tag.putInt("progress",progress);tag.putBoolean("isWorking",working);
        var s=new CompoundTag();s.putInt("version",1);s.putInt("max_progress",maxProgress);s.putLong("uncertain",uncertain);s.putString("failure",failure);s.put("plan",plan.copy());s.put("hold",hold.copy());tag.put("scex_blast",s);
    }
    @Override public void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){
        super.loadAdditional(tag,provider);itemHandler.deserializeNBT(provider,tag.getCompound("inventory"));air.readFromNBT(provider,tag.getCompound("airTank"));
        progress=tag.getInt("progress");working=tag.getBoolean("isWorking");plan=new CompoundTag();hold=new CompoundTag();uncertain=0;maxProgress=BASE_OPERATION_TICKS;
        if(tag.contains("scex_blast",Tag.TAG_COMPOUND)){
            var s=tag.getCompound("scex_blast");if(s.getInt("version")!=1)hold=s.copy();
            else{plan=s.getCompound("plan").copy();hold=s.getCompound("hold").copy();uncertain=s.getLong("uncertain");failure=s.getString("failure");maxProgress=s.getInt("max_progress");}
        }
        if(progress<0||progress>200000||maxProgress<=0||maxProgress>200000||getHeatStored()>HEAT_STORAGE_CAPACITY||tag.getLong("heat")>HEAT_STORAGE_CAPACITY||tag.getLong("heat")<0||uncertain<0||getAirAmount()>AIR_TANK_CAPACITY||!air.isEmpty()&&!air.isFluidValid(air.getFluid())){
            hold=tag.copy();hold.remove("scex_blast");hold.putString("reason","Out-of-range saved state retained");
        }
        frame=Long.MIN_VALUE;received=0;recipeChecked=Long.MIN_VALUE;
    }
    private final class HeatPort implements IMioIcifCapabilities.IHeatStorage{
        private final Direction side;HeatPort(Direction side){this.side=side;}
        private boolean valid(){return live()&&front()==side;}
        public long getHeatStored(){return mio_icif_blast_furnace.this.getHeatStored();}
        public long getMaxHeatStored(){return HEAT_STORAGE_CAPACITY;}
        public long receiveHeat(long n,boolean sim){return valid()?mio_icif_blast_furnace.this.receiveHeat(n,sim):0;}
        public long extractHeat(long n,boolean sim){return 0;}
        public boolean canExtractHeat(){return false;}
        public boolean canReceiveHeat(){return valid()&&mio_icif_blast_furnace.this.canReceiveHeat();}
        public int getTemperature(){return mio_icif_blast_furnace.this.getTemperature();}
        public boolean isOverheated(){return mio_icif_blast_furnace.this.isOverheated();}
        public long getHeatLossPerTick(){return 0;}
        public long getMaxReceive(){return MAX_HEAT_RECEIVE;}
        public long getMaxExtract(){return 0;}
    }
}
