// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import com.miophas.singularity_iteration.core.runtime.reactor.FluidReactorCycle;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Owned tanks and containers; cycle conversion comes from the R119 ordinary-save reference. */
public class mio_icif_fluid_reactor_handler implements IFluidHandler {
    public static final int FLUID_CAPACITY=10000,INPUT_CELL_SLOT_1=0,OUTPUT_EMPTY_SLOT_1=1,
        INPUT_CELL_SLOT_2=2,OUTPUT_EMPTY_SLOT_2=3,TOTAL_ITEM_SLOTS=4;
    /**
     * IC2 {@code Recipes.liquidHeatupManager} 的冷媒→热媒映射（默认配置取值）。
     * {@code huPerMB} 即交换 1 mB 冷媒所需的热量，对应 IC2 {@code heatExchanger*} 配置项。
     */
    public record Coolant(Fluid cold,Fluid hot,int huPerMB){}
    public static final Coolant COOLANT=new Coolant(mio_icif_fluids.COOLANT.get(),mio_icif_fluids.HOTCOOLANT.get(),FluidReactorCycle.COOLANT_HU_PER_MB);
    public static final Coolant WATER=new Coolant(Fluids.WATER,mio_icif_fluids.HOTWATER.get(),FluidReactorCycle.WATER_HU_PER_MB);
    public static final List<Coolant> COOLANTS=List.of(COOLANT,WATER);

