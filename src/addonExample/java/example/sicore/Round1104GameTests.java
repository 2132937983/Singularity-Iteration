package example.sicore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.IWeaponItem;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.4 (patch): nugget-to-ingot crafting, the iridium neutron reflector recipe, the
 * RTG pellet with plutonium, the user's 3D swords, the energy faces of the reactor coolant injectors,
 * the blocks the reactor heat never changes, the compute core state, and the electric weapons of the
 * Armory Wingman (melee bonus and charge, the mining laser and the plasma launcher as ranged weapons).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1104GameTests {
    private Round1104GameTests() {}

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

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

    // ------------------------------------------------------------------ recipes

    @GameTest(batch = "round1104_recipes", template = "empty", timeoutTicks = 20)
    public static void nuggetsCraftBackIntoIngots(GameTestHelper h) {
        String[][] pairs = {{"resource/item_tin_nugget", "mio_icif:resource/item_ingot_tin"},
            {"resource/item_lead_nugget", "mio_icif:resource/item_lead_ingot"},
            {"resource/item_copper_nugget", "minecraft:copper_ingot"},
            {"resource/item_titanium_nugget", "mio_icif:resource/item_titanium_ingot"},
            {"resource/item_silver_nugget", "mio_icif:resource/item_silver_ingot"}};
        for (String[] p : pairs) {
            String[] grid = new String[9];
            java.util.Arrays.fill(grid, p[0]);
            ItemStack out = craft(h, grid);
            h.assertTrue(id(out).equals(p[1]) && out.getCount() == 1, p[0] + " x9 crafts " + id(out) + " x" + out.getCount());
        }
        h.succeed();
    }

    @GameTest(batch = "round1104_recipes", template = "empty", timeoutTicks = 20)
    public static void reactorRecipes(GameTestHelper h) {
        String t = "reactor/item_reactor_thick_reflector", d = "resource/item_copper_denseplate", i = "resource/item_iridium_plate";
        ItemStack reflector = craft(h, new String[]{t, t, t, d, i, d, t, t, t});
        h.assertTrue(id(reflector).equals("mio_icif:reactor/item_reactor_iridium_reflector"), "iridium reflector recipe gives " + id(reflector));
        String plate = "resource/item_iron_denseplate", pu = "resource/item_plutonium", ir = "resource/item_iridium";
        ItemStack pellet = craft(h, new String[]{plate, pu, plate, plate, pu, plate, plate, pu, plate});
        h.assertTrue(id(pellet).equals("mio_icif:resource/item_rtg_pellet"), "RTG pellet with plutonium gives " + id(pellet));
        ItemStack old = craft(h, new String[]{plate, ir, plate, plate, ir, plate, plate, ir, plate});
        h.assertTrue(old.isEmpty(), "the RTG pellet still takes iridium");
        h.succeed();
    }

    // ------------------------------------------------------------------ swords

    // 0.1.10.42: swordsCarryTheUserModels removed: it checked the 0.1.10.4 wrapper layout that 0.1.10.11 replaced;
    // Round1111GameTests.swordModelsLoadInTheHand checks the sword models (textures and overrides included)

    // ------------------------------------------------------------------ reactor

    // 0.1.10.42: injectorNeverConnectsTowardTheReactor removed: the injector faces six ways and opens its front since
    // 0.1.10.14; CoolantInjectorGameTests.energyFacesAndUpgradesFollowIc2 checks the faces

    @GameTest(batch = "round1104_reactor", template = "empty", timeoutTicks = 20)
    public static void heatNeverMeltsReactorParts(GameTestHelper h) {
        TagKey<Block> immune = TagKey.create(Registries.BLOCK, rl("reactor_heat_immune"));
        for (String path : new String[]{"reactor/block_reactor_vessel", "reactor/block_reactor_chamber", "reactor/block_reactor_access_hatch",
                "reactor/block_reactor_fluid_port", "reactor/block_reactor_redstone_port", "generator/block_nuclear_reactor_generator",
                "build/block_construction_foam", "build/block_cf_wall", "build/block_construction_wall"}) {
            Block block = BuiltInRegistries.BLOCK.get(rl(path));
            h.assertTrue(block != net.minecraft.world.level.block.Blocks.AIR && block.defaultBlockState().is(immune), path + " is not heat immune");
        }
        h.assertTrue(net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState().is(immune), "bedrock is not heat immune");
        h.assertTrue(!net.minecraft.world.level.block.Blocks.STONE.defaultBlockState().is(immune), "stone must still melt");
        h.succeed();
    }

    // ------------------------------------------------------------------ compute core

    @GameTest(batch = "round1104_core", template = "empty", timeoutTicks = 20)
    public static void computeCoreStateTellsWhy(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        ItemStack chest = Round1101GameTests.piece("chestplate", "THREAT_SENSOR", "COMPUTE_CORE");
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        Class<?> links = Round33GameTests.common("suit.quantum.QuantumLinks");
        h.assertTrue("ACTIVE".equals(String.valueOf(Round33GameTests.call(links, "computeCoreState", p))), "a powered, switched-on core is not active");
        h.assertTrue((Integer) Round33GameTests.call(links, "capacity", p) == 3, "no third link with an active core");
        ArmorFeatureState.setEnabled(chest, "fcs_compute_core", false);
        h.assertTrue("OFF".equals(String.valueOf(Round33GameTests.call(links, "computeCoreState", p))), "a switched-off core does not read OFF");
        h.assertTrue((Integer) Round33GameTests.call(links, "capacity", p) == 2, "a switched-off core still adds a link");
        ArmorFeatureState.setEnabled(chest, "fcs_compute_core", true);
        ((IBatteryItem) chest.getItem()).setEnergy(chest, 0);
        h.assertTrue("NO_POWER".equals(String.valueOf(Round33GameTests.call(links, "computeCoreState", p))), "an empty suit does not read NO_POWER");
        // the leggings core counts when the chest core is off
        ((IBatteryItem) chest.getItem()).setEnergy(chest, ((IBatteryItem) chest.getItem()).getMaxEnergy(chest));
        ArmorFeatureState.setEnabled(chest, "fcs_compute_core", false);
        p.setItemSlot(EquipmentSlot.LEGS, Round1101GameTests.piece("leggings", "COMPUTE_CORE"));
        h.assertTrue("ACTIVE".equals(String.valueOf(Round33GameTests.call(links, "computeCoreState", p))), "an active leggings core is hidden by a switched-off chest core");
        p.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ Wingman weapons

    private static Mob wingman(GameTestHelper h) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl("wingman"));
        Mob w = (Mob) type.create(h.getLevel());
        h.assertTrue(w != null, "no wingman");
        w.moveTo(h.absoluteVec(new Vec3(1.5, 2, 1.5)));
        return w;
    }

    @GameTest(batch = "round1104_wingman", template = "empty", timeoutTicks = 20)
    public static void wingmanNanosaberHitsAndPays(GameTestHelper h) {
        Mob w = wingman(h);
        ItemStack saber = Round33GameTests.item("item_tool_nanosaber");
        IBatteryItem battery = (IBatteryItem) saber.getItem();
        battery.setEnergy(saber, battery.getMaxEnergy(saber));
        Round33GameTests.call(Round33GameTests.common("item.tools.mio_icif_nanosaber"), "setActive", saber, true);
        w.setItemSlot(EquipmentSlot.MAINHAND, saber);
        var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 1));
        zombie.setNoAi(true);
        float before = zombie.getHealth();
        long energy = battery.getEnergy(saber);
        h.assertTrue(w.doHurtTarget(zombie), "the hit did not land");
        h.assertTrue(before - zombie.getHealth() > 10, "the active nanosaber hit for " + (before - zombie.getHealth()));
        h.assertTrue(battery.getEnergy(saber) < energy, "the nanosaber hit was free");
        zombie.discard();
        h.succeed();
    }

    @GameTest(batch = "round1104_wingman", template = "empty", timeoutTicks = 20)
    public static void wingmanFiresTheLaserAndThePlasma(GameTestHelper h) {
        Mob w = wingman(h);
        for (String path : new String[]{"item_tool_laser_miner", "item_tool_plasma_launcher"}) {
            ItemStack gun = Round33GameTests.item(path);
            h.assertTrue(gun.getItem() instanceof IWeaponItem weapon && weapon.isRanged(), path + " is not a ranged weapon for the Wingman");
            IWeaponItem weapon = (IWeaponItem) gun.getItem();
            weapon.setEnergy(gun, weapon.getMaxEnergy(gun));
            long energy = weapon.getEnergy(gun);
            AABB around = new AABB(w.position(), w.position()).inflate(4);
            int shotsBefore = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, around).size();
            int cd = weapon.fireFrom(gun, w, new Vec3(1, 0, 0), true);
            int shots = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, around).size() - shotsBefore;
            h.assertTrue(cd > 0 && shots > 0 && weapon.getEnergy(gun) < energy, path + ": cooldown " + cd + ", bolts " + shots);
            for (var bolt : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, around)) bolt.discard();
        }
        h.succeed();
    }
}
