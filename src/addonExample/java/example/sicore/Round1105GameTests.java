package example.sicore;

import com.miophas.singularity_iteration.core.api.item.EnergySaving;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.5 (patch): the player attributes and the tick guard of the Armory Wingman
 * (SlashBlade crash), capture blocking and sortie tokens (duplicated suits), Energy Saving in the
 * cost checks, the blast warning data map, the replication lists, lead shielding, the reactor
 * recipes and the Radiation Containment Box, the heat exchanger texture.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1105GameTests {
    private Round1105GameTests() {}

    private static final String CORE = "com.miophas.singularity_iteration" + ".core.";

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Class<?> core(String name) {
        try {
            return Class.forName(CORE + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    private static Mob wingman(GameTestHelper h) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl("wingman"));
        Mob w = (Mob) type.create(h.getLevel());
        h.assertTrue(w != null, "no wingman");
        w.moveTo(h.absoluteVec(new Vec3(1.5, 2, 1.5)));
        return w;
    }

    private static ItemStack craft(GameTestHelper h, String[] grid) {
        List<ItemStack> items = new ArrayList<>();
        for (String id : grid) items.add(id == null ? ItemStack.EMPTY
            : new ItemStack(BuiltInRegistries.ITEM.get(id.contains(":") ? ResourceLocation.parse(id) : rl(id))));
        CraftingInput input = CraftingInput.of(3, 3, items);
        var level = h.getLevel();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
            .map(r -> r.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static String id(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }

    // ------------------------------------------------------------------ 1. SlashBlade crash

    @GameTest(batch = "round1105_wingman", template = "empty", timeoutTicks = 20)
    public static void wingmanHasEveryPlayerAttribute(GameTestHelper h) {
        Mob w = wingman(h);
        var wanted = List.of(Attributes.SWEEPING_DAMAGE_RATIO, Attributes.ENTITY_INTERACTION_RANGE, Attributes.BLOCK_INTERACTION_RANGE,
            Attributes.LUCK, Attributes.BLOCK_BREAK_SPEED, Attributes.MINING_EFFICIENCY, Attributes.SUBMERGED_MINING_SPEED,
            Attributes.SNEAKING_SPEED, Attributes.ATTACK_KNOCKBACK);
        for (var a : wanted) {
            h.assertTrue(w.getAttribute(a) != null, "the Wingman lacks " + a.getRegisteredName());
            w.getAttributeValue(a);   // threw "Can't find attribute" in 0.1.10.3
        }
        h.assertTrue(w.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) == 3.0, "entity reach is not the player value");
        h.succeed();
    }

    @GameTest(batch = "round1105_wingman", template = "empty", timeoutTicks = 20)
    public static void tickGuardTakesOnlyWingmanErrors(GameTestHelper h) {
        Class<?> guard = Round33GameTests.common("armory.WingmanTickGuard");
        var pig = EntityType.PIG.create(h.getLevel());
        h.assertTrue(!(Boolean) Round33GameTests.call(guard, "handle", pig, new RuntimeException("test")),
            "the guard swallowed the error of an unrelated entity");
        Mob w = wingman(h);
        h.getLevel().addFreshEntity(w);
        Arrow arrow = new Arrow(EntityType.ARROW, h.getLevel());
        arrow.setOwner(w);
        h.assertTrue((Boolean) Round33GameTests.call(guard, "related", arrow), "an arrow of a Wingman is not related");
        h.assertTrue((Boolean) Round33GameTests.call(guard, "handle", arrow, new IllegalArgumentException("Can't find attribute")),
            "the guard did not take the error of a Wingman's arrow");
        h.assertTrue(arrow.isRemoved(), "the failing arrow was not removed");
        h.assertTrue((Boolean) Round33GameTests.call(guard, "handle", w, new RuntimeException("test")), "the guard did not take a Wingman error");
        h.assertTrue(w.isRemoved(), "a failing Wingman stays");
        h.succeed();
    }

    // ------------------------------------------------------------------ 2. capture and sortie tokens

    @GameTest(batch = "round1105_wingman", template = "empty", timeoutTicks = 20)
    public static void wingmanCannotBeCaptured(GameTestHelper h) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl("wingman"));
        for (String tag : new String[]{"c:capturing_not_supported", "c:teleporting_not_supported",
                "industrialforegoing:mob_imprisonment_tool_blacklist", "ars_nouveau:jar_blacklist", "enderio:soul_vial_blacklist"}) {
            h.assertTrue(type.is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(tag))), "the Wingman is not in " + tag);
        }
        Mob w = wingman(h);
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        InteractionResult r = w.interact(p, InteractionHand.MAIN_HAND);
        h.assertTrue(r == InteractionResult.CONSUME, "an item on a Wingman gives " + r);
        Round33GameTests.remove(p);
        h.succeed();
    }

    @GameTest(batch = "round1105_wingman", template = "empty", timeoutTicks = 20)
    public static void copiesOfAClosedSortieAreVoided(GameTestHelper h) {
        Class<?> ledgerType = Round33GameTests.common("armory.WingmanSorties");
        Object ledger = Round33GameTests.call(ledgerType, "get", h.getLevel().getServer());
        UUID token = (UUID) Round33GameTests.call(ledger, "open", UUID.randomUUID());
        h.assertTrue("OPEN".equals(String.valueOf(Round33GameTests.call(ledger, "status", token))), "a new token is not open");
        Round33GameTests.call(ledger, "close", token);
        h.assertTrue("CLOSED".equals(String.valueOf(Round33GameTests.call(ledger, "status", token))), "a closed token is not closed");

        Mob copy = wingman(h);
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        copy.setItemSlot(EquipmentSlot.CHEST, chest);
        CompoundTag tag = copy.saveWithoutId(new CompoundTag());
        tag.putUUID("Sortie", token);
        copy.load(tag);
        h.assertTrue(!(Boolean) Round33GameTests.call(copy, "admit"), "a copy of a closed sortie was admitted");
        h.assertTrue(copy.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "the copy keeps its items");

        Mob legacy = wingman(h);
        h.assertTrue((Boolean) Round33GameTests.call(legacy, "admit"), "a Wingman of an older save was turned away");
        UUID adopted = (UUID) Round33GameTests.call(legacy, "sortie");
        h.assertTrue(adopted != null && "OPEN".equals(String.valueOf(Round33GameTests.call(ledger, "status", adopted))),
            "an old Wingman was not adopted");
        h.succeed();
    }

    // ------------------------------------------------------------------ 3. Energy Saving

    @GameTest(batch = "round1105_energy", template = "empty", timeoutTicks = 20)
    public static void energySavingCostChecks(GameTestHelper h) {
        h.assertTrue(EnergySaving.maxCost(5, 1) == 1 && EnergySaving.maxCost(5, 160) == 120 && EnergySaving.maxCost(0, 160) == 160,
            "maxCost rounds wrong");
        for (int i = 0; i < 200; i++) {
            for (int level = 1; level <= 5; level++) {
                long amount = 1 + i * 7L;
                h.assertTrue(EnergySaving.apply(level, amount) <= EnergySaving.maxCost(level, amount), "apply exceeds maxCost");
            }
        }
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        ItemStack helmet = Round33GameTests.item("armor/item_armor_quantum_helmet");
        var enchantment = h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(EnergySaving.KEY);
        helmet.enchant(enchantment, 5);
        IBatteryItem battery = (IBatteryItem) helmet.getItem();
        battery.setEnergy(helmet, 100);
        p.setItemSlot(EquipmentSlot.HEAD, helmet);
        ItemStack worn = p.getItemBySlot(EquipmentSlot.HEAD);
        Class<?> pool = Round33GameTests.common("suit.SuitEnergyPool");
        h.assertTrue((Boolean) Round33GameTests.call(pool, "canPay", p, worn, 120L), "75 % of 120 EU cannot be paid from 100 EU");
        h.assertTrue((Boolean) Round33GameTests.call(pool, "payAll", p, worn, 120L), "payAll fails with Energy Saving V");
        h.assertTrue(battery.getEnergy(worn) == 10, "payAll took " + (100 - battery.getEnergy(worn)) + " EU, not 90");
        h.assertTrue(!(Boolean) Round33GameTests.call(pool, "payAll", p, worn, 40L), "payAll paid with too little EU");
        h.assertTrue(battery.getEnergy(worn) == 10, "a refused payAll took EU");
        Round33GameTests.remove(p);
        h.succeed();
    }

    // ------------------------------------------------------------------ 4. blast warning data

    @GameTest(batch = "round1105_data", template = "empty", timeoutTicks = 20)
    public static void blastWarningDataMap(GameTestHelper h) {
        Class<?> data = Round33GameTests.common("suit.BlastWarningData");
        Object nuke = Round33GameTests.call(data, "entry", BuiltInRegistries.ENTITY_TYPE.get(rl("nuke_primed")));
        h.assertTrue(nuke != null && (Float) Round33GameTests.call(nuke, "radius") == 40F, "nuke data map entry: " + nuke);
        Object tnt = Round33GameTests.call(data, "entry", BuiltInRegistries.ENTITY_TYPE.get(rl("ic_tnt_primed")));
        h.assertTrue(tnt != null && (Float) Round33GameTests.call(tnt, "fuse") == 4F, "industrial TNT data map entry: " + tnt);
        h.succeed();
    }

    // ------------------------------------------------------------------ 6. replication lists

    @GameTest(batch = "round1105_data", template = "empty", timeoutTicks = 40)
    public static void replicationListsPriceNewItems(GameTestHelper h) {
        var server = h.getLevel().getServer();
        Class<?> book = core("runtime.uu.UuQuoteBook");
        Object rawCopper = Round33GameTests.call(book, "quote", server, new ItemStack(Items.RAW_COPPER));
        h.assertTrue(rawCopper != null && Math.abs((Double) Round33GameTests.call(rawCopper, "buckets") - 0.0008) < 1e-9,
            "raw copper quote: " + rawCopper);
        Object star = Round33GameTests.call(book, "quote", server, new ItemStack(Items.NETHER_STAR));
        h.assertTrue(star != null && (Double) Round33GameTests.call(star, "buckets") == 100.0, "nether star quote: " + star);
        h.assertTrue(Round33GameTests.call(book, "quote", server, new ItemStack(Items.DRAGON_EGG)) == null, "the dragon egg has a price");
        h.assertTrue(Round33GameTests.call(book, "quote", server, new ItemStack(Items.COPPER_INGOT)) != null, "smelted copper has no derived price");
        h.assertTrue((Long) Round33GameTests.call(book, "energy", server, new ItemStack(Items.RAW_COPPER)) == -1L, "raw copper has a list EU cost");
        h.succeed();
    }

    // ------------------------------------------------------------------ 8. lead shielding

    @GameTest(batch = "round1105_reactor", template = "empty", timeoutTicks = 20)
    public static void leadShieldsRadiation(GameTestHelper h) {
        Class<?> shield = Round33GameTests.common("util.RadiationShielding");
        Vec3 from = h.absoluteVec(new Vec3(0.5, 2.5, 2.5)), to = h.absoluteVec(new Vec3(4.5, 2.5, 2.5));
        double open = (Double) Round33GameTests.call(shield, "transmission", h.getLevel(), from, to, (BlockPos) null);
        h.assertTrue(open == 1.0, "an open line lets " + open + " through");
        h.setBlock(new BlockPos(2, 2, 2), BuiltInRegistries.BLOCK.get(rl("block_lead")));
        double one = (Double) Round33GameTests.call(shield, "transmission", h.getLevel(), from, to, (BlockPos) null);
        h.assertTrue(Math.abs(one - 0.1) < 1e-9, "one lead block lets " + one + " through");
        h.setBlock(new BlockPos(3, 2, 2), BuiltInRegistries.BLOCK.get(rl("block_lead")));
        double two = (Double) Round33GameTests.call(shield, "transmission", h.getLevel(), from, to, (BlockPos) null);
        h.assertTrue(two == 0.0, "two lead blocks let " + two + " through");
        h.setBlock(new BlockPos(3, 2, 2), Blocks.OBSIDIAN);
        double mixed = (Double) Round33GameTests.call(shield, "transmission", h.getLevel(), from, to, (BlockPos) null);
        h.assertTrue(Math.abs(mixed - 0.05) < 1e-9, "lead and obsidian let " + mixed + " through");
        h.succeed();
    }

    // ------------------------------------------------------------------ 9. recipes and the containment box

    @GameTest(batch = "round1105_recipes", template = "empty", timeoutTicks = 20)
    public static void reactorItemsHaveRecipes(GameTestHelper h) {
        String c = "resource/item_lead_casing";
        ItemStack box = craft(h, new String[]{c, c, c, c, null, c, c, c, c});
        h.assertTrue(id(box).equals("mio_icif:normal/item_contain_ment_box"), "containment box recipe gives " + id(box));
        String u = "resource/item_uran_238", pu = "resource/item_plutonium";
        ItemStack mox = craft(h, new String[]{u, u, u, pu, pu, pu, u, u, u});
        h.assertTrue(id(mox).equals("mio_icif:resource/item_mox"), "MOX recipe gives " + id(mox));
        ItemStack fuel = craft(h, new String[]{"normal/item_re_enriched_uranium", "resource/item_coal_dust", null, null, null, null, null, null, null});
        h.assertTrue(id(fuel).equals("mio_icif:resource/item_uran"), "re-enriched uranium gives " + id(fuel));
        String cp = "resource/item_copper_plate";
        ItemStack pack = craft(h, new String[]{cp, "minecraft:redstone", cp, "minecraft:redstone", "minecraft:lava_bucket", "minecraft:redstone", cp, "minecraft:redstone", cp});
        h.assertTrue(id(pack).equals("mio_icif:reactor/item_reactor_heatpack"), "heat pack recipe gives " + id(pack));
        String t = "reactor/item_reactor_thick_reflector", d = "resource/item_copper_denseplate", i = "resource/item_iridium_plate";
        h.assertTrue(id(craft(h, new String[]{t, t, t, d, i, d, t, t, t})).equals("mio_icif:reactor/item_reactor_iridium_reflector"), "iridium reflector recipe");
        var recipes = h.getLevel().getRecipeManager();
        for (String r : new String[]{"canning/lithium_cell", "canning/isotope_rod"})
            h.assertTrue(recipes.byKey(rl(r)).isPresent(), "recipe " + r + " missing");
        h.succeed();
    }

    @GameTest(batch = "round1105_recipes", template = "empty", timeoutTicks = 20)
    public static void containmentBoxKeepsItems(GameTestHelper h) throws ReflectiveOperationException {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        ItemStack box = Round33GameTests.item("normal/item_contain_ment_box");
        h.assertTrue(box.getMaxStackSize() == 1, "the box stacks");
        p.setItemInHand(InteractionHand.MAIN_HAND, box);
        ItemStack held = p.getMainHandItem();
        Class<?> menuType = Round33GameTests.common("item.normal.ContainmentBoxMenu");
        Constructor<?> make = menuType.getConstructor(int.class, Inventory.class, ItemStack.class, InteractionHand.class);
        AbstractContainerMenu menu = (AbstractContainerMenu) make.newInstance(1, p.getInventory(), held, InteractionHand.MAIN_HAND);
        h.assertTrue(menu.slots.size() == 18 + 36, "the box menu has " + menu.slots.size() + " slots");
        ItemStack uranium = Round33GameTests.item("resource/item_uran");
        h.assertTrue(menu.getSlot(0).mayPlace(uranium), "uranium does not fit");
        h.assertTrue(!menu.getSlot(0).mayPlace(Round33GameTests.item("normal/item_contain_ment_box")), "a box fits into a box");
        menu.getSlot(0).set(uranium.copy());
        ItemContainerContents contents = held.get(DataComponents.CONTAINER);
        h.assertTrue(contents != null && contents.nonEmptyStream().count() == 1, "the box did not save its item");
        h.assertTrue(menu.stillValid(p), "the menu closed with the box in hand");
        Round33GameTests.remove(p);
        h.succeed();
    }

    // ------------------------------------------------------------------ 5. heat exchanger

    @GameTest(batch = "round1105_data", template = "empty", timeoutTicks = 20)
    public static void heatExchangerUsesItsOwnTexture(GameTestHelper h) {
        String off = Round33GameTests.resource("/assets/mio_icif/models/block/hugenerator/block_heat_source_fluid.json");
        String on = Round33GameTests.resource("/assets/mio_icif/models/block/hugenerator/block_heat_source_fluid_on.json");
        h.assertTrue(off != null && off.contains("block/producer/block_liquid_heat_ex_ec"), "the heat exchanger model lacks its texture");
        h.assertTrue(on != null && on.contains("\"uv\": [12, 0, 13.33, 16]"), "the active model does not use the active frames");
        h.succeed();
    }
}
