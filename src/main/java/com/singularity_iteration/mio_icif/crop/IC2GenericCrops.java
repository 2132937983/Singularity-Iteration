package com.singularity_iteration.mio_icif.crop;

import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.crop.PlantStats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("null")
public class IC2GenericCrops {

    public static PlantGenericCrop blazereed() {
        return new PlantGenericCrop(
                "blazereed", "Mr. Brain",
                new String[]{"Fire", "Blaze", "Reed", "Sulfur"},
                new PlantStats(6, 0, 4, 1, 0, 0),
                4, 4, 4,
                new ItemStack[]{new ItemStack(Items.BLAZE_ROD, 1)},
                new ItemStack[]{new ItemStack(Items.BLAZE_POWDER, 1), new ItemStack(mio_icif_resources.SULFUR_DUST.get(), 1)},
                1, 200
        );
    }

    public static PlantGenericCrop bobsYerUncle() {
        return new PlantGenericCrop(
                "bobs_yer_uncle_ranks_berries", "GenerikB",
                new String[]{"Shiny", "Vine", "Emerald", "Berylium", "Crystal"},
                new PlantStats(11, 4, 0, 8, 2, 9),
                4, 4, 4,
                new ItemStack[]{new ItemStack(Items.EMERALD, 1)},
                new ItemStack[]{new ItemStack(Items.EMERALD, 1)},
                1, 200
        );
    }

    public static PlantGenericCrop corium() {
        return new PlantGenericCrop(
                "corium", "Gregorius Techneticies",
                new String[]{"Cow", "Silk", "Vine"},
                new PlantStats(6, 0, 2, 3, 1, 0),
                4, 4, 4,
                new ItemStack[]{new ItemStack(Items.LEATHER, 1)},
                null,
                1, 200
        );
    }

    public static PlantGenericCrop corpsePlant() {
        return new PlantGenericCrop(
                "corpse_plant", "Mr. Kenny",
                new String[]{"Toxic", "Undead", "Vine", "Edible", "Rotten"},
                new PlantStats(5, 0, 2, 1, 0, 3),
                4, 4, 4,
                new ItemStack[]{new ItemStack(Items.ROTTEN_FLESH, 1)},
                new ItemStack[]{new ItemStack(Items.BONE, 1), new ItemStack(Items.INK_SAC, 1), new ItemStack(Items.INK_SAC, 1)},
                1, 200
        );
    }

    public static PlantGenericCrop creeperWeed() {
        return new PlantGenericCrop(
                "creeper_weed", "General Spaz",
                new String[]{"Creeper", "Vine", "Explosive", "Fire", "Sulfur", "Saltpeter", "Coal"},
                new PlantStats(7, 3, 0, 5, 1, 3),
                4, 4, 4,
                new ItemStack[]{new ItemStack(Items.GUNPOWDER, 1)},
                null,
                1, 200
        );
    }

    public static PlantGenericCrop diareed() {
        return new PlantGenericCrop(
                "diareed", "Diareed",
                new String[]{"Fire", "Shiny", "Reed", "Coal", "Diamond", "Crystal"},
                new PlantStats(12, 5, 0, 10, 2, 10),
                4, 4, 4,
                new ItemStack[]{new ItemStack(Items.DIAMOND, 1)},
                new ItemStack[]{new ItemStack(Items.DIAMOND, 1)},
                1, 200
        );
    }

    public static PlantGenericCrop eggPlant() {
        return new PlantGenericCrop(
                "egg_plant", "Link",
                new String[]{"Chicken", "Egg", "Edible", "Feather", "Flower", "Addictive"},
                new PlantStats(6, 0, 4, 1, 0, 0),
                3, 3, 3,
                new ItemStack[]{new ItemStack(Items.EGG, 1)},
                new ItemStack[]{new ItemStack(Items.FEATHER, 1), new ItemStack(Items.CHICKEN, 1), new ItemStack(Items.CHICKEN, 1), new ItemStack(Items.CHICKEN, 1)},
                2, 900
        );
    }

