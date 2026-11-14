package example.sicore;

import com.miophas.singularity_iteration.core.api.event.MachineWorkCompleteEvent;
import com.miophas.singularity_iteration.core.api.event.MachineWorkStartEvent;
import com.miophas.singularity_iteration.core.api.suit.SuitNanoCore;
import com.miophas.singularity_iteration.core.api.suit.SuitQuantumCore;
import com.miophas.singularity_iteration.core.api.upgrade.tile.CycleDecision;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.2: the work cycle upgrade API (P5: {@code IWorkCycleUpgrade}, the machine work events) and the
 * suit core costs (P6: {@code SuitQuantumCore.trySpendCoherence}, {@code SuitNanoCore.trySpendReserve} / {@code addHeat}).
 * The machine is the macerator (0.1.11.1 timing: 300 ticks at 2 EU/t; the stone to gravel recipe).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1152GameTests {
    private Round1152GameTests() {}

    private static final int INPUT = 0, OUTPUT = 2, UPGRADE = 3;
    private static final ResourceLocation STONE_TO_GRAVEL = ResourceLocation.fromNamespaceAndPath("mio_icif", "powder/stone_to_gravel");

    /** Event listener state, by absolute machine position: positions whose start event is cancelled, and event counts. */
    private static final Set<BlockPos> CANCEL_START = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, Integer> STARTS = new ConcurrentHashMap<>(), COMPLETES = new ConcurrentHashMap<>();
    private static volatile boolean listening;

    private static synchronized void listen() {
        if (listening) return;
        listening = true;
        NeoForge.EVENT_BUS.addListener((MachineWorkStartEvent e) -> {
            BlockPos pos = e.getPos().immutable();
            STARTS.merge(pos, 1, Integer::sum);
            if (CANCEL_START.contains(pos)) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((MachineWorkCompleteEvent e) -> COMPLETES.merge(e.getPos().immutable(), 1, Integer::sum));
    }

    private static CompoundTag tag(Object... pairs) {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < pairs.length; i += 2) {
            String key = (String) pairs[i];
            Object value = pairs[i + 1];
            if (value instanceof Boolean b) tag.putBoolean(key, b);
            else if (value instanceof Float f) tag.putFloat(key, f);
            else tag.putString(key, String.valueOf(value));
        }
        return tag;
    }

    /** A macerator at {@code at} with 64 stone and the upgrade (none when empty). */
    private static AbstractProcessingMachineBlockEntity macerator(GameTestHelper h, BlockPos at, ItemStack upgrade) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_powder_elc")));
        var m = (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
        h.assertTrue(m.getItemHandler().insertItem(INPUT, new ItemStack(Items.STONE, 64), false).isEmpty(), "stone was refused");
        if (!upgrade.isEmpty()) h.assertTrue(m.getItemHandler().insertItem(UPGRADE, upgrade, false).isEmpty(), "the upgrade was refused");
        return m;
    }

    private static AbstractProcessingMachineBlockEntity machine(GameTestHelper h, BlockPos at) {
        return (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
    }

    @SuppressWarnings("deprecation")
    private static void power(GameTestHelper h, BlockPos... at) {
        for (BlockPos p : at) if (h.getBlockEntity(p) instanceof AbstractProcessingMachineBlockEntity m) m.getEnergyStorageInternal().setStored(600);
    }

    @SuppressWarnings("deprecation")
    private static long stored(AbstractProcessingMachineBlockEntity m) {
        return m.getEnergyStorageInternal().getAmount();
    }

    // ------------------------------------------------------------------ P5

    /** CycleDecision clamps each factor to 0.25..4; a value that is not finite is 1. */
    @GameTest(template = "empty", batch = "round1152_cycle", timeoutTicks = 20)
    public static void decisionFactorsAreClamped(GameTestHelper h) {
        CycleDecision d = CycleDecision.proceed(10F, 0.1F);
        h.assertTrue(d.durationFactor() == 4F && d.energyFactor() == 0.25F && !d.isHold(), "not clamped: " + d);
        CycleDecision n = CycleDecision.proceed(Float.NaN, Float.POSITIVE_INFINITY);
        h.assertTrue(n.durationFactor() == 1F && n.energyFactor() == 1F, "not finite: " + n);
        h.assertTrue(CycleDecision.hold().isHold() && !CycleDecision.proceed().isHold(), "hold / proceed");
        h.succeed();
    }

    /** Test 1 and 5: proceed(2, 1) doubles the cycle of the macerator; the macerator without the upgrade keeps 300 ticks. */
    @GameTest(template = "empty", batch = "round1152_cycle", timeoutTicks = 800)
    public static void durationFactorDoublesTheCycle(GameTestHelper h) {
        BlockPos plain = new BlockPos(1, 1, 1), slow = new BlockPos(3, 1, 1);
        macerator(h, plain, ItemStack.EMPTY);
        macerator(h, slow, ExampleCycleUpgrade.configured(tag("duration", 2.0F, "energy", 1.0F)));
        long[] done = {-1, -1};
        h.onEachTick(() -> {
            power(h, plain, slow);
            long now = h.getTick();
            if (now == 20) {
                var m = machine(h, slow);
                h.assertTrue(m.getMaxProgress() == 600 && m.getCycleDurationFactor() == 2F, "slow cycle length " + m.getMaxProgress());
                h.assertTrue(m.getEffectiveEnergyPerTick() == 2, "energy factor 1 changed the EU/t: " + m.getEffectiveEnergyPerTick());
                h.assertTrue(machine(h, plain).getMaxProgress() == 300 && machine(h, plain).getCycleDurationFactor() == 1F,
                    "plain cycle length " + machine(h, plain).getMaxProgress());
            }
            if (done[0] < 0 && !machine(h, plain).getItemHandler().getStackInSlot(OUTPUT).isEmpty()) done[0] = now;
            if (done[1] < 0 && !machine(h, slow).getItemHandler().getStackInSlot(OUTPUT).isEmpty()) {
                done[1] = now;
                h.assertTrue(done[0] >= 295 && done[0] <= 305, "the plain macerator took " + done[0] + " ticks");
                h.assertTrue(done[1] >= 2 * done[0] - 5 && done[1] <= 2 * done[0] + 5, "the slow macerator took " + done[1] + " ticks, plain " + done[0]);
            }
            // the next cycle asks again and gets the factor again
            if (done[1] > 0 && now == done[1] + 3) {
                h.assertTrue(machine(h, slow).getCycleDurationFactor() == 2F && machine(h, slow).getMaxProgress() == 600,
                    "the next cycle did not ask again: " + machine(h, slow).getMaxProgress());
                h.succeed();
            }
        });
    }

    /** The energy factor multiplies the EU per tick of the cycle; the context reports the values without the factors. */
    @GameTest(template = "empty", batch = "round1152_cycle", timeoutTicks = 40)
    public static void energyFactorScalesTheEuPerTick(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        macerator(h, at, ExampleCycleUpgrade.configured(tag("energy", 3.0F)));
        h.onEachTick(() -> power(h, at));
        h.runAfterDelay(5, () -> {
            var m = machine(h, at);
            h.assertTrue(m.getEffectiveEnergyPerTick() == 6 && m.getMaxProgress() == 300, "EU/t " + m.getEffectiveEnergyPerTick() + ", ticks " + m.getMaxProgress());
            var ctx = ExampleCycleUpgrade.LAST_START.get(h.absolutePos(at));
            h.assertTrue(ctx != null && ctx.baseTicks() == 300 && ctx.baseEnergyPerTick() == 2, "context " + ctx);
            h.succeed();
        });
    }

    /** Test 2: hold() keeps the macerator at progress 0 without energy use or sleep; proceed() lets it work again. */
    @GameTest(template = "empty", batch = "round1152_cycle", timeoutTicks = 120)
    public static void holdKeepsTheMachineAtZero(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        macerator(h, at, ExampleCycleUpgrade.configured(tag("hold", true)));
        power(h, at);
        h.runAfterDelay(60, () -> {
            var m = machine(h, at);
            h.assertTrue(m.getProgress() == 0 && !m.isWorking(), "held machine progress " + m.getProgress());
            h.assertTrue(stored(m) == 600, "the held machine used energy: " + stored(m));
            h.assertTrue(!m.isIdleAsleep(), "the held machine sleeps (it must ask every tick)");
            h.assertTrue(ExampleCycleUpgrade.LAST_START.containsKey(h.absolutePos(at)), "the upgrade was not asked");
            m.getItemHandler().extractItem(UPGRADE, 64, false);
            h.assertTrue(m.getItemHandler().insertItem(UPGRADE, ExampleCycleUpgrade.configured(new CompoundTag()), false).isEmpty(), "swap refused");
            h.runAfterDelay(20, () -> {
                h.assertTrue(m.getProgress() > 0 && m.isWorking(), "the machine did not resume: " + m.getProgress());
                h.assertTrue(stored(m) < 600, "no energy used after the resume");
                h.succeed();
            });
        });
    }

    /** Test 3: a bonus appears when the output has room; a bonus without room is discarded and the machine goes on. */
    @GameTest(template = "empty", batch = "round1152_cycle", timeoutTicks = 400)
    public static void bonusOutputsFillFreeSpaceOnly(GameTestHelper h) {
        BlockPos same = new BlockPos(1, 1, 1), other = new BlockPos(3, 1, 1), full = new BlockPos(5, 1, 1);
        macerator(h, same, ExampleCycleUpgrade.configured(tag("duration", 0.25F, "bonus", "minecraft:gravel")));
        macerator(h, other, ExampleCycleUpgrade.configured(tag("duration", 0.25F, "bonus", "minecraft:clay_ball")));
        var f = macerator(h, full, ExampleCycleUpgrade.configured(tag("duration", 0.25F, "bonus", "minecraft:gravel")));
        ((net.neoforged.neoforge.items.IItemHandlerModifiable) f.getItemHandler()).setStackInSlot(OUTPUT, new ItemStack(Items.GRAVEL, 63));
        boolean[] checked = new boolean[3];
        h.onEachTick(() -> {
            power(h, same, other, full);
            var a = machine(h, same).getItemHandler();
            if (!checked[0] && !a.getStackInSlot(OUTPUT).isEmpty()) {
                checked[0] = true;
                h.assertTrue(a.getStackInSlot(OUTPUT).is(Items.GRAVEL) && a.getStackInSlot(OUTPUT).getCount() == 2,
                    "output with room: " + a.getStackInSlot(OUTPUT));
            }
            var c = machine(h, full).getItemHandler();
            if (!checked[2] && c.getStackInSlot(INPUT).getCount() == 63) {
                checked[2] = true;
                h.assertTrue(c.getStackInSlot(OUTPUT).getCount() == 64, "full output: " + c.getStackInSlot(OUTPUT));
            }
            var b = machine(h, other).getItemHandler();
            if (!checked[1] && b.getStackInSlot(INPUT).getCount() <= 61) {
                checked[1] = true;
                h.assertTrue(b.getStackInSlot(OUTPUT).is(Items.GRAVEL) && b.getStackInSlot(OUTPUT).getCount() == 64 - b.getStackInSlot(INPUT).getCount(),
                    "a bonus that does not fit changed the output: " + b.getStackInSlot(OUTPUT));
                h.assertTrue(ExampleCycleUpgrade.COMPLETIONS.getOrDefault(h.absolutePos(other), 0) >= 3, "completions not reported");
            }
            if (checked[0] && checked[1] && checked[2]) h.succeed();
        });
    }

    /** Test 4: the context names the recipe of the macerator. */
    @GameTest(template = "empty", batch = "round1152_cycle", timeoutTicks = 40)
    public static void contextNamesTheRecipe(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        macerator(h, at, ExampleCycleUpgrade.configured(new CompoundTag()));
        h.onEachTick(() -> power(h, at));
        h.runAfterDelay(5, () -> {
            var ctx = ExampleCycleUpgrade.LAST_START.get(h.absolutePos(at));
            h.assertTrue(ctx != null && STONE_TO_GRAVEL.equals(ctx.recipeId()), "recipe id " + (ctx == null ? "no context" : ctx.recipeId()));
            h.assertTrue(ctx.machine() == machine(h, at) && ctx.level() == h.getLevel(), "context machine / level");
            h.succeed();
        });
    }

    /** The start event is posted and a cancel holds the machine; the complete event follows each output. */
    @GameTest(template = "empty", batch = "round1152_cycle", timeoutTicks = 400)
    public static void workEventsArePosted(GameTestHelper h) {
        listen();
        BlockPos runs = new BlockPos(1, 1, 1), held = new BlockPos(3, 1, 1);
        BlockPos runsAbs = h.absolutePos(runs), heldAbs = h.absolutePos(held);
        CANCEL_START.add(heldAbs);
        macerator(h, runs, ExampleCycleUpgrade.configured(tag("duration", 0.25F)));
        macerator(h, held, ItemStack.EMPTY);
        h.onEachTick(() -> power(h, runs, held));
        h.runAfterDelay(100, () -> {
            h.assertTrue(STARTS.getOrDefault(runsAbs, 0) >= 1 && COMPLETES.getOrDefault(runsAbs, 0) >= 1,
                "events of the working machine: " + STARTS.get(runsAbs) + " / " + COMPLETES.get(runsAbs));
            var m = machine(h, held);
            h.assertTrue(m.getProgress() == 0 && !m.isWorking() && STARTS.getOrDefault(heldAbs, 0) >= 90,
                "cancelled start: progress " + m.getProgress() + ", asked " + STARTS.get(heldAbs));
            h.assertTrue(COMPLETES.getOrDefault(heldAbs, 0) == 0, "a held machine completed");
            CANCEL_START.remove(heldAbs);
            h.runAfterDelay(10, () -> {
                h.assertTrue(m.getProgress() > 0 && m.isWorking(), "no work after the cancel ended");
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ P6

    /** Test 1: coherence is spent when there is enough, else nothing changes. */
    @GameTest(template = "empty", batch = "round1152_suit", timeoutTicks = 20)
    public static void coherenceIsSpentOnlyWhenEnough(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        p.setItemSlot(EquipmentSlot.CHEST, Round33GameTests.item("armor/item_armor_quantum_chestplate"));
        h.assertTrue(SuitQuantumCore.trySpendCoherence(p, 30F), "30 of 100 refused");
        h.assertTrue(Math.abs(SuitQuantumCore.coherence(p) - 70F) < 0.01F, "after 30: " + SuitQuantumCore.coherence(p));
        h.assertTrue(!SuitQuantumCore.trySpendCoherence(p, 80F), "80 of 70 accepted");
        h.assertTrue(!SuitQuantumCore.trySpendCoherence(p, -1F) && !SuitQuantumCore.trySpendCoherence(p, Float.NaN), "bad amounts accepted");
        h.assertTrue(Math.abs(SuitQuantumCore.coherence(p) - 70F) < 0.01F, "a refused spend changed the coherence: " + SuitQuantumCore.coherence(p));
        h.assertTrue(SuitQuantumCore.trySpendCoherence(p, 70F) && SuitQuantumCore.decoherent(p), "spending all does not decohere");
        h.assertTrue(!SuitQuantumCore.trySpendCoherence(p, 0F), "a decoherent core accepted a spend");
        h.succeed();
    }

    /** Test 2: the nano reserve is spent when there is enough, else nothing changes. */
    @GameTest(template = "empty", batch = "round1152_suit", timeoutTicks = 20)
    public static void reserveIsSpentOnlyWhenEnough(GameTestHelper h) {
        ServerPlayer p = nanoPlayer(h);
        float start = SuitNanoCore.reserve(p);
        h.assertTrue(start >= 50F, "reserve at the start " + start);
        h.assertTrue(SuitNanoCore.trySpendReserve(p, 25F), "25 refused");
        h.assertTrue(Math.abs(SuitNanoCore.reserve(p) - (start - 25F)) < 0.01F, "after 25: " + SuitNanoCore.reserve(p));
        h.assertTrue(!SuitNanoCore.trySpendReserve(p, start), "more than the reserve accepted");
        h.assertTrue(!SuitNanoCore.trySpendReserve(p, Float.POSITIVE_INFINITY), "infinity accepted");
        h.assertTrue(Math.abs(SuitNanoCore.reserve(p) - (start - 25F)) < 0.01F, "a refused spend changed the reserve");
        h.succeed();
    }

    /** Test 3: heat goes up; at the cap the suit overheats. */
    @GameTest(template = "empty", batch = "round1152_suit", timeoutTicks = 20)
    public static void heatRisesAndOverheats(GameTestHelper h) {
        ServerPlayer p = nanoPlayer(h);
        float before = SuitNanoCore.heat(p);
        SuitNanoCore.addHeat(p, 10F);
        h.assertTrue(Math.abs(SuitNanoCore.heat(p) - (before + 10F)) < 0.01F && !SuitNanoCore.overheated(p), "heat " + SuitNanoCore.heat(p));
        SuitNanoCore.addHeat(p, Float.NaN);
        SuitNanoCore.addHeat(p, -5F);
        h.assertTrue(Math.abs(SuitNanoCore.heat(p) - (before + 10F)) < 0.01F, "bad amounts changed the heat");
        SuitNanoCore.addHeat(p, SuitNanoCore.heatCap(p) + 10F);
        h.assertTrue(SuitNanoCore.overheated(p) && SuitNanoCore.heat(p) <= SuitNanoCore.heatCap(p), "no overheat at the cap: " + SuitNanoCore.heat(p));
        h.succeed();
    }

    /** Test 4: a player without a suit: no method changes a state (no core is even created). */
    @GameTest(template = "empty", batch = "round1152_suit", timeoutTicks = 20)
    public static void noSuitNoChange(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        h.assertTrue(!SuitQuantumCore.trySpendCoherence(p, 1F), "coherence spent without a suit");
        h.assertTrue(!SuitNanoCore.trySpendReserve(p, 1F), "reserve spent without a suit");
        SuitNanoCore.addHeat(p, 50F);
        // A new core reads 100 coherence / 100 reserve; 0 means that no core state was made.
        h.assertTrue(SuitQuantumCore.coherence(p) == 0F && SuitNanoCore.reserve(p) == 0F && SuitNanoCore.heat(p) == 0F
            && !SuitNanoCore.overheated(p), "state changed: " + SuitQuantumCore.coherence(p) + " / " + SuitNanoCore.reserve(p) + " / " + SuitNanoCore.heat(p));
        h.succeed();
    }

    /** A player with a nano chestplate that carries one unit (the nano core is online). */
    private static ServerPlayer nanoPlayer(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        ItemStack chest = Round1100GameTests.charged("armor/item_armor_nano_chestplate");
        h.assertTrue(Round1100GameTests.install(chest, "EMP_EMITTER", 1), "the EMP unit did not fit");
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        h.assertTrue(SuitNanoCore.trySpendReserve(p, 0F), "the nano core is not online");
        return p;
    }
}
