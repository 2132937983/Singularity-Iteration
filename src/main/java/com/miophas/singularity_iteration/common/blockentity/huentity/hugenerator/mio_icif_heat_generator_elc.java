// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.huentity.hugenerator;

import com.miophas.singularity_iteration.common.block.hugenerator.mio_icif_block_heat_generator_elc;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.common.menu.huentity.HeatGeneratorElcMenu;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.item.electric.IElectricItem;
import com.miophas.singularity_iteration.core.api.machine.IHeatGeneratorBlock;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** Independent R106 adapter. R102 normal fixed-binary observations specify 1 EU/HU and 10 HU/coil/tick.
 * R104 public SI ABI specifies inventory/menu integration. The predecessor body was never opened. */
public class mio_icif_heat_generator_elc extends AbstractProcessingMachineBlockEntity
        implements IHeatGeneratorBlock, IMioIcifCapabilities.IHeatStorage {
    public static final int BATTERY_SLOT=0, COIL_SLOT_START=1, COIL_SLOT_COUNT=10, TOTAL_SLOTS=11;
    /** IC2 getMaxHeatEmittedPerTick() 的绝对上限：10 枚线圈 × 10 HU/t。 */
    private static final int MAX_HEAT=100;
    /** IC2 的放电槽是 InvSlotDischarge(tier = 4 = EV)，只接受不超过 EV 的电池。 */
    private static final int BATTERY_MAX_TIER=4;
    private long heat,uncertainHeat;
    private String transferFailure="";
    private boolean transferring;
    /** IC2 TileEntityHeatSourceInventory#transmitHeat：最近一次真正被抽走的 HU。 */
    private int transmitHeat;
    private long outputFrame=Long.MIN_VALUE,frameExtracted;
    private final FacePort[] ports=new FacePort[6];

    public mio_icif_heat_generator_elc(BlockPos pos,BlockState state) {
        super(pos,state,mio_icif_block_entities.HEAT_GENERATOR_ELC.get(),10000,2048,0,1,
                SlotLayout.builder().battery().coil(COIL_SLOT_COUNT).build(),0,CableTier.EV);
        for (var side:Direction.values()) ports[side.get3DDataValue()]=new FacePort(side);
    }
    @Override protected MachineItemHandler createItemHandler(SlotLayout layout) {
        var result=new MachineItemHandler(layout) {
            @Override protected void onContentsChanged(int slot) { dirty(); }
        };
        result.setValidator(this);return result;
    }
    @Override protected boolean isItemValidForSlot(int slot,ItemStack stack) {
        return slot==BATTERY_SLOT ? isBatteryAllowed(stack)
                : slot>=COIL_SLOT_START && slot<TOTAL_SLOTS && stack.is(mio_icif_resources.COIL.get());
    }
    /**
     * IC2 {@code InvSlotDischarge.accepts()} 的等级门槛：它用
     * {@code ElectricItem.manager.discharge(stack, ∞, tier = 4, ...)} 试探，等级高于 EV 的电池
     * 既进不了 GUI 槽位、也进不了自动化。mio_icif 的 IBatteryItem 不带等级信息（仅在本模组内
     * 使用），因此该上限只对实现了 IC2 风格 {@link IElectricItem} 的物品生效。
     */
    public static boolean isBatteryTierAllowed(ItemStack stack) {
        return !(stack.getItem() instanceof IElectricItem electric) || electric.getTier(stack)<=BATTERY_MAX_TIER;
    }
    private boolean isBatteryAllowed(ItemStack stack) { return isBattery(stack) && isBatteryTierAllowed(stack); }
    /**
     * 自动化访问权限对齐 IC2：
     * <ul>
     *   <li>coilSlot = InvSlotConsumableItemStack(..., 10, coil) → {@code Access.I}：
     *       管道/漏斗可插入线圈，但不能抽出（且该槽的 preferredSide 为 TOP，但 IC2 的
     *       func_180462_a 在本机只有线圈可插入时对任意面都返回 true，故不限面）；</li>
     *   <li>dischargeSlot = InvSlotDischarge({@code Access.NONE})：电池槽与自动化完全隔绝，
     *       只能从 GUI 手动放入/取出。</li>
     * </ul>
     */
    @Override protected int[] getSlotsForDirection(Direction side) {
        int[] slots=new int[COIL_SLOT_COUNT];
        for(int i=0;i<COIL_SLOT_COUNT;i++)slots[i]=COIL_SLOT_START+i;
        return slots;
    }
    @Override protected boolean canInsertItem(int slot,ItemStack stack,Direction side) {
        return slot>=COIL_SLOT_START && slot<TOTAL_SLOTS && isItemValidForSlot(slot,stack);
    }
    /** IC2 的 Access.I（线圈）与 Access.NONE（电池）都不允许自动化抽出。 */
    @Override protected boolean canExtractItem(int slot,Direction side) { return false; }
    @Override protected boolean canWork() { return false; }
    @Override protected void doWork() { }
    private void dirty() { setChanged();if(level!=null && !level.isClientSide)ContainerToTank.markUnsaved(this); }
    private boolean live() {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread() || isRemoved())return false;
        var chunk=server.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4);
        return chunk!=null && chunk.getBlockEntity(worldPosition,LevelChunk.EntityCreationType.CHECK)==this;
    }
    private Direction front() { return getBlockState().getValue(mio_icif_block_heat_generator_elc.FACING); }
    private void frame() {
        long now=level==null ? 0 : level.getGameTime();
        if(now!=outputFrame){outputFrame=now;frameExtracted=0;}
    }
    public static void tick(Level level,BlockPos pos,BlockState state,mio_icif_heat_generator_elc m) {
        if(!m.live() || m.transferring)return;
        m.frame();m.handleBatterySlot();
        int generation=m.getHeatGeneration();
        int made=0;
        if(m.uncertainHeat==0) {
            // IC2 TileEntityHeatSourceInventory#updateEntityServer + fillHeatBuffer：缓冲上限就是本 tick
            // 的产热能力（线圈数 × 10），差多少补多少；换算为 1 EU → 1 HU（配置 outputMultiplier = 1.0）。
            long budget=Math.min(m.getEnergyStorageInternal().getAmount(),Math.max(0,(long)generation-m.heat));
            if(budget>0) {
                long paid=m.getEnergyStorageInternal().consumeEnergyInternal(budget,false);
                m.heat+=paid;made=(int)paid;m.dirty();
            }
            m.pushFront();
        }
        // 对齐原版 IC2：缓冲已满时不算“停机”（IC2 的激活状态在缓冲满时保持上一次的值），
        // 否则用热方满时会在 100/0 之间抖动。
        boolean bufferFull=generation>0 && m.heat>=generation;
        m.isWorking=generation>0 && (made>0||bufferFull);m.progress=m.isWorking?1:0;
        var actual=m.getBlockState();
        if(actual.getValue(mio_icif_block_heat_generator_elc.ACTIVE)!=m.isWorking)
            level.setBlock(pos,actual.setValue(mio_icif_block_heat_generator_elc.ACTIVE,m.isWorking),3);
    }
    /** One reserved transfer. An invalid receipt retains the unknown amount and blocks further conversion. */
    private void pushFront() {
        long limit=bandwidth();
        if(heat==0 || limit<=0 || frameExtracted>=limit || !(level instanceof ServerLevel server))return;
        var side=front();var targetPos=worldPosition.relative(side);
        if(server.getChunkSource().getChunkNow(targetPos.getX()>>4,targetPos.getZ()>>4)==null)return;
        var target=server.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK,targetPos,side.getOpposite());
        if(target==null || target==this)return;
        transferring=true;
        try {
            if(!target.canReceiveHeat())return;
            long offered=Math.min(heat,limit-frameExtracted);
            long proposed=target.receiveHeat(offered,true);
            if(proposed<=0 || proposed>offered)return;
            heat-=proposed;uncertainHeat=proposed;dirty();
            long accepted=target.receiveHeat(proposed,false);
            if(accepted<0 || accepted>proposed) {
                transferFailure="Receiver returned an invalid HU receipt";dirty();return;
            }
            heat+=proposed-accepted;uncertainHeat=0;frameExtracted+=accepted;recordTransmit((int)accepted);
            transferFailure="";dirty();
        } catch(RuntimeException failure) {
            if(uncertainHeat>0){transferFailure=failure.getClass().getName();dirty();}
        } finally {transferring=false;}
    }
    /** IC2 只统计非空线圈槽（槽位本身已限定只能放线圈）。 */
    public int getCoilCount() {
        int count=0;
        for(int slot=COIL_SLOT_START;slot<TOTAL_SLOTS;slot++) {
            if(!itemHandler.getStackInSlot(slot).isEmpty())count++;
        }
        return count;
    }
    /** IC2 getMaxHeatEmittedPerTick()：线圈数 × 10（10 枚封顶 100），也是正面唯一的输出带宽。 */
    public int getMaxHeatEmittedPerTick(){return getHeatGeneration();}
    /** 每 tick 允许取走的 HU 上限，等同 IC2 getConnectionBandwidth() 对正面的返回值。 */
    private long bandwidth(){return Math.min(MAX_HEAT,getHeatGeneration());}
    /** IC2 transmitHeat：最近一次实际被抽走的 HU（空闲时保留上一次值，与原版一致）。 */
    public int getTransmitHeat(){return transmitHeat;}
    private void recordTransmit(int amount){if(amount>0)transmitHeat=amount;}
    public IMioIcifCapabilities.IHeatStorage getHeatStorage(){return this;}
    /** IC2 facingMatchesDirection：只有正面能取热，其余面（含无方向查询）一律返回 0。 */
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(Direction side) {
        return side!=null && side==front() ? ports[side.get3DDataValue()] : null;
    }
    @Override public long getHeatStored(){return heat;}
    /**
     * 容量 = IC2 的热缓冲上限 getMaxHeatEmittedPerTick()（线圈数 × 10，封顶 100）。
     * 下限取当前存量：拆掉线圈时缓冲里可能仍留着旧值，不能让能力快照把它夹掉。
     */
    @Override public long getMaxHeatStored(){return Math.max(bandwidth(),heat);}
    @Override public long receiveHeat(long amount,boolean simulate){return 0;}
    @Override public long extractHeat(long amount,boolean simulate) {
        if(transferring || amount<=0)return 0;
        frame();long taken=Math.min(Math.min(amount,heat),Math.max(0,bandwidth()-frameExtracted));
        if(!simulate && taken>0){heat-=taken;frameExtracted+=taken;recordTransmit((int)taken);dirty();}return taken;
    }
    @Override public boolean canExtractHeat(){frame();return !transferring && heat>0 && frameExtracted<bandwidth();}
    @Override public boolean canReceiveHeat(){return false;}
    /**
     * 展示用温度。原版 IC2 的热源（TileEntityHeatSourceInventory）没有温度/过热概念
     * （对照液体热交换机：过热检查已移除），因此这里只按缓冲占空比给一个不会触发过热的读数；
     * 此前用 20..1000 线性映射，导致缓冲一满就恒报“过热”。
     */
    @Override public int getTemperature(){return com.miophas.singularity_iteration.core.api.util.BoundedUnits.gauge(heat,Math.max(1,getMaxHeatStored()),20,700);}
    @Override public boolean isOverheated(){return false;}
    @Override public long getHeatLossPerTick(){return 0;}
    @Override public long getMaxReceive(){return 0;}
    @Override public long getMaxExtract(){return bandwidth();}
    @Override public void setHeat(long value){if(transferring)return;long next=Math.max(0,value);if(next!=heat){heat=next;dirty();}}
    /** IC2 热源没有独立容量概念（缓冲上限由线圈数决定），保留 ABI 但忽略写入。 */
    @Override public void setCapacity(long value){ }
    @Override public long applyHeatLoss(){return 0;}
    @Override public long consumeHeatInternal(long amount,boolean simulate){return extractHeat(amount,simulate);}
    @Override public long generateHeatInternal(long amount,boolean simulate) {
        if(transferring || amount<=0)return 0;long accepted=Math.min(amount,Math.max(0,MAX_HEAT-heat));
        if(!simulate && accepted>0){heat+=accepted;dirty();}return accepted;
    }
    public int getHeatGeneration(){return getCoilCount()*getHeatPerCoil();}
    public static int getHeatPerCoil(){return 10;}
    public float getEfficiency(){return getCoilCount()/10.0f;}
    public long getHeatCapacity(){return getMaxHeatStored();}
    public float getHeatProgress(){long max=getMaxHeatStored();return max<=0?0:(float)Math.min(1.0,(double)heat/max);}
    @Override public Component getDisplayName(){return Component.translatable("block.mio_icif.hugenerator.block_heat_generator_elc");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new HeatGeneratorElcMenu(id,inventory,this);}
    /** 当前发热能力（对齐液体热交换机的 getHeatOutput()，供 Jade 的“产生”一行使用）。 */
    @Override public int getHeatOutput(){return getHeatGeneration();}
    @Override public boolean isGenerating(){return isWorking;}
    @Override public int getBurnTime(){return 0;}
    @Override public int getBurnDuration(){return 0;}
    @Override public int getHeatGenerationRate(){return getHeatGeneration();}
    public long getUncertainHeat(){return uncertainHeat;}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider) {
        super.saveAdditional(tag,provider);var saved=new CompoundTag();saved.putInt("version",1);
        saved.putLong("heat",heat);saved.putLong("uncertain",uncertainHeat);
        saved.putString("failure",transferFailure);tag.put("scex_electric_heater",saved);
    }
    @Override public void loadAdditional(CompoundTag tag,HolderLookup.Provider provider) {
        super.loadAdditional(tag,provider);heat=0;uncertainHeat=0;transferFailure="";
        if(tag.contains("scex_electric_heater",Tag.TAG_COMPOUND)) {
            var saved=tag.getCompound("scex_electric_heater");
            if(saved.getInt("version")!=1)throw new IllegalArgumentException("Unsupported electric heater save version");
            heat=Math.max(0,saved.getLong("heat"));
            uncertainHeat=Math.max(0,saved.getLong("uncertain"));transferFailure=saved.getString("failure");
        }
        frameExtracted=0;outputFrame=Long.MIN_VALUE;transferring=false;transmitHeat=0;
    }
    private final class FacePort implements IMioIcifCapabilities.IHeatStorage {
        private final Direction side;
        FacePort(Direction side){this.side=side;}
        private boolean usable(){return live() && side==front();}
        @Override public long getHeatStored(){return heat;}
        @Override public long getMaxHeatStored(){return mio_icif_heat_generator_elc.this.getMaxHeatStored();}
        @Override public long receiveHeat(long amount,boolean simulate){return 0;}
        @Override public long extractHeat(long amount,boolean simulate){return usable()?mio_icif_heat_generator_elc.this.extractHeat(amount,simulate):0;}
        @Override public boolean canExtractHeat(){return usable() && mio_icif_heat_generator_elc.this.canExtractHeat();}
        @Override public boolean canReceiveHeat(){return false;}
        @Override public int getTemperature(){return mio_icif_heat_generator_elc.this.getTemperature();}
        @Override public boolean isOverheated(){return mio_icif_heat_generator_elc.this.isOverheated();}
        @Override public long getHeatLossPerTick(){return 0;}
        @Override public long getMaxReceive(){return 0;}
        @Override public long getMaxExtract(){return usable()?bandwidth():0;}
        @Override public long consumeHeatInternal(long amount,boolean simulate){return extractHeat(amount,simulate);}
        @Override public long generateHeatInternal(long amount,boolean simulate){return 0;}
    }
}