    public static PlantGenericCrop enderBlossom() {
        return new PlantGenericCrop(
                "ender_blossom", "RichardG",
                new String[]{"Ender", "Flower", "Shiny"},
                new PlantStats(10, 5, 0, 2, 1, 6),
                4, 4, 4,
                new ItemStack[]{new ItemStack(Items.ENDER_PEARL, 1)},
                new ItemStack[]{new ItemStack(Items.ENDER_PEARL, 1), new ItemStack(Items.ENDER_PEARL, 1), new ItemStack(Items.ENDER_EYE, 1)},
                1, 200
        );
    }

    public static PlantGenericCrop meatRose() {
        return new PlantGenericCrop(
                "meat_rose", "VintageBeef",
                new String[]{"Edible", "Flower", "Cow", "Chicken", "Pig", "Sheep"},
                new PlantStats(7, 0, 4, 1, 3, 0),
                4, 4, 4,
                new ItemStack[]{new ItemStack(Items.COOKED_PORKCHOP, 1)},
                new ItemStack[]{new ItemStack(Items.COOKED_BEEF, 1), new ItemStack(Items.COOKED_CHICKEN, 1), new ItemStack(Items.COOKED_MUTTON, 1)},
                1, 1500
        );
    }

    public static PlantGenericCrop milkWart() {
        return new PlantGenericCrop(
                "milk_wart", "Mr. Brain",
                new String[]{"Edible", "Milk", "Cow"},
                new PlantStats(6, 0, 3, 0, 1, 0),
                3, 3, 3,
                new ItemStack[]{new ItemStack(Items.MILK_BUCKET, 1)},
                null,
                1, 900
        );
    }

    public static PlantGenericCrop oilBerries() {
        return new PlantGenericCrop(
                "oil_berries", "Spacetoad",
                new String[]{"Fire", "Dark", "Reed", "Rotten", "Coal", "Oil"},
                new PlantStats(9, 6, 1, 2, 1, 12),
                3, 3, 3,
                new ItemStack[]{new ItemStack(Items.SLIME_BALL, 1)},
                null,
                1, 200
        );
    }

    public static PlantGenericCrop slimePlant() {
        return new PlantGenericCrop(
                "slime_plant", "Neowulf",
                new String[]{"Slime", "Bouncy", "Sticky", "Bush"},
                new PlantStats(6, 3, 0, 0, 0, 2),
                4, 4, 4,
                new ItemStack[]{new ItemStack(Items.SLIME_BALL, 1)},
                null,
                3, 200
        );
    }

    public static PlantGenericCrop spidernip() {
        return new PlantGenericCrop(
                "spidernip", "Mr. Kenny",
                new String[]{"Toxic", "Silk", "Spider", "Flower", "Ingredient", "Addictive"},
                new PlantStats(4, 2, 1, 4, 1, 3),
                4, 4, 4,
                new ItemStack[]{new ItemStack(Items.COBWEB, 1)},
                new ItemStack[]{new ItemStack(Items.SPIDER_EYE, 1), new ItemStack(Items.VINE, 1)},
                1, 600
        );
    }

    public static PlantGenericCrop tearstalks() {
        return new PlantGenericCrop(
                "tearstalks", "Neowulf",
                new String[]{"Healing", "Nether", "Ingredient", "Reed", "Ghast"},
                new PlantStats(8, 1, 2, 0, 0, 0),
                4, 4, 4,
                new ItemStack[]{new ItemStack(Items.GHAST_TEAR, 1)},
                null,
                1, 200
        );
    }

    public static PlantGenericCrop withereed() {
        return new PlantGenericCrop(
                "withereed", "CovertJaguar",
                new String[]{"Fire", "Undead", "Reed", "Coal", "Rotten", "Wither"},
                new PlantStats(8, 2, 0, 4, 1, 3),
                4, 4, 4,
                new ItemStack[]{new ItemStack(mio_icif_resources.COAL_DUST.get(), 1)},
                new ItemStack[]{new ItemStack(Items.WITHER_SKELETON_SKULL, 1), new ItemStack(Items.WITHER_SKELETON_SKULL, 1)},
                1, 200
        );
    }
}