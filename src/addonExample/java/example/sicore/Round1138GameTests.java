package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.38. Phase A: depleted rods stay in their slot (a reactor walled in by chambers lost the depleted quad rod
 * it threw out at its own position); a reactor chamber is a fluid port of its reactor. Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1138GameTests {
    private Round1138GameTests() {}

    private static final BlockPos REACTOR = new BlockPos(2, 2, 2);

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }

    /** A fuel rod with {@code remaining} of {@code max} cycles left. */
    private static ItemStack rod(GameTestHelper h, String path, int remaining, int max) {
        CompoundTag tag = (CompoundTag) item(path).save(h.getLevel().registryAccess());
        CompoundTag components = tag.getCompound("components");
        CompoundTag durability = new CompoundTag();
        durability.putInt("remaining_uses", remaining); durability.putInt("max_uses", max); durability.putInt("tick_counter", 0);
        components.put("mio_icif:fuel_rod_durability", durability); tag.put("components", components);
        return ItemStack.parse(h.getLevel().registryAccess(), tag).orElseThrow();
    }

    // ------------------------------------------------------------------ A2: depleted rods stay in their slot

    /**
     * A quad uranium rod one cycle from empty in a reactor with a chamber on all six faces (the usual quad build): the depleted
     * quad rod stays in its slot. Before, it was thrown out at the reactor's own position, inside the walled-in reactor block.
     */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 80)
    public static void depletedQuadRodStaysInAReactorWalledInByChambers(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        for (Direction side : Direction.values())
            h.setBlock(REACTOR.relative(side), BuiltInRegistries.BLOCK.get(id("reactor/block_reactor_chamber")));
        h.setBlock(REACTOR.east(2), Blocks.REDSTONE_BLOCK);   // powers the east chamber, which enables the reactor
        IReactorController reactor = (IReactorController) h.getBlockEntity(REACTOR);
        h.assertTrue(reactor.getItemHandler().insertItem(0, rod(h, "reactor/item_reactor_uranium_quad", 1, 20000), false).isEmpty(), "Fuel rejected");
        var depleted = BuiltInRegistries.ITEM.get(id("reactor/item_reactor_uranium_quad_deplete"));
        h.runAfterDelay(25, () -> {
            h.assertTrue(reactor.getAvailableColumns() == 9, "six chambers give 9 columns: " + reactor.getAvailableColumns());
            h.assertTrue(reactor.getItemHandler().getStackInSlot(0).is(depleted), "The quad rod did not burn out: " + reactor.getItemHandler().getStackInSlot(0));
            h.runAfterDelay(20, () -> {
                h.assertTrue(reactor.getItemHandler().getStackInSlot(0).is(depleted), "The depleted quad rod must stay in its slot");
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ A5: reactor chamber fluid port

    /** Water pumped into a reactor chamber reaches its reactor's liquid cooling (the chamber had no fluid port before). */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 20)
    public static void reactorChamberIsAFluidPortOfItsReactor(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        h.setBlock(REACTOR.north(), BuiltInRegistries.BLOCK.get(id("reactor/block_reactor_chamber")));
        h.runAfterDelay(2, () -> {
            IFluidHandler chamber = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(REACTOR.north()), Direction.NORTH);
            h.assertTrue(chamber != null, "The reactor chamber has no fluid handler");
            h.assertTrue(chamber.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "The chamber refused coolant");
            IFluidHandler core = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(REACTOR), null);
            h.assertTrue(core != null && core.getFluidInTank(0).getAmount() == 1000, "The coolant did not reach the reactor");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ A3: reactor output above 8192 EU/t, reload, wireless

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Object field(Object target, String name) {
        try {
            for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
                try { var f = k.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
                catch (NoSuchFieldException next) { /* superclass */ }
            }
            throw new AssertionError("no field " + name);
        } catch (IllegalAccessException e) { throw new AssertionError(name, e); }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
                try { var f = k.getDeclaredField(name); f.setAccessible(true); f.set(target, value); return; }
                catch (NoSuchFieldException next) { /* superclass */ }
            }
            throw new AssertionError("no field " + name);
        } catch (IllegalAccessException e) { throw new AssertionError(name, e); }
    }

    private static long stored(Object block) { return (Long) call(call(block, "getEnergyStorageInternal"), "getAmount"); }

    /**
     * A reactor producing 20,000 EU/t delivers more than 8192 EU in one tick to a sink that takes it (a GESU input module): it
     * sends several IV packets per tick (up to 64) instead of one, so the output above 8192 EU/t is no longer lost.
     */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 80)
    public static void reactorDeliversMoreThan8192EuPerTick(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        h.setBlock(REACTOR.east(), BuiltInRegistries.BLOCK.get(id("wiring/block_gesu_input_iv")));
        h.setBlock(REACTOR.above(), Blocks.REDSTONE_BLOCK);
        IReactorController reactor = (IReactorController) h.getBlockEntity(REACTOR);
        h.assertTrue((Integer) call(reactor, "getPacketCount") == 64, "the reactor must send up to 64 packets");
        h.assertTrue(reactor.getItemHandler().insertItem(10, item("reactor/item_reactor_uranium_simple"), false).isEmpty(), "Fuel rejected");
        var sink = h.getBlockEntity(REACTOR.east());
        h.runAfterDelay(25, () -> {
            h.assertTrue(reactor.getCurrentEnergyGeneration() == 5, "the reactor did not run a cycle: " + reactor.getCurrentEnergyGeneration());
            Object rate = field(reactor, "rate");
            try {
                setField(reactor, "rate", rate.getClass().getConstructor(long.class, long.class).newInstance(20_000L, 0L));
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            long before = stored(sink);
            h.runAfterDelay(2, () -> {
                long perTick = (stored(sink) - before) / 2;
                h.assertTrue(perTick > 8192, "the reactor delivered only " + perTick + " EU/t");
                h.succeed();
            });
        });
    }

    /**
     * A saved rate above 8192 EU/t loads (before, the reactor was put on hold for good after a reload), and a reactor already held
     * for that reason in an older save is freed.
     */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 20)
    public static void reactorReloadsAFastRateAndFreesAHeldReactor(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        var reactor = h.getBlockEntity(REACTOR);
        var registries = h.getLevel().registryAccess();
        CompoundTag tag = reactor.saveWithoutMetadata(registries);
        CompoundTag own = tag.getCompound("scex_reactor");
        h.assertTrue(own.contains("rate"), "no saved reactor state: " + tag.getAllKeys());
        own.putLong("rate", 20_000L);
        reactor.loadWithComponents(tag, registries);
        h.assertTrue(((CompoundTag) field(reactor, "hold")).isEmpty(), "a 20,000 EU/t reactor was held after a reload: " + field(reactor, "hold"));
        h.assertTrue((Long) call(field(reactor, "rate"), "whole") == 20_000L, "the saved rate was not kept");

        CompoundTag held = own.copy();   // an older save: the hold written by the 8192 check
        CompoundTag hold = new CompoundTag();
        hold.put("invalid_rate", held.copy());
        own.put("hold", hold);
        reactor.loadWithComponents(tag, registries);
        h.assertTrue(((CompoundTag) field(reactor, "hold")).isEmpty(), "a reactor held by the old check was not freed: " + field(reactor, "hold"));
        h.succeed();
    }

    /** The wireless node takes the configured limit (default 64 IV packets per tick, it was 32768). */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 20)
    public static void wirelessNodeUsesTheConfiguredLimit(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("wiring/block_wireless_power_transmission_node")));
        var node = h.getBlockEntity(REACTOR);
        long limit = (Long) field(node, "transferSpeed");
        h.assertTrue(limit == 524_288L, "default wireless limit: " + limit);
        h.assertTrue((Long) call(call(node, "getEnergyStorageInternal"), "getCapacity") == limit * 6, "the buffer holds 6 ticks");
        h.succeed();
    }

    // ------------------------------------------------------------------ A7: autocrafting ingredients (data for tools/recipes_1138.py)

    /**
     * Writes {@code autocraft_ingredients.json} in the game directory: every electric item of the mod (an {@code IBatteryItem}, with
     * "full" when its default stack is fully charged), every machine block (a block with a block entity that a wrench dismantles
     * with its data; cables, pipes and crop sticks left out), and the item tags that contain one of them. tools/recipes_1138.py
     * turns these inputs of the crafting recipes into {@code mio_icif:electric} ingredients.
     */
    @GameTest(batch = "round1138_dump", template = "empty", timeoutTicks = 20)
    public static void dumpAutocraftIngredients(GameTestHelper h) {
        var items = new java.util.TreeMap<String, String>();
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            var key = entry.getKey().location();
            if (!key.getNamespace().equals("mio_icif")) continue;
            var item = entry.getValue();
            if (item instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem battery) {
                ItemStack stack = new ItemStack(item);
                long capacity = battery.getMaxEnergy(stack);
                items.put(key.toString(), capacity > 0 && battery.getEnergy(stack) >= capacity ? "electric_full" : "electric");
            } else if (item instanceof net.minecraft.world.item.BlockItem blockItem
                    && blockItem.getBlock() instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
                net.minecraft.world.level.block.entity.BlockEntity be;
                try { be = entityBlock.newBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState()); }
                catch (RuntimeException e) { continue; }
                if (be == null || be instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractWireBlockEntity) continue;
                String name = be.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
                // not machines: pipes, crop sticks, special cables (breaker, detector, fuse, splitter), foam, vanilla-type signs
                if (name.contains("pipe") || name.contains("crop") || name.contains("cable") || name.contains("foam")
                    || be.getClass().getName().startsWith("net.minecraft.")) continue;
                items.put(key.toString(), "machine");
            }
        }
        var tags = new java.util.TreeSet<String>();
        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            for (var holder : pair.getSecond())
                if (items.containsKey(BuiltInRegistries.ITEM.getKey(holder.value()).toString())) { tags.add(pair.getFirst().location().toString()); break; }
        });
        var out = new StringBuilder("{\n  \"items\": {\n");
        int n = 0;
        for (var e : items.entrySet()) out.append(n++ == 0 ? "" : ",\n").append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append('"');
        out.append("\n  },\n  \"tags\": [");
        n = 0;
        for (String t : tags) out.append(n++ == 0 ? "\n    \"" : ",\n    \"").append(t).append('"');
        out.append("\n  ]\n}\n");
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("autocraft_ingredients.json"), out.toString());
        } catch (java.io.IOException e) { throw new AssertionError("cannot write autocraft_ingredients.json", e); }
        h.assertTrue(items.values().stream().filter("electric_full"::equals).count() >= 30, "expected about 36 items charged by default");
        h.succeed();
    }

    private static boolean electric(net.minecraft.world.item.crafting.Ingredient ingredient) {
        return ingredient.isCustom() && id("electric").equals(
            net.neoforged.neoforge.registries.NeoForgeRegistries.INGREDIENT_TYPES.getKey(ingredient.getCustomIngredient().getType()));
    }

    /** The grid of a crafting recipe filled with the first stack each slot shows (the empty stacks of electric inputs). */
    private static net.minecraft.world.item.crafting.CraftingInput shownInputs(net.minecraft.world.item.crafting.CraftingRecipe recipe) {
        var stacks = new java.util.ArrayList<ItemStack>();
        for (var ingredient : recipe.getIngredients()) {
            ItemStack[] shown = ingredient.getItems();
            stacks.add(shown.length == 0 ? ItemStack.EMPTY : shown[0].copy());
        }
        return recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped
            ? net.minecraft.world.item.crafting.CraftingInput.of(shaped.getWidth(), shaped.getHeight(), stacks)
            : net.minecraft.world.item.crafting.CraftingInput.of(stacks.size(), 1, stacks);
    }

    /**
     * What autocrafting needs: every electric or machine input (mio_icif:electric) shows exactly the stack its own recipe makes
     * from the stacks that recipe shows. So a pattern recorded from a recipe matches what the sub-pattern crafts (36 items are
     * charged by default; a pattern recorded the full one while the craft gave an empty one, and the chain never autocrafted).
     */
    @GameTest(batch = "round1138_recipes", template = "empty", timeoutTicks = 20)
    public static void electricInputsShowWhatTheirRecipesMake(GameTestHelper h) {
        var level = h.getLevel();
        var recipes = level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
        var producers = new java.util.HashMap<net.minecraft.world.item.Item, net.minecraft.world.item.crafting.CraftingRecipe>();
        for (var holder : recipes) {
            if (!holder.id().getNamespace().equals("mio_icif") || holder.value().isSpecial()) continue;
            producers.putIfAbsent(holder.value().getResultItem(level.registryAccess()).getItem(), holder.value());
        }
        int converted = 0, checked = 0;
        var mismatches = new java.util.ArrayList<String>();
        for (var holder : recipes) {
            if (!holder.id().getNamespace().equals("mio_icif")) continue;
            for (var ingredient : holder.value().getIngredients()) {
                if (!electric(ingredient)) continue;
                converted++;
                for (ItemStack shown : ingredient.getItems()) {
                    var producer = producers.get(shown.getItem());
                    if (producer == null) continue;
                    var input = shownInputs(producer);
                    if (!producer.matches(input, level)) continue;
                    checked++;
                    ItemStack made = producer.assemble(input, level.registryAccess());
                    if (!ItemStack.isSameItemSameComponents(made, shown))
                        mismatches.add(holder.id() + ": shows " + shown.getComponentsPatch() + ", its recipe makes " + made.getComponentsPatch());
                }
            }
        }
        h.assertTrue(converted >= 250, "only " + converted + " electric inputs");
        h.assertTrue(checked >= 100, "only " + checked + " inputs had a recipe to check");
        h.assertTrue(mismatches.isEmpty(), mismatches.size() + " inputs show another stack than their recipe makes, first: "
            + (mismatches.isEmpty() ? "" : mismatches.get(0)));
        h.succeed();
    }

    /** The chainsaw's power unit input takes a full and an empty power unit and shows the empty one (the default is full). */
    @GameTest(batch = "round1138_recipes", template = "empty", timeoutTicks = 20)
    public static void electricInputTakesAnyChargeAndShowsTheEmptyStack(GameTestHelper h) {
        var holder = h.getLevel().getRecipeManager().byKey(id("shaped_152_chainsaw")).orElseThrow();
        var unit = BuiltInRegistries.ITEM.get(id("item_tool_power_unit"));
        var ingredient = holder.value().getIngredients().stream().filter(i -> java.util.Arrays.stream(i.getItems()).anyMatch(s -> s.is(unit)))
            .findFirst().orElseThrow();
        h.assertTrue(electric(ingredient), "the chainsaw's power unit input is not mio_icif:electric");
        ItemStack full = new ItemStack(unit);
        var battery = (com.miophas.singularity_iteration.core.api.item.IBatteryItem) unit;
        h.assertTrue(battery.getEnergy(full) > 0 && battery.getEnergy(full) >= battery.getMaxEnergy(full), "the default power unit is full");
        ItemStack shown = ingredient.getItems()[0];
        h.assertTrue(battery.getEnergy(shown) == 0, "the input shows a charged power unit");
        h.assertTrue(ingredient.test(full) && ingredient.test(shown), "the input must take any charge");
        h.succeed();
    }

    /**
     * D14: an AE2 pattern recorded with a plain machine takes one dismantled by a wrench (with its kept data), as a crafting table
     * does; another machine, or a vanilla item with data, is still another material.
     */
    @GameTest(batch = "round1138_recipes", template = "empty", timeoutTicks = 20)
    public static void dismantledMachineIsTheSameMaterialForPatterns(GameTestHelper h) {
        Class<?> diff = Round33GameTests.common("item.EnergyStackDiff");
        ItemStack plain = item("wiring/block_mfsu");
        ItemStack dismantled = plain.copy();
        CompoundTag data = new CompoundTag();
        data.putString("id", "mio_icif:mfsu"); data.putLong("Energy", 123_456L);
        dismantled.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(data));
        h.assertTrue((Boolean) call(diff, "sameMaterial", plain, dismantled), "a dismantled MFSU is not the MFSU of the pattern");
        h.assertTrue(!(Boolean) call(diff, "sameMaterial", plain, item("wiring/block_mfe")), "an MFE matched an MFSU");
        ItemStack chest = new ItemStack(net.minecraft.world.item.Items.CHEST), filled = chest.copy();
        filled.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(data));
        h.assertTrue(!(Boolean) call(diff, "sameMaterial", chest, filled), "a vanilla block with data matched");
        h.succeed();
    }

    // ------------------------------------------------------------------ A1: Energy Converter at EV

    /** The Energy Converter takes EV packets (2048 EU) without exploding: its tier was HV (512 EU) while it took 2048 EU/t. */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 20)
    public static void energyConverterIsEv(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("energy_converter/energy_converter")));
        var converter = h.getBlockEntity(REACTOR);
        long rating = (Long) call(call(converter, "getEffectiveCableTier"), "getPowerRating");
        h.assertTrue(rating == 2048, "the converter's voltage limit is " + rating);
        h.succeed();
    }
}
