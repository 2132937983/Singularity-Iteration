// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.runtime.crop.CropGrowthMath;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * <p><b>EN:</b> Server-side reading of one crop stick for the Advanced Crop Scanner HUD (0.1.8.2): plant,
 * stage, traits, environment, stored resources, the environment quality against the requirement
 * (with the Cropmatron tending bonus), and the first growth condition that is not met.
 * <p><b>ZH:</b> 次世代作物扫描仪 HUD 在服务器端读取一个作物架的结果（0.1.8.2）：植物、阶段、属性、环境、储存资源、环境品质与需求的比较
 * （含作物监护机照料加成），以及第一个未满足的生长条件。
 * <p><b>JA:</b> 次世代型作物スキャナー HUD のための、作物棒 1 本のサーバー側の読み取り（0.1.8.2）：植物、段階、特性値、環境、貯蔵資源、
 * 要求に対する環境品質（作物管理機の管理ボーナス込み）、満たしていない最初の成長条件。
 */
public final class CropDiagnosis {
    private CropDiagnosis() {}

    public static final int F_HYBRID = 1, F_TENDED = 2, F_CAN_GROW = 4, F_MATURE = 8, F_HARVESTABLE = 16, F_WEED = 32;
    /** EN: 0.1.10.38: the server uses the strict IC2 crop rules. ZH: 0.1.10.38：服务器采用严格 IC2 作物规则。JA: 0.1.10.38：サーバーは厳密な IC2 作物ルールを使う。 */
    public static final int F_STRICT = 64;
    /** EN: Growth blockers. ZH: 生长阻碍。JA: 成長の妨げ。 */
    public static final int B_NONE = 0, B_LIGHT_LOW = 1, B_LIGHT_HIGH = 2, B_ROOT = 3, B_WATER = 4, B_NUTRIENTS = 5, B_OTHER = 6;

    /**
     * <p><b>EN:</b> One reading. {@code typeId} is empty for an empty stick. {@code detail} is a block
     * description id for {@link #B_ROOT}; else empty.
     * <p><b>ZH:</b> 一次读数。空作物架的 {@code typeId} 为空。{@link #B_ROOT} 时 {@code detail} 为方块描述 ID，否则为空。
     * <p><b>JA:</b> 1 回の読み取り。空の作物棒では {@code typeId} は空。{@link #B_ROOT} のとき {@code detail} はブロックの説明 ID、それ以外は空。
     */
    public record Snapshot(String modId, String typeId, int stage, int maxStage, int harvestStage, int progress, int growthTime,
                           int growth, int gain, int resistance, int humidity, int soil, int air, int light,
                           int water, int nutrients, int weedEx, int required, int quality, int flags,
                           int blocker, int blockerValue, String detail) {
        public boolean has(int flag) { return (flags & flag) != 0; }
        public boolean empty() { return typeId.isEmpty(); }
        /** EN: Growth speed in percent of the base (100 = no bonus, no penalty). ZH: 生长速度占基础值的百分比（100 = 无加成无惩罚）。JA: 基礎に対する成長速度の百分率（100 = ボーナスもペナルティもなし）。 */
        public int growthRate() { return CropGrowthMath.growthPoints(100, required, quality, has(F_STRICT)); }
        public int penalty() { return CropGrowthMath.penalty(required, quality, has(F_STRICT)); }

        public void write(FriendlyByteBuf b) {
            b.writeUtf(modId, 64); b.writeUtf(typeId, 128);
            b.writeVarInt(stage); b.writeVarInt(maxStage); b.writeVarInt(harvestStage); b.writeVarInt(progress); b.writeVarInt(growthTime);
            b.writeByte(growth); b.writeByte(gain); b.writeByte(resistance);
            b.writeInt(humidity); b.writeInt(soil); b.writeInt(air); b.writeByte(light);
            b.writeVarInt(water); b.writeVarInt(nutrients); b.writeVarInt(weedEx); b.writeVarInt(required); b.writeVarInt(quality);
            b.writeVarInt(flags); b.writeByte(blocker); b.writeVarInt(blockerValue); b.writeUtf(detail, 256);
        }

        public static Snapshot read(FriendlyByteBuf b) {
            return new Snapshot(b.readUtf(64), b.readUtf(128), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(),
                b.readUnsignedByte(), b.readUnsignedByte(), b.readUnsignedByte(), b.readInt(), b.readInt(), b.readInt(), b.readUnsignedByte(),
                b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readUnsignedByte(),
                b.readVarInt(), b.readUtf(256));
        }
    }

