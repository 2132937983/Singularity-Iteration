package example.sicore;

import static example.sicore.HeatComplexBuilderGameTests.*;
import example.sicore.HeatComplexBuilderGameTests.Module;
import com.google.gson.*;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import java.nio.file.*;
import java.util.*;

/** Temporary native-world construction and commissioning; never included in the mod jar. */
@EventBusSubscriber(modid=CoreExampleMod.ID)
public final class IntegratedHeatTowerBuilderGameTests {
    static final BlockPos ORIGIN=new BlockPos(4219,84,1179), MASTER=new BlockPos(4219,88,1202);
    static ServerLevel level;
    static JsonObject plan;
    static final Set<BlockPos> occupied=new HashSet<>();
    static final List<Module> modules=new ArrayList<>();
    static final List<BlockPos> stores=new ArrayList<>(), levers=new ArrayList<>();
    static int stripe=4144,ticks=-1;
    static long baseline, retainedEnergy;
    static final List<BlockPos> testLoads=new ArrayList<>();
    static final List<net.minecraft.world.level.block.state.BlockState> savedLoadStates=new ArrayList<>();
    static boolean reload;
    static BlockPos pos(JsonArray a) {return ORIGIN.offset(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());}
    static BlockPos local(BlockPos c,int x,int y,int z,int rotation) {
        for(int i=0;i<rotation;i++){int old=x;x=-z;z=old;}return c.offset(x,y,z);
    }
    static Direction rotate(Direction d,int n) {for(int i=0;i<n;i++)if(d.getAxis().isHorizontal())d=d.getClockWise();return d;}
    static void command(String s) {level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack().withPermission(4),s);}
    static void power(int index,boolean value) {
        var p=levers.get(index);var state=level.getBlockState(p);
        level.setBlock(p,state.setValue(BlockStateProperties.POWERED,value),3);
        level.updateNeighborsAt(p,Blocks.LEVER);level.updateNeighborsAt(p.below(),Blocks.SMOOTH_STONE);
    }
    static long energy() {
        long result=0;for(var p:stores)result+=((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal().getAmount();
        for(var m:modules)for(var p:m.generators)result+=((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal().getAmount();
        return result;
    }
    @SubscribeEvent public static void start(ServerStartedEvent event) {
        if(!Boolean.getBoolean("si.integrated.build")&&!Boolean.getBoolean("si.integrated.reload"))return;
        try {
            level=event.getServer().overworld();reload=Boolean.getBoolean("si.integrated.reload");
            plan=JsonParser.parseString(Files.readString(Path.of("layout.json"))).getAsJsonObject();
            for(var e:plan.getAsJsonArray("blocks"))occupied.add(pos(e.getAsJsonObject().getAsJsonArray("position")));
            if(reload) {
                identify();ticks=0;command("function si_heat_complex:start");
                System.out.println("INTEGRATED_TOWER reload started");return;
            }
            command("scoreboard players set #running si_heat 0");
            // Stop the old native reactors before dismantling; no original world is open here.
            for(int r=0;r<4;r++)for(int c=0;c<4;c++) {
                var p=new BlockPos(4157+40*c,96,1128+40*r);
                if(level.getBlockState(p).is(Blocks.LEVER))level.setBlock(p,level.getBlockState(p).setValue(BlockStateProperties.POWERED,false),3);
                for(int x=9;x<=16;x++) {
                    var be=level.getBlockEntity(new BlockPos(4160+40*c+x,96,1128+40*r-3));
                    if(be instanceof AbstractEnergyBlockEntity eb)retainedEnergy+=eb.getEnergyStorageInternal().getAmount();
                }
            }
            var oldMaster=new BlockPos(4224,85,1310);
            if(level.getBlockState(oldMaster).is(Blocks.LEVER))level.setBlock(oldMaster,level.getBlockState(oldMaster).setValue(BlockStateProperties.POWERED,false),3);
            command("forceload add 4144 1112 4319 1279");
            System.out.println("INTEGRATED_TOWER rebuilding original hall; retained storage EU="+retainedEnergy);
        }catch(Exception e){fail(e);}
    }
    static void identify() {
        for(var e:plan.getAsJsonArray("centres")) {
            var c=e.getAsJsonObject();var center=pos(c.getAsJsonArray("center"));int n=c.get("rotation").getAsInt()/90;
            var m=new Module(level,center);modules.add(m);levers.add(local(center,-3,0,0,n));
            int[][] bs={{0,3,0},{0,-3,0},{4,0,0}};Direction[] axes={Direction.UP,Direction.DOWN,rotate(Direction.EAST,n)};
            for(int group=0;group<3;group++) {
                var b=local(center,bs[group][0],bs[group][1],bs[group][2],n);m.boilers.add(b);
                var sides=group<2?new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}:new Direction[]{rotate(Direction.WEST,n)};
                for(var d:sides)m.exchangers.add(b.relative(d));
                for(int t=1;t<=2;t++){var turbine=b.relative(axes[group],t);m.turbines.add(turbine);m.generators.add(turbine.relative(rotate(Direction.NORTH,n)));}
                m.condensers.add(b.relative(axes[group],3));
            }
        }
        for(var e:plan.getAsJsonArray("blocks")){var b=e.getAsJsonObject();if(b.get("kind").getAsString().equals("mfsu"))stores.add(pos(b.getAsJsonArray("position")));}
    }
    static void construct() {
        architecture();
        Map<String,String> paths=Map.ofEntries(
            Map.entry("reactor","generator/block_nuclear_reactor_generator"),Map.entry("chamber","reactor/block_reactor_chamber"),
            Map.entry("vessel","reactor/block_reactor_vessel"),Map.entry("redstone_port","reactor/block_reactor_redstone_port"),
            Map.entry("access_hatch","reactor/block_reactor_access_hatch"),Map.entry("fluid_port","reactor/block_reactor_fluid_port"),
            Map.entry("boiler","producer/block_steam_generator"),Map.entry("exchanger","hugenerator/block_heat_source_fluid"),
            Map.entry("turbine","producer/block_steam_kinetic_generator"),Map.entry("kinetic_generator","generator/block_kinetic_generator"),
            Map.entry("condenser","producer/block_condenser"),Map.entry("mfsu","wiring/block_mfsu"),
            Map.entry("generation_cable","wiring/cable/block_glass_cable"),Map.entry("output_cable","wiring/cable/block_glass_cable"));
        // Chambers enforce survival during onPlace: install every core before any chamber.
        for(var e:plan.getAsJsonArray("blocks")) {
            var b=e.getAsJsonObject();if(b.get("kind").getAsString().equals("reactor"))set(level,pos(b.getAsJsonArray("position")),paths.get("reactor"));
        }
        for(var e:plan.getAsJsonArray("blocks")) {
            var b=e.getAsJsonObject();var p=pos(b.getAsJsonArray("position"));String kind=b.get("kind").getAsString();
            if(kind.equals("lever"))continue;
            if(kind.equals("mfsu"))facing(level,p,paths.get(kind),Direction.byName(b.get("facing").getAsString()));
            else set(level,p,paths.get(kind));
        }
        identify();
        for(int i=0;i<modules.size();i++) {
            var m=modules.get(i);var c=m.center;int n=i%4;
            var lp=levers.get(i);level.setBlock(lp.below(),Blocks.SMOOTH_STONE.defaultBlockState(),3);
            level.setBlock(lp,Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR).setValue(BlockStateProperties.HORIZONTAL_FACING,rotate(Direction.EAST,n)),3);
            Direction[] axes={Direction.UP,Direction.DOWN,rotate(Direction.EAST,n)};
            for(int group=0;group<3;group++) {
                var b=m.boilers.get(group);var be=level.getBlockEntity(b);var tag=be.saveWithoutMetadata(level.registryAccess());
                tag.putInt("pressure",221);tag.putInt("inputMB",group<2?4:1);tag.putFloat("systemHeat",100F+221F/220F*100F*2.74F);be.loadWithComponents(tag,level.registryAccess());
                if(fluids(level,b).fill(new FluidStack(BuiltInRegistries.FLUID.get(id("distilledwater")),10000),IFluidHandler.FluidAction.EXECUTE)!=10000)throw new IllegalStateException("Water charge failed");
                var sides=group<2?new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}:new Direction[]{rotate(Direction.WEST,n)};
                for(var d:sides) {
                    var ex=b.relative(d);facing(level,ex,"hugenerator/block_heat_source_fluid",d.getOpposite());
                    for(int s=4;s<14;s++)insert(level,ex,s,item("resource/item_heatconductor",1));ejector(level,ex,14,1,null);
                    var port=group<2?ex.relative(group==0?Direction.DOWN:Direction.UP):ex.relative(rotate(Direction.WEST,n));ejector(level,port,0,1,null);
                }
                for(int t=1;t<=2;t++) {
                    var p=b.relative(axes[group],t);facing(level,p,"producer/block_steam_kinetic_generator",rotate(Direction.NORTH,n));
                    insert(level,p,2,item("normal/item_steam_turbin",1));ejector(level,p,3,4,axes[group].getOpposite());
                    facing(level,p.relative(rotate(Direction.NORTH,n)),"generator/block_kinetic_generator",rotate(Direction.SOUTH,n));
                }
                var co=m.condensers.get(group);ejector(level,co,7,4,axes[group].getOpposite());
                if(group<2){insert(level,co,0,item("reactor/item_reactor_vent",1));((AbstractEnergyBlockEntity)level.getBlockEntity(co)).getEnergyStorageInternal().setEnergy(1000);}
            }
            for(int y=0;y<6;y++)for(int x=0;x<9;x++) {
                String suffix=switch(ROWS[y].charAt(x)){case 'C'->"golden_vent";case 'Y'->"iridium_reflector";case 'S'->"uranium_quad";case 'J'->"vent_spread";default->null;};
                if(suffix!=null)insert(level,c,y*9+x,item("reactor/item_reactor_"+suffix,1));
            }
        }
        long each=retainedEnergy/128,remainder=retainedEnergy%128;
        for(var p:stores){((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal().setEnergy(each+(remainder-->0?1:0));}
        level.setBlock(MASTER.below(),Blocks.POLISHED_DEEPSLATE.defaultBlockState(),3);
        level.setBlock(MASTER,Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR),3);
        sign(level,4217,88,1202,"16 核集成能源塔","总开关 / 安全停堆","4 层 × 4 堆","检修前先停堆");
        sign(level,4221,88,1202,"128 台 MFSU","51.2 亿 EU 储能","四向共用 EV 输出","转子与燃料需更换");
        sign(level,4224,85,1310,"旧控制台已停用","集成塔位于北侧","总开关 4219 88 1202","请勿启动旧机组");
        for(int i=0;i<modules.size();i++) {
            var m=modules.get(i);var sp=local(m.center,1,0,4,i%4);
            sign(level,sp.getX(),sp.getY(),sp.getZ(),String.format("R%02d / L%d",i+1,i/4+1),"896 HU/t / 221","舱口更换核燃料","冷却液闭环 10 L");
        }
        var cp=ORIGIN.offset(3,1,24);level.setBlock(cp,Blocks.CHEST.defaultBlockState(),3);
        var chest=(ChestBlockEntity)level.getBlockEntity(cp);
        for(int i=0;i<27;i++)chest.setItem(i,item(i<16?"reactor/item_reactor_uranium_quad":"normal/item_steam_turbin",1));chest.setChanged();
        System.out.println("INTEGRATED_TOWER constructed 16 cores, 128 MFSUs, two common isolated buses");
    }
    static void decor(int x,int y,int z,net.minecraft.world.level.block.Block b) {
        var p=ORIGIN.offset(x,y,z);if(!occupied.contains(p))level.setBlock(p,b.defaultBlockState(),2);
    }
    static void architecture() {
        // Open mechanical tower, four service frames; foundations never overwrite conductors.
        for(int x=-27;x<=27;x++)for(int z=-27;z<=27;z++) {
            decor(x,0,z,Blocks.POLISHED_DEEPSLATE);
            if(Math.abs(x)<=22&&Math.abs(z)<=22)decor(x,1,z,(x%8==0&&z%8==0)?Blocks.SEA_LANTERN:Blocks.GRAY_CONCRETE);
        }
        for(int y=2;y<=74;y++)for(int x:new int[]{-22,22})for(int z:new int[]{-22,22}) {
            decor(x,y,z,Blocks.IRON_BLOCK);decor(x+(x>0?-1:1),y,z,y%4==0?Blocks.SEA_LANTERN:Blocks.CYAN_CONCRETE);
        }
        for(int cy:new int[]{12,28,44,60})for(int deck:new int[]{cy-7,cy-1,cy+5}) {
            for(int x=-22;x<=22;x++)for(int z=-22;z<=22;z++)if(Math.abs(x)>=20||Math.abs(z)>=20) {
                decor(x,deck,z,(Math.abs(x)==22||Math.abs(z)==22)?((x+z)%2==0?Blocks.YELLOW_CONCRETE:Blocks.BLACK_CONCRETE):Blocks.SMOOTH_STONE);
                if(Math.abs(x)==22||Math.abs(z)==22)decor(x,deck+1,z,Blocks.IRON_BARS);
            }
            for(var e:plan.getAsJsonArray("centres")) {
                var c=e.getAsJsonObject();var center=pos(c.getAsJsonArray("center"));if(center.getY()!=84+cy)continue;int n=c.get("rotation").getAsInt()/90;
                // Access lanes on the hatch side and along the generator side; masks retain every machine.
                for(int x=-4;x<=11;x++)for(int z:new int[]{3,4,5,-4,-5}) {
                    var p=local(center,x,deck-cy,z,n);decor(p.getX()-4219,p.getY()-84,p.getZ()-1179,Blocks.SMOOTH_STONE);
                }
                for(int yy=cy-7;yy<=cy+6;yy++) {
                    var wall=local(center,-5,yy-cy,5,n);if(!occupied.contains(wall))level.setBlock(wall,Blocks.IRON_BLOCK.defaultBlockState(),2);
                    var ladder=local(center,-4,yy-cy,5,n);if(!occupied.contains(ladder))level.setBlock(ladder,Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,rotate(Direction.EAST,n)),2);
                }
            }
        }
        // Continuous perimeter ladders serve each maintenance deck in survival mode.
        for(int y=2;y<=74;y++) {
            decor(19,y,20,Blocks.IRON_BLOCK);
            level.setBlock(ORIGIN.offset(19,y,21),Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH),2);
        }
        for(int y:new int[]{3,20,36,52,68,74})for(int d=-22;d<=22;d++)for(int o:new int[]{-22,22}) {
            decor(d,y,o,Blocks.IRON_BLOCK);decor(o,y,d,Blocks.IRON_BLOCK);
        }
        for(int y=4;y<=72;y++)for(int d=-21;d<=21;d++)if(Math.floorMod(y+d,16)==0) {
            decor(d,y,-22,Blocks.ORANGE_CONCRETE);decor(22,y,d,Blocks.ORANGE_CONCRETE);
        }
        // Four high-volume EV connection arms, striped service plates underneath.
        for(int d=22;d<=27;d++)for(int w=-2;w<=2;w++)for(int sign:new int[]{-1,1}) {
            decor(sign*d,1,w,Math.abs(w)==2?Blocks.YELLOW_CONCRETE:Blocks.IRON_BLOCK);
            decor(w,1,sign*d,Math.abs(w)==2?Blocks.YELLOW_CONCRETE:Blocks.IRON_BLOCK);
        }
        for(int z=22;z<=27;z++)for(int x=-2;x<=2;x++)decor(x,3,z,Blocks.SMOOTH_STONE);
        for(int x=-3;x<=3;x++)decor(x,3,23,Blocks.POLISHED_DEEPSLATE);
        for(int step=0;step<3;step++)for(int x=-2;x<=2;x++)decor(x,step,27-step,Blocks.SMOOTH_STONE);
        // Entrance cutouts through service rails; no enclosing decorative shell.
        for(int x=-2;x<=2;x++)for(int y=4;y<=6;y++)level.setBlock(ORIGIN.offset(x,y,22),Blocks.AIR.defaultBlockState(),2);
    }
    static void check(boolean running) {
        for(int i=0;i<16;i++) {
            var m=modules.get(i);
            if(m.coolant()!=10000||m.reactor().getCurrentHeat()>32)throw new IllegalStateException("Unsafe R"+(i+1)+" heat="+m.reactor().getCurrentHeat()+" coolant="+m.coolant());
            if(running&&!m.reactor().isRunning())throw new IllegalStateException("Stopped R"+(i+1)+" formed="+m.reactor().isValidFluidReactorStructure());
            for(var b:m.boilers)if(fluids(level,b).getFluidInTank(0).getAmount()<8000)throw new IllegalStateException("Water loop draining R"+(i+1));
        }
    }
    static void freshConsumables() {
        for(var m:modules) {
            var h=(IItemHandler)ArmoryServiceGameTests.call(level.getBlockEntity(m.center),"getItemHandler");h.extractItem(10,1,false);insert(level,m.center,10,item("reactor/item_reactor_uranium_quad",1));
            for(var p:m.turbines){var th=(IItemHandler)ArmoryServiceGameTests.call(level.getBlockEntity(p),"getItemHandler");th.extractItem(2,1,false);insert(level,p,2,item("normal/item_steam_turbin",1));}
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if(level==null||event.getServer()!=level.getServer())return;
        try {
            if(!reload&&stripe<=4312) {
                for(int x=stripe;x<=Math.min(stripe+3,4312);x++)for(int z=1112;z<=1272;z++)for(int y=85;y<=180;y++) {
                    var p=new BlockPos(x,y,z);if(!level.getBlockState(p).isAir())level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
                }
                if(stripe%32==16)System.out.println("INTEGRATED_TOWER clearance x="+stripe);stripe+=4;return;
            }
            if(ticks==-1){construct();ticks=0;return;}
            ticks++;
            if(!reload&&ticks==100){for(int i=0;i<16;i++){modules.get(i).charge();power(i,true);}}
            if(ticks<200)return;
            if(ticks<3000)check(true);
            if(ticks==1000)baseline=energy();
            if(ticks%500==0)System.out.println("INTEGRATED_TOWER tick="+ticks+" stored+buffer EU="+energy());
            if(ticks==3000) {
                check(true);double rate=(energy()-baseline)/2000.0;
                if(rate<9500)throw new IllegalStateException("Shared network throughput too low: "+rate);
                StringBuilder report=new StringBuilder("Integrated 16-core tower: native 2000-tick net generation = "+rate+" EU/t\n");
                for(int i=0;i<16;i++)report.append("R").append(i+1).append(" at ").append(modules.get(i).center).append(" hull=").append(modules.get(i).reactor().getCurrentHeat()).append(" coolant=").append(modules.get(i).coolant()).append('\n');
                Files.writeString(Path.of(reload?"reload-commissioning.txt":"commissioning.txt"),report.toString());System.out.println(report);
                if(reload) {
                    var p=modules.getFirst().turbines.getFirst();((IItemHandler)ArmoryServiceGameTests.call(level.getBlockEntity(p),"getItemHandler")).extractItem(2,1,false);
                } else for(int i=0;i<16;i++)power(i,false);
            }
            if(reload&&ticks==3040) {
                for(var m:modules)if(m.reactor().isRunning())throw new IllegalStateException("Rotor interlock failed");
                insert(level,modules.getFirst().turbines.getFirst(),2,item("normal/item_steam_turbin",1));command("function si_heat_complex:start");
            }
            if(reload&&ticks==3240){check(true);command("function si_heat_complex:stop");}
            if(reload&&ticks==3300) {
                // Four temporary real EV receivers prove that the common output bus is usable.
                int[][] endpoints={{27,3,0},{0,3,27},{-27,3,0},{0,3,-27}};
                for(int i=0;i<4;i++) {
                    var p=ORIGIN.offset(endpoints[i][0],endpoints[i][1],endpoints[i][2]);testLoads.add(p);savedLoadStates.add(level.getBlockState(p));
                    facing(level,p,"wiring/block_mfsu",rotate(Direction.EAST,i));
                }
            }
            if(reload&&ticks==4300) {
                StringBuilder outputs=new StringBuilder("Four native EV output receivers after 1000 ticks:\n");
                long returned=0;
                for(int i=0;i<4;i++) {
                    var p=testLoads.get(i);long eu=((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal().getAmount();
                    if(eu<250000)throw new IllegalStateException("External output arm "+i+" failed: "+eu);
                    outputs.append(p).append(" received ").append(eu).append(" EU\n");
                    returned+=eu;
                    level.setBlock(p,savedLoadStates.get(i),3);
                }
                long share=returned/128,rest=returned%128;
                for(var p:stores){var s=((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal();s.setEnergy(s.getAmount()+share+(rest-->0?1:0));}
                Files.writeString(Path.of("TEST_ENERGY_RETURNED"),Long.toString(returned));
                Files.writeString(Path.of("output-commissioning.txt"),outputs.toString());System.out.println(outputs);
            }
            int finish=reload?4400:3300;
            if(ticks==finish-20) {
                for(var m:modules)if(m.reactor().isRunning())throw new IllegalStateException("Stop failed");freshConsumables();
            }
            if(ticks==finish) {
                command("scoreboard players set #running si_heat 0");
                command("kill @e[type=minecraft:item,x=4144,y=85,z=1112,dx=168,dy=95,dz=160]");
                command("forceload remove 4144 1112 4319 1311");
                if(!level.getForcedChunks().isEmpty())System.out.println("INTEGRATED_TOWER unrelated forced chunks="+level.getForcedChunks().size());
                event.getServer().saveEverything(true,true,true);
                Files.writeString(Path.of(reload?"RELOAD_VERIFIED":"CONSTRUCTION_COMPLETE"),"Native shared-grid generation, all sixteen coolant and water loops, safe stopped delivery verified.\n");
                level=null;event.getServer().halt(false);
            }
        }catch(Exception e){fail(e);}
    }
    static void fail(Exception e){e.printStackTrace();if(level!=null){var server=level.getServer();level=null;server.halt(false);}}
}
