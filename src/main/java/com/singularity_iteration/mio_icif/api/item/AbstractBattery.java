package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 电池抽象基类，提供完整的默认能量存储实现。
 *
 * <p>Addon 开发者只需继承此类并指定构造参数即可创建一个功能完整的电池物品，
 * 无需访问任何 mio_icif 内部类。
 *
 * <h3>能量存储机制</h3>
 * <p>支持两种存储方式：
 * <ul>
 *   <li><b>不可堆叠电池</b>（maxStackSize = 1）：使用耐久度系统存储能量，
 *       耐久度条满（damageValue = 0）= 能量满，耐久度条空（damageValue = maxEnergy）= 能量为 0</li>
 *   <li><b>可堆叠电池</b>（maxStackSize > 1）：使用自定义 DataComponent 存储能量，
 *       每个物品堆独立存储能量值</li>
 * </ul>
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
 *   <li>能量存储（基于耐久度或 DataComponent）</li>
 *   <li>能量条渲染（绿→红渐变）</li>
 *   <li>能量 Tooltip 显示</li>
 *   <li>Shift 右键给装备充电</li>
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
     * @param maxStackSize  最大堆叠数，大于 1 时使用 DataComponent 存储能量
     */
    protected AbstractBattery(Properties properties, long maxEnergy, long initialEnergy, long chargeRate, int maxStackSize) {
        super(buildProperties(properties, maxEnergy, initialEnergy, maxStackSize));
        this.maxEnergy = maxEnergy;
        this.chargeRate = chargeRate;
        this.stackable = maxStackSize > 1;
    }

    private static Properties buildProperties(Properties properties, long maxEnergy, long initialEnergy, int maxStackSize) {
        if (maxStackSize > 1) {
            return properties
                .stacksTo(maxStackSize);
        } else {
            int durability = capToInt(maxEnergy, "durability");
            int damage = capToInt(Math.max(0, maxEnergy - initialEnergy), "initial damage");
            return properties
                .durability(durability)
                .component(DataComponents.DAMAGE, damage)
                .component(DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(false));
        }
    }

    private static int capToInt(long value, String context) {
        if (value > Integer.MAX_VALUE) {
            LOGGER.warn("AbstractBattery: {} value {} exceeds int limit, capping to {}. Non-stackable items are limited to int durability.", context, value, Integer.MAX_VALUE);
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
        if (stackable) {
            return getStackableEnergy(stack);
        } else {
            return maxEnergy - stack.getDamageValue();
        }
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        energy = Math.max(0L, Math.min(maxEnergy, energy));
        if (stackable) {
            setStackableEnergy(stack, energy);
        } else {
            long damage = maxEnergy - energy;
            stack.setDamageValue(capToInt(damage, "setEnergy damage"));
        }
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
        long newEnergy = Math.max(0L, currentEnergy - amount);
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

    // ==================== 可堆叠电池的能量存储 ====================

    /**
     * 从可堆叠电池的 CUSTOM_DATA 中读取能量值。
     * 子类可以覆盖此方法以使用不同的存储方式。
     *
     * @param stack 物品堆
     * @return 当前存储的能量
     */
    protected long getStackableEnergy(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0L;
        return customData.copyTag().getLong("energy");
    }

    /**
     * 向可堆叠电池的 CUSTOM_DATA 中写入能量值。
     * 子类可以覆盖此方法以使用不同的存储方式。
     *
     * @param stack  物品堆
     * @param energy 要设置的能量值
     */
    protected void setStackableEnergy(ItemStack stack, long energy) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putLong("energy", energy);
        tag.putLong("maxEnergy", maxEnergy);
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
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

    // ==================== Shift 右键分配能量 ====================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player.isShiftKeyDown()) {
            long batteryEnergy = getEnergy(stack);
            if (batteryEnergy > 0) {
                distributeEnergyToItems(player, stack, batteryEnergy);
                player.displayClientMessage(
                    Component.translatable("message.mio_icif.bat.distribute", getEnergy(stack), maxEnergy),
                    true
                );
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    /**
     * 将电池能量分配给玩家背包中的其他电力装备。
     * 子类可以覆盖此方法以自定义分配逻辑。
     *
     * @param player        玩家
     * @param batteryStack  电池物品堆
     * @param totalEnergy   可分配的总能量
     */
    protected void distributeEnergyToItems(Player player, ItemStack batteryStack, long totalEnergy) {
        List<ItemStack> targets = new ArrayList<>();
        Inventory inv = player.getInventory();
        var api = com.singularity_iteration.mio_icif.api.MioIcifAPI.instance().getItemAPI();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || s == batteryStack) continue;
            if (api.isBattery(s) && !api.isElectricTool(s)) continue;
            if (api.isBattery(s)) {
                if (!api.isBatteryFull(s)) targets.add(s);
            } else if (api.isElectricArmor(s)) {
                if (api.getElectricArmorStored(s) < api.getElectricArmorMaxEnergy(s)) targets.add(s);
            }
        }

        if (targets.isEmpty()) return;

        long energyPerTarget = totalEnergy / targets.size();
        if (energyPerTarget <= 0) return;

        long actuallyDistributed = 0;
        for (ItemStack target : targets) {
            if (api.isBattery(target)) {
                long added = api.chargeBattery(target, energyPerTarget, false);
                actuallyDistributed += added;
            } else if (api.isElectricArmor(target)) {
                long added = api.chargeElectricArmor(target, energyPerTarget, false);
                actuallyDistributed += added;
            }
        }

        extractEnergy(batteryStack, actuallyDistributed);
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
    }

    // ==================== 生命周期 ====================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (stackable) {
            setEnergy(stack, 0);
        } else {
            stack.setDamageValue(capToInt(maxEnergy, "craft damage"));
        }
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        if (stackable) {
            setEnergy(stack, 0);
        } else {
            stack.setDamageValue(capToInt(maxEnergy, "default damage"));
        }
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