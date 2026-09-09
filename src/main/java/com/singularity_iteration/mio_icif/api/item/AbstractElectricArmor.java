package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 电力装甲抽象基类，提供完整的默认能量存储实现。
 *
 * <p>Addon 开发者只需继承此类并指定构造参数即可创建一个功能完整的电力装甲，
 * 无需访问任何 mio_icif 内部类。
 *
 * <h3>能量存储机制</h3>
 * <p>使用 Minecraft 的耐久度系统来表示能量：
 * <ul>
 *   <li>耐久度条满（damageValue = 0）= 能量满</li>
 *   <li>耐久度条空（damageValue = maxEnergy）= 能量为 0</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * public class MyChestplate extends AbstractElectricArmor {
 *     public MyChestplate(Holder<ArmorMaterial> material, Properties properties) {
 *         super(material, Type.CHESTPLATE, properties,
 *               1_000_000,    // maxEnergy: 100万EU
 *               0,            // initialEnergy: 初始为空
 *               512,          // chargeRate: 充电速率 512 EU/t
 *               0,            // energyPerTick: 无持续消耗
 *               3,            // armorTier: 自定义等级3
 *               "my_mod/my_armor"  // texturePrefix: 材质路径
 *         );
 *     }
 *
 *     @Override
 *     public long getEnergyPerDamage() {
 *         return 10000; // 每点伤害消耗 10000 EU
 *     }
 *
 *     @Override
 *     public float getDamageAbsorptionRatio(EquipmentSlot slot) {
 *         return switch (slot) {
 *             case CHEST -> 0.40F;
 *             case HEAD, FEET -> 0.15F;
 *             case LEGS -> 0.30F;
 *             default -> 0.0F;
 *         };
 *     }
 *
 *     @Override
 *     public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
 *         return List.of(
 *             new ArmorFeatureInfo(getEquipmentSlot(), "my_feature", "tooltip.my_mod.my_feature")
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h3>自动提供的功能</h3>
 * <ul>
 *   <li>能量存储（基于耐久度系统）</li>
 *   <li>能量条渲染（绿→红渐变）</li>
 *   <li>能量 Tooltip 显示</li>
 *   <li>特性 Tooltip 显示（含开关/模式状态）</li>
 *   <li>充电器兼容（通过 IBatteryItem 接口）</li>
 *   <li>伤害吸收（通过 IElectricArmorItem 接口，由事件系统驱动）</li>
 *   <li>特性切换（通过 IItemAPI.toggleArmorFeature()）</li>
 *   <li>防止原版耐久消耗</li>
 * </ul>
 */
public abstract class AbstractElectricArmor extends ArmorItem implements IElectricArmorItem {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractElectricArmor.class);

    private final long maxEnergy;
    private final long energyPerTick;
    private final long chargeRate;
    private final int armorTier;
    private final String texturePrefix;

    // ==================== 构造函数 ====================

    /**
     * 创建电力装甲。
     *
     * @param material      装甲材料
     * @param type          装甲类型（头盔/胸甲/护腿/靴子）
     * @param properties    物品属性
     * @param maxEnergy     最大能量容量 (EU)
     * @param initialEnergy 初始能量 (EU)
     * @param chargeRate    充电速率 (EU/tick)
     * @param energyPerTick 每 tick 持续消耗 (EU)，0 表示无持续消耗
     * @param armorTier     装甲等级，用于伤害吸收规则判断
     * @param texturePrefix 材质路径前缀，格式为 "namespace/path"，
     *                      实际材质文件为 namespace:textures/armor/path_1.png 和 _2.png
     */
    protected AbstractElectricArmor(Holder<ArmorMaterial> material, Type type, Properties properties,
                                    long maxEnergy, long initialEnergy, long chargeRate,
                                    long energyPerTick, int armorTier, String texturePrefix) {
        super(material, type, buildArmorProperties(properties, maxEnergy, initialEnergy));
        this.maxEnergy = maxEnergy;
        this.chargeRate = chargeRate;
        this.energyPerTick = energyPerTick;
        this.armorTier = armorTier;
        this.texturePrefix = texturePrefix;
    }

    // ==================== 能量存储（基于耐久度系统） ====================

    private static Properties buildArmorProperties(Properties properties, long maxEnergy, long initialEnergy) {
        int durability = capToInt(maxEnergy, "armor durability");
        int damage = capToInt(Math.max(0, maxEnergy - initialEnergy), "armor initial damage");
        return properties
                .durability(durability)
                .component(DataComponents.DAMAGE, damage)
                .component(DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(false));
    }

    private static int capToInt(long value, String context) {
        if (value > Integer.MAX_VALUE) {
            LOGGER.warn("AbstractElectricArmor: {} value {} exceeds int limit, capping to {}. Armor durability is limited to int range.", context, value, Integer.MAX_VALUE);
            return Integer.MAX_VALUE;
        }
        if (value < 0) return 0;
        return (int) value;
    }

    // ==================== IBatteryItem 实现 ====================

    @Override
    public long getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public long getMaxEnergy(ItemStack stack) {
        return maxEnergy;
    }

    @Override
    public long getEnergy(ItemStack stack) {
        return maxEnergy - stack.getDamageValue();
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        energy = Math.max(0, Math.min(maxEnergy, energy));
        long damage = maxEnergy - energy;
        stack.setDamageValue(capToInt(damage, "setEnergy"));
    }

    @Override
    public long addEnergy(ItemStack stack, long amount) {
        long currentEnergy = getEnergy(stack);
        long newEnergy = Math.min(maxEnergy, currentEnergy + amount);
        long addedEnergy = newEnergy - currentEnergy;
        setEnergy(stack, newEnergy);
        return addedEnergy;
    }

    @Override
    public long extractEnergy(ItemStack stack, long amount) {
        long currentEnergy = getEnergy(stack);
        long newEnergy = Math.max(0, currentEnergy - amount);
        long extractedEnergy = currentEnergy - newEnergy;
        setEnergy(stack, newEnergy);
        return extractedEnergy;
    }

    @Override
    public boolean isFull(ItemStack stack) {
        return getEnergy(stack) >= maxEnergy;
    }

    @Override
    public boolean isEmpty(ItemStack stack) {
        return getEnergy(stack) <= 0;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    // ==================== IElectricArmorItem 实现 ====================

    @Override
    public long getEnergyPerTick() {
        return energyPerTick;
    }

    @Override
    public int getArmorTier() {
        return armorTier;
    }

    @Override
    public boolean hasEnoughEnergy(ItemStack stack) {
        return getEnergy(stack) >= energyPerTick;
    }

    @Override
    public boolean consumeEnergy(ItemStack stack, long amount) {
        if (getEnergy(stack) >= amount) {
            extractEnergy(stack, amount);
            return true;
        }
        return false;
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return Collections.emptyList();
    }

    @Override
    public long getEnergyPerDamage() {
        return 0;
    }

    @Override
    public float getDamageAbsorptionRatio(EquipmentSlot slot) {
        return 0.0F;
    }

    // ==================== 能量条渲染 ====================

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long currentEnergy = getEnergy(stack);
        if (maxEnergy <= 0) return 0;
        return (int) Math.round(13.0 * currentEnergy / maxEnergy);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        long currentEnergy = getEnergy(stack);
        float ratio = maxEnergy <= 0 ? 0.0F : (float) currentEnergy / maxEnergy;
        int r = Math.round(255 * (1.0F - ratio));
        int g = Math.round(255 * ratio);
        return r << 16 | g << 8;
    }

    // ==================== 防止原版耐久消耗 ====================

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        return 0;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        return false;
    }

    // ==================== Tooltip ====================

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        appendEnergyTooltip(stack, tooltipComponents);
        appendFeatureTooltip(stack, tooltipComponents);
    }

    /**
     * 追加能量信息到 Tooltip。
     * 子类可以覆盖此方法来自定义能量显示格式。
     *
     * @param stack             物品堆
     * @param tooltipComponents Tooltip 列表
     */
    protected void appendEnergyTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(
            Component.translatable("tooltip.mio_icif.armor.energy", getEnergy(stack), maxEnergy)
                .withStyle(ChatFormatting.GRAY)
        );
    }

    /**
     * 追加特性信息到 Tooltip。
     * 子类可以覆盖此方法来自定义特性显示格式。
     *
     * @param stack             物品堆
     * @param tooltipComponents Tooltip 列表
     */
    protected void appendFeatureTooltip(ItemStack stack, List<Component> tooltipComponents) {
        List<ArmorFeatureInfo> features = getFeatures(stack);
        for (ArmorFeatureInfo feature : features) {
            if (feature.isMode()) {
                addModeLine(tooltipComponents, stack, feature);
            } else {
                addToggleLine(tooltipComponents, stack, feature);
            }
        }
    }

    private void addToggleLine(List<Component> tooltip, ItemStack stack, ArmorFeatureInfo feature) {
        boolean enabled = isFeatureEnabled(stack, feature.featureKey());
        ChatFormatting statusColor = enabled ? ChatFormatting.GREEN : ChatFormatting.RED;
        String statusKey = enabled ? "tooltip.mio_icif.armor.feature_on" : "tooltip.mio_icif.armor.feature_off";

        MutableComponent statusText = Component.translatable(statusKey).withStyle(statusColor);
        MutableComponent featureText = Component.translatable(feature.featureNameKey()).withStyle(ChatFormatting.YELLOW);

        tooltip.add(featureText.append(Component.literal(": ")).append(statusText));
    }

    private void addModeLine(List<Component> tooltip, ItemStack stack, ArmorFeatureInfo feature) {
        ChatFormatting statusColor = ChatFormatting.AQUA;
        MutableComponent modeName = feature.currentModeName() != null
                ? feature.currentModeName().copy()
                : Component.translatable(feature.featureNameKey() + ".default");
        modeName.withStyle(statusColor);

        MutableComponent featureText = Component.translatable(feature.featureNameKey()).withStyle(ChatFormatting.YELLOW);
        tooltip.add(featureText.append(Component.literal(": ")).append(modeName));
    }

    /**
     * 检查特性是否启用。
     * 默认实现使用 CUSTOM_DATA 存储，键格式为 "feat_{featureKey}"。
     * 子类可以覆盖此方法以使用不同的存储方式。
     *
     * @param stack      物品堆
     * @param featureKey 特性键
     * @return 是否启用
     */
    protected boolean isFeatureEnabled(ItemStack stack, String featureKey) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        return customData.copyTag().getBoolean("feat_" + featureKey);
    }

    // ==================== 材质 ====================

    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        String suffix = (slot == EquipmentSlot.LEGS) ? "_2" : "_1";
        int slashIndex = texturePrefix.indexOf('/');
        String namespace = slashIndex > 0 ? texturePrefix.substring(0, slashIndex) : "minecraft";
        String path = slashIndex > 0 ? texturePrefix.substring(slashIndex + 1) : texturePrefix;
        return ResourceLocation.fromNamespaceAndPath(namespace, "textures/armor/" + path + suffix + ".png");
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取材质路径前缀
     */
    public String getTexturePrefix() {
        return texturePrefix;
    }

    /**
     * 检查玩家是否正在穿戴此装甲
     */
    public boolean isWearingThisArmor(Player player, EquipmentSlot slot) {
        ItemStack equipped = player.getItemBySlot(slot);
        return equipped.getItem() == this;
    }

    /**
     * 检查玩家是否穿齐了同系列装甲。
     * 同系列定义为具有相同 texturePrefix 的 AbstractElectricArmor 子类。
     */
    public boolean isWearingFullSet(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!isSameArmorSeries(stack)) return false;
        }
        return true;
    }

    private boolean isSameArmorSeries(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof AbstractElectricArmor other)) return false;
        return other.getTexturePrefix().equals(this.texturePrefix);
    }

    /**
     * 获取能量剩余百分比 (0.0 ~ 1.0)
     */
    public float getDurabilityPercent(ItemStack stack) {
        return maxEnergy <= 0 ? 0.0F : (float) getEnergy(stack) / maxEnergy;
    }

    /**
     * 获取剩余可运行 tick 数
     */
    public long getRemainingTicks(ItemStack stack) {
        return energyPerTick <= 0 ? 0 : getEnergy(stack) / energyPerTick;
    }

    /**
     * 检查是否有足够能量使用
     */
    public boolean canUse(ItemStack stack) {
        return !isEmpty(stack);
    }
}