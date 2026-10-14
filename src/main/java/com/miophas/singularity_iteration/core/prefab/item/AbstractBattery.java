package com.miophas.singularity_iteration.core.prefab.item;

import com.miophas.singularity_iteration.core.api.item.BatteryEnergy;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.IEnergyDistributable;
import com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Abstract base class for batteries. It gives a complete default energy storage implementation.
 *
 * <p>To make a complete battery item, an addon developer extends this class and supplies the constructor arguments.
 * The addon does not need access to internal mio_icif classes.
 *
 * <h3>Energy storage</h3>
 * <p>All batteries (stackable or not) store energy in the DataComponent {@code mio_icif:battery_energy}
 * ({@link BatteryEnergy}, with the fields energy and maxEnergy).
 * Each item stack stores its own energy value. The durability system is not used.
 * Energy that old saves stored in durability or CUSTOM_DATA moves to the component automatically on the first read.
 *
 * <h3>Stacking (0.1.10.3)</h3>
 * <p>Only empty batteries stack. A battery with a stack size above 1 (or an empty stack size, see
 * {@link #AbstractBattery(Properties, long, long, long, int, int)}) stacks up to that size while it holds
 * no energy; as soon as it holds energy its stack cannot grow, so two charged batteries never merge.
 * An empty stackable battery carries no energy component: a new, a crafted and a discharged battery are
 * the same stack. A stack of more than one battery never takes or gives energy (charger slots hold one).
 *
 * <h3>Example</h3>
 * <pre>{@code
 * // Non-stackable battery (for example, large batteries such as MFE or MFSU)
 * public class MyLargeBattery extends AbstractBattery {
 *     public MyLargeBattery() {
 *         super(new Properties(), 1_000_000, 0, 512);
 *     }
 * }
 *
 * // Stackable battery (for example, a normal battery or a rechargeable battery)
 * public class MySmallBattery extends AbstractBattery {
 *     public MySmallBattery() {
 *         super(new Properties(), 10000, 10000, 10000, 64);
 *     }
 * }
 * }</pre>
 *
 * <h3>Features that this class supplies</h3>
 * <ul>
 *   <li>Energy storage (in the shared DataComponent).</li>
 *   <li>Energy bar rendering (color changes from green to red).</li>
 *   <li>Energy tooltip.</li>
 *   <li>Right-click to charge equipment.</li>
 *   <li>Charger compatibility (through the IBatteryItem interface).</li>
 *   <li>No vanilla durability loss.</li>
 * </ul>
 */
public class AbstractBattery extends Item implements IBatteryItem {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBattery.class);

    /** Legacy CUSTOM_DATA key of stackable batteries (before the shared component). */
    private static final String LEGACY_ENERGY_KEY = "energy";

    private final long maxEnergy;
    private final long chargeRate;
    private final boolean stackable;
    private final int emptyStackSize;

    /**
     * Creates a non-stackable battery (maxStackSize = 1).
     *
     * @param properties    the item properties
     * @param maxEnergy     the maximum energy capacity (EU)
     * @param initialEnergy the initial energy (EU)
     * @param chargeRate    the charge rate (EU/tick)
     */
    protected AbstractBattery(Properties properties, long maxEnergy, long initialEnergy, long chargeRate) {
        this(properties, maxEnergy, initialEnergy, chargeRate, 1);
    }

    /**
     * Creates a battery.
     *
     * @param properties    the item properties
     * @param maxEnergy     the maximum energy capacity (EU). A negative value becomes 0.
     * @param initialEnergy the initial energy (EU). Only a non-stackable battery uses this value.
     * @param chargeRate    the charge rate (EU/tick)
     * @param maxStackSize  the maximum stack size
     */
    protected AbstractBattery(Properties properties, long maxEnergy, long initialEnergy, long chargeRate, int maxStackSize) {
        this(properties, maxEnergy, initialEnergy, chargeRate, maxStackSize, maxStackSize);
    }

    /**
     * Creates a battery that stacks only while it is empty (0.1.10.3).
     *
     * <p>With a {@code maxStackSize} of 1 the battery keeps the non-stackable storage: the default
     * component holds {@code initialEnergy}, so old saves keep their charge. Empty batteries of this
     * item still stack up to {@code emptyStackSize}.
     *
     * @param properties     the item properties
     * @param maxEnergy      the maximum energy capacity (EU). A negative value becomes 0.
     * @param initialEnergy  the initial energy (EU). Only a battery with a stack size of 1 uses this value.
     * @param chargeRate     the charge rate (EU/tick)
     * @param maxStackSize   the stack size of the item (1 = non-stackable storage)
     * @param emptyStackSize the stack size of empty batteries (1 to 99; it is at least {@code maxStackSize})
     */
    protected AbstractBattery(Properties properties, long maxEnergy, long initialEnergy, long chargeRate, int maxStackSize, int emptyStackSize) {
        super(buildProperties(properties, maxEnergy, initialEnergy, maxStackSize));
        this.maxEnergy = Math.max(0, maxEnergy);
        this.chargeRate = chargeRate;
        this.stackable = maxStackSize > 1;
        this.emptyStackSize = Math.clamp(Math.max(maxStackSize, emptyStackSize), 1, 99);
    }

    private static Properties buildProperties(Properties properties, long maxEnergy, long initialEnergy, int maxStackSize) {
        if (maxStackSize > 1) {
            return properties
                .stacksTo(maxStackSize);
        } else {
            // Non-stackable battery: the default component holds the initial energy (this keeps the old meaning of initialEnergy)
            long clamped = Math.clamp(initialEnergy, 0L, Math.max(0, maxEnergy));
            return properties
                .component(CoreDataComponents.BATTERY_ENERGY.get(), new BatteryEnergy(clamped, maxEnergy));
        }
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
        energy = Math.max(0L, Math.min(maxEnergy, energy));
        // 0.1.10.3: a stack of several empty batteries is never charged as one (each item would get the energy)
        if (energy > 0 && emptyStackSize > 1 && stack.getCount() > 1) return;
        writeStoredEnergy(stack, energy);
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

    // ==================== Shared energy storage (DataComponent) ====================

    /**
     * Reads the energy value from the shared DataComponent ({@code mio_icif:battery_energy}) of the item stack.
     * If the component is not present, this method reads and migrates legacy data.
     * Subclasses can override this method to use a different storage.
     *
     * @param stack the item stack
     * @return the stored energy (EU)
     */
    protected long readStoredEnergy(ItemStack stack) {
        BatteryEnergy data = stack.get(CoreDataComponents.BATTERY_ENERGY.get());
        if (data != null) return data.energy();
        return readLegacyEnergy(stack);
    }

    /**
     * Writes the energy value to the shared DataComponent ({@code mio_icif:battery_energy}) of the item stack.
     * Subclasses can override this method to use a different storage.
     *
     * @param stack  the item stack
     * @param energy the energy value to set (EU)
     */
    protected void writeStoredEnergy(ItemStack stack, long energy) {
        if (stackable && energy <= 0) {
            // 0.1.10.3: an empty stackable battery carries no energy data, so it stacks with a new one
            stack.remove(CoreDataComponents.BATTERY_ENERGY.get());
            dropLegacyEnergyKey(stack);
            return;
        }
        stack.set(CoreDataComponents.BATTERY_ENERGY.get(), new BatteryEnergy(energy, maxEnergy));
    }

    /**
     * Compatibility with old saves.
     * Old versions stored energy in the durability system (non-stackable) or in CUSTOM_DATA (stackable).
     * This method moves that energy to the shared DataComponent.
     * The migration occurs one time, only when old data is found.
     */
    private long readLegacyEnergy(ItemStack stack) {
        if (stackable) {
            var customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData == null || !customData.contains(LEGACY_ENERGY_KEY)) return 0L;
            long legacy = customData.copyTag().getLong(LEGACY_ENERGY_KEY);
            if (legacy > 0) migrateLegacyEnergy(stack, legacy);
            return legacy;
        }
        long legacy = maxEnergy - Math.max(0, stack.getDamageValue());
        if (legacy > 0) migrateLegacyEnergy(stack, legacy);
        return legacy;
    }

    private void migrateLegacyEnergy(ItemStack stack, long legacy) {
        long migrated = Math.clamp(legacy, 0L, Math.max(0, maxEnergy));
        stack.set(CoreDataComponents.BATTERY_ENERGY.get(), new BatteryEnergy(migrated, maxEnergy));
        // the key would bring the old charge back once the component is removed at 0 EU
        if (stackable) dropLegacyEnergyKey(stack);
        LOGGER.debug("AbstractBattery: migrated legacy energy {} -> DataComponent for {}", migrated, stack.getItem());
    }

    private static void dropLegacyEnergyKey(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(LEGACY_ENERGY_KEY)) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(LEGACY_ENERGY_KEY));
    }

    /**
     * 0.1.10.3: older versions wrote an explicit zero charge when a stackable battery was crafted,
     * discharged or taken from the creative menu. Such a battery differed from a new one and never
     * stacked with it; the zero is dropped whenever a stack is loaded, received or copied.
     */
    @Override
    public void verifyComponentsAfterLoad(ItemStack stack) {
        super.verifyComponentsAfterLoad(stack);
        if (!stackable) return;
        var type = CoreDataComponents.BATTERY_ENERGY.get();
        BatteryEnergy data = stack.get(type);
        if (data != null && data.energy() <= 0) {
            stack.remove(type);
            data = null;
        }
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains(LEGACY_ENERGY_KEY)
                && (data != null || customData.copyTag().getLong(LEGACY_ENERGY_KEY) <= 0)) {
            dropLegacyEnergyKey(stack);   // stale (already migrated) or empty
        }
    }

    // ==================== Stack size ====================

    /**
     * 0.1.10.3: empty batteries stack up to the empty stack size; a battery that holds energy never
     * grows its stack, so two charged batteries (even with the same charge) stay apart.
     */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        int base = super.getMaxStackSize(stack);
        if (emptyStackSize <= 1) return base;
        if (getEnergy(stack) > 0) return Math.max(1, Math.min(base, stack.getCount()));
        return Math.max(base, emptyStackSize);
    }

    /** The stack size of empty batteries of this item (1 = never stacks). */
    public int getEmptyStackSize() {
        return emptyStackSize;
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

    // ==================== No vanilla durability loss ====================

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        return 0;
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        return getEnergy(stack) < maxEnergy;
    }

    // ==================== Right-click energy distribution ====================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (this instanceof IEnergyDistributable distributable) {
            InteractionResultHolder<ItemStack> result = distributable.handleDistributeUse(level, player, hand);
            if (result != null) return result;
        }
        return super.use(level, player, hand);
    }

    // ==================== Tooltip ====================

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        appendEnergyTooltip(stack, tooltipComponents);
    }

    /**
     * Adds energy information to the tooltip.
     * Subclasses can override this method to change the energy display format.
     *
     * @param stack             the item stack
     * @param tooltipComponents the tooltip list
     */
    protected void appendEnergyTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(
            Component.translatable("tooltip.mio_icif.energy", getEnergy(stack), maxEnergy)
                .withStyle(ChatFormatting.GRAY)
        );
        if (this instanceof IEnergyDistributable d && d.supportsAutoCharge()) {
            boolean on = com.miophas.singularity_iteration.core.api.item.BatteryAutoCharge.isOn(stack);
            tooltipComponents.add(Component.translatable(on ? "tooltip.mio_icif.bat.auto_on" : "tooltip.mio_icif.bat.auto_off")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
    }

    // ==================== Lifecycle ====================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && this instanceof IEnergyDistributable d && d.supportsAutoCharge() && entity instanceof Player player) {
            com.miophas.singularity_iteration.core.api.item.BatteryAutoCharge.tick(player, stack, this, chargeRate, slotId);
        }
    }

    /** Auto-charge mode shows as the enchantment glint. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || com.miophas.singularity_iteration.core.api.item.BatteryAutoCharge.isOn(stack);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (getEnergy(stack) > 0) return;
        setEnergy(stack, 0);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        setEnergy(stack, 0);
        return stack;
    }

    // ==================== Helper methods ====================

    /**
     * Gets the charge rate (EU/tick).
     */
    public long getChargeRate() {
        return chargeRate;
    }

    /**
     * Returns true if the battery is stackable.
     */
    public boolean isStackable() {
        return stackable;
    }

    /**
     * Gets the remaining energy fraction (0.0 to 1.0).
     */
    public float getDurabilityPercent(ItemStack stack) {
        return maxEnergy <= 0 ? 0.0F : (float) getEnergy(stack) / maxEnergy;
    }
}