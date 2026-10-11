package com.miophas.singularity_iteration.core.prefab.item;

import com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo;
import com.miophas.singularity_iteration.core.api.item.BatteryEnergy;
import com.miophas.singularity_iteration.core.api.item.IElectricArmorItem;
import com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Abstract base class for electric armor. It gives a full default energy storage implementation.
 *
 * <p>An addon developer only extends this class and gives the constructor arguments to create a fully functional electric armor piece.
 * No access to mio_icif internal classes is necessary.
 *
 * <h3>Energy storage</h3>
 * <p>Energy is always stored in the DataComponent {@code mio_icif:battery_energy}
 * ({@link BatteryEnergy}, with the two fields energy and maxEnergy). It does not use the durability system.
 * Energy that old saves stored as durability moves to the component automatically on the first read.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public class MyChestplate extends AbstractElectricArmor {
 *     public MyChestplate(Holder<ArmorMaterial> material, Properties properties) {
 *         super(material, Type.CHESTPLATE, properties,
 *               1_000_000,    // maxEnergy: 1,000,000 EU
 *               0,            // initialEnergy: starts empty
 *               512,          // chargeRate: charge rate 512 EU/t
 *               0,            // energyPerTick: no continuous use
 *               3,            // armorTier: custom tier 3
 *               "my_mod/my_armor"  // texturePrefix: texture path
 *         );
 *     }
 *
 *     @Override
 *     public long getEnergyPerDamage() {
 *         return 10000; // Each damage point uses 10000 EU
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
 * <h3>Functions supplied automatically</h3>
 * <ul>
 *   <li>Energy storage (in the common DataComponent)</li>
 *   <li>Energy bar rendering (green-to-red gradient)</li>
 *   <li>Energy tooltip</li>
 *   <li>Feature tooltip (clickable toggle / mode switch, see {@link ArmorFeatureTooltip})</li>
 *   <li>Charger compatibility (through the IBatteryItem interface)</li>
 *   <li>Damage absorption (through the IElectricArmorItem interface, driven by the event system)</li>
 *   <li>Feature toggle (through {@code toggleArmorFeature} /
 *       {@code cycleArmorFeature} of {@code IItemAPI}, or directly with {@link ArmorFeatures})</li>
 *   <li>No vanilla durability loss</li>
 * </ul>
 */
public abstract class AbstractElectricArmor extends ArmorItem implements IElectricArmorItem {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractElectricArmor.class);

    private final long maxEnergy;
    private final long energyPerTick;
    private final long chargeRate;
    private final int armorTier;
    private final String texturePrefix;

    // ==================== Constructor ====================

    /**
     * Creates an electric armor piece.
     *
     * @param material      the armor material
     * @param type          the armor type (helmet, chestplate, leggings or boots)
     * @param properties    the item properties
     * @param maxEnergy     the maximum energy capacity (EU)
     * @param initialEnergy the initial energy (EU)
     * @param chargeRate    the charge rate (EU/tick)
     * @param energyPerTick the continuous energy use per tick (EU). 0 means no continuous use.
     * @param armorTier     the armor tier. The damage absorption rules use it.
     * @param texturePrefix the texture path prefix, in the format "namespace/path".
     *                      The texture files are namespace:textures/armor/path_1.png and _2.png.
     */
    protected AbstractElectricArmor(Holder<ArmorMaterial> material, Type type, Properties properties,
                                    long maxEnergy, long initialEnergy, long chargeRate,
                                    long energyPerTick, int armorTier, String texturePrefix) {
        super(material, type, buildArmorProperties(properties, maxEnergy, initialEnergy));
        this.maxEnergy = Math.max(0, maxEnergy);
        this.chargeRate = chargeRate;
        this.energyPerTick = energyPerTick;
        this.armorTier = armorTier;
        this.texturePrefix = texturePrefix;
    }

    // ==================== Energy storage (common DataComponent) ====================

    private static Properties buildArmorProperties(Properties properties, long maxEnergy, long initialEnergy) {
        // The default component holds the initial energy (this keeps the meaning of initialEnergy from the old design)
        long clamped = Math.clamp(initialEnergy, 0L, Math.max(0, maxEnergy));
        return properties.component(CoreDataComponents.BATTERY_ENERGY.get(), new BatteryEnergy(clamped, maxEnergy));
    }

    // ==================== IBatteryItem implementation ====================

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
        if (stack.isEmpty()) return 0;
        return Math.clamp(readStoredEnergy(stack), 0L, Math.max(0, maxEnergy));
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        energy = Math.max(0, Math.min(maxEnergy, energy));
        stack.set(CoreDataComponents.BATTERY_ENERGY.get(), new BatteryEnergy(energy, maxEnergy));
    }

    /**
     * Compatibility with old saves: moves energy that old versions stored in the durability system to the common DataComponent.
     * It runs one time only, when it finds old data (no component, but durability damage).
     */
    private long readStoredEnergy(ItemStack stack) {
        BatteryEnergy data = stack.get(CoreDataComponents.BATTERY_ENERGY.get());
        if (data != null) return data.energy();
        long legacy = maxEnergy - Math.max(0, stack.getDamageValue());
        if (legacy > 0) {
            stack.set(CoreDataComponents.BATTERY_ENERGY.get(), new BatteryEnergy(legacy, maxEnergy));
            LOGGER.debug("AbstractElectricArmor: migrated legacy energy {} -> DataComponent for {}", legacy, stack.getItem());
        }
        return legacy;
    }

    @Override
    public long addEnergy(ItemStack stack, long amount) {
        if (stack.isEmpty() || stack.getCount() != 1 || amount <= 0) return 0;
        long currentEnergy = getEnergy(stack);
        long accepted = Math.min(amount, Math.max(0, maxEnergy - currentEnergy));
        if (accepted == 0) return 0;
        setEnergy(stack, currentEnergy + accepted);
        return getEnergy(stack) - currentEnergy;
    }

    @Override
    public long extractEnergy(ItemStack stack, long amount) {
        if (stack.isEmpty() || stack.getCount() != 1 || amount <= 0) return 0;
        long currentEnergy = getEnergy(stack);
        long extracted = Math.min(amount, currentEnergy);
        if (extracted == 0) return 0;
        setEnergy(stack, currentEnergy - extracted);
        return currentEnergy - getEnergy(stack);
    }

    @Override
    public boolean isFull(ItemStack stack) {
        return getEnergy(stack) >= getMaxEnergy(stack);
    }

    @Override
    public boolean isEmpty(ItemStack stack) {
        return getEnergy(stack) <= 0;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    // ==================== IElectricArmorItem implementation ====================

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
        return com.miophas.singularity_iteration.core.api.item.BatteryTransfer.consume(stack, this, amount);
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

    // ==================== Energy bar rendering ====================

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

    // ==================== Prevent vanilla durability loss ====================

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
     * Adds energy information to the tooltip.
     * Subclasses can override this method to use a custom energy display format.
     *
     * @param stack             the item stack
     * @param tooltipComponents the tooltip list
     */
    protected void appendEnergyTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(
            Component.translatable("tooltip.mio_icif.armor.energy", getEnergy(stack), maxEnergy)
                .withStyle(ChatFormatting.GRAY)
        );
    }

    /**
     * Adds feature information to the tooltip.
     *
     * <p>It uses the common {@link ArmorFeatureTooltip} to render clickable lines (toggle / mode switch).
     * All items that implement {@link IElectricArmorItem} (also from addon mods) use the same interaction path.
     * Subclasses can override this method to use a custom feature display format.
     *
     * @param stack             the item stack
     * @param tooltipComponents the tooltip list
     */
    protected void appendFeatureTooltip(ItemStack stack, List<Component> tooltipComponents) {
        ArmorFeatureTooltip.appendAll(tooltipComponents, stack, ArmorFeatures.features(stack));
    }

    /**
     * Checks if an on/off feature is enabled.
     *
     * <p>It delegates to {@link ArmorFeatureState} (CustomData key {@code feat_{featureKey}}).
     * A feature that was never written is enabled by default. Subclasses can override this method to use a different storage.
     *
     * @param stack      the item stack
     * @param featureKey the feature key
     * @return true if the feature is enabled
     */
    protected boolean isFeatureEnabled(ItemStack stack, String featureKey) {
        return ArmorFeatureState.isEnabled(stack, featureKey);
    }

    // ==================== Texture ====================

    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        String suffix = (slot == EquipmentSlot.LEGS) ? "_2" : "_1";
        int slashIndex = texturePrefix.indexOf('/');
        String namespace = slashIndex > 0 ? texturePrefix.substring(0, slashIndex) : "minecraft";
        String path = slashIndex > 0 ? texturePrefix.substring(slashIndex + 1) : texturePrefix;
        return ResourceLocation.fromNamespaceAndPath(namespace, "textures/armor/" + path + suffix + ".png");
    }

    // ==================== Helper methods ====================

    /**
     * Gets the texture path prefix.
     */
    public String getTexturePrefix() {
        return texturePrefix;
    }

    /**
     * Checks if the player wears this armor piece in the given slot.
     */
    public boolean isWearingThisArmor(Player player, EquipmentSlot slot) {
        ItemStack equipped = player.getItemBySlot(slot);
        return equipped.getItem() == this;
    }

    /**
     * Checks if the player wears a full set of the same armor series.
     * The same series means AbstractElectricArmor subclasses with the same texturePrefix.
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
     * Gets the remaining energy fraction (0.0 to 1.0).
     */
    public float getDurabilityPercent(ItemStack stack) {
        return maxEnergy <= 0 ? 0.0F : (float) getEnergy(stack) / maxEnergy;
    }

    /**
     * Gets the number of ticks that the armor can still run. Returns 0 if energyPerTick is 0 or less.
     */
    public long getRemainingTicks(ItemStack stack) {
        return energyPerTick <= 0 ? 0 : getEnergy(stack) / energyPerTick;
    }

    /**
     * Checks if the item has energy to use (energy is more than 0).
     */
    public boolean canUse(ItemStack stack) {
        return !isEmpty(stack);
    }
}
