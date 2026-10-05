package example.sicore;

import com.miophas.singularity_iteration.core.api.item.BatteryAutoCharge;
import com.miophas.singularity_iteration.core.api.item.BatteryTransfer;
import com.miophas.singularity_iteration.core.api.item.EnergySaving;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.IElectricArmorItem;
import com.miophas.singularity_iteration.core.api.item.IElectricToolItem;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.7.35: Energy Saving enchantment, battery auto-charge mode and its solar loop guard, the
 * Armory return of hand items, the nano item renderer models and the boots orientation.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round35GameTests {
    private Round35GameTests() {}

    static final TagKey<Item> ELECTRIC = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("mio_icif", "enchantable/electric"));

    static ItemStack item(String path) { return Round33GameTests.item(path); }

    static ItemStack enchant(GameTestHelper h, ItemStack stack, int level) {
        var holder = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EnergySaving.KEY);
        stack.enchant(holder, level);
        return stack;
    }

    // ------------------------------------------------------------------ Energy Saving

    /** Levels I..V cost 95 / 90 / 85 / 80 / 75 % on average; whole hundreds are exact. */
    @GameTest(batch = "round35_saving", template = "empty", timeoutTicks = 20)
    public static void energySavingFactors(GameTestHelper h) {
        for (int level = 0; level <= 5; level++) {
            long expected = 100 - 5L * level;
            h.assertTrue(EnergySaving.apply(level, 100) == expected, "level " + level + ": 100 EU -> " + EnergySaving.apply(level, 100));
            h.assertTrue(Math.abs(EnergySaving.factor(level) - expected / 100.0) < 1e-9, "factor " + level);
        }
        long sum = 0;
        for (int i = 0; i < 20_000; i++) sum += EnergySaving.apply(1, 1);
        h.assertTrue(Math.abs(sum / 20_000.0 - 0.95) < 0.02, "1 EU at level I averages " + sum / 20_000.0);
        h.assertTrue(EnergySaving.apply(3, 0) == 0 && EnergySaving.apply(3, -5) == -5, "zero / negative costs pass through");
        h.succeed();
    }

    /** The enchantment is registered (max V), applies to electric gear only, and every tool debit pays less. */
    @GameTest(batch = "round35_saving", template = "empty", timeoutTicks = 20)
    public static void energySavingReducesToolAndArmorDebits(GameTestHelper h) {
        var holder = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EnergySaving.KEY);
        h.assertTrue(holder.value().getMaxLevel() == 5, "max level " + holder.value().getMaxLevel());
        ItemStack sword = item("item_tool_quantum_sword");
        if (sword.getItem() instanceof IBatteryItem b) b.setEnergy(sword, b.getMaxEnergy(sword));
        h.assertTrue(sword.getItem() instanceof IBatteryItem, "quantum sword not found");
        h.assertTrue(holder.value().isSupportedItem(sword), "quantum sword cannot take Energy Saving");
        h.assertTrue(!holder.value().isSupportedItem(new ItemStack(Items.IRON_SWORD)), "an iron sword must not take Energy Saving");
        enchant(h, sword, 5);
        IBatteryItem battery = (IBatteryItem) sword.getItem();
        long before = battery.getEnergy(sword);
        h.assertTrue(BatteryTransfer.consume(sword, battery, 1000), "debit refused");
        h.assertTrue(before - battery.getEnergy(sword) == 750, "level V tool debit: " + (before - battery.getEnergy(sword)));
        ItemStack chest = Round33GameTests.charged("armor/item_armor_quantum_chestplate");
        enchant(h, chest, 2);
        long c0 = ((IBatteryItem) chest.getItem()).getEnergy(chest);
        Object paid = Round33GameTests.call(Round33GameTests.common("suit.SuitModules"), "extract", chest, 10_000L);
        h.assertTrue((long) paid == 9_000 && c0 - ((IBatteryItem) chest.getItem()).getEnergy(chest) == 9_000, "level II suit debit: " + paid);
        h.succeed();
    }

    /** Every electric tool / weapon / armor item of SI is in the enchantable tag (the tag is data, this keeps it complete). */
    @GameTest(batch = "round35_saving", template = "empty", timeoutTicks = 20)
    public static void electricTagCoversEveryElectricItem(GameTestHelper h) {
        TreeSet<String> missing = new TreeSet<>(), all = new TreeSet<>();
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            Item it = entry.getValue();
            if (!id.getNamespace().equals("mio_icif")) continue;
            if (!(it instanceof IElectricToolItem) && !(it instanceof IElectricArmorItem)) continue;
            if (it instanceof com.miophas.singularity_iteration.core.api.item.IEnergyPackItem) continue;   // packs only give energy
            all.add(id.toString());
            if (!new ItemStack(it).is(ELECTRIC)) missing.add(id.toString());
        }
        if (!missing.isEmpty()) {
            try {
                Path out = Path.of("electric_items.txt").toAbsolutePath();
                Files.writeString(out, String.join("\n", all), StandardCharsets.UTF_8);
            } catch (Exception ignored) { }
        }
        h.assertTrue(all.size() > 20, "only " + all.size() + " electric items found");
        h.assertTrue(missing.isEmpty(), "not in #mio_icif:enchantable/electric: " + missing);
        h.succeed();
    }

    // ------------------------------------------------------------------ battery auto-charge

    static ItemStack firstBattery() {
        for (String p : new String[]{"normal/item_lapotron_crystal_lev0", "normal/item_crystal_lev0", "normal/item_advbat_lev0", "normal/item_bat_lev0"}) {
            ItemStack s = item(p);
            if (s.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IEnergyDistributable d && d.supportsAutoCharge()) return s;
        }
        return ItemStack.EMPTY;
    }

    static void runPass(ServerPlayer p, ItemStack battery) {
        IBatteryItem b = (IBatteryItem) battery.getItem();
        int slot = Math.floorMod(-p.tickCount, BatteryAutoCharge.INTERVAL);
        BatteryAutoCharge.tick(p, battery, b, 1000, slot);
    }

    /** Right click switches the mode; on, the battery feeds tools in the inventory; off, it gives nothing. */
    @GameTest(batch = "round35_battery", template = "empty", timeoutTicks = 20)
    public static void batteryRightClickTogglesAutoCharge(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        ItemStack battery = firstBattery();
        h.assertTrue(!battery.isEmpty(), "no battery item");
        IBatteryItem b = (IBatteryItem) battery.getItem();
        b.setEnergy(battery, b.getMaxEnergy(battery));
        ItemStack tool = item("armor/item_armor_quantum_helmet");
        IBatteryItem t = (IBatteryItem) tool.getItem();
        t.setEnergy(tool, 0);
        p.getInventory().setItem(0, battery);
        p.getInventory().setItem(1, tool);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, battery);
        battery = p.getMainHandItem();
        tool = p.getInventory().getItem(1);
        long full = b.getEnergy(battery);
        battery.getItem().use(h.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(BatteryAutoCharge.isOn(battery), "right click did not switch the mode on");
        h.assertTrue(b.getEnergy(battery) == full, "right click must not discharge the battery any more");
        runPass(p, battery);
        long got = t.getEnergy(tool);
        h.assertTrue(got > 0, "auto mode on: tool not charged");
        battery.getItem().use(h.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(!BatteryAutoCharge.isOn(battery), "second right click did not switch the mode off");
        runPass(p, battery);
        h.assertTrue(t.getEnergy(tool) == got, "auto mode off: tool still charged");
        Round33GameTests.remove(p);
        h.succeed();
    }

    /** A solar helmet never charges a battery in auto mode (no helmet <-> battery loop). */
    @GameTest(batch = "round35_battery", template = "empty", timeoutTicks = 20)
    public static void solarHelmetSkipsAutoChargingBattery(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        ItemStack helmet = Round33GameTests.charged("armor/item_armor_advanced_solar_helmet");
        h.assertTrue(helmet.getItem() instanceof IBatteryItem, "advanced solar helmet not found");
        p.setItemSlot(EquipmentSlot.HEAD, helmet);
        ItemStack battery = firstBattery();
        IBatteryItem b = (IBatteryItem) battery.getItem();
        b.setEnergy(battery, 0);
        BatteryAutoCharge.toggle(battery);
        p.getInventory().setItem(3, battery);
        ItemStack worn = p.getItemBySlot(EquipmentSlot.HEAD);
        for (int i = 0; i < 60; i++) {
            com.miophas.singularity_iteration.core.runtime.energy.SolarHelmetCharging.tick(worn, (IBatteryItem) worn.getItem(),
                h.getLevel(), p, 20, 1000);
        }
        h.assertTrue(b.getEnergy(p.getInventory().getItem(3)) == 0, "solar helmet charged a battery in auto mode");
        Round33GameTests.remove(p);
        h.succeed();
    }

    // ------------------------------------------------------------------ Armory return

    /** Returning the suit sends the main hand and off hand items home too (the remote stays). */
    @GameTest(template = "reactor_loop", batch = "round35_armory", timeoutTicks = 40)
    public static void returnSuitTakesHandItems(GameTestHelper h) {
        AbstractProcessingMachineBlockEntity armory = ArmoryServiceGameTests.armory(h, 300_000);
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        ArmoryServiceGameTests.call(armory, "tryBind", p);
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        Class<?> service = Round33GameTests.common("armory.ArmoryRemoteService");
        Round33GameTests.call(service, "returnSuit", p, armory);
        h.assertTrue(p.getMainHandItem().isEmpty(), "main hand item was not returned");
        h.assertTrue(p.getOffhandItem().isEmpty(), "off hand item was not returned");
        h.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "helmet was not returned");
        var items = armory.getItemHandler();
        boolean sword = false, shield = false;
        for (int i = 0; i < items.getSlots(); i++) {
            sword |= items.getStackInSlot(i).is(Items.IRON_SWORD);
            shield |= items.getStackInSlot(i).is(Items.SHIELD);
        }
        h.assertTrue(sword && shield, "hand items not stored in the Armory (sword=" + sword + ", shield=" + shield + ")");
        p.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ nano suit

    /** Nano items draw their 3D model (builtin/entity) and the boots face forward (toes at -z in model space). */
    @GameTest(batch = "round35_nano", template = "empty", timeoutTicks = 20)
    public static void nanoItemsUseTheirModelAndBootsFaceForward(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String piece : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/models/item/armor/item_armor_nano_" + piece + ".json");
            if (json == null || !json.contains("builtin/entity")) problems.add(piece + " item model is not builtin/entity");
        }
        String boots = Round33GameTests.resource("/assets/mio_icif/armor_models/nano_boots.json");
        var parts = com.google.gson.JsonParser.parseString(boots).getAsJsonObject().getAsJsonObject("parts");
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (String part : new String[]{"right_leg", "left_leg"}) {
            for (var q : parts.getAsJsonArray(part)) {
                var v = q.getAsJsonObject().getAsJsonArray("v");
                for (int i = 0; i < 4; i++) {
                    double z = v.get(i * 5 + 2).getAsDouble();
                    minZ = Math.min(minZ, z);
                    maxZ = Math.max(maxZ, z);
                }
            }
        }
        // the toe caps reach further than the heel: the front (-z) must be the long side
        if (-minZ <= maxZ) problems.add("boots face backwards: z from " + minZ + " to " + maxZ);
        h.assertTrue(problems.isEmpty(), "nano: " + problems);
        h.succeed();
    }
}
