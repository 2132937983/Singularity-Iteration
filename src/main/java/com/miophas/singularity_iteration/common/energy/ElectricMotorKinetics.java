// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import com.miophas.singularity_iteration.core.runtime.energy.DemandEnergySource;
import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Kinetic_Generator_elc;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/** R110/R111: one paid 1000 KU source buffer; 4 KU per EU; 100 KU/t per installed motor. */
public final class ElectricMotorKinetics {
    private final mio_icif_Kinetic_Generator_elc owner;
    private long ku,uncertain,frame=Long.MIN_VALUE,extracted,activeAt=Long.MIN_VALUE;
    private boolean transferring;
    private String failure="";
    private CompoundTag hold=new CompoundTag();
    private final Port[] sides=new Port[6];
    private final Port aggregate=new Port(null);
    public ElectricMotorKinetics(mio_icif_Kinetic_Generator_elc owner){this.owner=owner;for(var side:Direction.values())sides[side.ordinal()]=new Port(side);}
    private boolean live(){
        if(!(owner.getLevel() instanceof ServerLevel world)||!world.getServer().isSameThread()||owner.isRemoved())return false;
        var p=owner.getBlockPos();var chunk=world.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4);
        return chunk!=null&&chunk.getBlockEntity(p,LevelChunk.EntityCreationType.CHECK)==owner;
    }
    private Direction front(){return owner.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);}
    private void dirty(){owner.setChanged();if(live())ContainerToTank.markUnsaved(owner);}
    private void frame(){long now=owner.getLevel()==null?0:owner.getLevel().getGameTime();if(frame!=now){frame=now;extracted=0;}}
    private boolean enabled(){return live()&&!transferring&&uncertain==0&&hold.isEmpty()&&owner.getEnergyStorageInternal().scexNetworkControlled();}
    public static EnergyAmount cost(long ku){return new EnergyAmount(ku/4,(ku%4)*(EnergyAmount.UNITS/4));}
    public boolean tick(){
        if(!enabled())return false;frame();var energy=owner.getEnergyStorageInternal();var available=energy.scexExactAmount();
        long affordable=available.whole()>=250?1000:available.whole()*4+available.fraction()/(EnergyAmount.UNITS/4);
        long made=Math.min(Math.max(0,1000-ku),affordable);
        if(made>0){var amount=cost(made);if(!energy.scexConsumeEnergy(amount,false).equals(amount))throw new IllegalStateException("Motor payment changed");ku+=made;activeAt=frame;dirty();}
        push();return active();
    }
    public boolean active(){return owner.getLevel()!=null&&activeAt!=Long.MIN_VALUE&&owner.getLevel().getGameTime()-activeAt<=1;}
    public long stored(){return ku;}
    public long uncertain(){return uncertain;}
    public boolean held(){return !hold.isEmpty();}
    public IMioIcifCapabilities.IKineticStorage port(Direction side){return side==null?aggregate:side==front()?sides[side.ordinal()]:null;}
    private long available(long wanted){frame();return Math.min(Math.max(0,wanted),Math.min(ku,Math.max(0,owner.getMotorCount()*100L-extracted)));}
    private void push(){
        if(!enabled()||available(1000)==0)return;
        var world=(ServerLevel)owner.getLevel();var at=owner.getBlockPos().relative(front());
        var chunk=world.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4);if(chunk==null)return;
        // The independent converter pulls only what the electric grid can deliver; legacy KU consumers retain push interop.
        if(chunk.getBlockEntity(at,LevelChunk.EntityCreationType.CHECK) instanceof DemandEnergySource)return;
        var target=world.getCapability(IMioIcifCapabilities.KINETIC_STORAGE_BLOCK,at,front().getOpposite());
        if(target==null||target==aggregate)return;transferring=true;
        try{
            if(!target.canReceiveKinetic()||owner.getMotorCount()*1000<=target.getRPM())return;
            long offer=available(1000);long proposed=target.receiveKinetic(offer,true);if(proposed<=0||proposed>offer)return;
            ku-=proposed;uncertain=proposed;dirty();long accepted=target.receiveKinetic(proposed,false);
            if(accepted<0||accepted>proposed){failure="Invalid KU push receipt";dirty();return;}
            ku+=proposed-accepted;uncertain=0;extracted+=accepted;if(accepted>0)activeAt=frame;failure="";dirty();
        }catch(RuntimeException error){if(uncertain>0){failure=error.getClass().getName();dirty();}}finally{transferring=false;}
    }
    public void save(CompoundTag tag){
        tag.putLong("scex_kinetic_credit_ku",ku);var state=new CompoundTag();state.putInt("version",1);state.putLong("uncertain",uncertain);state.putString("failure",failure);state.put("hold",hold.copy());tag.put("scex_motor",state);
    }
    public void load(CompoundTag tag){
        hold=new CompoundTag();ku=tag.getLong("scex_kinetic_credit_ku");uncertain=0;failure="";
        if(ku<0||ku>1000){hold.put("invalid_old_credit",tag.get("scex_kinetic_credit_ku").copy());ku=0;}
        if(tag.contains("scex_motor",Tag.TAG_COMPOUND)){var state=tag.getCompound("scex_motor");
            if(state.getInt("version")==1){hold.merge(state.getCompound("hold").copy());uncertain=Math.max(0,state.getLong("uncertain"));failure=state.getString("failure");}
            else hold.put("unknown_motor_version",state.copy());}
        frame=Long.MIN_VALUE;extracted=0;activeAt=Long.MIN_VALUE;transferring=false;
    }
    private final class Port implements IMioIcifCapabilities.IKineticStorage {
        private final Direction side;Port(Direction side){this.side=side;}
        private boolean usable(){return enabled()&&(side==null||side==front());}
        @Override public long receiveKinetic(long amount,boolean simulate){return 0;}
        @Override public long extractKinetic(long amount,boolean simulate){
            if(!usable()||amount<=0)return 0;long value=available(amount);
            if(!simulate&&value>0){ku-=value;extracted+=value;activeAt=frame;dirty();}return value;
        }
        @Override public long getKineticStored(){return ku;}
        @Override public long getMaxKineticStored(){return 1000;}
        @Override public boolean canExtractKinetic(){return usable()&&available(1000)>0;}
        @Override public boolean canReceiveKinetic(){return false;}
        @Override public int getRPM(){return owner.getMotorCount()*1000;}
        @Override public boolean isOverspeed(){return getRPM()>=8000;}
        @Override public long getKineticLossPerTick(){return 0;}
        @Override public long getMaxReceive(){return 0;}
        @Override public long getMaxExtract(){return usable()?owner.getMotorCount()*100L:0;}
    }
}
