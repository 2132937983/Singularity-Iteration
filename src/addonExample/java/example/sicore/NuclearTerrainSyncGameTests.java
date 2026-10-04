package example.sicore;

import com.miophas.singularity_iteration.core.runtime.radiation.NuclearFalloutState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class NuclearTerrainSyncGameTests {
    private static Class<?> updates(){return CommonConfigGameTests.implementation("reactor.NuclearTerrainUpdates");}
    private static Class<?> task(String name){return CommonConfigGameTests.implementation("blockentity.reactor."+name);}
    private static void flush(GameTestHelper h){ArmoryServiceGameTests.call(updates(),"flush",h.getLevel().getServer(),1_000_000_000L);}

    private static void border(GameTestHelper h,int coordinate,java.util.function.Consumer<LevelChunk> test){
        var position=new ChunkPos(coordinate>>4,coordinate>>4);
        h.getLevel().getChunkSource().addRegionTicket(TicketType.FORCED,position,0,position);
        var chunk=h.getLevel().getChunk(position.x,position.z);
        h.runAfterDelay(5,()->{
            try{
                h.assertTrue(chunk.getFullStatus()==FullChunkStatus.FULL,"Test must use a visible FULL chunk outside block ticking: "+chunk.getFullStatus());
                test.accept(chunk);h.succeed();
            }finally{h.getLevel().getChunkSource().removeRegionTicket(TicketType.FORCED,position,0,position);}
        });
    }
    private static void observed(GameTestHelper h,LevelChunk chunk,java.util.function.BiConsumer<Map<BlockPos,BlockState>,List<Packet<?>>> test){
        var player=h.makeMockServerPlayerInLevel();var original=player.connection;
        var connection=(Connection)ArmoryServiceGameTests.field(original,"connection");
        var packets=new ArrayList<Packet<?>>();var client=new HashMap<BlockPos,BlockState>();
        new ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,player,CommonListenerCookie.createInitial(player.getGameProfile(),false)){
            @Override public void send(Packet<?> packet){
                packets.add(packet);
                if(packet instanceof ClientboundSectionBlocksUpdatePacket sections)sections.runUpdates((pos,state)->client.put(pos.immutable(),state));
                if(packet instanceof ClientboundBlockUpdatePacket block)client.put(block.getPos(),block.getBlockState());
            }
        };
        player.setChunkTrackingView(ChunkTrackingView.of(chunk.getPos(),2));
        try{
            h.assertTrue(h.getLevel().getChunkSource().chunkMap.getPlayers(chunk.getPos(),false).contains(player),"Observer is not watching the FULL chunk");
            test.accept(client,packets);
        }finally{player.connection=original;h.getLevel().getServer().getPlayerList().remove(player);}
    }

    @GameTest(template="reactor_loop",batch="fallout_client_sync",timeoutTicks=60)
    public static void completedFalloutActuallyUpdatesAClientWatchingNonTickingChunks(GameTestHelper h){
        border(h,5_000_008,chunk->observed(h,chunk,(client,packets)->{
            var at=new BlockPos(chunk.getPos().getMinBlockX()+8,-57,chunk.getPos().getMinBlockZ()+8);
            h.getLevel().setBlock(at,Blocks.GRASS_BLOCK.defaultBlockState(),2);
            h.getLevel().setBlock(at.above(),Blocks.OAK_LEAVES.defaultBlockState(),2);
            h.getLevel().setBlock(at.west(),Blocks.BLUE_ICE.defaultBlockState(),2);
            h.getLevel().setBlock(at.west().above(),Blocks.SNOW.defaultBlockState(),18);
            client.put(at,Blocks.GRASS_BLOCK.defaultBlockState());client.put(at.above(),Blocks.OAK_LEAVES.defaultBlockState());
            client.put(at.west(),Blocks.BLUE_ICE.defaultBlockState());client.put(at.west().above(),Blocks.SNOW.defaultBlockState());
            var region=new NuclearFalloutState.Region(Vec3.atCenterOf(at),2000,2000,0);
            try{
                Object cleanup=task("NuclearFalloutChunkTask").getConstructor(ServerLevel.class,NuclearFalloutState.Region.class,LevelChunk.class,NuclearFalloutState.class,BooleanSupplier.class)
                    .newInstance(h.getLevel(),region,chunk,null,(BooleanSupplier)()->true);
                int steps=0;while(!(boolean)ArmoryServiceGameTests.call(cleanup,"getAsBoolean")&&steps++<1_000_000){}
                h.assertTrue(region.columns.getOrDefault(chunk.getPos().toLong(),0)==256&&!chunk.getBlockState(at).is(Blocks.GRASS_BLOCK),"Server fallout did not complete");
                flush(h);
                h.assertTrue(client.get(at)==chunk.getBlockState(at)&&client.get(at.above()).isAir(),"Server marked the chunk complete but the watching client still displays untouched terrain");
                h.assertTrue(client.get(at.west()).is(Blocks.WATER)&&client.get(at.west().above()).isAir(),"Visible non-ticking chunk did not receive melted ice/snow states");
                h.assertTrue(packets.stream().anyMatch(p->p instanceof ClientboundSectionBlocksUpdatePacket),"No batched terrain update reached the visible FULL chunk");
            }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
        }));
    }

    @GameTest(template="reactor_loop",batch="fallout_client_sync",timeoutTicks=60)
    public static void pressureDamageAlsoUpdatesNonTickingVisibleTerrain(GameTestHelper h){
        border(h,5_200_008,chunk->observed(h,chunk,(client,packets)->{
            var at=new BlockPos(chunk.getPos().getMinBlockX()+8,-57,chunk.getPos().getMinBlockZ()+8);
            h.getLevel().setBlock(at,Blocks.BRICKS.defaultBlockState(),2);client.put(at,Blocks.BRICKS.defaultBlockState());
            try{
                Object pressure=task("NukeExplosionTask").getConstructor(ServerLevel.class,Vec3.class,float.class,int.class,List.class)
                    .newInstance(h.getLevel(),Vec3.atCenterOf(at),400F,2,List.of());
                int steps=0;while(!(boolean)ArmoryServiceGameTests.call(pressure,"isComplete")&&steps++<100_000)ArmoryServiceGameTests.call(pressure,"advance");
                h.assertTrue(steps<100_000&&chunk.getBlockState(at).isAir(),"Server pressure damage did not complete");
                flush(h);h.assertTrue(client.get(at).isAir(),"Client still displays blocks already destroyed by server pressure damage");
            }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
        }));
    }

    @GameTest(template="reactor_loop",batch="fallout_client_sync",timeoutTicks=60)
    public static void terrainPacketsAreBatchedBoundedAndCarryTheFinalState(GameTestHelper h){
        border(h,5_400_008,chunk->observed(h,chunk,(client,packets)->{
            var positions=new ArrayList<BlockPos>();
            for(int i=0;i<20;i++){
                var at=new BlockPos(chunk.getPos().getMinBlockX()+8,-63+i*16,chunk.getPos().getMinBlockZ()+8);positions.add(at);
                h.getLevel().setBlock(at,Blocks.BRICKS.defaultBlockState(),2);client.put(at,Blocks.BRICKS.defaultBlockState());
                ArmoryServiceGameTests.call(updates(),"replace",h.getLevel(),at,Blocks.STONE.defaultBlockState(),18);
                ArmoryServiceGameTests.call(updates(),"replace",h.getLevel(),at,Blocks.AIR.defaultBlockState(),18);
            }
            h.assertTrue((int)ArmoryServiceGameTests.call(updates(),"pendingSections",h.getLevel().getServer())==20,"Repeated edits were not coalesced by section");
            flush(h);h.assertTrue(packets.stream().filter(p->p instanceof ClientboundSectionBlocksUpdatePacket).count()==16,"Section packet cap exceeded or updates were lost");
            h.assertTrue((int)ArmoryServiceGameTests.call(updates(),"pendingSections",h.getLevel().getServer())==4,"Over-budget sections were discarded");
            flush(h);for(var at:positions)h.assertTrue(client.get(at).isAir(),"Coalesced packet contains stale intermediate state");
        }));
    }
}
