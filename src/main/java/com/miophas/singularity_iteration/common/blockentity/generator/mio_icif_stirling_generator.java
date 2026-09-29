// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_block_stirling_generator;
import com.miophas.singularity_iteration.common.menu.generator.StirlingGeneratorMenu;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.machine.IGeneratorBlock;
import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyAcceptor;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergySource;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import com.miophas.singularity_iteration.core.runtime.energy.DemandEnergySource;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
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
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** Independently authored from normal R102 HU/EU receipts. No predecessor implementation was inspected. */
public class mio_icif_stirling_generator extends AbstractHeatBlockEntity
        implements IEnergySource, IGeneratorBlock, DemandEnergySource {
    private final CustomEUEnergyStorage energy=new CustomEUEnergyStorage(200000,0,128,CableTier.MV);
    private long lastFrame=Long.MIN_VALUE,frameHu,generatedFrame,uncertainHu;
    private long generatedAt=Long.MIN_VALUE,lastOutput;
    private boolean acquiring;
    private CompoundTag legacyHold=new CompoundTag();
    private String failure="";
    public mio_icif_stirling_generator(BlockPos pos,BlockState state){
        super(mio_icif_block_entities.STIRLING_GENERATOR_ENTITY_TYPE.get(),pos,state,20000,1000,0,20,1000,0);
        energy.scexSetNetworkControlled(IndependentSiEnergy.controls(state));energy.setAsPowerSource(128);
    }
    private void dirty(){setChanged();if(level!=null && !level.isClientSide)ContainerToTank.markUnsaved(this);}
    private boolean live(){
        if(!(level instanceof ServerLevel server) || !server.getServer().isSameThread() || isRemoved())return false;
        var chunk=server.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4);
        return chunk!=null && chunk.getBlockEntity(worldPosition,LevelChunk.EntityCreationType.CHECK)==this;
    }
    private void frame(){long now=level==null?0:level.getGameTime();if(lastFrame!=now){lastFrame=now;frameHu=0;generatedFrame=0;}}
    private Direction input(){return getBlockState().getValue(mio_icif_block_stirling_generator.FACING).getOpposite();}
    private IMioIcifCapabilities.IHeatStorage supplier(){
        if(!(level instanceof ServerLevel server))return null;
        var side=input();var at=worldPosition.relative(side);
        if(server.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4)==null)return null;
        return server.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK,at,side.getOpposite());
    }
    private static EnergyAmount eu(long hu){return new EnergyAmount(hu/2,(hu%2)*(EnergyAmount.UNITS/2));}
    private long availableHeat(){
        if(!live() || !energy.scexNetworkControlled() || acquiring || uncertainHu!=0 || !legacyHold.isEmpty())return 0;
        frame();long limit=Math.max(0,1000-frameHu);long owned=Math.min(limit,heatStorage.getHeatStored());
        if(owned==limit)return owned;
        try{
            var source=supplier();if(source==null || source==this || !source.canExtractHeat())return owned;
            long reply=source.extractHeat(limit-owned,true);
            return reply>=0 && reply<=limit-owned ? owned+reply : owned;
        }catch(RuntimeException unavailable){return owned;}
    }
    @Override public CustomEUEnergyStorage ownedEnergy(){return energy;}
    @Override public int outputFaces(){return 63 ^ (1 << input().get3DDataValue());}
    @Override public EnergyAmount potentialEnergy(){
        return energy.scexExactAmount().add(eu(availableHeat()).min(energy.scexExactAmount().roomBelow(energy.getCapacity())));
    }
    @Override public void prepareEnergy(EnergyAmount requested){
        if(!live() || acquiring || uncertainHu!=0 || !legacyHold.isEmpty() || !energy.scexNetworkControlled())return;
        var balance=energy.scexExactAmount();if(requested.compareTo(balance)<=0)return;
        frame();var need=requested.subtract(balance).min(balance.roomBelow(energy.getCapacity()));
        // At most 1000 integer HU per frame. A sub-half-EU route can leave a paid fractional remainder.
        long count=need.whole()>=500?1000:need.whole()*2+(need.fraction()==0?0:need.fraction()<=EnergyAmount.UNITS/2?1:2);
        count=Math.min(count,Math.max(0,1000-frameHu));
        long room=balance.roomBelow(energy.getCapacity()).whole();
        if(room<500)count=Math.min(count,room*2+(balance.roomBelow(energy.getCapacity()).fraction()>=EnergyAmount.UNITS/2?1:0));
        if(count==0)return;
        count-=convertOwnedHeat(count);
        if(count==0)return;
        acquiring=true;
        try{
            var source=supplier();if(source==null || source==this || !source.canExtractHeat())return;
            long proposed=source.extractHeat(count,true);if(proposed<=0 || proposed>count)return;
            uncertainHu=proposed;dirty();
            long actual=source.extractHeat(proposed,false);
            if(actual<0 || actual>proposed){failure="Invalid external HU extraction receipt";dirty();return;}
            uncertainHu=0;credit(actual);failure="";dirty();
        }catch(RuntimeException problem){if(uncertainHu>0){failure=problem.getClass().getName();dirty();}}
        finally{acquiring=false;}
    }
    private void credit(long hu){
        var value=eu(hu);var got=energy.scexGenerateEnergy(value,false);
        if(!got.equals(value))throw new IllegalStateException("Owned HU exceeds reserved EU capacity");
        frameHu+=hu;generatedFrame+=hu;if(hu>0){generatedAt=level.getGameTime();lastOutput=generatedFrame/2;}dirty();
    }
    /**
     * 把自身 HU 缓冲里的热量转成 EU（2 HU = 1 EU），上限同时受本帧预算、内部 EU 剩余空间与缓冲实有量约束。
     * @return 实际消耗的 HU
     */
    private long convertOwnedHeat(long maxHu){
        if(maxHu<=0)return 0;
        var room=energy.scexExactAmount().roomBelow(energy.getCapacity());
        if(room.isZero())return 0;
        long roomHu=room.whole()*2+(room.fraction()>=EnergyAmount.UNITS/2?1:0);
        long owned=Math.min(Math.min(maxHu,roomHu),heatStorage.getHeatStored());
        if(owned<=0)return 0;
        heatStorage.consumeHeatInternal(owned,false);credit(owned);return owned;
    }
    /**
     * IC2 {@code TileEntityConversionGenerator} 的行为：每 tick 主动把缓冲热量换成 EU 存进内部缓冲，
     * 不等电网索电（存储起来的 EU 由电网或线缆自行抽走）。高级斯特林同样是每 tick 主动转换，
     * 否则"热交换机推热 → 缓冲满 → 却永远不出电"。
     */
    private void selfConvert(){
        // 这里只消费【自身 HU 缓冲】里的热，与"对外抽热是否不确定（uncertainHu）"、旧存档残留（legacyHold）
        // 无关：这两者只应保守地限制【对外抽取】那条路径（availableHeat/prepareEnergy）。
        // 原先把它们也写进本方法的守卫，会让一台机器彻底停机，而且这两个标志随存档保存，
        // 玩家除了拆掉重放别无他法（缓存抽空也不会恢复）—— 现仅保留存活/事务中判定。
        if(!live() || acquiring)return;
        frame();convertOwnedHeat(Math.max(0,1000-frameHu));
    }
    public static void tick(Level level,BlockPos pos,BlockState state,mio_icif_stirling_generator m){
        if(!m.live())return;m.frame();
        m.selfConvert();
        var actual=m.getBlockState();boolean active=m.isWorking();
        if(actual.getValue(mio_icif_block_stirling_generator.ACTIVE)!=active)
            level.setBlock(pos,actual.setValue(mio_icif_block_stirling_generator.ACTIVE,active),3);
    }
    @Override public void setLevel(Level level){super.setLevel(level);energy.setBlockContext(level,worldPosition);}
    @Override public void onLoad(){super.onLoad();if(energy.scexNetworkControlled())IndependentSiEnergy.changed(this);}
    @Override public void setRemoved(){super.setRemoved();IndependentSiEnergy.changed(this);}
    @Override public void clearRemoved(){super.clearRemoved();if(energy.scexNetworkControlled())IndependentSiEnergy.changed(this);}
    public CustomEUEnergyStorage getEnergyStorage(){return energy;}
    public IEUEnergyStorage getEnergyStorageCapability(Direction side){return side==input()?null:energy;}
    // 基类按构造参数 (capacity=20000, maxReceive=1000, maxExtract=0) 已给出正确语义：
    // 背面可被动接收 HU（≤1000/t、缓冲 20000），但任何设备都不能从它抽取热量。
    // 这里曾经用四个覆写把接收/抽取全部掐死，导致 getHeatStorageCapability 暴露的背面入口形同虚设
    // （注册处是侧面感知的 lambda，只有 input() 面会拿到它），同时让 availableHeat()/prepareEnergy()
    // 里"优先消耗自身缓冲"的分支永远空跑。现直接沿用基类实现。
    // 真正的"按需抽取"仍由 supplier() 在电网需要电力时执行，两条路径互补。
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(Direction side){return side==null?this:side==input()?this:null;}
    public long getHeatBuffer(){return heatStorage.getHeatStored();}
    public long getEnergyStored(){return energy.getAmount();}
    public long getEnergyCapacity(){return energy.getCapacity();}
    public long getEnergyOutput(){return isWorking()?lastOutput:0;}
    public boolean isWorking(){return level!=null && generatedAt!=Long.MIN_VALUE && level.getGameTime()-generatedAt<=1;}
    public long getUncertainHeat(){return uncertainHu;}
    public boolean hasLegacyHold(){return !legacyHold.isEmpty();}
    public long extractPowerForConsumer(long request,boolean simulate){return live()?energy.extract(request,simulate):0;}
    // ABI retained; all actual grid traffic belongs to IndependentSiEnergy.
    @Override public double getOfferedEnergy(){return 0;}
    @Override public void drawEnergy(double amount){}
    @Override public int getSourceTier(){return CableTier.MV.getTier();}
    @Override public boolean emitsEnergyTo(IEnergyAcceptor acceptor,Direction side){return false;}
    @Override public CableTier getCableTier(){return CableTier.MV;}
    @Override public boolean isBurning(){return isWorking();}
    @Override public int getBurnTime(){return 0;}
    @Override public int getMaxBurnTime(){return 0;}
    @Override public long getPowerOutput(){return isWorking()?128:0;}
    @Override public ItemStack getFuelSlotItem(){return ItemStack.EMPTY;}
    @Override public ItemStack getChargeSlotItem(){return ItemStack.EMPTY;}
    @Override public int getDefaultBurnTime(){return 0;}
    @Override public void setBurnTime(int ticks){}
    @Override public Component getDisplayName(){return Component.translatable("block.mio_icif.generator.block_stirling_generator");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){
        return new StirlingGeneratorMenu(id,inventory,this,null,new SimpleContainerData(6));
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){
        super.saveAdditional(tag,provider);tag.putLong("Energy",energy.getAmount());tag.putLong("scex_energy_fraction",energy.scexSavedFraction());
        var saved=new CompoundTag();saved.putInt("version",1);saved.putLong("uncertain",uncertainHu);saved.putString("failure",failure);
        saved.put("legacy_hold",legacyHold.copy());tag.put("scex_stirling",saved);
    }
    @Override public void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){
        super.loadAdditional(tag,provider);energy.setEnergy(Math.max(0,tag.getLong("Energy")));energy.scexLoadFraction(tag.getLong("scex_energy_fraction"));
        legacyHold=new CompoundTag();uncertainHu=0;failure="";
        if(tag.contains("scex_stirling",Tag.TAG_COMPOUND)){
            var saved=tag.getCompound("scex_stirling");
            if(saved.getInt("version")==1){legacyHold=saved.getCompound("legacy_hold").copy();uncertainHu=Math.max(0,saved.getLong("uncertain"));failure=saved.getString("failure");}
            else legacyHold.put("unknown_stirling_version",saved.copy());
        }else if(tag.contains("HeatBuffer") && tag.getLong("HeatBuffer")!=0)legacyHold.put("HeatBuffer",tag.get("HeatBuffer").copy());
        acquiring=false;lastFrame=Long.MIN_VALUE;frameHu=0;generatedFrame=0;
    }
}
