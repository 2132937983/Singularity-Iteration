package com.singularity_iteration.mio_icif.event;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.internal.crop.PlantRegistry;
import com.singularity_iteration.mio_icif.crop.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 作物注册事件处理�? * 负责在游戏启动时注册所有作物类�? */
@SuppressWarnings("null")
public class CropRegistryEvent {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 杂草和小�
        PlantRegistry.instance.registerPlant(new PlantWeed());
            PlantRegistry.instance.registerPlant(new PlantWheat());

            // 注册原版种子映射
            registerBaseSeeds();

            // 基础作物
            PlantRegistry.instance.registerPlant(new PlantCarrots());
            PlantRegistry.instance.registerPlant(new PlantPotato());
            PlantRegistry.instance.registerPlant(new PlantCocoa());
            PlantRegistry.instance.registerPlant(new PlantCoffee());
            PlantRegistry.instance.registerPlant(new PlantHops());
            PlantRegistry.instance.registerPlant(new PlantMelon());
            PlantRegistry.instance.registerPlant(new PlantPumpkin());
            PlantRegistry.instance.registerPlant(new PlantBeetroot());
            PlantRegistry.instance.registerPlant(new PlantFlax());

            // 花朵
            PlantRegistry.instance.registerPlant(new PlantDandelion());
            PlantRegistry.instance.registerPlant(new PlantRose());
            PlantRegistry.instance.registerPlant(new PlantTulip());
            PlantRegistry.instance.registerPlant(new PlantCyazint());

            // 金属植物
            PlantRegistry.instance.registerPlant(new PlantFerru());
            PlantRegistry.instance.registerPlant(new PlantCyprium());
            PlantRegistry.instance.registerPlant(new PlantStagnium());
            PlantRegistry.instance.registerPlant(new PlantPlumbiscus());
            PlantRegistry.instance.registerPlant(new PlantAurelia());
            PlantRegistry.instance.registerPlant(new PlantShining());
            PlantRegistry.instance.registerPlant(new PlantTitanium());
            PlantRegistry.instance.registerPlant(new PlantUranium());

            // 特殊植物
            PlantRegistry.instance.registerPlant(new PlantRedwheat());
            PlantRegistry.instance.registerPlant(new PlantNetherWart());
            PlantRegistry.instance.registerPlant(new PlantTerraWart());
            PlantRegistry.instance.registerPlant(new PlantRedMushroom());
            PlantRegistry.instance.registerPlant(new PlantBrownMushroom());
            PlantRegistry.instance.registerPlant(new PlantReed());
            PlantRegistry.instance.registerPlant(new PlantStickreed());
            PlantRegistry.instance.registerPlant(new PlantBlackthorn());
            PlantRegistry.instance.registerPlant(new PlantVenomilia());
            PlantRegistry.instance.registerPlant(new PlantEatingPlant());

            // 树苗作物（添加原木作为次要作物）
            PlantRegistry.instance.registerPlant(new PlantBaseSapling("oak_sapling",
                    new String[]{"Leaves", "Sapling", "Green"},
                    new ItemStack(Blocks.OAK_LEAVES), new ItemStack(Blocks.OAK_SAPLING),
                    new ItemStack(Blocks.OAK_LOG), true));
            PlantRegistry.instance.registerPlant(new PlantBaseSapling("spruce_sapling",
                    new String[]{"Leaves", "Sapling", "Green"},
                    new ItemStack(Blocks.SPRUCE_LEAVES), new ItemStack(Blocks.SPRUCE_SAPLING),
                    new ItemStack(Blocks.SPRUCE_LOG), false));
            PlantRegistry.instance.registerPlant(new PlantBaseSapling("birch_sapling",
                    new String[]{"Leaves", "Sapling", "Green"},
                    new ItemStack(Blocks.BIRCH_LEAVES), new ItemStack(Blocks.BIRCH_SAPLING),
                    new ItemStack(Blocks.BIRCH_LOG), false));
            PlantRegistry.instance.registerPlant(new PlantBaseSapling("jungle_sapling",
                    new String[]{"Leaves", "Sapling", "Green"},
                    new ItemStack(Blocks.JUNGLE_LEAVES), new ItemStack(Blocks.JUNGLE_SAPLING),
                    new ItemStack(Blocks.JUNGLE_LOG), false));
            PlantRegistry.instance.registerPlant(new PlantBaseSapling("acacia_sapling",
                    new String[]{"Leaves", "Sapling", "Green"},
                    new ItemStack(Blocks.ACACIA_LEAVES), new ItemStack(Blocks.ACACIA_SAPLING),
                    new ItemStack(Blocks.ACACIA_LOG), false));
            PlantRegistry.instance.registerPlant(new PlantBaseSapling("dark_oak_sapling",
                    new String[]{"Leaves", "Sapling", "Green"},
                    new ItemStack(Blocks.DARK_OAK_LEAVES), new ItemStack(Blocks.DARK_OAK_SAPLING),
                    new ItemStack(Blocks.DARK_OAK_LOG), false));

