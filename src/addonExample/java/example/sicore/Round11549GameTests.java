package example.sicore;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.49 (escort and open Armory phase F0): the Connector Kit is gone and the Armory takes the gear of every mod; the
 * item tag {@code mio_icif:armory_blacklist} keeps items out (this test mod puts its cycle upgrade in it); an item linked
 * before keeps its component and the slot it names (decision two A); a saved Connector Kit loads as nothing.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11549GameTests {
    private Round11549GameTests() {}

    private static final Class<?> RULES = Round33GameTests.common("armory.ArmoryRules");

    private static boolean fits(ItemStack stack, String piece) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object p = Enum.valueOf((Class) Round33GameTests.common("armory.ArmoryPiece"), piece);
        return (Boolean) Round33GameTests.call(RULES, "fits", stack, p);
    }

    private static boolean summonable(ItemStack stack) { return (Boolean) Round33GameTests.call(RULES, "isSummonable", stack); }

    /** Foreign items go to the hands, armour by its slot; the blacklist tag keeps an item out; empty is never taken. */
    @GameTest(template = "empty", batch = "round11549_rules", timeoutTicks = 20)
    public static void everyModIsAcceptedButTheBlacklist(GameTestHelper h) {
        ItemStack seed = new ItemStack(CoreExampleMod.FOREIGN_CROP_SEED.get()), sonar = new ItemStack(CoreExampleMod.SONAR_UNIT.get());
        ItemStack banned = new ItemStack(CoreExampleMod.CYCLE_UPGRADE.get());
        h.assertTrue(summonable(seed) && summonable(sonar), "foreign items are summonable");
        h.assertTrue(fits(seed, "MAINHAND") && fits(seed, "OFFHAND"), "a foreign item fits both hands");
        h.assertTrue(!fits(seed, "HEAD") && !fits(seed, "CHEST"), "a foreign hand item stays out of armour positions");
        h.assertTrue(fits(new ItemStack(Items.NETHERITE_HELMET), "HEAD") && !fits(new ItemStack(Items.NETHERITE_HELMET), "MAINHAND"),
            "armour still docks by its slot");
        h.assertTrue(!summonable(banned) && !fits(banned, "MAINHAND"), "the blacklist tag keeps an item out");
        h.assertTrue(!summonable(ItemStack.EMPTY), "empty is never summonable");
        h.succeed();
    }

    /** A stack linked before 0.1.11.49 loads with its component, and the slot it names still counts. */
    @GameTest(template = "empty", batch = "round11549_legacy", timeoutTicks = 20)
    public static void linkedGearOfOldSavesKeepsItsSlot(GameTestHelper h) {
        @SuppressWarnings("unchecked")
        DataComponentType<Object> link = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(
            ResourceLocation.fromNamespaceAndPath("mio_icif", "armory_link"));
        h.assertTrue(link != null, "the armory_link component stays registered");
        Object head;
        try {
            head = Round33GameTests.common("armory.ArmoryLink").getConstructor(String.class).newInstance("head");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("ArmoryLink", e);
        }
        ItemStack seed = new ItemStack(CoreExampleMod.FOREIGN_CROP_SEED.get());
        seed.set(link, head);
        Tag saved = seed.save(h.getLevel().registryAccess());
        ItemStack loaded = ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag) saved);
        h.assertTrue(!loaded.isEmpty() && loaded.has(link), "the linked stack loads with its component");
        h.assertTrue(fits(loaded, "HEAD") && !fits(loaded, "MAINHAND"), "the slot of the old link still counts");
        h.succeed();
    }

    /** The Connector Kit id resolves to air: a saved kit loads as an empty stack, and no kit is registered. */
    @GameTest(template = "empty", batch = "round11549_kit", timeoutTicks = 20)
    public static void savedConnectorKitLoadsAsNothing(GameTestHelper h) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("mio_icif", "normal/item_armory_connector");
        h.assertTrue(BuiltInRegistries.ITEM.get(id) == Items.AIR, "the kit id is an alias of air");
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.toString());
        tag.putInt("count", 3);
        h.assertTrue(ItemStack.parseOptional(h.getLevel().registryAccess(), tag).isEmpty(), "a saved kit loads as nothing");
        h.succeed();
    }
}
