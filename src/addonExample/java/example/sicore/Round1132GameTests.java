package example.sicore;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.32 (visuals, plan C): the level of detail and the whole-body pass budget, the hit ripple rules, the iris
 * and gate rules, the hit report of the quantum armour, the Armory icons in its update tag. The drawing itself runs on the
 * client and is checked by the offline harness. Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1132GameTests {
    private Round1132GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> common(String name) { return Round33GameTests.common(name); }

    private static Object make(Class<?> type, Object... args) {
        for (var c : type.getDeclaredConstructors()) {
            if (c.getParameterCount() != args.length) continue;
            try {
                c.setAccessible(true);
                return c.newInstance(args);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(type.getSimpleName(), e);
            }
        }
        throw new AssertionError("no constructor " + type);
    }

    /** Tiers, segments, a budget that grants the nearest; the prepared ripple equals the rule. */
    @GameTest(batch = "round1132_rules", template = "empty", timeoutTicks = 20)
    public static void lodAndRipple(GameTestHelper h) {
        Class<?> lod = common("client.render.FxLodRules");
        h.assertTrue((int) call(lod, "tier", 100.0, true) == 0 && (int) call(lod, "tier", 30.0 * 30, true) == 1
            && (int) call(lod, "tier", 100.0 * 100, true) == 2 && (int) call(lod, "tier", 100.0 * 100, false) == 0, "tiers");
        Object budget = make(common("client.render.FxLodRules$Budget"), 2);
        call(budget, "request", 1, 400.0);
        call(budget, "request", 2, 9.0);
        call(budget, "request", 3, 100.0);
        call(budget, "frame");
        h.assertTrue((boolean) call(budget, "allowed", 2) && (boolean) call(budget, "allowed", 3) && !(boolean) call(budget, "allowed", 1),
            "the budget grants the two nearest");
        Class<?> fx = common("client.suit.SuitFxRules");
        float[] prep = new float[3];
        for (float age = 0; age < 12; age += 0.7F) {
            call(fx, "prepare", age, 0.8F, prep, 0);
            for (double d = 0; d < 2; d += 0.13) {
                float fast = (float) call(fx, "rippleAt", d * d, prep, 0), ref = (float) call(fx, "ripple", d, age, 0.8F);
                h.assertTrue(Math.abs(fast - ref) <= 2.0E-4F, "prepared ripple " + fast + " vs " + ref + " at " + d + ", " + age);
            }
        }
        h.assertTrue((float) call(fx, "visor", 4F, 0.05F) < 0.2F && (float) call(fx, "visor", 4F, 1F) == 1F, "visor sweep");
        h.succeed();
    }

    /** The iris blades tile the hexagon when shut; the hatch and gate timelines. */
    @GameTest(batch = "round1132_rules", template = "empty", timeoutTicks = 20)
    public static void irisAndGate(GameTestHelper h) {
        Class<?> rules = common("client.armory.ArmoryFxRules");
        float[] b = new float[6];
        double area = 0;
        for (int i = 0; i < 6; i++) {
            call(rules, "blade", i, 0F, b);
            area += Math.abs((b[2] - b[0]) * (b[5] - b[1]) - (b[4] - b[0]) * (b[3] - b[1])) / 2;
        }
        double r = 0.34;
        h.assertTrue(Math.abs(area - 3 * Math.sqrt(3) / 2 * r * r) < 1e-5, "shut blades cover " + area);
        h.assertTrue((float) call(rules, "hatch", -4F, 40, false) == 1F && (float) call(rules, "hatch", 13F, 40, false) == 0F, "hatch");
        h.assertTrue((float) call(rules, "gate", -3F, true) == 1F && (float) call(rules, "swallow", 40F, 40) == 0F, "return gate and swallow");
        h.succeed();
    }

    /** Quantum pieces report absorbed hits; a fall comes from below; a hit without a place reports nothing. */
    @GameTest(batch = "round1132_hits", template = "empty", timeoutTicks = 20)
    public static void armourHitReport(GameTestHelper h) {
        Class<?> hit = common("suit.ArmorHitFx");
        for (String id : new String[]{"armor/item_armor_quantum_chestplate", "armor/item_armor_quantum_boots"}) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
            h.assertTrue((boolean) call(hit, "quantum", stack), id + " not a quantum piece");
        }
        h.assertTrue(!(boolean) call(hit, "quantum", new ItemStack(BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("mio_icif", "armor/item_armor_nano_chestplate")))), "nano counted as quantum");
        h.assertTrue(!(boolean) call(hit, "quantum", new ItemStack(Items.DIAMOND_CHESTPLATE)), "diamond counted as quantum");
        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, h.getLevel());
        stand.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1, 2, 1))));
        Vec3 down = (Vec3) call(hit, "direction", stand, h.getLevel().damageSources().fall());
        h.assertTrue(down != null && down.y < -0.99, "fall direction " + down);
        h.assertTrue(call(hit, "direction", stand, h.getLevel().damageSources().generic()) == null, "a hit without a place reports a direction");
        h.succeed();
    }

    /** The Armory puts one icon piece per stored suit into its update tag; a client copy reads them back. */
    @GameTest(batch = "round1132_armory", template = "empty", timeoutTicks = 20)
    public static void armoryIcons(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 2, 1), other = new BlockPos(3, 2, 1);
        var state = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_armory")).defaultBlockState();
        h.setBlock(pos, state);
        h.setBlock(other, state);
        BlockEntity armory = h.getBlockEntity(pos);
        ItemStack chest = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "armor/item_armor_quantum_chestplate")));
        Object piece = common("armory.ArmoryPiece").getEnumConstants()[0];
        for (Object p : common("armory.ArmoryPiece").getEnumConstants()) if ("CHEST".equals(p.toString())) piece = p;
        ItemStack rest = (ItemStack) call(armory, "store", 2, piece, chest.copy());
        h.assertTrue(rest.isEmpty(), "the chestplate was not stored: " + rest);
        var registries = h.getLevel().registryAccess();
        CompoundTag tag = armory.getUpdateTag(registries);
        h.assertTrue(tag.contains("ArmoryIcons") && tag.getList("ArmoryIcons", Tag.TAG_COMPOUND).size() == 6 && tag.contains("ArmoryMaint"),
            "no icons in the update tag");
        BlockEntity copy = h.getBlockEntity(other);
        copy.loadWithComponents(tag, registries);
        @SuppressWarnings("unchecked")
        var icons = (java.util.List<ItemStack>) call(copy, "icons");
        Set<Integer> filled = new HashSet<>();
        for (int i = 0; i < icons.size(); i++) if (!icons.get(i).isEmpty()) filled.add(i);
        h.assertTrue(filled.equals(Set.of(2)) && icons.get(2).is(chest.getItem()), "icons read back: " + filled);
        h.succeed();
    }
}
