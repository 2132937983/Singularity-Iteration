package com.singularity_iteration.mio_icif.uu;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * UU 扫描值配置加载器
 * 负责从配置文件加载 UU 基础值
 * 与 IC2 1.12.2 原版 uu_scan_values.ini 对应
 */
public class UuScanValues {
    private final Map<ItemStack, Double> worldScanValues = new HashMap<>();
    private final Map<ItemStack, Double> predefinedValues = new HashMap<>();

    /**
     * 从默认资源加载 UU 扫描值
     */
    public void loadDefaultValues() {
        worldScanValues.clear();

        // 基础世界扫描值（与 IC2 原版 uu_scan_values.ini 对应）
        // 这些值代表从世界中直接获取物品的基础 UU 价值
        addWorldScanValue("minecraft:cobblestone", 1.0);
        addWorldScanValue("minecraft:dirt", 14.857);
        addWorldScanValue("minecraft:sand", 106.903);
        addWorldScanValue("minecraft:gravel", 44.486);
        addWorldScanValue("minecraft:flint", 399.761);
        addWorldScanValue("minecraft:coal", 60.626);
        addWorldScanValue("minecraft:redstone", 79.725);
        addWorldScanValue("minecraft:iron_ore", 107.273);
        addWorldScanValue("minecraft:gold_ore", 1015.020);
        addWorldScanValue("minecraft:diamond", 2879.305);
        addWorldScanValue("minecraft:emerald", 26754.193);
        addWorldScanValue("minecraft:clay_ball", 1734.485);
        addWorldScanValue("minecraft:oak_log", 3505.472);
        addWorldScanValue("minecraft:spruce_log", 2938.849);
        addWorldScanValue("minecraft:birch_log", 2938.849);
        addWorldScanValue("minecraft:jungle_log", 5604.989);
        addWorldScanValue("minecraft:acacia_log", 3505.472);
        addWorldScanValue("minecraft:dark_oak_log", 3505.472);
        addWorldScanValue("minecraft:wheat_seeds", 4738.072);
        addWorldScanValue("minecraft:apple", 42805.387);
        addWorldScanValue("minecraft:cactus", 264075.306);
        addWorldScanValue("minecraft:sugar_cane", 194425.815);
        addWorldScanValue("minecraft:pumpkin", 2439907.056);
        addWorldScanValue("minecraft:melon", 874916.167);
        addWorldScanValue("minecraft:bone", 261681.875);
        addWorldScanValue("minecraft:rotten_flesh", 226448.890);
        addWorldScanValue("minecraft:gunpowder", 247123.254);
        addWorldScanValue("minecraft:string", 9721.291);
        addWorldScanValue("minecraft:spider_eye", 5432374.0);
        addWorldScanValue("minecraft:feather", 100000.0);
        addWorldScanValue("minecraft:leather", 500000.0);
        addWorldScanValue("minecraft:slime_ball", 2000000.0);
        addWorldScanValue("minecraft:ender_pearl", 50000000.0);
        addWorldScanValue("minecraft:blaze_rod", 30000000.0);
        addWorldScanValue("minecraft:ghast_tear", 100000000.0);
        addWorldScanValue("minecraft:nether_star", 1000000000.0);
        addWorldScanValue("minecraft:prismarine_shard", 500000.0);
        addWorldScanValue("minecraft:prismarine_crystals", 1007170.936);
        addWorldScanValue("minecraft:chorus_fruit", 20000000.0);
        addWorldScanValue("minecraft:shulker_shell", 500000000.0);
        addWorldScanValue("minecraft:obsidian", 43308350.25);
        addWorldScanValue("minecraft:netherrack", 100.0);
        addWorldScanValue("minecraft:soul_sand", 500.0);
        addWorldScanValue("minecraft:glowstone_dust", 5000000.0);
        addWorldScanValue("minecraft:quartz", 8000000.0);
        addWorldScanValue("minecraft:ancient_debris", 5000000.0);
        addWorldScanValue("minecraft:netherite_scrap", 5000000.0);
        addWorldScanValue("minecraft:ice", 1000.0);
        addWorldScanValue("minecraft:sea_pickle", 500000.0);
        addWorldScanValue("minecraft:lily_pad", 45491.965);
        addWorldScanValue("minecraft:brown_mushroom", 62971.065);
        addWorldScanValue("minecraft:red_mushroom", 165141.469);
        addWorldScanValue("minecraft:vine", 50000.0);
        addWorldScanValue("minecraft:water_bucket", 100000.0);
        addWorldScanValue("minecraft:lava_bucket", 100000.0);
        addWorldScanValue("minecraft:milk_bucket", 500000.0);
        addWorldScanValue("minecraft:snowball", 276.315);
        addWorldScanValue("minecraft:egg", 1000000.0);
        addWorldScanValue("minecraft:scute", 5000000.0);
        addWorldScanValue("minecraft:ink_sac", 2000000.0);
        addWorldScanValue("minecraft:glow_ink_sac", 5000000.0);
        addWorldScanValue("minecraft:honeycomb", 10000000.0);
        addWorldScanValue("minecraft:amethyst_shard", 50000000.0);
        addWorldScanValue("minecraft:pointed_dripstone", 100000.0);
        addWorldScanValue("minecraft:moss_block", 500000.0);
        addWorldScanValue("minecraft:big_dripleaf", 2000000.0);
        addWorldScanValue("minecraft:glow_berries", 5000000.0);
        addWorldScanValue("minecraft:spore_blossom", 10000000.0);
        addWorldScanValue("minecraft:copper_ore", 111.0157);
        addWorldScanValue("minecraft:deepslate_copper_ore", 111.0157);
        addWorldScanValue("mio_icif:block_ore_tin", 136.038);
        addWorldScanValue("mio_icif:block_ore_tin_in_deep", 136.038);
        addWorldScanValue("mio_icif:block_ore_lead", 918.236);
        addWorldScanValue("mio_icif:block_ore_lead_in_deep", 918.236);
        addWorldScanValue("mio_icif:block_ore_uran", 1607.004);
        addWorldScanValue("mio_icif:block_ore_uran_in_deep", 1607.004);

        addWorldScanValue("minecraft:sweet_berries", 500000.0);
        addWorldScanValue("minecraft:cocoa_beans", 500000.0);
        addWorldScanValue("minecraft:kelp", 100000.0);
        addWorldScanValue("minecraft:bamboo", 100000.0);
        addWorldScanValue("minecraft:salmon", 500000.0);
        addWorldScanValue("minecraft:cod", 500000.0);
        addWorldScanValue("minecraft:pufferfish", 2000000.0);
        addWorldScanValue("minecraft:tropical_fish", 2000000.0);
        addWorldScanValue("minecraft:ink_sac", 2000000.0);
        addWorldScanValue("minecraft:lapis_lazuli", 416.574);

        addWorldScanValue("minecraft:deepslate_iron_ore", 107.273);
        addWorldScanValue("minecraft:deepslate_gold_ore", 1015.020);
        addWorldScanValue("minecraft:deepslate_diamond_ore", 2879.305);
        addWorldScanValue("minecraft:deepslate_emerald_ore", 26754.193);
        addWorldScanValue("minecraft:deepslate_lapis_ore", 416.574);
        addWorldScanValue("minecraft:deepslate_redstone_ore", 79.725);
        addWorldScanValue("minecraft:deepslate_coal_ore", 60.626);
        addWorldScanValue("minecraft:stone", 1.0);
        addWorldScanValue("minecraft:granite", 10.0);
        addWorldScanValue("minecraft:diorite", 10.0);
        addWorldScanValue("minecraft:andesite", 10.0);
        addWorldScanValue("minecraft:deepslate", 10.0);
        addWorldScanValue("minecraft:tuff", 10.0);
        addWorldScanValue("minecraft:calcite", 50.0);
        addWorldScanValue("minecraft:dripstone_block", 100.0);
        addWorldScanValue("minecraft:grass_block", 20.0);
        addWorldScanValue("minecraft:podzol", 50.0);
        addWorldScanValue("minecraft:rooted_dirt", 50.0);
        addWorldScanValue("minecraft:mud", 30.0);
        addWorldScanValue("minecraft:clay", 5000.0);
        addWorldScanValue("minecraft:gravel", 44.486);
        addWorldScanValue("minecraft:sandstone", 133.571);
        addWorldScanValue("minecraft:red_sand", 150.0);
        addWorldScanValue("minecraft:red_sandstone", 200.0);
        addWorldScanValue("minecraft:terracotta", 615.558);
        addWorldScanValue("minecraft:white_terracotta", 700.0);
        addWorldScanValue("minecraft:orange_terracotta", 700.0);
        addWorldScanValue("minecraft:magenta_terracotta", 700.0);
        addWorldScanValue("minecraft:light_blue_terracotta", 700.0);
        addWorldScanValue("minecraft:yellow_terracotta", 700.0);
        addWorldScanValue("minecraft:lime_terracotta", 700.0);
        addWorldScanValue("minecraft:pink_terracotta", 700.0);
        addWorldScanValue("minecraft:gray_terracotta", 700.0);
        addWorldScanValue("minecraft:light_gray_terracotta", 700.0);
        addWorldScanValue("minecraft:cyan_terracotta", 700.0);
        addWorldScanValue("minecraft:purple_terracotta", 700.0);
        addWorldScanValue("minecraft:blue_terracotta", 700.0);
        addWorldScanValue("minecraft:brown_terracotta", 700.0);
        addWorldScanValue("minecraft:green_terracotta", 700.0);
        addWorldScanValue("minecraft:red_terracotta", 700.0);
        addWorldScanValue("minecraft:black_terracotta", 700.0);
        addWorldScanValue("minecraft:oak_leaves", 1000.0);
        addWorldScanValue("minecraft:spruce_leaves", 1000.0);
        addWorldScanValue("minecraft:birch_leaves", 1000.0);
        addWorldScanValue("minecraft:jungle_leaves", 1000.0);
        addWorldScanValue("minecraft:acacia_leaves", 1000.0);
        addWorldScanValue("minecraft:dark_oak_leaves", 1000.0);
        addWorldScanValue("minecraft:mangrove_leaves", 1000.0);
        addWorldScanValue("minecraft:azalea_leaves", 2000.0);
        addWorldScanValue("minecraft:flowering_azalea_leaves", 5000.0);
        addWorldScanValue("minecraft:cherry_leaves", 2000.0);
        addWorldScanValue("minecraft:oak_sapling", 5349.186);
        addWorldScanValue("minecraft:spruce_sapling", 10000.0);
        addWorldScanValue("minecraft:birch_sapling", 10000.0);
        addWorldScanValue("minecraft:jungle_sapling", 13292.925);
        addWorldScanValue("minecraft:acacia_sapling", 10000.0);
        addWorldScanValue("minecraft:dark_oak_sapling", 10000.0);
        addWorldScanValue("minecraft:mangrove_propagule", 15000.0);
        addWorldScanValue("minecraft:cherry_sapling", 15000.0);
        addWorldScanValue("minecraft:azalea", 20000.0);
        addWorldScanValue("minecraft:flowering_azalea", 50000.0);
        addWorldScanValue("minecraft:torchflower_seeds", 50000.0);
        addWorldScanValue("minecraft:pitcher_pod", 50000.0);
        addWorldScanValue("minecraft:seagrass", 5000.0);
        addWorldScanValue("minecraft:sea_pickle", 50000.0);
        addWorldScanValue("minecraft:tube_coral", 100000.0);
        addWorldScanValue("minecraft:brain_coral", 100000.0);
        addWorldScanValue("minecraft:bubble_coral", 100000.0);
        addWorldScanValue("minecraft:fire_coral", 100000.0);
        addWorldScanValue("minecraft:horn_coral", 100000.0);
        addWorldScanValue("minecraft:tube_coral_fan", 100000.0);
        addWorldScanValue("minecraft:brain_coral_fan", 100000.0);
        addWorldScanValue("minecraft:bubble_coral_fan", 100000.0);
        addWorldScanValue("minecraft:fire_coral_fan", 100000.0);
        addWorldScanValue("minecraft:horn_coral_fan", 100000.0);
        addWorldScanValue("minecraft:dead_tube_coral", 50000.0);
        addWorldScanValue("minecraft:dead_brain_coral", 50000.0);
        addWorldScanValue("minecraft:dead_bubble_coral", 50000.0);
        addWorldScanValue("minecraft:dead_fire_coral", 50000.0);
        addWorldScanValue("minecraft:dead_horn_coral", 50000.0);
        addWorldScanValue("minecraft:snow_block", 1000.0);
        addWorldScanValue("minecraft:ice", 1000.0);
        addWorldScanValue("minecraft:packed_ice", 5000.0);
        addWorldScanValue("minecraft:blue_ice", 25000.0);
        addWorldScanValue("minecraft:prismarine", 29381.513);
        addWorldScanValue("minecraft:prismarine_bricks", 50000.0);
        addWorldScanValue("minecraft:dark_prismarine", 50000.0);
        addWorldScanValue("minecraft:magma_block", 100000.0);
        addWorldScanValue("minecraft:soul_soil", 500.0);
        addWorldScanValue("minecraft:basalt", 100.0);
        addWorldScanValue("minecraft:smooth_basalt", 100.0);
        addWorldScanValue("minecraft:blackstone", 200.0);
        addWorldScanValue("minecraft:gilded_blackstone", 500000.0);
        addWorldScanValue("minecraft:end_stone", 100.0);
        addWorldScanValue("minecraft:chorus_plant", 500000.0);
        addWorldScanValue("minecraft:chorus_flower", 1000000.0);
        addWorldScanValue("minecraft:dragon_breath", 1000000000.0);
        addWorldScanValue("minecraft:dragon_egg", 10000000000.0);
        addWorldScanValue("minecraft:elytra", 50000000000.0);
        addWorldScanValue("minecraft:totem_of_undying", 100000000000.0);
        addWorldScanValue("minecraft:heart_of_the_sea", 50000000000.0);
        addWorldScanValue("minecraft:trident", 50000000000.0);
        addWorldScanValue("minecraft:sniffer_egg", 100000000.0);
        addWorldScanValue("minecraft:turtle_egg", 50000000.0);
        addWorldScanValue("minecraft:armadillo_scute", 5000000.0);
        addWorldScanValue("minecraft:wolf_armor", 100000000.0);
        addWorldScanValue("minecraft:experience_bottle", 10000000.0);
        addWorldScanValue("minecraft:saddle", 2706771.891);
        addWorldScanValue("minecraft:name_tag", 2510629.0);
        addWorldScanValue("minecraft:lead", 500000.0);
        addWorldScanValue("minecraft:bundle", 10000000.0);
        addWorldScanValue("minecraft:disc_fragment_5", 50000000.0);
        addWorldScanValue("minecraft:echo_shard", 500000000.0);
        addWorldScanValue("minecraft:disc_fragment_5", 50000000.0);
        addWorldScanValue("minecraft:netherite_upgrade_smithing_template", 100000000000.0);
        addWorldScanValue("minecraft:sentry_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:vex_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:wild_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:coast_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:dune_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:wayfinder_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:raiser_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:host_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:ward_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:silence_armor_trim_smithing_template", 100000000000.0);
        addWorldScanValue("minecraft:tide_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:snout_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:rib_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:eye_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:spire_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:flow_armor_trim_smithing_template", 50000000000.0);
        addWorldScanValue("minecraft:bolt_armor_trim_smithing_template", 50000000000.0);

        // MIO-ICIF 模组物品
        // IC2原版: iridium_ore = 12000, iridium_shard = 1333
        addWorldScanValue("mio_icif:resource/item_shard_iridium", 1333.0);
        addWorldScanValue("mio_icif:resource/item_iridium", 11997.0);
        addWorldScanValue("mio_icif:resource/item_iridium_ingot", 11997.0);
        addWorldScanValue("mio_icif:resource/item_iridium_plate", 54000.0);

        Singularity_Iteration.LOGGER.info("[UU] Loaded {} default world scan values.", worldScanValues.size());
    }

