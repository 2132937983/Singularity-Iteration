package com.singularity_iteration.mio_icif.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("null")
public class mio_icif_tags {

    public static final TagKey<Item> NUCLEAR = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuclear"));

    public static final TagKey<Item> CIRCUITS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "circuits"));
    public static final TagKey<Item> CIRCUITS_BASIC = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "circuits/basic"));
    public static final TagKey<Item> CIRCUITS_ADVANCED = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "circuits/advanced"));

    public static final TagKey<Block> MACHINE = BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", "machine"));

    public static final TagKey<Block> CABLE = BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", "cable"));

    public static final TagKey<Block> GENERATOR = BlockTags.create(ResourceLocation.fromNamespaceAndPath("mio_icif", "generator"));

    public static final TagKey<Block> ENERGY_CONTAINER = BlockTags.create(ResourceLocation.fromNamespaceAndPath("mio_icif", "energy_container"));

    public static final TagKey<Block> PRODUCER = BlockTags.create(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer"));

    public static final TagKey<Block> CHARGEPAD = BlockTags.create(ResourceLocation.fromNamespaceAndPath("mio_icif", "chargepad"));

    public static final TagKey<Item> ORES_IRON = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/iron"));
    public static final TagKey<Item> ORES_GOLD = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/gold"));
    public static final TagKey<Item> ORES_COPPER = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/copper"));
    public static final TagKey<Item> ORES_TIN = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/tin"));
    public static final TagKey<Item> ORES_LEAD = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/lead"));
    public static final TagKey<Item> ORES_URANIUM = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/uranium"));
    public static final TagKey<Item> ORES_COAL = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/coal"));
    public static final TagKey<Item> ORES_DIAMOND = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/diamond"));
    public static final TagKey<Item> ORES_EMERALD = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/emerald"));
    public static final TagKey<Item> ORES_LAPIS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/lapis"));
    public static final TagKey<Item> ORES_REDSTONE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/redstone"));
    public static final TagKey<Item> ORES_QUARTZ = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/quartz"));
    public static final TagKey<Item> ORES_NETHER_GOLD = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/nether_gold"));
}