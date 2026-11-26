package com.miophas.singularity_iteration.common.item;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.ArmoryRegistry;
import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.builder.BuilderRegistry;
import com.miophas.singularity_iteration.common.guide.GuideBook;
import com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors;
import com.miophas.singularity_iteration.common.item.build.CFSprayerItem;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_dynamic_cell;
import com.miophas.singularity_iteration.common.item.crop.CropSeedItem;
import com.miophas.singularity_iteration.common.item.envtemplate.mio_icif_env_templates;
import com.miophas.singularity_iteration.common.item.lathe.mio_icif_lathe_items;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_bat;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.item.reactor.mio_icif_reactors;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_items_tools;
import com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrades;
import com.miophas.singularity_iteration.common.logistics.LogisticsRegistry;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.registry.mio_icif_items;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitRegistry;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.api.suit.SuitClass;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's creative tab, grouped by kind so that each kind sits in one run (0.1.11.160; regrouped 0.1.11.161):
 * <ol>
 *   <li>items: the manual; every tool (hand and electric tools, scanners, remotes and the TESU connector, the building tools:
 *       sprayer, obscurator, painters, the lathing tools, boxes); weapons and explosives; armour and trinkets; ores and resource
 *       blocks; each metal's chain (raw, crushed, dust, nugget, ingot, plate, dense plate, casing); non-metals; nuclear and
 *       advanced materials; components; batteries; reactor components; upgrades and templates; farming; food; fluid containers;
 *       odds and ends;</li>
 *   <li>blocks: cables and transformers; energy storage (BatBox to TESU); generators (EU, kinetic, heat, steam); the nuclear and
 *       fusion reactors; machines; item and fluid transport; every building block (hulls, scaffolds, storage boxes and tanks,
 *       glass, doors, construction foam, rubber wood);</li>
 *   <li>last, the suit modification station and the nano and quantum suit plugins (units, Mk.II after each Mk.I, the bay
 *       expander, the lattice densifier, frame cores).</li>
 * </ol>
 * Electric items come empty, then full. 0.1.11.160 put things by theme (the connector by the TESU, the storage boxes with the
 * pipes...), which split the tools and the building blocks over several places.
 */
