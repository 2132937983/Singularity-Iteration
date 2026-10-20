package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.*;
import java.nio.file.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Offline world construction and real closed-loop commissioning; excluded from the release jar. */
@GameTestHolder("si_heat_complex")
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid=CoreExampleMod.ID)
public final class HeatComplexBuilderGameTests {
    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    static Block block(String path) {
        Block b = BuiltInRegistries.BLOCK.get(id(path));
        if (b == Blocks.AIR) throw new IllegalStateException("Unknown block " + path);
        return b;
    }
    static ItemStack item(String path, int count) {
        ItemStack s = new ItemStack(BuiltInRegistries.ITEM.get(id(path)), count);
        if (s.isEmpty()) throw new IllegalStateException("Unknown item " + path);
        return s;
    }
    static void set(ServerLevel l, BlockPos p, String path) { l.setBlock(p, block(path).defaultBlockState(), 3); }
    static void facing(ServerLevel l, BlockPos p, String path, Direction d) {
        l.setBlock(p, block(path).defaultBlockState().setValue(BlockStateProperties.FACING, d), 3);
    }
    static void insert(ServerLevel l, BlockPos p, int slot, ItemStack s) {
        var handler = (IItemHandler) ArmoryServiceGameTests.call(l.getBlockEntity(p), "getItemHandler");
        if (!handler.insertItem(slot, s, false).isEmpty()) throw new IllegalStateException("Rejected " + s + " at " + p + " slot " + slot);
    }
    static void ejector(ServerLevel l, BlockPos p, int slot, int count, Direction d) {
        ItemStack s = item("upgrade/fluid_ejector_upgrade", count);
        if (d != null) { CompoundTag t = new CompoundTag(); t.putByte("dir", (byte)(d.ordinal()+1)); s.set(DataComponents.CUSTOM_DATA, CustomData.of(t)); }
        insert(l, p, slot, s);
    }
    static IFluidHandler fluids(ServerLevel l, BlockPos p) {
        var f = l.getCapability(Capabilities.FluidHandler.BLOCK, p, null);
        if (f == null) throw new IllegalStateException("Missing fluid handler " + p);
        return f;
    }
    static final String[] ROWS = {"CYAAJAJAA", "YSYJCJCJA", "AYJCJCJCJ", "AJCJCJCJA", "JCJCJCJCJ", "AJAJAJAJA"};
    static final class Module {
        final ServerLevel level; final BlockPos center;
        final List<BlockPos> boilers = new ArrayList<>(), exchangers = new ArrayList<>(), turbines = new ArrayList<>(), condensers = new ArrayList<>(), stores = new ArrayList<>(), generators = new ArrayList<>();
        Module(ServerLevel level, BlockPos center) { this.level=level; this.center=center; }
        IReactorController reactor() { return (IReactorController)level.getBlockEntity(center); }
        void charge() {
            if (!reactor().isValidFluidReactorStructure()) throw new IllegalStateException("Reactor did not form " + center);
            if (reactor().getFluidHandler().fill(new FluidStack(BuiltInRegistries.FLUID.get(id("coolant")), 10000), IFluidHandler.FluidAction.EXECUTE) != 10000)
                throw new IllegalStateException("Rejected initial coolant");
        }
        long energy() {
            long result=0;
            for (var p : stores) result+=((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal().getAmount();
            for (var p : generators) result+=((AbstractEnergyBlockEntity)level.getBlockEntity(p)).getEnergyStorageInternal().getAmount();
            return result;
        }
        int coolant() {
            int result=0;
            var own=reactor().getFluidHandler();
            for (int i=0;i<own.getTanks();i++) result+=own.getFluidInTank(i).getAmount();
            for (var p:exchangers) { var f=fluids(level,p); for(int i=0;i<f.getTanks();i++)result+=f.getFluidInTank(i).getAmount(); }
            return result;
        }
        void power(boolean value) {
            BlockPos p=center.west(3);
            level.setBlock(p, Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST).setValue(BlockStateProperties.POWERED,value),3);
            level.updateNeighborsAt(p,Blocks.LEVER); level.updateNeighborsAt(p.below(),Blocks.SMOOTH_STONE);
        }
        String status() {
            StringBuilder b=new StringBuilder("hull="+reactor().getCurrentHeat()+" coolant="+coolant()+" EU="+energy());
            for (var p:boilers) b.append(" boiler=").append(level.getBlockEntity(p).saveWithoutMetadata(level.registryAccess()));
            for (var p:turbines) b.append(" turbine=").append(level.getBlockEntity(p).saveWithoutMetadata(level.registryAccess()));
            return b.toString();
        }
    }
    static Module buildModule(ServerLevel l, BlockPos c) {
        Module m=new Module(l,c);
        set(l,c,"generator/block_nuclear_reactor_generator");
        for(var d:Direction.values()) set(l,c.relative(d),"reactor/block_reactor_chamber");
        for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++)
            if(Math.max(Math.max(Math.abs(x),Math.abs(y)),Math.abs(z))==2)set(l,c.offset(x,y,z),"reactor/block_reactor_vessel");
        set(l,c.west(2),"reactor/block_reactor_redstone_port");
        set(l,c.south(2),"reactor/block_reactor_access_hatch");
        l.setBlock(c.west(3).below(),Blocks.SMOOTH_STONE.defaultBlockState(),3); m.power(false);
        BlockPos[] bp={c.above(3),c.below(3),c.east(4)};
        Direction[] axes={Direction.UP,Direction.DOWN,Direction.EAST};
        for(int group=0;group<3;group++) {
            BlockPos b=bp[group]; Direction axis=axes[group];
            set(l,b,"producer/block_steam_generator"); m.boilers.add(b);
            var be=l.getBlockEntity(b); var tag=be.saveWithoutMetadata(l.registryAccess());
            tag.putInt("pressure",221); tag.putInt("inputMB",group<2?4:1);
            tag.putFloat("systemHeat",100F+221F/220F*100F*2.74F);
            be.loadWithComponents(tag,l.registryAccess());
            if(fluids(l,b).fill(new FluidStack(BuiltInRegistries.FLUID.get(id("distilledwater")),10000),IFluidHandler.FluidAction.EXECUTE)!=10000)throw new IllegalStateException("Water charge");
            Direction[] sides=group<2?new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}:new Direction[]{Direction.WEST};
            for(var d:sides) {
                BlockPos e=b.relative(d),port=group<2?e.relative(group==0?Direction.DOWN:Direction.UP):e.west();
                set(l,port,"reactor/block_reactor_fluid_port"); facing(l,e,"hugenerator/block_heat_source_fluid",d.getOpposite());
                for(int slot=4;slot<14;slot++)insert(l,e,slot,item("resource/item_heatconductor",1));
                ejector(l,e,14,1,null); ejector(l,port,0,1,null); m.exchangers.add(e);
            }
            for(int t=1;t<=2;t++) {
                BlockPos p=b.relative(axis,t),g=p.north();
                facing(l,p,"producer/block_steam_kinetic_generator",Direction.NORTH);
                insert(l,p,2,item("normal/item_steam_turbin",1)); ejector(l,p,3,4,axis.getOpposite()); m.turbines.add(p);
                facing(l,g,"generator/block_kinetic_generator",Direction.SOUTH); m.generators.add(g);
                set(l,g.north(),"wiring/cable/block_glass_cable");set(l,g.north(2),"wiring/cable/block_glass_cable");
            }
            BlockPos co=b.relative(axis,3);set(l,co,"producer/block_condenser");m.condensers.add(co);
            ejector(l,co,7,4,axis.getOpposite());
            if(group<2) { insert(l,co,0,item("reactor/item_reactor_vent",1)); ((AbstractEnergyBlockEntity)l.getBlockEntity(co)).getEnergyStorageInternal().setEnergy(1000); }
            for(int i=1;i<=3;i++)set(l,co.north(i),"wiring/cable/block_glass_cable");
        }
        for(int y=-6;y<=6;y++)set(l,c.offset(0,y,-3),"wiring/cable/block_glass_cable");
        for(int x=0;x<=8;x++)set(l,c.offset(x,0,-3),"wiring/cable/block_glass_cable");
        for(int x=5;x<=7;x++)set(l,c.offset(x,0,-3),"wiring/cable/block_glass_cable");
        for(int x=8;x<=16;x++)set(l,c.offset(x,0,-4),"wiring/cable/block_glass_cable");
        for(int x=9;x<=16;x++){BlockPos p=c.offset(x,0,-3);facing(l,p,"wiring/block_mfsu",Direction.SOUTH);m.stores.add(p);}
        for(int y=0;y<6;y++)for(int x=0;x<9;x++) {
            String suffix=switch(ROWS[y].charAt(x)){case 'C'->"golden_vent";case 'Y'->"iridium_reflector";case 'S'->"uranium_quad";case 'J'->"vent_spread";default->null;};
            if(suffix!=null)insert(l,c,y*9+x,item("reactor/item_reactor_"+suffix,1));
        }
        return m;
    }
    @GameTest(batch="heat_complex",template="loop",timeoutTicks=6100)
    public static void closedLoopProducesPowerWithoutReplenishment(GameTestHelper h) {
        Module m=buildModule(h.getLevel(),h.absolutePos(new BlockPos(12,12,12)));
        h.runAfterDelay(5,()->{m.charge();m.power(true);});
        long[] baseline={0};
        h.onEachTick(()->{
            long tick=h.getTick();if(tick<6)return;
            h.assertTrue(m.coolant()==10000,"Lost coolant");
            h.assertTrue(m.reactor().getCurrentHeat()<=32,"Unsafe hull: "+m.status());
            if(tick%1000==0)System.out.println("HEAT_COMPLEX tick="+tick+" "+m.status());
            if(tick==5000)baseline[0]=m.energy();
            if(tick==6000){
                long rate=m.energy()-baseline[0];
                h.assertTrue(rate>500000,"Insufficient real generation: "+rate+" EU per 1000 ticks");
                for(var b:m.boilers)h.assertTrue(fluids(m.level,b).getFluidInTank(0).getAmount()>8000,"Water loop draining");
                System.out.println("HEAT_COMPLEX VERIFIED EU/t="+(rate/1000.0));m.power(false);h.succeed();
            }
        });
    }

    private static final List<Module> PLANT=new ArrayList<>();
    private static ServerLevel construction;
    private static int stripe=4128, commissioning=-1;
    private static long[] baseline;
    private static boolean reloadVerification;
    static void box(ServerLevel l,int x0,int y0,int z0,int x1,int y1,int z1,BlockState state) {
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)for(int y=y0;y<=y1;y++)l.setBlock(new BlockPos(x,y,z),state,2);
    }
    static void box(ServerLevel l,int x0,int y0,int z0,int x1,int y1,int z1,Block block) { box(l,x0,y0,z0,x1,y1,z1,block.defaultBlockState()); }
    static void sign(ServerLevel l,int x,int y,int z,String a,String b,String c,String d) {
        var p=new BlockPos(x,y,z);l.setBlock(p,Blocks.OAK_SIGN.defaultBlockState(),3);
        var s=(SignBlockEntity)l.getBlockEntity(p);var text=s.getFrontText();
        String[] lines={a,b,c,d};for(int i=0;i<4;i++)text=text.setMessage(i,Component.literal(lines[i]));s.setText(text,true);s.setWaxed(true);s.setChanged();
    }
    @SubscribeEvent public static void constructionStarted(ServerStartedEvent event) {
        if(Boolean.getBoolean("si.heatcomplex.verify")) {
            construction=event.getServer().overworld();reloadVerification=true;commissioning=0;
            for(int row=0;row<4;row++)for(int col=0;col<4;col++){
                var m=new Module(construction,new BlockPos(4160+40*col,96,1128+40*row));
                var c=m.center;BlockPos[] bp={c.above(3),c.below(3),c.east(4)};Direction[] axes={Direction.UP,Direction.DOWN,Direction.EAST};
                for(int group=0;group<3;group++){
                    m.boilers.add(bp[group]);for(int t=1;t<=2;t++){m.turbines.add(bp[group].relative(axes[group],t));m.generators.add(bp[group].relative(axes[group],t).north());}
                    m.condensers.add(bp[group].relative(axes[group],3));
                    Direction[] sides=group<2?new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}:new Direction[]{Direction.WEST};
                    for(var d:sides)m.exchangers.add(bp[group].relative(d));
                }
                for(int x=9;x<=16;x++)m.stores.add(c.offset(x,0,-3));PLANT.add(m);
            }
            command("execute in minecraft:overworld run forceload add 4144 1112 4303 1311");
            for(var m:PLANT){m.power(false);serviceAccess(construction,m.center);}
            for(int tx:new int[]{4352,4404})for(int tz:new int[]{1144,1232})basin(construction,tx,tz);
            System.out.println("HEAT_COMPLEX RELOAD checking native save and datapack controls");return;
        }
        if(!Boolean.getBoolean("si.heatcomplex.build"))return;
        construction=event.getServer().overworld();
        // Temporary construction tickets; no tickets from this sandbox are exported.
        for(int x=257;x<=278;x++)for(int z=67;z<=86;z++)construction.setChunkForced(x,z,true);
        System.out.println("HEAT_COMPLEX CONSTRUCTION terrain-generation begun");
    }
    @SubscribeEvent public static void constructionTick(ServerTickEvent.Post event) {
        if(construction==null||construction.getServer()!=event.getServer())return;
        try {
            if(reloadVerification) { reloadTick(event);return; }
            if(stripe<=4431) {
                // Work in stripes instead of stalling the server for the whole terrain clearance.
                for(int x=stripe;x<=Math.min(stripe+1,4431);x++)for(int z=1088;z<=1375;z++) {
                    for(int y=85;y<=220;y++){var p=new BlockPos(x,y,z);if(!construction.getBlockState(p).isAir())construction.setBlock(p,Blocks.AIR.defaultBlockState(),2);}
                    for(int y=81;y<=84;y++)construction.setBlock(new BlockPos(x,y,z),(y==84?Blocks.SMOOTH_STONE:Blocks.DEEPSLATE_BRICKS).defaultBlockState(),2);
                    if(x%16==0&&z%16==0)for(int y=80;y>=construction.getMinBuildHeight();y--){var p=new BlockPos(x,y,z);if(construction.getBlockState(p).isSolid())break;construction.setBlock(p,Blocks.DEEPSLATE_BRICKS.defaultBlockState(),2);}
                }
                if(stripe%32==0)System.out.println("HEAT_COMPLEX clear stripe="+stripe);stripe+=2;return;
            }
            if(commissioning==-1) {
                architecture(construction);
                for(int row=0;row<4;row++)for(int col=0;col<4;col++)PLANT.add(buildModule(construction,new BlockPos(4160+40*col,96,1128+40*row)));
                commissioning=0;System.out.println("HEAT_COMPLEX constructed 16 modules");return;
            }
            commissioning++;
            if(commissioning==10)for(var m:PLANT){m.charge();m.power(true);}
            if(commissioning<11)return;
            for(int i=0;i<PLANT.size();i++) {
                var m=PLANT.get(i);
                if(m.coolant()!=10000||m.reactor().getCurrentHeat()>32)throw new IllegalStateException("Unsafe module "+i+" "+m.status());
            }
            if(commissioning==1000){baseline=new long[16];for(int i=0;i<16;i++)baseline[i]=PLANT.get(i).energy();}
            if(commissioning%500==0)System.out.println("HEAT_COMPLEX station tick="+commissioning+" EU="+PLANT.stream().mapToLong(Module::energy).sum());
            if(commissioning==2000) {
                StringBuilder report=new StringBuilder("16-reactor station / actual 1000-tick measurements\n");long total=0;
                for(int i=0;i<16;i++) {
                    var m=PLANT.get(i);long gain=m.energy()-baseline[i];
                    if(gain<500000)throw new IllegalStateException("Insufficient output module "+i+" "+gain);
                    for(var b:m.boilers)if(fluids(construction,b).getFluidInTank(0).getAmount()<8000)throw new IllegalStateException("Water draining "+i);
                    total+=gain;report.append("R").append(i+1).append(" @ ").append(m.center).append(" EU/t=").append(gain/1000.0).append(" hull=").append(m.reactor().getCurrentHeat()).append(" coolant=").append(m.coolant()).append('\n');
                    m.power(false);
                    // Replace commissioning consumables with new, ordinary rods/rotors for delivery.
                    var rh=(IItemHandler)ArmoryServiceGameTests.call(construction.getBlockEntity(m.center),"getItemHandler");
                    rh.extractItem(10,1,false);insert(construction,m.center,10,item("reactor/item_reactor_uranium_quad",1));
                    for(var t:m.turbines){var th=(IItemHandler)ArmoryServiceGameTests.call(construction.getBlockEntity(t),"getItemHandler");th.extractItem(2,1,false);insert(construction,t,2,item("normal/item_steam_turbin",1));}
                }
                report.append("TOTAL NET EU/t=").append(total/1000.0).append('\n');
                Files.writeString(Path.of("commissioning.txt"),report.toString());System.out.println(report);
            }
            if(commissioning==2060) {
                for(var m:PLANT)if(m.reactor().isRunning())throw new IllegalStateException("Delivery shutdown failed");
                for(int x=257;x<=278;x++)for(int z=67;z<=86;z++)construction.setChunkForced(x,z,false);
                event.getServer().saveEverything(true,true,true);
                Files.writeString(Path.of("CONSTRUCTION_COMPLETE"),"16 safe stopped reactors; actual closed loops verified\n");
                construction=null;event.getServer().halt(false);
            }
        }catch(Exception failure){failure.printStackTrace();construction=null;event.getServer().halt(false);}
    }
    private static void command(String text) {
        construction.getServer().getCommands().performPrefixedCommand(construction.getServer().createCommandSourceStack().withPermission(4),text);
    }
    private static void reloadTick(ServerTickEvent.Post event)throws Exception {
        commissioning++;
        if(commissioning==5)command("function si_heat_complex:start");
        if(commissioning<200)return;
        for(var m:PLANT) {
            if(m.coolant()!=10000||m.reactor().getCurrentHeat()>32)throw new IllegalStateException("Reload unsafe "+m.status());
            if(commissioning==200&&!m.reactor().isRunning())throw new IllegalStateException("Datapack did not start "+m.center+" formed="+m.reactor().isValidFluidReactorStructure()+" "+m.status());
        }
        if(commissioning==300){baseline=new long[16];for(int i=0;i<16;i++)baseline[i]=PLANT.get(i).energy();}
        if(commissioning==1300){
            long total=0;for(int i=0;i<16;i++){long gain=PLANT.get(i).energy()-baseline[i];if(gain<500000)throw new IllegalStateException("Reload generation failed "+i+" "+gain);total+=gain;}
            System.out.println("HEAT_COMPLEX RELOAD net EU/t="+total/1000.0);
            // Remove one genuine rotor and require the datapack safety function to stop all cores.
            var m=PLANT.getFirst();var t=m.turbines.getFirst();
            ((IItemHandler)ArmoryServiceGameTests.call(construction.getBlockEntity(t),"getItemHandler")).extractItem(2,1,false);
        }
        if(commissioning==1340) {
            for(var m:PLANT)if(m.reactor().isRunning())throw new IllegalStateException("Missing rotor safety failed "+m.center);
            insert(construction,PLANT.getFirst().turbines.getFirst(),2,item("normal/item_steam_turbin",1));
            command("function si_heat_complex:start");
        }
        if(commissioning==1380) {
            for(var m:PLANT)if(!m.reactor().isRunning())throw new IllegalStateException("Restart after repair failed "+m.center);
            command("function si_heat_complex:stop");
        }
        if(commissioning==1410) {
            for(var m:PLANT) {
                if(m.reactor().isRunning())throw new IllegalStateException("Master shutdown failed");
                var rh=(IItemHandler)ArmoryServiceGameTests.call(construction.getBlockEntity(m.center),"getItemHandler");rh.extractItem(10,1,false);insert(construction,m.center,10,item("reactor/item_reactor_uranium_quad",1));
                for(var t:m.turbines){var th=(IItemHandler)ArmoryServiceGameTests.call(construction.getBlockEntity(t),"getItemHandler");th.extractItem(2,1,false);insert(construction,t,2,item("normal/item_steam_turbin",1));}
            }
            sign(construction,4222,85,1310,"中央总开关","右键启动 / 停堆","全站同步加载","检修时请停堆");
            sign(construction,4226,85,1310,"16 堆热核基地","约 10.6 kEU/t","转子异常自动停堆","备品位于西侧箱子");
        }
        if(commissioning==1610) {
            if(!construction.getForcedChunks().isEmpty())throw new IllegalStateException("Stopped station left forced chunks");
            event.getServer().saveEverything(true,true,true);Files.writeString(Path.of("RELOAD_VERIFIED"),"Native reload, 16-core generation, missing rotor SCRAM, repair/restart, master stop and ticket release passed.\n");
            construction=null;event.getServer().halt(false);
        }
    }
    static void architecture(ServerLevel l) {
        // 304 x 288 m raised campus, illuminated perimeter and avenues.
        for(int x=4128;x<=4431;x++)for(int z=1088;z<=1375;z++) {
            boolean edge=x<4132||x>4427||z<1092||z>1371;
            boolean road=x>=4320&&x<=4331||z>=1276&&z<=1283||x>=4216&&x<=4231&&z>=1280;
            if(edge||road)l.setBlock(new BlockPos(x,84,z),(edge?Blocks.POLISHED_DEEPSLATE:Blocks.GRAY_CONCRETE).defaultBlockState(),2);
            if((x%16==0&&z%16==0)||road&&(x%16==0||z%16==0))l.setBlock(new BlockPos(x,84,z),Blocks.SEA_LANTERN.defaultBlockState(),2);
        }
        for(int x=4130;x<=4430;x+=16){box(l,x,85,1090,x,87,1090,Blocks.POLISHED_DEEPSLATE);box(l,x,88,1090,x,88,1090,Blocks.SEA_LANTERN);box(l,x,85,1373,x,87,1373,Blocks.POLISHED_DEEPSLATE);box(l,x,88,1373,x,88,1373,Blocks.SEA_LANTERN);}
        // Cathedral-scale barrel vault. All mechanical circuits remain accessible underneath.
        box(l,4144,85,1112,4312,117,1112,Blocks.LIGHT_BLUE_STAINED_GLASS);
        box(l,4144,85,1272,4312,117,1272,Blocks.LIGHT_BLUE_STAINED_GLASS);
        box(l,4144,85,1113,4144,117,1271,Blocks.LIGHT_BLUE_STAINED_GLASS);
        box(l,4312,85,1113,4312,117,1271,Blocks.LIGHT_BLUE_STAINED_GLASS);
        for(int z=1112;z<=1272;z+=16)for(int x:new int[]{4144,4312}){box(l,x-1,85,z-1,x+1,118,z+1,Blocks.WHITE_CONCRETE);box(l,x,89,z-2,x,115,z-2,Blocks.SEA_LANTERN);}
        for(int x=4144;x<=4312;x++) {
            double t=(x-4228)/84.0;int roof=117+(int)Math.round(25*Math.sqrt(Math.max(0,1-t*t)));
            for(int z=1112;z<=1272;z++) {
                l.setBlock(new BlockPos(x,roof,z),(z%16==8?Blocks.POLISHED_DEEPSLATE:Blocks.CYAN_STAINED_GLASS).defaultBlockState(),2);
                if(z==1112||z==1272)for(int y=118;y<roof;y++)l.setBlock(new BlockPos(x,y,z),Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),2);
            }
        }
        for(int x=4152;x<=4304;x+=16){box(l,x,85,1112,x+2,118,1112,Blocks.WHITE_CONCRETE);box(l,x,85,1272,x+2,118,1272,Blocks.WHITE_CONCRETE);}
        // Ground entrances and viewing corridors.
        box(l,4218,85,1272,4237,94,1272,Blocks.AIR);
        box(l,4218,85,1112,4237,94,1112,Blocks.AIR);
        for(int row=0;row<4;row++)for(int col=0;col<4;col++) {
            int cx=4160+40*col,cz=1128+40*row;
            box(l,cx-5,88,cz-6,cx+18,88,cz+5,Blocks.IRON_BLOCK);
            for(int x=cx-5;x<=cx+18;x++)for(int z=cz-6;z<=cz+5;z++)if(x==cx-5||x==cx+18||z==cz-6||z==cz+5)
                l.setBlock(new BlockPos(x,88,z),((x+z)%2==0?Blocks.YELLOW_CONCRETE:Blocks.BLACK_CONCRETE).defaultBlockState(),2);
            // Short broad access steps, never occupy machine faces.
            for(int rise=0;rise<4;rise++)box(l,cx+9,85+rise,cz+9-rise,cx+16,85+rise,cz+9-rise,Blocks.SMOOTH_STONE);
            for(int dx:new int[]{-5,18})for(int dz:new int[]{-6,5})box(l,cx+dx,89,cz+dz,cx+dx,105,cz+dz,Blocks.POLISHED_DEEPSLATE);
            sign(l,cx+10,89,cz+5,"REACTOR "+(row*4+col+1),"896 HU/t","221 bar / 2-stage","8 MFSU / outputs S");
            sign(l,cx-4,89,cz+5,"LOCAL CONTROL","OFF for delivery","10,000 mB coolant","Do not break loop");
            serviceAccess(l,new BlockPos(cx,96,cz));
        }
        for(int z:new int[]{1148,1188,1228,1268}) {
            box(l,4148,107,z-2,4308,107,z+2,Blocks.POLISHED_DEEPSLATE);
            box(l,4148,108,z-3,4308,109,z-3,Blocks.CYAN_STAINED_GLASS);
            box(l,4148,108,z+3,4308,109,z+3,Blocks.CYAN_STAINED_GLASS);
            for(int x=4152;x<=4304;x+=8)box(l,x,107,z,x,107,z,Blocks.SEA_LANTERN);
        }
        box(l,4304,107,1116,4308,107,1270,Blocks.POLISHED_DEEPSLATE);
        box(l,4309,108,1116,4309,109,1270,Blocks.CYAN_STAINED_GLASS);
        // A 23-step staircase gives genuine access to the upper observation deck.
        for(int i=0;i<23;i++)box(l,4298,85+i,1294-i,4302,85+i,1294-i,Blocks.SMOOTH_STONE);
        box(l,4300,107,1268,4308,107,1272,Blocks.SMOOTH_STONE);
        // Overhead yellow travelling gantry and suspended hook.
        box(l,4148,120,1184,4308,122,1188,Blocks.YELLOW_CONCRETE);
        for(int x=4148;x<=4308;x+=4)box(l,x,120,1183,x+1,120,1183,Blocks.BLACK_CONCRETE);
        box(l,4222,118,1183,4232,124,1189,Blocks.POLISHED_DEEPSLATE);
        box(l,4227,109,1186,4227,117,1186,Blocks.CHAIN);box(l,4225,108,1186,4227,108,1186,Blocks.IRON_BLOCK);
        // Four 96 m hyperboloid cooling towers with glowing rim and real smoke particles.
        for(int tx:new int[]{4352,4404})for(int tz:new int[]{1144,1232})tower(l,tx,tz);
        // Control building / arrival plaza.
        box(l,4184,85,1288,4264,104,1324,Blocks.WHITE_CONCRETE);
        box(l,4186,85,1290,4262,103,1322,Blocks.AIR);
        box(l,4184,89,1288,4264,101,1288,Blocks.CYAN_STAINED_GLASS);
        box(l,4184,89,1324,4264,101,1324,Blocks.LIGHT_BLUE_STAINED_GLASS);
        box(l,4194,105,1294,4254,105,1318,Blocks.BLACK_CONCRETE);
        box(l,4196,106,1296,4252,106,1316,Blocks.GRAY_CONCRETE);
        box(l,4218,85,1324,4230,94,1324,Blocks.AIR);
        box(l,4218,85,1288,4230,94,1288,Blocks.AIR);
        for(int x=4192;x<=4256;x+=8){box(l,x,88,1296,x+3,88,1296,Blocks.POLISHED_DEEPSLATE);box(l,x,89,1296,x+3,90,1296,Blocks.LIME_STAINED_GLASS);sign(l,x+1,91,1296,"SI THERMAL GRID","16 reactors","48 steam loops","128 MFSU");}
        box(l,4222,84,1310,4226,84,1312,Blocks.SMOOTH_STONE);
        l.setBlock(new BlockPos(4224,85,1310),Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR),3);
        sign(l,4222,85,1310,"MASTER SWITCH","UP: ALL STOPPED","DOWN: ALL RUN","Keep circuits intact");
        sign(l,4226,85,1310,"16 x THERMAL","~10.6 kEU/t","Fuel/rotors finite","See world README");
        // Spare ordinary consumables, not automated fuel or water creation.
        var chestPos=new BlockPos(4192,85,1314);l.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),3);
        var chest=(ChestBlockEntity)l.getBlockEntity(chestPos);
        for(int i=0;i<16;i++)chest.setItem(i,item("reactor/item_reactor_uranium_quad",1));
        for(int i=16;i<27;i++)chest.setItem(i,item("normal/item_steam_turbin",1));chest.setChanged();
        var chest2Pos=new BlockPos(4195,85,1314);l.setBlock(chest2Pos,Blocks.CHEST.defaultBlockState(),3);
        var chest2=(ChestBlockEntity)l.getBlockEntity(chest2Pos);for(int i=0;i<27;i++)chest2.setItem(i,item("normal/item_steam_turbin",1));chest2.setChanged();
        for(int x=4140;x<=4420;x+=20)for(int z:new int[]{1096,1360}) {
            box(l,x,85,z,x,96,z,Blocks.POLISHED_DEEPSLATE);box(l,x-1,97,z-1,x+1,97,z+1,Blocks.SEA_LANTERN);
        }
        // Monumental gate, cyan beacon-like luminous spire and reflecting pools.
        for(int x:new int[]{4204,4244}){box(l,x,85,1344,x+3,116,1347,Blocks.WHITE_CONCRETE);box(l,x+1,87,1343,x+1,115,1343,Blocks.SEA_LANTERN);}
        box(l,4204,116,1344,4247,119,1347,Blocks.POLISHED_DEEPSLATE);
        sign(l,4224,85,1342,"SINGULARITY","16-CORE THERMAL","POWER STATION","CONTROL ROOM AHEAD");
        for(int x0:new int[]{4152,4272}){box(l,x0,84,1328,x0+32,84,1352,Blocks.WHITE_CONCRETE);box(l,x0+1,84,1329,x0+31,84,1351,Blocks.WATER);}
        box(l,4348,85,1328,4364,85,1344,Blocks.POLISHED_DEEPSLATE);
        box(l,4354,86,1334,4358,153,1338,Blocks.CYAN_STAINED_GLASS);
        box(l,4356,86,1336,4356,164,1336,Blocks.SEA_LANTERN);
    }
    static void tower(ServerLevel l,int cx,int cz) {
        for(int y=85;y<=180;y++) {
            double dy=(y-144)/48.0;double radius=Math.sqrt(13*13+175*dy*dy);
            for(int dx=-25;dx<=25;dx++)for(int dz=-25;dz<=25;dz++) {
                double r=Math.sqrt(dx*dx+dz*dz);if(r>radius||r<radius-1.6)continue;
                if(y<97&&(Math.abs(dx)%8>2&&Math.abs(dz)%8>2))continue;
                double angle=Math.atan2(dz,dx);boolean brace=Math.floorMod((int)(angle*24/Math.PI)+(y/4),12)==0;
                Block b=y==180||y==141&&Math.floorMod(dx+dz,8)==0?Blocks.SEA_LANTERN:brace?Blocks.LIGHT_GRAY_CONCRETE:Blocks.WHITE_CONCRETE;
                l.setBlock(new BlockPos(cx+dx,y,cz+dz),b.defaultBlockState(),2);
            }
        }
        for(int dx=-18;dx<=18;dx++)for(int dz=-18;dz<=18;dz++)if(dx*dx+dz*dz<18*18)l.setBlock(new BlockPos(cx+dx,85,cz+dz),Blocks.WATER.defaultBlockState(),2);
        for(int i=0;i<8;i++){double a=i*Math.PI/4;int x=cx+(int)(Math.cos(a)*14),z=cz+(int)(Math.sin(a)*14);l.setBlock(new BlockPos(x,179,z),Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.SIGNAL_FIRE,true),2);}
        sign(l,cx,86,cz+25,"COOLING TOWER","THERMAL CAMPUS","Landscape structure","Closed loops in hall");
        basin(l,cx,cz);
    }
    static void basin(ServerLevel l,int cx,int cz) {
        // Retaining wall below the arch openings keeps ornamental water off the service roads.
        for(int dx=-27;dx<=27;dx++)for(int dz=-27;dz<=27;dz++) {
            double r=Math.sqrt(dx*dx+dz*dz);
            for(int y=85;y<=87;y++) {
                var p=new BlockPos(cx+dx,y,cz+dz);
                if(r>=18&&r<=20.5&&y<=86)l.setBlock(p,Blocks.WHITE_CONCRETE.defaultBlockState(),3);
                else if(r>=18&&l.getBlockState(p).is(Blocks.WATER))l.setBlock(p,Blocks.AIR.defaultBlockState(),3);
            }
        }
    }
    static void serviceAccess(ServerLevel l,BlockPos c) {
        // Accessible reactor hatch, battery outputs and both turbine levels in survival reach.
        set(l,c.south(2),"reactor/block_reactor_access_hatch");
        int x=c.getX(),y=c.getY(),z=c.getZ();
        box(l,x-4,y-1,z+4,x+17,y-1,z+4,Blocks.SMOOTH_STONE);
        box(l,x+8,y-1,z-2,x+17,y-1,z+3,Blocks.SMOOTH_STONE);
        box(l,x-4,y-1,z-4,x-3,y-1,z+3,Blocks.SMOOTH_STONE);
        for(int step=0;step<11;step++)box(l,x+16,y-11+step,z+15-step,x+17,y-11+step,z+15-step,Blocks.SMOOTH_STONE);
        box(l,x-3,y+5,z+4,x+3,y+5,z+5,Blocks.SMOOTH_STONE);
        for(int yy=y;yy<=y+7;yy++)l.setBlock(new BlockPos(x-4,yy,z+5),Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST),3);
        sign(l,x+2,y,z+4,"反应堆检修口","右键南侧访问舱","换燃料前停堆","南面为储电输出");
    }
}
