package example.sicore;

import com.miophas.singularity_iteration.core.runtime.radiation.NuclearFalloutState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import java.util.UUID;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class NuclearFalloutRoutingGameTests {
    private static Class<?> manager() { return CommonConfigGameTests.implementation("blockentity.reactor.NuclearFalloutManager"); }
    private static UUID register(GameTestHelper h, BlockPos center) {
        UUID id = UUID.randomUUID();
        ArmoryServiceGameTests.call(manager(), "register", h.getLevel(), id, Vec3.atCenterOf(center), 2000, 2000);
        return id;
    }
    private static void loaded(GameTestHelper h, long key) {
        ArmoryServiceGameTests.call(manager(), "loaded", h.getLevel(), key);
    }
    private static void remove(GameTestHelper h, UUID id) {
        ArmoryServiceGameTests.call(manager(), "remove", h.getLevel(), id);
    }

    @GameTest(template="reactor_loop", batch="fallout_routing", timeoutTicks=200)
    public static void unavailableFrontChunksCannotStarveFarForwardAndSidewaysExploration(GameTestHelper h) {
        BlockPos center = new BlockPos(3_000_008, -57, 3_000_008);
        UUID id = register(h, center);
        var region = NuclearFalloutState.get(h.getLevel()).regions.get(id);
        var targets = List.of(center.offset(1856, 0, 0), center.offset(1536, 0, 1536), center.offset(1536, 0, -1536));
        // Loaded-event addresses may precede FULL availability or remain until deferred unload completes.
        for (int i=0; i<64; i++) {
            int x=(center.getX()>>4)+i, z=center.getZ()>>4;
            h.assertTrue(h.getLevel().getChunkSource().getChunkNow(x,z)==null, "Unavailable prefix already loaded");
            loaded(h,ChunkPos.asLong(x,z));
        }
        for (BlockPos target:targets) {
            // Model chunks kept in view by an exploring player; getChunk alone only
            // obtains a transient ticket that can expire before the fair retry reaches it.
            h.getLevel().setChunkForced(target.getX()>>4,target.getZ()>>4,true);
            h.getLevel().getChunk(target.getX()>>4,target.getZ()>>4);
            h.getLevel().setBlock(target,Blocks.GRASS_BLOCK.defaultBlockState(),2);
            h.getLevel().setBlock(target.above(),Blocks.OAK_LEAVES.defaultBlockState(),2);
            loaded(h,ChunkPos.asLong(target.getX()>>4,target.getZ()>>4));
        }
        ArmoryServiceGameTests.call(manager(),"ready",h.getLevel(),id);
        ArmoryServiceGameTests.call(manager(),"tick",new ServerTickEvent.Pre(()->true,h.getLevel().getServer()));
        h.runAfterDelay(100,()->{
            try {
                for (BlockPos target:targets) {
                    h.assertTrue(!h.getLevel().getBlockState(target).is(Blocks.GRASS_BLOCK)
                        &&h.getLevel().getBlockState(target.above()).isAir(),"Forward/sideways chunk starved behind unavailable queue prefix: "+target
                        +" progress="+region.columns.getOrDefault(ChunkPos.asLong(target.getX()>>4,target.getZ()>>4),0)
                        +" available="+(h.getLevel().getChunkSource().getChunkNow(target.getX()>>4,target.getZ()>>4)!=null));
                    h.assertTrue(region.columns.getOrDefault(ChunkPos.asLong(target.getX()>>4,target.getZ()>>4),0)==256,"Exploration chunk did not finish all columns");
                }
                h.assertTrue(h.getLevel().getChunkSource().getChunkNow(center.getX()>>4,center.getZ()>>4)==null,"Retry queue force-loaded unavailable terrain");
                h.succeed();
            } finally {
                remove(h,id);
                for(BlockPos target:targets)h.getLevel().setChunkForced(target.getX()>>4,target.getZ()>>4,false);
            }
        });
    }

    @GameTest(template="reactor_loop", batch="fallout_routing", timeoutTicks=40)
    public static void maximumChargeHandsOffFalloutWithoutScanningTheEntireChunkRectangle(GameTestHelper h) throws Exception {
        Object nuke=CommonConfigGameTests.nuke(h,new BlockPos(6,3,6));
        for(int slot=1;slot<8;slot++)ArmoryServiceGameTests.call(nuke,"setItem",slot,new ItemStack(CommonConfigGameTests.block("reactor/block_ic_tnt"),64));
        ArmoryServiceGameTests.call(nuke,"setItem",8,new ItemStack(BuiltInRegistries.ITEM.get(CommonConfigGameTests.id("resource/item_plutonium")),64));
        float yield=(float)ArmoryServiceGameTests.call(nuke,"calculateBaseExplosionPower");
        h.assertTrue(yield==104192,"Eight stacks of industrial TNT plus plutonium no longer match their original yield");
        BlockPos center=new BlockPos(2_400_008,-57,2_400_008);
        UUID id=register(h,center);
        try {
            Object task=CommonConfigGameTests.implementation("blockentity.reactor.NukeExplosionTask")
                .getConstructor(ServerLevel.class,Vec3.class,float.class,int.class,List.class)
                .newInstance(h.getLevel(),Vec3.atCenterOf(center),yield,2000,List.of());
            ArmoryServiceGameTests.call(task,"bindFallout",id);
            int steps=0;
            while(!(boolean)ArmoryServiceGameTests.call(task,"isComplete")&&steps++<25000)ArmoryServiceGameTests.call(task,"advance");
            h.assertTrue(steps<25000,"Persistent fallout handoff still scans the full radius-3000 chunk rectangle");
            var region=NuclearFalloutState.get(h.getLevel()).regions.get(id);
            h.assertTrue(region.ready&&region.columns.isEmpty(),"Pressure completion did not open deferred chunk cleanup");
            h.assertTrue(h.getLevel().getChunkSource().getChunkNow(center.getX()>>4,center.getZ()>>4)==null,"Pressure handoff force-loaded the center");
            h.succeed();
        } finally { remove(h,id); }
    }
}
