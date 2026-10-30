package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle;
import com.miophas.singularity_iteration.core.runtime.reactor.SteamReactorCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.36. Phase 0: a breeder that finishes in a reactor (lithium cell -> tritium cell, isotope rod ->
 * re-enriched uranium) no longer stalls the reactor; the tritium cell stays in its slot (IC2 isUsefulItem), re-enriched
 * uranium is thrown out at the start of the next cycle. Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1136GameTests {
    private Round1136GameTests() {}

    private static final BlockPos REACTOR = new BlockPos(2, 1, 2);

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }

    private static ItemStack breeder(GameTestHelper h, String suffix, int stored) {
        return component(h, "reactor/item_reactor_" + suffix, stored, 10000);
    }

    /** A reactor component holding {@code stored} of {@code max} (breeding progress, heat). */
    private static ItemStack component(GameTestHelper h, String path, int stored, int max) {
        CompoundTag tag = (CompoundTag) item(path).save(h.getLevel().registryAccess());
        CompoundTag components = tag.getCompound("components");
        CompoundTag data = new CompoundTag();
        data.putInt("stored_value", stored); data.putInt("max_value", max);
        components.put("mio_icif:reactor_component_data", data); tag.put("components", components);
        return ItemStack.parse(h.getLevel().registryAccess(), tag).orElseThrow();
    }

    private static int fuelRemaining(GameTestHelper h, ItemStack stack) {
        return ((CompoundTag) stack.save(h.getLevel().registryAccess())).getCompound("components")
            .getCompound("mio_icif:fuel_rod_durability").getInt("remaining_uses");
    }

    /** A running reactor: one uranium rod (slot 10) next to a breeder one gain short of its threshold (slot 11). */
    private static IReactorController reactor(GameTestHelper h, String breeder) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        BlockEntity entity = h.getBlockEntity(REACTOR);
        IReactorController reactor = (IReactorController) entity;
        h.assertTrue(reactor.getItemHandler().insertItem(10, item("reactor/item_reactor_uranium_simple"), false).isEmpty(), "Fuel rejected");
        h.assertTrue(reactor.getItemHandler().insertItem(11, breeder(h, breeder, 9999), false).isEmpty(), "Breeder rejected");
        // hull 3000 HU: a lithium cell gains 3000 / 3000 = 1 per pulse, so the first cycle finishes it (IC2 ItemReactorLithiumCell)
        ArmoryServiceGameTests.call(ArmoryServiceGameTests.call(entity, "getHeatStorage"), "setHeat", 3000L);
        h.setBlock(REACTOR.east(), Blocks.REDSTONE_BLOCK);
        return reactor;
    }

    @GameTest(batch = "round1136_breeding", template = "empty", timeoutTicks = 80)
    public static void finishedLithiumCellStaysAsTritiumAndTheReactorKeepsRunning(GameTestHelper h) {
        IReactorController reactor = reactor(h, "lithium_cell");
        h.runAfterDelay(25, () -> {
            var slot = reactor.getItemHandler().getStackInSlot(11);
            h.assertTrue(slot.is(BuiltInRegistries.ITEM.get(id("normal/item_tritium_cell"))), "Lithium cell did not become a tritium cell: " + slot);
            h.assertTrue(fuelRemaining(h, reactor.getItemHandler().getStackInSlot(10)) == 19999, "The breeding cycle did not commit");
            h.runAfterDelay(20, () -> {
                h.assertTrue(reactor.getItemHandler().getStackInSlot(11).is(BuiltInRegistries.ITEM.get(id("normal/item_tritium_cell"))),
                    "The tritium cell must stay in the reactor");
                h.assertTrue(fuelRemaining(h, reactor.getItemHandler().getStackInSlot(10)) == 19998, "The reactor stalled after breeding");
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ phase 1: Mining Laser

    private static ItemStack laser(String mode) {
        ItemStack stack = item("item_tool_laser_miner");
        ((IBatteryItem) stack.getItem()).setEnergy(stack, 300_000);
        CompoundTag data = new CompoundTag();
        data.putInt("ScexLaserMode", laserMode(mode).ordinal());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    private static Enum<?> laserMode(String name) {
        try {
            Class<?> mode = Class.forName("com.miophas.singularity_iteration.common.item.tools.mio_icif_laser_miner$Mode");
            for (Object constant : mode.getEnumConstants()) if (((Enum<?>) constant).name().equals(name)) return (Enum<?>) constant;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        throw new AssertionError("No laser mode " + name);
    }

    /**
     * The 3x3 mode mines the 3x3 around the aimed block only: the chests beside the shooter and the ceiling above the head stay
     * (before, nine parallel bolts from the eyes broke them); a chest in the outer ring of the 3x3 stays too.
     */
    @GameTest(batch = "round1136_laser", template = "reactor_loop", timeoutTicks = 60)
    public static void laserThreeByThreeMinesAroundTheAimedBlockOnly(GameTestHelper h) {
        for (int x = 0; x <= 6; x++) for (int y = 1; y <= 5; y++) for (int z = 0; z <= 6; z++) h.setBlock(x, y, z, Blocks.AIR);
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) h.setBlock(x, 0, z, Blocks.STONE);
        BlockPos chestOnRing = new BlockPos(4, 3, 5);
        for (int x = 2; x <= 4; x++) for (int y = 1; y <= 3; y++) h.setBlock(x, y, 5, Blocks.STONE);
        h.setBlock(chestOnRing, Blocks.CHEST);
        BlockPos left = new BlockPos(2, 2, 1), right = new BlockPos(4, 2, 1), ceiling = new BlockPos(3, 3, 1);
        h.setBlock(left, Blocks.CHEST); h.setBlock(right, Blocks.CHEST); h.setBlock(ceiling, Blocks.STONE);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        BlockPos feet = h.absolutePos(new BlockPos(3, 1, 1));
        player.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0F, 0.0F);   // yaw 0: facing +z, at the wall
        ItemStack stack = laser("THREE_BY_THREE");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        h.assertTrue(stack.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "The laser did not fire");
        h.runAfterDelay(15, () -> {
            for (int x = 2; x <= 4; x++) for (int y = 1; y <= 3; y++) {
                BlockPos pos = new BlockPos(x, y, 5);
                if (pos.equals(chestOnRing)) continue;
                h.assertBlockPresent(Blocks.AIR, pos);
            }
            h.assertBlockPresent(Blocks.CHEST, chestOnRing);
            h.assertBlockPresent(Blocks.CHEST, left);
            h.assertBlockPresent(Blocks.CHEST, right);
            h.assertBlockPresent(Blocks.STONE, ceiling);
            h.succeed();
        });
    }

    /**
     * The Ultra-High Energy Blast: 32767 for a direct hit and for every entity within the blast radius (behind cover too);
     * between the radius and twice the radius the vanilla falloff stays.
     */
    @GameTest(batch = "round1136_laser", template = "reactor_loop", timeoutTicks = 40)
    public static void laserUltraBlastHitsEverythingInTheRadiusFor32767(GameTestHelper h) {
        try {
            Object nuclear = laserMode("NUCLEAR_EXPLOSIVE");
            var damageField = nuclear.getClass().getDeclaredField("damage"); damageField.setAccessible(true);
            h.assertTrue(damageField.getFloat(nuclear) == 32767.0F, "Direct hit damage is not 32767");
            for (int x = 0; x <= 12; x++) for (int y = 1; y <= 4; y++) for (int z = 0; z <= 12; z++) h.setBlock(x, y, z, Blocks.AIR);
            ServerPlayer source = h.makeMockServerPlayerInLevel();
            Class<?> type = Class.forName("com.miophas.singularity_iteration.common.item.tools.LaserUltraBlastDamage");
            var constructor = type.getDeclaredConstructor(Entity.class); constructor.setAccessible(true);
            var calculator = (ExplosionDamageCalculator) constructor.newInstance(source);
            Vec3 center = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 1, 6)));
            var explosion = new Explosion(h.getLevel(), source, center.x, center.y, center.z, 4.0F, false, Explosion.BlockInteraction.KEEP);
            for (int y = 1; y <= 4; y++) for (int z = 4; z <= 8; z++) h.setBlock(2, y, z, Blocks.OBSIDIAN);   // cover
            var covered = h.spawn(EntityType.PIG, new BlockPos(4, 1, 6));   // 3 blocks away, behind obsidian
            var outside = h.spawn(EntityType.PIG, new BlockPos(7, 1, 6));   // 6 blocks away: between r and 2r
            h.assertTrue(calculator.getEntityDamageAmount(explosion, covered) == 32767.0F, "A target behind cover in the radius must take 32767");
            float far = calculator.getEntityDamageAmount(explosion, outside);
            h.assertTrue(far < 32767.0F, "Beyond the radius the vanilla falloff must apply, got " + far);
            h.succeed();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    // ------------------------------------------------------------------ phase 2: market tag matching

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> common(String name) { return Round33GameTests.common(name); }

    /**
     * Bounties and commissions take any item sharing a material tag of the requested one ({@code c:gears/si_test_unification}
     * holds brick and nether brick in the test datapack): the requested item is taken first, a named (not plain) stack never
     * counts, and a broad parent tag such as {@code c:ingots} is never a material tag.
     */
    @GameTest(batch = "round1136_market", template = "empty", timeoutTicks = 20)
    public static void marketAcceptsItemsOfTheSameMaterialTag(GameTestHelper h) {
        Class<?> matchType = common("exchange.ItemMatch");
        Object match = call(matchType, "of", "minecraft:brick", "");
        h.assertTrue((boolean) call(match, "test", new ItemStack(net.minecraft.world.item.Items.NETHER_BRICK)), "Nether brick must count for brick");
        h.assertTrue(!(boolean) call(match, "test", new ItemStack(net.minecraft.world.item.Items.IRON_INGOT)), "Iron must not count for brick");
        ItemStack named = new ItemStack(net.minecraft.world.item.Items.NETHER_BRICK);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("x"));
        h.assertTrue(!(boolean) call(match, "test", named), "A named stack is not plain");
        h.assertTrue(!(boolean) call(matchType, "materialTag", ResourceLocation.parse("c:ingots")), "A parent tag is no material tag");
        h.assertTrue((boolean) call(matchType, "materialTag", ResourceLocation.parse("c:ingots/copper")), "c:ingots/copper is a material tag");

        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.NETHER_BRICK, 3));
        player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.BRICK, 2));
        try {
            Object plan = common("exchange.InventoryPlan").getConstructor(net.minecraft.world.entity.player.Inventory.class).newInstance(player.getInventory());
            h.assertTrue((long) call(plan, "count", match) == 5L, "Five accepted items expected");
            h.assertTrue((boolean) call(plan, "removeMain", match, 3L), "Taking three failed");
            call(plan, "commit");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        h.assertTrue(player.getInventory().getItem(1).isEmpty(), "The requested brick must be taken first");
        h.assertTrue(player.getInventory().getItem(0).getCount() == 2, "One nether brick must be taken after the bricks");
        h.succeed();
    }

    // ------------------------------------------------------------------ phase 2: armor enchanting

    /**
     * Bronze and hazmat take every armor enchantment, Mending and Unbreaking too; nano and quantum (no durability) take the
     * enchanting table and every armor enchantment of their slot, but never Mending or Unbreaking.
     */
    @GameTest(batch = "round1136_armor", template = "empty", timeoutTicks = 20)
    public static void suitArmorTakesEnchantmentsButNanoAndQuantumNeverMending(GameTestHelper h) {
        var enchantments = h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        java.util.function.Function<net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>, net.minecraft.world.item.enchantment.Enchantment> e =
            key -> enchantments.getHolderOrThrow(key).value();
        for (String piece : new String[]{"bronze", "hazmat"}) {
            ItemStack chest = item("armor/item_armor_" + piece + "_chestplate"), boots = item("armor/item_armor_" + piece + "_boots");
            h.assertTrue(chest.isEnchantable(), piece + " chestplate is not enchantable");
            h.assertTrue(e.apply(net.minecraft.world.item.enchantment.Enchantments.PROTECTION).canEnchant(chest), piece + ": no Protection");
            h.assertTrue(e.apply(net.minecraft.world.item.enchantment.Enchantments.MENDING).canEnchant(chest), piece + ": no Mending");
            h.assertTrue(e.apply(net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING).canEnchant(boots), piece + ": no Feather Falling");
        }
        for (String piece : new String[]{"nano", "quantum"}) {
            ItemStack helmet = item("armor/item_armor_" + piece + "_helmet"), chest = item("armor/item_armor_" + piece + "_chestplate");
            ItemStack legs = item("armor/item_armor_" + piece + "_leggings"), boots = item("armor/item_armor_" + piece + "_boots");
            for (ItemStack s : new ItemStack[]{helmet, chest, legs, boots}) {
                h.assertTrue(s.isEnchantable(), s + " is not enchantable at the table");
                h.assertTrue(s.getEnchantmentValue() > 0, s + " has no enchantment value");
                h.assertTrue(e.apply(net.minecraft.world.item.enchantment.Enchantments.PROTECTION).canEnchant(s), s + ": no Protection");
                h.assertTrue(!e.apply(net.minecraft.world.item.enchantment.Enchantments.MENDING).canEnchant(s), s + " must not take Mending");
                h.assertTrue(!e.apply(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING).canEnchant(s), s + " must not take Unbreaking");
            }
            h.assertTrue(e.apply(net.minecraft.world.item.enchantment.Enchantments.RESPIRATION).canEnchant(helmet), piece + ": no Respiration");
            h.assertTrue(e.apply(net.minecraft.world.item.enchantment.Enchantments.THORNS).canEnchant(chest), piece + ": no Thorns");
            h.assertTrue(e.apply(net.minecraft.world.item.enchantment.Enchantments.SWIFT_SNEAK).canEnchant(legs), piece + ": no Swift Sneak");
            h.assertTrue(e.apply(net.minecraft.world.item.enchantment.Enchantments.DEPTH_STRIDER).canEnchant(boots), piece + ": no Depth Strider");
            h.assertTrue(e.apply(net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE).canEnchant(chest), piece + ": no Curse of Binding");
        }
        h.assertTrue(!item("armor/item_armor_jetpack_electric").isEnchantable(), "The electric jetpack must stay unenchantable at the table");
        h.succeed();
    }

    // ------------------------------------------------------------------ phase 2: Miner

    /**
     * The Miner's drill slot takes the Mining Laser; with the drill slot empty the Miner pulls its pipe column back up (IC2C),
     * the lowest pipe first, into the pipe slot; every pipe comes back (pipe and tip both drop one).
     */
    @GameTest(batch = "round1136_miner", template = "reactor_loop", timeoutTicks = 120)
    public static void minerTakesTheLaserAndRetractsItsPipes(GameTestHelper h) {
        for (int x = 1; x <= 5; x++) for (int y = 1; y <= 6; y++) for (int z = 1; z <= 5; z++) h.setBlock(x, y, z, Blocks.AIR);
        BlockPos miner = new BlockPos(3, 6, 3);
        h.setBlock(miner, BuiltInRegistries.BLOCK.get(id("producer/block_miner_elc")));
        var pipe = BuiltInRegistries.BLOCK.get(id("produce/block_mining_pipe"));
        var tip = BuiltInRegistries.BLOCK.get(id("produce/block_mining_tip"));
        for (int y = 3; y <= 5; y++) h.setBlock(3, y, 3, pipe);
        h.setBlock(3, 2, 3, tip);
        BlockEntity entity = h.getBlockEntity(miner);
        ((com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity) entity).getEnergyStorageInternal().setEnergy(10_000);
        var items = (net.neoforged.neoforge.items.IItemHandler) call(entity, "getItemHandler");
        h.assertTrue(items.insertItem(17, laser("MINING"), true).isEmpty(), "The drill slot must take the Mining Laser");
        h.assertTrue(items.getStackInSlot(17).isEmpty(), "The drill slot must stay empty for the retraction test");
        h.runAfterDelay(60, () -> {
            for (int y = 2; y <= 5; y++) h.assertBlockPresent(Blocks.AIR, new BlockPos(3, y, 3));
            ItemStack pipes = items.getStackInSlot(18);
            h.assertTrue(pipes.is(pipe.asItem()) && pipes.getCount() == 4, "All four pipes must come back to the pipe slot, got " + pipes);
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ phase 3: fuels

    private static ReactorCycle.Part[] grid(ReactorCycle.Part a, ReactorCycle.Part b) {
        var parts = new ReactorCycle.Part[54];
        parts[10] = a; parts[11] = b;
        return parts;
    }

    /**
     * The proton rod gives 3x the EU and 1.5x the heat of uranium; the tritium neutron source gives each adjacent rod one more
     * pulse (heat and EU), decays by one per cycle and gives 1 HU; the lithium cell breeds 1 + hull/3000 per pulse and gives
     * 1 HU per absorbed pulse. The ten-argument profile keeps factors of 1.
     */
    @GameTest(batch = "round1136_fuel", template = "empty", timeoutTicks = 20)
    public static void protonTritiumAndLithiumFollowTheirProfiles(GameTestHelper h) {
        var uranium = ReactorCycle.Profile.fuel(1, false);
        var proton = ReactorCycle.Profile.fuel(1, false, 3.0f, 1.5f);
        var alone = ReactorCycle.step(grid(new ReactorCycle.Part(uranium, 0, 100), null), 3, 0, true);
        var protonAlone = ReactorCycle.step(grid(new ReactorCycle.Part(proton, 0, 100), null), 3, 0, true);
        h.assertTrue(alone.euPerTick() == 5 && alone.generatedHeat() == 4, "uranium alone: " + alone.euPerTick() + " EU, " + alone.generatedHeat() + " HU");
        h.assertTrue(protonAlone.euPerTick() == 15 && protonAlone.generatedHeat() == 6,
            "proton alone: " + protonAlone.euPerTick() + " EU, " + protonAlone.generatedHeat() + " HU");
        h.assertTrue(new ReactorCycle.Profile(ReactorCycle.Kind.FUEL, 1, false, 0, 0, 0, 0, 0, 0, 0).euFactor() == 1.0f, "old profiles keep factor 1");

        var tritium = ReactorCycle.Profile.neutronSource(10, 1);
        var boosted = ReactorCycle.step(grid(new ReactorCycle.Part(uranium, 0, 100), new ReactorCycle.Part(tritium, 0, 0)), 3, 0, true);
        h.assertTrue(boosted.euPerTick() == 10, "a neutron source must add one pulse of EU: " + boosted.euPerTick());
        h.assertTrue(boosted.generatedHeat() == 12 + 1, "two pulses of heat (12) and 1 HU of decay: " + boosted.generatedHeat());
        h.assertTrue(boosted.parts()[11].stored() == 1, "the source decays by one per cycle");
        var spent = ReactorCycle.step(grid(new ReactorCycle.Part(uranium, 0, 100), new ReactorCycle.Part(tritium, 10, 0)), 3, 0, true);
        h.assertTrue(spent.euPerTick() == 5 && spent.generatedHeat() == 4, "a decayed source does nothing: " + spent.euPerTick());

        var lithium = ReactorCycle.Profile.lithium(10000, 1, 1);
        var bred = ReactorCycle.step(grid(new ReactorCycle.Part(uranium, 0, 100), new ReactorCycle.Part(lithium, 0, 0)), 3, 0, true);
        h.assertTrue(bred.parts()[11].stored() == 1, "a cool lithium cell still breeds 1 per pulse: " + bred.parts()[11].stored());
        h.assertTrue(bred.hullHeat() == 12 + 1, "two pulses of heat plus 1 HU from the lithium reaction: " + bred.hullHeat());
        h.assertTrue(bred.euPerTick() == 5, "a breeder adds no EU (IC2)");
        h.succeed();
    }

    // ------------------------------------------------------------------ phase 3: deepslate iridium ore

    /** The ore drops small iridium dust with a pickaxe and itself with Silk Touch. */
    @GameTest(batch = "round1136_iridium", template = "empty", timeoutTicks = 20)
    public static void deepslateIridiumOreDropsSmallDust(GameTestHelper h) {
        var ore = BuiltInRegistries.BLOCK.get(id("block_ore_iridium_in_deep"));
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ore);
        var abs = h.absolutePos(pos);
        var pick = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        var drops = net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(abs), h.getLevel(), abs, null, null, pick);
        h.assertTrue(drops.size() == 1 && drops.get(0).is(BuiltInRegistries.ITEM.get(id("resource/item_iridium_dust_small"))), "drops: " + drops);
        var silk = pick.copy();
        silk.enchant(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        var kept = net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(abs), h.getLevel(), abs, null, null, silk);
        h.assertTrue(kept.size() == 1 && kept.get(0).is(ore.asItem()), "silk touch: " + kept);
        h.assertTrue(h.getLevel().getBlockState(abs).is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL), "needs a diamond tool");
        h.succeed();
    }

    /**
     * A nuclear blast region turns the deepslate ores inside its blast (radius 3 here) into deepslate iridium ore, through
     * the fallout pass; an ore outside the radius stays. The region is small so the fallout stays inside the structure.
     */
    @GameTest(batch = "round1136_iridium", template = "reactor_loop", timeoutTicks = 200)
    public static void nuclearBlastTurnsDeepslateOresIntoIridiumOre(GameTestHelper h) {
        BlockPos inside = new BlockPos(6, 3, 6), below = new BlockPos(6, 1, 6), outside = new BlockPos(6, 3, 10);
        h.setBlock(inside, Blocks.DEEPSLATE_DIAMOND_ORE);
        h.setBlock(below, Blocks.DEEPSLATE_COPPER_ORE);
        h.setBlock(outside, Blocks.DEEPSLATE_DIAMOND_ORE);
        var manager = common("blockentity.reactor.NuclearFalloutManager");
        java.util.UUID id = java.util.UUID.randomUUID();
        var center = net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(inside));
        call(manager, "register", h.getLevel(), id, center, 3, 3);
        // 0.1.10.37: a blast turns 0.02 % of the ores by default; this blast turns all of them, to check where it works
        com.miophas.singularity_iteration.core.runtime.radiation.NuclearFalloutState.get(h.getLevel()).regions.get(id).transmuteChance = 1.0;
        call(manager, "ready", h.getLevel(), id);
        var iridium = BuiltInRegistries.BLOCK.get(id("block_ore_iridium_in_deep"));
        h.succeedWhen(() -> {
            h.assertBlockPresent(iridium, inside);
            h.assertBlockPresent(iridium, below);
            h.assertBlockPresent(Blocks.DEEPSLATE_DIAMOND_ORE, outside);
            call(manager, "remove", h.getLevel(), id);
        });
    }

    // ------------------------------------------------------------------ phase 4.1: the Scanner prices unpriced items

    /**
     * Items the published generation does not know get a Scanner price: a sculk sensor (no recipe) an estimate, a calibrated
     * sculk sensor (crafted from it and amethyst) a price from recipes, at least the sculk sensor's; a spawn egg none.
     */
    @GameTest(batch = "round1136_scanner", template = "empty", timeoutTicks = 20)
    public static void scannerPricesItemsWithoutAPublishedPrice(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var pricing = common("processing.scanner.ScannerPricing");
        ItemStack sensor = new ItemStack(net.minecraft.world.item.Items.SCULK_SENSOR);
        ItemStack calibrated = new ItemStack(net.minecraft.world.item.Items.CALIBRATED_SCULK_SENSOR);
        h.assertTrue(com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.publishedQuote(server, sensor) == null,
            "the test needs an unpublished sculk sensor");
        Object sensorPrice = call(pricing, "price", server, sensor);
        h.assertTrue(sensorPrice != null && "ESTIMATE".equals(String.valueOf(call(sensorPrice, "source"))), "sculk sensor: " + sensorPrice);
        if (com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.publishedQuote(server, calibrated) == null) {
            Object calibratedPrice = call(pricing, "price", server, calibrated);
            h.assertTrue(calibratedPrice != null && "RECIPE".equals(String.valueOf(call(calibratedPrice, "source"))),
                "calibrated sculk sensor: " + calibratedPrice);
            h.assertTrue((double) call(calibratedPrice, "buckets") >= (double) call(sensorPrice, "buckets"),
                "a crafted item costs at least its input");
        }
        h.assertTrue(call(pricing, "price", server, new ItemStack(net.minecraft.world.item.Items.PIG_SPAWN_EGG)) == null,
            "a spawn egg is never priced");
        h.succeed();
    }

    /** A Scanner given an unpriced, renamed item registers a Scanner price for the plain item (what the Replicator reads). */
    @GameTest(batch = "round1136_scanner", template = "empty", timeoutTicks = 40)
    public static void scannerRegistersAPriceTheReplicatorCanRead(GameTestHelper h) {
        var server = h.getLevel().getServer();
        // Prices need a published generation (as on a real server). The GameTest server's own generation may be missing:
        // then an empty one stands in for this test.
        boolean installed = com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.generation(server) < 0;
        if (installed) com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.install(server,
            new com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex(16384));
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(id("producer/block_scanner_elc")));
        BlockEntity scanner = h.getBlockEntity(pos);
        ((com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity) scanner).getEnergyStorageInternal().setEnergy(100_000);
        ItemStack named = new ItemStack(net.minecraft.world.item.Items.SCULK_SENSOR);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("x"));
        var items = (net.neoforged.neoforge.items.IItemHandler) call(scanner, "getItemHandler");
        h.assertTrue(items.insertItem(0, named, false).isEmpty(), "the scanner did not take the item");
        h.runAfterDelay(5, () -> {
            try {
                var plain = new ItemStack(net.minecraft.world.item.Items.SCULK_SENSOR);
                h.assertTrue(com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.quote(server, plain) != null, "no quote for the plain item");
                h.assertTrue(com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.isScanned(server, plain), "the quote is not a Scanner price");
                h.assertTrue(com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.publishedQuote(server, plain) == null,
                    "a Scanner price must not become a published (market) price");
                h.succeed();
            } finally {
                if (installed) com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.remove(server);
            }
        });
    }

    /** 0.1.10.38: re-enriched uranium stays in its slot (it was thrown out before) and the reactor keeps running. */
    @GameTest(batch = "round1136_breeding", template = "empty", timeoutTicks = 80)
    public static void finishedIsotopeRodStaysAsReEnrichedUraniumAndTheReactorKeepsRunning(GameTestHelper h) {
        IReactorController reactor = reactor(h, "isotope_rod");
        h.runAfterDelay(25, () -> {
            h.assertTrue(reactor.getItemHandler().getStackInSlot(11).is(BuiltInRegistries.ITEM.get(id("normal/item_re_enriched_uranium"))),
                "Isotope rod did not become re-enriched uranium");
            h.runAfterDelay(20, () -> {
                h.assertTrue(reactor.getItemHandler().getStackInSlot(11).is(BuiltInRegistries.ITEM.get(id("normal/item_re_enriched_uranium"))),
                    "Re-enriched uranium must stay in its slot");
                h.assertTrue(fuelRemaining(h, reactor.getItemHandler().getStackInSlot(10)) == 19998, "The reactor stalled after breeding");
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ phase 4.2: steam reactor and steam turbine

    private static ReactorCycle.Part steamVent(int stored) {
        return new ReactorCycle.Part(ReactorCycle.Profile.steamVent(1100, 12, 0), stored, 0);
    }

    /**
     * A steam vent turns its heat into steam: efficiency (heat - 100) / (1100 - 100), one unit of vent heat = 40 mB of steam from
     * 0.4 mB of water, no more than the water and the free steam room allow; what they cannot take stays in the vent. In any
     * other reactor it is an ordinary vent.
     */
    @GameTest(batch = "round1136_steam", template = "empty", timeoutTicks = 20)
    public static void steamVentsTurnTheirHeatIntoSteamWithinWaterAndRoom(GameTestHelper h) {
        // 1000 HU: floor(12 * 0.9) = 10 units = 400 mB of steam from 4 mB of water
        var hot = SteamReactorCycle.step(grid(null, steamVent(1000)), 3, 0, true, 10_000, 128_000);
        h.assertTrue(hot.steamMade() == 400 && hot.waterUsed() == 4, "hot vent: " + hot.steamMade() + " mB from " + hot.waterUsed() + " mB");
        h.assertTrue(hot.cycle().parts()[11].stored() == 990 && hot.cycle().emittedHeat() == 0,
            "the steam heat leaves the vent and is not vented: " + hot.cycle().parts()[11].stored());
        var dry = SteamReactorCycle.step(grid(null, steamVent(1000)), 3, 0, true, 0, 128_000);
        h.assertTrue(dry.steamMade() == 0 && dry.cycle().parts()[11].stored() == 1000, "without water the heat stays in the vent");
        var full = SteamReactorCycle.step(grid(null, steamVent(1000)), 3, 0, true, 10_000, 200);
        h.assertTrue(full.steamMade() == 200 && full.waterUsed() == 2 && full.cycle().parts()[11].stored() == 995,
            "200 mB of room takes 5 units: " + full.steamMade() + " mB, vent " + full.cycle().parts()[11].stored());
        var cool = SteamReactorCycle.step(grid(null, steamVent(100)), 3, 0, true, 10_000, 128_000);
        h.assertTrue(cool.steamMade() == 0 && cool.cycle().parts()[11].stored() == 100, "a vent at 100 HU makes no steam");
        var electric = ReactorCycle.step(grid(null, steamVent(1000)), 3, 0, true);
        h.assertTrue(electric.parts()[11].stored() == 988 && electric.emittedHeat() == 12 && electric.steamHeat() == 0,
            "outside a steam reactor it vents 12: " + electric.parts()[11].stored());
        h.succeed();
    }

    /**
     * A steam reactor takes steam reactor chambers only (an electric chamber never joins it), makes no EU, turns water into
     * steam in its hot steam vents and pushes the steam into an adjacent steam turbine generator, which makes EU.
     */
    @GameTest(batch = "round1136_steam", template = "empty", timeoutTicks = 120)
    public static void steamReactorTakesSteamChambersAndFeedsATurbine(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_steam_reactor")));
        h.setBlock(REACTOR.north(), BuiltInRegistries.BLOCK.get(id("reactor/block_steam_reactor_chamber")));
        h.setBlock(REACTOR.above(), BuiltInRegistries.BLOCK.get(id("reactor/block_reactor_chamber")));
        IReactorController reactor = (IReactorController) h.getBlockEntity(REACTOR);
        IFluidHandler tanks = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(REACTOR), null);
        h.assertTrue(tanks != null && tanks.fill(new FluidStack(Fluids.WATER, 10_000), IFluidHandler.FluidAction.EXECUTE) == 10_000,
            "The steam reactor must take water");
        h.assertTrue(reactor.getItemHandler().insertItem(0, item("reactor/item_reactor_uranium_simple"), false).isEmpty(), "Fuel rejected");
        h.assertTrue(reactor.getItemHandler().insertItem(1, component(h, "reactor/item_reactor_steam_vent", 1000, 1100), false).isEmpty(), "Vent rejected");
        h.assertTrue(reactor.getItemHandler().insertItem(9, component(h, "reactor/item_reactor_steam_vent", 1000, 1100), false).isEmpty(), "Vent rejected");
        h.setBlock(REACTOR.west(), BuiltInRegistries.BLOCK.get(id("generator/block_steam_turbine")));
        h.setBlock(REACTOR.east(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(2, () -> {
            h.assertTrue(reactor.getAvailableColumns() == 4, "only the steam chamber adds a column: " + reactor.getAvailableColumns());
            BlockEntity electric = h.getLevel().getBlockEntity(h.absolutePos(REACTOR.above()));
            h.assertTrue(electric == null || call(electric, "getConnectedReactor") == null, "An electric chamber joined the steam reactor");
            BlockEntity steamChamber = h.getBlockEntity(REACTOR.north());
            h.assertTrue(call(steamChamber, "getConnectedReactor") == reactor, "The steam chamber did not join the steam reactor");
        });
        h.runAfterDelay(80, () -> {
            int water = tanks.getFluidInTank(0).getAmount();
            h.assertTrue(water < 10_000, "No water became steam");
            h.assertTrue(reactor.getCurrentEnergyGeneration() == 0, "A steam reactor makes no EU: " + reactor.getCurrentEnergyGeneration());
            BlockEntity turbine = h.getBlockEntity(REACTOR.west());
            long energy = (Long) call(call(turbine, "getEnergyStorage"), "getAmount");
            int steam = (Integer) call(turbine, "getFuelAmount");
            h.assertTrue(energy > 0, "The turbine made no EU (steam " + steam + " mB, water used " + (10_000 - water) + " mB)");
            // 2 mB of steam per EU: the EU never exceeds half the steam the used water made (100 mB of steam per mB of water)
            h.assertTrue(energy <= (10_000L - water) * 100 / 2, "Too much EU for the steam made: " + energy);
            h.succeed();
        });
    }
}
