package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.223 (Wingman cave overhaul G4): what a Wingman knows. A husk behind rock that hunts nobody is unknown; one the
 * Wingman saw and that went behind a wall is remembered at its old place and counts as lost; what one Wingman sees, the
 * other Wingmen of its owner know; a hunter behind glass, known but unseen and without a path, is rejected as a target
 * (decision five A). The Wingmen have no AI here; the common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111223GameTests {
    private Round111223GameTests() {}

    private static Mob still(GameTestHelper h, ServerPlayer owner, Vec3 at) {
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, at);
        w.setNoAi(true);
        return w;
    }

    private static ServerPlayer owner(GameTestHelper h, Vec3 at) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        p.moveTo(h.absoluteVec(at));
        return p;
    }

    private static Object perception(Mob w) { return Round33GameTests.call(w, "perception"); }

    private static void update(GameTestHelper h, Mob w, ServerPlayer owner) {
        Round33GameTests.call(perception(w), "update", owner, owner.position(), 24.0);
    }

    private static boolean known(Mob w, Zombie z) { return (Boolean) Round33GameTests.call(perception(w), "known", z); }

    private static void cleanup(GameTestHelper h, ServerPlayer owner, net.minecraft.world.entity.Entity... es) {
        for (var e : es) e.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    @GameTest(template = "cave", batch = "round111223_rock", timeoutTicks = 40)
    public static void rockHidesAMobThatHuntsNobody(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 4, 30, 4, 6);
        Round111220GameTests.carve(h, 2, 9, 4, 30, 11, 6);
        ServerPlayer owner = owner(h, new Vec3(3.5, 2, 5.5));
        Mob w = still(h, owner, new Vec3(6.5, 2, 5.5));
        Zombie above = Round111220GameTests.zombie(h, 9.5, 9, 5.5, null);
        Zombie ahead = Round111220GameTests.zombie(h, 20.5, 2, 5.5, null);
        update(h, w, owner);
        h.assertTrue(!known(w, above), "a husk behind 4 blocks of rock is known");
        h.assertTrue(known(w, ahead), "a husk down the open tunnel is not known");
        cleanup(h, owner, w, above, ahead);
    }

    @GameTest(template = "cave", batch = "round111223_memory", timeoutTicks = 60)
    public static void aHiddenMobIsRememberedWhereItWasSeen(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 4, 30, 4, 6);
        Round111220GameTests.carve(h, 20, 2, 8, 22, 4, 8);   // a closed pocket beside the tunnel
        ServerPlayer owner = owner(h, new Vec3(3.5, 2, 5.5));
        Mob w = still(h, owner, new Vec3(6.5, 2, 5.5));
        Zombie z = Round111220GameTests.zombie(h, 21.5, 2, 5.5, null);
        update(h, w, owner);
        h.assertTrue(known(w, z), "a husk in sight is not known");
        Vec3 seenAt = z.position();
        Vec3 pocket = h.absoluteVec(new Vec3(21.5, 2, 8.5));
        z.teleportTo(pocket.x, pocket.y, pocket.z);
        h.runAfterDelay(6, () -> {
            update(h, w, owner);
            h.assertTrue(known(w, z), "a husk that went behind a wall is forgotten at once");
            Object c = Round33GameTests.call(perception(w), "contact", z);
            Vec3 pos = (Vec3) Round33GameTests.call(c, "pos");
            h.assertTrue(pos.distanceTo(seenAt) < 0.01, "the husk is remembered at " + pos + ", not where it was seen (" + seenAt + ")");
            h.assertTrue((Boolean) Round33GameTests.call(perception(w), "lost", z), "the hidden husk does not count as lost");
            cleanup(h, owner, w, z);
        });
    }

    @GameTest(template = "cave", batch = "round111223_squad", timeoutTicks = 40)
    public static void theSquadSharesWhatItSees(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 4, 30, 4, 6);       // the tunnel with the husk and Wingman A
        Round111220GameTests.carve(h, 2, 2, 8, 6, 4, 8);        // a closed room: the owner and Wingman B
        ServerPlayer owner = owner(h, new Vec3(3.5, 2, 8.5));
        Mob a = still(h, owner, new Vec3(10.5, 2, 5.5));
        Mob b = still(h, owner, new Vec3(5.5, 2, 8.5));
        Zombie z = Round111220GameTests.zombie(h, 24.5, 2, 5.5, null);
        update(h, a, owner);
        update(h, b, owner);
        h.assertTrue(known(a, z), "Wingman A does not see the husk down its tunnel");
        h.assertTrue(known(b, z), "Wingman B, walled in with the owner, does not learn of the husk A sees");
        cleanup(h, owner, a, b, z);
    }

    @GameTest(template = "cave", batch = "round111223_glass", timeoutTicks = 40)
    public static void aHunterBehindGlassIsNoTarget(GameTestHelper h) {
        Round111220GameTests.shell(h, true);
        Round111220GameTests.carve(h, 2, 2, 4, 20, 4, 6);
        Round111220GameTests.fill(h, 21, 2, 4, 21, 4, 6, Blocks.GLASS.defaultBlockState());
        Round111220GameTests.carve(h, 22, 2, 4, 26, 4, 6);
        ServerPlayer owner = owner(h, new Vec3(3.5, 2, 5.5));
        Mob w = still(h, owner, new Vec3(6.5, 2, 5.5));
        Zombie z = Round111220GameTests.zombie(h, 24.5, 2, 5.5, owner);
        update(h, w, owner);
        h.assertTrue(known(w, z), "a husk that hunts the owner is not known");
        h.assertTrue((Boolean) Round33GameTests.call(perception(w), "rejects", z), "a hunter behind glass (no path, unseen) is a target");
        cleanup(h, owner, w, z);
    }
}