    /** EN: Reads the planter (server thread). ZH: 读取作物架（服务器线程）。JA: 作物棒を読む（サーバースレッド）。 */
    public static Snapshot of(IPlanter p) {
        PlantType plant = p.getPlant();
        int flags = (p.isHybridBase() ? F_HYBRID : 0) | (p.isTended() ? F_TENDED : 0) | (CropGrowthMath.strictIc2Rules() ? F_STRICT : 0);
        int humidity = p.getHumidity(), soil = p.getSoilNutrients(), air = p.getAirQuality(), light = p.getLightLevel();
        if (plant == null) {
            return new Snapshot("", "", 0, 0, 0, 0, 0, 0, 0, 0, humidity, soil, air, light, p.getWater(), p.getNutrients(), p.getWeedControl(),
                0, 0, flags, B_NONE, 0, "");
        }
        int stage = p.getGrowthStage(), max = plant.getMaxGrowthStage();
        int required = CropGrowthMath.requiredQuality(plant.getStats().getLevel(), p.getGrowthSpeed(), p.getYield(), p.getResilience());
        int quality = plant.weightInfluences(p, humidity, soil, air) * 5;
        if (p.isTended()) quality = CropGrowthMath.tendedQuality(quality, required);
        boolean canGrow = plant.canGrow(p);
        if (canGrow) flags |= F_CAN_GROW;
        if (stage >= max) flags |= F_MATURE;
        if (plant.isHarvestable(p)) flags |= F_HARVESTABLE;
        if (plant.isWeed(p)) flags |= F_WEED;
        int blocker = B_NONE, value = 0;
        String detail = "";
        if (!canGrow && stage < max) {
            int[] lightRange = lightRange(plant);
            if (lightRange != null && light < lightRange[0]) { blocker = B_LIGHT_LOW; value = lightRange[0]; }
            else if (lightRange != null && light > lightRange[1]) { blocker = B_LIGHT_HIGH; value = lightRange[1]; }
            else if ((detail = rootDetail(plant, stage, max)) != null) blocker = B_ROOT;
            else if (isMushroom(plant) && p.getWater() <= 0) blocker = B_WATER;
            else if ("cocoa".equals(plant.getTypeId()) && p.getNutrients() < 3) { blocker = B_NUTRIENTS; value = 3; }
            else blocker = B_OTHER;
            if (detail == null) detail = "";
        }
        return new Snapshot(plant.getModId(), plant.getTypeId(), stage, max, plant.getHarvestStage(), p.getProgress(),
            Math.max(1, plant.getGrowthTime(p)), p.getGrowthSpeed(), p.getYield(), p.getResilience(), humidity, soil, air, light,
            p.getWater(), p.getNutrients(), p.getWeedControl(), required, quality, flags, blocker, value, detail);
    }

    /**
     * 0.1.10.39: why {@code plant} cannot grow on {@code p} now, as the crop scanner says it: light too low or too high, or another
     * condition. Shown when a seed bag is refused.
     */
    public static Component cannotGrowReason(PlantType plant, IPlanter p) {
        int light = p.getLightLevel();
        int[] range = lightRange(plant);
        if (range != null && light < range[0]) return Component.translatable("hud.mio_icif.crop_scanner.light_low", light, range[0]);
        if (range != null && light > range[1]) return Component.translatable("hud.mio_icif.crop_scanner.light_high", light, range[1]);
        return Component.translatable("hud.mio_icif.crop_scanner.other");
    }

    /** EN: Light range {min, max} that the plant needs, or null for no light rule. ZH: 植物所需的光照范围 {最小, 最大}，无光照规则时为 null。JA: 植物が必要とする明るさの範囲 {最小, 最大}。明るさの規則がなければ null。 */
    static int[] lightRange(PlantType plant) {
        if (!"mio_icif".equals(plant.getModId())) return null;
        if (plant instanceof PlantBaseSapling) return new int[]{9, 15};
        return switch (plant.getTypeId()) {
            case "blackthorn", "cyazint", "dandelion", "rose", "tulip", "venomilia" -> new int[]{12, 15};
            case "wheat", "carrots", "beetroot", "potato", "coffee", "hops", "flax", "pumpkin", "melon" -> new int[]{9, 15};
            case "redwheat" -> new int[]{5, 10};
            case "eatingplant" -> new int[]{11, 15};
            default -> null;
        };
    }

    private static boolean isMushroom(PlantType plant) {
        return "mio_icif".equals(plant.getModId()) && ("brownMushroom".equals(plant.getTypeId()) || "redMushroom".equals(plant.getTypeId()));
    }

    /** EN: Description id of a root block that the next stage needs, or null. ZH: 下一阶段所需根部方块的描述 ID，不需要时为 null。JA: 次の段階に要る根のブロックの説明 ID。不要なら null。 */
    private static String rootDetail(PlantType plant, int stage, int max) {
        if (plant instanceof OreRootCrop ore && stage >= max - 1) {
            Block[] roots = ore.rootBlocks();
            return roots.length == 0 ? "" : roots[0].getDescriptionId();
        }
        if (!"mio_icif".equals(plant.getModId())) return null;
        if ("eatingplant".equals(plant.getTypeId()) && stage >= 3) return Blocks.LAVA.getDescriptionId();
        String metal = switch (plant.getTypeId()) {
            case "ferru" -> "iron"; case "cyprium" -> "copper"; case "stagnium" -> "tin";
            case "plumbiscus" -> "lead"; case "aurelia" -> "gold"; case "shining" -> "silver";
            default -> null;
        };
        if (metal == null || stage < max - 1) return null;
        TagKey<Block> tag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/" + metal));
        return BuiltInRegistries.BLOCK.getTag(tag).flatMap(set -> set.stream().findFirst())
            .map(holder -> holder.value().getDescriptionId()).orElse("#c:ores/" + metal);
    }
}