    /**
     * 从配置文件目录加载用户自定义值
     */
    public void loadFromFile(Path configDir) {
        Path filePath = configDir.resolve("uu_scan_values.ini");
        if (!Files.exists(filePath)) {
            // 创建默认配置文件
            createDefaultConfig(filePath);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) continue;

                String[] parts = line.split("=");
                if (parts.length != 2) continue;

                String itemId = parts[0].trim();
                String valueStr = parts[1].trim();

                try {
                    double value = Double.parseDouble(valueStr);
                    ResourceLocation rl = ResourceLocation.tryParse(itemId);
                    if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) {
                        Item item = BuiltInRegistries.ITEM.get(rl);
                        worldScanValues.put(new ItemStack(item), value);
                    }
                } catch (NumberFormatException e) {
                    Singularity_Iteration.LOGGER.warn("[UU] Invalid value in uu_scan_values.ini: {}", line);
                }
            }
            Singularity_Iteration.LOGGER.info("[UU] Loaded {} values from uu_scan_values.ini.", worldScanValues.size());
        } catch (IOException e) {
            Singularity_Iteration.LOGGER.error("[UU] Failed to load uu_scan_values.ini: {}", e.getMessage());
        }
    }

    private void addWorldScanValue(String itemId, double value) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) {
            Item item = BuiltInRegistries.ITEM.get(rl);
            worldScanValues.put(new ItemStack(item), value);
        }
    }

    private void createDefaultConfig(Path filePath) {
        try {
            Files.createDirectories(filePath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                writer.write("; UU Scan Values Configuration");
                writer.newLine();
                writer.write("; Format: item_id = uu_value");
                writer.newLine();
                writer.write("; These values represent the base UU value of items obtainable from the world.");
                writer.newLine();
                writer.newLine();
                writer.write("; Base blocks");
                writer.newLine();
                writer.write("minecraft:cobblestone = 1.0");
                writer.newLine();
                writer.write("minecraft:dirt = 14.857");
                writer.newLine();
                writer.write("minecraft:sand = 106.903");
                writer.newLine();
                writer.write("minecraft:gravel = 44.486");
                writer.newLine();
                writer.write("minecraft:stone = 1.0");
                writer.newLine();
                writer.newLine();
                writer.write("; Ores");
                writer.newLine();
                writer.write("minecraft:coal = 60.626");
                writer.newLine();
                writer.write("minecraft:redstone = 79.725");
                writer.newLine();
                writer.write("minecraft:iron_ore = 107.273");
                writer.newLine();
                writer.write("minecraft:gold_ore = 1015.020");
                writer.newLine();
                writer.write("minecraft:diamond = 2879.305");
                writer.newLine();
                writer.write("minecraft:emerald = 26754.193");
                writer.newLine();
                writer.newLine();
                writer.write("; Logs");
                writer.newLine();
                writer.write("minecraft:oak_log = 3505.472");
                writer.newLine();
                writer.write("minecraft:spruce_log = 2938.849");
                writer.newLine();
                writer.newLine();
                writer.write("; Add your custom values below");
                writer.newLine();
            }
            Singularity_Iteration.LOGGER.info("[UU] Created default uu_scan_values.ini at {}", filePath);
        } catch (IOException e) {
            Singularity_Iteration.LOGGER.error("[UU] Failed to create default config: {}", e.getMessage());
        }
    }

    public Map<ItemStack, Double> getWorldScanValues() {
        return worldScanValues;
    }

    public Map<ItemStack, Double> getPredefinedValues() {
        return predefinedValues;
    }
}