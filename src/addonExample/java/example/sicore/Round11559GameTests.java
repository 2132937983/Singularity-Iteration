package example.sicore;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.59 (gaze and Iron Man phase P7, decisions A / A): the Clean Slate protocol needs a second request within 3 s;
 * then every Wingman climbs and bursts in the colours of its armour, and its suit goes home (here, with no Armory: into the
 * owner's inventory), nothing is lost.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11559GameTests {
    private Round11559GameTests() {}

    private static final Class<?> SLATE = Round33GameTests.common("armory.WingmanCleanSlate");

    /** The confirmation window and the colours by armour. */
    @GameTest(template = "empty", batch = "round11559_rules", timeoutTicks = 20)
    public static void confirmationAndColours(GameTestHelper h) {
        h.assertTrue(!(Boolean) Round33GameTests.call(SLATE, "confirms", (Long) null, 100L), "confirmed without a request");
        h.assertTrue((Boolean) Round33GameTests.call(SLATE, "confirms", 100L, 160L), "a second request within 3 s");
        h.assertTrue(!(Boolean) Round33GameTests.call(SLATE, "confirms", 100L, 161L), "a request after 3 s confirmed");
        int[] gold = (int[]) Round33GameTests.call(SLATE, "colours", List.of(new ItemStack(Items.GOLDEN_CHESTPLATE)));
        ItemStack red = new ItemStack(Items.LEATHER_CHESTPLATE);
        red.set(DataComponents.DYED_COLOR, new DyedItemColor(0xC03030, true));
        int[] dyed = (int[]) Round33GameTests.call(SLATE, "colours", List.of(ItemStack.EMPTY, red));
        h.assertTrue(gold[0] == 0xFFD83D && dyed[0] == 0xC03030, "colours " + Integer.toHexString(gold[0]) + " / " + Integer.toHexString(dyed[0]));
        h.succeed();
    }

    /** Asked twice: the Wingman climbs, bursts and is gone; its suit is in the owner's inventory. */
    @GameTest(template = "empty", batch = "round11559_run", timeoutTicks = 40)
    public static void wingmenBurstAndTheSuitsGoHome(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 2, 2.5));
        w.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        w.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        Round33GameTests.call(SLATE, "request", owner);
        h.assertTrue(!(Boolean) Round33GameTests.call(w, "cleaningSlate"), "the first request already ran it");
        Round33GameTests.call(SLATE, "request", owner);
        h.assertTrue((Boolean) Round33GameTests.call(w, "cleaningSlate"), "the second request did not run it");
        double y0 = w.getDeltaMovement().y;
        for (int i = 0; i < 200 && !w.isRemoved(); i++) Round33GameTests.call(w, "tickCleanSlate");
        h.assertTrue(w.isRemoved(), "the Wingman did not burst");
        int found = 0;
        for (ItemStack s : owner.getInventory().items) if (s.is(Items.DIAMOND_CHESTPLATE) || s.is(Items.DIAMOND_SWORD)) found++;
        h.assertTrue(found == 2, "the suit did not go home: " + found + " of 2 pieces in the inventory");
        Round33GameTests.remove(owner);
        h.succeed();
    }
}
