// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.prefab.crop.Ic2PlantType;
import com.miophas.singularity_iteration.core.runtime.crop.PlantRegistry;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Shared policies for the original IC2 species; identity and texture IDs stay in the catalog classes. */
public abstract class BuiltinCrop extends Ic2PlantType {
    private String metal() {
        return switch (getTypeId()) {
            case "ferru" -> "iron"; case "cyprium" -> "copper"; case "stagnium" -> "tin";
            case "plumbiscus" -> "lead"; case "aurelia" -> "gold"; case "shining" -> "silver";
            default -> null;
        };
    }
    private boolean flower() {
        return switch (getTypeId()) { case "blackthorn", "cyazint", "dandelion", "rose", "tulip" -> true; default -> false; };
    }
    @Override public boolean canGrow(IPlanter p) {
        int stage = p.getGrowthStage();
        if (stage >= getMaxGrowthStage()) return false;
        if (metal() != null) return stage < getMaxGrowthStage() - 1
            || p.isBlockBelow("c:ores/" + metal()) || p.isBlockBelow("c:storage_blocks/" + metal());
        if (flower()) return p.getLightLevel() >= 12;
        return switch (getTypeId()) {
            case "wheat", "carrots", "beetroot", "potato", "coffee", "hops", "flax", "pumpkin", "melon" -> p.getLightLevel() >= 9;
            case "cocoa" -> p.getNutrients() >= 3;
            case "brownMushroom", "redMushroom" -> p.getWater() > 0;
            case "redwheat" -> p.getLightLevel() >= 5 && p.getLightLevel() <= 10;
            case "venomilia" -> stage == 5 || p.getLightLevel() >= 12;
            case "eatingplant" -> p.getLightLevel() > 10 && (stage < 3 || p.isBlockBelow(Blocks.LAVA));
            default -> true;
        };
    }
    @Override public int getRootDepth(IPlanter p) {
        return metal() != null || getTypeId().equals("netherwart") || getTypeId().equals("terrawart")
            || getTypeId().equals("eatingplant") ? 5 : super.getRootDepth(p);
    }
    @Override public int getHarvestStage() {
        return switch (getTypeId()) {
            case "reed", "stickreed" -> 2; case "coffee", "venomilia", "eatingplant" -> 4;
            case "potato" -> 3; default -> getMaxGrowthStage();
        };
    }
    @Override public int getOptimalHarvestStage() {
        return switch (getTypeId()) { case "potato" -> 3; case "venomilia", "eatingplant" -> 4;
            case "weed" -> 1; default -> getMaxGrowthStage(); };
    }
    @Override public boolean canBeHarvested(IPlanter p) {
        if (getTypeId().equals("weed")) return false;
        if (getTypeId().equals("eatingplant")) return p.getGrowthStage() >= 4 && p.getGrowthStage() < 6;
        return p.getGrowthStage() >= getHarvestStage();
    }
    @Override public int getStageAfterHarvest() {
        if (metal() != null) return 2;
        if (flower()) return 3;
        return switch (getTypeId()) {
            case "wheat", "redwheat" -> 2;
            case "coffee", "hops", "cocoa", "venomilia", "pumpkin", "melon" -> 3;
            default -> 1;
        };
    }
    @Override public int getStageAfterHarvest(IPlanter p) {
        return getTypeId().equals("stickreed") && p.getGrowthStage() == 4
            ? 1 + p.getPlanterWorld().random.nextInt(3) : getStageAfterHarvest();
    }
    @Override public int getGrowthTime(IPlanter p) {
        int stage = p.getGrowthStage();
        if (metal() != null) return getMaxGrowthStage() == 4 ? (stage == 3 ? 2000 : 800) : (stage == 4 ? 2200 : 750);
        if (flower()) return stage == 3 ? 600 : 400;
        return switch (getTypeId()) {
            case "coffee" -> stage == 3 ? 700 : stage == 4 ? 2100 : 1400;
            case "hops", "redwheat" -> 600;
            case "cocoa" -> stage == 3 ? 900 : 400;
            case "reed", "brownMushroom", "redMushroom" -> 200;
            case "stickreed" -> stage == 4 ? 400 : 100;
            case "weed" -> 300;
            case "pumpkin" -> stage == 3 ? 600 : 200;
            case "melon" -> stage == 3 ? 700 : 250;
            case "venomilia" -> stage >= 3 ? 600 : 400;
            case "eatingplant" -> {
                var biome = p.getPlanterWorld().getBiome(p.getPlanterPos());
                float multiplier = CropEnvironment.has(biome, "c:is_swamp") || CropEnvironment.has(biome, "minecraft:is_mountain") ? 1f / 1.5f : 1;
                yield (int) (1200 * multiplier / (1 + p.getAirQuality() / 10f));
            }
            default -> super.getGrowthTime(p);
        };
    }
    @Override public int weightInfluences(IPlanter p, int h, int n, int a) {
        return switch (getTypeId()) {
            case "coffee" -> (int) (0.4 * h + 1.4 * n + 1.2 * a);
            case "cocoa" -> (int) (0.8 * h + 1.3 * n + 0.9 * a);
            case "reed", "stickreed" -> (int) (1.2 * h + n + 0.8 * a);
            case "pumpkin", "melon" -> (int) (1.1 * h + 0.9 * n + a);
            default -> super.weightInfluences(p, h, n, a);
        };
    }
    @Override public double dropGainChance() {
        if (metal() != null && getMaxGrowthStage() == 4) return super.dropGainChance() / 2;
        return switch (getTypeId()) { case "redwheat" -> 0.5; case "netherwart" -> 2; case "terrawart" -> 0.8; default -> super.dropGainChance(); };
    }
    @Override public ItemStack[] getHarvest(IPlanter p) {
        var random = p.getPlanterWorld().random;
        int stage = p.getGrowthStage();
        Item item = switch (getTypeId()) {
            case "wheat" -> Items.WHEAT; case "carrots" -> Items.CARROT; case "beetroot" -> Items.BEETROOT;
            case "potato" -> stage >= 4 && random.nextInt(20) == 0 ? Items.POISONOUS_POTATO : Items.POTATO;
            case "pumpkin" -> Items.PUMPKIN;
            case "melon" -> random.nextInt(3) == 0 ? Items.MELON : Items.MELON_SLICE;
            case "coffee" -> stage == 5 ? mio_icif_resources.COFFEE_BEAN.get() : Items.AIR;
            case "hops" -> mio_icif_normal.HOPS.get(); case "cocoa" -> Items.COCOA_BEANS;
            case "flax" -> Items.STRING;
            case "reed" -> Items.SUGAR_CANE;
            case "stickreed" -> stage == 4 ? mio_icif_resources.HARZ.get() : Items.SUGAR_CANE;
            case "blackthorn" -> Items.BLACK_DYE; case "cyazint" -> Items.CYAN_DYE;
            case "dandelion" -> Items.YELLOW_DYE; case "rose" -> Items.RED_DYE; case "tulip" -> Items.PURPLE_DYE;
            case "brownMushroom" -> Items.BROWN_MUSHROOM; case "redMushroom" -> Items.RED_MUSHROOM;
            case "ferru" -> mio_icif_resources.IRON_DUST_SMALL.get(); case "cyprium" -> mio_icif_resources.COPPER_DUST_SMALL.get();
            case "stagnium" -> mio_icif_resources.TIN_DUST_SMALL.get(); case "plumbiscus" -> mio_icif_resources.LEAD_DUST_SMALL.get();
            case "aurelia" -> mio_icif_resources.GOLDEN_DUST_SMALL.get(); case "shining" -> mio_icif_resources.SILVER_DUST_SMALL.get();
            case "redwheat" -> p.getPlanterWorld().hasNeighborSignal(p.getPlanterPos()) || random.nextBoolean() ? Items.REDSTONE : Items.WHEAT;
            case "venomilia" -> stage == 5 ? mio_icif_resources.GRIN_DUST.get() : Items.PURPLE_DYE;
            case "netherwart" -> Items.NETHER_WART; case "terrawart" -> mio_icif_resources.TERRA_WART.get();
            case "eatingplant" -> Items.CACTUS;
            case "weed" -> Items.AIR;
            default -> throw new IllegalStateException("Unknown built-in crop " + getTypeId());
        };
        if (item == Items.AIR) return new ItemStack[0];
        int count = item == Items.SUGAR_CANE ? stage - 1 : item == Items.MELON_SLICE ? 2 + random.nextInt(4) : 1;
        return new ItemStack[]{new ItemStack(item, count)};
    }
    @Override public ItemStack getSeedItem(IPlanter p) {
        if (p.getGrowthSpeed() <= 1 && p.getYield() <= 1 && p.getResilience() <= 1) {
            Item seed = switch (getTypeId()) {
                case "wheat" -> Items.WHEAT_SEEDS; case "beetroot" -> Items.BEETROOT_SEEDS;
                case "carrots" -> Items.CARROT; case "potato" -> Items.POTATO;
                case "pumpkin" -> Items.PUMPKIN_SEEDS; case "melon" -> Items.MELON_SEEDS;
                default -> Items.AIR;
            };
            if (seed != Items.AIR) return new ItemStack(seed, getTypeId().equals("pumpkin") ? 1 + p.getPlanterWorld().random.nextInt(3)
                : getTypeId().equals("melon") ? 1 + p.getPlanterWorld().random.nextInt(2) : 1);
        }
        return super.getSeedItem(p);
    }
    @Override public void tick(IPlanter p) {
        boolean nether = getTypeId().equals("netherwart");
        if (nether || getTypeId().equals("terrawart")) {
            if (p.isBlockBelow(nether ? Blocks.SOUL_SAND : Blocks.SNOW_BLOCK)) {
                if (canGrow(p)) p.setProgress(p.getProgress() + 100);
            }
            else if (p.isBlockBelow(nether ? Blocks.SNOW_BLOCK : Blocks.SOUL_SAND) && p.getPlanterWorld().random.nextInt(300) == 0)
                p.setPlant(PlantRegistry.instance.getPlant(getModId(), nether ? "terrawart" : "netherwart"));
        } else if (getTypeId().equals("eatingplant")) EatingPlantBehavior.tick(p, this);
    }
    @Override public boolean isWeed(IPlanter p) {
        return getTypeId().equals("venomilia") ? p.getGrowthStage() == 5 && p.getGrowthSpeed() >= 8 : super.isWeed(p);
    }
    @Override public int getRedstoneSignal(IPlanter p) { return getTypeId().equals("redwheat") && p.getGrowthStage() == 7 ? 15 : 0; }
    @Override public int getLightEmission(IPlanter p) { return getRedstoneSignal(p) > 0 ? 7 : 0; }
    @Override public boolean onCollision(IPlanter p, Entity entity) {
        if (getTypeId().equals("weed") || getTypeId().equals("reed") || getTypeId().equals("stickreed")) return false;
        if (getTypeId().equals("venomilia") && p.getGrowthStage() == 5 && entity instanceof LivingEntity living) {
            if (!(entity instanceof Player player && player.isShiftKeyDown() && p.getPlanterWorld().random.nextInt(50) != 0)) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, (5 + p.getPlanterWorld().random.nextInt(10)) * 20));
                p.setGrowthStage(4); p.updateState();
            }
        }
        return super.onCollision(p, entity);
    }
    @Override public boolean onInteract(IPlanter p, Player player) {
        if (getTypeId().equals("venomilia") && !player.isShiftKeyDown()) onCollision(p, player);
        return super.onInteract(p, player);
    }
    @Override public boolean onLeftClick(IPlanter p, Player player) {
        if (getTypeId().equals("weed")) return false;
        if (getTypeId().equals("venomilia") && !player.isShiftKeyDown()) onCollision(p, player);
        return super.onLeftClick(p, player);
    }
}
