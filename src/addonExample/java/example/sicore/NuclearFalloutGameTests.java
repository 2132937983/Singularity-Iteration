package example.sicore;

import com.miophas.singularity_iteration.core.runtime.radiation.NuclearFalloutState;
import com.miophas.singularity_iteration.core.runtime.radiation.FalloutVisualRange;
import com.miophas.singularity_iteration.core.runtime.radiation.RadiationState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class NuclearFalloutGameTests {
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("mio_icif",path);}
    private static Class<?> implementation(String name){
        try{
            Block block=BuiltInRegistries.BLOCK.get(id("reactor/block_reactor_nuke"));
            var entity=((EntityBlock)block).newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            return Class.forName(entity.getClass().getPackageName()+"."+name);
        }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private static Class<?> scheduler(){
        try{return Class.forName(implementation("NukeExplosionTask").getPackageName().replace("blockentity.reactor","reactor")+".ExplosionWorkScheduler");}
        catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private static UUID region(GameTestHelper h,BlockPos center,int radius,int radiusY){
        UUID key=(UUID)ArmoryServiceGameTests.call(implementation("NukeRadiationZoneManager"),"createRadiationZone",h.getLevel(),center,radius,1000F);
        ArmoryServiceGameTests.call(implementation("NuclearFalloutManager"),"register",h.getLevel(),key,Vec3.atCenterOf(center),radius,radiusY);return key;
    }
    private static void cleanup(GameTestHelper h,UUID key){ArmoryServiceGameTests.call(implementation("NukeRadiationZoneManager"),"removeRadiationZone",key);}
    private static void pump(GameTestHelper h){ArmoryServiceGameTests.call(implementation("NuclearFalloutManager"),"tick",new ServerTickEvent.Pre(()->true,h.getLevel().getServer()));}
    private static void finishChunk(GameTestHelper h,NuclearFalloutState.Region region,LevelChunk chunk){
        try{
            Object task=implementation("NuclearFalloutChunkTask").getConstructor(ServerLevel.class,NuclearFalloutState.Region.class,LevelChunk.class,NuclearFalloutState.class,BooleanSupplier.class)
                .newInstance(h.getLevel(),region,chunk,null,(BooleanSupplier)()->true);
            int steps=0;while(!(boolean)ArmoryServiceGameTests.call(task,"getAsBoolean")&&steps++<1200000){}
            h.assertTrue(steps<1200000,"Chunk fallout exceeded bounded build-height work");
        }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    @GameTest(template="reactor_loop",batch="fallout_geometry",timeoutTicks=40)
    public static void highYieldAndNegativeCoordinatesUseCircularGeometry(GameTestHelper h){
        for(int radius:new int[]{4,37,2000}){
            var region=new NuclearFalloutState.Region(new Vec3(-31.5,3,-47.5),radius,radius,0);
            int extent=(int)Math.ceil(radius*1.5);
            h.assertTrue(region.containsColumn(-32+extent,-48)==(extent<=radius*1.5),"Axis boundary wrong");
            h.assertTrue(!region.containsColumn(-32+extent,-48+extent),"Square corner accepted as circular fallout");
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){
                int cx=-2+x*extent/16,cz=-3+z*extent/16;
                boolean expected=false;
                for(int bx=0;bx<16;bx++)for(int bz=0;bz<16;bz++)expected|=region.containsColumn(cx*16+bx,cz*16+bz);
                h.assertTrue(region.intersectsChunk(cx,cz)==expected,"Chunk intersection differs from actual circular columns");
            }
        }h.succeed();
    }
    @GameTest(template="reactor_loop",batch="fallout_geometry",timeoutTicks=40)
    public static void allConvertibleColumnsAcrossChunkEdgesFormACircle(GameTestHelper h){
        BlockPos center=h.absolutePos(new BlockPos(6,3,6));var region=new NuclearFalloutState.Region(Vec3.atCenterOf(center),4,4,0);
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){
            h.getLevel().setBlock(center.offset(x,0,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
            h.getLevel().setBlock(center.offset(x,1,z),Blocks.SHORT_GRASS.defaultBlockState(),2);
        }
        for(int x=(center.getX()-6)>>4;x<=(center.getX()+6)>>4;x++)for(int z=(center.getZ()-6)>>4;z<=(center.getZ()+6)>>4;z++)
            finishChunk(h,region,h.getLevel().getChunk(x,z));
        // Resolve the actual registry name through the resulting axis column; every inside column must match it.
        Block converted=h.getLevel().getBlockState(center).getBlock();
        h.assertTrue(converted!=Blocks.GRASS_BLOCK&&converted!=Blocks.AIR,"Ground did not convert");
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){
            boolean inside=x*x+z*z<=36;BlockPos at=center.offset(x,0,z);
            h.assertTrue(h.getLevel().getBlockState(at).is(inside?converted:Blocks.GRASS_BLOCK),"Circular soil boundary wrong at "+x+","+z);
            h.assertTrue(inside?h.getLevel().getBlockState(at.above()).isAir():h.getLevel().getBlockState(at.above()).is(Blocks.SHORT_GRASS),"Vegetation crosses circular boundary");
        }h.succeed();
    }
    @GameTest(template="reactor_loop",batch="fallout_geometry",timeoutTicks=40)
    public static void leavesFlowersAndWaterloggedPlantsAreRemovedWithoutDestroyingSolidCover(GameTestHelper h){
        BlockPos center=h.absolutePos(new BlockPos(6,3,6));var region=new NuclearFalloutState.Region(Vec3.atCenterOf(center),4,4,0);
        h.getLevel().setBlock(center.below(),Blocks.GRASS_BLOCK.defaultBlockState(),2);
        h.getLevel().setBlock(center,Blocks.DANDELION.defaultBlockState(),2);
        h.getLevel().setBlock(center.above(),Blocks.OAK_LEAVES.defaultBlockState(),2);
        h.getLevel().setBlock(center.above(2),Blocks.BRICKS.defaultBlockState(),2);
        h.getLevel().setBlock(center.above(3),Blocks.OAK_LEAVES.defaultBlockState(),2);
        h.getLevel().setBlock(center.east(),Blocks.SEAGRASS.defaultBlockState(),2);
        h.getLevel().setBlock(center.west(),Blocks.TALL_GRASS.defaultBlockState(),18);
        h.getLevel().setBlock(center.west().above(),Blocks.TALL_GRASS.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),18);
        finishChunk(h,region,h.getLevel().getChunkAt(center));
        for(int y:new int[]{0,1,3})h.assertTrue(h.getLevel().getBlockState(center.above(y)).isAir(),"Vegetation survived inside vertical fallout range");
        h.assertTrue(h.getLevel().getBlockState(center.above(2)).is(Blocks.BRICKS),"Fallout removed non-vegetation cover");
        h.assertTrue(h.getLevel().getBlockState(center.east()).is(Blocks.WATER),"Removed waterlogged plant deleted its water");
        h.assertTrue(h.getLevel().getBlockState(center.west()).isAir()&&h.getLevel().getBlockState(center.west().above()).isAir(),"Double-height vegetation survived fallout");
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(4)).isEmpty(),"Vegetation cleanup created item drops");h.succeed();
    }
    @GameTest(template="reactor_loop",batch="fallout_geometry",timeoutTicks=40)
    public static void radiationCannotReachUnprocessedColumnsOrExceedTheReactorVerticalShape(GameTestHelper h){
        BlockPos center=h.absolutePos(new BlockPos(6,3,6));UUID key=region(h,center,8,2);
        try{
            var manager=implementation("NukeRadiationZoneManager");var region=NuclearFalloutState.get(h.getLevel()).regions.get(key);
            h.assertTrue(!(boolean)ArmoryServiceGameTests.call(manager,"isInRadiationZone",h.getLevel(),center),"Untouched column immediately receives full-zone radiation");
            int column=(center.getZ()&15)*16+(center.getX()&15);long chunk=ChunkPos.asLong(center.getX()>>4,center.getZ()>>4);
            region.columns.put(chunk,column);
            h.assertTrue(!(boolean)ArmoryServiceGameTests.call(manager,"isInRadiationZone",h.getLevel(),center),"Incomplete current column receives radiation");
            region.columns.put(chunk,column+1);
            h.assertTrue((float)ArmoryServiceGameTests.call(manager,"getRadiationIntensity",h.getLevel(),center)==1,"Completed center column lost radiation");
            h.assertTrue(!(boolean)ArmoryServiceGameTests.call(manager,"isInRadiationZone",h.getLevel(),center.above(3)),"Reactor fallout used the bomb's taller sphere");
        }finally{cleanup(h,key);}h.succeed();
    }
    @GameTest(template="reactor_loop",batch="fallout_deferred",timeoutTicks=200)
    public static void missingChunksResumeAfterSaveReloadAndDoNotReplayCompletedColumns(GameTestHelper h){
        BlockPos at=new BlockPos(2000000,h.absolutePos(new BlockPos(0,1,0)).getY(),2000000);
        h.assertTrue(h.getLevel().getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4)==null,"Deferred test chunk already loaded");
        UUID key=region(h,at,4,4);
        try{
            Object task=implementation("NukeExplosionTask").getConstructor(ServerLevel.class,Vec3.class,float.class,int.class,List.class)
                .newInstance(h.getLevel(),Vec3.atCenterOf(at),1F,4,List.of());
            ArmoryServiceGameTests.call(task,"bindFallout",key);
            int steps=0;while(!(boolean)ArmoryServiceGameTests.call(task,"isComplete")&&steps++<100000)ArmoryServiceGameTests.call(task,"advance");
            h.assertTrue(steps<100000&&h.getLevel().getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4)==null,"Initial fallout force-loaded a missing chunk");
            var state=NuclearFalloutState.get(h.getLevel());var saved=state.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
            var loaded=NuclearFalloutState.load(saved,h.getLevel().registryAccess());state.regions.put(key,loaded.regions.get(key));
            h.assertTrue(state.regions.get(key).ready&&state.regions.get(key).columns.isEmpty(),"Missing footprint lost across save reload");
            h.getLevel().getChunk(at.getX()>>4,at.getZ()>>4);
            h.getLevel().setBlock(at,Blocks.GRASS_BLOCK.defaultBlockState(),2);h.getLevel().setBlock(at.above(),Blocks.OAK_LEAVES.defaultBlockState(),2);
            ArmoryServiceGameTests.call(implementation("NuclearFalloutManager"),"loaded",h.getLevel(),ChunkPos.asLong(at.getX()>>4,at.getZ()>>4));pump(h);
        }catch(ReflectiveOperationException|RuntimeException failure){cleanup(h,key);throw new AssertionError(failure);}
        h.runAfterDelay(30,()->{
            try{
                h.assertTrue(!h.getLevel().getBlockState(at).is(Blocks.GRASS_BLOCK)&&h.getLevel().getBlockState(at.above()).isAir(),"Loaded chunk never resumed fallout");
                h.assertTrue((float)ArmoryServiceGameTests.call(implementation("NukeRadiationZoneManager"),"getRadiationIntensity",h.getLevel(),at)>0,"Completed loaded column is not radioactive");
                h.getLevel().setBlock(at.above(),Blocks.SHORT_GRASS.defaultBlockState(),2);
                ArmoryServiceGameTests.call(implementation("NuclearFalloutManager"),"loaded",h.getLevel(),ChunkPos.asLong(at.getX()>>4,at.getZ()>>4));pump(h);
                h.runAfterDelay(10,()->{try{h.assertTrue(h.getLevel().getBlockState(at.above()).is(Blocks.SHORT_GRASS),"Completed columns were destructively replayed");h.succeed();}finally{cleanup(h,key);}});
            }catch(RuntimeException failure){cleanup(h,key);throw failure;}
        });
    }
    @GameTest(template="reactor_loop",batch="fallout_backlog",timeoutTicks=150)
    public static void deferredFalloutRetainsProgressWhenTheSharedQueueIsFull(GameTestHelper h){
        BlockPos at=h.absolutePos(new BlockPos(6,3,6));UUID key=region(h,at,4,4);Object category=new Object();Class<?> scheduler=scheduler();
        h.getLevel().setBlock(at,Blocks.GRASS_BLOCK.defaultBlockState(),2);h.getLevel().setBlock(at.above(),Blocks.OAK_LEAVES.defaultBlockState(),2);
        ArmoryServiceGameTests.call(implementation("NuclearFalloutManager"),"ready",h.getLevel(),key);
        ArmoryServiceGameTests.call(implementation("NuclearFalloutManager"),"loaded",h.getLevel(),ChunkPos.asLong(at.getX()>>4,at.getZ()>>4));
        try{
            while((boolean)ArmoryServiceGameTests.call(scheduler,"hasCapacity",h.getLevel()))ArmoryServiceGameTests.call(scheduler,"trySubmit",h.getLevel(),at,category,new Object(),(BooleanSupplier)()->false);
            pump(h);h.assertTrue(NuclearFalloutState.get(h.getLevel()).regions.get(key).columns.isEmpty(),"Rejected cleanup consumed pending columns");
        }finally{ArmoryServiceGameTests.call(scheduler,"clear",h.getLevel(),category);}
        pump(h);h.runAfterDelay(30,()->{try{h.assertTrue(!h.getLevel().getBlockState(at).is(Blocks.GRASS_BLOCK)&&h.getLevel().getBlockState(at.above()).isAir(),"Full-queue fallout did not retry");h.succeed();}finally{cleanup(h,key);}});
    }
    @GameTest(template="reactor_loop",batch="fallout_expired",timeoutTicks=150)
    public static void radiationExpiryDoesNotDiscardPendingTerrainConversion(GameTestHelper h){
        BlockPos at=h.absolutePos(new BlockPos(6,3,6));UUID key=region(h,at,4,4);
        var original=NuclearFalloutState.get(h.getLevel()).regions.get(key);
        var expired=new NuclearFalloutState.Region(original.center,4,4,System.currentTimeMillis()-RadiationState.DURATION-1000);
        NuclearFalloutState.get(h.getLevel()).regions.put(key,expired);
        RadiationState.get(h.getLevel()).zones.remove(key); // Natural expiry removes the dose, not pending terrain work.
        h.getLevel().setBlock(at,Blocks.GRASS_BLOCK.defaultBlockState(),2);h.getLevel().setBlock(at.above(),Blocks.OAK_LEAVES.defaultBlockState(),2);
        ArmoryServiceGameTests.call(implementation("NuclearFalloutManager"),"ready",h.getLevel(),key);
        ArmoryServiceGameTests.call(implementation("NuclearFalloutManager"),"loaded",h.getLevel(),ChunkPos.asLong(at.getX()>>4,at.getZ()>>4));pump(h);
        h.runAfterDelay(30,()->{try{
            h.assertTrue(!h.getLevel().getBlockState(at).is(Blocks.GRASS_BLOCK)&&h.getLevel().getBlockState(at.above()).isAir(),"Expired dose discarded pending physical fallout");
            h.assertTrue((float)ArmoryServiceGameTests.call(implementation("NukeRadiationZoneManager"),"getRadiationIntensity",h.getLevel(),at)==0,"Terrain resume revived an expired dose zone");h.succeed();
        }finally{cleanup(h,key);}});
    }
    @GameTest(template="reactor_loop",batch="fallout_geometry",timeoutTicks=40)
    public static void localFogCannotReachCleanGroundDozensOfBlocksAway(GameTestHelper h){
        h.assertTrue(FalloutVisualRange.intensity(0,1.5,0)>.8,"Standing on contaminated ground has no fog");
        h.assertTrue(FalloutVisualRange.intensity(3,0,0)==0&&FalloutVisualRange.intensity(64,0,0)==0,"Fog leaks beyond local columns");
        h.assertTrue(FalloutVisualRange.intensity(0,16,0)==0&&FalloutVisualRange.intensity(Double.NaN,0,0)==0,"Fog accepts distant or invalid coordinates");h.succeed();
    }
}