@SuppressWarnings("null")
public class mio_icif_creativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Singularity_Iteration.MOD_ID);

    public static final Supplier<CreativeModeTab> ICIF_TAB = TABS.register("icif_tab", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(mio_icif_blocks.THERMAL_GENERATOR.get()))
            .title(Component.translatable("itemGroup.mio_icif"))
            .displayItems((parameters, output) -> {
                GuideBook.addToTab(output);
                // items
                tools(output);
                weapons(output);
                armor(output);
                worldResources(output);
                metals(output);
                nonMetals(output);
                nuclearMaterials(output);
                advancedMaterials(output);
                components(output);
                batteries(output);
                reactorComponents(output);
                upgrades(output);
                farming(output);
                food(output);
                fluids(output);
                miscItems(output);
                // blocks
                grid(output);
                energyStorage(output);
                generators(output);
                reactors(output);
                machines(output);
                logistics(output);
                building(output);
                suit(output);   // the suit station and the nano / quantum plugins close the tab
            }).build());

    // ------------------------------------------------------------------ stacks

    private static IItemAPI api() { return MioIcifAPI.instance().getItemAPI(); }

    /** A battery-based item holding {@code energy}. */
    private static ItemStack charged(ItemLike item, long energy) {
        ItemStack stack = new ItemStack(item);
        api().setBatteryEnergy(stack, energy);
        return stack;
    }

    /** A battery-based item at full charge. */
    private static ItemStack full(ItemLike item) {
        ItemStack stack = new ItemStack(item);
        api().setBatteryEnergy(stack, api().getBatteryCapacity(stack));
        return stack;
    }

    /** An electric tool (its own energy store) holding {@code energy}. */
    private static ItemStack tool(ItemLike item, long energy) {
        ItemStack stack = new ItemStack(item);
        api().setElectricToolEnergy(stack, energy);
        return stack;
    }

    /** An electric tool at full charge. */
    private static ItemStack fullTool(ItemLike item) {
        ItemStack stack = new ItemStack(item);
        api().setElectricToolEnergy(stack, api().getElectricToolMaxEnergy(stack));
        return stack;
    }

    /** A battery item ({@link mio_icif_bat}) holding {@code energy}. */
    private static ItemStack bat(ItemLike item, long energy) {
        ItemStack stack = new ItemStack(item);
        ((mio_icif_bat) stack.getItem()).setEnergy(stack, energy);
        return stack;
    }

    /**
     * The item empty, then full (battery-based). 0.1.11.161: the empty one is written out: many tools hold their full capacity
     * as the default charge, so the item as registered is the full one and the tab used to show no empty one.
     */
    private static void withFull(CreativeModeTab.Output out, ItemLike item) {
        out.accept(charged(item, 0));
        out.accept(full(item));
    }

    /** The item empty, then full (electric tool). */
    private static void withFullTool(CreativeModeTab.Output out, ItemLike item) {
        out.accept(tool(item, 0));
        out.accept(fullTool(item));
    }

    /** A battery item at 0, then at {@code max}. */
    private static void batPair(CreativeModeTab.Output out, ItemLike item, long max) {
        out.accept(bat(item, 0));
        out.accept(bat(item, max));
    }

    /** A battery-based item at 0, then full. */
    private static void chargedPair(CreativeModeTab.Output out, ItemLike item) {
        out.accept(charged(item, 0));
        out.accept(full(item));
    }

    /** An energy crystal empty, then full. */
    private static void crystal(CreativeModeTab.Output out, ItemLike item) {
        out.accept(bat(item, 0));
        ItemStack full = new ItemStack(item);
        if (full.getItem() instanceof mio_icif_bat bat) bat.setEnergy(full, bat.getMaxEnergy());
        out.accept(full);
    }

    /** A storage block as crafted, then holding {@code energy} (its block entity data). */
    private static void storageBlock(CreativeModeTab.Output out, ItemLike block, long energy, String blockEntityId) {
        out.accept(block);
        ItemStack full = new ItemStack(block);
        full.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(energy, blockEntityId)));
        out.accept(full);
    }

    /** A universal fluid cell holding 1000 mB of {@code fluid}. */
    private static void cell(CreativeModeTab.Output out, Fluid fluid) {
        ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
        ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(fluid, 1000));
        out.accept(cell);
    }

    private static void seed(CreativeModeTab.Output out, String crop) {
        out.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", crop, 0, 0, 0));
    }

    // ================================================================== items

    /** Every tool in one place: hand and electric tools, scanners, remotes, the TESU connector, building and farming tools. */
    private static void tools(CreativeModeTab.Output o) {
        // bronze and hand tools
        o.accept(mio_icif_items_tools.TOOL_BRONZE_PICKAXE.get());
        o.accept(mio_icif_items_tools.TOOL_BRONZE_AXE.get());
        o.accept(mio_icif_items_tools.TOOL_BRONZE_SHOVEL.get());
        o.accept(mio_icif_items_tools.TOOL_BRONZE_HOE.get());
        o.accept(mio_icif_items.TOOL_BRONZE_SWORD.get());
        o.accept(mio_icif_items_tools.TOOL_HAMMER.get());
        o.accept(mio_icif_items_tools.TOOL_CUTTER.get());
        o.accept(mio_icif_items_tools.WRENCH.get());
        o.accept(mio_icif_items_tools.TOOL_TREE_TAP.get());
        o.accept(mio_icif_normal.WEEDING_TROWEL.get());
        // electric tools
        withFull(o, mio_icif_items_tools.WRENCH_ELC.get());
        withFull(o, mio_icif_items_tools.TREETAP_ELC.get());
        withFull(o, mio_icif_items_tools.IRON_DRILLER.get());
        withFull(o, mio_icif_items_tools.DIAMOND_DRILLER.get());
        withFull(o, mio_icif_items_tools.IRIDIUM_DRILLER.get());
        withFull(o, mio_icif_items_tools.IRON_CHAINSAW.get());
        withFull(o, mio_icif_items_tools.IRON_HOE.get());
        withFull(o, mio_icif_items_tools.TOOL_LASER_MINER.get());
        withFull(o, mio_icif_items_tools.POWER_UNIT_SMALL.get());
        withFull(o, mio_icif_items_tools.POWER_UNIT.get());
        // scanners and meters
        withFull(o, mio_icif_items_tools.OD_SCANNER.get());
        withFull(o, mio_icif_items_tools.OV_SCANNER.get());
        o.accept(mio_icif_items_tools.AREA_SCANNER.get());
        withFull(o, mio_icif_normal.CROP_ANALYZER.get());
        o.accept(mio_icif_items_tools.EU_METER.get());   // 0.1.11.200 (T5c): the multimeter; the wind meter and geomagnetic detector convert into it
        // remotes and link tools
        withFull(o, mio_icif_items_tools.ELECTRIC_WIRELESS_MANAGER.get());
        o.accept(mio_icif_normal.TESU_CONNECTOR.get());
        o.accept(mio_icif_normal.FREQ.get());
        o.accept(ArmoryRegistry.REMOTE.get());
        withFull(o, mio_icif_normal.PORTABLE_FUTURES_TRADER.get());   // 0.1.10.38
        // portable electric devices
        withFull(o, mio_icif_items_tools.ELECTRIC_NUTRITION_SUPPLY.get());
        withFull(o, mio_icif_items_tools.ELECTRIC_FIRST_AID_LIFE_SUPPORT.get());
        withFull(o, mio_icif_items_tools.ELECTRIC_FORCE_FIELD_GENERATOR.get());
        withFull(o, mio_icif_items_tools.ELECTRIC_FISHING_ROD.get());
        withFull(o, mio_icif_items_tools.ELECTRIC_LIGHTER.get());
        // building tools: the construction foam sprayer (empty, then with a full tank), the obscurator, the painters
        o.accept(mio_icif_normal.CF_SPRAYER.get());
        ItemStack fullSprayer = new ItemStack(mio_icif_normal.CF_SPRAYER.get());
        mio_icif_normal.CF_SPRAYER.get().fillFluid(fullSprayer, new FluidStack(mio_icif_fluids.CONSTRUCTIONFOAM.get(), CFSprayerItem.CAPACITY));
        o.accept(fullSprayer);
        withFull(o, mio_icif_normal.OBSCURATOR.get());   // 0.1.11.161: it is electric too
        o.accept(mio_icif_normal.PAINTER.get());
        o.accept(mio_icif_normal.PAINTER_WHITE.get());
        o.accept(mio_icif_normal.PAINTER_ORANGE.get());
        o.accept(mio_icif_normal.PAINTER_MAGENTA.get());
        o.accept(mio_icif_normal.PAINTER_LIGHT_BLUE.get());
        o.accept(mio_icif_normal.PAINTER_YELLOW.get());
        o.accept(mio_icif_normal.PAINTER_LIME.get());
        o.accept(mio_icif_normal.PAINTER_PINK.get());
        o.accept(mio_icif_normal.PAINTER_GRAY.get());
        o.accept(mio_icif_normal.PAINTER_LIGHT_GRAY.get());
        o.accept(mio_icif_normal.PAINTER_CYAN.get());
        o.accept(mio_icif_normal.PAINTER_PURPLE.get());
        o.accept(mio_icif_normal.PAINTER_BLUE.get());
        o.accept(mio_icif_normal.PAINTER_BROWN.get());
        o.accept(mio_icif_normal.PAINTER_GREEN.get());
        o.accept(mio_icif_normal.PAINTER_RED.get());
        o.accept(mio_icif_normal.PAINTER_BLACK.get());
        // lathing tools
        o.accept(mio_icif_lathe_items.IRON_LATHING_TOOL.get());
        o.accept(mio_icif_lathe_items.DIAMOND_LATHING_TOOL.get());
        // boxes
        o.accept(mio_icif_normal.TOOLBOX.get());
        o.accept(mio_icif_normal.TOOLBOX_X.get());
        o.accept(mio_icif_normal.CONTAIN_MENT_BOX.get());
    }

    private static void weapons(CreativeModeTab.Output o) {
        withFull(o, mio_icif_items_tools.NANO_SABER.get());
        withFullTool(o, mio_icif_items_tools.QUANTUM_SWORD.get());
        withFull(o, mio_icif_items_tools.PLASMA_LAUNCHER.get());
        withFullTool(o, mio_icif_items_tools.ELECTRIC_RIFLE.get());
        withFullTool(o, mio_icif_items_tools.ADVANCED_ELECTRIC_RIFLE.get());
        withFullTool(o, mio_icif_items_tools.TACTICAL_LASER_RIFLE.get());
        withFullTool(o, mio_icif_items_tools.PLASMA_AIR_CANNON.get());
        withFullTool(o, mio_icif_items_tools.ELECTRIC_PLASMA_GUN.get());
        withFullTool(o, mio_icif_items_tools.TACHYON_DISRUPTOR.get());
        withFull(o, mio_icif_items_tools.NANO_BOW.get());
        withFullTool(o, mio_icif_items_tools.ROCKET_LAUNCHER.get());
        o.accept(mio_icif_items_tools.ROCKET.get());
        withFull(o, mio_icif_items_tools.STEEL_SHIELD.get());
        // explosives
        o.accept(mio_icif_normal.DYNAMITE.get());
        o.accept(mio_icif_normal.STICKY_DYNAMITE.get());
        o.accept(mio_icif_normal.REMOTE.get());
        o.accept(mio_icif_blocks.IC_TNT.get());
        o.accept(mio_icif_blocks.NUKE.get());
    }

    private static void armor(CreativeModeTab.Output o) {
        o.accept(mio_icif_items_armors.ARMOR_BRONZE_HELMET.get());
        o.accept(mio_icif_items_armors.ARMOR_BRONZE_CHESTPLATE.get());
        o.accept(mio_icif_items_armors.ARMOR_BRONZE_LEGGINGS.get());
        o.accept(mio_icif_items_armors.ARMOR_BRONZE_BOOTS.get());
        o.accept(mio_icif_items_armors.HAZMAT_HELMET.get());
        o.accept(mio_icif_items_armors.HAZMAT_CHESTPLATE.get());
        o.accept(mio_icif_items_armors.HAZMAT_LEGGINGS.get());
        o.accept(mio_icif_items_armors.HAZMAT_BOOTS.get());
        o.accept(mio_icif_normal.STATIC_BOOTS.get());
        o.accept(mio_icif_items_armors.SOUNDPROOF_EARMUFFS.get());   // 0.1.10.14
        withFull(o, mio_icif_items_armors.ARMOR_DIVING_MASK.get());
        withFull(o, mio_icif_items_armors.ARMOR_NIGHTVISION_GOGGLES.get());
        // battery packs and jetpacks
        withFull(o, mio_icif_items_armors.ARMOR_BATPACK.get());
        withFull(o, mio_icif_items_armors.ARMOR_ADV_BATPACK.get());
        withFull(o, mio_icif_items_armors.ARMOR_ENERGYPACK.get());
        withFull(o, mio_icif_items_armors.ARMOR_LAPPACK.get());
        withFull(o, mio_icif_items_armors.ARMOR_JETPACK_ELECTRIC.get());
        withFull(o, mio_icif_items_armors.ARMOR_ADVANCED_JETPACK.get());
        // nano and quantum suits (0.1.9.0: heavy / advanced quantum chestplates and solar helmets retired: frame cores)
        withFull(o, mio_icif_items_armors.ARMOR_NANO_HELMET.get());
        withFull(o, mio_icif_items_armors.ARMOR_NANO_CHESTPLATE.get());
        withFull(o, mio_icif_items_armors.ARMOR_NANO_LEGGINGS.get());
        withFull(o, mio_icif_items_armors.ARMOR_NANO_BOOTS.get());
        withFull(o, mio_icif_items_armors.ARMOR_QUANTUM_HELMET.get());
        withFull(o, mio_icif_items_armors.ARMOR_QUANTUM_CHESTPLATE.get());
        withFull(o, mio_icif_items_armors.ARMOR_QUANTUM_LEGGINGS.get());
        withFull(o, mio_icif_items_armors.ARMOR_QUANTUM_BOOTS.get());
        trinkets(o);
    }

    /** The Curios trinkets, empty then full (only with Curios). */
    private static void trinkets(CreativeModeTab.Output o) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) return;
        try {
            Class<?> curios = Class.forName("com.miophas.singularity_iteration.common.integration.CuriosIntegration");
            for (String field : new String[]{"TRINKET_FIREPROOF_NECKLACE", "TRINKET_ENERGY_CRYSTAL_BELT", "TRINKET_LAPORTON_CRYSTAL_BELT",
                                             "TRINKET_LIFE_SUPPORT_RING", "TRINKET_FLIGHT_RING"}) {
                ItemLike item = ((DeferredItem<?>) curios.getField(field).get(null)).get();
                o.accept(tool(item, 0));
                o.accept(fullTool(item));
            }
        } catch (Exception e) {
            com.miophas.singularity_iteration.core.runtime.diagnostics.QuietFailures.report("creative-tab-curios", "Creative tab entries of the Curios trinkets", e);
        }
    }

    /** Ores, raw ore blocks, metal blocks, irradiated ground and the rubber tree: what the world gives. */
    private static void worldResources(CreativeModeTab.Output o) {
        // ores, each with its deepslate form
        o.accept(mio_icif_blocks.BLOCK_ORE_TIN.get());
        o.accept(mio_icif_blocks.BLOCK_ORE_TIN_IN_DEEP.get());
        o.accept(mio_icif_blocks.BLOCK_ORE_LEAD.get());
        o.accept(mio_icif_blocks.BLOCK_ORE_LEAD_IN_DEEP.get());
        o.accept(mio_icif_blocks.BLOCK_ORE_SILVER.get());          // 0.1.11.98
        o.accept(mio_icif_blocks.BLOCK_ORE_SILVER_IN_DEEP.get());
        o.accept(mio_icif_blocks.BLOCK_ORE_URAN.get());
        o.accept(mio_icif_blocks.BLOCK_ORE_URAN_IN_DEEP.get());
        o.accept(mio_icif_blocks.BLOCK_ORE_NIOBIUM.get());
        o.accept(mio_icif_blocks.BLOCK_ORE_NIOBIUM_IN_DEEP.get());
        o.accept(mio_icif_blocks.BLOCK_ORE_TITANIUM.get());
        o.accept(mio_icif_blocks.BLOCK_ORE_TITANIUM_IN_DEEP.get());
        o.accept(mio_icif_blocks.BLOCK_ORE_IRIDIUM_IN_DEEP.get());   // 0.1.10.36: made by nuclear blasts
        // raw ore blocks
        o.accept(mio_icif_blocks.BLOCK_RAW_TIN.get());
        o.accept(mio_icif_blocks.BLOCK_RAW_LEAD.get());
        o.accept(mio_icif_blocks.BLOCK_RAW_SILVER.get());
        o.accept(mio_icif_blocks.BLOCK_RAW_URAN.get());
        o.accept(mio_icif_blocks.BLOCK_RAW_NIOBIUM.get());
        o.accept(mio_icif_blocks.BLOCK_RAW_TITANIUM.get());
        // metal blocks
        o.accept(mio_icif_blocks.BLOCK_TIN.get());
        o.accept(mio_icif_blocks.BLOCK_BRONZE.get());
        o.accept(mio_icif_blocks.BLOCK_LEAD.get());
        o.accept(mio_icif_blocks.BLOCK_SILVER.get());
        o.accept(mio_icif_blocks.BLOCK_ADVIRON.get());
        o.accept(mio_icif_blocks.BLOCK_URAN.get());
        o.accept(mio_icif_blocks.BLOCK_NIOBIUM.get());
        o.accept(mio_icif_blocks.BLOCK_TITANIUM.get());
        // irradiated ground
        o.accept(mio_icif_blocks.BLOCK_RADIATING_STONE.get());
        o.accept(mio_icif_blocks.BLOCK_RADIATING_DIRT.get());
        o.accept(mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get());
        // the rubber tree (its planks and other wood blocks are with the building blocks)
        o.accept(mio_icif_blocks.BLOCK_RUBBER_SAPLING.get());
        o.accept(mio_icif_blocks.BLOCK_RUBBER_TREE.get());
        o.accept(mio_icif_blocks.BLOCK_STRIPPED_RUBBER_WOOD.get());
        o.accept(mio_icif_blocks.BLOCK_RUBBER_LEAF.get());
    }

    /** Each metal: raw, crushed, purified crushed, dust, small dust, nugget, ingot, plate, dense plate, casing. */
    private static void metals(CreativeModeTab.Output o) {
        // copper
        o.accept(mio_icif_resources.COPPER_ORE_CRUSHED.get());
        o.accept(mio_icif_resources.COPPER_ORE_CRUSHED_PURIFIED.get());
        o.accept(mio_icif_resources.COPPER_DUST.get());
        o.accept(mio_icif_resources.COPPER_DUST_SMALL.get());
        o.accept(mio_icif_resources.COPPER_NUGGET.get());
        o.accept(mio_icif_resources.COPPER_PLATE.get());
        o.accept(mio_icif_resources.COPPER_DENSEPLATE.get());
        o.accept(mio_icif_resources.COPPER_CASING.get());
        // tin
        o.accept(mio_icif_resources.RAW_TIN.get());
        o.accept(mio_icif_resources.TIN_ORE_CRUSHED.get());
        o.accept(mio_icif_resources.TIN_ORE_CRUSHED_PURIFIED.get());
        o.accept(mio_icif_resources.TIN_DUST.get());
        o.accept(mio_icif_resources.TIN_DUST_SMALL.get());
        o.accept(mio_icif_resources.TIN_NUGGET.get());
        o.accept(mio_icif_resources.INGOT_TIN.get());
        o.accept(mio_icif_resources.TIN_PLATE.get());
        o.accept(mio_icif_resources.TIN_DENSEPLATE.get());
        o.accept(mio_icif_resources.TIN_CASING.get());
        // bronze
        o.accept(mio_icif_resources.BRONZE_DUST.get());
        o.accept(mio_icif_resources.BRONZE_DUST_SMALL.get());
        o.accept(mio_icif_resources.INGOT_BRONZE.get());
        o.accept(mio_icif_resources.BRONZE_PLATE.get());
        o.accept(mio_icif_resources.BRONZE_DENSEPLATE.get());
        o.accept(mio_icif_resources.BRONZE_CASING.get());
        // iron
        o.accept(mio_icif_resources.IRON_ORE_CRUSHED.get());
        o.accept(mio_icif_resources.IRON_ORE_CRUSHED_PURIFIED.get());
        o.accept(mio_icif_resources.IRON_DUST.get());
        o.accept(mio_icif_resources.IRON_DUST_SMALL.get());
        o.accept(mio_icif_resources.IRON_PLATE.get());
        o.accept(mio_icif_resources.IRON_DENSEPLATE.get());
        o.accept(mio_icif_resources.IRON_CASING.get());
        // refined iron
        o.accept(mio_icif_resources.ADVIRON_INGOT.get());
        o.accept(mio_icif_resources.ADVIRON_PLATE.get());
        o.accept(mio_icif_resources.ADVIRON_DENSEPLATE.get());
        o.accept(mio_icif_resources.ADVIRON_CASING.get());
        // gold
        o.accept(mio_icif_resources.GOLD_ORE_CRUSHED.get());
        o.accept(mio_icif_resources.GOLD_ORE_CRUSHED_PURIFIED.get());
        o.accept(mio_icif_resources.GOLDEN_DUST.get());
        o.accept(mio_icif_resources.GOLDEN_DUST_SMALL.get());
        o.accept(mio_icif_resources.GOLDEN_PLATE.get());
        o.accept(mio_icif_resources.GOLDEN_DENSEPLATE.get());
        o.accept(mio_icif_resources.GOLDEN_CASING.get());
        // lead
        o.accept(mio_icif_resources.RAW_LEAD.get());
        o.accept(mio_icif_resources.LEAD_ORE_CRUSHED.get());
        o.accept(mio_icif_resources.LEAD_ORE_CRUSHED_PURIFIED.get());
        o.accept(mio_icif_resources.LEAD_DUST.get());
        o.accept(mio_icif_resources.LEAD_DUST_SMALL.get());
        o.accept(mio_icif_resources.LEAD_NUGGET.get());
        o.accept(mio_icif_resources.LEAD_INGOT.get());
        o.accept(mio_icif_resources.LEAD_PLATE.get());
        o.accept(mio_icif_resources.LEAD_DENSEPLATE.get());
        o.accept(mio_icif_resources.LEAD_CASING.get());
        // silver (0.1.11.98; plate, dense plate and casing 0.1.11.153)
        o.accept(mio_icif_resources.RAW_SILVER.get());
        o.accept(mio_icif_resources.SILVER_ORE_CRUSHED.get());
        o.accept(mio_icif_resources.SILVER_ORE_CRUSHED_PURIFIED.get());
        o.accept(mio_icif_resources.SILVER_DUST.get());
        o.accept(mio_icif_resources.SILVER_DUST_SMALL.get());
        o.accept(mio_icif_resources.SILVER_NUGGET.get());
        o.accept(mio_icif_resources.SILVER_INGOT.get());
        o.accept(mio_icif_resources.SILVER_PLATE.get());
        o.accept(mio_icif_resources.SILVER_DENSEPLATE.get());
        o.accept(mio_icif_resources.SILVER_CASING.get());
        // niobium
        o.accept(mio_icif_resources.RAW_NIOBIUM.get());
        o.accept(mio_icif_resources.NIOBIUM_ORE_CRUSHED.get());
        o.accept(mio_icif_resources.NIOBIUM_ORE_CRUSHED_PURIFIED.get());
        o.accept(mio_icif_resources.NIOBIUM_DUST.get());
        o.accept(mio_icif_resources.NIOBIUM_DUST_SMALL.get());
        o.accept(mio_icif_resources.NIOBIUM_INGOT.get());
        o.accept(mio_icif_resources.NIOBIUM_PLATE.get());
        o.accept(mio_icif_resources.NIOBIUM_DENSEPLATE.get());
        o.accept(mio_icif_resources.NIOBIUM_CASING.get());
        // titanium
        o.accept(mio_icif_resources.RAW_TITANIUM.get());
        o.accept(mio_icif_resources.TITANIUM_ORE_CRUSHED.get());
        o.accept(mio_icif_resources.TITANIUM_ORE_CRUSHED_PURIFIED.get());
        o.accept(mio_icif_resources.TITANIUM_DUST.get());
        o.accept(mio_icif_resources.TITANIUM_DUST_SMALL.get());
        o.accept(mio_icif_resources.TITANIUM_NUGGET.get());
        o.accept(mio_icif_resources.TITANIUM_INGOT.get());
        o.accept(mio_icif_resources.TITANIUM_PLATE.get());
        o.accept(mio_icif_resources.TITANIUM_DENSEPLATE.get());
        o.accept(mio_icif_resources.TITANIUM_CASING.get());
        // niobium-titanium
        o.accept(mio_icif_resources.NIOBIUM_TITANIUM_DUST.get());
        o.accept(mio_icif_resources.NIOBIUM_TITANIUM_INGOT.get());
        o.accept(mio_icif_resources.NIOBIUM_TITANIUM_PLATE.get());
        // iridium
        o.accept(mio_icif_resources.IRIDIUM.get());
        o.accept(mio_icif_resources.SHARD_IRIDIUM.get());
        o.accept(mio_icif_resources.IRIDIUM_DUST_SMALL.get());
        o.accept(mio_icif_resources.IRIDIUM_INGOT.get());
        o.accept(mio_icif_resources.IRIDIUM_PLATE.get());
        // advanced alloy
        o.accept(mio_icif_resources.ALLOY_INGOT.get());
        o.accept(mio_icif_resources.ALLOY_PLATE.get());
        o.accept(mio_icif_resources.DCP_PLATE.get());
    }

    private static void nonMetals(CreativeModeTab.Output o) {
        // rubber
        o.accept(mio_icif_resources.HARZ.get());
        o.accept(mio_icif_resources.RUBBER.get());
        // lapis and obsidian
        o.accept(mio_icif_resources.LAPI_DUST.get());
        o.accept(mio_icif_resources.LAPI_DUST_SMALL.get());
        o.accept(mio_icif_resources.LAPI_PLATE.get());
        o.accept(mio_icif_resources.LAPI_DENSEPLATE.get());
        o.accept(mio_icif_resources.OBSIDIAN_DUST.get());
        o.accept(mio_icif_resources.OBSIDIAN_DUST_SMALL.get());
        o.accept(mio_icif_resources.OBSIDIAN_PLATE.get());
        o.accept(mio_icif_resources.OBSIDIAN_DENSEPLATE.get());
        // gems
        o.accept(mio_icif_resources.DIAMOND_DUST.get());
        o.accept(mio_icif_resources.DIAMOND_DUST_SMALL.get());
        o.accept(mio_icif_normal.INDUSTRIAL_DIAMOND.get());
        o.accept(mio_icif_resources.EMERALD_DUST.get());
        o.accept(mio_icif_resources.EMERALD_DUST_SMALL.get());
        o.accept(mio_icif_resources.ENDER_PEARL_DUST.get());
        o.accept(mio_icif_resources.ENERGIUM_DUST.get());
        // coal and carbon
        o.accept(mio_icif_resources.COAL_DUST.get());
        o.accept(mio_icif_resources.CHARCOAL_DUST.get());
        o.accept(mio_icif_resources.COAL_BALL.get());
        o.accept(mio_icif_resources.ZIP_COAL_BLOCK.get());
        o.accept(mio_icif_resources.COAL_CHUNK.get());
        o.accept(mio_icif_resources.COAL_FABRE.get());
        o.accept(mio_icif_resources.COAL_MESH.get());
        o.accept(mio_icif_resources.CARBON_PLATE.get());
        // other dusts
        o.accept(mio_icif_resources.STONE_DUST.get());
        o.accept(mio_icif_resources.CLAY_DUST.get());
        o.accept(mio_icif_resources.NETHERRACK_DUST.get());
        o.accept(mio_icif_resources.FLINT_DUST.get());
        o.accept(mio_icif_resources.SILICONDIOXIDE_DUST.get());
        o.accept(mio_icif_resources.SULFUR_DUST.get());
        o.accept(mio_icif_resources.SULFUR_DUST_SMALL.get());
        o.accept(mio_icif_resources.NITRE_DUST.get());
        o.accept(mio_icif_resources.LITHIUM_DUST.get());
        o.accept(mio_icif_resources.LITHIUM_DUST_SMALL.get());
        o.accept(mio_icif_resources.GRIN_DUST.get());
        o.accept(mio_icif_resources.ASH.get());
        // construction foam
        o.accept(mio_icif_resources.CF_DUST.get());
        o.accept(mio_icif_resources.PELLET.get());
        // scrap
        o.accept(mio_icif_normal.SCRAP.get());
        o.accept(mio_icif_normal.SCRAPBOX.get());
        o.accept(mio_icif_normal.SLAG.get());
    }

    private static void nuclearMaterials(CreativeModeTab.Output o) {
        o.accept(mio_icif_resources.RAW_URAN.get());
        o.accept(mio_icif_resources.URAN_ORE_CRUSHED.get());
        o.accept(mio_icif_resources.URAN_ORE_CRUSHED_PURIFIED.get());
        o.accept(mio_icif_resources.URAN.get());
        o.accept(mio_icif_resources.URAN_238.get());
        o.accept(mio_icif_resources.URAN_238_SMALL.get());
        o.accept(mio_icif_resources.URAN_235.get());
        o.accept(mio_icif_resources.URAN_235_SMALL.get());
        o.accept(mio_icif_resources.URAN_PELLET.get());
        o.accept(mio_icif_normal.RE_ENRICHED_URANIUM.get());
        o.accept(mio_icif_resources.PLUTONIUM.get());
        o.accept(mio_icif_resources.PLUTONIUM_SMALL.get());
        o.accept(mio_icif_resources.MOX.get());
        o.accept(mio_icif_resources.RTG_PELLET.get());
        o.accept(mio_icif_resources.THORIUM_DUST.get());
        o.accept(mio_icif_resources.THORIUM_DUST_SMALL.get());
        o.accept(mio_icif_resources.THORIUM_SCRAP.get());
    }

    private static void advancedMaterials(CreativeModeTab.Output o) {
        // the advanced solar panels' materials
        o.accept(mio_icif_resources.SUNNARIUM_SMALL.get());
        o.accept(mio_icif_resources.SUNNARIUM.get());
        o.accept(mio_icif_resources.SUNNARIUM_ALLOY.get());
        o.accept(mio_icif_resources.ENRICHED_SUNNARIUM.get());
        o.accept(mio_icif_resources.ENRICHED_SUNNARIUM_ALLOY.get());
        o.accept(mio_icif_resources.URANIUM_INGOT.get());
        o.accept(mio_icif_resources.IRRADIANT_URANIUM_INGOT.get());
        o.accept(mio_icif_resources.IRIDIUMIRON_PLATE.get());
        o.accept(mio_icif_resources.IRIDIUMIRON_REINFORCED_PLATE.get());
        o.accept(mio_icif_resources.IRRADIANT_REINFORCED_PLATE.get());
        // late-game alloys
        o.accept(mio_icif_resources.METS_SUPER_IRIDIUM_ALLOY.get());
        o.accept(mio_icif_resources.METS_SUPER_IRIDIUM_COMPRESS_PLATE.get());
        o.accept(mio_icif_resources.METS_NEUTRON_PLATE.get());
        o.accept(mio_icif_resources.METS_NANO_LIVING_METAL.get());
        o.accept(mio_icif_resources.METS_PLANT_EXTRACT.get());
    }

    private static void components(CreativeModeTab.Output o) {
        // circuits
        o.accept(mio_icif_normal.CIRCUIT.get());
        o.accept(mio_icif_normal.ADCIRCUIT.get());
        o.accept(mio_icif_normal.METS_SUPER_CIRCUIT.get());
        o.accept(mio_icif_normal.METS_LIVING_CIRCUIT.get());
        // parts
        o.accept(mio_icif_resources.MOTOR.get());
        o.accept(mio_icif_resources.COIL.get());
        o.accept(mio_icif_resources.HEATCONDUCTOR.get());
        o.accept(mio_icif_resources.DRILL_BIT.get());
        o.accept(mio_icif_resources.CHAINSAW_BLADE.get());
        o.accept(mio_icif_resources.IRON_CUT_BLADE.get());
        o.accept(mio_icif_resources.ADVIRON_CUTBLADE.get());
        o.accept(mio_icif_resources.DIAMOND_CUT_BLADE.get());
        o.accept(mio_icif_normal.METS_LENS.get());
        o.accept(mio_icif_normal.METS_DIAMOND_LENS.get());
        o.accept(mio_icif_normal.METS_FIELD_GENERATOR.get());
        o.accept(mio_icif_resources.CRYSTAL_MEMORY_RAW.get());
        o.accept(mio_icif_resources.CRYSTAL_MEMORY.get());
        o.accept(mio_icif_resources.QUANTUM_CORE.get());
        o.accept(mio_icif_resources.MOLECULAR_TRANSFORMER_CORE.get());
        // shafts, rotor blades and the rotors of the wind and water kinetic generators
        o.accept(mio_icif_resources.BRONZE_SHAFT.get());
        o.accept(mio_icif_resources.IRON_SHAFT.get());
        o.accept(mio_icif_resources.ADVIRON_SHAFT.get());
        o.accept(mio_icif_resources.METS_TITANIUM_SHAFT.get());
        o.accept(mio_icif_resources.WOOD_ROTOR_BLADE.get());
        o.accept(mio_icif_resources.BRONZE_ROTOR_BLADE.get());
        o.accept(mio_icif_resources.IRON_ROTOR_BLADE.get());
        o.accept(mio_icif_resources.CARBON_ROTORBLADE.get());
        o.accept(mio_icif_resources.ADVIRON_ROTOR_BLADE.get());
        o.accept(mio_icif_resources.TITANIUM_IRON_ROTOR_BLADE.get());
        o.accept(mio_icif_resources.SUPER_IRIDIUM_ROTOR_BLADE.get());
        o.accept(mio_icif_items.ROTOR_WOOD.get());
        o.accept(mio_icif_items.ROTOR_BRONZE.get());
        o.accept(mio_icif_items.ROTOR_IRON.get());
        o.accept(mio_icif_items.ROTOR_CARBON.get());
        o.accept(mio_icif_items.ROTOR_ADVIRON.get());
        o.accept(mio_icif_items.ROTOR_TITANIUM_IRON.get());
        o.accept(mio_icif_items.ROTOR_SUPER_IRIDIUM.get());
        // steam parts
        o.accept(mio_icif_resources.COPPER_BOILER.get());
        o.accept(mio_icif_normal.STEAM_TURBIN.get());
        o.accept(mio_icif_normal.STEAM_TURBINE_BLADE.get());
        // lathe blanks
        o.accept(mio_icif_normal.WOODEN_TURNING_BLANKS.get());
        o.accept(mio_icif_normal.IRON_TURNING_BLANKS.get());
    }

    /** Batteries and energy crystals, empty then full; the charging ones (they charge your inventory) after them. */
    private static void batteries(CreativeModeTab.Output o) {
        batPair(o, mio_icif_normal.BAT_LEV0.get(), 10_000);
        batPair(o, mio_icif_normal.ADVBAT_LEV0.get(), 100_000);
        batPair(o, mio_icif_normal.LITHIUM_BATTERY.get(), 50_000);
        batPair(o, mio_icif_normal.ADV_LITHIUM_BATTERY.get(), 200_000);
        chargedPair(o, mio_icif_normal.THORIUM_BATTERY.get());
        crystal(o, mio_icif_normal.CRYSTAL_LEV0.get());
        crystal(o, mio_icif_normal.LAPOTRON_CRYSTAL_LEV0.get());
        chargedPair(o, mio_icif_normal.SUPER_LAPOTRON_CRYSTAL.get());
        chargedPair(o, mio_icif_normal.CHARGEBAT_LEV0.get());
        chargedPair(o, mio_icif_normal.ADVCHARGEBAT_0.get());
        chargedPair(o, mio_icif_normal.CRYSTAL_CHARGEBAT_LEV0.get());
        chargedPair(o, mio_icif_normal.LAMACRYSTAL_CHARGEBAT_LEV0.get());
        chargedPair(o, mio_icif_normal.CHARGING_SUPER_LAPOTRON_CRYSTAL.get());
    }

    /** What goes into the nuclear reactor's grid, and the fusion reactor's fuel. */
    private static void reactorComponents(CreativeModeTab.Output o) {
        // fuel rods: uranium, MOX, proton (0.1.10.36; octuple 0.1.10.38)
        o.accept(mio_icif_normal.FUEL_ROD.get());
        o.accept(mio_icif_reactors.URANIUM_SIMPLE.get());
        o.accept(mio_icif_reactors.URANIUM_DUAL.get());
        o.accept(mio_icif_reactors.URANIUM_QUAD.get());
        o.accept(mio_icif_reactors.URANIUM_SIMPLE_DEPLETED.get());
        o.accept(mio_icif_reactors.URANIUM_DUAL_DEPLETED.get());
        o.accept(mio_icif_reactors.URANIUM_QUAD_DEPLETED.get());
        o.accept(mio_icif_reactors.MOX_SIMPLE.get());
        o.accept(mio_icif_reactors.MOX_DUAL.get());
        o.accept(mio_icif_reactors.MOX_QUAD.get());
        o.accept(mio_icif_reactors.MOX_SIMPLE_DEPLETED.get());
        o.accept(mio_icif_reactors.MOX_DUAL_DEPLETED.get());
        o.accept(mio_icif_reactors.MOX_QUAD_DEPLETED.get());
        o.accept(mio_icif_normal.MOX_QUAD_DEPLETE.get());
        o.accept(mio_icif_reactors.PROTON_SIMPLE.get());
        o.accept(mio_icif_reactors.PROTON_DUAL.get());
        o.accept(mio_icif_reactors.PROTON_QUAD.get());
        o.accept(mio_icif_reactors.PROTON_OCTUPLE.get());
        o.accept(mio_icif_reactors.PROTON_SIMPLE_DEPLETED.get());
        o.accept(mio_icif_reactors.PROTON_DUAL_DEPLETED.get());
        o.accept(mio_icif_reactors.PROTON_QUAD_DEPLETED.get());
        o.accept(mio_icif_reactors.PROTON_OCTUPLE_DEPLETED.get());
        o.accept(mio_icif_reactors.PROTON_OCTUPLE_CASING.get());
        o.accept(mio_icif_reactors.PROTON_SHARD.get());
        // neutron sources and breeding
        o.accept(mio_icif_reactors.ISOTOPE_ROD.get());
        o.accept(mio_icif_reactors.LITHIUM_CELL.get());
        o.accept(mio_icif_normal.TRITIUM_CELL.get());   // 0.1.10.36
        o.accept(mio_icif_reactors.HEATPACK.get());
        // cooling
        o.accept(mio_icif_reactors.COLLANT_SIMPLE.get());
        o.accept(mio_icif_reactors.COLLANT_TRIPLE.get());
        o.accept(mio_icif_reactors.COOLANT_SIX.get());
        o.accept(mio_icif_reactors.CONDENSATOR.get());
        o.accept(mio_icif_reactors.CONDENSATOR_LAP.get());
        o.accept(mio_icif_reactors.VENT.get());
        o.accept(mio_icif_reactors.VENT_CORE.get());
        o.accept(mio_icif_reactors.VENT_SPREAD.get());
        o.accept(mio_icif_reactors.OVERCLOCKED_HEAT_VENT.get());
        o.accept(mio_icif_reactors.DIAMOND_VENT.get());
        o.accept(mio_icif_reactors.IRIDIUM_HEAT_VENT.get());
        o.accept(mio_icif_reactors.IRIDIUM_OVERCLOCKED_HEAT_VENT.get());
        o.accept(mio_icif_reactors.STEAM_VENT.get());   // 0.1.10.36: the steam reactor's vents
        o.accept(mio_icif_reactors.STEAM_VENT_CORE.get());
        o.accept(mio_icif_reactors.STEAM_VENT_OVERCLOCKED.get());
        o.accept(mio_icif_reactors.STEAM_VENT_ADVANCED.get());
        o.accept(mio_icif_reactors.HEAT_SWITCH.get());
        o.accept(mio_icif_reactors.HEAT_SWITCH_CORE.get());
        o.accept(mio_icif_reactors.HEAT_SWITCH_SPREAD.get());
        o.accept(mio_icif_reactors.DIAMOND_HEAT_SWITCH.get());
        // plating and reflectors
        o.accept(mio_icif_reactors.PLATE.get());
        o.accept(mio_icif_reactors.HEAT_PLATE.get());
        o.accept(mio_icif_reactors.EXPLOSIVE_PLATE.get());
        o.accept(mio_icif_reactors.REFLECTOR.get());
        o.accept(mio_icif_reactors.THICK_REFLECTOR.get());
        o.accept(mio_icif_reactors.IRIDIUM_REFLECTOR.get());
        // records and configuration
        o.accept(mio_icif_reactors.REACTOR_BLACKBOX.get());   // 0.1.10.12
        o.accept(mio_icif_reactors.REACTOR_CONFIG_CARD.get());   // 0.1.11.76
        // fusion fuel
        o.accept(mio_icif_normal.DEUTERIUM_CELL.get());   // 0.1.11.92
        o.accept(mio_icif_normal.FROZEN_NEON_PELLET.get());   // 0.1.11.137
    }

    /** Machine upgrades, the trade box upgrade kit and the terraformer's templates. */
    private static void upgrades(CreativeModeTab.Output o) {
        o.accept(mio_icif_upgrades.OVERCLOCKER_UPGRADE.get());
        o.accept(mio_icif_upgrades.ENERGY_STORAGE_UPGRADE.get());
        o.accept(mio_icif_upgrades.TRANSFORMER_UPGRADE.get());
        o.accept(mio_icif_upgrades.EJECTOR_UPGRADE.get());
        o.accept(mio_icif_upgrades.PULLING_UPGRADE.get());
        o.accept(mio_icif_upgrades.FLUID_EJECTOR_UPGRADE.get());
        o.accept(mio_icif_upgrades.FLUID_PULLING_UPGRADE.get());
        o.accept(mio_icif_upgrades.THERMAL_RETENTION_UPGRADE.get());   // 0.1.9.1
        o.accept(mio_icif_upgrades.REDSTONE_INVERTER_UPGRADE.get());
        o.accept(mio_icif_upgrades.WIRELESS_UPGRADE.get());   // 0.1.11.158: the TESU wireless field
        o.accept(mio_icif_normal.TRADE_BOX_UPGRADE.get());
        o.accept(mio_icif_env_templates.EVT_EMPTY.get());
        o.accept(mio_icif_env_templates.EVT_CULTIVATION.get());
        o.accept(mio_icif_env_templates.EVT_DESERT.get());
        o.accept(mio_icif_env_templates.EVT_IRRIGATION.get());
        o.accept(mio_icif_env_templates.EVT_CHILLING.get());
        o.accept(mio_icif_env_templates.EVT_FLATIFICATION.get());
        o.accept(mio_icif_env_templates.EVT_MUSHROOM.get());
    }

    private static void farming(CreativeModeTab.Output o) {
        o.accept(mio_icif_resources.FERTILIZER.get());
        o.accept(mio_icif_normal.WEEDEX.get());
        // seeds
        for (String crop : new String[]{"wheat", "carrots", "potato", "cocoa", "coffee", "hops", "melon", "pumpkin", "dandelion", "rose",
                                        "tulip", "cyazint", "ferru", "cyprium", "stagnium", "plumbiscus", "redwheat", "netherWart",
                                        "redMushroom", "brownMushroom", "reed", "stickreed", "aurelia", "shining", "blackthorn",
                                        "venomilia", "eatingplant", "titanium", "uranium"}) seed(o, crop);
        // ore-rich seeds (vanilla farmland)
        o.accept(mio_icif_normal.SEED_IRON_RICH.get());
        o.accept(mio_icif_normal.SEED_COPPER_RICH.get());
        o.accept(mio_icif_normal.SEED_TIN_RICH.get());
        o.accept(mio_icif_normal.SEED_TITANIUM_RICH.get());
        o.accept(mio_icif_normal.SEED_LEAD_RICH.get());
        o.accept(mio_icif_normal.SEED_URANIUM_RICH.get());
        // harvests
        o.accept(mio_icif_resources.TERRA_WART.get());
        o.accept(mio_icif_resources.MILK_WART.get());
        o.accept(mio_icif_resources.OIL_BERRY.get());
        o.accept(mio_icif_resources.BOBS_BERRY.get());
        o.accept(mio_icif_resources.COFFEE_BEAN.get());
        o.accept(mio_icif_resources.COFFEE_DUST.get());
        o.accept(mio_icif_normal.HOPS.get());
        o.accept(mio_icif_normal.BIOCHAFF.get());
        o.accept(mio_icif_resources.FUEL_PLANT_BALL.get());
    }

    private static void food(CreativeModeTab.Output o) {
        o.accept(mio_icif_normal.COFFEE_0.get());
        o.accept(mio_icif_normal.COFFEE_1.get());
        o.accept(mio_icif_normal.COFFEE_2.get());
        o.accept(mio_icif_normal.BEER.get());
        o.accept(mio_icif_normal.RUM.get());
        o.accept(mio_icif_normal.ALE.get());
        o.accept(mio_icif_normal.BREW.get());
        o.accept(mio_icif_normal.YOUNGSTER.get());
        o.accept(mio_icif_normal.EMPTY_MUG.get());
        o.accept(mio_icif_normal.RED_MUG.get());
        o.accept(mio_icif_normal.CAN.get());
        o.accept(mio_icif_normal.TIN_EMPTY_CAN.get());
        o.accept(mio_icif_normal.TIN_FILLED_CAN.get());
    }

    /** Buckets, then the universal cell empty and filled, in the same fluid order. */
    private static void fluids(CreativeModeTab.Output o) {
        o.accept(mio_icif_fluids.DISTILLEDWATER_BUCKET.get());
        o.accept(mio_icif_fluids.HOTWATER_BUCKET.get());
        o.accept(mio_icif_fluids.UNSTABLE_HOTWATER_BUCKET.get());   // 0.1.11.94
        o.accept(mio_icif_fluids.STEAM_BUCKET.get());
        o.accept(mio_icif_fluids.SUPERHEATEDSTEAM_BUCKET.get());
        o.accept(mio_icif_fluids.COOLANT_BUCKET.get());
        o.accept(mio_icif_fluids.HOTCOOLANT_BUCKET.get());
        o.accept(mio_icif_fluids.PAHOEHOELAVA_BUCKET.get());
        o.accept(mio_icif_fluids.BIOMASS_BUCKET.get());
        o.accept(mio_icif_fluids.BIOGAS_BUCKET.get());
        o.accept(mio_icif_fluids.CRUDEOIL_BUCKET.get());
        o.accept(mio_icif_fluids.DIESELOIL_BUCKET.get());
        o.accept(mio_icif_fluids.CONSTRUCTIONFOAM_BUCKET.get());
        o.accept(mio_icif_fluids.UUMATTER_BUCKET.get());
        o.accept(mio_icif_cells.CELL_EMPTY.get());
        cell(o, Fluids.WATER);
        cell(o, Fluids.LAVA);
        cell(o, mio_icif_fluids.DISTILLEDWATER.get());
        cell(o, mio_icif_fluids.HOTWATER.get());
        cell(o, mio_icif_fluids.UNSTABLE_HOTWATER.get());
        cell(o, mio_icif_fluids.STEAM.get());
        cell(o, mio_icif_fluids.SUPERHEATEDSTEAM.get());
        cell(o, mio_icif_fluids.COOLANT.get());
        cell(o, mio_icif_fluids.HOTCOOLANT.get());
        cell(o, mio_icif_fluids.PAHOEHOELAVA.get());
        cell(o, mio_icif_fluids.BIOMASS.get());
        cell(o, mio_icif_fluids.BIOGAS.get());
        cell(o, mio_icif_fluids.CONSTRUCTIONFOAM.get());
        cell(o, mio_icif_fluids.UUMATTER.get());
        cell(o, mio_icif_fluids.AIR.get());
    }

    private static void miscItems(CreativeModeTab.Output o) {
        o.accept(mio_icif_normal.ENTITY_RUBBER_BOAT.get());
        o.accept(mio_icif_normal.BROKEN_RUBBER_BOAT.get());
        o.accept(mio_icif_normal.ENTITY_COAL_BOAT.get());
        o.accept(mio_icif_normal.ENTITY_ELECTRIC_BOAT.get());
        o.accept(mio_icif_normal.COIN.get());
        o.accept(mio_icif_normal.ID_CARD.get());
        o.accept(mio_icif_normal.INTELLIGENCE.get());
        o.accept(mio_icif_normal.MODIFY.get());
        o.accept(mio_icif_normal.STUFF.get());
        o.accept(mio_icif_normal.DRAGONBLOOD.get());
    }

    // ================================================================== blocks

    private static void grid(CreativeModeTab.Output o) {
        // bare and insulated wires by voltage
        o.accept(mio_icif_blocks.WIRE_LV.get());
        o.accept(mio_icif_blocks.WIRE_MV.get());
        o.accept(mio_icif_blocks.WIRE_HV.get());
        o.accept(mio_icif_blocks.WIRE_EV.get());
        o.accept(mio_icif_blocks.WIRE_IV.get());
        o.accept(mio_icif_blocks.WIRE_ISOLATION_LV.get());
        o.accept(mio_icif_blocks.WIRE_ISOLATION_MV.get());
        o.accept(mio_icif_blocks.WIRE_ISOLATION_HV.get());
        o.accept(mio_icif_blocks.WIRE_ISOLATION_EV.get());
        o.accept(mio_icif_blocks.WIRE_ISOLATION_HV_2.get());
        o.accept(mio_icif_blocks.WIRE_ISOLATION_EV_2.get());
        o.accept(mio_icif_blocks.WIRE_ISOLATION_EV_3.get());
        o.accept(mio_icif_blocks.SUPERCONDUCTING_CABLE.get());
        // special cables
        o.accept(mio_icif_blocks.WIRE_DETECTOR.get());
        o.accept(mio_icif_blocks.WIRE_SPLITTER.get());
        o.accept(mio_icif_blocks.WIRE_BREAKER.get());   // 0.1.10.22
        o.accept(mio_icif_blocks.WIRE_FUSE.get());
        // 0.1.11.177: the voltage checker is hidden (still registered, so placed ones survive)
        // transformers
        o.accept(mio_icif_blocks.TRANSFORMER_LV_MV.get());
        o.accept(mio_icif_blocks.TRANSFORMER_MV_HV.get());
        o.accept(mio_icif_blocks.TRANSFORMER_HV_EV.get());
        o.accept(mio_icif_blocks.TRANSFORMER_EV_SC.get());
        o.accept(mio_icif_blocks.TRANSFORMER_IV_LUV.get());
        o.accept(mio_icif_blocks.TRANSFORMER_LUV_ZPMV.get());
        // wireless power, the grid-tie interface (Energy Converter id; a legacy block from 0.1.10.39 to 0.1.11.0) and the terminal
        o.accept(mio_icif_blocks.WIRELESS_POWER_TRANSMISSION_NODE.get());
        o.accept(mio_icif_blocks.ENERGY_CONVERTER.get());
        o.accept(mio_icif_blocks.ENERGY_TERMINAL.get());
    }

    /** Each storage block as crafted, then full; the chargers beside them. */
    private static void energyStorage(CreativeModeTab.Output o) {
        storageBlock(o, mio_icif_blocks.BAT_BOX.get(), 40_000L, "mio_icif:batbox");
        storageBlock(o, mio_icif_blocks.BATBOX_CHARGER.get(), 40_000L, "mio_icif:batbox_charger");
        storageBlock(o, mio_icif_blocks.CESU.get(), 300_000L, "mio_icif:cesu");
        storageBlock(o, mio_icif_blocks.CESU_CHARGER.get(), 300_000L, "mio_icif:cesu_charger");
        storageBlock(o, mio_icif_blocks.MFE.get(), 4_000_000L, "mio_icif:mfe");
        storageBlock(o, mio_icif_blocks.MFE_CHARGER.get(), 4_000_000L, "mio_icif:mfe_charger");
        storageBlock(o, mio_icif_blocks.MFSU.get(), 40_000_000L, "mio_icif:mfsu");
        storageBlock(o, mio_icif_blocks.MFSU_CHARGER.get(), 40_000_000L, "mio_icif:mfsu_charger");
        storageBlock(o, mio_icif_blocks.LESU.get(), 1_000_000L, "mio_icif:lesu");
        storageBlock(o, mio_icif_blocks.LESU_CHARGER.get(), 1_000_000L, "mio_icif:lesu_charger");
        storageBlock(o, mio_icif_blocks.EESU.get(), 400_000_000L, "mio_icif:eesu");
        storageBlock(o, mio_icif_blocks.EESU_CHARGER.get(), 400_000_000L, "mio_icif:eesu_charger");
        // GESU
        storageBlock(o, mio_icif_blocks.GESU_CORE.get(), 2_147_483_647L, "mio_icif:gesu_core");
        o.accept(mio_icif_blocks.GESU_INPUT_IV.get());
        o.accept(mio_icif_blocks.GESU_OUTPUT_IV.get());
        o.accept(mio_icif_blocks.GESU_OUTPUT_LUV.get());
        o.accept(mio_icif_blocks.GESU_INDUCTION_PORT.get());   // 0.1.11.206
        // TESU (0.1.11.157; 227,000,000,000 EU since 0.1.11.158) and its wireless field (the connector is with the tools)
        storageBlock(o, mio_icif_blocks.TESU_CONTROLLER.get(), 227_000_000_000L, "mio_icif:tesu_controller");
        o.accept(mio_icif_blocks.TESU_INPUT.get());
        o.accept(mio_icif_blocks.TESU_OUTPUT.get());
        o.accept(mio_icif_blocks.TESU_CASING.get());
        o.accept(mio_icif_blocks.TESU_CELL.get());
        o.accept(mio_icif_blocks.TESU_WIRELESS_PORT.get());
        o.accept(mio_icif_blocks.TESU_INDUCTION_PORT.get());   // 0.1.11.206
        o.accept(mio_icif_blocks.TESU_RELAY.get());
    }

    /** EU generators, then kinetic, heat and steam. */
    private static void generators(CreativeModeTab.Output o) {
        o.accept(mio_icif_blocks.THERMAL_GENERATOR.get());
        o.accept(mio_icif_blocks.GEO_GENERATOR.get());
        o.accept(mio_icif_blocks.WATER_GENERATOR.get());
        o.accept(mio_icif_blocks.WIND_GENERATOR.get());
        // solar panels
        o.accept(mio_icif_blocks.SOLAR_GENERATOR.get());
        o.accept(mio_icif_blocks.ADVANCED_SOLAR_PANEL.get());
        o.accept(mio_icif_blocks.HYBRID_SOLAR_PANEL.get());
        o.accept(mio_icif_blocks.ULTIMATE_HYBRID_SOLAR_PANEL.get());
        o.accept(mio_icif_blocks.QUANTUM_SOLAR_PANEL.get());
        o.accept(mio_icif_blocks.METS_ADVANCED_SOLAR_GENERATOR.get());
        o.accept(mio_icif_blocks.PHOTON_RESONANCE_SOLAR_GENERATOR.get());
        o.accept(mio_icif_blocks.ULTIMATE_PHOTON_RESONANCE_SOLAR_GENERATOR.get());
        // fuel generators
        o.accept(mio_icif_blocks.SEMIFLUID_GENERATOR.get());
        o.accept(mio_icif_blocks.ADVANCED_SEMIFLUID_GENERATOR.get());
        o.accept(mio_icif_blocks.DIESEL_GENERATOR.get());
        o.accept(mio_icif_blocks.RT_GENERATOR.get());
        o.accept(mio_icif_blocks.DROP_GENERATOR.get());
        o.accept(mio_icif_blocks.ADVANCED_DROP_GENERATOR.get());
        o.accept(mio_icif_blocks.EXPERIENCE_GENERATOR.get());
        o.accept(mio_icif_blocks.ADVANCED_EXPERIENCE_GENERATOR.get());
        // geomagnetic
        o.accept(mio_icif_blocks.GEOMAGNETIC_GENERATOR.get());
        o.accept(mio_icif_blocks.GEOMAGNETIC_ANTENNA.get());
        o.accept(mio_icif_blocks.GEOMAGNETIC_PEDESTAL.get());
        // 0.1.10.36: the Infinite Generator is hidden (still registered, so placed ones survive), and so is the quantum generator
        o.accept(mio_icif_blocks.CREATIVE_GENERATOR.get());
        // kinetic
        o.accept(mio_icif_blocks.MANUAL_KINETIC_GENERATOR.get());
        o.accept(mio_icif_blocks.STIRLING_KINETIC_GENERATOR.get());
        o.accept(mio_icif_blocks.WIND_KINETIC_GENERATOR.get());
        o.accept(mio_icif_blocks.WATER_KINETIC_GENERATOR.get());
        o.accept(mio_icif_blocks.KINETIC_GENERATOR_ELC.get());
        o.accept(mio_icif_blocks.KINETIC_GENERATOR.get());
        o.accept(mio_icif_blocks.TURBO_KINETIC_GENERATOR.get());
        o.accept(mio_icif_blocks.TWIN_TURBO_KINETIC_GENERATOR.get());
        // heat
        o.accept(mio_icif_blocks.SOLID_HEAT_GENERATOR.get());
        o.accept(mio_icif_blocks.FLUID_HEAT_GENERATOR.get());
        o.accept(mio_icif_blocks.RT_HEAT_GENERATOR.get());
        o.accept(mio_icif_blocks.HEAT_GENERATOR_ELC.get());
        o.accept(mio_icif_blocks.HEAT_SOURCE_FLUID.get());
        o.accept(mio_icif_blocks.STIRLING_GENERATOR.get());
        o.accept(mio_icif_blocks.ADVANCED_STIRLING_GENERATOR.get());
        // steam, the steam tunnel (0.1.10.39) and its rotors (0.1.10.41)
        o.accept(mio_icif_blocks.STEAM_GENERATOR.get());
        o.accept(mio_icif_blocks.STEAM_KINETIC_GENERATOR.get());
        o.accept(mio_icif_blocks.SOLAR_DISTILLER.get());
        o.accept(mio_icif_blocks.STEAM_REPRESSURIZER.get());
        o.accept(mio_icif_blocks.STEAM_TURBINE.get());   // 0.1.10.36
        o.accept(mio_icif_blocks.STEAM_TUNNEL.get());
        o.accept(mio_icif_blocks.STEAM_TUNNEL_CASING.get());
        o.accept(mio_icif_blocks.STEAM_TUNNEL_FLUID_PORT.get());
        o.accept(mio_icif_blocks.STEAM_TUNNEL_POWER_PORT.get());
        o.accept(mio_icif_blocks.STEAM_TURBINE_ROTOR_BRONZE.get());
        o.accept(mio_icif_blocks.STEAM_TURBINE_ROTOR_IRON.get());
        o.accept(mio_icif_blocks.STEAM_TURBINE_ROTOR_ADVIRON.get());
        o.accept(mio_icif_blocks.STEAM_TURBINE_ROTOR_TITANIUM.get());
        o.accept(mio_icif_blocks.STEAM_TURBINE_ROTOR_IRIDIUM.get());
        o.accept(mio_icif_blocks.STEAM_TURBINE_ROTOR_NETHERITE.get());
    }

    /** The nuclear reactors' blocks and the fusion reactor's. */
    private static void reactors(CreativeModeTab.Output o) {
        o.accept(mio_icif_blocks.NUCLEAR_REACTOR_GENERATOR.get());
        o.accept(mio_icif_blocks.REACTOR_CHAMBER.get());
        o.accept(mio_icif_blocks.STEAM_REACTOR.get());   // 0.1.10.36
        o.accept(mio_icif_blocks.STEAM_REACTOR_CHAMBER.get());
        o.accept(mio_icif_blocks.REACTOR_VESSEL.get());
        o.accept(mio_icif_blocks.REACTOR_ACCESS_HATCH.get());
        o.accept(mio_icif_blocks.REACTOR_FLUID_PORT.get());
        o.accept(mio_icif_blocks.REACTOR_REDSTONE_PORT.get());
        o.accept(mio_icif_blocks.REDSTONE_REACTOR_COOLANT_INJECTOR.get());
        o.accept(mio_icif_blocks.LAPIS_REACTOR_COOLANT_INJECTOR.get());
        o.accept(mio_icif_blocks.FUSION_CONTROLLER.get());   // 0.1.11.89
        o.accept(mio_icif_blocks.FUSION_CENTRAL_SOLENOID.get());
        o.accept(mio_icif_blocks.FUSION_TOROIDAL_COIL.get());
        o.accept(mio_icif_blocks.FUSION_POLOIDAL_COIL.get());
        o.accept(mio_icif_blocks.FUSION_VACUUM_VESSEL.get());
        o.accept(mio_icif_blocks.FUSION_BLANKET.get());
        o.accept(mio_icif_blocks.FUSION_VIEWPORT.get());   // 0.1.11.139
        o.accept(mio_icif_blocks.FUSION_DIVERTOR.get());
        o.accept(mio_icif_blocks.FUSION_CRYOSTAT.get());
        o.accept(mio_icif_blocks.FUSION_NBI_PORT.get());
        o.accept(mio_icif_blocks.FUSION_ECRH_PORT.get());
        o.accept(mio_icif_blocks.FUSION_FUEL_PORT.get());
        o.accept(mio_icif_blocks.FUSION_EXHAUST_PORT.get());
        o.accept(mio_icif_blocks.FUSION_CRYO_PORT.get());
        o.accept(mio_icif_blocks.FUSION_POWER_PORT.get());
        o.accept(mio_icif_blocks.FUSION_MITIGATION_PORT.get());
    }

    private static void machines(CreativeModeTab.Output o) {
        // basic processing
        o.accept(mio_icif_blocks.FURNACE_ELC.get());
        o.accept(mio_icif_blocks.INDUCTION_ELC.get());
        o.accept(mio_icif_blocks.POWDER_ELC.get());
        o.accept(mio_icif_blocks.POWDER_ADVANCED_ELC.get());
        o.accept(mio_icif_blocks.COMPRESSOR_ELC.get());
        o.accept(mio_icif_blocks.COMPRESSOR_ADVANCED_ELC.get());
        o.accept(mio_icif_blocks.EXTRACTOR_ELC.get());
        o.accept(mio_icif_blocks.WASHER_ELC.get());
        o.accept(mio_icif_blocks.METAL_FORMER.get());
        o.accept(mio_icif_blocks.METAL_FORMER_ADVANCED.get());
        o.accept(mio_icif_blocks.BLOCK_CUTTER.get());
        o.accept(mio_icif_blocks.LATHE.get());
        o.accept(mio_icif_blocks.CANNER_ELC.get());
        o.accept(mio_icif_blocks.RECYCLER_ELC.get());
        o.accept(mio_icif_blocks.BLAST_FURNACE.get());
        o.accept(mio_icif_blocks.BLAST_FURNACE_ELC.get());
        o.accept(mio_icif_blocks.BLAST_FURNACE_ADVANCED.get());
        // chemistry and fluids
        o.accept(mio_icif_blocks.CENTRIFUGE_ELC.get());
        o.accept(mio_icif_blocks.ELECTROLYZER.get());
        o.accept(mio_icif_blocks.MAGNETIZER.get());
        o.accept(mio_icif_blocks.PUMP_ELC.get());
        o.accept(mio_icif_blocks.CONDENSER.get());
        o.accept(mio_icif_blocks.FERMENTER_ELC.get());
        o.accept(mio_icif_blocks.OIL_REFINERY_ELC.get());
        // UU-matter
        o.accept(mio_icif_blocks.MATTER_ELC.get());
        o.accept(mio_icif_blocks.LARGE_FABRICATOR_CORE.get());
        o.accept(mio_icif_blocks.LARGE_FABRICATOR_INPUT_IV.get());
        o.accept(mio_icif_blocks.LARGE_FABRICATOR_TANK.get());
        o.accept(mio_icif_blocks.LARGE_FABRICATOR_SCRAP.get());
        o.accept(mio_icif_blocks.SCANNER_ELC.get());
        o.accept(mio_icif_blocks.REPLICATOR_ELC.get());
        o.accept(mio_icif_blocks.NEUTRON_POLYMERIZER.get());
        o.accept(mio_icif_blocks.MOLECULAR_TRANSFORMER.get());
        // special machines
        o.accept(mio_icif_blocks.TELEPORTER_ELC.get());
        o.accept(mio_icif_blocks.TERRA_ELC.get());
        o.accept(mio_icif_blocks.INDUSTRIAL_WORKBENCH.get());
        o.accept(mio_icif_blocks.BATCH_CRAFTER.get());
        o.accept(BuilderRegistry.BUILDER_ITEM.get());
        o.accept(mio_icif_blocks.CHUNK_LOADER.get());
        // agriculture
        o.accept(mio_icif_blocks.RESIN_COLLECTOR.get());
        o.accept(mio_icif_blocks.CROP_STICK.get());
        o.accept(mio_icif_blocks.CROP_STICK_UPGRADED.get());
        o.accept(mio_icif_blocks.HARVEST_ELC.get());
        o.accept(mio_icif_blocks.MATRON_ELC.get());
        // oil and mining
        o.accept(mio_icif_blocks.OIL_RIG_CORE.get());
        o.accept(mio_icif_blocks.OIL_RIG_BASE.get());
        o.accept(mio_icif_blocks.OIL_RIG_INPUT.get());
        o.accept(mio_icif_blocks.OIL_RIG_OUTPUT.get());
        o.accept(mio_icif_blocks.OIL_RIG_PANEL.get());
        o.accept(mio_icif_blocks.DIMENSION_OIL_RIG_CORE.get());
        o.accept(mio_icif_blocks.MINER_ELC.get());
        o.accept(mio_icif_blocks.ADVANCED_MINER_ELC.get());
        o.accept(mio_icif_blocks.BLOCK_MINING_PIPE.get());
        o.accept(mio_icif_blocks.BLOCK_MINING_TIP.get());
        // defence
        o.accept(mio_icif_blocks.TESLA.get());
        o.accept(mio_icif_blocks.LASER_DEFENSE_TOWER.get());
        o.accept(mio_icif_blocks.SKY_PATROL_LASER_TOWER.get());
        // the armory
        o.accept(ArmoryRegistry.ARMORY_ITEM.get());
        o.accept(ArmoryRegistry.SHOWCASE_ITEM.get());
        o.accept(BuilderRegistry.MATERIAL_INTERFACE_ITEM.get());
        o.accept(BuilderRegistry.INFINITE_MATERIAL_INTERFACE_ITEM.get());   // 0.1.11.187
        // trade and futures
        o.accept(mio_icif_blocks.TRADE_TERMINAL.get());
        o.accept(mio_icif_blocks.TRADE_BOX.get());   // 0.1.10.42
        o.accept(mio_icif_blocks.TRADE_BOX_ADVANCED.get());
        o.accept(mio_icif_blocks.FUTURE_ELC.get());
    }

    /** Item and fluid transport. */
    private static void logistics(CreativeModeTab.Output o) {
        o.accept(mio_icif_blocks.ITEM_BUFFER_ELC.get());
        o.accept(mio_icif_blocks.SORTER_ELC.get());
        o.accept(mio_icif_blocks.ITEM_DISTRIBUTOR_ELC.get());
        o.accept(LogisticsRegistry.EJECTOR_ITEM.get());
        o.accept(mio_icif_blocks.PIPE_ITEM_INPUT.get());
        o.accept(mio_icif_blocks.PIPE_ITEM_TRANSPORT.get());
        // fluids (the legacy water pipe stays registered and craftable for existing worlds)
        o.accept(mio_icif_blocks.FLUID_DISTRIBUTOR_ELC.get());
        o.accept(mio_icif_blocks.WEIGHTED_FLUID_DISTRIBUTOR_ELC.get());
        o.accept(mio_icif_blocks.FLUID_REGULATOR_ELC.get());
        mio_icif_blocks.FLUID_PIPE_ITEMS.forEach(pipe -> o.accept(pipe.get()));
        o.accept(mio_icif_blocks.PIPE_WATER_EXTRACT.get());
    }

    /** Every building block in one run: machine hulls, the iron fence, then all the build/ blocks. */
    private static void building(CreativeModeTab.Output o) {
        o.accept(mio_icif_blocks.MACHINE_HULL_BASIC.get());
        o.accept(mio_icif_blocks.MACHINE_HULL_ADVANCED.get());
        o.accept(mio_icif_blocks.BLOCK_FENCE_IRON.get());
        // scaffolds
        o.accept(mio_icif_blocks.SCAFFOLD_WOOD.get());
        o.accept(mio_icif_blocks.SCAFFOLD_IRON.get());
        o.accept(mio_icif_blocks.SCAFFOLD_STEEL.get());
        o.accept(mio_icif_blocks.SCAFFOLD_CARBON.get());
        o.accept(mio_icif_blocks.TITANIUM_DRILL_FRAME.get());
        // storage boxes and tanks, by material
        o.accept(mio_icif_blocks.STORAGE_BOX_WOOD.get());
        o.accept(mio_icif_blocks.STORAGE_BOX_BRONZE.get());
        o.accept(mio_icif_blocks.STORAGE_BOX_IRON.get());
        o.accept(mio_icif_blocks.STORAGE_BOX_ADVIRON.get());
        o.accept(mio_icif_blocks.STORAGE_BOX_TITANIUM.get());
        o.accept(mio_icif_blocks.STORAGE_BOX_IRIDIUM.get());
        o.accept(mio_icif_blocks.BRONZE_TANK.get());
        o.accept(mio_icif_blocks.IRON_TANK.get());
        o.accept(mio_icif_blocks.ADVIRON_TANK.get());
        o.accept(mio_icif_blocks.TITANIUM_TANK.get());
        o.accept(mio_icif_blocks.IRIDIUM_TANK.get());
        // glass, door, construction foam
        o.accept(mio_icif_blocks.BLOCK_ALLOY_GLASS.get());
        o.accept(mio_icif_blocks.BLOCK_IRRADIANT_GLASS_PANE.get());
        o.accept(mio_icif_blocks.BLOCK_ALLOY_DOOR.get());
        o.accept(mio_icif_blocks.CONSTRUCTION_FOAM.get());
        o.accept(mio_icif_blocks.CONSTRUCTION_WALL.get());
        // rubber wood
        o.accept(mio_icif_blocks.RUBBER_PLANKS.get());
        o.accept(mio_icif_blocks.RUBBER_STAIRS.get());
        o.accept(mio_icif_blocks.RUBBER_SLAB.get());
        o.accept(mio_icif_blocks.RUBBER_FENCE.get());
        o.accept(mio_icif_blocks.RUBBER_FENCE_GATE.get());
        o.accept(mio_icif_blocks.RUBBER_DOOR.get());
        o.accept(mio_icif_blocks.RUBBER_TRAPDOOR.get());
        o.accept(mio_icif_blocks.RUBBER_PRESSURE_PLATE.get());
        o.accept(mio_icif_blocks.RUBBER_BUTTON.get());
        o.accept(mio_icif_blocks.RUBBER_SIGN.get());
        o.accept(mio_icif_blocks.RUBBER_HANGING_SIGN.get());
    }

    // ------------------------------------------------------------------ suits (last)

    /**
     * Suit plugins: quantum-only units and the bay expander, units both suits take, nano-only units
     * and the lattice densifier, each unit's Mk.II right after its Mk.I; the frame cores last.
     * (The standalone modification station was retired into the Armory's integrated modification bay.)
     */
    private static void suit(CreativeModeTab.Output o) {
        for (int group = 0; group < 3; group++) {
            for (SuitModuleType type : SuitModuleType.VALUES) {
                boolean nano = type.allows(SuitClass.NANO), quantum = type.allows(SuitClass.QUANTUM);
                int of = type.nanoOnly() ? 2 : nano && quantum ? 1 : 0;
                if (of != group) continue;
                o.accept(SuitRegistry.unitItem(type));
                if (type.hasMk2()) o.accept(SuitRegistry.unitItem(type, 2));
            }
            if (group == 0) {
                o.accept(SuitRegistry.BAY_EXPANDER.get());
                o.accept(SuitRegistry.LIVING_NANO_LINING.get());
            }
            if (group == 2) o.accept(SuitRegistry.LATTICE_DENSIFIER.get());
        }
        for (var core : SuitRegistry.cores()) o.accept(core.get());
    }

    /**
     * 创建包含能量数据的BlockEntityTag NBT 标签
     * 用于给储电方块物品添加满电状态
     * @param energy 能量值（EU）
     * @param blockEntityId BlockEntity 的注册ID（如 "mio_icif:batbox"）
     * @return 包含能量数据的CompoundTag
     */
    private static CompoundTag createEnergyTag(long energy, String blockEntityId) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", blockEntityId);
        tag.putLong("energy", energy);
        return tag;
    }

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
