// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.event;

import com.miophas.singularity_iteration.core.api.crop.PlantStats;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.runtime.crop.PlantRegistry;
import com.miophas.singularity_iteration.common.crop.PlantBaseSapling;
import com.miophas.singularity_iteration.common.crop.PlantEatingPlant;
import com.miophas.singularity_iteration.common.crop.PlantGenericCrop;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Independent registration of the 54 observed SI crop types and 28 base seeds. */
public class CropRegistryEvent {
    private static final String PACKAGE = "com.miophas.singularity_iteration.common.crop.";
    private static final String[] ZERO_ARG_PLANTS = {
        "PlantAurelia", "PlantBeetroot", "PlantBlackthorn", "PlantBrownMushroom", "PlantCarrots",
        "PlantCocoa", "PlantCoffee", "PlantCyazint", "PlantCyprium", "PlantDandelion", "PlantFerru",
        "PlantFlax", "PlantHops", "PlantMelon", "PlantNetherWart", "PlantPlumbiscus", "PlantPotato",
        "PlantPumpkin", "PlantRedMushroom", "PlantRedwheat", "PlantReed", "PlantRose", "PlantShining",
        "PlantStagnium", "PlantStickreed", "PlantTerraWart", "PlantTitanium", "PlantTulip",
        "PlantUranium", "PlantVenomilia", "PlantWeed", "PlantWheat"
    };

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CropRegistryEvent::registerAll);
    }

    private static void registerAll() {
        PlantRegistry registry = PlantRegistry.instance;
        for (String name : ZERO_ARG_PLANTS) {
            try {
                PlantType plant = (PlantType)Class.forName(PACKAGE + name).getConstructor().newInstance();
                register(registry, plant);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to construct public crop class " + name, exception);
            }
        }
        register(registry, new PlantEatingPlant());
        for (PlantType plant : configuredPlants()) register(registry, plant);
        registerBaseSeeds(registry);
    }

    private static void register(PlantRegistry registry, PlantType plant) {
        if (registry.getPlant(plant.getModId(), plant.getTypeId()) == null) registry.registerPlant(plant);
    }

    private static List<PlantType> configuredPlants() {
        return List.of(
            generic("blazereed", "Mr. Brain", a("Fire","Blaze","Reed","Sulfur"), s(6,0,4,1,0,0), 4,4,4,1,1200, "minecraft:blaze_rod"),
            generic("bobs_yer_uncle_ranks_berries", "GenerikB", a("Shiny","Vine","Emerald","Berylium","Crystal"), s(11,4,0,8,2,9), 4,4,4,1,2200, "mio_icif:resource/item_bobs_yer_uncle_ranks_berry"),
            generic("corium", "Gregorius Techneticies", a("Cow","Silk","Vine"), s(6,0,2,3,1,0), 4,4,4,1,1200, "minecraft:leather"),
            generic("corpse_plant", "Mr. Kenny", a("Toxic","Undead","Vine","Edible","Rotten"), s(5,0,2,1,0,3), 4,4,4,1,1000, "minecraft:rotten_flesh"),
            generic("creeper_weed", "General Spaz", a("Creeper","Vine","Explosive","Fire","Sulfur","Saltpeter","Coal"), s(7,3,0,5,1,3), 4,4,4,1,1400, "minecraft:gunpowder"),
            generic("diareed", "Diareed", a("Fire","Shiny","Reed","Coal","Diamond","Crystal"), s(12,5,0,10,2,10), 4,4,4,1,2400, "mio_icif:resource/item_diamond_dust_small"),
            generic("egg_plant", "Link", a("Chicken","Egg","Edible","Feather","Flower","Addictive"), s(6,0,4,1,0,0), 3,3,3,2,5400, "minecraft:egg"),
            generic("ender_blossom", "RichardG", a("Ender","Flower","Shiny"), s(10,5,0,2,1,6), 4,4,4,1,2000, "mio_icif:resource/item_ender_pearl_dust"),
            generic("meat_rose", "VintageBeef", a("Edible","Flower","Cow","Chicken","Pig","Sheep"), s(7,0,4,1,3,0), 4,4,4,1,10500, "minecraft:pink_dye"),
            generic("milk_wart", "Mr. Brain", a("Edible","Milk","Cow"), s(6,0,3,0,1,0), 3,3,3,1,5400, "mio_icif:resource/item_milk_wart"),
            generic("oil_berries", "Spacetoad", a("Fire","Dark","Reed","Rotten","Coal","Oil"), s(9,6,1,2,1,12), 3,3,3,1,1800, "mio_icif:resource/item_oil_berry"),
            generic("slime_plant", "Neowulf", a("Slime","Bouncy","Sticky","Bush"), s(6,3,0,0,0,2), 4,4,4,3,1200, "minecraft:slime_ball"),
            generic("spidernip", "Mr. Kenny", a("Toxic","Silk","Spider","Flower","Ingredient","Addictive"), s(4,2,1,4,1,3), 4,4,4,1,2400, "minecraft:string"),
            generic("tearstalks", "Neowulf", a("Healing","Nether","Ingredient","Reed","Ghast"), s(8,1,2,0,0,0), 4,4,4,1,1600, "minecraft:ghast_tear"),
            generic("withereed", "CovertJaguar", a("Fire","Undead","Reed","Coal","Rotten","Wither"), s(8,2,0,4,1,3), 4,4,4,1,1600, "mio_icif:resource/item_coal_dust"),
            sapling("oak_sapling", "minecraft:oak_leaves", "minecraft:oak_sapling", "minecraft:oak_log"),
            sapling("spruce_sapling", "minecraft:spruce_leaves", "minecraft:spruce_sapling", "minecraft:spruce_log"),
            sapling("birch_sapling", "minecraft:birch_leaves", "minecraft:birch_sapling", "minecraft:birch_log"),
            sapling("jungle_sapling", "minecraft:jungle_leaves", "minecraft:jungle_sapling", "minecraft:jungle_log"),
            sapling("acacia_sapling", "minecraft:acacia_leaves", "minecraft:acacia_sapling", "minecraft:acacia_log"),
            sapling("dark_oak_sapling", "minecraft:dark_oak_leaves", "minecraft:dark_oak_sapling", "minecraft:dark_oak_log")
        );
    }

    /** Shared by legacy factories so there is only one authoritative built-in definition. */
    public static PlantGenericCrop createConfiguredCrop(String id) {
        return configuredPlants().stream().filter(plant -> plant instanceof PlantGenericCrop && plant.getTypeId().equals(id))
            .map(plant -> (PlantGenericCrop) plant).findFirst().orElseThrow(() -> new IllegalArgumentException(id));
    }

    private static PlantGenericCrop generic(String id, String foundBy, String[] traits, PlantStats stats,
            int max, int harvest, int optimal, int after, int growthTime, String... drops) {
        if (growthTime % Math.max(1, stats.getLevel()) != 0) {
            throw new IllegalArgumentException("Observed crop growth time is not level-aligned: " + id);
        }
        return new PlantGenericCrop(id, foundBy, traits, stats, max, harvest, optimal,
            java.util.Arrays.stream(drops).map(CropRegistryEvent::stack).toArray(ItemStack[]::new),
            java.util.Arrays.stream(specialDrops(id)).map(CropRegistryEvent::stack).toArray(ItemStack[]::new), after, growthTime / Math.max(1, stats.getLevel()));
    }

    private static String[] specialDrops(String id) {
        return switch (id) {
            case "blazereed" -> a("minecraft:blaze_powder", "mio_icif:resource/item_sulfur_dust");
            case "bobs_yer_uncle_ranks_berries" -> a("minecraft:emerald");
            case "corpse_plant" -> a("minecraft:bone", "minecraft:bone_meal", "minecraft:bone_meal");
            case "diareed" -> a("minecraft:diamond");
            case "egg_plant" -> a("minecraft:chicken", "minecraft:feather", "minecraft:feather", "minecraft:feather");
            case "ender_blossom" -> a("minecraft:ender_pearl", "minecraft:ender_pearl", "minecraft:ender_eye");
            case "meat_rose" -> a("minecraft:beef", "minecraft:porkchop", "minecraft:chicken", "minecraft:mutton");
            case "spidernip" -> a("minecraft:spider_eye", "minecraft:vine");
            case "withereed" -> a("minecraft:coal", "minecraft:coal");
            default -> a();
        };
    }

    private static PlantBaseSapling sapling(String id, String leaves, String sapling, String log) {
        return new PlantBaseSapling(id, a("Leaves","Sapling","Green"), stack(leaves), stack(sapling),
            stack(log), "oak_sapling".equals(id));
    }

    private static String[] a(String... values) { return values; }
    private static PlantStats s(int... v) { return new PlantStats(v[0],v[1],v[2],v[3],v[4],v[5]); }
    private static ItemStack stack(String id) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (BuiltInRegistries.ITEM.getKey(item).equals(ResourceLocation.withDefaultNamespace("air"))) {
            throw new IllegalStateException("Missing base-seed or crop drop item " + id);
        }
        return new ItemStack(item);
    }

    private static void registerBaseSeeds(PlantRegistry registry) {
        Object[][] rows = {
            {"mio_icif:resource/item_milk_wart","milk_wart",1,1,1,1,0},
            {"minecraft:wheat_seeds","wheat",1,1,1,1,0}, {"minecraft:pumpkin_seeds","pumpkin",1,1,1,1,0},
            {"minecraft:melon_seeds","melon",1,1,1,1,0}, {"minecraft:nether_wart","netherwart",1,1,1,1,0},
            {"mio_icif:resource/item_terra_wart","terrawart",1,1,1,1,0}, {"mio_icif:resource/item_coffee_bean","coffee",1,1,1,1,0},
            {"minecraft:sugar_cane","reed",1,3,0,2,0}, {"minecraft:cocoa_beans","cocoa",1,0,0,0,0},
            {"minecraft:poppy","rose",4,1,1,1,0}, {"minecraft:dandelion","dandelion",4,1,1,1,0},
            {"minecraft:carrot","carrots",1,1,1,1,0}, {"minecraft:potato","potato",1,1,1,1,0},
            {"minecraft:brown_mushroom","brownMushroom",1,1,1,1,0}, {"minecraft:red_mushroom","redMushroom",1,1,1,1,0},
            {"minecraft:cactus","eatingplant",1,1,1,1,0}, {"minecraft:beetroot_seeds","beetroot",1,1,1,1,0},
            {"minecraft:oak_sapling","oak_sapling",1,1,1,1,0}, {"minecraft:spruce_sapling","spruce_sapling",1,1,1,1,0},
            {"minecraft:birch_sapling","birch_sapling",1,1,1,1,0}, {"minecraft:jungle_sapling","jungle_sapling",1,1,1,1,0},
            {"minecraft:acacia_sapling","acacia_sapling",1,1,1,1,0}, {"minecraft:dark_oak_sapling","dark_oak_sapling",1,1,1,1,0},
            {"mio_icif:crop/iron_rich_seed","ferru",1,1,1,1,0}, {"mio_icif:crop/copper_rich_seed","cyprium",1,1,1,1,0},
            {"mio_icif:crop/tin_rich_seed","stagnium",1,1,1,1,0}, {"mio_icif:crop/titanium_rich_seed","titanium",1,1,1,1,0},
            {"mio_icif:crop/lead_rich_seed","plumbiscus",1,1,1,1,0}, {"mio_icif:crop/uranium_rich_seed","uranium",1,1,1,1,0}
        };
        for (Object[] row : rows) {
            PlantType plant = registry.getPlant("mio_icif", (String)row[1]);
            if (plant == null) throw new IllegalStateException("Missing registered plant " + row[1]);
            ItemStack baseSeed = stack((String)row[0]);
            if (baseSeed.is(net.minecraft.world.item.Items.POPPY) || baseSeed.is(net.minecraft.world.item.Items.DANDELION)
                    || baseSeed.is(net.minecraft.world.item.Items.BROWN_MUSHROOM) || baseSeed.is(net.minecraft.world.item.Items.RED_MUSHROOM)) baseSeed.setCount(4);
            registry.registerBaseSeed(baseSeed, plant,
                (int)row[2], (int)row[3], (int)row[4], (int)row[5], (int)row[6]);
        }
    }
}