    private final BooleanSupplier available;
    private final Runnable changed;
    private int currentHeatGeneration;
    private int currentHuPerMB=FluidReactorCycle.COOLANT_HU_PER_MB;
    private CompoundTag hold=new CompoundTag();
    private final FluidTank inputTank=new FluidTank(FLUID_CAPACITY,stack->coldCoolant(stack.getFluid())!=null);
    private final FluidTank outputTank=new FluidTank(FLUID_CAPACITY,stack->hotCoolant(stack.getFluid())!=null);
    private final MachineItemHandler itemHandler=new MachineItemHandler(SlotLayout.builder().extra(4).build()){
        @Override protected void onContentsChanged(int slot){if(changed!=null)changed.run();}
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot==0?isColdContainer(stack):slot==2&&isEmptyContainer(stack);}
    };
    public mio_icif_fluid_reactor_handler(){this(()->true,()->{});}
    public mio_icif_fluid_reactor_handler(BooleanSupplier available,Runnable changed){this.available=available;this.changed=changed;}
    /** 冷流体→冷媒映射；未知流体返回 null。 */
    public static Coolant coldCoolant(Fluid fluid){for(var coolant:COOLANTS)if(fluid==coolant.cold())return coolant;return null;}
    /** 热流体→冷媒映射；未知流体返回 null。 */
    public static Coolant hotCoolant(Fluid fluid){for(var coolant:COOLANTS)if(fluid==coolant.hot())return coolant;return null;}
    /** 当前输入罐使用的冷媒；罐空时按冷却液处理（对应 IC2 无输入流体时的分支）。 */
    public Coolant activeCoolant(){var kind=coldCoolant(inputTank.getFluid().getFluid());return kind==null?COOLANT:kind;}
    /** 输入罐冷媒下标（= {@link #COOLANTS} 顺序），罐空或未知返回 -1。 */
    public int getInputCoolantOrdinal(){var kind=coldCoolant(inputTank.getFluid().getFluid());return kind==null?-1:COOLANTS.indexOf(kind);}
    /** 输出罐热媒下标，罐空或未知返回 -1。 */
    public int getOutputCoolantOrdinal(){var kind=hotCoolant(outputTank.getFluid().getFluid());return kind==null?-1:COOLANTS.indexOf(kind);}
    /** IC2 换热公式里每 1 mB 冷媒携带的热量。 */
    public int getHuPerMB(){return currentHuPerMB;}
    /** IC2 流体反应堆 GUI 显示的 {@code EmitHeat}（HU）= 本周期实际被冷媒吸收的热量。 */
    public int getEmitHeat(){return (int)Math.min(Integer.MAX_VALUE,(long)currentHeatGeneration*currentHuPerMB);}
    private boolean live(){return available.getAsBoolean()&&hold.isEmpty();}
    private static boolean isColdContainer(ItemStack stack){
        if(stack.is(mio_icif_fluids.COOLANT_BUCKET.get())||stack.is(Items.WATER_BUCKET))return true;
        return coldCoolant(mio_icif_cells.getCellFluid(stack).getFluid())!=null;
    }
    private static boolean isEmptyContainer(ItemStack stack){return stack.is(Items.BUCKET)||mio_icif_cells.isEmptyCell(stack);}
    /** 输入容器里的冷媒流体（桶按 1000 mB，单元按自身容量）。 */
    private static FluidStack coldContainerContent(ItemStack stack){
        if(stack.is(mio_icif_fluids.COOLANT_BUCKET.get()))return new FluidStack(mio_icif_fluids.COOLANT.get(),1000);
        if(stack.is(Items.WATER_BUCKET))return new FluidStack(Fluids.WATER,1000);
        return mio_icif_cells.getCellFluid(stack);
    }
    /** 该热媒对应的桶物品；未知热媒返回 null。 */
    private static net.minecraft.world.item.Item hotBucket(Fluid hot){
        if(hot==mio_icif_fluids.HOTCOOLANT.get())return mio_icif_fluids.HOTCOOLANT_BUCKET.get();
        if(hot==mio_icif_fluids.HOTWATER.get())return mio_icif_fluids.HOTWATER_BUCKET.get();
        return null;
    }
    public void processContainers(){
        if(!live())return;
        ItemStack cold=itemHandler.getStackInSlot(0),empty=itemHandler.getStackInSlot(2);
        if(isColdContainer(cold)){
            var content=coldContainerContent(cold);
            var remainder=cold.is(mio_icif_fluids.COOLANT_BUCKET.get())||cold.is(Items.WATER_BUCKET)
                ?new ItemStack(Items.BUCKET):mio_icif_cells.getEmptyCellForStack(cold);
            remainder.applyComponents(cold.getComponentsPatch());
            if(remainder.getItem() instanceof com.miophas.singularity_iteration.common.item.cell.mio_icif_dynamic_cell cell)cell.writeFluidToNBT(remainder,FluidStack.EMPTY);
            if((remainder.is(Items.BUCKET)||mio_icif_cells.isEmptyCell(remainder))&&ContainerToTank.transfer(itemHandler,0,1,inputTank,content,remainder))changed.run();
        }
        if(isEmptyContainer(empty)){
            Fluid hot=outputTank.isEmpty()?activeCoolant().hot():outputTank.getFluid().getFluid();
            ItemStack filled;
            if(empty.is(Items.BUCKET)){
                var bucket=hotBucket(hot);if(bucket==null)return;filled=new ItemStack(bucket);
            }else filled=mio_icif_cells.getFilledCellForFluidStack(hot);
            if(filled.isEmpty())return;
            var content=empty.is(Items.BUCKET)?new FluidStack(hot,1000):mio_icif_cells.getCellFluid(filled);
            filled.applyComponents(empty.getComponentsPatch());
            if((filled.is(hotBucket(hot))||FluidStack.matches(content,mio_icif_cells.getCellFluid(filled)))&&ContainerToTank.drainToContainer(itemHandler,2,3,outputTank,content,filled))changed.run();
        }
    }
    public boolean hasHold(){return !hold.isEmpty();}
    public boolean canConvert(){return hold.isEmpty()&&validTanks();}
    /** A valid pair of tanks can temporarily hold incompatible fluids after switching coolant. */
    public boolean isOutputCompatible(){return inputTank.isEmpty()||outputTank.isEmpty()
        ||outputTank.getFluid().getFluid()==activeCoolant().hot();}
    private boolean validTanks(){return (inputTank.isEmpty()||coldCoolant(inputTank.getFluid().getFluid())!=null)
        &&(outputTank.isEmpty()||hotCoolant(outputTank.getFluid().getFluid())!=null)&&getInputFluidAmount()<=FLUID_CAPACITY&&getOutputFluidAmount()<=FLUID_CAPACITY;}
    /** Called inside the owner's inventory commit; no callbacks expose a half-completed cycle. */
    public void commitCycle(int cold,int hot,int converted){
        commitCycle(cold,hot,converted,FluidReactorCycle.COOLANT_HU_PER_MB);
    }
    /**
     * 提交一次循环的罐体状态。热媒流体沿用输出罐现有种类，罐空时按本次冷媒推导，
     * 与 IC2 {@code outputTank.canFillFluidType()} 不允许混装的行为一致。
     */
    public void commitCycle(int cold,int hot,int converted,int huPerMB){
        if(converted>0&&!isOutputCompatible())throw new IllegalStateException("Incompatible reactor hot fluid");
        Fluid coldFluid=inputTank.isEmpty()?activeCoolant().cold():inputTank.getFluid().getFluid();
        Fluid hotFluid=outputTank.isEmpty()?activeCoolant().hot():outputTank.getFluid().getFluid();
        inputTank.setFluid(cold==0?FluidStack.EMPTY:new FluidStack(coldFluid,cold));
        outputTank.setFluid(hot==0?FluidStack.EMPTY:new FluidStack(hotFluid,hot));
        currentHeatGeneration=converted;
        currentHuPerMB=huPerMB>0?huPerMB:FluidReactorCycle.COOLANT_HU_PER_MB;
    }
    /** SI legacy standalone entry point; owner integration uses the atomic cycle commit instead. */
    public int tick(int emittedHeat){
        if(!live()||!canConvert())return 0;processContainers();
        var kind=activeCoolant();
        var converted=FluidReactorCycle.convert(emittedHeat,getInputFluidAmount(),getOutputFluidAmount(),FLUID_CAPACITY,kind.huPerMB(),isOutputCompatible());
        commitCycle(getInputFluidAmount()-converted.millibuckets(),getOutputFluidAmount()+converted.millibuckets(),converted.millibuckets(),kind.huPerMB());
        changed.run();return converted.millibuckets();
    }
    public FluidTank getInputTank(){return inputTank;}
    public FluidTank getOutputTank(){return outputTank;}
    public int getCurrentHeatGeneration(){return currentHeatGeneration;}
    public int getInputFluidAmount(){return inputTank.getFluidAmount();}
    public int getOutputFluidAmount(){return outputTank.getFluidAmount();}
    public int getInputCapacity(){return FLUID_CAPACITY;}
    public int getOutputCapacity(){return FLUID_CAPACITY;}
    public MachineItemHandler getItemHandler(){return itemHandler;}
    public ItemStack getStackInSlot(int slot){return itemHandler.getStackInSlot(slot);}
    public void setStackInSlot(int slot,ItemStack stack){itemHandler.setStackInSlot(slot,stack);}
    public void saveToNBT(CompoundTag tag,HolderLookup.Provider registries){
        tag.put("InputTank",inputTank.writeToNBT(registries,new CompoundTag()));tag.put("OutputTank",outputTank.writeToNBT(registries,new CompoundTag()));
        tag.putInt("CurrentHeatGen",currentHeatGeneration);tag.putInt("CurrentHuPerMB",currentHuPerMB);
        tag.put("ItemHandler",itemHandler.serializeNBT(registries));tag.put("scex_fluid_hold",hold.copy());
    }
    public void loadFromNBT(CompoundTag tag,HolderLookup.Provider registries){
        hold=tag.getCompound("scex_fluid_hold").copy();inputTank.readFromNBT(registries,tag.getCompound("InputTank"));outputTank.readFromNBT(registries,tag.getCompound("OutputTank"));
        currentHeatGeneration=Math.max(0,tag.getInt("CurrentHeatGen"));
        int savedHuPerMB=tag.getInt("CurrentHuPerMB");currentHuPerMB=savedHuPerMB>0?savedHuPerMB:FluidReactorCycle.COOLANT_HU_PER_MB;
        if(tag.contains("ItemHandler"))itemHandler.deserializeNBT(registries,tag.getCompound("ItemHandler"));
        if(!hold.contains("invalid_fluid_state")&&(!validTanks()||!tag.getCompound("InputTank").isEmpty()&&inputTank.isEmpty()||!tag.getCompound("OutputTank").isEmpty()&&outputTank.isEmpty()))hold.put("invalid_fluid_state",tag.copy());
    }
    @Override public int getTanks(){return 2;}
    @Override public FluidStack getFluidInTank(int tank){return (tank==0?inputTank:outputTank).getFluid().copy();}
    @Override public int getTankCapacity(int tank){return FLUID_CAPACITY;}
    @Override public boolean isFluidValid(int tank,FluidStack stack){return tank==0&&coldCoolant(stack.getFluid())!=null;}
    @Override public int fill(FluidStack stack,FluidAction action){if(!live())return 0;int n=inputTank.fill(stack,action);if(n>0&&action.execute())changed.run();return n;}
    @Override public FluidStack drain(FluidStack stack,FluidAction action){if(!live())return FluidStack.EMPTY;var out=outputTank.drain(stack,action);if(!out.isEmpty()&&action.execute())changed.run();return out;}
    @Override public FluidStack drain(int amount,FluidAction action){if(!live())return FluidStack.EMPTY;var out=outputTank.drain(amount,action);if(!out.isEmpty()&&action.execute())changed.run();return out;}
}
