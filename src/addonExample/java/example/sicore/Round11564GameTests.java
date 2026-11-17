package example.sicore;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.64 (hit feel and constructivist FX phase Q3, decision five A): the Clean Slate ring: the places lie on a circle
 * 12 blocks above the owner, evenly spaced and at least 4 blocks out; a Wingman climbs to its place, bursts, and its pieces
 * fly home as straight echoes while the items are put away.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11564GameTests {
    private Round11564GameTests() {}

    private static final Class<?> SLATE = Round33GameTests.common("armory.WingmanCleanSlate");

    @GameTest(template = "empty", batch = "round11564_ring", timeoutTicks = 20)
    public static void ringPlacesAreEvenAndHigh(GameTestHelper h) {
        Vec3 owner = new Vec3(10, 64, -5);
        for (int n : new int[]{1, 3, 8}) {
            double r = (Double) Round33GameTests.call(SLATE, "ringRadius", n);
            h.assertTrue(r >= 4, "radius " + r + " for " + n);
            for (int i = 0; i < n; i++) {
                Vec3 p = (Vec3) Round33GameTests.call(SLATE, "ringSlot", owner, i, n);
                h.assertTrue(Math.abs(p.y - owner.y - 12) < 1e-9, "not 12 blocks up");
                h.assertTrue(Math.abs(Math.hypot(p.x - owner.x, p.z - owner.z) - r) < 1e-9, "not on the ring");
                if (n > 1) {
                    Vec3 q = (Vec3) Round33GameTests.call(SLATE, "ringSlot", owner, (i + 1) % n, n);
                    double chord = 2 * r * Math.sin(Math.PI / n);
                    h.assertTrue(Math.abs(p.distanceTo(q) - chord) < 1e-6, "uneven spacing for " + n);
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11564_burst", timeoutTicks = 40)
    public static void burstSendsStraightPiecesHome(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 2, 2.5));
        w.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        w.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        Round33GameTests.call(SLATE, "request", owner);
        Round33GameTests.call(SLATE, "request", owner);
        Vec3 burstAt = null;
        for (int i = 0; i < 200 && !w.isRemoved(); i++) {
            burstAt = w.position();
            Round33GameTests.call(w, "tickCleanSlate");
        }
        h.assertTrue(w.isRemoved(), "no burst");
        List<Entity> pieces = h.getLevel().getEntities((Entity) null, new AABB(burstAt, burstAt).inflate(6),
            e -> e.getType().toString().contains("piece") || e.getClass().getSimpleName().contains("ArmoryPiece"));
        long straight = pieces.stream().filter(e -> (Boolean) Round33GameTests.call(e, "isStraight")).count();
        h.assertTrue(straight == 2, "straight echoes: " + straight + " of " + pieces.size());
        int kept = 0;
        for (ItemStack s : owner.getInventory().items) if (s.is(Items.IRON_CHESTPLATE) || s.is(Items.IRON_HELMET)) kept++;
        h.assertTrue(kept == 2, "the items went missing: " + kept);
        for (Entity e : pieces) e.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }
}
