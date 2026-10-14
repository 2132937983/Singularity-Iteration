package example.sicore;

import com.miophas.singularity_iteration.core.api.armory.ArmoryEvent;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.2 (quantum phase Q3, quantum command): the order of a Wingman and its save
 * data (older saves load without an order), the individual mode against the squad mode, the
 * command link rule (entanglement relay), the move / focus / form-up orders, the public command
 * event, the quantum recall, the roster of unloaded Wingmen and the holomap Mk.II.
 * The SI common classes are reached by reflection (the addon module may only use the core API).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1102GameTests {
    private static final String ENTITY = "armory.WingmanEntity", COMMANDS = "armory.WingmanCommands", SERVICE = "armory.WingmanService";
    private static final UUID ALL = new UUID(0L, 0L);
    private static final byte A_MOVE = 0, A_FOCUS = 3, A_FORM_UP = 4, A_DATALINK = 8;
    /** Set by a test to cancel every command event (the listener is registered once). */
    private static volatile boolean cancelCommands;
    private static boolean listening;

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object constant(String enumClass, String name) {
        return Enum.valueOf((Class) Round33GameTests.common(enumClass), name);
    }

    /** A Wingman of {@code owner} in the test level (no Armory items: the suit is empty). */
    static Entity wingman(GameTestHelper h, ServerPlayer owner, Vec3 rel) {
        try {
            int pieces = ((Object[]) Round33GameTests.common("armory.ArmoryPiece").getField("VALUES").get(null)).length;
            ItemStack[] items = new ItemStack[pieces];
            java.util.Arrays.fill(items, ItemStack.EMPTY);
            GlobalPos armory = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(0, 1, 0)));
            Entity w = (Entity) Round33GameTests.call(Round33GameTests.common(ENTITY), "create", h.getLevel(), owner, armory, 0,
                constant("armory.WingmanMode", "OFFENSIVE"), items, h.absoluteVec(rel));
            h.getLevel().addFreshEntity(w);
            return w;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("wingman", e);
        }
    }

    /** A survival player wearing a charged quantum chestplate with an entanglement relay (the command link). */
    static ServerPlayer commander(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        p.setItemSlot(EquipmentSlot.CHEST, Round1101GameTests.piece("chestplate", "ENTANGLEMENT_RELAY"));
        return p;
    }

    static Object command(byte action, UUID unit, byte arg, Vec3 point, int target) {
        try {
            Class<?> type = Round33GameTests.common("armory.WingmanCommandPackets$Command");
            Constructor<?> c = type.getDeclaredConstructors()[0];
            return c.newInstance(action, unit, arg, point.x, point.y, point.z, target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("command", e);
        }
    }

    static void handle(ServerPlayer p, Object command) {
        Round33GameTests.call(Round33GameTests.common(COMMANDS), "handle", p, command);
    }

    static String order(Entity w) {
        return ((Enum<?>) Round33GameTests.call(w, "order")).name();
    }

    static String mode(Entity w) {
        return ((Enum<?>) Round33GameTests.call(w, "mode")).name();
    }

    /** An order and the individual mode survive a save; a 0.1.10.1 save (no keys) loads without an order. */
    @GameTest(template = "empty", batch = "round1102_orders", timeoutTicks = 20)
    public static void orderSurvivesSave(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Entity w = wingman(h, owner, new Vec3(1.5, 4, 1.5));
        Vec3 point = h.absoluteVec(new Vec3(5.5, 2, 5.5));
        Round33GameTests.call(w, "order", constant("armory.WingmanOrder", "HOLD"), point, null);
        Round33GameTests.call(w, "setMode", constant("armory.WingmanMode", "ESCORT"), true);
        CompoundTag tag = w.saveWithoutId(new CompoundTag());
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wingman"));
        Entity copy = type.create(h.getLevel());
        copy.load(tag);
        h.assertTrue("HOLD".equals(order(copy)) && point.equals(Round33GameTests.call(copy, "orderPos")), "order lost: " + order(copy));
        h.assertTrue((Boolean) Round33GameTests.call(copy, "individual") && "ESCORT".equals(mode(copy)), "individual mode lost");
        CompoundTag old = tag.copy();
        old.remove("Order");
        old.remove("OrderX");
        old.remove("Individual");
        Entity legacy = type.create(h.getLevel());
        legacy.load(old);
        h.assertTrue("NONE".equals(order(legacy)) && !(Boolean) Round33GameTests.call(legacy, "individual"), "an old save got an order");
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** The squad mode changes every Wingman except one with an individual mode. */
    @GameTest(template = "empty", batch = "round1102_orders", timeoutTicks = 20)
    public static void individualModeKeepsItsMode(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Entity a = wingman(h, owner, new Vec3(1.5, 4, 1.5)), b = wingman(h, owner, new Vec3(3.5, 4, 1.5));
        Round33GameTests.call(a, "setMode", constant("armory.WingmanMode", "ESCORT"), true);
        Round33GameTests.call(Round33GameTests.common(SERVICE), "setMode", owner, constant("armory.WingmanMode", "DISPERSAL"));
        h.assertTrue("ESCORT".equals(mode(a)) && "DISPERSAL".equals(mode(b)), "modes " + mode(a) + " / " + mode(b));
        a.discard();
        b.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** No command without the relay; with it: move, focus and form up reach the Wingman. */
    @GameTest(template = "empty", batch = "round1102_command", timeoutTicks = 20)
    public static void commandNeedsTheRelay(GameTestHelper h) {
        ServerPlayer plain = Round33GameTests.survivalPlayer(h);
        Entity w0 = wingman(h, plain, new Vec3(1.5, 4, 1.5));
        Vec3 point = h.absoluteVec(new Vec3(6.5, 2, 6.5));
        handle(plain, command(A_MOVE, ALL, (byte) 0, point, -1));
        h.assertTrue("NONE".equals(order(w0)), "an order without the relay");
        w0.discard();
        Round33GameTests.remove(plain);

        ServerPlayer p = commander(h);
        h.assertTrue((Boolean) Round33GameTests.call(Round33GameTests.common(COMMANDS), "commander", p), "the relay gives no command link");
        Entity w = wingman(h, p, new Vec3(1.5, 4, 1.5));
        handle(p, command(A_MOVE, ALL, (byte) 0, point, -1));
        h.assertTrue("MOVE".equals(order(w)) && point.equals(Round33GameTests.call(w, "orderPos")), "move order " + order(w));
        Zombie zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(4.5, 2, 1.5));
        handle(p, command(A_FOCUS, w.getUUID(), (byte) 0, Vec3.ZERO, zombie.getId()));
        h.assertTrue("FOCUS".equals(order(w)) && (Integer) Round33GameTests.call(w, "orderTargetId") == zombie.getId(), "focus order " + order(w));
        handle(p, command(A_FORM_UP, ALL, (byte) 0, Vec3.ZERO, -1));
        h.assertTrue("NONE".equals(order(w)), "form up left " + order(w));
        handle(p, command(A_DATALINK, ALL, (byte) 0, Vec3.ZERO, -1));
        h.assertTrue(!(Boolean) Round33GameTests.call(Round33GameTests.common(COMMANDS), "datalink", p), "the data link did not switch off");
        w.discard();
        Round33GameTests.remove(p);
        h.succeed();
    }

    /** A canceled ArmoryEvent.WingmanCommand refuses the order. */
    @GameTest(template = "empty", batch = "round1102_api", timeoutTicks = 20)
    public static void commandEventCancels(GameTestHelper h) {
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener((ArmoryEvent.WingmanCommand e) -> { if (cancelCommands) e.setCanceled(true); });
        }
        ServerPlayer p = commander(h);
        Entity w = wingman(h, p, new Vec3(1.5, 4, 1.5));
        cancelCommands = true;
        try {
            handle(p, command(A_MOVE, ALL, (byte) 0, h.absoluteVec(new Vec3(6.5, 2, 6.5)), -1));
        } finally {
            cancelCommands = false;
        }
        h.assertTrue("NONE".equals(order(w)), "a canceled order applied");
        w.discard();
        Round33GameTests.remove(p);
        h.succeed();
    }

    /** The quantum recall sends a loaded Wingman home at once and then cools down. */
    @GameTest(template = "empty", batch = "round1102_command", timeoutTicks = 20)
    public static void quantumRecallSendsHome(GameTestHelper h) {
        ServerPlayer p = commander(h);
        Entity w = wingman(h, p, new Vec3(1.5, 4, 1.5));
        int first = (Integer) Round33GameTests.call(Round33GameTests.common(COMMANDS), "quantumRecall", p);
        h.assertTrue(first == 1 && w.isRemoved(), "recall reached " + first + ", removed " + w.isRemoved());
        Entity again = wingman(h, p, new Vec3(1.5, 4, 1.5));
        int second = (Integer) Round33GameTests.call(Round33GameTests.common(COMMANDS), "quantumRecall", p);
        h.assertTrue(second == 0 && !again.isRemoved(), "the cooldown did not hold");
        again.discard();
        Round33GameTests.remove(p);
        h.succeed();
    }

    /** The roster keeps an unloaded Wingman across a save and forgets it on removal. */
    @GameTest(template = "empty", batch = "round1102_roster", timeoutTicks = 20)
    public static void rosterRoundTrip(GameTestHelper h) {
        Class<?> roster = Round33GameTests.common("armory.WingmanRoster");
        Object data = Round33GameTests.call(roster, "get", h.getLevel().getServer());
        UUID wingman = UUID.randomUUID(), owner = UUID.randomUUID();
        try {
            Class<?> entryType = Round33GameTests.common("armory.WingmanRoster$Entry");
            Object entry = entryType.getDeclaredConstructors()[0].newInstance(wingman, owner, h.getLevel().dimension(), new Vec3(100, 64, -40), 2);
            Round33GameTests.call(data, "put", entry);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("entry", e);
        }
        h.assertTrue(((List<?>) Round33GameTests.call(data, "of", owner)).size() == 1, "the roster lost the entry");
        CompoundTag saved = (CompoundTag) Round33GameTests.call(data, "save", new CompoundTag(), h.getLevel().registryAccess());
        Object loaded = Round33GameTests.call(roster, "load", saved, h.getLevel().registryAccess());
        List<?> back = new ArrayList<>((List<?>) Round33GameTests.call(loaded, "of", owner));
        h.assertTrue(back.size() == 1 && wingman.equals(Round33GameTests.call(back.get(0), "wingman"))
            && (Integer) Round33GameTests.call(back.get(0), "set") == 2, "the saved roster " + back);
        Round33GameTests.call(data, "remove", wingman);
        h.assertTrue(((List<?>) Round33GameTests.call(data, "of", owner)).isEmpty(), "the entry stayed");
        h.succeed();
    }

    /** The holomap has a Mk.II item now (the 3D holo table). */
    @GameTest(template = "empty", batch = "round1102_api")
    public static void holomapMk2Exists(GameTestHelper h) {
        h.assertTrue(!Round33GameTests.item("module/item_module_holomap_mk2").isEmpty(), "no holomap Mk.II item");
        h.assertTrue((Boolean) Round33GameTests.call(constant("suit.SuitModuleType", "HOLOMAP"), "quantumMk2"), "the holomap is not a quantum Mk.II");
        h.succeed();
    }
}
