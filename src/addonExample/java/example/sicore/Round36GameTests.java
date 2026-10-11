package example.sicore;

import com.miophas.singularity_iteration.core.api.upgrade.tile.IUpgradeItem;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.7.36: datapack tuning values, payload count bounds, the client-to-server rate limit, the
 * upgrade effect recursion fix and the translated transformer message.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round36GameTests {
    private Round36GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    static Object staticField(Class<?> type, String name) {
        try {
            return type.getField(name).get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(type.getSimpleName() + "." + name, e);
        }
    }

    // ------------------------------------------------------------------ tuning

    /** Every declared tuning value has its bundled JSON entry, and the code default is equal to it. */
    @GameTest(batch = "round36_tuning", template = "empty", timeoutTicks = 20)
    public static void tuningDefaultsMatchBundledFiles(GameTestHelper h) {
        Class<?> tuning = common("tuning.SiTuning");
        common("tuning.SiTuningValues");          // Class.forName initializes the handle table
        List<?> declared = (List<?>) call(tuning, "declared");
        h.assertTrue(declared.size() >= 10, "declared tuning values: " + declared.size());
        for (Object v : declared) {
            Object file = readField(v, "file");
            String key = (String) readField(v, "key");
            double def = (Double) call(v, "defaultValue");
            Double loaded = (Double) call(tuning, "loaded", file, key);
            h.assertTrue(loaded != null, "no bundled value for " + file + "/" + key);
            h.assertTrue(Math.abs(loaded - def) < 1e-9, file + "/" + key + ": file " + loaded + " != code " + def);
        }
        h.succeed();
    }

    /** The handles read the loaded values: OV scanner radius 12, cost 96; teleporter cooldown 20. */
    @GameTest(batch = "round36_tuning", template = "empty", timeoutTicks = 20)
    public static void tuningHandlesReadLoadedValues(GameTestHelper h) {
        Class<?> values = common("tuning.SiTuningValues");
        h.assertTrue((Integer) call(staticField(values, "OV_SCAN_RADIUS"), "get") == 12, "OV scan radius");
        h.assertTrue((Integer) call(staticField(values, "OV_ENERGY_PER_USE"), "get") == 96, "OV energy per use");
        h.assertTrue((Integer) call(staticField(values, "OD_SCAN_RADIUS"), "get") == 6, "OD scan radius");
        h.assertTrue((Integer) call(staticField(values, "TELEPORTER_COOLDOWN"), "get") == 20, "teleporter cooldown");
        h.assertTrue((Integer) call(staticField(values, "ARMORY_PLAYER_RANGE"), "get") == 24, "armory player range");
        h.assertTrue((Integer) call(common("tuning.SiTuning"), "generation") >= 1, "tuning was never reloaded");
        h.succeed();
    }

    static Object readField(Object target, String name) {
        try {
            var f = target.getClass().getField(name);
            return f.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    // ------------------------------------------------------------------ payload bounds

    /** readCount refuses negative and oversized counts; writeCount caps the count. */
    @GameTest(batch = "round36_packets", template = "empty", timeoutTicks = 20)
    public static void packetCountsAreBounded(GameTestHelper h) {
        Class<?> limits = common("util.PacketLimits");
        for (int bad : new int[] {-1, 65}) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeVarInt(bad);
            boolean thrown = false;
            try {
                call(limits, "readCount", buf, 64, "test");
            } catch (AssertionError e) {
                // call(...) wraps the DecoderException
                thrown = e.getCause() instanceof io.netty.handler.codec.DecoderException;
            }
            h.assertTrue(thrown, "count " + bad + " was accepted");
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        h.assertTrue((Integer) call(limits, "writeCount", buf, 100, 64) == 64, "writeCount did not cap");
        h.assertTrue((Integer) call(limits, "readCount", buf, 64, "test") == 64, "capped count does not read back");
        h.succeed();
    }

    /** A machine status packet larger than the limit encodes the capped count and decodes with no bytes left. */
    @GameTest(batch = "round36_packets", template = "empty", timeoutTicks = 20)
    @SuppressWarnings("unchecked")
    public static void machineStatusCodecLeavesNoBytes(GameTestHelper h) {
        Class<?> type = common("machine.MachineStatusPacket");
        int max = (Integer) staticField(type, "MAX_ENTRIES");
        long[] pos = new long[max + 10];
        byte[] st = new byte[max + 10];
        Object packet;
        try {
            packet = type.getConstructor(long[].class, byte[].class).newInstance(pos, st);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        var codec = (StreamCodec<FriendlyByteBuf, Object>) staticField(type, "CODEC");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, packet);
        Object back = codec.decode(buf);
        long[] decoded = (long[]) call(back, "positions");
        h.assertTrue(decoded.length == max, "decoded " + decoded.length);
        h.assertTrue(buf.readableBytes() == 0, "bytes left: " + buf.readableBytes());
        h.succeed();
    }

    /** The showcase list of the console sync packet is capped by the encoder, not by a lossy decoder clamp. */
    @GameTest(batch = "round36_packets", template = "empty", timeoutTicks = 20)
    @SuppressWarnings("unchecked")
    public static void armorySyncCodecCapsShowcases(GameTestHelper h) {
        Class<?> type = common("armory.ArmoryConsoleSyncPacket");
        Class<?> view = common("armory.ArmoryRemoteService$ShowcaseView");
        int max = (Integer) staticField(type, "MAX_SHOWCASES");
        List<Object> views = new java.util.ArrayList<>();
        try {
            var ctor = view.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            for (int i = 0; i < max + 5; i++)
                views.add(ctor.newInstance(new net.minecraft.core.BlockPos(i, 0, 0), "x".repeat(80), List.of()));
            var packetCtor = type.getDeclaredConstructors()[0];
            Object packet = packetCtor.newInstance(true, null, views, net.minecraft.network.chat.Component.empty());
            var codec = (StreamCodec<RegistryFriendlyByteBuf, Object>) staticField(type, "CODEC");
            var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
            codec.encode(buf, packet);
            Object back = codec.decode(buf);
            h.assertTrue(((List<?>) call(back, "showcases")).size() == max, "showcase count");
            h.assertTrue(buf.readableBytes() == 0, "bytes left: " + buf.readableBytes());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ rate limit

    /** The maneuver input bucket allows a burst of 10, then refuses until ticks pass. */
    @GameTest(batch = "round36_packets", template = "empty", timeoutTicks = 20)
    public static void rateLimitDropsFloods(GameTestHelper h) {
        ServerPlayer player = Round33GameTests.survivalPlayer(h);
        try {
            Class<?> limit = common("util.PacketRateLimit");
            Object bucket = staticField(limit, "MANEUVER_INPUT");
            int allowed = 0;
            for (int i = 0; i < 50; i++) if ((Boolean) call(limit, "allow", player, bucket)) allowed++;
            h.assertTrue(allowed == 10, "allowed in one tick: " + allowed);
        } finally {
            Round33GameTests.remove(player);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ fixes

    /** The default upgrade effect calls the outer item methods (it recursed into itself before 0.1.7.36). */
    @GameTest(batch = "round36_fixes", template = "empty", timeoutTicks = 20)
    public static void upgradeEffectDoesNotRecurse(GameTestHelper h) {
        IUpgradeItem item = new IUpgradeItem() {
            @Override public boolean isSuitableFor(ItemStack stack, Set<com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty> properties) { return true; }
            @Override public boolean onTick(ItemStack stack, com.miophas.singularity_iteration.core.api.upgrade.tile.IUpgradableBlock block) { return false; }
            @Override public Collection<ItemStack> onProcessEnd(ItemStack stack, com.miophas.singularity_iteration.core.api.upgrade.tile.IUpgradableBlock block, Collection<ItemStack> outputs) { return outputs; }
            @Override public long getExtraCapacity() { return 1234; }
            @Override public boolean hasAutoEject() { return true; }
        };
        var effect = item.getUpgradeEffect(ItemStack.EMPTY);
        h.assertTrue(effect.getExtraCapacity() == 1234, "extra capacity");
        h.assertTrue(effect.hasAutoEject(), "auto eject");
        h.assertTrue(effect.getExtraOutputSlots() == item.getExtraOutputSlots(), "output slots");
        h.assertTrue(effect.getTierUpgrade() == item.getTierUpgrade(), "tier upgrade");
        h.succeed();
    }

    /** The transformer mode message is a translation key in all three languages. */
    @GameTest(batch = "round36_fixes", template = "empty", timeoutTicks = 20)
    public static void transformerMessageIsTranslated(GameTestHelper h) {
        for (String lang : new String[] {"en_us", "zh_cn", "ja_jp"}) {
            String json = Round33GameTests.resource("assets/mio_icif/lang/" + lang + ".json");
            h.assertTrue(json != null, lang + " missing");
            for (String key : new String[] {"message.mio_icif.transformer.mode_switched", "gui.mio_icif.transformer.mode.step_up",
                    "gui.mio_icif.transformer.mode.step_down", "gui.mio_icif.transformer.mode.redstone_control"})
                h.assertTrue(json.contains("\"" + key + "\""), lang + " lacks " + key);
        }
        h.succeed();
    }
}
