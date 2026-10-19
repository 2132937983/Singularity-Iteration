package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GameTests of 0.1.10.12: the reactor accident audit (blackbox, cause analysis, incident log, the
 * blackbox item, the meltdown with the explosions off) and the max-min share of the fair energy grid
 * (a storage block that feeds machines is not starved by a huge load next to it).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1112GameTests {
    private Round1112GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";
    private static final String ENGINE = "com.miophas.singularity_iteration.core" + ".runtime.energy.engine.";

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(rl(path)); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    private static Class<?> common(String name) { return type(COMMON + name); }

    private static Object create(Class<?> record, Object... args) {
        try {
            var constructor = record.getDeclaredConstructors()[0];
            for (var c : record.getDeclaredConstructors()) if (c.getParameterCount() == args.length) constructor = c;
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(record.getName(), e);
        }
    }

    private static BlockState facing(String id, Direction side) {
        var state = block(id).defaultBlockState();
        return state.setValue((DirectionProperty) state.getBlock().getStateDefinition().getProperty("facing"), side);
    }

    // ------------------------------------------------------------------ item 2: grid starvation

    /**
     * Max-min share: small demands are met in full, the rest is shared at one level; a storage block
     * asks for its output rate in the priority stage only while it relays to machines.
     */
    @GameTest(batch = "round1112_grid", template = "empty", timeoutTicks = 20)
    public static void maxMinShareMeetsSmallDemandsFirst(GameTestHelper h) {
        Class<?> distributor = type(ENGINE + "FairShareDistributor");
        long[] planned = (long[]) call(distributor, "maxMinLossless", new long[]{Long.MAX_VALUE / 4, 32L, 32L, 512L}, 1000L);
        h.assertTrue(Arrays.equals(planned, new long[]{468, 32, 32, 468}), "max-min plan " + Arrays.toString(planned));
        long[] uneven = (long[]) call(distributor, "maxMinLossless", new long[]{100L, 100L, 100L}, 100L);
        h.assertTrue(Arrays.stream(uneven).sum() == 100 && Arrays.stream(uneven).max().getAsLong() - Arrays.stream(uneven).min().getAsLong() <= 1,
            "uneven level " + Arrays.toString(uneven));
        long[] enough = (long[]) call(distributor, "maxMinLossless", new long[]{10L, 0L, 20L}, 100L);
        h.assertTrue(Arrays.equals(enough, new long[]{10, 0, 20}), "budget above the demand " + Arrays.toString(enough));
        // stage 0: a load asks for its rated input, a relaying storage block for its output rate, a plain one for nothing
        h.assertTrue((Long) call(distributor, "stageDemand", 0, 0, 120_000_000L, -1L, 0L) == 120_000_000L, "load stage 0");
        h.assertTrue((Long) call(distributor, "stageDemand", 0, 1, 40_000L, 32L, 32L) == 32L, "relay stage 0");
        h.assertTrue((Long) call(distributor, "stageDemand", 0, 1, 40_000L, 32L, 0L) == 0L, "storage without relay stage 0");
        h.assertTrue((Long) call(distributor, "stageDemand", 2, 1, 40_000L, 32L, 0L) == 32L, "storage stage 2");
        h.succeed();
    }

    /**
     * The reported topology in small: a generator feeds a Molecular Transformer (no input limit, 120M
     * room) and a BatBox whose output feeds an electric furnace. Before 0.1.10.12 the transformer took the
     * whole output and the furnace behind the BatBox got nothing; now the BatBox gets its output rate.
     */
    @GameTest(batch = "round1112_grid", template = "empty", timeoutTicks = 100)
    public static void storageFeedingMachinesIsNotStarvedByAHugeLoad(GameTestHelper h) {
        BlockPos furnacePos = new BlockPos(0, 1, 2), boxPos = new BlockPos(1, 1, 2), genPos = new BlockPos(2, 1, 2), mtPos = new BlockPos(3, 1, 2);
        h.setBlock(furnacePos, block("producer/block_furnace_elc"));
        h.setBlock(boxPos, facing("wiring/block_bat_box", Direction.WEST));
        h.setBlock(genPos, block("generator/block_creative_generator"));
        h.setBlock(mtPos, block("producer/block_molecular_transformer"));
        call(h.getBlockEntity(genPos), "configure", 128L, true);
        h.runAfterDelay(60, () -> {
            BlockEntity furnace = h.getBlockEntity(furnacePos), transformer = h.getBlockEntity(mtPos);
            h.assertTrue(furnace instanceof AbstractEnergyBlockEntity, "furnace missing");
            long fed = ((AbstractEnergyBlockEntity) furnace).getEnergyStorageInternal().getAmount();
            h.assertTrue(fed > 0, "the furnace behind the BatBox starved");
            if (transformer instanceof AbstractEnergyBlockEntity mt)
                h.assertTrue(mt.getEnergyStorageInternal().getAmount() > 0, "the transformer got nothing");
            for (BlockPos p : new BlockPos[]{furnacePos, boxPos, genPos, mtPos}) h.setBlock(p, Blocks.AIR);
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ item 1: cause analysis

    private static CompoundTag sample(long time, long before, long after, long generated, long emitted) {
        CompoundTag t = new CompoundTag();
        t.putLong("T", time); t.putLong("Hb", before); t.putLong("Ha", after); t.putLong("Hm", 10_000);
        t.putLong("G", generated); t.putLong("E", emitted); t.putInt("F", 1); t.putInt("C", 9); t.putInt("Fr", 4);
        t.putBoolean("On", true);
        return t;
    }

    private static List<?> samples(List<CompoundTag> tags) {
        ListTag list = new ListTag();
        list.addAll(tags);
        return (List<?>) call(common("reactor.audit.ReactorBlackbox"), "loadSamples", list);
    }

    private static List<?> melts(int count, long time) {
        ListTag list = new ListTag();
        for (int i = 0; i < count; i++) {
            CompoundTag m = new CompoundTag();
            m.putString("I", "mio_icif:reactor/item_reactor_vent"); m.putInt("K", 3); m.putInt("S", 1000); m.putInt("C", 1000);
            m.putInt("Slot", i); m.putLong("T", time);
            list.add(m);
        }
        return (List<?>) call(common("reactor.audit.ReactorBlackbox"), "loadMelts", list);
    }

    private static List<String> codes(List<?> findings) {
        List<String> out = new ArrayList<>();
        for (Object f : findings) out.add((String) call(f, "code"));
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<String> args(Object finding) { return (List<String>) call(finding, "args"); }

    /** The analyzer names the main cause for the typical accidents. */
    @GameTest(batch = "round1112_audit", template = "empty", timeoutTicks = 20)
    public static void analyzerNamesTheMainCause(GameTestHelper h) {
        Class<?> analyzer = common("reactor.audit.ReactorAccidentAnalyzer");
        // a steady rise: not enough cooling
        List<CompoundTag> rise = new ArrayList<>();
        for (int i = 0; i < 10; i++) rise.add(sample(1000 + 20L * i, 1000 + 500L * i, 1500 + 500L * i, 800, 300));
        List<?> found = (List<?>) call(analyzer, "analyze", samples(rise), List.of(), 1200L, false, List.of());
        h.assertTrue(codes(found).get(0).equals("heat_runaway"), "runaway: " + codes(found));
        h.assertTrue(args(found.get(0)).equals(List.of("800", "300", "10")), "runaway args " + args(found.get(0)));
        // a calm hull and two vents melted in the last cycle
        List<CompoundTag> calm = new ArrayList<>();
        for (int i = 0; i < 3; i++) calm.add(sample(2000 + 20L * i, 2000, 2000, 400, 400));
        calm.add(sample(2060, 2000, 2600, 400, 0));
        found = (List<?>) call(analyzer, "analyze", samples(calm), melts(2, 2060), 2070L, false, List.of());
        h.assertTrue(codes(found).get(0).equals("component_melted"), "melted: " + codes(found));
        h.assertTrue(args(found.get(0)).get(1).equals("vent:2"), "melted kinds " + args(found.get(0)));
        // fluid reactor whose hot coolant tank is full
        CompoundTag hot = sample(3020, 5000, 5400, 600, 600);
        hot.putBoolean("Fl", true); hot.putInt("Ho", 10_000); hot.putInt("Fc", 10_000); hot.putInt("Ci", 5000);
        found = (List<?>) call(analyzer, "analyze", samples(List.of(sample(3000, 4600, 5000, 600, 600), hot)), List.of(), 3030L, false, List.of());
        h.assertTrue(codes(found).get(0).equals("coolant_hot_full"), "hot full: " + codes(found));
        // a chamber in an unloaded chunk
        CompoundTag lost = sample(4020, 3000, 3800, 600, 100);
        lost.putInt("C", 6); lost.putInt("U", 2); lost.putInt("Dr", 9);
        found = (List<?>) call(analyzer, "analyze", samples(List.of(sample(4000, 3000, 3000, 600, 600), lost)), List.of(), 4030L, false, List.of());
        h.assertTrue(codes(found).get(0).equals("chunk_boundary") && codes(found).contains("chamber_lost"), "chunks: " + codes(found));
        // an overvoltage blast next to the reactor
        Object blast = create(common("reactor.audit.ReactorAccidentAnalyzer$Blast"), 1, 2, 3, 4990L);
        found = (List<?>) call(analyzer, "analyze", samples(List.of(sample(5000, 1000, 1000, 0, 0))), List.of(), 5010L, false, List.of(blast));
        h.assertTrue(codes(found).contains("grid_blast"), "grid blast: " + codes(found));
        // removal, and no data at all
        found = (List<?>) call(analyzer, "analyze", samples(List.of(sample(6000, 3000, 3000, 0, 0))), List.of(), 6010L, true, List.of());
        h.assertTrue(codes(found).get(0).equals("removed") && args(found.get(0)).equals(List.of("30")), "removed: " + codes(found));
        found = (List<?>) call(analyzer, "analyze", List.of(), List.of(), 7000L, false, List.of());
        h.assertTrue(codes(found).equals(List.of("unknown")), "no data: " + codes(found));
        h.succeed();
    }

    /** An incident is saved with its blackbox and causes; the dropped blackbox carries its id and survives. */
    @GameTest(batch = "round1112_audit", template = "empty", timeoutTicks = 40)
    public static void incidentLogAndBlackboxItem(GameTestHelper h) {
        Class<?> boxType = common("reactor.audit.ReactorBlackbox"), incidents = common("reactor.audit.ReactorIncidents");
        Object box = create(boxType);
        Object layout = call(boxType, "loadLayout", new ListTag());
        for (Object s : samples(List.of(sample(100, 1000, 2000, 1500, 500), sample(120, 2000, 3000, 1500, 500), sample(140, 3000, 4000, 1500, 500))))
            call(box, "record", s, layout, List.of());
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        Object facts = create(common("reactor.audit.ReactorIncidents$Facts"), "local", 150L, player.getUUID(), "Tester", 6000, "generator", 1, 10_000L, 10_000L);
        BlockPos at = h.absolutePos(new BlockPos(2, 1, 2));
        int id = (Integer) call(incidents, "record", h.getLevel(), at, box, facts);
        h.assertTrue(id > 0, "no incident id");
        CompoundTag incident = (CompoundTag) call(incidents, "get", h.getLevel().getServer(), id);
        h.assertTrue(incident != null && incident.getString("Type").equals("local") && incident.getList("Samples", 10).size() == 3,
            "incident not saved: " + incident);
        h.assertTrue(incident.getList("Layout", 10).size() == 54 && incident.getInt("X") == at.getX(), "layout or position");
        h.assertTrue(call(incidents, "mainCause", incident).equals("heat_runaway"), "main cause " + call(incidents, "mainCause", incident));
        h.assertTrue((Boolean) call(incidents, "mayView", player, incident), "the owner may not view the incident");
        List<?> all = (List<?>) call(incidents, "all", h.getLevel().getServer());
        h.assertTrue(((CompoundTag) all.get(0)).getInt("Id") == id, "newest incident not first");
        call(incidents, "dropBlackbox", h.getLevel(), at, id);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(3),
            e -> BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).equals(rl("reactor/item_reactor_blackbox")));
        h.assertTrue(drops.size() == 1, "blackbox drops: " + drops.size());
        ItemEntity drop = drops.get(0);
        h.assertTrue((Integer) call(incidents, "idOf", drop.getItem()) == id, "the blackbox has the wrong id");
        h.assertTrue(drop.isInvulnerable() && drop.getItem().getItem().isFoil(drop.getItem()), "the blackbox can be destroyed or is not marked");
        drop.discard();
        h.succeed();
    }

    /** With the reactor explosions off, an overheated reactor does not explode but its meltdown is logged once. */
    @GameTest(batch = "round1112_reactor", template = "reactor_loop", timeoutTicks = 40)
    public static void meltdownWithExplosionsOffIsLogged(GameTestHelper h) {
        Object multiplier;
        try {
            multiplier = common("Singularity_Iteration_Config").getField("REACTOR_EXPLOSION_MULTIPLIER").get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        Object old = call(multiplier, "get");
        Class<?> incidents = common("reactor.audit.ReactorIncidents");
        BlockPos pos = new BlockPos(6, 3, 6);
        try {
            call(multiplier, "set", 0.0);
            List<?> before = (List<?>) call(incidents, "all", h.getLevel().getServer());
            int newest = before.isEmpty() ? 0 : ((CompoundTag) before.get(0)).getInt("Id");
            h.setBlock(pos, block("generator/block_nuclear_reactor_generator"));
            h.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
            BlockEntity reactor = h.getBlockEntity(pos);
            IItemHandler items = (IItemHandler) call(reactor, "getItemHandler");
            h.assertTrue(items.insertItem(0, Round33GameTests.item("reactor/item_reactor_uranium_quad"), false).isEmpty(), "fuel rejected");
            call(call(reactor, "getHeatStorage"), "setHeat", 9999L);
            for (int cycle = 0; cycle < 2; cycle++) {
                try {
                    var f = reactor.getClass().getDeclaredField("cycleRemaining");
                    f.setAccessible(true);
                    f.setInt(reactor, 0);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
                call(reactor.getClass(), "tick", h.getLevel(), reactor.getBlockPos(), reactor.getBlockState(), reactor);
            }
            h.assertTrue(h.getBlockEntity(pos) == reactor && !(Boolean) call(reactor, "hasReactorAccident"), "the reactor exploded with explosions off");
            List<?> after = (List<?>) call(incidents, "all", h.getLevel().getServer());
            long logged = after.stream().filter(t -> ((CompoundTag) t).getInt("Id") > newest).count();
            h.assertTrue(logged == 1, "contained meltdowns logged: " + logged);
            h.assertTrue(((CompoundTag) after.get(0)).getString("Type").equals("contained"), "kind " + ((CompoundTag) after.get(0)).getString("Type"));
            items.extractItem(0, 64, false);
            call(call(reactor, "getHeatStorage"), "setHeat", 0L);
        } finally {
            call(multiplier, "set", old);
            h.setBlock(pos.east(), Blocks.AIR);
            h.setBlock(pos, Blocks.AIR);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ assets and languages

    /** The blackbox item, its model and texture, the guide page and the new keys in all three languages. */
    @GameTest(batch = "round1112_assets", template = "empty", timeoutTicks = 20)
    public static void blackboxAssetsAndLanguages(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(rl("reactor/item_reactor_blackbox")), "blackbox item missing");
        ItemStack blank = Round33GameTests.item("reactor/item_reactor_blackbox");
        h.assertTrue(blank.getMaxStackSize() == 1 && !blank.getItem().isFoil(blank), "blank blackbox stack");
        h.assertTrue(Round33GameTests.resource("/assets/mio_icif/models/item/reactor/item_reactor_blackbox.json") != null, "model missing");
        try {
            var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
            h.assertTrue(java.nio.file.Files.exists(file.findResource("assets/mio_icif/textures/item/reactor/item_reactor_blackbox.png")), "texture missing");
        } catch (RuntimeException e) {
            throw new AssertionError("texture lookup", e);
        }
        for (String folder : new String[]{"", "_zh_cn/", "_ja_jp/"})
            h.assertTrue(Round33GameTests.resource("/assets/mio_icif/guides/mio_icif/manual/" + folder + "machines/reactor_audit.md") != null,
                "guide page missing in " + folder);
        List<String> missing = new ArrayList<>();
        String[] keys = {"item.mio_icif.reactor.item_reactor_blackbox", "gui.mio_icif.reactor_audit.title", "command.mio_icif.reactor_audit.header",
            "reactor_audit.cause.coolant_hot_full", "reactor_audit.cause.component_melted.desc", "reactor_audit.cause.grid_blast",
            "reactor_audit.type.contained", "reactor_audit.kind.component_vent", "tooltip.mio_icif.reactor_blackbox.use"};
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
            for (String k : keys) if (json == null || !json.contains("\"" + k + "\"")) missing.add(lang + ":" + k);
        }
        h.assertTrue(missing.isEmpty(), "missing keys: " + missing);
        h.succeed();
    }
}
