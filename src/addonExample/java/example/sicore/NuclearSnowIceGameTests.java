package example.sicore;

import com.miophas.singularity_iteration.core.runtime.radiation.NuclearFalloutState;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class NuclearSnowIceGameTests {
    private static Class<?> implementation(String name){return CommonConfigGameTests.implementation("blockentity.reactor."+name);}
    private static void finish(GameTestHelper h,NuclearFalloutState.Region region,LevelChunk chunk){
        try{
            Object task=implementation("NuclearFalloutChunkTask").getConstructor(ServerLevel.class,NuclearFalloutState.Region.class,LevelChunk.class,NuclearFalloutState.class,BooleanSupplier.class)
                .newInstance(h.getLevel(),region,chunk,null,(BooleanSupplier)()->true);
            int steps=0;while(!(boolean)ArmoryServiceGameTests.call(task,"getAsBoolean")&&steps++<1_000_000){}
            h.assertTrue(steps<1_000_000,"Snow/ice cleanup exceeded bounded build-height work");
        }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private static List<BlockState> frozen(){return List.of(Blocks.SNOW.defaultBlockState(),Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS,8),
        Blocks.SNOW_BLOCK.defaultBlockState(),Blocks.POWDER_SNOW.defaultBlockState(),Blocks.ICE.defaultBlockState(),Blocks.FROSTED_ICE.defaultBlockState(),
        Blocks.PACKED_ICE.defaultBlockState(),Blocks.BLUE_ICE.defaultBlockState());}

    @GameTest(template="reactor_loop",batch="nuclear_snow_ice",timeoutTicks=40)
    public static void allSnowAndIceTypesMeltAndUncoverConvertibleGround(GameTestHelper h){
        BlockPos center=h.absolutePos(new BlockPos(6,3,6));var region=new NuclearFalloutState.Region(Vec3.atCenterOf(center),8,8,0);
        var states=frozen();
        for(int i=0;i<states.size();i++){
            var at=center.offset(i-4,0,0);h.getLevel().setBlock(at.below(),Blocks.GRASS_BLOCK.defaultBlockState(),2);
            h.getLevel().setBlock(at,states.get(i),18);
        }
        for(int x=(center.getX()-4)>>4;x<=(center.getX()+3)>>4;x++)finish(h,region,h.getLevel().getChunk(x,center.getZ()>>4));
        for(int i=0;i<states.size();i++){
            var at=center.offset(i-4,0,0);
            h.assertTrue(i<2?h.getLevel().getBlockState(at).isAir():h.getLevel().getBlockState(at).is(Blocks.WATER),"Snow/ice melt result wrong: "+states.get(i));
            h.assertTrue(!h.getLevel().getBlockState(at.below()).is(Blocks.GRASS_BLOCK),"Frozen cover prevented soil conversion");
        }
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(9)).isEmpty(),"Melting created item drops");
        // A lower section containing ice still needs inspection after solid cover stops soil conversion.
        var cover=center.above(24);h.getLevel().setBlock(cover,Blocks.BRICKS.defaultBlockState(),2);
        h.getLevel().setBlock(cover.below(16),Blocks.BLUE_ICE.defaultBlockState(),2);
        finish(h,new NuclearFalloutState.Region(Vec3.atCenterOf(cover),37,37,0),h.getLevel().getChunkAt(cover));
        h.assertTrue(h.getLevel().getBlockState(cover).is(Blocks.BRICKS)&&h.getLevel().getBlockState(cover.below(16)).is(Blocks.WATER),"Lower ice section was skipped after non-frozen cover");h.succeed();
    }

    @GameTest(template="reactor_loop",batch="nuclear_snow_ice",timeoutTicks=40)
    public static void meltingFollowsTheCircularAndVerticalBoundary(GameTestHelper h){
        BlockPos center=h.absolutePos(new BlockPos(6,3,6));var region=new NuclearFalloutState.Region(Vec3.atCenterOf(center),4,4,0);
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){
            h.getLevel().setBlock(center.offset(x,0,z),Blocks.PACKED_ICE.defaultBlockState(),2);
            h.getLevel().setBlock(center.offset(x,1,z),Blocks.SNOW.defaultBlockState(),18);
        }
        h.getLevel().setBlock(center.above(5),Blocks.BLUE_ICE.defaultBlockState(),2);
        for(int x=(center.getX()-6)>>4;x<=(center.getX()+6)>>4;x++)for(int z=(center.getZ()-6)>>4;z<=(center.getZ()+6)>>4;z++)finish(h,region,h.getLevel().getChunk(x,z));
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){
            boolean inside=x*x+z*z<=36;var at=center.offset(x,0,z);
            h.assertTrue(h.getLevel().getBlockState(at).is(inside?Blocks.WATER:Blocks.PACKED_ICE),"Ice melting escaped circular boundary");
            h.assertTrue(inside?h.getLevel().getBlockState(at.above()).isAir():h.getLevel().getBlockState(at.above()).is(Blocks.SNOW),"Snow melting escaped circular boundary");
        }
        h.assertTrue(h.getLevel().getBlockState(center.above(5)).is(Blocks.BLUE_ICE),"Melting escaped vertical blast range");h.succeed();
    }

    @GameTest(template="reactor_loop",batch="nuclear_snow_ice",timeoutTicks=40)
    public static void pressureKeepsItsMeltWaterAndNetherHeatEvaporatesIt(GameTestHelper h)throws Exception{
        BlockPos at=h.absolutePos(new BlockPos(6,3,6));h.getLevel().setBlock(at,Blocks.BLUE_ICE.defaultBlockState(),2);
        h.getLevel().setBlock(at.above(),Blocks.SNOW.defaultBlockState(),18);
        Object pressure=implementation("NukeExplosionTask").getConstructor(ServerLevel.class,Vec3.class,float.class,int.class,List.class)
            .newInstance(h.getLevel(),Vec3.atCenterOf(at),400F,2,List.of());
        int steps=0;while(!(boolean)ArmoryServiceGameTests.call(pressure,"isComplete")&&steps++<100_000)ArmoryServiceGameTests.call(pressure,"advance");
        h.assertTrue(steps<100_000&&h.getLevel().getBlockState(at).is(Blocks.WATER),"Overlapping pressure rays erased freshly melted water");
        h.assertTrue(h.getLevel().getBlockState(at.above()).isAir()
            &&h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(at).inflate(4)).isEmpty(),"Pressure melting dropped snow items or left frozen cover");
        var nether=h.getLevel().getServer().getLevel(Level.NETHER);h.assertTrue(nether!=null&&nether.dimensionType().ultraWarm(),"Nether test dimension unavailable");
        var heat=CommonConfigGameTests.implementation("reactor.NuclearThermalEffects");
        for(var state:frozen())h.assertTrue(((BlockState)ArmoryServiceGameTests.call(heat,"melted",nether,state)).isAir(),"Nuclear melting created water in an ultra-warm dimension");h.succeed();
    }

    @GameTest(template="reactor_loop",batch="nuclear_snow_ice_deferred",timeoutTicks=150)
    public static void snowAndIceInMissingChunksResumeAfterSavedFootprintReload(GameTestHelper h){
        BlockPos at=new BlockPos(6_000_008,-57,6_000_008);var manager=implementation("NuclearFalloutManager");UUID id=UUID.randomUUID();
        h.assertTrue(h.getLevel().getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4)==null,"Deferred frozen chunk already loaded");
        ArmoryServiceGameTests.call(manager,"register",h.getLevel(),id,Vec3.atCenterOf(at),4,4);
        var data=NuclearFalloutState.get(h.getLevel());var reloaded=NuclearFalloutState.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        data.regions.put(id,reloaded.regions.get(id));
        h.getLevel().setChunkForced(at.getX()>>4,at.getZ()>>4,true);
        h.getLevel().setBlock(at.below(),Blocks.GRASS_BLOCK.defaultBlockState(),2);h.getLevel().setBlock(at,Blocks.ICE.defaultBlockState(),2);
        h.getLevel().setBlock(at.above(),Blocks.SNOW.defaultBlockState(),18);
        ArmoryServiceGameTests.call(manager,"loaded",h.getLevel(),net.minecraft.world.level.ChunkPos.asLong(at.getX()>>4,at.getZ()>>4));
        h.runAfterDelay(40,()->{
            try{
                h.assertTrue(h.getLevel().getBlockState(at).is(Blocks.WATER)&&h.getLevel().getBlockState(at.above()).isAir(),"Loaded frozen chunk did not resume melting");
                h.assertTrue(!h.getLevel().getBlockState(at.below()).is(Blocks.GRASS_BLOCK),"Deferred ice still masks unconverted ground");h.succeed();
            }finally{ArmoryServiceGameTests.call(manager,"remove",h.getLevel(),id);h.getLevel().setChunkForced(at.getX()>>4,at.getZ()>>4,false);}
        });
    }
}
