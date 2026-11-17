package example.sicore;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.54 (gaze and Iron Man phase P2, decision four A): a suit swap between the owner and an active Wingman. The
 * Wingman's pieces end on the owner and the owner's gear on the Wingman, which stays in the squad; nothing is lost or
 * doubled; hand items that are not gear go into the inventory; a bound piece stays where it is; an owner without gear
 * dismisses the Wingman; a cancelled or saved swap puts the pieces back on the Wingman. The swap phases are driven by
 * calling the Wingman's swap tick (no flight in the test area).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11554GameTests {
    private Round11554GameTests() {}

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private static void dress(net.minecraft.world.entity.LivingEntity e, ItemStack... items) {
        for (int i = 0; i < SLOTS.length; i++) e.setItemSlot(SLOTS[i], i < items.length ? items[i] : ItemStack.EMPTY);
    }

    private static Mob wingmanAtSwapPlace(GameTestHelper h, ServerPlayer owner) {
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 2, 3.5));
        Vec3 place = (Vec3) Round33GameTests.call(Round33GameTests.common("armory.WingmanEntity"), "swapPlace", owner);
        w.moveTo(place.x, place.y, place.z);
        return w;
    }

    /** Runs the swap until it ends (at most 400 ticks of it). */
    private static void runSwap(GameTestHelper h, Mob w, ServerPlayer owner) {
        for (int i = 0; i < 400 && (Byte) Round33GameTests.call(w, "swapPhase") != 0; i++) {
            if (w.isRemoved()) return;
            Round33GameTests.call(w, "tickSwap", owner, true);
        }
        h.assertTrue(w.isRemoved() || (Byte) Round33GameTests.call(w, "swapPhase") == 0, "the swap did not end");
    }

    /** A full swap: the suits change places, the Wingman stays active, no item is lost or doubled. */
    @GameTest(template = "empty", batch = "round11554_swap", timeoutTicks = 40)
    public static void suitsChangePlaces(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        dress(owner, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS),
            new ItemStack(Items.IRON_BOOTS), new ItemStack(Items.IRON_SWORD));
        Mob w = wingmanAtSwapPlace(h, owner);
        dress(w, new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS),
            new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.SHIELD));
        h.assertTrue(Round33GameTests.call(w, "suitUp", owner) == null, "the swap did not start");
        h.assertTrue(Round33GameTests.call(w, "suitUp", owner) != null, "a second swap started during the first");
        runSwap(h, w, owner);
        h.assertTrue(owner.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE) && owner.getMainHandItem().is(Items.DIAMOND_SWORD)
            && owner.getOffhandItem().is(Items.SHIELD), "the owner is not in the Wingman's suit");
        h.assertTrue(w.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && w.getItemBySlot(EquipmentSlot.FEET).is(Items.IRON_BOOTS)
            && w.getMainHandItem().is(Items.IRON_SWORD) && w.getOffhandItem().isEmpty(), "the Wingman is not in the owner's old gear");
        h.assertTrue(!w.isRemoved() && (Byte) Round33GameTests.call(w, "state") == 0, "the Wingman left the squad");
        int swords = 0;
        var inv = owner.getInventory();
        for (int i = 0; i < inv.items.size(); i++)
            if (i != inv.selected && (inv.items.get(i).is(Items.DIAMOND_SWORD) || inv.items.get(i).is(Items.IRON_SWORD))) swords++;
        h.assertTrue(swords == 0, "a sword was doubled into the inventory");
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** A block in the hand goes to the inventory; a bound helmet stays on the owner and the Wingman keeps its own. */
    @GameTest(template = "empty", batch = "round11554_rules", timeoutTicks = 40)
    public static void handItemsAndBoundPieces(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        ItemStack bound = new ItemStack(Items.LEATHER_HELMET);
        bound.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE), 1);
        dress(owner, bound, new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS), ItemStack.EMPTY,
            new ItemStack(Items.DIRT, 32));
        Mob w = wingmanAtSwapPlace(h, owner);
        dress(w, new ItemStack(Items.GOLDEN_HELMET), new ItemStack(Items.GOLDEN_CHESTPLATE), ItemStack.EMPTY, new ItemStack(Items.GOLDEN_BOOTS),
            new ItemStack(Items.GOLDEN_SWORD));
        h.assertTrue(Round33GameTests.call(w, "suitUp", owner) == null, "the swap did not start");
        runSwap(h, w, owner);
        h.assertTrue(owner.getItemBySlot(EquipmentSlot.HEAD).is(Items.LEATHER_HELMET) && w.getItemBySlot(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET),
            "the bound helmet slot was swapped");
        h.assertTrue(owner.getMainHandItem().is(Items.GOLDEN_SWORD) && w.getMainHandItem().isEmpty(), "the hand swap");
        int dirt = 0;
        for (ItemStack s : owner.getInventory().items) if (s.is(Items.DIRT)) dirt += s.getCount();
        h.assertTrue(dirt == 32, "the dirt went elsewhere: " + dirt + " in the inventory");
        h.assertTrue(owner.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && w.getItemBySlot(EquipmentSlot.LEGS).is(Items.IRON_LEGGINGS),
            "the leggings went to the Wingman");
        h.assertTrue(owner.getItemBySlot(EquipmentSlot.FEET).is(Items.GOLDEN_BOOTS) && w.getItemBySlot(EquipmentSlot.FEET).isEmpty(), "the boots");
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** An owner without gear takes the whole suit: the Wingman is dismissed. */
    @GameTest(template = "empty", batch = "round11554_dismiss", timeoutTicks = 40)
    public static void ownerWithoutGearDismissesTheWingman(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        dress(owner);
        Mob w = wingmanAtSwapPlace(h, owner);
        dress(w, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE));
        h.assertTrue(Round33GameTests.call(w, "suitUp", owner) == null, "the swap did not start");
        runSwap(h, w, owner);
        h.assertTrue(w.isRemoved(), "the empty Wingman is still there");
        h.assertTrue(owner.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && owner.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),
            "the owner did not get the suit");
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** A cancel after the pieces came off, and a save in the middle, put the pieces back on the Wingman. */
    @GameTest(template = "empty", batch = "round11554_cancel", timeoutTicks = 40)
    public static void cancelledAndSavedSwapsKeepThePieces(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        dress(owner, new ItemStack(Items.IRON_HELMET));
        Mob w = wingmanAtSwapPlace(h, owner);
        dress(w, new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE));
        Round33GameTests.call(w, "suitUp", owner);
        Round33GameTests.call(w, "tickSwap", owner, true);   // the pieces come off
        h.assertTrue((Byte) Round33GameTests.call(w, "swapPhase") == 2 && w.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "the pieces did not come off");
        // a save now: the copy holds the pieces and puts them back on its first tick
        CompoundTag tag = new CompoundTag();
        w.saveWithoutId(tag);
        Entity copy = w.getType().create(h.getLevel());
        copy.load(tag);
        Round33GameTests.call(copy, "tickSwap", (Object) null, false);
        Mob m = (Mob) copy;
        h.assertTrue(m.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET) && m.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),
            "the saved copy lost the pieces");
        copy.discard();
        // the owner leaves before the pieces arrive: back on the Wingman, the owner unchanged
        for (int i = 0; i < 400 && (Byte) Round33GameTests.call(w, "swapPhase") == 2; i++) Round33GameTests.call(w, "tickSwap", owner, false);
        h.assertTrue((Byte) Round33GameTests.call(w, "swapPhase") == 0, "the swap did not end");
        h.assertTrue(w.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET) && w.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),
            "the cancelled swap lost the pieces");
        h.assertTrue(owner.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "the owner changed in a cancelled swap");
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }
}
