package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.39 (Wingman audit W0): a deployed suit keeps its Armory positions (send-home, automation, the store fallback) and
 * gets them back; the limit counts the Wingmen in unloaded chunks; the drop-in point is below a ceiling; a fatal hit sends
 * the Wingman home protected; unpaid upkeep sends it home; a squad mode leaves an individual mode alone; a console recall
 * by entity id; the squad order follows the suit sets. The SI common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11539GameTests {
    private Round11539GameTests() {}

    private static final BlockPos ARMORY = new BlockPos(2, 2, 2);
    private static final String SERVICE = "armory.WingmanService";

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static String key(Object message) {
        return ((Component) message).getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(message);
    }

    private static int slot(int set, String piece) {
        return (Integer) call(Round33GameTests.common("armory.mio_icif_armory"), "slotOf", set, Round1102GameTests.constant("armory.ArmoryPiece", piece));
    }

    /** A powered Armory bound to {@code owner} with an iron helmet in suit 0. */
    private static BlockEntity armory(GameTestHelper h, ServerPlayer owner) {
        h.setBlock(ARMORY, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_armory")));
        BlockEntity armory = h.getBlockEntity(ARMORY);
        ((AbstractEnergyBlockEntity) armory).getEnergyStorageInternal().setEnergy(500_000);
        call(armory, "tryBind", owner);
        call(call(armory, "getItemHandler"), "setStackInSlot", slot(0, "HEAD"), new ItemStack(Items.IRON_HELMET));
        return armory;
    }

    @SuppressWarnings("unchecked")
    private static List<Entity> wingmen(ServerPlayer owner) {
        return (List<Entity>) call(Round33GameTests.common(SERVICE), "of", owner);
    }

    private static Object deploy(ServerPlayer owner, BlockEntity armory) {
        return call(Round33GameTests.common(SERVICE), "deploy", owner, armory, 0, Round1102GameTests.constant("armory.WingmanMode", "ESCORT"));
    }

    /** A Wingman of suit {@code set} (no items, no Armory there). */
    private static Entity wingman(GameTestHelper h, ServerPlayer owner, int set, Vec3 rel) {
        try {
            int pieces = ((Object[]) Round33GameTests.common("armory.ArmoryPiece").getField("VALUES").get(null)).length;
            ItemStack[] items = new ItemStack[pieces];
            java.util.Arrays.fill(items, ItemStack.EMPTY);
            GlobalPos home = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(0, 1, 0)));
            Entity w = (Entity) call(Round33GameTests.common("armory.WingmanEntity"), "create", h.getLevel(), owner, home, set,
                Round1102GameTests.constant("armory.WingmanMode", "OFFENSIVE"), items, h.absoluteVec(rel));
            h.getLevel().addFreshEntity(w);
            return w;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("wingman", e);
        }
    }

    private static byte state(Entity w) { return (Byte) call(w, "state"); }

    /** G-1: the deployed suit is not "free": send-home and automation skip it; the Wingman's items come back to it. */
    @GameTest(template = "empty", batch = "round11539_armory", timeoutTicks = 40)
    public static void deployedSuitKeepsItsPositions(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        BlockEntity armory = armory(h, owner);
        Object result = deploy(owner, armory);
        List<Entity> out = wingmen(owner);
        h.assertTrue(out.size() == 1, "deploy: " + key(result));
        h.assertTrue((Boolean) call(armory, "deployed", 0) && !(Boolean) call(armory, "deployed", 1), "suit 0 is not marked out");
        h.assertTrue((Integer) call(armory, "firstFreeSet") == 1, "send-home would use suit " + call(armory, "firstFreeSet"));
        call(armory, "cycleAutomation", owner);   // LOCKED -> DEPOSIT
        ItemStack helmet = new ItemStack(Items.GOLDEN_HELMET);
        h.assertTrue(!(Boolean) call(armory, "canInsertItem", slot(0, "HEAD"), helmet, null)
            && (Boolean) call(armory, "canInsertItem", slot(1, "HEAD"), helmet, null), "automation may fill the deployed suit");
        ItemStack rest = (ItemStack) call(armory, "store", 0, Round1102GameTests.constant("armory.ArmoryPiece", "HEAD"), helmet);
        h.assertTrue(rest.isEmpty() && ((ItemStack) call(armory, "piece", 0, Round1102GameTests.constant("armory.ArmoryPiece", "HEAD"))).isEmpty(),
            "a stored piece took a position of the deployed suit");
        call(out.get(0), "secure");
        out.get(0).discard();
        ItemStack back = (ItemStack) call(armory, "piece", 0, Round1102GameTests.constant("armory.ArmoryPiece", "HEAD"));
        h.assertTrue(back.is(Items.IRON_HELMET) && !(Boolean) call(armory, "deployed", 0), "the suit did not come back to its place: " + back);
        Round33GameTests.remove(owner);
        h.setBlock(ARMORY, Blocks.AIR);
        h.succeed();
    }

    /** G-2: the Wingmen on the roster (unloaded chunks) count against the limit. */
    @GameTest(template = "empty", batch = "round11539_limit", timeoutTicks = 40)
    public static void limitCountsUnloadedWingmen(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        BlockEntity armory = armory(h, owner);
        Class<?> rosterType = Round33GameTests.common("armory.WingmanRoster");
        Object roster = call(rosterType, "get", h.getLevel().getServer());
        UUID[] far = new UUID[4];
        try {
            Class<?> entryType = Round33GameTests.common("armory.WingmanRoster$Entry");
            for (int i = 0; i < far.length; i++) {
                far[i] = UUID.randomUUID();
                call(roster, "put", entryType.getDeclaredConstructors()[0].newInstance(far[i], owner.getUUID(), h.getLevel().dimension(),
                    new Vec3(5000 + i, 80, 5000), i + 1));
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("entry", e);
        }
        Object result = deploy(owner, armory);
        h.assertTrue("message.mio_icif.wingman.limit".equals(key(result)) && wingmen(owner).isEmpty(), "deployed past the limit: " + key(result));
        h.assertTrue((Integer) call(Round33GameTests.common(SERVICE), "count", owner) == 4, "count");
        for (UUID id : far) call(roster, "remove", id);
        Round33GameTests.remove(owner);
        h.setBlock(ARMORY, Blocks.AIR);
        h.succeed();
    }

    /**
     * G-4: the drop-in point has room for the body. The test arena is closed by a barrier ceiling a little above the
     * owner, so the open column is too short here and the Wingman drops in beside the owner (before: 18 blocks up, in the
     * barrier). With the owner boxed in, there is no point.
     */
    @GameTest(template = "empty", batch = "round11539_drop", timeoutTicks = 20)
    public static void dropInFindsRoom(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);   // feet at y 2 (relative)
        var size = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wingman")).getDimensions();
        Vec3 at = (Vec3) call(Round33GameTests.common(SERVICE), "dropInPoint", owner);
        h.assertTrue(at != null && h.getLevel().noCollision(size.makeBoundingBox(at)) && at.y >= owner.getY() && at.y <= owner.getY() + 18
            && !at.equals(owner.position().add(0, 18, 0)), "drop-in point " + at + " owner " + owner.position());
        BlockPos feet = owner.blockPosition();
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = feet.offset(dx, dy, dz);
                if (dy == 2 || dx != 0 || dz != 0) h.getLevel().setBlock(p, Blocks.STONE.defaultBlockState(), 3);
            }
        }
        h.getLevel().setBlock(feet, Blocks.STONE.defaultBlockState(), 3);   // the owner stands in a block: no room anywhere
        h.assertTrue(call(Round33GameTests.common(SERVICE), "dropInPoint", owner) == null, "a point inside the rock");
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** A fatal hit leaves 1 health and sends the Wingman home, invulnerable; only a kill removes it. */
    @GameTest(template = "empty", batch = "round11539_damage", timeoutTicks = 20)
    public static void fatalHitReturnsProtected(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Entity w = wingman(h, owner, 0, new Vec3(1.5, 4, 1.5));
        var sources = h.getLevel().damageSources();
        w.hurt(sources.generic(), 1000);
        net.minecraft.world.entity.LivingEntity living = (net.minecraft.world.entity.LivingEntity) w;
        h.assertTrue(living.getHealth() == 1.0F && state(w) == 1 && !w.isRemoved(), "health " + living.getHealth() + " state " + state(w));
        w.invulnerableTime = 0;
        h.assertTrue(!w.hurt(sources.generic(), 5), "hurt on the protected way home");
        w.invulnerableTime = 0;
        w.hurt(sources.genericKill(), 1000);
        h.assertTrue(!w.isAlive(), "a kill did not end it");
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /**
     * Upkeep that nobody pays (no electric piece, no Armory, no reserve) sends the Wingman home after the low-power
     * seconds. 0.1.11.46: the upkeep runs directly (a Wingman left to fly can leave the ticking chunks of the test area).
     */
    @GameTest(template = "empty", batch = "round11539_upkeep", timeoutTicks = 20)
    public static void unpaidUpkeepSendsHome(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Entity w = wingman(h, owner, 0, new Vec3(1.5, 4, 1.5));
        for (int second = 1; second <= 4; second++) call(w, "upkeep", h.getLevel(), owner);
        h.assertTrue(state(w) == 0, "returned before 5 s without upkeep");
        call(w, "upkeep", h.getLevel(), owner);
        h.assertTrue(state(w) == 1, "still flying after 5 s without upkeep");
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** G-12: the wheel's squad mode leaves an individual mode alone; a console recall by entity id reaches only that one. */
    @GameTest(template = "empty", batch = "round11539_command", timeoutTicks = 20)
    public static void squadModeAndRecallById(GameTestHelper h) {
        ServerPlayer owner = Round1102GameTests.commander(h);
        Entity a = wingman(h, owner, 0, new Vec3(1.5, 4, 1.5)), b = wingman(h, owner, 1, new Vec3(3.5, 4, 1.5));
        call(a, "setMode", Round1102GameTests.constant("armory.WingmanMode", "ESCORT"), true);
        Round1102GameTests.handle(owner, Round1102GameTests.command((byte) 5, new UUID(0L, 0L), (byte) 1, Vec3.ZERO, -1));   // A_MODE all: DISPERSAL
        h.assertTrue("ESCORT".equals(Round1102GameTests.mode(a)) && (Boolean) call(a, "individual"), "the individual mode was overwritten");
        h.assertTrue("DISPERSAL".equals(Round1102GameTests.mode(b)), "the squad mode did not apply: " + Round1102GameTests.mode(b));
        call(Round33GameTests.common(SERVICE), "recallById", owner, b.getId());
        h.assertTrue(state(b) == 1 && state(a) == 0, "recall by id: a " + state(a) + " b " + state(b));
        a.discard();
        b.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** G-13: the squad order follows the suit sets, whatever order the Wingmen came in. */
    @GameTest(template = "empty", batch = "round11539_slots", timeoutTicks = 20)
    public static void squadOrderFollowsTheSuits(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Entity late = wingman(h, owner, 3, new Vec3(1.5, 4, 1.5)), early = wingman(h, owner, 1, new Vec3(3.5, 4, 1.5));
        h.assertTrue((Integer) call(early, "squadSlot") == 0 && (Integer) call(late, "squadSlot") == 1,
            "slots: suit 1 -> " + call(early, "squadSlot") + ", suit 3 -> " + call(late, "squadSlot"));
        late.discard();
        early.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }
}
