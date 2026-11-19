package example.sicore;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.80: the clean energy total. Placing a clean generator makes the placer its owner; an unowned one is claimed by
 * the first right-click (not taken over by a second player). Only clean generators count (a Generator does not), a
 * kinetic generator only with a wind or water kinetic generator in front. Crossing 2,147,483,647 EU grants Clean
 * Energy Ambassador, and a total already past it grants it at login. The totals survive a save.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11580GameTests {
    private Round11580GameTests() {}

    private static final String CLEAN = "advancement.CleanEnergy";

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Class<?> clean() { return Round33GameTests.common(CLEAN); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Object ledger(GameTestHelper h) {
        return call(Round33GameTests.common(CLEAN + "$Ledger"), "get", h.getLevel().getServer());
    }

    private static long total(GameTestHelper h, UUID player) { return (long) call(ledger(h), "total", player); }

    private static UUID owner(BlockEntity be) { return (UUID) call(clean(), "owner", be); }

    private static ServerPlayer survival(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static boolean ambassador(GameTestHelper h, ServerPlayer p) {
        return p.getAdvancements().getOrStartProgress(h.getLevel().getServer().getAdvancements().get(id("clean_ambassador"))).isDone();
    }

    private static BlockEntity place(GameTestHelper h, BlockPos at, String block) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(id(block)));
        return h.getBlockEntity(at);
    }

    @GameTest(template = "empty", batch = "round11580_owner", timeoutTicks = 20)
    public static void placingAndClaimingSetTheOwner(GameTestHelper h) {
        ServerPlayer placer = survival(h);
        BlockPos floor = new BlockPos(2, 1, 2);
        h.setBlock(floor, Blocks.STONE);
        ItemStack mill = new ItemStack(BuiltInRegistries.ITEM.get(id("generator/block_wind_generator")));
        placer.setItemInHand(InteractionHand.MAIN_HAND, mill);
        BlockPos abs = h.absolutePos(floor);
        mill.useOn(new UseOnContext(placer, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
        BlockEntity placed = h.getBlockEntity(floor.above());
        h.assertTrue(placed != null && placer.getUUID().equals(owner(placed)), "placing a Wind Mill did not make the placer its owner");
        // an unowned Solar Generator (from an old world): the first right-click claims it, a second player does not take it
        BlockPos solar = new BlockPos(4, 2, 2);
        BlockEntity panel = place(h, solar, "generator/block_solar_generator");
        h.assertTrue(owner(panel) == null, "a set block already has an owner");
        ServerPlayer first = survival(h), second = survival(h);
        BlockPos sAbs = h.absolutePos(solar);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(sAbs), Direction.UP, sAbs, false);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(first, InteractionHand.MAIN_HAND, sAbs, hit));
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(second, InteractionHand.MAIN_HAND, sAbs, hit));
        h.assertTrue(first.getUUID().equals(owner(panel)), "the first right-click did not claim the unowned generator");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11580_count", timeoutTicks = 20)
    public static void onlyCleanEnergyCounts(GameTestHelper h) {
        UUID owner = UUID.randomUUID();
        BlockEntity mill = place(h, new BlockPos(1, 2, 1), "generator/block_wind_generator");
        BlockEntity furnaceGen = place(h, new BlockPos(3, 2, 1), "generator/block_thermal_generator");
        call(clean(), "setOwner", mill, owner);
        call(clean(), "setOwner", furnaceGen, owner);
        com.miophas.singularity_iteration.core.api.energy.EnergyProductionListeners.fire(mill, 1000.5);
        com.miophas.singularity_iteration.core.api.energy.EnergyProductionListeners.fire(furnaceGen, 5000);
        h.assertTrue(total(h, owner) == 1000, "a Wind Mill and a Generator gave " + total(h, owner) + " clean EU (expected 1000)");
        com.miophas.singularity_iteration.core.api.energy.EnergyProductionListeners.fire(mill, 0.5);
        h.assertTrue(total(h, owner) == 1001, "the half EU was not carried: " + total(h, owner));
        // a kinetic generator: clean only with a wind or water kinetic generator in front
        BlockPos kg = new BlockPos(2, 2, 3);
        BlockEntity kinetic = place(h, kg, "generator/block_kinetic_generator");
        call(clean(), "setOwner", kinetic, owner);
        Direction facing = kinetic.getBlockState().getValue(BlockStateProperties.FACING);
        place(h, kg.relative(facing), "kugenerator/block_manual_kinetic_generator");
        com.miophas.singularity_iteration.core.api.energy.EnergyProductionListeners.fire(kinetic, 100);
        h.assertTrue(total(h, owner) == 1001, "a hand-cranked kinetic source counted as clean");
        place(h, kg.relative(facing), "kugenerator/block_wind_kinetic_generator");
        com.miophas.singularity_iteration.core.api.energy.EnergyProductionListeners.fire(kinetic, 100);
        h.assertTrue(total(h, owner) == 1101, "a wind kinetic source did not count as clean: " + total(h, owner));
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11580_target", timeoutTicks = 20)
    public static void crossingTheTargetGrantsTheAmbassador(GameTestHelper h) {
        ServerPlayer online = survival(h);
        Object ledger = ledger(h);
        call(ledger, "add", h.getLevel().getServer(), online.getUUID(), (double) (Integer.MAX_VALUE - 10L));
        h.assertTrue(!ambassador(h, online), "the ambassador was granted below the target");
        call(ledger, "add", h.getLevel().getServer(), online.getUUID(), 10.0);
        h.assertTrue(ambassador(h, online), "crossing 2,147,483,647 EU did not grant Clean Energy Ambassador");
        // a total that crossed while the player was away: granted at login
        ServerPlayer away = survival(h);
        call(ledger, "add", h.getLevel().getServer(), UUID.randomUUID(), 1.0);   // somebody else
        var totals = new CompoundTag();
        call(ledger, "add", h.getLevel().getServer(), away.getUUID(), (double) Integer.MAX_VALUE);
        // the online check above already granted it; take it back to test the login path
        var holder = h.getLevel().getServer().getAdvancements().get(id("clean_ambassador"));
        away.getAdvancements().revoke(holder, "total");
        h.assertTrue(!ambassador(h, away), "revoke failed");
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(away));
        h.assertTrue(ambassador(h, away), "a total past the target did not grant the ambassador at login");
        // the totals survive a save
        CompoundTag saved = (CompoundTag) call(ledger, "save", totals, h.getLevel().registryAccess());
        Object loaded = call(Round33GameTests.common(CLEAN + "$Ledger"), "load", saved, h.getLevel().registryAccess());
        h.assertTrue((long) call(loaded, "total", away.getUUID()) >= Integer.MAX_VALUE, "the ledger lost a total on save");
        h.succeed();
    }
}
