// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.armor;

import com.miophas.singularity_iteration.core.api.item.ISolarHelmetItem;
import com.miophas.singularity_iteration.core.runtime.energy.SolarHelmetCharging;
import com.miophas.singularity_iteration.core.runtime.energy.engine.SolarOutputModel;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

/**
 * IC2 1.12.2 {@code ItemArmorSolarHelmet} 的对应重构。
 *
 * <ul>
 *   <li>自身保留电池缓冲（SI 架构），带能量条</li>
 *   <li>用 {@code TileEntitySolarGenerator.getSkyLight()} 的光照模型产电（原版天空光照 × 太阳高度角 × 天气衰减）</li>
 *   <li>每 tick 只把电导向胸甲槽，充不完的留在自身缓冲里</li>
 *   <li>沙漠群系免疫雨天/雷暴衰减</li>
 *   <li>自身 0 防御</li>
 * </ul>
 */
@SuppressWarnings({"null", "deprecation"})
public class mio_icif_solar_helmet extends mio_icif_armor_elc implements ISolarHelmetItem {
    public static final int SOLAR_HELMET_MAX_ENERGY = 8000;
    /** IC2 getSkyLight 的峰值约 1 EU/t（晴日正午无遮挡），也用作每 tick 导给胸甲的上限。 */
    public static final int SOLAR_CHARGE_PER_TICK = 1;

    private static final TagKey<Biome> SANDY_BIOMES =
        TagKey.create(Registries.BIOME, ResourceLocation.parse("c:sandy"));

    public mio_icif_solar_helmet(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties, SOLAR_HELMET_MAX_ENERGY, 0,
            "solar", SOLAR_CHARGE_PER_TICK, 0, 1);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (level.isClientSide || !(entity instanceof Player player)
                || player.getItemBySlot(EquipmentSlot.HEAD) != stack) return;
        // IC2 1.12.2: charge the chest slot with TileEntitySolarGenerator.getSkyLight(), evaluated at the player
        // position. Sandy biomes skip the rain/thunder reduction.
        BlockPos pos = player.blockPosition();
        boolean sandy = level.getBiome(pos).is(SANDY_BIOMES);
        double generation = SolarOutputModel.rate(level.getDayTime(),
            level.getBrightness(LightLayer.SKY, pos),
            sandy ? 0.0F : level.getRainLevel(1.0F),
            sandy ? 0.0F : level.getThunderLevel(1.0F));
        SolarHelmetCharging.tickChestOnly(stack, this, level, player, generation, SOLAR_CHARGE_PER_TICK);
    }

    @Override
    public ItemStack getDefaultInstance() {
        return new ItemStack(this);
    }

    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
            ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/armor/solar_1.png");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.solar_helmet.solar", SOLAR_CHARGE_PER_TICK)
            .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.mio_icif.solar_helmet.distribute")
            .withStyle(ChatFormatting.GRAY));
    }

    @Override public long getGenerationRate() { return SOLAR_CHARGE_PER_TICK; }
    @Override public boolean requiresSky() { return true; }
    @Override public boolean isDayOnly() { return true; }
}
