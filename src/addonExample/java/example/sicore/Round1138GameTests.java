package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.item.BatteryTransfer;
import com.miophas.singularity_iteration.core.api.item.EnergySaving;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.runtime.crop.CropGrowthMath;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.38. Phase A: depleted rods stay in their slot (a reactor walled in by chambers lost the depleted quad rod
 * it threw out at its own position); a reactor chamber is a fluid port of its reactor. Phase B: idle machines sleep almost
 * every tick, the centrifuge and the ore washer sleep too, the lamp goes off 20 ticks after work stops, the redstone input is
 * tracked from neighbour updates. Internals are reached by reflection.
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

    // ------------------------------------------------------------------ B: processing machine performance

    private static AbstractProcessingMachineBlockEntity machine(GameTestHelper h, BlockPos at) {
        return (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
    }

    private static boolean lit(GameTestHelper h, BlockPos at) {
        var state = h.getBlockState(at);
        for (var property : state.getProperties())
            if (property.getName().equals("lit") && property instanceof BooleanProperty lit) return state.getValue(lit);
        throw new AssertionError("no lit property on " + state);
    }

    /** An electric furnace at {@code at} smelting cobblestone; its buffer is refilled on every tick. */
    @SuppressWarnings("deprecation")
    private static void smelting(GameTestHelper h, BlockPos at) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(id("producer/block_furnace_elc")));
        machine(h, at).getItemHandler().insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        h.onEachTick(() -> {
            if (h.getBlockEntity(at) instanceof AbstractProcessingMachineBlockEntity m) m.getEnergyStorageInternal().setStored(300);
        });
    }

    /**
     * An idle machine skips almost all its ticks: woken by its recheck timer with nothing changed, it sleeps again after one
     * full tick. It used to count 20 quiet full ticks again, so it slept only half the time.
     */
    @GameTest(batch = "round1138_perf", template = "empty", timeoutTicks = 200)
    public static void idleMachineSleepsAlmostEveryTick(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(id("producer/block_powder_elc")));
        int[] asleep = {0};
        h.onEachTick(() -> {
            long now = h.getTick();
            if (now > 40 && now <= 140 && machine(h, at).isIdleAsleep()) asleep[0]++;
            if (now == 141) {
                h.assertTrue(asleep[0] >= 90, "the idle macerator was asleep at " + asleep[0] + " of 100 ticks");
                h.succeed();
            }
        });
    }

    /** A machine that stops stays lit for LIT_OFF_DELAY (20) ticks: a machine short of input or power used to flicker. */
    @GameTest(batch = "round1138_perf", template = "empty", timeoutTicks = 120)
    public static void machineStaysLitForAMomentAfterItStops(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        smelting(h, at);
        h.runAfterDelay(20, () -> {
            h.assertTrue(machine(h, at).isWorking() && lit(h, at), "the furnace does not work");
            machine(h, at).getItemHandler().extractItem(0, 64, false);
            h.runAfterDelay(10, () -> {
                h.assertTrue(!machine(h, at).isWorking(), "the furnace still works without input");
                h.assertTrue(lit(h, at), "the lamp went off 10 ticks after the furnace stopped");
                h.runAfterDelay(15, () -> {
                    h.assertTrue(!lit(h, at), "the lamp is still on 25 ticks after the furnace stopped");
                    h.succeed();
                });
            });
        });
    }

    /** A redstone signal stops a machine at its next tick, and removing it starts the machine at its next tick (tracked input). */
    @GameTest(batch = "round1138_perf", template = "empty", timeoutTicks = 100)
    public static void redstoneStopsAndStartsAMachineAtOnce(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2), side = at.east();
        smelting(h, at);
        h.runAfterDelay(20, () -> {
            h.assertTrue(machine(h, at).isWorking(), "the furnace does not work");
            h.setBlock(side, Blocks.REDSTONE_BLOCK);
            h.runAfterDelay(1, () -> {
                h.assertTrue(!machine(h, at).isWorking(), "a redstone signal did not stop the furnace at its next tick");
                h.setBlock(side, Blocks.AIR);
                h.runAfterDelay(1, () -> {
                    h.assertTrue(machine(h, at).isWorking(), "the furnace did not start again at its next tick");
                    h.succeed();
                });
            });
        });
    }

    /**
     * The machine keeps its redstone input between neighbour updates, and reads it again within 20 ticks anyway: a signal
     * placed without a neighbour update (as some mods do) is noticed late, but noticed.
     */
    @GameTest(batch = "round1138_perf", template = "empty", timeoutTicks = 100)
    public static void silentRedstoneChangeIsReadAgainWithinASecond(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2), side = at.east();
        smelting(h, at);
        h.runAfterDelay(20, () -> {
            h.assertTrue(machine(h, at).isWorking(), "the furnace does not work");
            setField(machine(h, at), "redstoneReadAt", h.getLevel().getGameTime());   // the input was just read
            h.getLevel().setBlock(h.absolutePos(side), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            h.runAfterDelay(3, () -> {
                h.assertTrue(machine(h, at).isWorking(), "the furnace read its redstone input on every tick");
                h.runAfterDelay(20, () -> {
                    h.assertTrue(!machine(h, at).isWorking(), "the furnace did not read its redstone input again within 20 ticks");
                    h.succeed();
                });
            });
        });
    }

    /** A hot, idle centrifuge sleeps and keeps its heat; an inserted item starts it at once. It never slept before. */
    @SuppressWarnings("deprecation")
    @GameTest(batch = "round1138_perf", template = "empty", timeoutTicks = 120)
    public static void hotIdleCentrifugeSleepsAndWakesOnInput(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(id("producer/block_centrifuge_elc")));
        setField(machine(h, at), "heatStorage", 5000);
        machine(h, at).getEnergyStorageInternal().setStored(24_000);
        h.runAfterDelay(40, () -> {
            h.assertTrue(machine(h, at).isIdleAsleep(), "a hot idle centrifuge does not sleep");
            h.assertTrue((Integer) field(machine(h, at), "heatStorage") == 5000, "the sleeping centrifuge lost heat");
            machine(h, at).getItemHandler().insertItem(0, new ItemStack(Items.COBBLESTONE), false);
            h.runAfterDelay(2, () -> {
                h.assertTrue(machine(h, at).isWorking(), "an inserted item did not start the sleeping centrifuge");
                h.succeed();
            });
        });
    }

    /** An ore washer without water sleeps; water piped into its tank wakes it, and it starts at once. It never slept before. */
    @SuppressWarnings("deprecation")
    @GameTest(batch = "round1138_perf", template = "empty", timeoutTicks = 120)
    public static void dryWasherSleepsAndWakesWhenWaterArrives(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(id("producer/block_washer_elc")));
        machine(h, at).getEnergyStorageInternal().setStored(8_000);
        machine(h, at).getItemHandler().insertItem(0, new ItemStack(Items.GRAVEL, 4), false);
        h.runAfterDelay(40, () -> {
            h.assertTrue(machine(h, at).isIdleAsleep(), "a washer without water does not sleep");
            IFluidHandler tank = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(at), Direction.UP);
            h.assertTrue(tank != null && tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                "the washer took no water");
            h.assertTrue(!machine(h, at).isIdleAsleep(), "water did not wake the washer");
            h.runAfterDelay(2, () -> {
                h.assertTrue(machine(h, at).isWorking(), "the washer did not start with water");
                h.succeed();
            });
        });
    }

    /** RecipeSlots.fits (canWork, no copies) gives the same answer as prepare(...).isPresent() on random inventories. */
    @GameTest(batch = "round1138_perf", template = "empty", timeoutTicks = 20)
    public static void recipeFitCheckAgreesWithThePreparedPlan(GameTestHelper h) {
        var layout = SlotLayout.builder().input(1).output(3).build();
        Item[] items = {Items.COBBLESTONE, Items.STONE, Items.ENDER_PEARL, Items.DIAMOND_SWORD};
        var random = new java.util.Random(1138);
        int fit = 0;
        for (int round = 0; round < 4000; round++) {
            var inventory = new MachineItemHandler(layout);
            for (int slot = 0; slot < 4; slot++) {
                if (random.nextInt(3) == 0) continue;
                var stack = new ItemStack(items[random.nextInt(items.length)]);
                stack.setCount(1 + random.nextInt(stack.getMaxStackSize()));
                inventory.setStackInSlot(slot, stack);
            }
            int[] outputs = switch (random.nextInt(6)) {
                case 0 -> new int[]{1};
                case 1 -> new int[]{1, 2};
                case 2 -> new int[]{1, 2, 3};
                case 3 -> new int[]{1, 1};
                case 4 -> new int[]{0, 2};
                default -> new int[]{2, 9};
            };
            var results = new java.util.ArrayList<ItemStack>();
            for (int i = random.nextInt(outputs.length + 2); i > 0; i--) {
                if (random.nextInt(4) == 0) { results.add(ItemStack.EMPTY); continue; }
                var result = new ItemStack(items[random.nextInt(items.length)]);
                result.setCount(1 + random.nextInt(Math.min(64, result.getMaxStackSize() + 2)));   // sometimes more than a stack
                results.add(result);
            }
            int consumed = random.nextInt(4), input = random.nextInt(10) == 0 ? 7 : 0;
            boolean planned = RecipeSlots.prepare(inventory, input, consumed, outputs, results).isPresent();
            boolean fits = RecipeSlots.fits(inventory, input, consumed, outputs, results);
            if (planned != fits) throw new AssertionError("round " + round + ": prepare " + planned + ", fits " + fits);
            if (fits) fit++;
        }
        h.assertTrue(fit > 40, "too few cases fit: " + fit);   // 71 of the 4000 with this seed
        h.succeed();
    }

    // ------------------------------------------------------------------ D5: Energy Saving above V, D6: MOX-like proton rods

    /**
     * Levels above V keep saving: factor 0.75 x 0.95^(level - 5), exact on whole amounts, never zero; I-V are unchanged, and a
     * sword enchanted at X pays that factor.
     */
    @GameTest(batch = "round1138_balance", template = "empty", timeoutTicks = 20)
    public static void energySavingKeepsSavingAboveLevelFive(GameTestHelper h) {
        for (int level = 0; level <= 5; level++)
            h.assertTrue(EnergySaving.apply(level, 100) == 100 - 5L * level, "level " + level + " changed: " + EnergySaving.apply(level, 100));
        h.assertTrue(Math.abs(EnergySaving.factor(6) - 0.7125) < 1e-9, "factor VI " + EnergySaving.factor(6));
        h.assertTrue(Math.abs(EnergySaving.factor(10) - 0.75 * Math.pow(0.95, 5)) < 1e-9, "factor X " + EnergySaving.factor(10));
        h.assertTrue(EnergySaving.apply(6, 10_000) == 7_125 && EnergySaving.maxCost(6, 10_000) == 7_125,
            "level VI: 10000 EU -> " + EnergySaving.apply(6, 10_000));
        long seven = EnergySaving.apply(7, 10_000);
        h.assertTrue((seven == 6_768 || seven == 6_769) && EnergySaving.maxCost(7, 10_000) == 6_769, "level VII: 10000 EU -> " + seven);
        double previous = 1;
        for (int level = 1; level <= 300; level++) {
            double factor = EnergySaving.factor(level);
            h.assertTrue(factor > 0 && (factor < previous || level > EnergySaving.MAX_EFFECTIVE_LEVEL), "factor " + level + " = " + factor);
            previous = factor;
        }
        h.assertTrue(EnergySaving.apply(255, 1_000_000) > 0, "level 255 costs nothing");
        var holder = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EnergySaving.KEY);
        ItemStack sword = item("item_tool_quantum_sword");
        sword.enchant(holder, 10);
        h.assertTrue(EnergySaving.level(sword) == 10, "level X read as " + EnergySaving.level(sword));
        IBatteryItem battery = (IBatteryItem) sword.getItem();
        battery.setEnergy(sword, battery.getMaxEnergy(sword));
        long before = battery.getEnergy(sword);
        h.assertTrue(BatteryTransfer.consume(sword, battery, 100_000), "debit refused");
        long paid = before - battery.getEnergy(sword);
        h.assertTrue(Math.abs(paid - 100_000 * 0.75 * Math.pow(0.95, 5)) <= 1, "level X paid " + paid + " EU of 100000");
        h.succeed();
    }

    private static Object staticField(Class<?> owner, String name) {
        try { var f = owner.getDeclaredField(name); f.setAccessible(true); return f.get(null); }
        catch (ReflectiveOperationException e) { throw new AssertionError(name, e); }
    }

    /** The proton rods are MOX-like: their EU rises with the hull heat, from 1x uranium cold to x3 at half heat. */
    @GameTest(batch = "round1138_balance", template = "empty", timeoutTicks = 20)
    public static void protonRodEuRisesWithHullHeatLikeMox(GameTestHelper h) {
        @SuppressWarnings("unchecked")
        var profiles = (java.util.Map<String, ReactorCycle.Profile>) staticField(Round33GameTests.common("reactor.ReactorInventory"), "PROFILES");
        for (String size : new String[]{"simple", "dual", "quad"})
            h.assertTrue(profiles.get("proton_" + size).mox(), "the " + size + " proton rod is not MOX-like");
        var parts = new ReactorCycle.Part[54];
        parts[10] = new ReactorCycle.Part(profiles.get("proton_simple"), 0, 100);
        double cold = ReactorCycle.step(parts.clone(), 3, 0, true).euPerTick();
        double hot = ReactorCycle.step(parts.clone(), 3, 5000, true).euPerTick();
        double expected = 5 * profiles.get("proton_simple").euFactor();   // uranium gives 5 EU/t; the hull warms a little within the cycle
        h.assertTrue(cold >= expected && cold <= expected * 1.02, "a cold proton rod gives " + cold + " EU/t, not " + expected);
        h.assertTrue(hot >= cold * 2.8 && hot <= cold * 3.2, "a proton rod at half heat gives " + hot + " EU/t, not about 3x " + cold);
        h.succeed();
    }

    // ------------------------------------------------------------------ D7/D8: crops

    /** Relative yield per hour: growth points (x10000, so no rounding steps) over the base rolls, times the 1.03^gain drops. */
    private static double yieldRate(int tier, int growth, int gain, int resistance, int quality, boolean strict) {
        int required = CropGrowthMath.requiredQuality(tier, growth, gain, resistance, strict);
        double points = 0;
        for (int roll = 0; roll <= 6; roll++) points += CropGrowthMath.growthPoints(10_000 * (3 + roll + growth), required, quality, strict);
        return points * Math.pow(1.03, gain);
    }

    /** {growth, gain, resistance}: the higher stats must not give less, while both crops live. */
    private static void notLower(int tier, int quality, int[] low, int[] high, String stat) {
        int lowRequired = CropGrowthMath.requiredQuality(tier, low[0], low[1], low[2], false);
        int highRequired = CropGrowthMath.requiredQuality(tier, high[0], high[1], high[2], false);
        if (CropGrowthMath.penalty(lowRequired, quality, false) > CropGrowthMath.LETHAL_PENALTY
                || CropGrowthMath.penalty(highRequired, quality, false) > CropGrowthMath.LETHAL_PENALTY) return;
        double a = yieldRate(tier, low[0], low[1], low[2], quality, false), b = yieldRate(tier, high[0], high[1], high[2], quality, false);
        if (b < a * (1 - 1e-6)) throw new AssertionError("more " + stat + " gives less: " + java.util.Arrays.toString(low) + " -> "
            + java.util.Arrays.toString(high) + " at tier " + tier + ", quality " + quality + ": " + a + " -> " + b);
    }

    /**
     * SI crop rules: Resistance lowers the requirement, and more Growth, Gain or Resistance never lowers the expected yield per
     * hour in the same environment. Under the IC2 rules a 20/20/20 crop yields less than a 20/10/10 one at quality 60.
     */
    @GameTest(batch = "round1138_crops", template = "empty", timeoutTicks = 40)
    public static void betterCropStatsNeverLowerTheYieldPerHour(GameTestHelper h) {
        h.assertTrue(CropGrowthMath.requiredQuality(1, 31, 31, 31, false) == 31 && CropGrowthMath.requiredQuality(1, 31, 31, 31, true) == 93,
            "Resistance must lower the requirement (SI) or raise it (IC2)");
        for (int tier : new int[]{1, 2, 4, 7, 10, 13})
            for (int quality = 0; quality <= 400; quality += 7)
                for (int a = 0; a < 31; a += 3) for (int b = 0; b <= 31; b += 6) for (int c = 0; c <= 31; c += 6) {
                    notLower(tier, quality, new int[]{a, b, c}, new int[]{a + 1, b, c}, "growth");
                    notLower(tier, quality, new int[]{b, a, c}, new int[]{b, a + 1, c}, "gain");
                    notLower(tier, quality, new int[]{b, c, a}, new int[]{b, c, a + 1}, "resistance");
                }
        h.assertTrue(yieldRate(2, 20, 20, 20, 60, true) < yieldRate(2, 20, 10, 10, 60, true), "the IC2 rules changed");
        h.assertTrue(yieldRate(2, 20, 20, 20, 60, false) > yieldRate(2, 20, 10, 10, 60, false), "20/20/20 still yields less than 20/10/10");
        h.succeed();
    }

    private static IPlanter crop(GameTestHelper h, BlockPos pos, String species, int stage) {
        h.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(id("crop/stick")));
        IPlanter crop = (IPlanter) h.getBlockEntity(pos);
        if (species != null) { crop.setPlant(MioIcifAPI.instance().getCropAPI().getPlant("mio_icif", species)); crop.setGrowthStage(stage); }
        return crop;
    }

    /**
     * Growth 24+ spreads weeds by a chance (4 % per point from 24, minus the resistance) instead of making the crop a weed source,
     * and Weed-EX protects empty crop sticks; under the IC2 rules the fast crop is a weed and an empty stick is always taken.
     */
    @GameTest(batch = "round1138_crops", template = "empty", timeoutTicks = 40)
    public static void fastCropsSpreadWeedsByChanceAndWeedExGuardsEmptySticks(GameTestHelper h) {
        h.assertTrue(CropGrowthMath.weedChance(23, 0, false) == 0 && CropGrowthMath.weedChance(24, 0, false) == 4
            && CropGrowthMath.weedChance(31, 0, false) == 32 && CropGrowthMath.weedChance(31, 31, false) == 1
            && CropGrowthMath.weedChance(31, 0, true) == 0, "weed chances");
        boolean strict = CropGrowthMath.strictIc2Rules();
        try {
            IPlanter fast = crop(h, new BlockPos(1, 2, 1), "wheat", 3);
            fast.setGrowthSpeed(31);
            CropGrowthMath.setStrictIc2Rules(false);
            h.assertTrue(!fast.getPlant().isWeed(fast), "a Growth 31 crop is a weed under the SI rules");
            CropGrowthMath.setStrictIc2Rules(true);
            h.assertTrue(fast.getPlant().isWeed(fast), "a Growth 31 crop is no weed source under the IC2 rules");

            BlockPos center = new BlockPos(5, 2, 5);
            IPlanter weed = crop(h, center, "weed", 3);
            weed.setGrowthSpeed(31); weed.setResilience(31);   // spreads often, never dies
            IPlanter[] sticks = new IPlanter[4];
            int i = 0;
            for (Direction d : Direction.Plane.HORIZONTAL) { sticks[i] = crop(h, center.relative(d), null, 0); sticks[i++].setWeedControl(100); }
            CropGrowthMath.setStrictIc2Rules(false);
            h.getLevel().random.setSeed(1138);
            for (int c = 0; c < 30; c++) cycle(weed);
            int left = 0;
            for (IPlanter stick : sticks) {
                h.assertTrue(stick.getPlant() == null, "Weed-EX did not protect an empty crop stick");
                left += stick.getWeedControl();
            }
            h.assertTrue(left < 400, "the weed never reached the sticks");
            CropGrowthMath.setStrictIc2Rules(true);
            for (int c = 0; c < 30; c++) cycle(weed);
            boolean taken = false;
            for (IPlanter stick : sticks) taken |= stick.getPlant() != null;
            h.assertTrue(taken, "under the IC2 rules no empty stick was taken");
        } finally {
            CropGrowthMath.setStrictIc2Rules(strict);
        }
        h.succeed();
    }

    private static void cycle(IPlanter crop) { call(crop, "performCropCycle"); }

    // ------------------------------------------------------------------ D11: basic machines on the futures market

    /**
     * The basic machines are market goods (bought and sold, so also in bounties). Their made_from lists only market goods (one
     * other input turns the bid cap off), the bid stays below the material cost and the base price above it.
     */
    @GameTest(batch = "round1138_market", template = "empty", timeoutTicks = 20)
    public static void basicMachinesTradeOnTheFuturesMarket(GameTestHelper h) {
        Class<?> manager = Round33GameTests.common("future.FutureCommodityManager");
        String[] machines = {"producer/block_machine_hull_basic", "producer/block_powder_elc", "producer/block_extractor_elc",
            "producer/block_compressor_elc", "producer/block_furnace_elc", "producer/block_recycler_elc", "generator/block_thermal_generator",
            "generator/block_geo_generator", "generator/block_solar_generator", "wiring/block_bat_box"};
        for (String path : machines) {
            String id = "mio_icif:" + path;
            Object machine = call(manager, "getCommodityById", id);
            h.assertTrue(machine != null, id + " is not on the market");
            h.assertTrue(!(Boolean) call(machine, "sellOnly"), id + " cannot be bought");
            @SuppressWarnings("unchecked")
            var madeFrom = (java.util.Map<String, Double>) call(machine, "madeFrom");
            h.assertTrue(!madeFrom.isEmpty(), id + " has no made_from");
            double cost = 0;
            for (var input : madeFrom.entrySet()) {
                Object good = call(manager, "getCommodityById", input.getKey());
                h.assertTrue(good != null, id + " is made from " + input.getKey() + ", no market good (the bid cap would be off)");
                cost += input.getValue() * (Integer) call(good, "getBasePrice");
            }
            int bid = (Integer) call(machine, "getBidPrice"), base = (Integer) call(machine, "getBasePrice");
            h.assertTrue(bid < cost && base > cost, id + ": bid " + bid + ", base " + base + ", material cost " + cost);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ D10: Portable Futures Trader

    /** A supplier of an exchange result (ExchangeService.Result, by reflection). */
    private static java.util.function.Supplier<Object> result(boolean ok) {
        return () -> {
            try {
                return Round33GameTests.common("exchange.ExchangeService$Result").getDeclaredConstructor(boolean.class, String.class, Object[].class)
                    .newInstance(ok, "message.mio_icif.exchange.unknown_action", new Object[0]);
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        };
    }

    private static net.minecraft.world.inventory.AbstractContainerMenu open(ServerPlayer p) {
        try {
            return (net.minecraft.world.inventory.AbstractContainerMenu) Round33GameTests.common("menu.tool.PortableFuturesMenu")
                .getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class, InteractionHand.class)
                .newInstance(1, p.getInventory(), InteractionHand.MAIN_HAND);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static int coins(ServerPlayer p) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(item("normal/item_coin").getItem())) n += s.getCount();
        return n;
    }

    /**
     * The Portable Futures Trader serves the futures panel with itself as host after taking the data plan (200 coins for 30 market
     * days, once per period), pays each successful action's fee from its charge, keeps its own slot fixed and closes when it
     * leaves the hand; without coins for the plan it does not open.
     */
    @SuppressWarnings("deprecation")
    @GameTest(batch = "round1138_market", template = "empty", timeoutTicks = 20)
    public static void portableTraderServesThePanelForItsDataPlan(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        try {
            ItemStack trader = item("normal/item_portable_futures_trader");
            IBatteryItem battery = (IBatteryItem) trader.getItem();
            battery.setEnergy(trader, battery.getMaxEnergy(trader));
            p.getInventory().selected = 0;
            p.setItemInHand(InteractionHand.MAIN_HAND, trader);
            ItemStack pile = item("normal/item_coin");
            pile.setCount(64);
            for (int i = 0; i < 4; i++) p.getInventory().add(pile.copy());
            int before = coins(p);
            // the mock player cannot receive NeoForge's open-screen payload: pay the plan and open the menu as use() does
            Class<?> plan = Round33GameTests.common("exchange.DataPlan");
            h.assertTrue((Boolean) call(plan, "ensure", p), "the data plan was not paid");
            p.containerMenu = open(p);
            var menu = p.containerMenu;
            h.assertTrue(menu.getClass().getSimpleName().equals("PortableFuturesMenu") && call(menu, "host") == menu, "the trader did not open its own panel");
            h.assertTrue(before - coins(p) == 200, "the data plan took " + (before - coins(p)) + " coins, not 200");
            long today = (Long) call(Round33GameTests.common("exchange.ExchangeService"), "today", p.server);
            h.assertTrue((Long) call(plan, "paidUntil", p) == today + 30, "the plan does not cover 30 days");

            long charge = battery.getEnergy(p.getItemInHand(InteractionHand.MAIN_HAND));
            h.assertTrue((Boolean) call(menu, "serves", p) && (Boolean) call(menu, "hasEnoughEnergy"), "the trader does not serve its holder");
            h.assertTrue((Boolean) call(menu, "exchangeAction", p, result(true)), "an action failed");
            h.assertTrue(charge - battery.getEnergy(p.getItemInHand(InteractionHand.MAIN_HAND)) == 10, "an action did not cost 10 EU");
            charge = battery.getEnergy(p.getItemInHand(InteractionHand.MAIN_HAND));
            call(menu, "exchangeAction", p, result(false));
            h.assertTrue(charge == battery.getEnergy(p.getItemInHand(InteractionHand.MAIN_HAND)), "a failed action cost energy");

            menu.clicked(19 + 27, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, p);   // the hotbar's first slot (INVENTORY_FIRST + 27)
            h.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).is(trader.getItem()), "the trader's own slot moved");
            h.assertTrue((Boolean) call(plan, "ensure", p) && before - coins(p) == 200, "a second opening in the period charged again");
            ItemStack held = p.getItemInHand(InteractionHand.MAIN_HAND);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            h.assertTrue(!p.containerMenu.stillValid(p), "the panel stays open without the trader");
            p.closeContainer();

            ServerPlayer poor = Round33GameTests.survivalPlayer(h);
            try {
                poor.setItemInHand(InteractionHand.MAIN_HAND, held);
                held.use(h.getLevel(), poor, InteractionHand.MAIN_HAND);
                h.assertTrue(poor.containerMenu == poor.inventoryMenu, "the trader opened without a paid data plan");
            } finally {
                Round33GameTests.remove(poor);
            }
        } finally {
            Round33GameTests.remove(p);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ D9: Proton Shard and the octuple proton rod

    private static Object recipe(GameTestHelper h, String path) {
        return h.getLevel().getRecipeManager().byKey(id(path)).map(holder -> (Object) holder.value()).orElse(null);
    }

    /**
     * Industrial Upgrade's proton chain: the Molecular Transformer turns 1 plutonium into 1 Proton Shard for 15.5M EU, a shard and
     * an empty fuel rod make a proton rod (the 8.2M EU uranium-rod recipe of the Neutron Polymerizer is gone). The octuple proton
     * rod is a casing of 2 quad proton rods and 4 dense iron plates, finished in the Molecular Transformer for 2.108 billion EU (136
     * more shards' worth: about 144 plutonium in all). In the reactor it is two quads in one slot.
     */
    @SuppressWarnings("unchecked")
    @GameTest(batch = "round1138_balance", template = "empty", timeoutTicks = 20)
    public static void protonShardsMakeProtonRodsAndTheOctupleCostsAbout144Plutonium(GameTestHelper h) {
        Object shard = recipe(h, "molecular_transformer/plutonium_to_proton_shard");
        h.assertTrue(shard != null && (Long) call(shard, "getEuCost") == 15_500_000L, "the Proton Shard recipe is missing or not 15.5M EU");
        Object octuple = recipe(h, "molecular_transformer/proton_octuple_casing_to_rod");
        h.assertTrue(octuple != null && (Long) call(octuple, "getEuCost") == 2_108_000_000L, "the octuple rod recipe is missing or not 2.108B EU");
        h.assertTrue(recipe(h, "neutron_polymerizer/uranium_rod_to_proton_rod") == null, "the cheap uranium-rod recipe is still there");
        h.assertTrue(recipe(h, "shaped_proton_octuple_casing") != null, "the octuple casing has no recipe");

        var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(item("reactor/item_proton_shard"), item("normal/item_fuel_rod")));
        var made = h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, h.getLevel())
            .map(r -> r.value().assemble(input, h.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        h.assertTrue(made.is(item("reactor/item_reactor_proton_simple").getItem()), "a shard and an empty fuel rod do not make a proton rod: " + made);

        var profiles = (java.util.Map<String, ReactorCycle.Profile>) staticField(Round33GameTests.common("reactor.ReactorInventory"), "PROFILES");
        var quadParts = new ReactorCycle.Part[54];
        quadParts[10] = new ReactorCycle.Part(profiles.get("proton_quad"), 0, 100);
        var octParts = new ReactorCycle.Part[54];
        octParts[10] = new ReactorCycle.Part(profiles.get("proton_octuple"), 0, 100);
        var quad = ReactorCycle.step(quadParts, 3, 0, true);
        var oct = ReactorCycle.step(octParts, 3, 0, true);
        // both are MOX-like: the EU of each at its own hull heat, taken back to a cold hull (x (1 + 4 x heat / 10000)), is 2 : 1
        double octCold = oct.euPerTick() / (1 + 4.0 * oct.generatedHeat() / 10_000), quadCold = quad.euPerTick() / (1 + 4.0 * quad.generatedHeat() / 10_000);
        h.assertTrue(Math.abs(octCold / quadCold - 2) < 0.01 && Math.abs(oct.generatedHeat() - 2 * quad.generatedHeat()) <= 1,
            "the octuple rod is not two quads: " + oct.euPerTick() + " / " + quad.euPerTick() + " EU, " + oct.generatedHeat() + " / " + quad.generatedHeat() + " HU");
        h.assertTrue(profiles.get("proton_octuple").mox(), "the octuple rod is not MOX-like");
        var palette = (java.util.List<String>) staticField(Round33GameTests.common("reactor.ReactorInventory"), "PALETTE");
        h.assertTrue(palette.contains("proton_octuple") && palette.size() == 34, "the simulator palette lacks the octuple rod: " + palette.size());
        h.succeed();
    }

    // ------------------------------------------------------------------ verification of 0.1.10.38 (requested after the visual checks)

    /** The server option [Crops] strictIc2Rules reaches the crop formulas when the config is (re)loaded, and back. */
    @GameTest(batch = "round1138_crops", template = "empty", timeoutTicks = 20)
    public static void strictIc2RulesConfigSwitchesTheCropRules(GameTestHelper h) {
        Object option = staticField(Round33GameTests.common("Singularity_Iteration_Config"), "CROPS_STRICT_IC2");
        java.lang.reflect.Method sync;
        try {
            sync = Round33GameTests.common("Singularity_Iteration").getDeclaredMethod("syncVoltageOverloadConfig");
            sync.setAccessible(true);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        boolean before = (Boolean) call(option, "get");
        IPlanter fast = crop(h, new BlockPos(2, 2, 2), "wheat", 3);
        fast.setGrowthSpeed(31);
        try {
            call(option, "set", true);
            sync.invoke(null);
            h.assertTrue(CropGrowthMath.strictIc2Rules() && CropGrowthMath.requiredQuality(1, 31, 31, 31) == 93, "strictIc2Rules = true did not switch to the IC2 rules");
            h.assertTrue(fast.getPlant().isWeed(fast) && CropGrowthMath.weedChance(31, 0) == 0, "under the IC2 rules Growth 31 must make a weed source");
            call(option, "set", false);
            sync.invoke(null);
            h.assertTrue(!CropGrowthMath.strictIc2Rules() && CropGrowthMath.requiredQuality(1, 31, 31, 31) == 31, "strictIc2Rules = false did not switch back");
            h.assertTrue(!fast.getPlant().isWeed(fast) && CropGrowthMath.weedChance(31, 0) == 32, "under the SI rules Growth 31 must spread by chance");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        } finally {
            call(option, "set", before);
            try { sync.invoke(null); } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        }
        h.succeed();
    }

    /**
     * The octuple casing takes 2 full quad proton rods and 4 dense iron plates (a used quad rod does not fit), and the Molecular
     * Transformer's recipes take the casing and plutonium.
     */
    @GameTest(batch = "round1138_balance", template = "empty", timeoutTicks = 20)
    public static void octupleCasingTakesFullQuadRodsAndFeedsTheTransformer(GameTestHelper h) {
        ItemStack plate = item("resource/item_iron_denseplate"), full = rod(h, "reactor/item_reactor_proton_quad", 40000, 40000),
            used = rod(h, "reactor/item_reactor_proton_quad", 100, 40000);
        var good = net.minecraft.world.item.crafting.CraftingInput.of(3, 2, java.util.List.of(plate, full, plate, plate, full, plate));
        var made = h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, good, h.getLevel())
            .map(r -> r.value().assemble(good, h.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        h.assertTrue(made.is(item("reactor/item_proton_octuple_casing").getItem()), "2 quad rods and 4 dense plates do not make the casing: " + made);
        var worn = net.minecraft.world.item.crafting.CraftingInput.of(3, 2, java.util.List.of(plate, full, plate, plate, used, plate));
        h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, worn, h.getLevel()).isEmpty(),
            "a used quad rod made a casing");
        var casingInput = (net.minecraft.world.item.crafting.Ingredient) field(recipe(h, "molecular_transformer/proton_octuple_casing_to_rod"), "input");
        var shardInput = (net.minecraft.world.item.crafting.Ingredient) field(recipe(h, "molecular_transformer/plutonium_to_proton_shard"), "input");
        h.assertTrue(casingInput.test(item("reactor/item_proton_octuple_casing")) && shardInput.test(item("resource/item_plutonium")),
            "the Molecular Transformer recipes do not take the casing and plutonium");
        h.succeed();
    }

    /**
     * In a running reactor the octuple proton rod gives about twice the EU of a quad proton rod in the first cycle (2x, plus the
     * MOX bonus of its doubled heat: 2.11x), and a spent octuple rod becomes the depleted octuple rod and stays in its slot.
     */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 120)
    public static void octupleRodRunsAsTwoQuadsAndStaysWhenSpent(GameTestHelper h) {
        runOneRod(h, rod(h, "reactor/item_reactor_proton_quad", 40000, 40000));
        h.runAfterDelay(25, () -> {
            long quadEu = generation(h);
            runOneRod(h, rod(h, "reactor/item_reactor_proton_octuple", 40000, 40000));   // a new reactor: cold hull
            h.runAfterDelay(25, () -> {
                long octuple = generation(h);
                double ratio = (double) octuple / Math.max(1, quadEu);
                h.assertTrue(quadEu > 0 && ratio > 1.9 && ratio < 2.3, "octuple " + octuple + " EU/t against quad " + quadEu + " EU/t");
                runOneRod(h, rod(h, "reactor/item_reactor_proton_octuple", 1, 40000));
                var depleted = BuiltInRegistries.ITEM.get(id("reactor/item_reactor_proton_octuple_depleted"));
                h.runAfterDelay(45, () -> {
                    var slot = ((IReactorController) h.getBlockEntity(REACTOR)).getItemHandler().getStackInSlot(10);
                    h.assertTrue(slot.is(depleted), "the spent octuple rod is not the depleted octuple rod in its slot: " + slot);
                    h.succeed();
                });
            });
        });
    }

    /** Places a new (cold) reactor with one rod in its middle slot, then the redstone block that starts it. */
    private static void runOneRod(GameTestHelper h, ItemStack rod) {
        h.setBlock(REACTOR.east(), Blocks.AIR);
        h.setBlock(REACTOR, Blocks.AIR);
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        IReactorController reactor = (IReactorController) h.getBlockEntity(REACTOR);
        h.assertTrue(reactor.getItemHandler().insertItem(10, rod, false).isEmpty(), "rod rejected: " + rod);
        h.setBlock(REACTOR.east(), Blocks.REDSTONE_BLOCK);
    }

    private static long generation(GameTestHelper h) {
        return ((IReactorController) h.getBlockEntity(REACTOR)).getCurrentEnergyGeneration();
    }

    /**
     * The market day follows the world's day from the server tick (every 200 ticks), with no Futures Machine or Trade Terminal
     * ticking. Its own batch, so no other test of this run places one while it runs; the test checks that none is ticking.
     */
    @GameTest(batch = "round1138_marketday", template = "empty", timeoutTicks = 460)
    public static void marketDayAdvancesWithoutAFuturesMachine(GameTestHelper h) {
        var level = h.getLevel();
        java.util.List<?> tickers = (java.util.List<?>) field(level, "blockEntityTickers");
        long machines = tickers.stream().map(t -> ((net.minecraft.world.level.block.entity.TickingBlockEntity) t).getType())
            .filter(type -> type != null && (type.contains("future_elc") || type.contains("trade_terminal"))).count();
        h.assertTrue(machines == 0, machines + " Futures Machines / Trade Terminals are ticking; the test cannot tell which advanced the day");
        Class<?> service = Round33GameTests.common("exchange.ExchangeService");
        h.runAfterDelay(205, () -> {   // the server tick has initialized the market by now (if no test did)
            long before = (Long) call(service, "today", level.getServer());
            var overworld = level.getServer().overworld();
            overworld.setDayTime(overworld.getDayTime() + 24_000L);   // the same time of day, one day later
            h.runAfterDelay(210, () -> {
                long after = (Long) call(service, "today", level.getServer());
                h.assertTrue(after == before + 1, "the market day went from " + before + " to " + after + " in 210 ticks");
                h.succeed();
            });
        });
    }
}
