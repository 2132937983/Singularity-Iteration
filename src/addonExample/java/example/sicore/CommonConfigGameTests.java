package example.sicore;

import com.miophas.singularity_iteration.core.runtime.reactor.NuclearBlastProfile;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorAccidentLatch;
import com.miophas.singularity_iteration.core.runtime.radiation.RadiationState;
import com.miophas.singularity_iteration.core.runtime.radiation.NuclearFalloutState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.HashSet;
import java.util.List;
import java.util.function.BooleanSupplier;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class CommonConfigGameTests {
    static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("mio_icif",path);}
    static Block block(String path){return BuiltInRegistries.BLOCK.get(id(path));}
    static Class<?> implementation(String suffix){
        try{return Class.forName(block("reactor/block_reactor_nuke").getClass().getPackageName().replace("block.reactor",suffix));}
        catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    static Object config(String key){try{return implementation("Singularity_Iteration_Config").getField(key).get(null);}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}}
    static Object get(String key){return ArmoryServiceGameTests.call(config(key),"get");}
    static void set(String key,Object value){ArmoryServiceGameTests.call(config(key),"set",value);}
    static net.minecraft.world.level.block.entity.BlockEntity placed(GameTestHelper h,String path,BlockPos pos){h.setBlock(pos,block(path));return h.getBlockEntity(pos);}
    static Object nuke(GameTestHelper h,BlockPos pos){
        var entity=placed(h,"reactor/block_reactor_nuke",pos);
        ArmoryServiceGameTests.call(entity,"setItem",0,new ItemStack(block("reactor/block_ic_tnt"),64));return entity;
    }
    static void clean(GameTestHelper h,HashSet<java.util.UUID> before){
        ArmoryServiceGameTests.call(implementation("blockentity.reactor.NukeExplosionScheduler"),"clear",h.getLevel());
        for(var key:new HashSet<>(RadiationState.get(h.getLevel()).zones.keySet()))if(!before.contains(key))ArmoryServiceGameTests.call(implementation("blockentity.reactor.NukeRadiationZoneManager"),"removeRadiationZone",key);
    }
    @GameTest(template="reactor_loop",batch="common_config",timeoutTicks=40)
    public static void nuclearBombScaleRetainsLegacyCapsAndRejectsInvalidValues(GameTestHelper h){
        h.assertTrue(NuclearBlastProfile.calculate(1024,1).power()==1024&&NuclearBlastProfile.calculate(1024,1).radius()==61,"Default bomb changed");
        h.assertTrue(NuclearBlastProfile.calculate(1024,2).power()==2048&&NuclearBlastProfile.calculate(1024,2).radius()==102,"Bomb coefficient ignored");
        h.assertTrue(NuclearBlastProfile.calculate(1024,.5).radius()==41&&NuclearBlastProfile.calculate(1024,0).disabled(),"Low/zero coefficient wrong");
        h.assertTrue(NuclearBlastProfile.calculate(1000000,16).power()==1000000&&NuclearBlastProfile.calculate(1000000,16).radius()==2000,"Legacy cap bypassed");
        for(double invalid:new double[]{-1,17,Double.NaN,Double.POSITIVE_INFINITY}){
            boolean rejected=false;try{NuclearBlastProfile.calculate(1024,invalid);}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"Invalid multiplier accepted");
        }h.succeed();
    }
    @GameTest(template="reactor_loop",batch="common_config",timeoutTicks=40)
    public static void actualBombAdmissionUsesOnlyItsOwnCoefficient(GameTestHelper h){
        Object old=get("NUKE_EXPLOSION_MULTIPLIER"),enabled=get("ENABLE_NUCLEAR_EXPLOSION"),reactor=get("REACTOR_EXPLOSION_MULTIPLIER");
        var before=new HashSet<>(RadiationState.get(h.getLevel()).zones.keySet());
        try{
            set("ENABLE_NUCLEAR_EXPLOSION",true);set("REACTOR_EXPLOSION_MULTIPLIER",7.0);
            for(double multiplier:new double[]{.5,1,2}){
                set("NUKE_EXPLOSION_MULTIPLIER",multiplier);BlockPos pos=new BlockPos(6,3,6);Object entity=nuke(h,pos);
                float power=(float)ArmoryServiceGameTests.call(entity,"calculateExplosionPower");
                h.assertTrue(power==1024*multiplier,"Actual nuke reads reactor multiplier or ignores bomb multiplier");
                boolean admitted=(boolean)ArmoryServiceGameTests.call(entity.getClass(),"tryTriggerExplosion",h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),entity);
                h.assertTrue(admitted&&h.getBlockState(pos).isAir(),"Scaled bomb failed admission");
                var added=RadiationState.get(h.getLevel()).zones.entrySet().stream().filter(entry->!before.contains(entry.getKey())).toList();
                h.assertTrue(added.size()==1&&added.getFirst().getValue().power()==power
                    &&added.getFirst().getValue().radius()==(int)Math.ceil(20+power/25.0),"Scaled damage/radiation geometry differs from actual yield");
                clean(h,before);
            }
        }finally{set("NUKE_EXPLOSION_MULTIPLIER",old);set("ENABLE_NUCLEAR_EXPLOSION",enabled);set("REACTOR_EXPLOSION_MULTIPLIER",reactor);clean(h,before);}h.succeed();
    }
    @GameTest(template="reactor_loop",batch="common_config",timeoutTicks=40)
    public static void disabledPlacedAndPrimedBombsDoNotDestroyNearbyTerrain(GameTestHelper h){
        Object enabled=get("ENABLE_NUCLEAR_EXPLOSION"),scale=get("NUKE_EXPLOSION_MULTIPLIER");var before=new HashSet<>(RadiationState.get(h.getLevel()).zones.keySet());
        try{
            BlockPos pos=new BlockPos(6,3,6);h.setBlock(pos.east(),Blocks.GLASS);
            set("ENABLE_NUCLEAR_EXPLOSION",false);set("NUKE_EXPLOSION_MULTIPLIER",1.0);Object entity=nuke(h,pos);
            ArmoryServiceGameTests.call(entity.getClass(),"tryTriggerExplosion",h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),entity);
            h.assertTrue(h.getBlockState(pos).isAir()&&h.getBlockState(pos.east()).is(Blocks.GLASS),"Disabled placed nuke breaks neighbouring blocks");
            var example=BuiltInRegistries.ENTITY_TYPE.get(id("nuke_primed")).create(h.getLevel());Vec3 center=Vec3.atCenterOf(h.absolutePos(pos));
            Entity primed=(Entity)example.getClass().getConstructor(Level.class,double.class,double.class,double.class,LivingEntity.class,List.class)
                .newInstance(h.getLevel(),center.x,center.y,center.z,null,List.of(new ItemStack(block("reactor/block_ic_tnt"),64)));
            primed.setNoGravity(true);h.getLevel().addFreshEntity(primed);ArmoryServiceGameTests.call(primed,"setFuse",1);primed.tick();
            h.assertTrue(primed.isRemoved()&&h.getBlockState(pos.east()).is(Blocks.GLASS),"Disabled primed nuke breaks terrain");
            h.assertTrue(RadiationState.get(h.getLevel()).zones.keySet().equals(before),"Disabled nuke creates fallout");
            set("ENABLE_NUCLEAR_EXPLOSION",true);set("NUKE_EXPLOSION_MULTIPLIER",0.0);entity=nuke(h,pos);
            ArmoryServiceGameTests.call(entity.getClass(),"tryTriggerExplosion",h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),entity);
            h.assertTrue(h.getBlockState(pos).isAir()&&h.getBlockState(pos.east()).is(Blocks.GLASS)
                &&RadiationState.get(h.getLevel()).zones.keySet().equals(before),"Zero bomb coefficient creates nuclear terrain work or strands the source");
        }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
        finally{set("ENABLE_NUCLEAR_EXPLOSION",enabled);set("NUKE_EXPLOSION_MULTIPLIER",scale);clean(h,before);}h.succeed();
    }
    @GameTest(template="reactor_loop",batch="common_config",timeoutTicks=40)
    public static void globalDisablePausesAndResumesPressureAndFalloutCursors(GameTestHelper h){
        Object enabled=get("ENABLE_NUCLEAR_EXPLOSION");BlockPos pos=new BlockPos(6,3,6);Vec3 center=Vec3.atCenterOf(h.absolutePos(pos));
        try{
            h.setBlock(pos,Blocks.BRICKS);set("ENABLE_NUCLEAR_EXPLOSION",false);
            Object task=implementation("blockentity.reactor.NukeExplosionTask").getConstructor(ServerLevel.class,Vec3.class,float.class,int.class,List.class).newInstance(h.getLevel(),center,400F,2,List.of());
            for(int i=0;i<100;i++)ArmoryServiceGameTests.call(task,"advance");
            h.assertTrue(h.getBlockState(pos).is(Blocks.BRICKS)&&!(boolean)ArmoryServiceGameTests.call(task,"isComplete"),"Global disable did not pause pressure work");
            set("ENABLE_NUCLEAR_EXPLOSION",true);int steps=0;while(!(boolean)ArmoryServiceGameTests.call(task,"isComplete")&&steps++<1000000)ArmoryServiceGameTests.call(task,"advance");
            h.assertTrue(steps<1000000&&h.getBlockState(pos).isAir(),"Pressure work did not resume");
            h.setBlock(pos,Blocks.GRASS_BLOCK);h.setBlock(pos.above(),Blocks.OAK_LEAVES);
            var footprint=new NuclearFalloutState.Region(center,2,2,System.currentTimeMillis());
            Object cleanup=implementation("blockentity.reactor.NuclearFalloutChunkTask").getConstructor(ServerLevel.class,NuclearFalloutState.Region.class,LevelChunk.class,NuclearFalloutState.class,BooleanSupplier.class)
                .newInstance(h.getLevel(),footprint,h.getLevel().getChunkAt(h.absolutePos(pos)),null,(BooleanSupplier)()->true);
            set("ENABLE_NUCLEAR_EXPLOSION",false);for(int i=0;i<100;i++)ArmoryServiceGameTests.call(cleanup,"getAsBoolean");
            h.assertTrue(footprint.columns.isEmpty()&&h.getBlockState(pos).is(Blocks.GRASS_BLOCK),"Global disable did not pause fallout");
            set("ENABLE_NUCLEAR_EXPLOSION",true);steps=0;while(!(boolean)ArmoryServiceGameTests.call(cleanup,"getAsBoolean")&&steps++<1000000){}
            h.assertTrue(steps<1000000&&!h.getBlockState(pos).is(Blocks.GRASS_BLOCK)&&h.getBlockState(pos.above()).isAir(),"Fallout did not resume");
        }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
        finally{set("ENABLE_NUCLEAR_EXPLOSION",enabled);}h.succeed();
    }
    @GameTest(template="reactor_loop",batch="common_config",timeoutTicks=40)
    public static void futureStorageSettingsApplyAtConstructionAndReloadWithoutDeletingEnergy(GameTestHelper h){
        Object capacity=get("FUTURE_ENERGY_CAPACITY"),receive=get("FUTURE_MAX_RECEIVE");
        try{
            set("FUTURE_ENERGY_CAPACITY",24000);set("FUTURE_MAX_RECEIVE",75);
            var machine=placed(h,"producer/block_future_elc",new BlockPos(6,3,6));Object storage=ArmoryServiceGameTests.field(machine,"energyStorage");
            h.assertTrue((long)ArmoryServiceGameTests.call(storage,"getCapacity")==24000&&(long)ArmoryServiceGameTests.call(storage,"getMaxReceive")==75,"Future machine ignores config at construction");
            ArmoryServiceGameTests.call(storage,"generateEnergyInternal",20000L,false);
            set("FUTURE_ENERGY_CAPACITY",12000);set("FUTURE_MAX_RECEIVE",64);
            ArmoryServiceGameTests.call(machine.getClass(),"tick",h.getLevel(),machine.getBlockPos(),machine.getBlockState(),machine);
            h.assertTrue((long)ArmoryServiceGameTests.call(storage,"getCapacity")==12000&&(long)ArmoryServiceGameTests.call(storage,"getMaxReceive")==64
                &&(long)ArmoryServiceGameTests.call(storage,"getAmount")==20000,"Future reload ignores settings or deletes owned EU");
        }finally{set("FUTURE_ENERGY_CAPACITY",capacity);set("FUTURE_MAX_RECEIVE",receive);}h.succeed();
    }
    @GameTest(template="reactor_loop",batch="common_config",timeoutTicks=40)
    public static void malformedIdentifiersAndNanCommodityValuesAreRejectedWithoutThrowing(GameTestHelper h){
        var config=implementation("Singularity_Iteration_Config");
        // 0.1.7.32: commodities are datapack entries; malformed ones are skipped, the file still loads
        var loader=implementation("future.FutureCommodityLoader");
        var map=new java.util.LinkedHashMap<String,Object>();
        var file=com.google.gson.JsonParser.parseString("{\"category\":\"mineral\",\"commodities\":["
            +"{\"item\":\"Bad ID!\",\"base_price\":10,\"volatility\":0.1},"
            +"{\"item\":\"minecraft:coal\",\"base_price\":10,\"volatility\":\"NaN\"},"
            +"{\"item\":\"minecraft:coal\",\"base_price\":10,\"volatility\":\"Infinity\"},"
            +"{\"item\":\"minecraft:coal\",\"base_price\":40,\"volatility\":0.05}]}");
        ArmoryServiceGameTests.call(loader,"parseFile",net.minecraft.resources.ResourceLocation.parse("si_test:futures"),file,map);
        h.assertTrue(map.size()==1&&map.containsKey("minecraft:coal")
            &&(int)ArmoryServiceGameTests.call(map.get("minecraft:coal"),"getBasePrice")==40,"Malformed commodity accepted or valid one rejected: "+map.keySet());
        h.assertTrue(!(boolean)ArmoryServiceGameTests.call(config,"validateCustomCostConfig","Bad ID! = (100, 1000)"),"Malformed scanner ID accepted");
        h.succeed();
    }
}
