// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.reactor;
import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.item.IReactorChamber;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergySource;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyAcceptor;
import com.miophas.singularity_iteration.core.api.heat.IHeatStorage;
import com.miophas.singularity_iteration.core.runtime.reactor.GuardedReactorHeat;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.items.IItemHandler;

/** A live contact of exactly one adjacent core; owns neither heat nor EU nor items. */
public class mio_icif_reactor_chamber extends BlockEntity implements MenuProvider,IEnergySource,IReactorChamber {
 private BlockPos savedConnection;private long lastValidationTick;
 public mio_icif_reactor_chamber(BlockPos pos,BlockState state){super(mio_icif_block_entities.REACTOR_CHAMBER_ENTITY_TYPE.get(),pos,state);}
 private boolean live(){
  if(!(level instanceof ServerLevel w)||!w.getServer().isSameThread()||isRemoved())return false;
  var c=w.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4);
  return c!=null&&c.getBlockState(worldPosition).is(getBlockState().getBlock())&&c.getBlockEntity(worldPosition,LevelChunk.EntityCreationType.CHECK)==this;
 }
 public mio_icif_nuclear_reactor_generator getConnectedReactor(){
  if(!live())return null;var w=(ServerLevel)level;mio_icif_nuclear_reactor_generator result=null;
  for(var side:Direction.values()){
   var at=worldPosition.relative(side);var c=w.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4);if(c==null)continue;
   if(c.getBlockEntity(at,LevelChunk.EntityCreationType.CHECK) instanceof mio_icif_nuclear_reactor_generator core&&core.isLiveReactor()){
    if(result!=null)return null;result=core;
   }
  }return result;
 }
 public BlockPos getConnectedReactorPos(){var core=getConnectedReactor();return core==null?null:core.getBlockPos();}
 private void refresh(){var next=getConnectedReactorPos();if(!java.util.Objects.equals(next,savedConnection)){savedConnection=next;setChanged();}lastValidationTick=level==null?0:level.getGameTime();}
 @Override public void onLoad(){super.onLoad();refresh();IndependentSiEnergy.changed(this);}
 @Override public void setRemoved(){super.setRemoved();IndependentSiEnergy.changed(this);}
 @Override public void clearRemoved(){super.clearRemoved();IndependentSiEnergy.changed(this);}
 public static void tick(Level world,BlockPos pos,BlockState state,mio_icif_reactor_chamber chamber){if(!chamber.live())return;if(world.getGameTime()%20==0)com.miophas.singularity_iteration.common.reactor.ChamberTopology.checkAndDrop(world,pos);if(!chamber.live())return;chamber.refresh();chamber.spawnHeatParticles(world,pos);}
 @Override public Component getDisplayName(){var core=getConnectedReactor();return core==null?Component.translatable("container.mio_icif.reactor_chamber"):core.getDisplayName();}
 @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){var core=getConnectedReactor();return core==null?null:core.createMenu(id,inventory,player);}
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){
  super.saveAdditional(tag,provider);var at=getConnectedReactorPos();
  if(at!=null){tag.putInt("ReactorX",at.getX());tag.putInt("ReactorY",at.getY());tag.putInt("ReactorZ",at.getZ());}
  tag.putBoolean("ConnectionValidated",at!=null);tag.putLong("LastValidationTick",lastValidationTick);
 }
 @Override public void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){
  super.loadAdditional(tag,provider);savedConnection=tag.contains("ReactorX")&&tag.contains("ReactorY")&&tag.contains("ReactorZ")?new BlockPos(tag.getInt("ReactorX"),tag.getInt("ReactorY"),tag.getInt("ReactorZ")):null;lastValidationTick=tag.getLong("LastValidationTick");
 }
 public IEUEnergyStorage getEnergyStorageCapability(Direction side){
  var expected=getConnectedReactor();if(expected==null)return null;var delegate=expected.getEnergyStorageCapability(side);
  if(delegate==null)return null;
  return new IEUEnergyStorage(){
   private boolean available(){return getConnectedReactor()==expected&&expected.outputFaces()!=0;}
   @Override public long receive(long n,boolean simulate){return 0;}
   @Override public long extract(long n,boolean simulate){return available()?delegate.extract(n,simulate):0;}
   @Override public long getAmount(){return available()?delegate.getAmount():0;}
   @Override public long getCapacity(){return available()?delegate.getCapacity():0;}
   @Override public boolean canReceive(){return false;}
   @Override public boolean canExtract(){return available()&&delegate.canExtract();}
   @Override public boolean canConnect(CableTier tier){return available()&&delegate.canConnect(tier);}
   @Override public long generateEnergy(long n,boolean simulate){return 0;}
   @Override public long useEnergy(long n,boolean simulate){return 0;}
  };
 }
 public IHeatStorage getHeatStorageCapability(Direction side){
  var expected=getConnectedReactor();if(expected==null)return null;
  var delegate=expected.getHeatStorageCapability(side);if(delegate==null)return null;
  return new GuardedReactorHeat(expected.getHeatStorage(),()->getConnectedReactor()==expected);
 }
 public IItemHandler getItemHandlerCapability(Direction side){
  var expected=getConnectedReactor();if(expected==null)return null;var delegate=expected.getItemHandlerCapability(side);
  if(delegate==null)return null;
  return new IItemHandler(){
   private boolean available(){return getConnectedReactor()==expected;}
   @Override public int getSlots(){return delegate.getSlots();}
   @Override public ItemStack getStackInSlot(int slot){return available()?delegate.getStackInSlot(slot):ItemStack.EMPTY;}
   @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return available()?delegate.insertItem(slot,stack,simulate):stack;}
   @Override public ItemStack extractItem(int slot,int n,boolean simulate){return available()?delegate.extractItem(slot,n,simulate):ItemStack.EMPTY;}
   @Override public int getSlotLimit(int slot){return delegate.getSlotLimit(slot);}
   @Override public boolean isItemValid(int slot,ItemStack stack){return available()&&delegate.isItemValid(slot,stack);}
  };
 }
 // The independent grid settles every core once using all its contact positions.
 @Override public boolean emitsEnergyTo(IEnergyAcceptor acceptor,Direction side){return false;}
 @Override public double getOfferedEnergy(){return 0;}
 @Override public void drawEnergy(double amount){}
 @Override public int getSourceTier(){var core=getConnectedReactor();return core==null?-1:core.getSourceTier();}
 @Override public int getPacketCount(){return 1;}
    private void spawnHeatParticles(Level level, BlockPos pos) {
        if (level.isClientSide()) return;

        mio_icif_nuclear_reactor_generator reactor = getConnectedReactor();
        if (reactor == null) return;

        int currentHeat = (int) reactor.getCurrentHeat();
        int maxHeat = (int) reactor.getMaxHeat();
        double heatPercentage = (double) currentHeat / maxHeat;

        // 堆温超过 20%：产生烟灰粒子
        if (heatPercentage >= 0.20) {
            if (level.getGameTime() % 10 == 0) {
                spawnParticlesAtChamber(level, pos, net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, 2);
            }
        }

        // 堆温超过 40%：更多烟灰粒子
        if (heatPercentage >= 0.40) {
            if (level.getGameTime() % 8 == 0) {
                spawnParticlesAtChamber(level, pos, net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, 3);
            }
        }

        // 堆温超过 60%：产生火焰粒子
        if (heatPercentage >= 0.60) {
            if (level.getGameTime() % 5 == 0) {
                spawnParticlesAtChamber(level, pos, net.minecraft.core.particles.ParticleTypes.FLAME, 3);
            }
        }

        // 堆温超过 80%：大量火焰粒子
        if (heatPercentage >= 0.80) {
            if (level.getGameTime() % 3 == 0) {
                spawnParticlesAtChamber(level, pos, net.minecraft.core.particles.ParticleTypes.FLAME, 5);
                spawnParticlesAtChamber(level, pos, net.minecraft.core.particles.ParticleTypes.LAVA, 2);
            }
        }
    }

    /**
     * 在腔室位置产生粒子
     */
    private void spawnParticlesAtChamber(Level level, BlockPos pos, net.minecraft.core.particles.ParticleOptions particleType, int count) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8;
        double y = pos.getY() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8;
        double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8;

        serverLevel.sendParticles(
            particleType,
            x, y, z,
            count, 0.1, 0.1, 0.1, 0.01
        );
    }
    
}
