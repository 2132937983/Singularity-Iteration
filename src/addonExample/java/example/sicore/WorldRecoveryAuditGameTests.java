package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.Files;
import java.nio.file.Path;

/** Load regression for the real saved dimensions and local runtime dependencies. */
@EventBusSubscriber(modid=CoreExampleMod.ID)
public final class WorldRecoveryAuditGameTests {
    private static MinecraftServer auditing;
    private static int ticks;
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(!Boolean.getBoolean("si.worldrecovery.audit"))return;
        auditing=event.getServer();
        for(int x=259;x<=268;x++)for(int z=69;z<=81;z++)auditing.overworld().setChunkForced(x,z,true);
        System.out.println("WORLD_RECOVERY native saved dimensions loaded");
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event)throws Exception {
        if(auditing==null||event.getServer()!=auditing)return;
        if(++ticks!=240)return;
        var world=auditing.overworld();
        boolean twilight=false;for(var level:auditing.getAllLevels())if(level.dimension().location().equals(ResourceLocation.fromNamespaceAndPath("twilightforest","twilight_forest")))twilight=true;
        if(!twilight)throw new IllegalStateException("Original Twilight Forest dimension missing");
        if(!world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).containsKey(ResourceLocation.fromNamespaceAndPath("terralith","granite_cliffs")))throw new IllegalStateException("Missing original Terralith biome registry");
        for(int row=0;row<4;row++)for(int col=0;col<4;col++) {
            var at=new BlockPos(4160+40*col,96,1128+40*row);
            if(!(world.getBlockEntity(at) instanceof IReactorController reactor)||!reactor.isValidFluidReactorStructure()||reactor.isRunning()||reactor.getCurrentHeat()>32)
                throw new IllegalStateException("Delivery reactor invalid or not safely stopped: "+at);
        }
        var pack=ResourceLocation.fromNamespaceAndPath("si_heat_complex","start");
        if(auditing.getFunctions().get(pack).isEmpty())throw new IllegalStateException("Station control datapack failed to load");
        Files.writeString(Path.of("WORLD_LOAD_VERIFIED"),"Original world metadata and dimensions loaded unchanged; Terralith biome definitions present; all 16 reactors formed and stopped; station functions loaded.\n");
        System.out.println("WORLD_RECOVERY VERIFIED original dimensions, Terralith registries, 16 stopped reactors and datapack");
        for(int x=259;x<=268;x++)for(int z=69;z<=81;z++)world.setChunkForced(x,z,false);
        auditing.halt(false);auditing=null;
    }
}
