package example.sicore;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.31 (visuals, plan B): the motion rules of the quantum suit (boost jump, hard landing), the flame rules,
 * the physical display of the holo projector (switch, save, the side face rule) and the generated textures. The drawing itself
 * runs on the client and is checked by the offline harness. Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1131GameTests {
    private Round1131GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> common(String name) { return Round33GameTests.common(name); }

    private static Object tracker() {
        try {
            var c = common("client.suit.MotionFxRules$Tracker").getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("tracker", e);
        }
    }

    /** Feeds a vertical motion (vanilla physics) to a tracker; {@code boost}: the quantum boots add 0.3, 0.225, ... while rising. */
    private static int[] jump(boolean boost, double floorAfter) {
        Object t = tracker();
        double y = 0, vy = 0, floor = 0, charge = 1;
        boolean ground = true;
        int jumps = 0, lands = 0;
        for (int tick = 0; tick < 160; tick++) {
            if (boost && !ground && vy >= 0 && charge > 0.05) { vy += charge * 0.3; charge *= 0.75; }
            if (tick == 10) vy = 0.42;
            if (tick == 12) floor = floorAfter;
            double before = y;
            if (y + vy <= floor) { y = floor; ground = true; vy = 0; } else { y += vy; ground = false; }
            vy = (vy - 0.08) * 0.98;
            int e = (int) call(t, "tick", y, y - before, y, ground, false);
            if (e == 1) jumps++;
            if (e == 2) lands++;
        }
        return new int[]{jumps, lands};
    }

    /** A vanilla jump is no boost jump and no hard landing; a boost jump is both; a fall of 5 blocks lands hard. */
    @GameTest(batch = "round1131_motion", template = "empty", timeoutTicks = 20)
    public static void motionRules(GameTestHelper h) {
        int[] vanilla = jump(false, 0), boost = jump(true, 0), fall = jump(false, -5);
        h.assertTrue(vanilla[0] == 0 && vanilla[1] == 0, "vanilla jump: " + vanilla[0] + " jumps, " + vanilla[1] + " landings");
        h.assertTrue(boost[0] == 1 && boost[1] == 1, "boost jump: " + boost[0] + " jumps, " + boost[1] + " landings");
        h.assertTrue(fall[0] == 0 && fall[1] == 1, "jump off 5 blocks: " + fall[0] + " jumps, " + fall[1] + " landings");
        Class<?> rules = common("client.suit.MotionFxRules");
        h.assertTrue((boolean) call(rules, "streaks", true, true, 0.3) && !(boolean) call(rules, "streaks", false, true, 0.3), "sprint streaks");
        h.succeed();
    }

    /** The flame turns from white through blue to violet; a strip faces the camera at the origin. */
    @GameTest(batch = "round1131_motion", template = "empty", timeoutTicks = 20)
    public static void flameRules(GameTestHelper h) {
        Class<?> rules = common("client.suit.FlameRules");
        h.assertTrue((int) call(rules, "colour", 0F) == 0xFFF2E0 && (int) call(rules, "colour", 0.65F) == 0x5AA8FF
            && (int) call(rules, "colour", 1F) == 0xB878FF, "flame colours");
        double[] out = new double[12];
        h.assertTrue((boolean) call(rules, "strip", 1.0, -0.5, -3.0, 1.2, -1.0, -3.1, 0.1, out), "strip");
        double ux = out[3] - out[0], uy = out[4] - out[1], uz = out[5] - out[2], vx = out[6] - out[0], vy = out[7] - out[1], vz = out[8] - out[2];
        double toCam = (uy * vz - uz * vy) * -out[0] + (uz * vx - ux * vz) * -out[1] + (ux * vy - uy * vx) * -out[2];
        h.assertTrue(toCam > 0, "the strip faces away from the camera");
        h.assertTrue((float) call(rules, "sparks", 0.8F) == 0F && (float) call(rules, "sparks", 1F) > 1F, "sparks only at full thrust");
        h.succeed();
    }

    /** A side face switches the projector to the physical display; the mode is saved; the top and sneaking do not switch. */
    @GameTest(batch = "round1131_display", template = "empty", timeoutTicks = 20)
    public static void projectorPhysicalDisplay(GameTestHelper h) {
        Class<?> block = common("armory.ArmorShowcaseBlock");
        h.assertTrue((boolean) call(block, "sideSwitches", Direction.NORTH, false) && !(boolean) call(block, "sideSwitches", Direction.UP, false)
            && !(boolean) call(block, "sideSwitches", Direction.DOWN, false) && !(boolean) call(block, "sideSwitches", Direction.EAST, true),
            "side face rule");
        BlockPos pos = new BlockPos(1, 2, 1), other = new BlockPos(2, 2, 1);
        var state = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_armor_showcase")).defaultBlockState();
        h.setBlock(pos, state);
        h.setBlock(other, state);
        BlockEntity showcase = h.getBlockEntity(pos);
        h.assertTrue(!(boolean) call(showcase, "physical"), "a new projector shows a hologram");
        call(showcase, "toggleDisplay", h.makeMockPlayer(GameType.CREATIVE));
        h.assertTrue((boolean) call(showcase, "physical"), "not switched to the physical display");
        var registries = h.getLevel().registryAccess();
        CompoundTag tag = showcase.saveWithoutMetadata(registries);
        h.assertTrue(tag.getBoolean("Physical"), "mode not saved");
        BlockEntity copy = h.getBlockEntity(other);
        copy.loadWithComponents(tag, registries);
        h.assertTrue((boolean) call(copy, "physical"), "mode not loaded");
        h.succeed();
    }

    /** The generated textures ship: the flame strip (4 frames, grey light) and the visor glass textures. */
    @GameTest(batch = "round1131_display", template = "empty", timeoutTicks = 20)
    public static void texturesShip(GameTestHelper h) {
        BufferedImage flame = image("textures/effect/thruster_flame.png");
        h.assertTrue(flame != null && flame.getWidth() == 64 && flame.getHeight() == 64, "thruster_flame.png");
        int c = flame.getRGB(8, 4);
        h.assertTrue(((c >> 16) & 255) == (c & 255) && (c & 255) > 100, "the flame core is bright grey: " + Integer.toHexString(c));
        for (String name : new String[]{"rim", "fringe", "streak", "drop", "crack"}) {
            h.assertTrue(image("textures/gui/visor/" + name + ".png") != null, name + ".png missing");
        }
        h.succeed();
    }

    private static BufferedImage image(String path) {
        try (InputStream in = Round1131GameTests.class.getClassLoader().getResourceAsStream("assets/mio_icif/" + path)) {
            return in == null ? null : ImageIO.read(in);
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
