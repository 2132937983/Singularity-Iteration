package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of the 0.1.10.41 fixes: lead shields a creature in a nuclear blast zone in the open (the less open of the line to
 * the blast centre and the roof line counts), a recipe that asks for an Empty Cell takes only a cell that is really empty, not
 * the Empty Cell item holding coolant, and the guide's machine rates match the code. Radiation damage pushes nobody, a full
 * hazmat suit also blocks radioactive blocks, and the Oil Refinery takes its cycle time and EU per tick from its recipes.
 * Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1141FixGameTests {
    private Round1141FixGameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    /** An Empty Cell filled with a fluid through its fluid handler: still the Empty Cell item, the fluid in its data. */
    private static ItemStack filledEmptyCell(String fluid) {
        IFluidHandlerItem handler = Round33GameTests.item("cell/item_cell_empty").getCapability(Capabilities.FluidHandler.ITEM);
        handler.fill(new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluid)), 1_000), IFluidHandler.FluidAction.EXECUTE);
        return handler.getContainer();
    }

    // ------------------------------------------------------------------ 1. shelter in a blast zone

    /** A survival player in a full hazmat suit, standing at {@code pos}. */
    private static ServerPlayer hazmatPlayer(GameTestHelper h, BlockPos pos) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = h.absolutePos(pos);
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        player.setItemSlot(EquipmentSlot.HEAD, Round33GameTests.item("armor/item_armor_hazmat_helmet"));
        player.setItemSlot(EquipmentSlot.CHEST, Round33GameTests.item("armor/item_armor_hazmat_chestplate"));
        player.setItemSlot(EquipmentSlot.LEGS, Round33GameTests.item("armor/item_armor_hazmat_leggings"));
        player.setItemSlot(EquipmentSlot.FEET, Round33GameTests.item("armor/item_armor_hazmat_boots"));
        return player;
    }

    private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> sickness() {
        return BuiltInRegistries.MOB_EFFECT.getHolder(id("radiation")).orElseThrow();
    }

    /** Radiation damage has no attacker; it is tagged minecraft:no_knockback, so it does not push its target in a random direction. */
    @GameTest(batch = "round1141_cells", template = "empty", timeoutTicks = 20)
    public static void radiationDamagePushesNobody(GameTestHelper h) {
        var type = h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
            .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, id("radiation")));
        var source = new net.minecraft.world.damagesource.DamageSource(type);
        h.assertTrue(source.is(net.minecraft.tags.DamageTypeTags.NO_KNOCKBACK), "radiation damage is not tagged minecraft:no_knockback");
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, 2.5f, 0, 2.5f);
        pig.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(pig.hurt(source, 1), "the pig took no radiation damage");
        var push = pig.getDeltaMovement();
        h.assertTrue(push.x == 0 && push.z == 0 && push.y <= 0, "radiation damage pushed the pig: " + push);
        h.succeed();
    }

    /**
     * A radioactive deepslate block (radiation level 3) gives a pig next to it radiation sickness, weakness and wither; a player in
     * a full hazmat suit as close gets none of them, as in a blast zone.
     */
    @GameTest(batch = "round1141_cells", template = "empty", timeoutTicks = 20)
    public static void hazmatBlocksRadioactiveBlocks(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 0, 2);
        var block = BuiltInRegistries.BLOCK.get(id("environment/block_radiating_deepslate"));
        h.setBlock(at, block);
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, 4.5f, 0, 2.5f);
        ServerPlayer suited = hazmatPlayer(h, new BlockPos(2, 0, 4));
        call(block, "applyRadiationEffect", h.getLevel(), h.absolutePos(at));
        boolean pigSick = pig.hasEffect(sickness()), pigWeak = pig.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),
            pigWither = pig.hasEffect(net.minecraft.world.effect.MobEffects.WITHER);
        boolean suitHit = suited.hasEffect(sickness()) || suited.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)
            || suited.hasEffect(net.minecraft.world.effect.MobEffects.WITHER);
        suited.discard();
        h.assertTrue(pigSick && pigWeak && pigWither, "the pig: sickness " + pigSick + ", weakness " + pigWeak + ", wither " + pigWither);
        h.assertTrue(!suitHit, "the player in a full hazmat suit got an effect from the radioactive block");
        h.succeed();
    }

    /**
     * A blast zone (radius 6) around (2, 0, 2), on the ground under the open sky. Four subjects stand 2 blocks from the centre: a pig in the
     * open, a pig behind a lead wall, a pig under a lead roof and a player in a full hazmat suit. Only the pig in the open gets
     * radiation sickness: before 0.1.10.41 the open sky cancelled the wall. Radiation damage pushes nobody, so the pigs stay
     * where they stand (before, it knocked them in a random direction).
     */
    @GameTest(batch = "round1141_fallout", template = "empty", timeoutTicks = 60)
    public static void leadShieldsABlastZoneInTheOpen(GameTestHelper h) {
        Class<?> zones = Round33GameTests.common("blockentity.reactor.NukeRadiationZoneManager");
        BlockPos centre = new BlockPos(2, 0, 2);
        h.setBlock(new BlockPos(3, 0, 2), BuiltInRegistries.BLOCK.get(id("block_lead")));
        h.setBlock(new BlockPos(3, 1, 2), BuiltInRegistries.BLOCK.get(id("block_lead")));
        h.setBlock(new BlockPos(2, 3, 4), BuiltInRegistries.BLOCK.get(id("block_lead")));
        Pig open = h.spawnWithNoFreeWill(EntityType.PIG, 0.5f, 0, 2.5f);
        Pig walled = h.spawnWithNoFreeWill(EntityType.PIG, 4.5f, 0, 2.5f);
        Pig roofed = h.spawnWithNoFreeWill(EntityType.PIG, 2.5f, 0, 4.5f);
        ServerPlayer suited = hazmatPlayer(h, new BlockPos(2, 0, 0));
        UUID zone = (UUID) call(zones, "createRadiationZone", h.getLevel(), h.absolutePos(centre), 6, 500f);
        BlockPos absCentre = h.absolutePos(centre);
        h.runAfterDelay(30, () -> {
            double wall = (Double) call(zones, "shelterTransmission", h.getLevel(), absCentre, walled);
            double roof = (Double) call(zones, "shelterTransmission", h.getLevel(), absCentre, roofed);
            double sky = (Double) call(zones, "shelterTransmission", h.getLevel(), absCentre, open);
            var sickness = sickness();
            boolean openSick = open.hasEffect(sickness), wallSick = walled.hasEffect(sickness), roofSick = roofed.hasEffect(sickness),
                suitSick = suited.hasEffect(sickness);
            call(zones, "removeRadiationZone", zone);
            suited.discard();
            h.assertTrue(sky == 1.0 && openSick, "the pig in the open: " + sky + " gets through, sick " + openSick + lines(h, absCentre, open));
            h.assertTrue(Math.abs(wall - 0.1) < 1e-9 && !wallSick,
                "behind one lead wall in the open " + wall + " gets through (not 0.1), sick " + wallSick + lines(h, absCentre, walled));
            h.assertTrue(Math.abs(roof - 0.1) < 1e-9 && !roofSick,
                "under a lead roof " + roof + " gets through (not 0.1), sick " + roofSick + lines(h, absCentre, roofed));
            h.assertTrue(!suitSick, "the player in a full hazmat suit got radiation sickness");
            h.succeed();
        });
    }

    /** Where a creature stands and what each of the two lines lets through, for a failure message. */
    private static String lines(GameTestHelper h, BlockPos centre, LivingEntity entity) {
        Class<?> shield = Round33GameTests.common("util.RadiationShielding");
        var eyes = entity.getEyePosition();
        return " (at " + h.relativeVec(entity.position()) + ", line to the centre "
            + call(shield, "transmission", h.getLevel(), Vec3.atCenterOf(centre), entity, (BlockPos) null) + ", roof line "
            + call(shield, "transmission", h.getLevel(), eyes.add(0, 16, 0), eyes, (BlockPos) null) + ")";
    }

    // ------------------------------------------------------------------ 2. empty cells in recipes

    /** No crafting ingredient that takes an Empty Cell takes the Empty Cell item holding coolant (or water) as well. */
    @GameTest(batch = "round1141_cells", template = "empty", timeoutTicks = 20)
    public static void emptyCellIngredientsRefuseAFilledCell(GameTestHelper h) {
        ItemStack plain = Round33GameTests.item("cell/item_cell_empty"), coolant = filledEmptyCell("mio_icif:coolant"), water = filledEmptyCell("minecraft:water");
        h.assertTrue(coolant.is(plain.getItem()) && !coolant.getComponentsPatch().isEmpty(), "a coolant-filled Empty Cell is " + coolant);
        int strict = 0;
        for (var holder : h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            for (Ingredient ingredient : holder.value().getIngredients()) {
                if (ingredient.isEmpty() || !ingredient.test(plain)) continue;
                h.assertTrue(!ingredient.test(coolant) && !ingredient.test(water), holder.id() + " takes a filled Empty Cell as an empty one");
                strict++;
            }
        }
        h.assertTrue(strict >= 26, "only " + strict + " crafting ingredients take an Empty Cell (26 recipes ask for one)");
        h.succeed();
    }

    /** A coolant cell and a water bucket craft nothing; an Empty Cell and a water bucket still make a Water Cell. */
    @GameTest(batch = "round1141_cells", template = "empty", timeoutTicks = 20)
    public static void aCoolantCellIsNoEmptyCellInTheGrid(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        var bucket = new ItemStack(Items.WATER_BUCKET);
        var withEmpty = CraftingInput.of(2, 1, List.of(Round33GameTests.item("cell/item_cell_empty"), bucket));
        var withCoolant = CraftingInput.of(2, 1, List.of(filledEmptyCell("mio_icif:coolant"), bucket.copy()));
        var found = recipes.getRecipeFor(RecipeType.CRAFTING, withEmpty, h.getLevel());
        h.assertTrue(found.isPresent() && found.get().value().assemble(withEmpty, h.getLevel().registryAccess()).is(Round33GameTests.item("cell/item_cell_water").getItem()),
            "an Empty Cell and a water bucket make " + found.map(r -> r.id().toString()).orElse("nothing"));
        var water = recipes.byKey(id("shapeless_190_water_cell")).orElseThrow();
        h.assertTrue(!((CraftingRecipe) water.value()).matches(withCoolant, h.getLevel()), "the Water Cell recipe takes a coolant cell");
        h.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, withCoolant, h.getLevel()).isEmpty(),
            "a coolant cell and a water bucket craft " + recipes.getRecipeFor(RecipeType.CRAFTING, withCoolant, h.getLevel()).map(r -> r.id().toString()).orElse(""));
        h.succeed();
    }

    /** A full coolant cell leaves an Empty Cell in the grid; a drained one (marked empty) leaves nothing, so no cell is copied. */
    @GameTest(batch = "round1141_cells", template = "empty", timeoutTicks = 20)
    public static void onlyAFullCellLeavesAnEmptyCell(GameTestHelper h) {
        ItemStack full = Round33GameTests.item("cell/item_cell_coolant");
        h.assertTrue(full.hasCraftingRemainingItem() && full.getCraftingRemainingItem().is(Round33GameTests.item("cell/item_cell_empty").getItem()),
            "a full coolant cell leaves " + full.getCraftingRemainingItem());
        ItemStack drained = full.copy();
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("empty", true);
        drained.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        h.assertTrue(!drained.hasCraftingRemainingItem(), "a drained coolant cell leaves " + drained.getCraftingRemainingItem());
        h.assertTrue(Round33GameTests.item("cell/item_cell_empty").getCraftingRemainingItem().isEmpty(), "an Empty Cell leaves something");
        h.assertTrue(filledEmptyCell("mio_icif:coolant").getCraftingRemainingItem().is(Round33GameTests.item("cell/item_cell_empty").getItem()),
            "an Empty Cell holding coolant no longer leaves its cell");
        h.succeed();
    }

    // ------------------------------------------------------------------ 3. machine rates in the guide

    private static Number constant(String type, String name) {
        Class<?> owner = Round33GameTests.common("blockentity." + type);
        try {
            var f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return (Number) f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(type + "." + name, e);
        }
    }

    private static double num(String type, String name) { return constant(type, name).doubleValue(); }

    /** A number as the guide writes it: 3,072 and 0.25 (no trailing zeros). */
    private static String n(double value) {
        var exact = BigDecimal.valueOf(value).stripTrailingZeros();
        return exact.scale() <= 0 ? String.format(Locale.ROOT, "%,d", exact.longValueExact()) : exact.toPlainString();
    }

    /**
     * The rates of the 0.1.10.41 guide rows (fuel, steam, water and heat use, HU, KU and EU per tick) are the ones in the code: the
     * English pages hold the numbers read here from the block entities and the semifluid fuel table.
     */
    @GameTest(batch = "round1141_guide", template = "empty", timeoutTicks = 20)
    public static void guidePagesShowTheMachineRates(GameTestHelper h) {
        Map<String, List<String>> expected = new java.util.LinkedHashMap<>();
        Class<?> fuels = Round33GameTests.common("blockentity.generator.SemifluidFuels");
        for (String fluid : List.of("biogas", "biomass", "crudeoil", "dieseloil")) {
            Object fl = BuiltInRegistries.FLUID.get(id(fluid));
            Object eu = call(fuels, "generatorValue", fl), hu = call(fuels, "heatValue", fl);
            double perMb = ((Number) call(eu, "energyPerMb")).doubleValue(), perTick = ((Number) call(eu, "energyPerTick")).doubleValue();
            expected.computeIfAbsent("generator_block_semifluid_generator", k -> new java.util.ArrayList<>())
                .add(n(perTick / perMb) + " mB/t → " + n(perTick) + " EU/t (" + n(perMb) + " EU/mB)");
            double cycle = ((Number) call(hu, "millibucketsPerCycle")).doubleValue(), heat = ((Number) call(hu, "heatPerTick")).doubleValue();
            expected.computeIfAbsent("hugenerator_block_fluid_heat_generator", k -> new java.util.ArrayList<>())
                .add(n(cycle / 20) + " mB/t → " + n(heat) + " HU/t (" + n(((Number) call(hu, "heatPerMb")).doubleValue()) + " HU/mB)");
        }
        expected.get("hugenerator_block_fluid_heat_generator").add("| " + n(num("huentity.hugenerator.mio_icif_fluid_heat_generator", "FUEL_CAPACITY")) + " mB |");
        String adv = "generator.mio_icif_advanced_semifluid_generator";
        double advPerMb = num(adv, "ENERGY_PER_BUCKET") / num(adv, "FUEL_PER_BUCKET"), advTick = num(adv, "ENERGY_GENERATION_RATE");
        expected.put("generator_block_advanced_semifluid_generator", List.of(n(advTick / advPerMb) + " mB/t → " + n(advTick) + " EU/t (" + n(advPerMb) + " EU/mB)"));
        String geo = "generator.mio_icif_geo_generator";
        expected.put("generator_block_geo_generator", List.of(n(num(geo, "LAVA_MB_PER_TICK")) + " mB/t → " + n(num(geo, "ENERGY_GENERATION_RATE"))
            + " EU/t (" + n(num(geo, "EU_PER_MB")) + " EU/mB)", "+" + n(num(geo, "LAVA_BLOCK_ENERGY_RATE")) + " EU/t each, up to 6"));
        String diesel = "generator.mio_icif_diesel_generator";
        double dieselMb = num(diesel, "FUEL_CONSUME_PER_TICK"), dieselEu = num(diesel, "ENERGY_GENERATION_RATE");
        expected.put("generator_block_diesel_generator", List.of(n(dieselMb) + " mB/t → " + n(dieselEu) + " EU/t (" + n(dieselEu / dieselMb) + " EU/mB)"));
        String water = "generator.mio_icif_water_generator";
        double tank = num(water, "ENERGY_PER_BUCKET") / num(water, "WATER_PER_BUCKET"), face = num(water, "WATER_FACE_GENERATION");
        expected.put("generator_block_water_generator", List.of("1 mB/t → " + n(tank) + " EU/t (" + n(tank) + " EU/mB)", "+" + n(face) + " EU/t",
            "up to " + n(tank + face) + " EU/t"));
        String turbine = "generator.mio_icif_steam_turbine";
        double steamPerEu = num(turbine, "STEAM_PER_EU"), turbineEu = num(turbine, "MAX_OUTPUT");
        expected.put("generator_block_steam_turbine", List.of(n(steamPerEu * turbineEu) + " mB/t → " + n(turbineEu) + " EU/t (" + n(steamPerEu) + " mB per EU)"));
        String exchanger = "huentity.hugenerator.mio_icif_heat_source_fluid";
        double perConductor = num(exchanger, "HEAT_PER_CONDUCTOR"), maxHeat = num(exchanger, "MAX_HEAT_OUTPUT"), huPerMb = num(exchanger, "HU_PER_MB");
        expected.put("hugenerator_block_heat_source_fluid", List.of(n(huPerMb) + " HU per mB", n(perConductor) + " HU/t per Heat Conductor, up to " + n(maxHeat) + " HU/t",
            n(perConductor / huPerMb) + " mB/t per Heat Conductor, up to " + n(maxHeat / huPerMb) + " mB/t"));
        String solid = "huentity.hugenerator.mio_icif_solid_heat_generator";
        double solidHeat = num(solid, "HEAT_GENERATION_RATE"), perUnit = num(solid, "VANILLA_TICKS_PER_FUEL_UNIT");
        expected.put("hugenerator_block_solid_heat_generator", List.of(n(solidHeat) + " HU/t while the fuel burns", "furnace burn time ÷ " + n(perUnit)
            + " (Coal: " + n(1_600 / perUnit) + " t, " + n(1_600 / perUnit * solidHeat) + " HU)"));
        expected.put("hugenerator_block_heat_generator_elc", List.of("up to " + n(num("huentity.hugenerator.mio_icif_heat_generator_elc", "MAX_HEAT")) + " HU/t (10 coils)"));
        try {
            var rates = Round33GameTests.common("blockentity.huentity.hugenerator.mio_icif_rt_heat_generator").getDeclaredField("HEAT_RATES");
            rates.setAccessible(true);
            int[] r = (int[]) rates.get(null);
            expected.put("hugenerator_block_rt_heat_generator", List.of(r[1] + ", " + r[2] + ", " + r[3] + ", " + r[4] + ", " + r[5] + " or " + r[6] + " HU/t for 1 to 6 pellets"));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("HEAT_RATES", e);
        }
        String advStirling = "generator.mio_icif_advanced_stirling_generator";
        expected.put("generator_block_advanced_stirling_generator", List.of(n(num(advStirling, "HU_PER_CONVERSION")) + " HU → " + n(num(advStirling, "EU_PER_CONVERSION")) + " EU",
            "up to " + n(num(advStirling, "MAX_HEAT_RECEIVE")) + " HU/t"));
        String stirlingKu = "kuentity.kugenerator.mio_icif_Stirling_Kinetic_Generator";
        expected.put("kugenerator_block_stirling_kinetic_generator", List.of(n(num(stirlingKu, "HU_PER_OPERATION")) + " HU + " + n(num(stirlingKu, "MB_PER_OPERATION"))
            + " mB Water → " + n(num(stirlingKu, "KU_PER_OPERATION")) + " KU + " + n(num(stirlingKu, "MB_PER_OPERATION")) + " mB Hot Water",
            n(num(stirlingKu, "MAX_KU_BUFFER")) + " KU", n(num(stirlingKu, "MAX_HEATBUFFER")) + " HU"));
        String steamKu = "kuentity.kugenerator.mio_icif_steam_kinetic_generator";
        expected.put("producer_block_steam_kinetic_generator", List.of(n(num(steamKu, "KU_PER_MB_STEAM")) + " KU per mB, per 1,000 mB: "
            + n(1_000 - 1_000 / num(steamKu, "CONDENSATION_DIVISOR")) + " mB of exhaust steam and " + n(1_000 / num(steamKu, "CONDENSATION_DIVISOR") / num(steamKu, "CONDENSATION_THRESHOLD"))
            + " mB of Distilled Water", n(num(steamKu, "KU_PER_MB_SUPERHEATED")) + " KU per mB"));
        expected.put("generator_block_kinetic_generator", List.of("1 EU per " + n(num("generator.mio_icif_kinetic_generator", "KU_TO_EU_RATIO")) + " KU"));
        for (String turbo : List.of("turbo_kinetic_generator", "twin_turbo_kinetic_generator")) {
            String type = "generator.mio_icif_" + turbo;
            var full = BigDecimal.valueOf(num(type, "EU_PER_KU"));
            expected.put("generator_block_" + turbo, List.of(n(full.doubleValue()) + " EU per KU from wind or water kinetic generators, "
                + n(full.multiply(BigDecimal.valueOf(num(type, "EFFECTIVE_PERCENT_ARTIFICIAL"))).doubleValue()) + " EU per KU from other sources"));
        }
        String boiler = "producer.mio_icif_steam_generator";
        expected.put("producer_block_steam_generator", List.of("1 mB → " + n(num(boiler, "STEAM_EXPANSION")) + " mB", "up to " + n(num(boiler, "MAX_HU_PER_TICK")) + " HU/t",
            "from " + n(num(boiler, "SUPERHEATED_PRESSURE")) + " bar (valve up to " + n(num(boiler, "MAX_PRESSURE")) + " bar)"));
        String repress = "producer.mio_icif_steam_repressurizer";
        String batch = n(num(repress, "INPUT_BATCH")) + " mB + " + n(num(repress, "HEAT_PER_BATCH")) + " HU → ";
        expected.put("producer_block_steam_repressurizer", List.of(batch + n(num(repress, "NORMAL_OUTPUT_BATCH")) + " mB", batch + n(num(repress, "SUPERHEATED_OUTPUT_BATCH")) + " mB"));
        String condenser = "producer.mio_icif_condenser";
        expected.put("producer_block_condenser", List.of(n(num(condenser, "MAX_PROGRESS")) + " mB → " + n(num(condenser, "PRODUCT_PER_BATCH")) + " mB",
            n(num(condenser, "BASE_CONDENSE_RATE")) + " mB/t of steam, each Heat Vent +" + n(num(condenser, "VENT_BONUS")) + " mB/t for " + n(num(condenser, "VENT_EU_COST")) + " EU/t"));
        String washer = "producer.mio_icif_washer_elc";
        expected.put("producer_block_washer_elc", List.of(n(num(washer, "WATER_PER_OPERATION")) + " mB per operation (" + n(num(washer, "WATER_PER_OPERATION") / num(washer, "DEFAULT_WORK_TIME")) + " mB/t)"));
        expected.put("producer_block_pump_elc", List.of(n(num("producer.mio_icif_pump_elc", "FLUID_PER_OPERATION")) + " mB (one source block)"));
        String electrolyzer = "producer.mio_icif_electrolyzer_elc";
        expected.put("producer_block_electrolyzer_elc", List.of(n(num(electrolyzer, "ENERGY_PER_WATER_CELL")) + " EU per Water Cell, sent to adjacent blocks at up to "
            + n(num(electrolyzer, "DEFAULT_MAX_EXTRACT")) + " EU/t"));
        // the refinery's cycle time and EU per tick are the ones of its recipes (0.1.10.41)
        List<String> refining = new java.util.ArrayList<>();
        var refiningType = BuiltInRegistries.RECIPE_TYPE.get(id("fluid_refining"));
        for (var holder : h.getLevel().getRecipeManager().getRecipes()) {
            if (holder.value().getType() != refiningType || !holder.id().getNamespace().equals("mio_icif")) continue;
            var info = (com.miophas.singularity_iteration.core.api.recipe.IRecipeProcessingInfo) holder.value();
            int out = ((FluidStack) call(holder.value(), "getOutputFluid")).getAmount();
            refining.add("1 mB → " + out + " mB per cycle (" + n(info.getProcessingTime()) + " t, " + n(info.getEnergyPerTick()) + " EU/t)");
        }
        h.assertTrue(refining.size() >= 3, "only " + refining.size() + " refining recipes");
        expected.put("producer_block_oil_refinery_elc", refining);

        List<String> missing = new java.util.ArrayList<>();
        expected.forEach((page, lines) -> {
            String text = Round33GameTests.resource("/assets/mio_icif/guides/mio_icif/manual/machines/" + page + ".md");
            if (text == null) { missing.add(page + ": no page"); return; }
            for (String line : lines) if (!text.contains(line)) missing.add(page + ": " + line);
        });
        h.assertTrue(missing.isEmpty(), "guide pages miss the code's rates: " + missing);
        h.succeed();
    }

    /** A canner recipe an addon registers takes an empty cell as its can, not one holding coolant. */
    @GameTest(batch = "round1141_cells", template = "empty", timeoutTicks = 20)
    public static void addonCannerRecipesTakeOnlyEmptyCells(GameTestHelper h) {
        var api = MioIcifAPI.instance().getRecipeRegistrationAPI();
        var key = ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, "gametest_can_dead_bush");
        h.assertTrue(api.registerCannerRecipe(key, Ingredient.of(Blocks.DEAD_BUSH), new ItemStack(Items.STICK), 100), "the canner recipe was not registered");
        Object registered = ((Map<?, ?>) call(api, "getRegisteredRecipes")).get(key);
        Ingredient can = (Ingredient) call(call(registered, "recipe"), "getCanIngredient");
        h.assertTrue(can.test(Round33GameTests.item("cell/item_cell_empty")), "the can does not take an Empty Cell");
        h.assertTrue(!can.test(filledEmptyCell("mio_icif:coolant")), "the can takes an Empty Cell holding coolant");
        h.succeed();
    }

    // ------------------------------------------------------------------ 4. oil refinery recipes

    private static BlockEntity refinery(GameTestHelper h, BlockPos pos, String fluid) {
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(id("producer/block_oil_refinery_elc")));
        BlockEntity machine = h.getBlockEntity(pos);
        var tank = (IFluidHandler) call(machine, "getInputTank");
        h.assertTrue(tank.fill(new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluid)), 100), IFluidHandler.FluidAction.EXECUTE) == 100,
            "the refinery took no " + fluid);
        return machine;
    }

    /**
     * The Oil Refinery takes its cycle time and EU per tick from the recipe (processingtime, energypertick), as a datapack sets
     * them: a hot water recipe of 4 ticks and 0 EU/t runs without power, a biomass recipe of 6 ticks and 5 EU/t waits for it.
     */
    @GameTest(batch = "round1141_refinery", template = "empty", timeoutTicks = 60)
    public static void refineryCyclesFollowTheirRecipes(GameTestHelper h) {
        var api = MioIcifAPI.instance().getRecipeRegistrationAPI();
        var hot = new FluidStack(BuiltInRegistries.FLUID.get(id("hotwater")), 1);
        var biomass = new FluidStack(BuiltInRegistries.FLUID.get(id("biomass")), 1);
        h.assertTrue(api.registerFluidRefiningRecipe(ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, "gametest_refine_hot_water"),
            Ingredient.EMPTY, hot, new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1), 4, 0), "the hot water recipe was not registered");
        h.assertTrue(api.registerFluidRefiningRecipe(ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, "gametest_refine_biomass"),
            Ingredient.EMPTY, biomass, new FluidStack(BuiltInRegistries.FLUID.get(id("biogas")), 1), 6, 5), "the biomass recipe was not registered");
        api.syncToRecipeManager();
        BlockEntity free = refinery(h, new BlockPos(1, 0, 1), "mio_icif:hotwater"), paid = refinery(h, new BlockPos(3, 0, 1), "mio_icif:biomass");
        h.runAfterDelay(15, () -> {
            int freeMax = (Integer) call(free, "getMaxProgress"), paidMax = (Integer) call(paid, "getMaxProgress");
            long freeEu = (Long) call(free, "getEffectiveEnergyPerTick"), paidEu = (Long) call(paid, "getEffectiveEnergyPerTick");
            int water = ((IFluidHandler) call(free, "getOutputTank")).getFluidInTank(0).getAmount();
            int biogas = ((IFluidHandler) call(paid, "getOutputTank")).getFluidInTank(0).getAmount();
            h.assertTrue(freeMax == 4 && freeEu == 0, "the hot water cycle: " + freeMax + " ticks, " + freeEu + " EU/t (not 4 ticks, 0 EU/t)");
            h.assertTrue(water >= 2, "without power the 4-tick, 0 EU/t recipe made " + water + " mB in 15 ticks");
            h.assertTrue(paidMax == 6 && paidEu == 5, "the biomass cycle: " + paidMax + " ticks, " + paidEu + " EU/t (not 6 ticks, 5 EU/t)");
            h.assertTrue(biogas == 0, "without power the 5 EU/t recipe made " + biogas + " mB");
            h.succeed();
        });
    }
}
