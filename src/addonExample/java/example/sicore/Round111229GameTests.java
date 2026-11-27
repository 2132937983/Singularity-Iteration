package example.sicore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.229 (Wingman motion, AN1 part 2): the combat actions. Every strike lands on the impact tick of its keyframe
 * animation and lasts as long as it (so the blow and the picture meet); the client picks the bow's release and draw, a gun's
 * recoil and aim, the crouch and lean of a Wingman in cover, the glare, and nothing for a blade; a block at the waist towards
 * the target is cover with the open side to lean out of; in the open a Wingman dives onto a target from above, and on foot in
 * a low hall it strikes with the wind-up. The common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111229GameTests {
    private Round111229GameTests() {}

    private static Class<?> strikes() { return Round33GameTests.common("armory.WingmanStrikes"); }

    private static JsonObject animations() {
        try (InputStream in = strikes().getResourceAsStream("/assets/mio_icif/animations/entity/wingman.animation.json")) {
            if (in == null) throw new AssertionError("missing wingman.animation.json");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    @GameTest(template = "empty", batch = "round111229_timing", timeoutTicks = 20)
    public static void strikesLandOnTheirImpactFrame(GameTestHelper h) {
        JsonObject anims = animations();
        Class<?> action = Round33GameTests.common("armory.WingmanStrikes$Action");
        for (Object a : action.getEnumConstants()) {
            String name = ((Enum<?>) a).name();
            try {
                String anim = (String) action.getField("animation").get(a);
                if (anim == null) continue;
                int impact = action.getField("impact").getInt(a), length = action.getField("length").getInt(a);
                JsonObject an = anims.getAsJsonObject("animation.wingman." + anim);
                h.assertTrue(an != null, name + ": no animation " + anim);
                int len = (int) Math.round(an.get("animation_length").getAsDouble() * 20);
                boolean loops = an.has("loop") && an.get("loop").isJsonPrimitive() && an.get("loop").getAsJsonPrimitive().isBoolean()
                    && an.get("loop").getAsBoolean();
                // a looping one (the slam's fall) lasts as long as the action, its cycle is shorter
                h.assertTrue(loops || Math.abs(len - length) <= 1, name + " lasts " + length + " ticks, its animation " + len);
                if (an.has("timeline")) {
                    for (var e : an.getAsJsonObject("timeline").entrySet()) {
                        String marker = e.getValue().getAsString().replace(";", "").trim();
                        if (!marker.equals("impact")) continue;
                        int at = (int) Math.round(Double.parseDouble(e.getKey()) * 20);
                        h.assertTrue(Math.abs(at - impact) <= 1, name + " lands at tick " + impact + ", its animation's impact is at " + at);
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(name, e);
            }
        }
        Object step3 = Round33GameTests.call(strikes(), "combo", 2);
        h.assertTrue(((Enum<?>) step3).name().equals("MELEE_3"), "the third cut is " + step3);
        int v = (Integer) Round33GameTests.call(strikes(), "next", 0, Round1102GameTests.constant("armory.WingmanStrikes$Action", "KICK"));
        int w = (Integer) Round33GameTests.call(strikes(), "next", v, Round1102GameTests.constant("armory.WingmanStrikes$Action", "KICK"));
        h.assertTrue(v != w && ((Enum<?>) Round33GameTests.call(strikes(), "kindOf", w)).name().equals("KICK"), "a second kick is not a new action");
        h.succeed();
    }

    private static String act(String hold, boolean heavy, float combat, float shotAge, int cooldown, String action, float sinceAction,
                              float sinceGlare, int cover, boolean onFloor) {
        Object a = Round33GameTests.call(Round33GameTests.common("armory.WingmanKeyframeChoice"), "action",
            Round1102GameTests.constant("armory.WingmanPose$Hold", hold), heavy, combat, shotAge, 100, cooldown,
            Round1102GameTests.constant("armory.WingmanStrikes$Action", action), sinceAction, 50, sinceGlare, 60, (byte) cover, onFloor);
        return a == null ? null : (String) Round33GameTests.call(a, "name");
    }

    @GameTest(template = "empty", batch = "round111229_picks", timeoutTicks = 20)
    public static void theClientPicksTheWeaponMotion(GameTestHelper h) {
        h.assertTrue("bow_release".equals(act("BOW", false, 1, 2, 16, "NONE", 99, 99, 0, false)), "no release after a bow shot");
        h.assertTrue("bow_draw".equals(act("BOW", false, 1, 10, 16, "NONE", 99, 99, 0, false)), "no draw between bow shots");
        h.assertTrue("recoil_heavy".equals(act("RANGED", true, 1, 1, 20, "NONE", 99, 99, 0, false)), "no heavy recoil for a splash gun");
        h.assertTrue("aim_rifle".equals(act("RANGED", false, 1, 10, 20, "NONE", 99, 99, 0, false)), "no aim between shots");
        h.assertTrue("cover_crouch".equals(act("RANGED", false, 1, 8, 30, "NONE", 99, 99, 2, true)), "no crouch in cover between shots");
        h.assertTrue("peek_right".equals(act("RANGED", false, 1, 25, 30, "NONE", 99, 99, 2, true)), "no lean out before a shot from cover");
        h.assertTrue("peek_left".equals(act("RANGED", false, 1, 1, 30, "NONE", 99, 99, 1, true)), "no lean out to the left right after a shot");
        h.assertTrue("melee_2".equals(act("HELD", false, 1, 99, 20, "MELEE_2", 3, 99, 0, false)), "the second cut is not played");
        h.assertTrue(act("HELD", false, 1, 99, 20, "MELEE_2", 30, 99, 0, false) == null, "a cut plays on after its length");
        h.assertTrue("glare".equals(act("HELD", false, 1, 99, 20, "NONE", 99, 2, 0, false)), "no glare at a new lock");
        h.assertTrue(act("BLADE", false, 1, 1, 20, "MELEE_1", 1, 1, 0, false) == null, "a blade plays the keyframe strikes over SlashBlade's");
        h.succeed();
    }

    @GameTest(template = "cave", batch = "round111229_cover", timeoutTicks = 20)
    public static void aBlockAtTheWaistIsCover(GameTestHelper h) {
        Round111220GameTests.shell(h, false);
        Round111220GameTests.fill(h, 10, 2, 5, 10, 2, 5, Blocks.STONE.defaultBlockState());
        Class<?> tactics = Round33GameTests.common("armory.WingmanTactics");
        Vec3 feet = h.absoluteVec(new Vec3(9.5, 2, 5.5)), target = h.absoluteVec(new Vec3(20.5, 3, 5.5));
        byte side = (Byte) Round33GameTests.call(tactics, "coverSide", h.getLevel(), feet, target);
        h.assertTrue(side == 2, "the right side is open, the Wingman leans " + side);
        Round111220GameTests.fill(h, 10, 2, 6, 10, 2, 6, Blocks.STONE.defaultBlockState());   // block its right (facing east: south)
        side = (Byte) Round33GameTests.call(tactics, "coverSide", h.getLevel(), feet, target);
        h.assertTrue(side == 1, "with its right closed the Wingman leans " + side);
        byte none = (Byte) Round33GameTests.call(tactics, "coverSide", h.getLevel(), h.absoluteVec(new Vec3(5.5, 2, 5.5)), target);
        h.assertTrue(none == 0, "open ground counts as cover (" + none + ")");
        h.succeed();
    }

    /** Runs a fight and records the combat actions the Wingman publishes. */
    private static void fight(GameTestHelper h, boolean hall, int ticks, java.util.function.BiConsumer<Set<String>, Zombie> check) {
        Round111220GameTests.shell(h, hall);
        if (hall) Round111220GameTests.carve(h, 2, 2, 3, 30, 6, 7);
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        owner.moveTo(h.absoluteVec(new Vec3(3.5, 2, 5.5)));
        owner.getAbilities().invulnerable = true;
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(6.5, 2.2, 5.5));
        w.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        Round33GameTests.call(w, "setReserve", 1_000_000L);
        Zombie z = Round111220GameTests.zombie(h, 16.5, 2, 5.5, owner);
        z.setHealth(20);
        z.setInvulnerable(false);
        Set<String> seen = new HashSet<>();
        h.onEachTick(() -> seen.add(((Enum<?>) Round33GameTests.call(w, "action")).name()));
        h.runAtTickTime(ticks, () -> {
            float hp = z.getHealth();
            z.discard();
            w.discard();
            Round33GameTests.remove(owner);
            check.accept(seen, z);
            h.assertTrue(hp < 20, "the husk was never hurt (actions " + seen + ")");
            h.succeed();
        });
    }

    @GameTest(template = "cave", batch = "round111229_slam", timeoutTicks = 240)
    public static void inTheOpenItDivesOntoItsTarget(GameTestHelper h) {
        fight(h, false, 220, (seen, z) -> h.assertTrue(seen.contains("SLAM_IMPACT"), "no dive slam in the open (actions " + seen + ")"));
    }

    @GameTest(template = "cave", batch = "round111229_foot", timeoutTicks = 240)
    public static void onFootItStrikesWithAWindUp(GameTestHelper h) {
        fight(h, true, 220, (seen, z) -> {
            h.assertTrue(!seen.contains("SLAM_FALL") && !seen.contains("SLAM_IMPACT"), "a dive slam under a 5-high ceiling (actions " + seen + ")");
            h.assertTrue(seen.stream().anyMatch(a -> a.startsWith("MELEE_") || a.equals("DASH") || a.equals("KICK")),
                "no strike on foot (actions " + seen + ")");
        });
    }

    @GameTest(template = "empty", batch = "round111229_suit", timeoutTicks = 20)
    public static void theSuitAnimationsAreShipped(GameTestHelper h) {
        try (InputStream in = strikes().getResourceAsStream("/assets/mio_icif/animations/entity/suit_player.animation.json")) {
            h.assertTrue(in != null, "missing suit_player.animation.json");
            JsonObject anims = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
            // 0.1.11.229: the animations SuitKeyframes plays
            for (String n : new String[]{"hover", "takeoff", "land_heavy", "boost", "brake", "suit_up", "eject", "command_point", "command_recall"})
                h.assertTrue(anims.has("animation.suit." + n), "missing animation.suit." + n);
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        h.succeed();
    }
}
