package example.sicore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.core.api.item.BatteryEnergy;
import com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.9.0: frame cores, the core socket, the legacy shells of the retired solar
 * helmets and quantum chestplates, the gravity anchor, the arc discharge and the attachment data.
 * The SI common classes are reached by reflection (the addon module may only use the core API).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round190GameTests {
    private static final String CORES = "suit.core.FrameCores";

    // ------------------------------------------------------------------ helpers (also used by older rounds)

    static Object core(String id) {
        return Round33GameTests.call(Round33GameTests.common("suit.core.FrameCore"), "byId", id);
    }

    /** Installs the core {@code id}; returns the replaced core id or null. */
    static String install(ItemStack piece, String id) {
        Object old = Round33GameTests.call(Round33GameTests.common(CORES), "install", piece, core(id));
        return old == null ? null : (String) Round33GameTests.call(old, "id");
    }

    static String coreOf(ItemStack piece) {
        Object c = Round33GameTests.call(Round33GameTests.common(CORES), "core", piece);
        return c == null ? null : (String) Round33GameTests.call(c, "id");
    }

    /** A charged piece with a core (charged after the install, so the propulsion capacity counts). */
    static ItemStack withCore(String path, String coreId) {
        ItemStack stack = Round33GameTests.item(path);
        install(stack, coreId);
        if (stack.getItem() instanceof AbstractElectricArmor armor) armor.setEnergy(stack, armor.getMaxEnergy(stack));
        return stack;
    }

    static void coreTick(ServerPlayer player) {
        Round33GameTests.call(Round33GameTests.common(CORES), "tick", player);
    }

    static long energy(ItemStack stack) {
        return stack.getItem() instanceof AbstractElectricArmor armor ? armor.getEnergy(stack) : -1;
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    // ------------------------------------------------------------------ legacy shells

    private static final String[][] LEGACY = {
        // legacy item, new item, core (null: plain item)
        {"normal/item_solar_helmet", "module/item_module_solar_skin", null},
        {"armor/item_armor_advanced_solar_helmet", "armor/item_armor_nano_helmet", "solar_array_1"},
        {"armor/item_armor_hybrid_solar_helmet", "armor/item_armor_quantum_helmet", "solar_array_2"},
        {"armor/item_armor_ultimate_solar_helmet", "armor/item_armor_quantum_helmet", "solar_array_3"},
        {"armor/item_armor_heavy_quantum_chestplate", "armor/item_armor_quantum_chestplate", "graviton"},
        {"armor/item_armor_advanced_quantum_chestplate", "armor/item_armor_quantum_chestplate", "propulsion"}};

    /** Every legacy shell converts into its piece and core, with the energy and the other components. */
    @GameTest(template = "empty", batch = "round190_legacy")
    public static void legacyShellsConvert(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        Class<?> legacy = Round33GameTests.common("suit.core.LegacyArmor");
        for (String[] row : LEGACY) {
            ItemStack shell = Round33GameTests.item(row[0]);
            if (!(boolean) Round33GameTests.call(legacy, "isLegacy", shell)) { problems.add(row[0] + " is not a shell"); continue; }
            // below the 1,000,000 EU of the nano helmet the advanced solar helmet becomes (its old capacity was 1,000,000 too)
            shell.set(CoreDataComponents.BATTERY_ENERGY.get(), new BatteryEnergy(765_432L, 100_000_000L));
            shell.set(DataComponents.CUSTOM_NAME, Component.literal("Old Faithful"));
            Object result = Round33GameTests.call(legacy, "convert", shell);
            ItemStack out = (ItemStack) Round33GameTests.call(result, "stack");
            if (!id(out).equals(row[1])) problems.add(row[0] + " -> " + id(out));
            if (row[2] == null) continue;
            if (!row[2].equals(coreOf(out))) problems.add(row[0] + " core " + coreOf(out));
            if (energy(out) != 765_432L) problems.add(row[0] + " energy " + energy(out));
            if (!out.has(DataComponents.CUSTOM_NAME)) problems.add(row[0] + " lost its name");
        }
        h.assertTrue(problems.isEmpty(), "legacy conversion: " + problems);
        h.succeed();
    }

    /** A shell in the player's inventory or armour slot converts on the inventory tick. */
    @GameTest(template = "empty", batch = "round190_legacy", timeoutTicks = 20)
    public static void legacyShellConvertsWhenCarried(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        p.getInventory().setItem(4, Round33GameTests.item("armor/item_armor_heavy_quantum_chestplate"));
        p.setItemSlot(EquipmentSlot.HEAD, Round33GameTests.item("armor/item_armor_ultimate_solar_helmet"));
        p.getInventory().tick();
        ItemStack carried = p.getInventory().getItem(4), worn = p.getItemBySlot(EquipmentSlot.HEAD);
        Round33GameTests.remove(p);
        h.assertTrue(id(carried).equals("armor/item_armor_quantum_chestplate") && "graviton".equals(coreOf(carried)),
            "carried shell became " + id(carried) + " / " + coreOf(carried));
        h.assertTrue(id(worn).equals("armor/item_armor_quantum_helmet") && "solar_array_3".equals(coreOf(worn)),
            "worn shell became " + id(worn) + " / " + coreOf(worn));
        h.succeed();
    }

    /** One shell alone in the crafting grid converts (shells kept in chests). */
    @GameTest(template = "empty", batch = "round190_legacy")
    public static void legacyConversionRecipe(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        CraftingInput one = CraftingInput.of(1, 1, List.of(Round33GameTests.item("armor/item_armor_advanced_solar_helmet")));
        var match = recipes.getRecipeFor(RecipeType.CRAFTING, one, h.getLevel());
        h.assertTrue(match.isPresent(), "no recipe takes a lone legacy shell");
        ItemStack out = match.get().value().assemble(one, h.getLevel().registryAccess());
        h.assertTrue(id(out).equals("armor/item_armor_nano_helmet") && "solar_array_1".equals(coreOf(out)), "crafted " + id(out));
        CraftingInput two = CraftingInput.of(2, 1, List.of(Round33GameTests.item("armor/item_armor_advanced_solar_helmet"),
            new ItemStack(net.minecraft.world.item.Items.STICK)));
        h.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, two, h.getLevel()).isEmpty(), "a shell with another item converts");
        h.succeed();
    }

    // ------------------------------------------------------------------ socket rules

    /** Socket rules: class, slot, swap (the old core comes back), removal; the cores have recipes. */
    @GameTest(template = "empty", batch = "round190_socket")
    public static void coreSocketRules(GameTestHelper h) {
        Class<?> cores = Round33GameTests.common(CORES);
        ItemStack nanoHelmet = Round33GameTests.item("armor/item_armor_nano_helmet");
        h.assertTrue(String.valueOf(Round33GameTests.call(cores, "check", nanoHelmet, core("solar_array_3"))).equals("WRONG_CLASS"),
            "Solar Array III fits a nano helmet");
        h.assertTrue(String.valueOf(Round33GameTests.call(cores, "check", nanoHelmet, core("solar_array_1"))).equals("OK"),
            "Solar Array I does not fit a nano helmet");
        ItemStack quantumHelmet = Round33GameTests.item("armor/item_armor_quantum_helmet");
        h.assertTrue(String.valueOf(Round33GameTests.call(cores, "check", quantumHelmet, core("graviton"))).equals("WRONG_SLOT"),
            "a chest core fits a helmet");
        h.assertTrue(String.valueOf(Round33GameTests.call(cores, "check", Round33GameTests.item("armor/item_armor_nano_chestplate"),
            core("propulsion"))).equals("WRONG_CLASS"), "the propulsion core fits a nano chestplate");
        ItemStack chest = Round33GameTests.item("armor/item_armor_quantum_chestplate");
        h.assertTrue(install(chest, "graviton") == null && "graviton".equals(coreOf(chest)), "graviton not installed");
        h.assertTrue("graviton".equals(install(chest, "propulsion")) && "propulsion".equals(coreOf(chest)), "swap did not return graviton");
        Object removed = Round33GameTests.call(cores, "remove", chest);
        h.assertTrue(removed != null && coreOf(chest) == null, "core not removed");
        List<String> missing = new ArrayList<>();
        for (String r : new String[]{"item_core_solar_array_1", "item_core_solar_array_2", "item_core_solar_array_3",
                "item_core_solar_array_3_upgrade", "item_core_graviton", "item_core_propulsion", "legacy_armor_conversion"}) {
            if (h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("mio_icif", "suit/" + r)).isEmpty()) missing.add(r);
        }
        for (String old : new String[]{"shaped_advanced_quantum_chestplate", "shaped_heavy_quantum_chestplate", "shaped_hybrid_solar_helmet"}) {
            if (h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("mio_icif", old)).isPresent()) missing.add("still: " + old);
        }
        h.assertTrue(missing.isEmpty(), "core recipes: " + missing);
        h.succeed();
    }

    /** The station swaps a core and keeps the returned core in the unit slot (no swap back). */
    @GameTest(template = "reactor_loop", batch = "round190_station", timeoutTicks = 260)
    public static void stationSwapsCoreOnce(GameTestHelper h) {
        AbstractProcessingMachineBlockEntity be = Round33GameTests.station(h);
        ItemStack chest = withCore("armor/item_armor_quantum_chestplate", "graviton");
        Round33GameTests.slots(be).setStackInSlot(1, chest);
        Round33GameTests.slots(be).setStackInSlot(2, Round33GameTests.item("module/item_core_propulsion"));
        h.assertTrue(String.valueOf(Round33GameTests.call(be, "evaluate")).equals("CORE_SWAP"), "evaluate: " + Round33GameTests.call(be, "evaluate"));
        h.onEachTick(() -> ((com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity) be)
            .getEnergyStorageInternal().setEnergy(40_000));
        h.runAfterDelay(240, () -> {
            ItemStack piece = Round33GameTests.slots(be).getStackInSlot(1);
            ItemStack back = Round33GameTests.slots(be).getStackInSlot(2);
            h.assertTrue("propulsion".equals(coreOf(piece)), "piece core after the swap: " + coreOf(piece));
            h.assertTrue(id(back).equals("module/item_core_graviton"), "returned core: " + id(back));
            h.assertTrue(String.valueOf(Round33GameTests.call(be, "status")).equals("CORE_RETURNED"), "status " + Round33GameTests.call(be, "status"));
            h.succeed();
        });
    }

    /** A legacy shell in the station's piece slot converts at once. */
    @GameTest(template = "reactor_loop", batch = "round190_station", timeoutTicks = 20)
    public static void stationConvertsLegacyShell(GameTestHelper h) {
        AbstractProcessingMachineBlockEntity be = Round33GameTests.station(h);
        h.assertTrue(Round33GameTests.slots(be).isItemValid(1, Round33GameTests.item("armor/item_armor_advanced_quantum_chestplate")),
            "the station refuses a legacy shell");
        Round33GameTests.slots(be).setStackInSlot(1, Round33GameTests.item("armor/item_armor_advanced_quantum_chestplate"));
        h.runAfterDelay(3, () -> {
            ItemStack piece = Round33GameTests.slots(be).getStackInSlot(1);
            h.assertTrue(id(piece).equals("armor/item_armor_quantum_chestplate") && "propulsion".equals(coreOf(piece)),
                "station made " + id(piece) + " / " + coreOf(piece));
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ core effects

    /** Advanced Propulsion: 100 000 000 EU, thrust x1.15, no flight ceiling; the plain chestplate keeps its numbers. */
    @GameTest(template = "empty", batch = "round190_effects")
    public static void propulsionCapacityAndFlight(GameTestHelper h) {
        ItemStack plain = Round33GameTests.item("armor/item_armor_quantum_chestplate");
        ItemStack boosted = withCore("armor/item_armor_quantum_chestplate", "propulsion");
        var armor = (AbstractElectricArmor) plain.getItem();
        h.assertTrue(armor.getMaxEnergy(boosted) == armor.getMaxEnergy(plain) + 90_000_000L,
            "capacity " + armor.getMaxEnergy(boosted) + " vs " + armor.getMaxEnergy(plain));
        h.assertTrue(armor.getMaxEnergy(boosted) == 100_000_000L, "propulsion capacity " + armor.getMaxEnergy(boosted));
        h.assertTrue(Math.abs(JetpackFlightController.THRUST_SCALE.applyAsDouble(boosted) - 1.15) < 1e-9, "thrust scale");
        h.assertTrue(JetpackFlightController.THRUST_SCALE.applyAsDouble(plain) == 1.0, "plain thrust scale");
        h.assertTrue(JetpackFlightController.NO_CEILING.test(boosted) && !JetpackFlightController.NO_CEILING.test(plain), "flight ceiling hook");
        // removal clamps the energy to the plain capacity
        Round33GameTests.call(Round33GameTests.common(CORES), "remove", boosted);
        h.assertTrue(armor.getEnergy(boosted) <= armor.getMaxEnergy(plain), "energy above the capacity after removal: " + armor.getEnergy(boosted));
        h.succeed();
    }

    /** Graviton Bulwark gravity anchor: no fall damage, no knockback; switched off, both come back. */
    @GameTest(template = "empty", batch = "round190_effects", timeoutTicks = 20)
    public static void gravitonAnchor(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        ItemStack chest = withCore("armor/item_armor_quantum_chestplate", "graviton");
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        coreTick(p);
        p.setHealth(20);
        p.invulnerableTime = 0;
        // a real 12-block fall: below 3 blocks of fall distance the jetpack safety net cancels the fall, whatever the anchor does
        p.fallDistance = 12;
        p.causeFallDamage(12, 1, p.damageSources().fall());
        float afterFall = p.getHealth();
        p.setDeltaMovement(Vec3.ZERO);
        p.knockback(1.5, 1, 0);
        Vec3 pushed = p.getDeltaMovement();
        double resistance = p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        ArmorFeatures.setEnabled(p.getItemBySlot(EquipmentSlot.CHEST), "core_anchor", false);
        coreTick(p);
        p.invulnerableTime = 0;
        p.fallDistance = 12;
        p.causeFallDamage(12, 1, p.damageSources().fall());
        float unanchored = p.getHealth();
        Round33GameTests.remove(p);
        h.assertTrue(afterFall == 20, "anchored fall took " + (20 - afterFall));
        h.assertTrue(pushed.lengthSqr() < 1.0E-6, "anchored knockback moved the player: " + pushed);
        h.assertTrue(resistance >= 1.0, "knockback resistance " + resistance);
        h.assertTrue(unanchored < afterFall, "anchor switched off still blocks fall damage");
        h.succeed();
    }

    /** Arc discharge: hurts and pushes a nearby mob, costs EU, then waits for the cooldown; no core, no discharge. */
    @GameTest(template = "empty", batch = "round190_effects", timeoutTicks = 20)
    public static void arcDischarge(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        Zombie zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.moveTo(h.absoluteVec(new Vec3(3.5, 2, 1.5)));
        h.getLevel().addFreshEntity(zombie);
        Class<?> events = Round33GameTests.common("suit.core.CoreEvents");
        // without the core nothing happens
        p.setItemSlot(EquipmentSlot.CHEST, Round33GameTests.charged("armor/item_armor_quantum_chestplate"));
        Round33GameTests.call(events, "discharge", p);
        float untouched = zombie.getHealth();
        p.setItemSlot(EquipmentSlot.CHEST, withCore("armor/item_armor_quantum_chestplate", "propulsion"));
        long before = energy(p.getItemBySlot(EquipmentSlot.CHEST));
        Round33GameTests.call(events, "discharge", p);
        long afterFirst = energy(p.getItemBySlot(EquipmentSlot.CHEST));
        float hurt = zombie.getHealth();
        Vec3 push = zombie.getDeltaMovement();
        Round33GameTests.call(events, "discharge", p);
        long afterSecond = energy(p.getItemBySlot(EquipmentSlot.CHEST));
        zombie.discard();
        Round33GameTests.remove(p);
        h.assertTrue(untouched == zombie.getMaxHealth(), "discharge without the core hurt the zombie");
        h.assertTrue(hurt < untouched, "the arc did not hurt the zombie");
        h.assertTrue(push.x > 0.05, "the shock wave did not push the zombie away: " + push);
        h.assertTrue(before - afterFirst >= 1_500_000L, "discharge cost " + (before - afterFirst));
        h.assertTrue(afterSecond == afterFirst, "second discharge ignored the cooldown");
        h.succeed();
    }

    /** Solar rate: day rate in daylight under open sky, night rate at night, nothing under a roof. */
    @GameTest(template = "empty", batch = "round190_effects", timeoutTicks = 20)
    public static void solarArrayRates(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        Class<?> cores = Round33GameTests.common(CORES);
        BlockPos head = BlockPos.containing(p.getX(), p.getY() + 1, p.getZ());
        boolean sky = h.getLevel().canSeeSky(head);
        h.getLevel().setDayTime(6000);
        int day = (int) Round33GameTests.call(cores, "solarRate", h.getLevel(), p, core("solar_array_3"));
        h.getLevel().setDayTime(18000);
        int night = (int) Round33GameTests.call(cores, "solarRate", h.getLevel(), p, core("solar_array_3"));
        // the roof right above the head, inside the test area; the sky light under it is updated a few ticks later
        h.getLevel().setBlockAndUpdate(head.above(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        h.runAfterDelay(5, () -> {
            h.getLevel().setDayTime(6000);
            int roofed = (int) Round33GameTests.call(cores, "solarRate", h.getLevel(), p, core("solar_array_3"));
            h.getLevel().setBlockAndUpdate(head.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            Round33GameTests.remove(p);
            if (sky && !h.getLevel().isRaining()) {
                h.assertTrue(day == 512, "day rate " + day);
                h.assertTrue(night == 64, "night rate " + night);
            }
            h.assertTrue(roofed == 0, "a roof still lets the array generate: " + roofed);
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ assets

    /** The attachment files exist for every core and piece, with geometry and an existing texture; icons and sounds ship. */
    @GameTest(template = "empty", batch = "round190_data")
    public static void coreAssetsShip(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        String[] attachments = {"solar_array_1_nano_helmet", "solar_array_1_quantum_helmet", "solar_array_2_nano_helmet",
            "solar_array_2_quantum_helmet", "solar_array_3_quantum_helmet", "graviton_quantum_chestplate", "propulsion_quantum_chestplate"};
        for (String a : attachments) {
            String json = Round33GameTests.resource("/assets/mio_icif/armor_models/attachments/" + a + ".json");
            if (json == null) { problems.add(a + " missing"); continue; }
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String texture = root.get("texture").getAsString();
            if (!Round180GameTests.modResourceExists("assets/mio_icif/" + texture.substring(texture.indexOf(':') + 1))) problems.add(a + " texture " + texture);
            int segments = 0;
            for (JsonElement s : root.getAsJsonArray("segments")) {
                segments++;
                if (s.getAsJsonObject().getAsJsonArray("quads").isEmpty()) problems.add(a + "/" + s.getAsJsonObject().get("name") + " empty");
            }
            if (segments < 2) problems.add(a + " has " + segments + " segments");
        }
        for (String c : new String[]{"solar_array_1", "solar_array_2", "solar_array_3", "graviton", "propulsion"}) {
            if (!Round180GameTests.modResourceExists("assets/mio_icif/textures/item/module/item_core_" + c + ".png")) problems.add(c + " icon");
            if (Round33GameTests.resource("/assets/mio_icif/models/item/module/item_core_" + c + ".json") == null) problems.add(c + " item model");
            if (Round33GameTests.item("module/item_core_" + c).isEmpty()) problems.add(c + " item not registered");
        }
        String sounds = Round33GameTests.resource("/assets/mio_icif/sounds.json");
        for (String s : new String[]{"core.discharge", "core.install"}) {
            if (sounds == null || !sounds.contains("\"" + s + "\"")) problems.add("sound " + s);
        }
        h.assertTrue(problems.isEmpty(), "core assets: " + problems);
        h.succeed();
    }
}
