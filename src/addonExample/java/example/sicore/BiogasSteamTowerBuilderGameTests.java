package example.sicore;

import static example.sicore.HeatComplexBuilderGameTests.*;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.transport.PipeSize;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import java.nio.file.*;
import java.util.*;

/** Offline construction only. Production in the delivered world uses ordinary native machines. */
@EventBusSubscriber(modid=CoreExampleMod.ID)
public final class BiogasSteamTowerBuilderGameTests {
    static final BlockPos ORIGIN=new BlockPos(4479,84,1311),MASTER=new BlockPos(4479,88,1327);
    static ServerLevel level;
    static final Set<BlockPos> occupied=new HashSet<>();
    static final List<Unit> units=new ArrayList<>();
    static final List<BlockPos> stores=new ArrayList<>();
    static int stripe=4448,tick=-1;
    static boolean reload,probe;
    static long retainedEnergy;
    static int probeGas;
    static long baseline;
    static final List<BlockPos> testLoads=new ArrayList<>();
    static final List<BlockState> savedLoadStates=new ArrayList<>();
    static final class Unit {
        final BlockPos c;
        final List<BlockPos> crops=new ArrayList<>(),generators=new ArrayList<>(),pipes=new ArrayList<>(),heaters=new ArrayList<>(),waterPipes=new ArrayList<>();
        final Map<BlockPos,Integer> oldStages=new HashMap<>();
        long regrown,harvested;
        Unit(BlockPos c){this.c=c;}
        BlockPos p(int x,int y,int z){return c.offset(x,y,z);}
        int biomass(){return (Integer)ArmoryServiceGameTests.call(level.getBlockEntity(p(1,-2,0)),"getBiomassAmount");}
        int fertilizer(){var b=(BarrelBlockEntity)level.getBlockEntity(p(0,-2,0));int n=0;for(int i=0;i<27;i++)n+=b.getItem(i).getCount();return n;}
        void observe(){
            for(var p:crops) {
                if(!(level.getBlockEntity(p) instanceof IPlanter plant)||plant.getPlant()==null||!plant.getPlant().getTypeId().equals("reed"))throw new IllegalStateException("Lost crop "+p);
                int stage=plant.getGrowthStage();Integer old=oldStages.put(p,stage);
                if(old!=null){if(stage>old)regrown++;if(stage<old)harvested++;}
            }
        }
        String status(){
            StringBuilder s=new StringBuilder("centre="+c+" biomass="+biomass()+" fertilizer="+fertilizer()+" grown="+regrown+" harvests="+harvested);
            for(var p:heaters)s.append(" heatGas=").append(fluids(level,p).getFluidInTank(0).getAmount());
            s.append(" heaterGas=").append(fluids(level,p(2,-2,0)).getFluidInTank(0).getAmount());
            var mace=(IItemHandler)ArmoryServiceGameTests.call(level.getBlockEntity(p(0,-1,0)),"getItemHandler");
            var can=(IItemHandler)ArmoryServiceGameTests.call(level.getBlockEntity(p(1,-1,0)),"getItemHandler");
            s.append(" cane=").append(mace.getStackInSlot(0)).append(" chaff=").append(can.getStackInSlot(2));
            return s.toString();
        }
    }
    static void command(String text){level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack().withPermission(4),text);}
    static void put(BlockPos p,String path){occupied.add(p);set(level,p,path);}
    static void wire(BlockPos p){put(p,"wiring/cable/block_glass_cable");}
    static void face(BlockPos p,String path,Direction d){
        var state=block(path).defaultBlockState();
        if(state.hasProperty(BlockStateProperties.FACING))state=state.setValue(BlockStateProperties.FACING,d);
        else if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,d);
        occupied.add(p);level.setBlock(p,state,3);
    }
    static void vanilla(BlockPos p,BlockState state){occupied.add(p);level.setBlock(p,state,3);}
    static void ejectItem(BlockPos p,int slot,Direction d){
        var s=item("upgrade/ejector_upgrade",1);var tag=new CompoundTag();tag.putByte("dir",(byte)(d.ordinal()+1));s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));insert(level,p,slot,s);
    }
    @SuppressWarnings("unchecked") static void pipe(BlockPos p){
        var state=block("pipe/fluid_pipe_bronze").defaultBlockState();
        var property=state.getProperties().stream().filter(v->v.getName().equals("size")).findFirst().orElseThrow();
        vanilla(p,state.setValue((EnumProperty<PipeSize>)property,PipeSize.TINY));
        // IC2 pipe ports are closed until explicitly opened with a wrench.
        for(var side:Direction.values())ArmoryServiceGameTests.call(level.getBlockEntity(p),"unblockDirection",side);
    }
    static void charge(BlockPos p,long amount){((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal().setEnergy(amount);}
    static void fill(BlockPos p,String fluid,int amount){if(fluids(level,p).fill(new FluidStack(BuiltInRegistries.FLUID.get(id(fluid)),amount),IFluidHandler.FluidAction.EXECUTE)!=amount)throw new IllegalStateException("Rejected startup fluid "+p);}
    @SubscribeEvent public static void start(ServerStartedEvent event){
        if(!Boolean.getBoolean("si.biogas2.build")&&!Boolean.getBoolean("si.biogas2.reload")&&!Boolean.getBoolean("si.biogas2.probe"))return;
        try{
            level=event.getServer().overworld();reload=Boolean.getBoolean("si.biogas2.reload");probe=Boolean.getBoolean("si.biogas2.probe");
            // The old tower remains byte-identical in the delivered save; unload it only in this test copy.
            command("function si_heat_complex:stop");
            for(long chunk:level.getForcedChunks().toLongArray()){var cp=new net.minecraft.world.level.ChunkPos(chunk);if(cp.x<278||cp.x>281||cp.z<80||cp.z>83)level.setChunkForced(cp.x,cp.z,false);}
            command("forceload add 4448 1280 4511 1343");command("tick rate 200");
            if(reload){identify();lighting();command("time set midnight");tick=0;command("function si_biogas_tower:start");System.out.println("BIOGAS_STEAM_TOWER native reload started");}
            else {
                for(int x=4448;x<=4511;x++)for(int z=1280;z<=1343;z++)for(int y=85;y<=230;y++){var be=level.getBlockEntity(new BlockPos(x,y,z));if(be instanceof AbstractEnergyBlockEntity e)retainedEnergy+=e.getEnergyStorageInternal().getAmount();}
                System.out.println("BIOGAS_STEAM_TOWER replacing v1; retained EU="+retainedEnergy);
            }
        }catch(Exception e){fail(e);}
    }
    static void identify(){
        for(int layer=0;layer<8;layer++)for(int x:new int[]{-8,8})for(int z:new int[]{-8,8}){
            var u=new Unit(ORIGIN.offset(x,10+10*layer,z));units.add(u);
            for(int t=1;t<=2;t++)u.generators.add(u.p(t,-5,-1));
            u.generators.add(u.p(-2,-1,0));u.generators.add(u.p(3,-1,0));
            for(var p:List.of(u.p(0,-4,0),u.p(0,-5,-1),u.p(0,-5,1),u.p(0,-6,0)))u.heaters.add(p);
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)if(level.getBlockEntity(u.p(dx,0,dz)) instanceof IPlanter)u.crops.add(u.p(dx,0,dz));
            for(int wx=0;wx<=3;wx++)u.waterPipes.add(u.p(wx,-4,0));
        }
        for(int layer=0;layer<8;layer++){int y=5+10*layer;for(int v:new int[]{-1,0})for(var p:List.of(ORIGIN.offset(2,y,v),ORIGIN.offset(-3,y,v),ORIGIN.offset(v,y,-3),ORIGIN.offset(v,y,2)))stores.add(p);}
    }
    static void isolatedPipe(BlockPos p,Direction... sides){
        pipe(p);for(var d:Direction.values())ArmoryServiceGameTests.call(level.getBlockEntity(p),"blockDirection",d);
        for(var d:sides)ArmoryServiceGameTests.call(level.getBlockEntity(p),"unblockDirection",d);
    }
    static void steamCore(Unit u){
        var b=u.p(0,-5,0);face(b,"producer/block_steam_generator",Direction.EAST);
        var be=level.getBlockEntity(b);var tag=be.saveWithoutMetadata(level.registryAccess());
        tag.putInt("pressure",221);tag.putInt("inputMB",1);tag.putFloat("systemHeat",100F+221F/220F*100F*2.74F);be.loadWithComponents(tag,level.registryAccess());fill(b,"distilledwater",10000);
        BlockPos[] hp={u.p(0,-4,0),u.p(0,-5,-1),u.p(0,-5,1),u.p(0,-6,0)};
        Direction[] hd={Direction.DOWN,Direction.SOUTH,Direction.NORTH,Direction.UP};
        for(int i=0;i<4;i++){face(hp[i],"hugenerator/block_fluid_heat_generator",hd[i]);fill(hp[i],"biogas",10000);u.heaters.add(hp[i]);}
        for(int t=1;t<=2;t++){
            var p=u.p(t,-5,0);face(p,"producer/block_steam_kinetic_generator",Direction.NORTH);
            insert(level,p,2,item("normal/item_steam_turbin",1));ejector(level,p,3,1,Direction.UP);
            var g=u.p(t,-5,-1);face(g,"generator/block_kinetic_generator",Direction.SOUTH);u.generators.add(g);
        }
        var co=u.p(3,-5,0);face(co,"producer/block_condenser",Direction.SOUTH);ejector(level,co,7,1,Direction.UP);
        // The horizontal distributor is a passive one-way return gate: its boiler-facing input is absent.
        var gate=u.p(-1,-5,0);face(gate,"producer/block_fluid_distributor_elc",Direction.EAST);
        var gateBe=level.getBlockEntity(gate);var gateTag=gateBe.saveWithoutMetadata(level.registryAccess());gateTag.putBoolean("active",true);gateBe.loadWithComponents(gateTag,level.registryAccess());
        fill(gate,"distilledwater",1000);
        List<BlockPos> returns=new ArrayList<>();
        for(int x=1;x<=3;x++)returns.add(u.p(x,-4,0));
        returns.add(u.p(3,-4,1));for(int x=-1;x<=3;x++)returns.add(u.p(x,-4,2));returns.add(u.p(-1,-4,1));returns.add(u.p(-1,-4,0));
        for(var p:returns){isolatedPipe(p);u.waterPipes.add(p);}
        for(var p:returns)for(var d:Direction.values())if(d.getAxis()!=Direction.Axis.Y&&returns.contains(p.relative(d)))ArmoryServiceGameTests.call(level.getBlockEntity(p),"unblockDirection",d);
        for(int x=1;x<=3;x++)ArmoryServiceGameTests.call(level.getBlockEntity(u.p(x,-4,0)),"unblockDirection",Direction.DOWN);
        ArmoryServiceGameTests.call(level.getBlockEntity(u.p(-1,-4,0)),"unblockDirection",Direction.DOWN);
        for(var p:List.of(u.p(0,-4,-1),u.p(0,-4,1),u.p(-2,-4,0),u.p(-2,-5,0),u.p(-2,-6,0),u.p(-1,-6,0)))pipe(p);
        // Seal every gas face adjacent to the water circuit and the one-way gate.
        ArmoryServiceGameTests.call(level.getBlockEntity(u.p(-1,-3,0)),"blockDirection",Direction.DOWN);
        ArmoryServiceGameTests.call(level.getBlockEntity(u.p(-2,-4,0)),"blockDirection",Direction.EAST);
        ArmoryServiceGameTests.call(level.getBlockEntity(u.p(-2,-5,0)),"blockDirection",Direction.EAST);
        ArmoryServiceGameTests.call(level.getBlockEntity(u.p(-1,-6,0)),"blockDirection",Direction.UP);
        for(int x=-4;x<=2;x++)wire(u.p(x,-5,-2));
    }
    static void buildUnit(Unit u){
        // Native hydrated 9x9 crop bed, with retained pump pond and four corner irrigation sources.
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)vanilla(u.p(x,-1,z),Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE,7));
        for(int x:new int[]{-3,3})for(int z:new int[]{-3,3}){vanilla(u.p(x,-2,z),Blocks.SMOOTH_STONE.defaultBlockState());vanilla(u.p(x,-1,z),Blocks.WATER.defaultBlockState());}
        for(int x=0;x<=2;x++)for(int z=-4;z<=-2;z++)vanilla(u.p(x,-2,z),Blocks.SMOOTH_STONE.defaultBlockState());
        for(int x=-1;x<=3;x++)vanilla(u.p(x,-1,-5),Blocks.SMOOTH_STONE.defaultBlockState());
        for(int z=-4;z<=-2;z++)for(int x:new int[]{-1,3})vanilla(u.p(x,-1,z),Blocks.SMOOTH_STONE.defaultBlockState());
        face(u.p(0,0,0),"producer/block_harvest_elc",Direction.SOUTH);
        face(u.p(0,-1,0),"producer/block_powder_elc",Direction.SOUTH);
        face(u.p(1,-1,0),"producer/block_canner_elc",Direction.SOUTH);
        var canner=level.getBlockEntity(u.p(1,-1,0));var tag=canner.saveWithoutMetadata(level.registryAccess());tag.putInt("Mode",3);canner.loadWithComponents(tag,level.registryAccess());
        face(u.p(1,-1,-1),"producer/block_pump_elc",Direction.NORTH);
        face(u.p(1,-2,0),"producer/block_fermenter_elc",Direction.EAST);
        face(u.p(2,-2,0),"hugenerator/block_fluid_heat_generator",Direction.WEST);
        vanilla(u.p(0,-2,0),Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.SOUTH));
        for(int x=0;x<=2;x++)for(int z=-2;z<=2;z++){var p=u.p(x,-3,z);pipe(p);u.pipes.add(p);}
        for(int x=0;x<=2;x++)for(int z=-4;z<=-2;z++)vanilla(u.p(x,-1,z),Blocks.WATER.defaultBlockState());
        // Dedicated service generator: farm processing must not compete with hungry MFSU sinks.
        var service=u.p(-2,-1,0);face(service,"generator/block_semifluid_generator",Direction.EAST);u.generators.add(service);fill(service,"biogas",10000);
        var canPower=u.p(3,-1,0);face(canPower,"generator/block_semifluid_generator",Direction.WEST);u.generators.add(canPower);fill(canPower,"biogas",10000);
        pipe(u.p(3,-3,0));pipe(u.p(3,-2,0));
        for(int x=-2;x<0;x++)pipe(u.p(x,-3,0));pipe(u.p(-2,-2,0));
        wire(u.p(-1,-1,0));wire(u.p(-1,0,0));wire(u.p(2,-1,0));wire(u.p(2,-1,-1));
        ejectItem(u.p(0,0,0),17,Direction.DOWN);ejectItem(u.p(0,-1,0),3,Direction.EAST);
        ejector(level,u.p(1,-1,0),4,1,Direction.DOWN);ejector(level,u.p(1,-1,-1),1,1,Direction.SOUTH);
        ejector(level,u.p(1,-2,0),4,1,Direction.DOWN);ejectItem(u.p(1,-2,0),5,Direction.WEST);
        charge(u.p(0,0,0),10000);charge(u.p(0,-1,0),600);charge(u.p(1,-1,0),800);charge(u.p(1,-1,-1),20);
        fill(u.p(2,-2,0),"biogas",10000);fill(u.p(1,-2,0),"biomass",1000);
        var can=fluids(level,u.p(1,-1,0));if(can.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,4000),IFluidHandler.FluidAction.EXECUTE)!=4000)throw new IllegalStateException("Initial water");
        var reed=MioIcifAPI.instance().getCropAPI().getPlant("mio_icif","reed");if(reed==null)throw new IllegalStateException("Reed registry missing");
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            var p=u.p(x,0,z);if(occupied.contains(p)||!level.getBlockState(p.below()).is(Blocks.FARMLAND))continue;
            put(p,"crop/stick_upgraded");var crop=(IPlanter)level.getBlockEntity(p);
            crop.setPlant(reed);crop.setGrowthStage(1);crop.setGrowthSpeed(16);crop.setYield(10);crop.setResilience(0);crop.setScanLevel(4);crop.updateState();u.crops.add(p);
        }
        steamCore(u);
        var sample=(IPlanter)level.getBlockEntity(u.crops.getFirst());
        System.out.println("BIOGAS_STEAM_TOWER farm="+u.c+" plants="+u.crops.size()+" h/n/a="+sample.getHumidity()+"/"+sample.getSoilNutrients()+"/"+sample.getAirQuality());
    }
    static void build(){
        if(probe){
            var u=new Unit(ORIGIN.offset(-8,10,-8));buildUnit(u);units.add(u);
            var store=u.p(2,-5,-3);face(store,"wiring/block_mfsu",Direction.NORTH);stores.add(store);
            command("kill @e[type=minecraft:item,x=4448,y=84,z=1280,dx=63,dy=146,dz=63]");
            command("function si_biogas_tower:start");return;
        }
        for(int layer=0;layer<8;layer++)for(int x:new int[]{-8,8})for(int z:new int[]{-8,8}){var u=new Unit(ORIGIN.offset(x,10+10*layer,z));buildUnit(u);units.add(u);}
        for(int layer=0;layer<8;layer++){
            int y=5+10*layer;
            for(int v:new int[]{-1,0}){
                BlockPos[] ps={ORIGIN.offset(2,y,v),ORIGIN.offset(-3,y,v),ORIGIN.offset(v,y,-3),ORIGIN.offset(v,y,2)};
                Direction[] ds={Direction.WEST,Direction.EAST,Direction.SOUTH,Direction.NORTH};
                for(int i=0;i<4;i++){face(ps[i],"wiring/block_mfsu",ds[i]);stores.add(ps[i]);}
            }
            ring(y,-4,3);ring(y,-2,1);
            for(var u:units){if(u.c.getY()!=84+y+5)continue;
                int cx=u.c.getX()-4479,cz=u.c.getZ()-1311,inner=cx<0?cx+3:cx-4,edge=cx<0?-4:3,junction=cz-2;
                for(int x=Math.min(inner,edge);x<=Math.max(inner,edge);x++)wire(ORIGIN.offset(x,y,junction));
                // West modules need an extra conductor to reach the kinetic pair.
                if(cx<0)wire(u.p(3,-5,-2));
                for(int z=Math.min(junction,cz<0?-4:3);z<=Math.max(junction,cz<0?-4:3);z++)wire(ORIGIN.offset(edge,y,z));
            }
        }
        long share=retainedEnergy/64,rest=retainedEnergy%64;
        if(share>35000000)throw new IllegalStateException("Retained energy leaves insufficient acceptance-test headroom");
        for(var p:stores)charge(p,share+(rest-->0?1:0));
        for(int y=2;y<=75;y++)for(int x:new int[]{-2,1})for(int z:new int[]{-2,1})wire(ORIGIN.offset(x,y,z));
        ring(2,-2,1);
        for(int d=1;d<=17;d++)for(var p:List.of(ORIGIN.offset(d,2,0),ORIGIN.offset(-d,2,0),ORIGIN.offset(0,2,d),ORIGIN.offset(0,2,-d)))wire(p);
        architecture();
        level.setBlock(MASTER.below(),Blocks.POLISHED_DEEPSLATE.defaultBlockState(),3);
        level.setBlock(MASTER,Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR),3);
        sign(level,4477,88,1327,"二代 / 32 核沼电塔","221 bar 双级涡轮","32 农场 / 64 MFSU","常驻加载 / 随区块休眠");
        sign(level,4481,88,1327,"沼气过热蒸汽发电","沼气 → 热 → 动能 → 电","主体 31 × 31 × 85","定期更换蒸汽转子");
        for(int x:new int[]{-6,-4,4,6}){
            var p=ORIGIN.offset(x,1,18);vanilla(p,Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.SOUTH));
            var barrel=(BarrelBlockEntity)level.getBlockEntity(p);for(int slot=0;slot<16;slot++)barrel.setItem(slot,item("normal/item_steam_turbin",1));barrel.setChanged();
        }
        command("kill @e[type=minecraft:item,x=4448,y=84,z=1280,dx=63,dy=146,dz=63]");command("function si_biogas_tower:start");
        System.out.println("BIOGAS_STEAM_TOWER construction complete: 32 steam cores / 32 farms / 64 MFSUs");
    }
    static void lighting(){for(var u:units)for(int x:new int[]{-3,0,3})for(int z:new int[]{-3,0,3})level.setBlock(u.p(x,3,z),Blocks.SEA_LANTERN.defaultBlockState(),3);}
    static void ring(int y,int min,int max){for(int d=min;d<=max;d++)for(var p:List.of(ORIGIN.offset(d,y,min),ORIGIN.offset(d,y,max),ORIGIN.offset(min,y,d),ORIGIN.offset(max,y,d)))wire(p);}
    static void decor(int x,int y,int z,net.minecraft.world.level.block.Block b){var p=ORIGIN.offset(x,y,z);if(!occupied.contains(p))level.setBlock(p,b.defaultBlockState(),2);}
    static void architecture(){
        for(int x=-31;x<=32;x++)for(int z=-31;z<=32;z++)decor(x,0,z,Math.abs(x)<=17&&Math.abs(z)<=17?Blocks.POLISHED_DEEPSLATE:Blocks.SMOOTH_STONE);
        for(int y=1;y<=84;y++)for(int x:new int[]{-15,15})for(int z:new int[]{-15,15}){decor(x,y,z,Blocks.IRON_BLOCK);decor(x+(x>0?-1:1),y,z,y%5==0?Blocks.SEA_LANTERN:Blocks.LIME_CONCRETE);}
        for(int layer=0;layer<8;layer++){
            int cy=10+10*layer;
            for(int deck:new int[]{cy-7,cy-1})for(int x=-15;x<=15;x++)for(int z=-15;z<=15;z++)if(Math.abs(x)>=13||Math.abs(z)>=13||Math.abs(x)<=2||Math.abs(z)<=2){
                decor(x,deck,z,Math.abs(x)==15||Math.abs(z)==15?((x+z)%2==0?Blocks.YELLOW_CONCRETE:Blocks.BLACK_CONCRETE):Blocks.SMOOTH_STONE);
                if(Math.abs(x)==15||Math.abs(z)==15)decor(x,deck+1,z,Blocks.IRON_BARS);
            }
            for(var u:units){if(u.c.getY()!=84+cy)continue;int cx=u.c.getX()-4479,cz=u.c.getZ()-1311;
                for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
                    decor(cx+x,cy-7,cz+z,Blocks.IRON_BLOCK);
                    if(Math.abs(x)==5||Math.abs(z)==5){decor(cx+x,cy-1,cz+z,Blocks.SMOOTH_STONE);decor(cx+x,cy+1,cz+z,Blocks.LIME_STAINED_GLASS);}
                    if(Math.abs(x)<=4&&Math.abs(z)<=4)decor(cx+x,cy+4,cz+z,Blocks.GLASS);
                }
                for(int x:new int[]{-3,0,3})for(int z:new int[]{-3,0,3})decor(cx+x,cy+3,cz+z,Blocks.SEA_LANTERN);
                for(int y=cy-6;y<=cy;y++){
                    decor(cx+5,y,cz+4,Blocks.IRON_BLOCK);
                    var p=ORIGIN.offset(cx+4,y,cz+4);if(!occupied.contains(p))level.setBlock(p,Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST),2);
                }
                sign(level,u.c.getX()+5,84+cy-6,u.c.getZ(),"核心 "+(units.indexOf(u)+1),"128 HU/t / 221 bar","过热汽 → 普通汽 → 冷凝","顶层农场 / 底层发酵");
            }
            for(int d=-15;d<=15;d++)for(int e:new int[]{-15,15}){decor(d,cy-1,e,Blocks.IRON_BLOCK);decor(e,cy-1,d,Blocks.IRON_BLOCK);}
        }
        for(int y=1;y<=84;y++){decor(14,y,14,Blocks.IRON_BLOCK);level.setBlock(ORIGIN.offset(14,y,15),Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH),2);}
        for(int d=15;d<=17;d++)for(int w=-2;w<=2;w++)for(int sg:new int[]{-1,1}){decor(sg*d,1,w,Math.abs(w)==2?Blocks.YELLOW_CONCRETE:Blocks.IRON_BLOCK);decor(w,1,sg*d,Math.abs(w)==2?Blocks.YELLOW_CONCRETE:Blocks.IRON_BLOCK);}
        for(int x=4432;x<=4464;x++)for(int z=1309;z<=1313;z++)level.setBlock(new BlockPos(x,84,z),(z==1309||z==1313?Blocks.YELLOW_CONCRETE:Blocks.SMOOTH_STONE).defaultBlockState(),2);
        for(int x=-2;x<=2;x++)for(int z=15;z<=20;z++)decor(x,1,z,Blocks.SMOOTH_STONE);
    }
    static long energy(){long n=0;for(var p:stores)n+=((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal().getAmount();for(var u:units)for(var p:u.generators)n+=((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal().getAmount();return n;}
    static void verify(){
        for(var u:units){u.observe();if(tick%2000==0){var water=fluids(level,u.p(0,-5,0)).getFluidInTank(0);if(water.getAmount()<9000)throw new IllegalStateException("Boiler water leak "+u.c+" "+water);}
            if(u.crops.size()<55)throw new IllegalStateException("Too few plants "+u.c);}
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(level==null||level.getServer()!=event.getServer())return;
        try{
            if(!reload&&stripe<=4511){
                for(int x=stripe;x<=Math.min(stripe+3,4511);x++)for(int z=1280;z<=1343;z++){
                    for(int y=85;y<=230;y++){var p=new BlockPos(x,y,z);if(!level.getBlockState(p).isAir())level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}
                    for(int y=81;y<=84;y++)level.setBlock(new BlockPos(x,y,z),(y==84?Blocks.SMOOTH_STONE:Blocks.DEEPSLATE_BRICKS).defaultBlockState(),2);
                }
                stripe+=4;return;
            }
            if(tick==-1){build();tick=0;return;}
            tick++;
            if(reload&&tick==100){
                int[][] expected={{279,81},{279,82},{280,81},{280,82},{281,81},{278,81},{279,83},{279,80}};
                if(level.getForcedChunks().size()!=8)throw new IllegalStateException("Unexpected forced chunk count: "+level.getForcedChunks().size());
                for(var cp:expected)if(!level.getForcedChunks().contains(new net.minecraft.world.level.ChunkPos(cp[0],cp[1]).toLong()))throw new IllegalStateException("Missing loaded core/output chunk");
            }
            if(tick%20==0)verify();
            if(probe){
                if(tick==4000){baseline=energy();var u=units.getFirst();probeGas=0;for(var h:u.heaters)probeGas+=fluids(level,h).getFluidInTank(0).getAmount();
                    for(var p:List.of(u.p(0,-3,0),u.p(0,-4,-1),u.p(0,-4,1)))ArmoryServiceGameTests.call(level.getBlockEntity(p),"blockDirection",Direction.DOWN);
                    ArmoryServiceGameTests.call(level.getBlockEntity(u.p(0,-4,-1)),"blockDirection",Direction.SOUTH);
                    ArmoryServiceGameTests.call(level.getBlockEntity(u.p(0,-4,1)),"blockDirection",Direction.NORTH);
                    ArmoryServiceGameTests.call(level.getBlockEntity(u.p(-1,-6,0)),"blockDirection",Direction.EAST);
                }
                if(tick%1000==0)System.out.println("BIOGAS_STEAM_TOWER PROBE tick="+tick+" EU="+energy()+" "+units.getFirst().status());
                if(tick==6000){var u=units.getFirst();int gas=0;for(var h:u.heaters)gas+=fluids(level,h).getFluidInTank(0).getAmount();double rate=(energy()-baseline)/2000.0,eff=(energy()-baseline)/(double)(probeGas-gas);
                    String result="Native isolated-fuel probe: net="+rate+" EU/t; heater biogas consumption="+(probeGas-gas)+" mB; EU/mB="+eff+"\n"+u.status()+"\n";
                    if(rate<90||eff<44||probeGas-gas<3500)throw new IllegalStateException(result);
                    Files.writeString(Path.of("probe-commissioning.txt"),result);Files.writeString(Path.of("PROBE_VERIFIED"),result);System.out.println(result);level=null;event.getServer().halt(false);
                }return;
            }
            int end=reload?16000:40000,start=end-6000;
            if(tick==start)baseline=energy();
            if(tick%2000==0){System.out.println("BIOGAS_STEAM_TOWER tick="+tick+" EU="+energy());System.out.println(units.getFirst().status());}
            if(tick==end){
                verify();double rate=(energy()-baseline)/6000.0;
                StringBuilder report=new StringBuilder("32-core native biogas steam tower / actual net EU/t over last 6000 ticks = "+rate+"\n");
                for(var u:units){if(u.biomass()==0||u.fertilizer()<8)throw new IllegalStateException("Fermentation loop starved "+u.status());for(var heater:u.heaters)if(fluids(level,heater).getFluidInTank(0).getAmount()<1000)throw new IllegalStateException("Export gas buffer depleted "+u.status());if(u.regrown<50||u.harvested<20)throw new IllegalStateException("Farm not renewing "+u.status());report.append(u.status()).append('\n');}
                if(rate<2900)throw new IllegalStateException("Insufficient sustainable net generation "+rate+"\n"+report);
                Files.writeString(Path.of(reload?"reload-commissioning.txt":"commissioning.txt"),report.toString());System.out.println(report);
                if(!reload)command("function si_biogas_tower:standby");
            }
            if(reload&&tick==end+20){
                int[][] offsets={{17,3,0},{0,3,17},{-17,3,0},{0,3,-17}};
                for(int i=0;i<4;i++){var p=ORIGIN.offset(offsets[i][0],offsets[i][1],offsets[i][2]);testLoads.add(p);savedLoadStates.add(level.getBlockState(p));face(p,"wiring/block_mfsu",Direction.UP);}
            }
            if(reload&&tick==end+1020){
                StringBuilder output=new StringBuilder("Four real EV receivers after 1000 ticks:\n");long returned=0;
                for(int i=0;i<4;i++){var p=testLoads.get(i);long eu=((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal().getAmount();if(eu<250000)throw new IllegalStateException("Output failed "+p+": "+eu);returned+=eu;output.append(p).append(" received ").append(eu).append(" EU\n");level.setBlock(p,savedLoadStates.get(i),3);}
                long share=returned/64,rest=returned%64;for(var p:stores){var st=((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal();st.setEnergy(st.getAmount()+share+(rest-->0?1:0));}
                Files.writeString(Path.of("output-commissioning.txt"),output.toString());System.out.println(output);command("function si_biogas_tower:standby");
                for(int x=278;x<=281;x++)for(int z=80;z<=83;z++)if(level.getForcedChunks().contains(new net.minecraft.world.level.ChunkPos(x,z).toLong()))throw new IllegalStateException("Loading switch failed");
            }
            if(tick==end+(reload?1060:40)){
                if(reload)for(var u:units)for(int t=1;t<=2;t++){
                    var handler=(IItemHandler)ArmoryServiceGameTests.call(level.getBlockEntity(u.p(t,-5,0)),"getItemHandler");
                    var rotor=handler.getStackInSlot(2);if(rotor.isEmpty())throw new IllegalStateException("Missing delivery rotor");rotor.setDamageValue(0);level.getBlockEntity(u.p(t,-5,0)).setChanged();
                }
                // Keep the plant self-sustaining; standby controls loading, never overwrites production data.
                command("kill @e[type=minecraft:item,x=4448,y=84,z=1280,dx=63,dy=146,dz=63]");
                event.getServer().saveEverything(true,true,true);Files.writeString(Path.of(reload?"RELOAD_VERIFIED":"CONSTRUCTION_COMPLETE"),"Native crop renewal, item/fluid processing, self-fed fermentation and net EU production verified.\n");
                level=null;event.getServer().halt(false);
            }
        }catch(Exception e){fail(e);}
    }
    static void fail(Exception e){e.printStackTrace();if(level!=null){var server=level.getServer();level=null;server.halt(false);}}
}
