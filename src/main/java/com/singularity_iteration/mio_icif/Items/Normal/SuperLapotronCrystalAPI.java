package com.singularity_iteration.mio_icif.Items.Normal;

import com.singularity_iteration.mio_icif.api.item.IBatteryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 纯 API 实现的超级蓝波顿水晶。
 * 
 * <p>此类不继承 mio_icif_bat，而是直接实现 {@link IBatteryItem} 接口，
 * 使用 NBT 存储能量，所有能量操作通过接口方法暴露。
 * 
 * <p>这是展示如何为附属模组创建兼容电池的示例：
 * <ul>
 *   <li>实现 {@link IBatteryItem} 接口</li>
 *   <li>使用 {@link net.minecraft.world.item.component.CustomData} 存储能量到 NBT</li>
 *   <li>提供能量条显示（ durability bar ）</li>
 *   <li>自动获得所有 mio_icif 充电设备的兼容性</li>
 * </ul>
 */
public class SuperLapotronCrystalAPI extends Item implements IBatteryItem {

    public static final long MAX_ENERGY = 100_000_000L;
    public static final long CHARGE_RATE = 8192L;
    private static final String ENERGY_KEY = "mio_icif_api_energy";

    public SuperLapotronCrystalAPI(Properties properties) {
        super(properties.stacksTo(1).durability(1000));
    }

    // ==================== IBatteryItem 实现 ====================

    @Override
    public long getMaxEnergy() {
        return MAX_ENERGY;
    }

    @Override
    public long getEnergy(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = 
            stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            if (tag.contains(ENERGY_KEY)) {
                return tag.getLong(ENERGY_KEY);
            }
        }
        return 0L;
    }

    @Override
    public long addEnergy(ItemStack stack, long amount) {
        long current = getEnergy(stack);
        long added = Math.min(amount, MAX_ENERGY - current);
        if (added > 0) {
            setEnergy(stack, current + added);
        }
        return added;
    }

    @Override
    public long extractEnergy(ItemStack stack, long amount) {
        long current = getEnergy(stack);
        long extracted = Math.min(amount, current);
        if (extracted > 0) {
            setEnergy(stack, current - extracted);
        }
        return extracted;
    }

    @Override
    public boolean isFull(ItemStack stack) {
        return getEnergy(stack) >= MAX_ENERGY;
    }

    @Override
    public boolean isEmpty(ItemStack stack) {
        return getEnergy(stack) <= 0;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return CHARGE_RATE;
    }

    // ==================== 内部辅助方法 ====================

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        energy = Math.max(0L, Math.min(MAX_ENERGY, energy));
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        net.minecraft.world.item.component.CustomData customData = 
            stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }
        tag.putLong(ENERGY_KEY, energy);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
            net.minecraft.world.item.component.CustomData.of(tag));
        
        // 同步 durability bar
        updateDurabilityBar(stack, energy);
    }

    private void updateDurabilityBar(ItemStack stack, long energy) {
        float ratio = (float) energy / MAX_ENERGY;
        int damage = (int) ((1.0F - ratio) * 1000);
        stack.setDamageValue(Math.min(1000, Math.max(0, damage)));
    }

    // ==================== Item 覆盖方法 ====================

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long currentEnergy = getEnergy(stack);
        return (int) Math.round(13.0 * currentEnergy / MAX_ENERGY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        long currentEnergy = getEnergy(stack);
        float ratio = (float) currentEnergy / MAX_ENERGY;
        int r = Math.round(255 * (1.0F - ratio));
        int g = Math.round(255 * ratio);
        return r << 16 | g << 8;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, 
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(
            Component.translatable("tooltip.mio_icif.energy", getEnergy(stack), MAX_ENERGY)
                .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
            Component.translatable("tooltip.mio_icif.api_battery.desc")
                .withStyle(ChatFormatting.BLUE)
        );
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        setEnergy(stack, 0);
        return stack;
    }

    @Override
    public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(
            ItemStack stack, int amount, T entity, java.util.function.Consumer<Item> onBroken) {
        return 0; // 电池不应该被正常损坏
    }
}