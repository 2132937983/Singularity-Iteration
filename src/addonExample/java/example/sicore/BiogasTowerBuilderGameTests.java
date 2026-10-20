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
public final class BiogasTowerBuilderGameTests {
    static final BlockPos ORIGIN=new BlockPos(4479,84,1311),MASTER=new BlockPos(4479,88,1333);
    static ServerLevel level;
    static final Set<BlockPos> occupied=new HashSet<>();
    static final List<Unit> units=new ArrayList<>();
    static final List<BlockPos> stores=new ArrayList<>();
    static int stripe=4448,tick=-1;
    static boolean reload;
    static long baseline;
    static final List<BlockPos> testLoads=new ArrayList<>();
    static final List<BlockState> savedLoadStates=new ArrayList<>();
    static final class Unit {
        final BlockPos c;
        final List<BlockPos> crops=new ArrayList<>(),generators=new ArrayList<>(),pipes=new ArrayList<>();
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
            for(var p:generators)s.append(" gas=").append(fluids(level,p).getFluidInTank(0).getAmount());
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
        if(!Boolean.getBoolean("si.biogas.build")&&!Boolean.getBoolean("si.biogas.reload"))return;
        try{
            level=event.getServer().overworld();reload=Boolean.getBoolean("si.biogas.reload");
            // The old tower remains byte-identical in the delivered save; unload it only in this test copy.
            command("function si_heat_complex:stop");
            for(long chunk:level.getForcedChunks().toLongArray()){var cp=new net.minecraft.world.level.ChunkPos(chunk);if(cp.x<278||cp.x>281||cp.z<80||cp.z>83)level.setChunkForced(cp.x,cp.z,false);}
            command("forceload add 4448 1280 4511 1343");command("tick rate 200");
            if(reload){identify();lighting();command("time set midnight");tick=0;command("function si_biogas_tower:start");System.out.println("BIOGAS_TOWER native reload started");}
            else System.out.println("BIOGAS_TOWER building beside existing campus");
        }catch(Exception e){fail(e);}
    }
    static void identify(){
        for(int layer=0;layer<8;layer++)for(int x:new int[]{-12,12})for(int z:new int[]{-12,12}){
            var u=new Unit(ORIGIN.offset(x,10+12*layer,z));units.add(u);
            for(int gx:new int[]{0,2})for(int gz:new int[]{-2,2})u.generators.add(u.p(gx,-2,gz));
            u.generators.add(u.p(-2,-1,0));u.generators.add(u.p(3,-1,0));
            for(int px=0;px<=2;px++)for(int pz=-2;pz<=2;pz++)u.pipes.add(u.p(px,-3,pz));
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)if(level.getBlockEntity(u.p(dx,0,dz)) instanceof IPlanter)u.crops.add(u.p(dx,0,dz));
        }
        for(int layer=0;layer<8;layer++){int y=8+12*layer;for(int v:new int[]{-2,1})for(var p:List.of(ORIGIN.offset(5,y,v),ORIGIN.offset(-6,y,v),ORIGIN.offset(v,y,-6),ORIGIN.offset(v,y,5)))stores.add(p);}
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
        for(int x:new int[]{0,2})for(int z:new int[]{-2,2}){
            var p=u.p(x,-2,z);face(p,"generator/block_semifluid_generator",x==0?Direction.WEST:Direction.EAST);u.generators.add(p);fill(p,"biogas",10000);
        }
        for(int x=0;x<=2;x++)for(int z=-4;z<=-2;z++)vanilla(u.p(x,-1,z),Blocks.WATER.defaultBlockState());
        // Dedicated service generator: farm processing must not compete with hungry MFSU sinks.
        var service=u.p(-2,-1,0);face(service,"generator/block_semifluid_generator",Direction.EAST);u.generators.add(service);fill(service,"biogas",10000);
        var canPower=u.p(3,-1,0);face(canPower,"generator/block_semifluid_generator",Direction.WEST);u.generators.add(canPower);fill(canPower,"biogas",10000);
        pipe(u.p(3,-3,0));pipe(u.p(3,-2,0));
        for(int x=-2;x<0;x++)pipe(u.p(x,-3,0));pipe(u.p(-2,-2,0));
        for(int z=-3;z<=4;z++)for(int x:new int[]{-1,3})if(z!=0)wire(u.p(x,-2,z));
        for(int z=-3;z<=4;z++)wire(u.p(4,-2,z));
        for(int x=-1;x<=4;x++)wire(u.p(x,-2,-3));
        for(int x=-1;x<=3;x++)wire(u.p(x,-2,4));
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
        var sample=(IPlanter)level.getBlockEntity(u.crops.getFirst());
        System.out.println("BIOGAS_TOWER farm="+u.c+" plants="+u.crops.size()+" h/n/a="+sample.getHumidity()+"/"+sample.getSoilNutrients()+"/"+sample.getAirQuality());
    }
    static void build(){
        for(int layer=0;layer<8;layer++)for(int x:new int[]{-12,12})for(int z:new int[]{-12,12}){
            var u=new Unit(ORIGIN.offset(x,10+12*layer,z));buildUnit(u);units.add(u);
        }
        for(int layer=0;layer<8;layer++){
            int y=8+12*layer;
            for(int v:new int[]{-2,1}){
                BlockPos[] ps={ORIGIN.offset(5,y,v),ORIGIN.offset(-6,y,v),ORIGIN.offset(v,y,-6),ORIGIN.offset(v,y,5)};
                Direction[] ds={Direction.WEST,Direction.EAST,Direction.SOUTH,Direction.NORTH};
                for(int i=0;i<4;i++){face(ps[i],"wiring/block_mfsu",ds[i]);stores.add(ps[i]);}
            }
            ring(y,-7,6);ring(y,-5,4);
            for(var u:units){if(u.c.getY()!=y+86)continue;int cx=u.c.getX()-4479,cz=u.c.getZ()-1311;int edge=cx<0?-7:6,local=cx<0?cx+4:cx-1;int junction=cz+4;
                for(int x=Math.min(edge,local);x<=Math.max(edge,local);x++)wire(ORIGIN.offset(x,y,junction));
                for(int z=Math.min(junction,cz<0?-7:6);z<=Math.max(junction,cz<0?-7:6);z++)wire(ORIGIN.offset(edge,y,z));
            }
        }
        // One native generation/service grid per floor reduces route planning; all stores share the output bus.
        for(int y=2;y<=92;y++)for(int x:new int[]{-5,4})for(int z:new int[]{-5,4})wire(ORIGIN.offset(x,y,z));
        ring(2,-5,4);
        for(int distance=4;distance<=27;distance++)for(var p:List.of(ORIGIN.offset(distance,2,0),ORIGIN.offset(-distance-1,2,0),ORIGIN.offset(0,2,distance),ORIGIN.offset(0,2,-distance-1)))if(Math.abs(p.getX()-4479)<=27&&Math.abs(p.getZ()-1311)<=27)wire(p);
        architecture();
        for(int x=-3;x<=3;x++)for(int z=21;z<=23;z++)decor(x,3,z,Blocks.SMOOTH_STONE);
        for(int x=-2;x<=2;x++)decor(x,2,24,Blocks.SMOOTH_STONE);
        level.setBlock(MASTER.below(),Blocks.POLISHED_DEEPSLATE.defaultBlockState(),3);
        level.setBlock(MASTER,Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR),3);
        sign(level,4477,88,1333,"沼电 / 立体农业塔","八层 · 32 自动农场","常驻运行 / 区块休眠","加载开关，不模拟关机");
        sign(level,4481,88,1333,"128 半流质发电机","64 MFSU 共用储能","作物 → 生物质 → 沼气","循环供热 / 四向输出");
        command("kill @e[type=minecraft:item,x=4448,y=84,z=1280,dx=63,dy=146,dz=63]");
        command("function si_biogas_tower:start");
        System.out.println("BIOGAS_TOWER construction complete: 32 farm/process loops, 128 export + 64 service generators, 64 MFSUs");
    }
    static void lighting(){
        sign(level,4481,88,1333,"128 主机 + 64 自用机","64 MFSU 共用储能","作物 → 生物质 → 沼气","循环供热 / 四向输出");
        for(var u:units){for(int x:new int[]{-4,0,4})for(int z:new int[]{-4,0,4}){var p=u.p(x,4,z);if(level.getBlockState(p).is(Blocks.SEA_LANTERN))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);}
            for(int x:new int[]{-3,0,3})for(int z:new int[]{-3,0,3})level.setBlock(u.p(x,3,z),Blocks.SEA_LANTERN.defaultBlockState(),3);}
    }
    static void ring(int y,int min,int max){for(int d=min;d<=max;d++)for(var p:List.of(ORIGIN.offset(d,y,min),ORIGIN.offset(d,y,max),ORIGIN.offset(min,y,d),ORIGIN.offset(max,y,d)))wire(p);}
    static void decor(int x,int y,int z,net.minecraft.world.level.block.Block b){var p=ORIGIN.offset(x,y,z);if(!occupied.contains(p))level.setBlock(p,b.defaultBlockState(),2);}
    static void architecture(){
        for(int x=-31;x<=32;x++)for(int z=-31;z<=32;z++){decor(x,0,z,Blocks.POLISHED_DEEPSLATE);if(Math.abs(x)<=23&&Math.abs(z)<=23)decor(x,1,z,Blocks.SMOOTH_STONE);}
        for(int y=2;y<=102;y++)for(int x:new int[]{-23,23})for(int z:new int[]{-23,23}){decor(x,y,z,Blocks.IRON_BLOCK);decor(x+(x>0?-1:1),y,z,y%5==0?Blocks.SEA_LANTERN:Blocks.LIME_CONCRETE);}
        for(int layer=0;layer<8;layer++){
            int cropY=10+12*layer,deck=cropY-4;
            for(int x=-23;x<=23;x++)for(int z=-23;z<=23;z++)if(Math.abs(x)>=21||Math.abs(z)>=21||Math.abs(x)<=2||Math.abs(z)<=2){
                decor(x,deck,z,(Math.abs(x)==23||Math.abs(z)==23)?((x+z)%2==0?Blocks.YELLOW_CONCRETE:Blocks.BLACK_CONCRETE):Blocks.SMOOTH_STONE);
                if(Math.abs(x)==23||Math.abs(z)==23)decor(x,deck+1,z,Blocks.IRON_BARS);
            }
            for(var u:units){if(u.c.getY()!=84+cropY)continue;int cx=u.c.getX()-4479,cz=u.c.getZ()-1311;
                for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
                    decor(cx+x,deck,cz+z,Blocks.IRON_BLOCK);
                    if(Math.abs(x)==5||Math.abs(z)==5){decor(cx+x,cropY-1,cz+z,Blocks.SMOOTH_STONE);decor(cx+x,cropY+1,cz+z,Blocks.LIME_STAINED_GLASS);}
                    if(Math.abs(x)<=4&&Math.abs(z)<=4){decor(cx+x,cropY+5,cz+z,Blocks.GLASS);if(x%3==0&&z%3==0)decor(cx+x,cropY+3,cz+z,Blocks.SEA_LANTERN);}
                }
                for(int x:new int[]{-5,5})for(int z:new int[]{-5,5})for(int y=cropY;y<=cropY+5;y++)decor(cx+x,y,cz+z,Blocks.LIME_CONCRETE);
                // Service floors join each compact module to the perimeter and central walkways.
                for(int d=5;d<=11;d++)for(int w=-1;w<=1;w++){decor(cx+(cx>0?d:-d),deck,cz+w,Blocks.SMOOTH_STONE);decor(cx+w,deck,cz+(cz>0?d:-d),Blocks.SMOOTH_STONE);}
                for(int y=deck+1;y<=cropY+1;y++){
                    decor(cx+5,y,cz+4,Blocks.IRON_BLOCK);
                    var lp=ORIGIN.offset(cx+4,y,cz+4);if(!occupied.contains(lp))level.setBlock(lp,Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST),2);
                }
                sign(level,u.c.getX()+5,84+deck+1,u.c.getZ(),"FARM "+(units.indexOf(u)+1),"16/10/0 芦苇种植架","底层加工及发酵回路","沼气自供热 / 肥料桶");
            }
        }
        for(int y=2;y<=102;y++){
            decor(20,y,21,Blocks.IRON_BLOCK);level.setBlock(ORIGIN.offset(20,y,22),Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH),2);
        }
        for(int y:new int[]{6,18,30,42,54,66,78,90,102})for(int d=-23;d<=23;d++)for(int o:new int[]{-23,23}){decor(d,y,o,Blocks.IRON_BLOCK);decor(o,y,d,Blocks.IRON_BLOCK);}
        for(int d=23;d<=27;d++)for(int w=-2;w<=2;w++)for(int s:new int[]{-1,1}){decor(s*d,1,w,Math.abs(w)==2?Blocks.YELLOW_CONCRETE:Blocks.IRON_BLOCK);decor(w,1,s*d,Math.abs(w)==2?Blocks.YELLOW_CONCRETE:Blocks.IRON_BLOCK);}
        for(int x=4432;x<=4448;x++)for(int z=1309;z<=1313;z++){
            for(int y=85;y<=88;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
            level.setBlock(new BlockPos(x,84,z),(z==1309||z==1313?Blocks.YELLOW_CONCRETE:Blocks.SMOOTH_STONE).defaultBlockState(),2);
        }
        for(int x=-2;x<=2;x++)for(int z=22;z<=27;z++)decor(x,1,z,Blocks.SMOOTH_STONE);
        for(int x=-2;x<=2;x++)for(int y=2;y<=4;y++)if(!occupied.contains(ORIGIN.offset(x,y,23)))level.setBlock(ORIGIN.offset(x,y,23),Blocks.AIR.defaultBlockState(),2);
    }
    static long energy(){long n=0;for(var p:stores)n+=((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal().getAmount();for(var u:units)for(var p:u.generators)n+=((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal().getAmount();return n;}
    static void verify(){
        for(var u:units){u.observe();if(u.crops.size()<55)throw new IllegalStateException("Too few plants "+u.c);}
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
            if(tick%20==0)verify();
            int end=reload?16000:40000,start=end-6000;
            if(tick==start)baseline=energy();
            if(tick%2000==0){System.out.println("BIOGAS_TOWER tick="+tick+" EU="+energy());System.out.println(units.getFirst().status());}
            if(tick==end){
                verify();double rate=(energy()-baseline)/6000.0;
                StringBuilder report=new StringBuilder("32-farm native biogas tower / actual net EU/t over last 6000 ticks = "+rate+"\n");
                for(var u:units){if(u.biomass()==0||u.fertilizer()<8)throw new IllegalStateException("Fermentation loop starved "+u.status());for(int gi=0;gi<4;gi++)if(fluids(level,u.generators.get(gi)).getFluidInTank(0).getAmount()<1000)throw new IllegalStateException("Export gas buffer depleted "+u.status());if(u.regrown<50||u.harvested<20)throw new IllegalStateException("Farm not renewing "+u.status());report.append(u.status()).append('\n');}
                if(rate<1800)throw new IllegalStateException("Insufficient sustainable net generation "+rate+"\n"+report);
                Files.writeString(Path.of(reload?"reload-commissioning.txt":"commissioning.txt"),report.toString());System.out.println(report);
                if(!reload)command("function si_biogas_tower:standby");
            }
            if(reload&&tick==end+20){
                int[][] offsets={{27,3,0},{0,3,27},{-27,3,0},{0,3,-27}};
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
                // Keep the plant self-sustaining; standby controls loading, never overwrites production data.
                command("kill @e[type=minecraft:item,x=4448,y=84,z=1280,dx=63,dy=146,dz=63]");
                event.getServer().saveEverything(true,true,true);Files.writeString(Path.of(reload?"RELOAD_VERIFIED":"CONSTRUCTION_COMPLETE"),"Native crop renewal, item/fluid processing, self-fed fermentation and net EU production verified.\n");
                level=null;event.getServer().halt(false);
            }
        }catch(Exception e){fail(e);}
    }
    static void fail(Exception e){e.printStackTrace();if(level!=null){var server=level.getServer();level=null;server.halt(false);}}
}
