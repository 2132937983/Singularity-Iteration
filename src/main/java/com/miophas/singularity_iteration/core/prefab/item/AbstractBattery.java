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
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * 电池抽象基类，提供完整的默认能量存储实现。
 *
 * <p>Addon 开发者只需继承此类并指定构造参数即可创建一个功能完整的电池物品，
 * 无需访问任何 mio_icif 内部类。
 *
 * <h3>能量存储机制</h3>
 * <p>所有电池（无论是否可堆叠）统一使用 DataComponent {@code mio_icif:battery_energy}
 * （{@link BatteryEnergy}，含 energy / maxEnergy 两个字段）存储能量，
 * 每个物品堆独立存储能量值，不再依赖耐久度系统。
 * 旧存档中基于耐久度或 CUSTOM_DATA 存储的电量会在首次读取时自动迁移。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 不可堆叠电池（如 MFE、MFSU 等大型电池）
 * public class MyLargeBattery extends AbstractBattery {
 *     public MyLargeBattery() {
 *         super(new Properties(), 1_000_000, 0, 512);
 *     }
 * }
 *
 * // 可堆叠电池（如普通电池、充电电池）
 * public class MySmallBattery extends AbstractBattery {
 *     public MySmallBattery() {
 *         super(new Properties(), 10000, 10000, 10000, 64);
 *     }
 * }
 * }</pre>
 *
 * <h3>自动提供的功能</h3>
 * <ul>
 *   <li>能量存储（基于统一 DataComponent）</li>
 *   <li>能量条渲染（绿→红渐变）</li>
 *   <li>能量 Tooltip 显示</li>
 *   <li>右键给装备充电</li>
 *   <li>充电器兼容（通过 IBatteryItem 接口）</li>
 *   <li>防止原版耐久消耗</li>
 * </ul>
 */
public class AbstractBattery extends Item implements IBatteryItem {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBattery.class);

    private final long maxEnergy;
    private final long chargeRate;
    private final boolean stackable;

    /**
     * 创建不可堆叠电池（maxStackSize = 1）。
     *
     * @param properties    物品属性
     * @param maxEnergy     最大能量容量 (EU)
     * @param initialEnergy 初始能量 (EU)
     * @param chargeRate    充电速率 (EU/tick)
     */
    protected AbstractBattery(Properties properties, long maxEnergy, long initialEnergy, long chargeRate) {
        this(properties, maxEnergy, initialEnergy, chargeRate, 1);
    }

    /**
     * 创建电池。
     *
     * @param properties    物品属性
     * @param maxEnergy     最大能量容量 (EU)
     * @param initialEnergy 初始能量 (EU)
     * @param chargeRate    充电速率 (EU/tick)
     * @param maxStackSize  最大堆叠数
     */
    protected AbstractBattery(Properties properties, long maxEnergy, long initialEnergy, long chargeRate, int maxStackSize) {
        super(buildProperties(properties, maxEnergy, initialEnergy, maxStackSize));
        this.maxEnergy = Math.max(0, maxEnergy);
        this.chargeRate = chargeRate;
        this.stackable = maxStackSize > 1;
    }

    private static Properties buildProperties(Properties properties, long maxEnergy, long initialEnergy, int maxStackSize) {
        if (maxStackSize > 1) {
            return properties
                .stacksTo(maxStackSize);
        } else {
            // 不可堆叠电池：通过默认组件携带初始电量（保持旧方案中 initialEnergy 的语义）
            long clamped = Math.clamp(initialEnergy, 0L, Math.max(0, maxEnergy));
            return properties
                .component(CoreDataComponents.BATTERY_ENERGY.get(), new BatteryEnergy(clamped, maxEnergy));
        }
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
        if (stack.isEmpty()) return 0;
        return Math.clamp(readStoredEnergy(stack), 0L, Math.max(0, maxEnergy));
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        energy = Math.max(0L, Math.min(maxEnergy, energy));
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

    // ==================== 统一能量存储（DataComponent） ====================

    /**
     * 从物品堆的统一 DataComponent（{@code mio_icif:battery_energy}）中读取能量值。
     * 子类可以覆盖此方法以使用不同的存储方式。
     *
     * @param stack 物品堆
     * @return 当前存储的能量
     */
    protected long readStoredEnergy(ItemStack stack) {
        BatteryEnergy data = stack.get(CoreDataComponents.BATTERY_ENERGY.get());
        if (data != null) return data.energy();
        return readLegacyEnergy(stack);
    }

    /**
     * 向物品堆的统一 DataComponent（{@code mio_icif:battery_energy}）中写入能量值。
     * 子类可以覆盖此方法以使用不同的存储方式。
     *
     * @param stack  物品堆
     * @param energy 要设置的能量值
     */
    protected void writeStoredEnergy(ItemStack stack, long energy) {
        stack.set(CoreDataComponents.BATTERY_ENERGY.get(), new BatteryEnergy(energy, maxEnergy));
    }

    /**
     * 兼容旧存档：将旧版本通过耐久度系统（不可堆叠）或 CUSTOM_DATA（可堆叠）
     * 存储的电量迁移到统一 DataComponent，仅在检测到旧数据时执行一次。
     */
    private long readLegacyEnergy(ItemStack stack) {
        if (stackable) {
            var customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) return 0L;
            long legacy = customData.copyTag().getLong("energy");
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
        LOGGER.debug("AbstractBattery: migrated legacy energy {} -> DataComponent for {}", migrated, stack.getItem());
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
    public boolean isDamaged(ItemStack stack) {
        return getEnergy(stack) < maxEnergy;
    }

    // ==================== 右键分配能量 ====================

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
     * 追加能量信息到 Tooltip。
     * 子类可以覆盖此方法来自定义能量显示格式。
     *
     * @param stack             物品堆
     * @param tooltipComponents Tooltip 列表
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

    // ==================== 生命周期 ====================

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

    // ==================== 辅助方法 ====================

    /**
     * 获取充电速率
     */
    public long getChargeRate() {
        return chargeRate;
    }

    /**
     * 是否为可堆叠电池
     */
    public boolean isStackable() {
        return stackable;
    }

    /**
     * 获取能量剩余百分比 (0.0 ~ 1.0)
     */
    public float getDurabilityPercent(ItemStack stack) {
        return maxEnergy <= 0 ? 0.0F : (float) getEnergy(stack) / maxEnergy;
    }
}