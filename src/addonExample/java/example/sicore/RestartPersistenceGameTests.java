package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-restart regression sweep over <b>every</b> SI block that owns a block entity.
 *
 * <p>For each block the test does what a restart does to a chunk: place it, serialise
 * the entity with {@link BlockEntity#saveWithFullMetadata}, discard it, rebuild it
 * through {@link BlockEntity#loadStatic} (the path chunk loading uses, which goes
 * through the registered {@code BlockEntityType} factory rather than
 * {@code newBlockEntity}), and put the rebuilt entity back into the world.
 *
 * <p>It then requires that the reloaded entity has the same class as the placed one
 * (the IV-LuV / LuV-ZPMV transformer bug), that it accepts its own block state,
 * that its tag survives a second round trip without losing keys, and that a GUI
 * which could be opened before the restart can still be opened after it.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class RestartPersistenceGameTests {

    /** Blocks whose placement needs a multiblock host or a world feature; checked for class only. */
    private static boolean menuOptional(String id) {
        return id.contains("reactor_chamber") || id.contains("reactor_") || id.contains("multiblock")
            || id.contains("_port") || id.contains("gesu_");
    }

    /** True when a GUI can actually be built for the entity in its current world state. */
    private static boolean createsMenu(MenuProvider fromState, BlockEntity entity, net.minecraft.world.entity.player.Player player) {
        MenuProvider provider = fromState != null ? fromState : entity instanceof MenuProvider mp ? mp : null;
        if (provider == null) return false;
        try {
            return provider.createMenu(0, player.getInventory(), player) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @GameTest(template = "empty", batch = "recovery", timeoutTicks = 200)
    public static void everyMachineSurvivesRestartWithoutReplacing(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 1, 1);
        BlockPos abs = h.absolutePos(rel);
        var level = h.getLevel();
        var registries = level.registryAccess();
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(abs.getX() + 0.5, abs.getY() + 1, abs.getZ() + 1.5);

        List<String> failures = new ArrayList<>();
        int checked = 0, index = -1;
        String range = System.getenv("SI_SWEEP");
        int from = range == null ? 0 : Integer.parseInt(range.split(":")[0]);
        int to = range == null ? Integer.MAX_VALUE : Integer.parseInt(range.split(":")[1]);
        for (Block block : BuiltInRegistries.BLOCK) {
            var key = BuiltInRegistries.BLOCK.getKey(block);
            if (!"mio_icif".equals(key.getNamespace()) || !(block instanceof EntityBlock)) continue;
            String id = key.toString();
            index++;
            if (index < from || index >= to) continue;
            BlockState state = block.defaultBlockState();
            try {
                level.setBlock(abs, state, 2 | 16);
                BlockEntity placed = level.getBlockEntity(abs);
                if (placed == null) continue;   // e.g. blocks that only create entities on demand
                checked++;
                boolean hadMenu = createsMenu(level.getBlockState(abs).getMenuProvider(level, abs), placed, player);

                CompoundTag saved = placed.saveWithFullMetadata(registries);
                BlockState liveState = level.getBlockState(abs);
                BlockEntity reloaded = BlockEntity.loadStatic(abs, liveState, saved, registries);
                if (reloaded == null) { failures.add(id + ": loadStatic returned null"); continue; }
                if (reloaded.getClass() != placed.getClass()) {
                    failures.add(id + ": placed " + placed.getClass().getSimpleName()
                        + " but reloaded " + reloaded.getClass().getSimpleName());
                    continue;
                }
                if (!reloaded.getType().isValid(liveState)) failures.add(id + ": reloaded type rejects its block state");

                // Swap the reloaded entity in, exactly like a chunk coming back from disk.
                level.removeBlockEntity(abs);
                level.setBlockEntity(reloaded);
                BlockEntity live = level.getBlockEntity(abs);
                if (live != reloaded) { failures.add(id + ": reloaded entity was not accepted by the chunk"); continue; }

                CompoundTag again = live.saveWithFullMetadata(registries);
                for (String k : saved.getAllKeys()) {
                    if (!again.contains(k)) failures.add(id + ": key '" + k + "' lost after reload");
                }

                if (hadMenu && !menuOptional(id)) {
                    MenuProvider provider = liveState.getMenuProvider(level, abs);
                    if (provider == null && live instanceof MenuProvider mp) provider = mp;
                    if (provider == null) {
                        failures.add(id + ": GUI provider missing after reload");
                    } else {
                        try {
                            var menu = provider.createMenu(1, player.getInventory(), player);
                            if (menu == null) failures.add(id + ": GUI could not be created after reload");
                            else if (!menu.stillValid(player)) failures.add(id + ": GUI rejected the player after reload");
                        } catch (RuntimeException e) {
                            failures.add(id + ": GUI threw after reload: " + e);
                        }
                    }
                }
            } catch (RuntimeException e) {
                failures.add(id + ": " + e);
            } finally {
                level.removeBlockEntity(abs);
                level.setBlock(abs, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
        player.discard();
        if (range == null && checked < 50) failures.add("only " + checked + " block entities checked; registry scan broken?");
        if (!failures.isEmpty()) {
            org.slf4j.LoggerFactory.getLogger("si_restart_sweep").error("Restart persistence failures ({}/{}):\n  {}",
                failures.size(), checked, String.join("\n  ", failures));
            String first = failures.get(0);
            h.fail(failures.size() + "/" + checked + " restart failures, first: " + first.substring(0, Math.min(400, first.length())));
        }
        org.slf4j.LoggerFactory.getLogger("si_restart_sweep").info("Restart sweep checked {} block entities", checked);
        h.succeed();
    }
}
