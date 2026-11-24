package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests of 0.1.11.198: tool overhaul T5a (wrenches ask IWrenchable; force field, lighter and other utility fixes). */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111198GameTests {
    private Round111198GameTests() {}

    private static final String TOOLS = "com.miophas.singularity_iteration." + "common.item.tools.";

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id));
    }

    private static Class<?> type(String name) {
        try {
            return Class.forName(TOOLS + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    /** D8: a block whose IWrenchable allows everything keeps the action; a block without one keeps it too. */
    @GameTest(template = "empty")
    public static void wrenchesAskIWrenchable(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_compressor_elc")));
        BlockState state = h.getBlockState(at);
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        Class<?> rules = type("WrenchRules");
        Object rotate = Enum.valueOf(type("WrenchRules$Action").asSubclass(Enum.class), "ROTATE");
        Object kept = ArmoryServiceGameTests.call(rules, "consult", h.getLevel(), h.absolutePos(at), state, p, rotate, Direction.EAST);
        h.assertTrue(kept == rotate, "an SI machine keeps the rotation, got " + kept);
        h.assertTrue(ArmoryServiceGameTests.call(rules, "customDrops", h.getLevel(), h.absolutePos(at), state) == null,
            "an SI machine drops through its loot, not the interface's bare block");
        p.discard();
        h.succeed();
    }

    /** U-10: the electric lighter answered yes to every ability (shears, stripping, tilling). */
    @GameTest(template = "empty")
    public static void lighterIsOnlyAFireStarter(GameTestHelper h) {
        ItemStack lighter = new ItemStack(item("item_electric_lighter"));
        h.assertTrue(lighter.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT), "it lights fires");
        h.assertTrue(!lighter.canPerformAction(ItemAbilities.SHEARS_DIG) && !lighter.canPerformAction(ItemAbilities.AXE_STRIP)
            && !lighter.canPerformAction(ItemAbilities.HOE_TILL), "and is no shears, axe or hoe");
        h.succeed();
    }

    /** U-09: a shot-away shield waited for the effect to expire; a refill cut a larger absorption down to 10. */
    @GameTest(template = "empty")
    public static void forceFieldRefillsAndKeepsMore(GameTestHelper h) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        ItemStack field = new ItemStack(item("item_electric_force_field_generator"));
        ((IBatteryItem) field.getItem()).setEnergy(field, ((IBatteryItem) field.getItem()).getMaxEnergy(field));
        ArmoryServiceGameTests.call(field.getItem(), "setAutoSupplement", field, true);
        p.setItemInHand(InteractionHand.MAIN_HAND, field);
        p.tickCount = 20;
        field.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        h.assertTrue(p.getAbsorptionAmount() == 10, "the shield comes up at 10, has " + p.getAbsorptionAmount());
        p.setAbsorptionAmount(2);
        p.tickCount = 40;
        field.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        h.assertTrue(p.getAbsorptionAmount() == 10, "a shot-away shield refills at once, has " + p.getAbsorptionAmount());
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 2400, 3));
        p.setAbsorptionAmount(16);
        p.tickCount = 60;
        field.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        h.assertTrue(p.getAbsorptionAmount() == 16, "a golden apple's 16 stays, has " + p.getAbsorptionAmount());
        p.discard();
        h.succeed();
    }
}
