package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import com.miophas.singularity_iteration.core.runtime.radiation.RadiationState;

/** Only the isolated audit run enables this namespace; never edits a user game configuration. */
@GameTestHolder("si_config_audit")
@PrefixGameTestTemplate(false)
public final class CommonConfigFileGameTests {
    @GameTest(template="reactor_loop",batch="common_config_file",timeoutTicks=300)
    public static void actualTomlLoadsNonDefaultValuesAndFileWatcherReloadsLiveMachines(GameTestHelper h)throws Exception{
        Path path=Path.of(System.getProperty("si.config.audit.path")).toAbsolutePath().normalize();
        Path allowed=Path.of(System.getProperty("user.dir")).resolve("config").toAbsolutePath().normalize();
        h.assertTrue(path.startsWith(allowed),"Config audit tried to write outside its isolated game config directory");
        h.assertTrue(!(boolean)CommonConfigGameTests.get("ENABLE_NUCLEAR_EXPLOSION")
            &&(double)CommonConfigGameTests.get("NUKE_EXPLOSION_MULTIPLIER")==1.5
            &&(double)CommonConfigGameTests.get("REACTOR_EXPLOSION_MULTIPLIER")==2
            &&(int)CommonConfigGameTests.get("REACTOR_EXPLOSION_POWER_LIMIT")==30,"Common TOML values did not load");
        BlockPos pos=new BlockPos(6,3,6);h.setBlock(pos.east(),Blocks.GLASS);
        Object nuke=CommonConfigGameTests.nuke(h,pos);
        h.assertTrue((float)ArmoryServiceGameTests.call(nuke,"calculateExplosionPower")==1536,"File-configured nuke coefficient not applied");
        var before=new HashSet<>(RadiationState.get(h.getLevel()).zones.keySet());
        ArmoryServiceGameTests.call(nuke.getClass(),"tryTriggerExplosion",h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),nuke);
        h.assertTrue(h.getBlockState(pos.east()).is(Blocks.GLASS)&&RadiationState.get(h.getLevel()).zones.keySet().equals(before),"File-configured disable did not protect terrain");
        var machine=CommonConfigGameTests.placed(h,"producer/block_future_elc",pos);Object storage=ArmoryServiceGameTests.field(machine,"energyStorage");
        h.assertTrue((long)ArmoryServiceGameTests.call(storage,"getCapacity")==32123&&(long)ArmoryServiceGameTests.call(storage,"getMaxReceive")==75,"File-configured future machine values not applied");
        String text=Files.readString(path).replace("enableNuclearExplosion = false","enableNuclearExplosion = true")
            .replace("nukeExplosionMultiplier = 1.5","nukeExplosionMultiplier = 2.5")
            .replace("reactorExplosionMultiplier = 2.0","reactorExplosionMultiplier = 0.5")
            .replace("energyCapacity = 32123","energyCapacity = 12345").replace("maxReceive = 75","maxReceive = 64");
        Path temp=path.resolveSibling("common-audit-update.tmp");Files.writeString(temp,text);Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        h.succeedWhen(()->{
            h.assertTrue((boolean)CommonConfigGameTests.get("ENABLE_NUCLEAR_EXPLOSION")
                &&(double)CommonConfigGameTests.get("NUKE_EXPLOSION_MULTIPLIER")==2.5
                &&(double)CommonConfigGameTests.get("REACTOR_EXPLOSION_MULTIPLIER")==.5,"File watcher has not reloaded nuclear settings");
            h.assertTrue((long)ArmoryServiceGameTests.call(storage,"getCapacity")==12345&&(long)ArmoryServiceGameTests.call(storage,"getMaxReceive")==64,"Existing machine did not pick up the file reload");
        });
    }
}