            // IC2高级作物 (GenericCropCard)
            PlantRegistry.instance.registerPlant(IC2GenericCrops.blazereed());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.bobsYerUncle());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.corium());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.corpsePlant());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.creeperWeed());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.diareed());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.eggPlant());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.enderBlossom());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.meatRose());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.milkWart());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.oilBerries());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.slimePlant());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.spidernip());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.tearstalks());
            PlantRegistry.instance.registerPlant(IC2GenericCrops.withereed());

            Singularity_Iteration.LOGGER.info("Registered {} plant types", PlantRegistry.instance.getAllPlants().size());
        });
    }

    /**
     * 注册原版种子映射
     * 让原版种子可以种在作物架�
 */
    private static void registerBaseSeeds() {
        // === IC2原版base seed注册 ===
        // 以下严格按照IC2 1.12.2的IC2Crops.registerBaseSeeds()对齐

        // 小麦种子 -> 小麦 (size=1, 1, 1, 1)
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Items.WHEAT_SEEDS),
                new PlantWheat(),
                1, 1, 1, 1
        );

        // 南瓜种子 -> 南瓜 (size=1, 1, 1, 1)
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Items.PUMPKIN_SEEDS),
                new PlantPumpkin(),
                1, 1, 1, 1
        );

        // 西瓜种子 -> 西瓜 (size=1, 1, 1, 1)
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Items.MELON_SEEDS),
                new PlantMelon(),
                1, 1, 1, 1
        );

        // 地狱�?-> 地狱�?(size=1, 1, 1, 1)
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Items.NETHER_WART),
                new PlantNetherWart(),
                1, 1, 1, 1
        );

        // 大地�?-> 大地�?(size=1, 1, 1, 1)
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(mio_icif_resources.TERRA_WART.get()),
                new PlantTerraWart(),
                1, 1, 1, 1
        );

        // 咖啡�?-> 咖啡 (size=1, 1, 1, 1)
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(mio_icif_resources.COFFEE_BEAN.get()),
                new PlantCoffee(),
                1, 1, 1, 1
        );

        // 甘蔗 -> 甘蔗（芦苇）(size=1, 3, 0, 2) - IC2特殊�
    PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Items.SUGAR_CANE),
                new PlantReed(),
                1, 3, 0, 2
        );

        // 可可�?-> 可可 (size=1, 0, 0, 0) - IC2特殊�
    PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Items.COCOA_BEANS),
                new PlantCocoa(),
                1, 0, 0, 0
        );

        // 红色�?罂粟) -> 玫瑰 (size=4, 1, 1, 1) - IC2用方块形式，数量4
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Blocks.POPPY),
                new PlantRose(),
                4, 1, 1, 1
        );

        // 黄色�?蒲公�? -> 蒲公�?(size=4, 1, 1, 1) - IC2用方块形式，数量4
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Blocks.DANDELION),
                new PlantDandelion(),
                4, 1, 1, 1
        );

        // 胡萝�?-> 胡萝�?(size=1, 1, 1, 1)
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Items.CARROT),
                new PlantCarrots(),
                1, 1, 1, 1
        );

        // 马铃�?-> 马铃�?(size=1, 1, 1, 1)
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Items.POTATO),
                new PlantPotato(),
                1, 1, 1, 1
        );

        // 棕色蘑菇方块 -> 棕蘑�?(size=1, 1, 1, 1) - IC2用方块形态
    PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Blocks.BROWN_MUSHROOM),
                new PlantBrownMushroom(),
                1, 1, 1, 1
        );

        // 红色蘑菇方块 -> 红蘑�?(size=1, 1, 1, 1) - IC2用方块形态
    PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Blocks.RED_MUSHROOM),
                new PlantRedMushroom(),
                1, 1, 1, 1
        );

        // 仙人�?-> 食人�?(size=1, 1, 1, 1)
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Items.CACTUS),
                new PlantEatingPlant(),
                1, 1, 1, 1
        );

        // 甜菜种子 -> 甜菜 (size=1, 1, 1, 1)
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Items.BEETROOT_SEEDS),
                new PlantBeetroot(),
                1, 1, 1, 1
        );

        // === 树苗 (IC2用sapling方块，meta 0-5，添加原木作为次要作物) ===
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Blocks.OAK_SAPLING),
                new PlantBaseSapling("oak_sapling", new String[]{"Leaves", "Sapling", "Green"},
                        new ItemStack(Blocks.OAK_LEAVES), new ItemStack(Blocks.OAK_SAPLING),
                        new ItemStack(Blocks.OAK_LOG), true),
                1, 1, 1, 1
        );
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Blocks.SPRUCE_SAPLING),
                new PlantBaseSapling("spruce_sapling", new String[]{"Leaves", "Sapling", "Green"},
                        new ItemStack(Blocks.SPRUCE_LEAVES), new ItemStack(Blocks.SPRUCE_SAPLING),
                        new ItemStack(Blocks.SPRUCE_LOG), false),
                1, 1, 1, 1
        );
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Blocks.BIRCH_SAPLING),
                new PlantBaseSapling("birch_sapling", new String[]{"Leaves", "Sapling", "Green"},
                        new ItemStack(Blocks.BIRCH_LEAVES), new ItemStack(Blocks.BIRCH_SAPLING),
                        new ItemStack(Blocks.BIRCH_LOG), false),
                1, 1, 1, 1
        );
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Blocks.JUNGLE_SAPLING),
                new PlantBaseSapling("jungle_sapling", new String[]{"Leaves", "Sapling", "Green"},
                        new ItemStack(Blocks.JUNGLE_LEAVES), new ItemStack(Blocks.JUNGLE_SAPLING),
                        new ItemStack(Blocks.JUNGLE_LOG), false),
                1, 1, 1, 1
        );
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Blocks.ACACIA_SAPLING),
                new PlantBaseSapling("acacia_sapling", new String[]{"Leaves", "Sapling", "Green"},
                        new ItemStack(Blocks.ACACIA_LEAVES), new ItemStack(Blocks.ACACIA_SAPLING),
                        new ItemStack(Blocks.ACACIA_LOG), false),
                1, 1, 1, 1
        );
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(Blocks.DARK_OAK_SAPLING),
                new PlantBaseSapling("dark_oak_sapling", new String[]{"Leaves", "Sapling", "Green"},
                        new ItemStack(Blocks.DARK_OAK_LEAVES), new ItemStack(Blocks.DARK_OAK_SAPLING),
                        new ItemStack(Blocks.DARK_OAK_LOG), false),
                1, 1, 1, 1
        );

        // === 以下作物在IC2中无base seed，只能通过杂交获得 ===
        // 亚麻(Flax), 啤酒花(Hops), 红小麦(Redwheat), 粘性芦苇(Stickreed),
        // 黑刺(BlackThorn), 毒花(Venomilia), 郁金香(Tulip), 缤纷花(Cyazint),
        // 所有金属作物(Ferru/Cyprium/Stagnium/Plumbiscus/Aurelia/Shining),
        // 所有高级作物(Blazereed/BobsYerUncle/Corium/CorpsePlant/CreeperWeed/
        //   Diareed/EggPlant/EnderBlossom/MeatRose/OilBerries/SlimePlant/
        //   Spidernip/Tearstalks/Withereed)

        // === 富集作物种子注册 (可在作物架上种植) ===
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.SEED_IRON_RICH.get()),
                new PlantFerru(),
                1, 1, 1, 1
        );
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.SEED_COPPER_RICH.get()),
                new PlantCyprium(),
                1, 1, 1, 1
        );
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.SEED_TIN_RICH.get()),
                new PlantStagnium(),
                1, 1, 1, 1
        );
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.SEED_TITANIUM_RICH.get()),
                new PlantTitanium(),
                1, 1, 1, 1
        );
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.SEED_LEAD_RICH.get()),
                new PlantPlumbiscus(),
                1, 1, 1, 1
        );
        PlantRegistry.instance.registerBaseSeed(
                new ItemStack(com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal.SEED_URANIUM_RICH.get()),
                new PlantUranium(),
                1, 1, 1, 1
        );

        Singularity_Iteration.LOGGER.info("Registered base seeds (aligned with IC2)");
    }
}