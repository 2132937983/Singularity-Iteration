package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorAccidentLatch;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorBlastProfile;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle;
import com.miophas.singularity_iteration.core.runtime.radiation.RadiationState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.HashSet;
import java.util.List;
import java.util.function.BooleanSupplier;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class ReactorBlastGameTests {
    private static final BlockPos POS=new BlockPos(6,3,6);
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("mio_icif",path);}
    private static Block block(){return BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator"));}
    private static ItemStack fuel(String suffix){return new ItemStack(BuiltInRegistries.ITEM.get(id("reactor/item_reactor_"+suffix)));}
    private static Class<?> implementation(String suffix){
        try { return Class.forName(block().getClass().getPackageName().replace("block.generator",suffix)); }
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static Object config(String key){
        try { return implementation("Singularity_Iteration_Config").getField(key).get(null); }
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static Object get(Object value){return ArmoryServiceGameTests.call(value,"get");}
    private static void set(Object config,Object value){ArmoryServiceGameTests.call(config,"set",value);}
    private static void tick(GameTestHelper h,BlockEntity entity){
        ArmoryServiceGameTests.call(entity.getClass(),"tick",h.getLevel(),entity.getBlockPos(),entity.getBlockState(),entity);
    }
    private static ReactorAccidentLatch latch(BlockEntity entity){return (ReactorAccidentLatch)ArmoryServiceGameTests.field(entity,"accident");}
    private static BlockEntity overheat(GameTestHelper h,String rod){
        h.setBlock(POS,block());h.setBlock(POS.east(),Blocks.REDSTONE_BLOCK);
        BlockEntity entity=h.getBlockEntity(POS);
        var reactor=(IReactorController)entity;
        h.assertTrue(reactor.getItemHandler().insertItem(0,fuel(rod),false).isEmpty(),"Fuel insertion failed");
        ArmoryServiceGameTests.call(ArmoryServiceGameTests.call(entity,"getHeatStorage"),"setHeat",9999L);
        try { var f=entity.getClass().getDeclaredField("cycleRemaining");f.setAccessible(true);f.setInt(entity,0); }
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
        tick(h,entity);
        h.assertTrue(latch(entity).state()==ReactorAccidentLatch.State.PENDING,"Overheated cycle did not commit an accident");
        return entity;
    }
    private static void cleanup(GameTestHelper h,HashSet<java.util.UUID> before){
        ArmoryServiceGameTests.call(implementation("blockentity.reactor.NukeExplosionScheduler"),"clear",h.getLevel());
        for(var key:new HashSet<>(RadiationState.get(h.getLevel()).zones.keySet()))if(!before.contains(key))
            ArmoryServiceGameTests.call(implementation("blockentity.reactor.NukeRadiationZoneManager"),"removeRadiationZone",key);
    }
    @GameTest(template="reactor_loop",batch="reactor_blast",timeoutTicks=40)
    public static void activeSingleDualQuadAndMoxFuelFollowTheBackupCellWeights(GameTestHelper h){
        for(boolean mox:new boolean[]{false,true})for(int cells:new int[]{1,2,4}){
            var profile=ReactorBlastProfile.calculate(new ReactorCycle.Part[]{new ReactorCycle.Part(ReactorCycle.Profile.fuel(cells,mox),0,10)},1,1000);
            h.assertTrue(profile.fuelCells()==cells&&profile.radius()==8+4*cells,"Wrong cell weight/radius for "+cells+" mox="+mox);
        }
        var spent=ReactorBlastProfile.calculate(new ReactorCycle.Part[]{new ReactorCycle.Part(ReactorCycle.Profile.fuel(4,false),0,0)},1,1000);
        h.assertTrue(spent.fuelCells()==0&&spent.radius()==8,"Spent fuel still boosts the blast");h.succeed();
    }
    @GameTest(template="reactor_loop",batch="reactor_blast",timeoutTicks=40)
    public static void multiplierCapAndTwoPlatingFactorsActuallyScaleTheBlast(GameTestHelper h){
        var fuel=new ReactorCycle.Part(ReactorCycle.Profile.fuel(4,false),0,10);
        h.assertTrue(ReactorBlastProfile.calculate(new ReactorCycle.Part[]{fuel},2,1000).radius()==48,"Multiplier not applied");
        var packed=new ReactorCycle.Part[54];java.util.Arrays.fill(packed,fuel);
        h.assertTrue(ReactorBlastProfile.calculate(packed,1,1000).radius()==872,"Full reactor fuel count wrong");
        h.assertTrue(ReactorBlastProfile.calculate(packed,16,45).radius()==90,"Power limit bypassed by multiplier");
        var plated=ReactorBlastProfile.calculate(new ReactorCycle.Part[]{fuel,new ReactorCycle.Part(ReactorCycle.Profile.plating(1000,5),0,0)},1,1000);
        h.assertTrue(plated.radius()==22&&Math.abs(plated.heatEffectModifier()-.95)<.0001,"IC2 plating factors lost");
        h.assertTrue(ReactorBlastProfile.calculate(packed,0,45).disabled()&&ReactorBlastProfile.calculate(packed,1,0).disabled(),"Zero configuration does not disable blast");
        h.succeed();
    }
    @GameTest(template="reactor_loop",batch="reactor_blast",timeoutTicks=40)
    public static void realOverheatedCycleUsesConfiguredScaleAndAdmitsOneBlast(GameTestHelper h){
        Object multiplier=config("REACTOR_EXPLOSION_MULTIPLIER");Object old=get(multiplier);var zones=new HashSet<>(RadiationState.get(h.getLevel()).zones.keySet());
        try{
            set(multiplier,2.0);BlockEntity entity=overheat(h,"uranium_quad");
            h.assertTrue(latch(entity).trigger().powerMillis()==24000,"Actual cycle ignored fuel or coefficient");
            tick(h,entity);
            h.assertTrue(h.getBlockState(POS).isAir()&&latch(entity).state()==ReactorAccidentLatch.State.CLOSED,"Reactor source did not commit");
            var added=RadiationState.get(h.getLevel()).zones.entrySet().stream().filter(e->!zones.contains(e.getKey())).toList();
            h.assertTrue(added.size()==1&&added.getFirst().getValue().radius()==48,"Committed blast radius differs from configured profile");
            h.assertTrue(((IReactorController)entity).getItemHandler().getStackInSlot(0).isEmpty(),"Destroyed reactor scattered/retained active fuel");
            tick(h,entity);
            h.assertTrue(RadiationState.get(h.getLevel()).zones.size()==zones.size()+1,"Removed reactor submitted a second blast");
        }finally{set(multiplier,old);cleanup(h,zones);}
        h.succeed();
    }
    @GameTest(template="reactor_loop",batch="reactor_blast",timeoutTicks=40)
    public static void fullQueueRetainsTheReactorAndOvershootSurvivesSaveReload(GameTestHelper h){
        var zones=new HashSet<>(RadiationState.get(h.getLevel()).zones.keySet());
        Class<?> scheduler=implementation("reactor.ExplosionWorkScheduler");Object category=new Object();
        Object multiplier=config("REACTOR_EXPLOSION_MULTIPLIER"),old=get(multiplier);
        try{
            set(multiplier,1.0);BlockEntity entity=overheat(h,"mox_simple");
            while((boolean)ArmoryServiceGameTests.call(scheduler,"hasCapacity",h.getLevel()))
                ArmoryServiceGameTests.call(scheduler,"trySubmit",h.getLevel(),entity.getBlockPos(),category,new Object(),(BooleanSupplier)()->false);
            tick(h,entity);
            h.assertTrue(h.getBlockEntity(POS)==entity&&latch(entity).state()==ReactorAccidentLatch.State.PENDING,"Queue rejection consumed reactor");
            h.assertTrue(!((IReactorController)entity).getItemHandler().getStackInSlot(0).isEmpty(),"Queue rejection lost fuel");
            var saved=entity.saveWithoutMetadata(h.getLevel().registryAccess());
            set(multiplier,3.0);
            var restored=entity.getType().create(entity.getBlockPos(),entity.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(saved,h.getLevel().registryAccess());
            h.assertTrue(latch(restored).state()==ReactorAccidentLatch.State.PENDING&&latch(restored).trigger().powerMillis()==6000,
                "Clamped stored heat invalidated the saved overshoot or changed frozen blast strength");
            for(int version=1;version<=2;version++){
                var legacy=saved.copy();var accident=legacy.getCompound("scex_reactor").getCompound("accident");
                accident.putInt("version",version);accident.remove("power_millis");
                if(version==2){accident.putInt("explosive_cells",1);accident.putInt("containment_plates",0);}
                var migrated=entity.getType().create(entity.getBlockPos(),entity.getBlockState());migrated.setLevel(h.getLevel());
                migrated.loadWithComponents(legacy,h.getLevel().registryAccess());
                h.assertTrue(latch(migrated).state()==ReactorAccidentLatch.State.PENDING
                    &&latch(migrated).trigger().powerMillis()==(version==1?10000:5000),"Legacy accident v"+version+" did not retain its committed strength");
            }
            ArmoryServiceGameTests.call(scheduler,"clear",h.getLevel(),category);tick(h,entity);
            var added=RadiationState.get(h.getLevel()).zones.entrySet().stream().filter(e->!zones.contains(e.getKey())).toList();
            h.assertTrue(h.getBlockState(POS).isAir()&&added.size()==1&&added.getFirst().getValue().radius()==12,"Retry did not use committed strength exactly once");
        }finally{ArmoryServiceGameTests.call(scheduler,"clear",h.getLevel(),category);set(multiplier,old);cleanup(h,zones);}
        h.succeed();
    }
    @GameTest(template="reactor_loop",batch="reactor_blast",timeoutTicks=40)
    public static void disabledNuclearTerrainKeepsTheOrdinaryLocalMachineAccident(GameTestHelper h){
        Object enable=config("ENABLE_NUCLEAR_EXPLOSION");Object old=get(enable);var zones=new HashSet<>(RadiationState.get(h.getLevel()).zones.keySet());
        try{
            set(enable,false);h.setBlock(POS.west(2),Blocks.BRICKS);BlockEntity entity=overheat(h,"uranium_simple");
            h.assertTrue(latch(entity).trigger().effect()==ReactorAccidentLatch.Effect.LOCAL_MACHINE,"Nuclear disable ignored");tick(h,entity);
            h.assertTrue(h.getBlockState(POS).isAir()&&h.getBlockState(POS.west(2)).is(Blocks.BRICKS),"Local machine accident damaged nearby terrain");
            h.assertTrue(RadiationState.get(h.getLevel()).zones.keySet().equals(zones),"Disabled nuclear mode created fallout");
        }finally{set(enable,old);cleanup(h,zones);}
        h.succeed();
    }
    @GameTest(template="reactor_loop",batch="reactor_blast",timeoutTicks=40)
    public static void zeroCoefficientDefersAlreadyArmedAccidentsWithoutConsumingThem(GameTestHelper h){
        Object multiplier=config("REACTOR_EXPLOSION_MULTIPLIER"),old=get(multiplier);var zones=new HashSet<>(RadiationState.get(h.getLevel()).zones.keySet());
        try{
            set(multiplier,1.0);BlockEntity entity=overheat(h,"uranium_simple");set(multiplier,0.0);tick(h,entity);
            h.assertTrue(h.getBlockEntity(POS)==entity&&latch(entity).state()==ReactorAccidentLatch.State.PENDING,"Zero coefficient consumed armed source");
            h.assertTrue(RadiationState.get(h.getLevel()).zones.keySet().equals(zones),"Zero coefficient submitted a nuclear effect");
        }finally{set(multiplier,old);h.setBlock(POS,Blocks.AIR);cleanup(h,zones);}
        h.succeed();
    }
    @GameTest(template="reactor_loop",batch="reactor_blast",timeoutTicks=40)
    public static void reactorEllipsoidDoesNotDamageEntitiesOutsideItsVerticalRange(GameTestHelper h){
        Vec3 center=Vec3.atCenterOf(h.absolutePos(POS));
        ItemEntity inside=new ItemEntity(h.getLevel(),center.x+3,center.y,center.z,new ItemStack(Items.DIAMOND));
        ItemEntity above=new ItemEntity(h.getLevel(),center.x,center.y+3,center.z,new ItemStack(Items.DIAMOND));
        h.getLevel().addFreshEntity(inside);h.getLevel().addFreshEntity(above);
        try{
            Object task=implementation("blockentity.reactor.NukeExplosionTask").getConstructor(ServerLevel.class,Vec3.class,float.class,int.class,int.class,List.class)
                .newInstance(h.getLevel(),center,36F,6,2,List.of(inside,above));
            int steps=0;while(!(boolean)ArmoryServiceGameTests.call(task,"isComplete")&&steps++<100000)ArmoryServiceGameTests.call(task,"advance");
            h.assertTrue(steps<100000&&inside.isRemoved()&&!above.isRemoved(),"Reactor blast ignored its flattened vertical shape");
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        finally{inside.discard();above.discard();}
        h.succeed();
    }
}
