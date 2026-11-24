package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.201: tool overhaul T5d, the passive devices as accessories. Curios is not installed in the test
 * server, so the accessory slots are stood in for by setting the bridge's hook ({@code PassiveDeviceRules.wornCopies}).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111201GameTests {
    private Round111201GameTests() {}

    private static java.lang.reflect.Field hook() {
        try {
            var f = Class.forName("com.miophas.singularity_iteration." + "common.item.tools.PassiveDeviceRules").getField("wornCopies");
            return f;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** D7: with accessory slots, a kit in the inventory does nothing, a worn one heals (one point a second). */
    @GameTest(template = "empty", batch = "round111201_worn")
    public static void passiveDevicesWorkWhenWorn(GameTestHelper h) throws IllegalAccessException {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        ItemStack kit = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "item_electric_first_aid_life_support")));
        ((IBatteryItem) kit.getItem()).setEnergy(kit, ((IBatteryItem) kit.getItem()).getMaxEnergy(kit));
        ArmoryServiceGameTests.call(kit.getItem(), "setAutoSupplement", kit, true);
        var field = hook();
        Object before = field.get(null);
        List<ItemStack> worn = new java.util.ArrayList<>();
        Function<Player, List<ItemStack>> slots = player -> worn;
        field.set(null, slots);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, kit);
            p.setHealth(10);
            p.tickCount = 20;
            kit.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
            h.assertTrue(p.getHealth() == 10, "a kit in the inventory does nothing with accessory slots, health " + p.getHealth());
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            worn.add(kit);
            ArmoryServiceGameTests.call(kit.getItem(), "tickWorn", kit, p);
            h.assertTrue(p.getHealth() == 11, "a worn kit heals, health " + p.getHealth());
        } finally {
            field.set(null, before);
        }
        p.discard();
        h.succeed();
    }
}
