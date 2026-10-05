package com.miophas.singularity_iteration.common.item.armor;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import com.miophas.singularity_iteration.core.api.item.ISolarHelmetItem;

import java.util.List;

/**
 * 高级太阳能头盔 (Advanced Solar Helmet)
 * 参考 ASP 配置:
 * - 白天发电: 8 EU/t
 * - 夜晚发电: 1 EU/t
 * - 能量存储: 1,000,000 EU
 * - 传输限制: 3000 EU/t
 * - Tier: 3
 * - 伤害吸收: 90%
 */
@SuppressWarnings({"null", "deprecation"})
public class mio_icif_advanced_solar_helmet extends mio_icif_armor_elc implements ISolarHelmetItem {

    // AbstractElectricArmor and the SI item API store and transfer EU directly.
    public static final int MAX_ENERGY = 1_000_000;
    public static final int DAY_GENERATION = 8;
    public static final int NIGHT_GENERATION = 1;
    public static final int TRANSFER_LIMIT = 3000;
    public static final int TIER = 3;
    public static final double DAMAGE_ABSORPTION = 0.9;

    /**
     * 构造函数
     * @param material 护甲材料
     * @param properties 物品属性
     */
    public mio_icif_advanced_solar_helmet(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties, MAX_ENERGY, 0, "advanced_solar_helmet", TRANSFER_LIMIT, 0, TIER);
    }

    /**
     * 检查当前光照下是否可以发电
     */
    private boolean canGenerate(Level level, Entity entity) {
        if (!(entity instanceof Player)) {
            return false;
        }

        // Sky exposure is independent of time-of-day: night still has a configured
        // low-output mode, while a roof must disable both day and night output.
        var headPos = net.minecraft.core.BlockPos.containing(entity.position().x, entity.position().y + 1, entity.position().z);
        return level.canSeeSky(headPos);
    }

    /**
     * 检查是否是白天
     */
    private boolean isDay(Level level) {
        long timeOfDay = level.getDayTime() % 24000;
        return timeOfDay >= 0 && timeOfDay < 12000;
    }

    /**
     * 获取当前发电量
     */
    private int getGenerationRate(Level level, Entity entity) {
        if (!canGenerate(level, entity)) {
            return 0;
        }

        // 检查天气
        var headPos = net.minecraft.core.BlockPos.containing(entity.position().x, entity.position().y + 1, entity.position().z);
        boolean isRaining = level.isRainingAt(headPos) || level.isThundering();

        if (isDay(level) && !isRaining) {
            return DAY_GENERATION;
        } else {
            return NIGHT_GENERATION;
        }
    }

    /**
     * 护甲 tick 更新
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        // 检查玩家是否穿着这件头盔
        if (player.getItemBySlot(EquipmentSlot.HEAD) != stack) {
            return;
        }

        // ASP solar helmets charge every worn/carried piece of equipment, not just the armor slots.
        com.miophas.singularity_iteration.core.runtime.energy.SolarHelmetCharging.tick(stack, this, level, player,
            getGenerationRate(level, entity), TRANSFER_LIMIT);
        // crafted from a nano helmet: keeps its night vision trait
        if (ArmorFeatureToggle.isEnabled(stack, "night_vision")
                && consumeEnergy(stack, mio_icif_helmet_nano.NIGHT_VISION_ENERGY_COST)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,
                mio_icif_helmet_nano.NIGHT_VISION_DURATION, 0, false, false, false));
        }
    }

    @Override
    public List<com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(new com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo(EquipmentSlot.HEAD, "night_vision", "tooltip.mio_icif.armor.feature_night_vision"));
    }







    /**
     * 获取护甲纹理
     */
    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/armor/advanced_solar_helmet_1.png");
    }

    /**
     * 获取默认实例（满电状态）
     */
    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        setEnergy(stack, MAX_ENERGY);
        return stack;
    }

    /**
     * 添加 tooltip 显示
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.translatable("tooltip.mio_icif.advanced_solar_helmet.day", DAY_GENERATION)
                .withStyle(net.minecraft.ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.mio_icif.advanced_solar_helmet.night", NIGHT_GENERATION)
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.mio_icif.advanced_solar_helmet.transfer", TRANSFER_LIMIT)
                .withStyle(net.minecraft.ChatFormatting.GREEN));
    }

    /**
     * 获取每点伤害消耗的能量
     */
    @Override
    public long getEnergyPerDamage() {
        return (int) (2000 * DAMAGE_ABSORPTION);
    }

    @Override
    public float getDamageAbsorptionRatio(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD ? (float) DAMAGE_ABSORPTION : 0.0F;
    }

    // ==================== ISolarHelmetItem API ====================

    @Override
    public long getGenerationRate() {
        return DAY_GENERATION;
    }

    @Override
    public boolean requiresSky() {
        return true;
    }

    @Override
    public boolean isDayOnly() {
        return false;
    }
}
