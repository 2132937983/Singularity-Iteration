package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.IWeaponItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.197: tool overhaul T4, the weapons (the D5 number table, melee that needs energy, the steel
 * shield's cost per blocked point, creative rockets, the weapon terrain setting).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111197GameTests {
    private Round111197GameTests() {}

    private static final String TOOLS = "com.miophas.singularity_iteration." + "common.item.tools.";

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id));
    }

    private static ItemStack charged(String id) {
        ItemStack stack = new ItemStack(item(id));
        IBatteryItem battery = (IBatteryItem) stack.getItem();
        battery.setEnergy(stack, battery.getMaxEnergy(stack));
        return stack;
    }

    private static long energy(ItemStack stack) {
        return ((IBatteryItem) stack.getItem()).getEnergy(stack);
    }

    private static Object call(Object target, String name, Object... args) {
        return ArmoryServiceGameTests.call(target, name, args);
    }

    private static Object staticField(String type, String name) {
        try {
            var f = Class.forName(TOOLS + type).getDeclaredField(name);
            f.setAccessible(true);
            return f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(type + "." + name, e);
        }
    }

    private static double modifier(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        double[] sum = {0};
        stack.getAttributeModifiers().forEach(EquipmentSlotGroup.MAINHAND, (a, m) -> { if (a.is(attribute)) sum[0] += m.amount(); });
        return sum[0];
    }

    /** Single-target damage a second: a bolt's damage times shots a second, capped at 2 by the target's hurt cooldown. */
    private static double dps(double damage, int cooldownTicks) {
        return damage * Math.min(20.0 / cooldownTicks, 2.0);
    }

    private static double[] gun(String id) {
        Object spec = call(item(id), "spec");
        double damage = ((Number) call(spec, "damage")).doubleValue();
        int cooldown = ((Number) call(spec, "cooldown")).intValue();
        long cost = ((IWeaponItem) item(id)).getEnergyPerShot();
        return new double[] {dps(damage, cooldown), cost / damage};
    }

    /** D5: single-target damage a second and EU per point of damage rise with the tier (T2 rifle, T3 nano bow, T4, T5). */
    @GameTest(template = "empty")
    public static void weaponTableRisesWithTier(GameTestHelper h) {
        double bowDamage = ((Number) staticField("mio_icif_nano_bow", "ARROW_BASE_DAMAGE")).doubleValue() * 15.0;
        double bowCost = ((Number) staticField("mio_icif_nano_bow", "ENERGY_PER_SHOT")).doubleValue();
        double[][] rows = {
            gun("item_tool_electric_rifle"),
            {dps(bowDamage, 5), bowCost / bowDamage},
            gun("item_tool_advanced_electric_rifle"),
            gun("item_tool_tactical_laser_rifle")};
        String[] names = {"T2 rifle", "T3 nano bow", "T4 rifle", "T5 laser"};
        for (int i = 1; i < rows.length; i++) {
            h.assertTrue(rows[i][0] > rows[i - 1][0], names[i] + " deals " + rows[i][0] + " a second, not above " + names[i - 1] + " " + rows[i - 1][0]);
            h.assertTrue(rows[i][1] >= rows[i - 1][1], names[i] + " costs " + rows[i][1] + " EU a point, below " + names[i - 1] + " " + rows[i - 1][1]);
        }
        h.assertTrue(Math.abs(rows[0][0] - 12) < 0.5 && Math.abs(rows[1][0] - 30) < 0.5 && Math.abs(rows[2][0] - 60) < 0.5
            && Math.abs(rows[3][0] - 120) < 0.5, "the table's 12 / 30 / 60 / 120");
        h.succeed();
    }

    /** W-08 / D5: a gun's melee bonus needs energy, and melee weapons swing at a sword's speed. */
    @GameTest(template = "empty")
    public static void meleeNeedsEnergyAndSwordSpeed(GameTestHelper h) {
        ItemStack flat = new ItemStack(item("item_tool_tactical_laser_rifle"));
        h.assertTrue(modifier(flat, Attributes.ATTACK_DAMAGE) == 0, "a flat laser rifle adds no melee damage");
        h.assertTrue(modifier(flat, Attributes.ATTACK_SPEED) == -2.4, "and swings at a sword's speed");
        h.assertTrue(modifier(charged("item_tool_tactical_laser_rifle"), Attributes.ATTACK_DAMAGE) == 20, "a charged one adds 20");
        h.assertTrue(modifier(charged("item_tool_tachyon_disruptor"), Attributes.ATTACK_DAMAGE) == 30, "the tachyon disruptor adds 30");
        h.assertTrue(modifier(new ItemStack(item("item_tool_nanosaber")), Attributes.ATTACK_SPEED) == -2.4, "the nanosaber swings at a sword's speed");
        h.assertTrue(modifier(new ItemStack(item("item_tool_quantum_sword")), Attributes.ATTACK_DAMAGE) == 4, "a flat quantum sword hits for 5");
        h.assertTrue(modifier(charged("item_tool_quantum_sword"), Attributes.ATTACK_DAMAGE) == 24, "a charged one for 25");
        h.succeed();
    }

    /** D5: the steel shield pays 50 EU a blocked point; a flat one lets the hit through and cannot be raised for a while. */
    @GameTest(template = "empty")
    public static void steelShieldPaysPerBlockedPoint(GameTestHelper h) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        ItemStack shield = charged("item_tool_steel_shield");
        p.setItemInHand(InteractionHand.OFF_HAND, shield);
        p.startUsingItem(InteractionHand.OFF_HAND);
        var zombie = EntityType.ZOMBIE.create(h.getLevel());
        long before = energy(shield);
        var blocked = new LivingShieldBlockEvent(p, new DamageContainer(p.damageSources().mobAttack(zombie), 4), true);
        NeoForge.EVENT_BUS.post(blocked);
        h.assertTrue(blocked.getBlocked() && before - energy(shield) == 200, "4 blocked points cost 200 EU, cost " + (before - energy(shield)));
        ((IBatteryItem) shield.getItem()).setEnergy(shield, 100);
        p.startUsingItem(InteractionHand.OFF_HAND);
        var through = new LivingShieldBlockEvent(p, new DamageContainer(p.damageSources().mobAttack(zombie), 4), true);
        NeoForge.EVENT_BUS.post(through);
        h.assertTrue(!through.getBlocked(), "a shield that cannot pay lets the hit through");
        h.assertTrue(p.getCooldowns().isOnCooldown(shield.getItem()), "and cannot be raised for a while");
        p.discard();
        h.succeed();
    }

    /** W-17: the rocket launcher took a rocket in creative too. */
    @GameTest(template = "empty")
    public static void creativeKeepsItsRockets(GameTestHelper h) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        p.setGameMode(GameType.CREATIVE);
        ItemStack launcher = charged("item_tool_rocket_launcher");
        ItemStack rockets = new ItemStack(item("item_rocket"), 3);
        p.setItemInHand(InteractionHand.MAIN_HAND, launcher);
        p.getInventory().setItem(5, rockets);
        p.setXRot(-90);
        launcher.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(rockets.getCount() == 3, "creative keeps its rockets, has " + rockets.getCount());
        p.discard();
        h.succeed();
    }

    /** D6: weapons damage terrain unless the server turns it off. */
    @GameTest(template = "empty")
    public static void weaponTerrainSettingDefaultsOn(GameTestHelper h) {
        boolean on;
        try {
            on = (boolean) Class.forName("com.miophas.singularity_iteration." + "common.Singularity_Iteration_Config")
                .getMethod("weaponsDamageTerrain").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        h.assertTrue(on, "weaponsDamageTerrain defaults to on");
        h.succeed();
    }
}
