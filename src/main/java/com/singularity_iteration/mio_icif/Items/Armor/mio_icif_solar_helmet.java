package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import com.singularity_iteration.mio_icif.api.item.ISolarHelmetItem;

import java.util.List;

/**
 * 太阳能头盔
 * 参考 IC2 原版工业时代 2 - 太阳能头盔 (mcmod.cn/item/216.html)
 * - 白天且天空可见时自动为头盔充电（1 EU/t = 每秒 20 EU）
 * - 消耗头盔存的电能给玩家背包中的电力装备供电
 * - 只有在能量充足时才分发（至少保留 1 EU 防止能量为空）
 */
@SuppressWarnings({"null", "deprecation"})
public class mio_icif_solar_helmet extends mio_icif_armor_elc implements ISolarHelmetItem {

    // 太阳能头盔最大能量
    public static final int SOLAR_HELMET_MAX_ENERGY = 8000; // 8000 FE
    
    // 太阳能头盔每 tick 充电量（1 EU/t = 每秒 20 EU）
    // 参考 IC2 原版太阳能头盔
    public static final int SOLAR_CHARGE_PER_TICK = 1;

    /**
     * 构造函数
     * @param material 护甲材料
     * @param properties 物品属性
     */
    public mio_icif_solar_helmet(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties, SOLAR_HELMET_MAX_ENERGY, 0, "solar_helmet", 0, 0, 4);
    }

    /**
     * 检查当前光照下是否可以充电
     * 对应 IC2 的 TileEntitySolarGenerator.isSunVisible 逻辑
     */
    private boolean canCharge(Level level, Entity entity) {
        if (!level.isDay()) {
            return false;
        }
        
        if (entity instanceof Player) {
            // 检查玩家 y+1 位置的天空光照级别
            int skyLightLevel = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, 
                net.minecraft.core.BlockPos.containing(entity.position().x, entity.position().y + 1, entity.position().z));
            return skyLightLevel >= 10;
        }
        
        return false;
    }

    /**
     * 护甲每 tick 更新
     * 1. 白天从天空补充能量到头盔存储
 * 2. 消耗头盔存的能量给玩家背包中的电力装备充电（只在能量充足时）
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        // 检查玩家是否穿着这件头盔
        if (!isWearingThisArmor(player, EquipmentSlot.HEAD)) {
            return;
        }

        // 1. 太阳能充电：白天且天空可见时为头盔补充能量
        if (canCharge(level, entity)) {
            long currentEnergy = getEnergy(stack);
            if (currentEnergy < SOLAR_HELMET_MAX_ENERGY) {
                long newEnergy = Math.min(SOLAR_HELMET_MAX_ENERGY, currentEnergy + SOLAR_CHARGE_PER_TICK);
                setEnergy(stack, newEnergy);
            }
        }

        // 2. 分发能量给玩家的电力装备（穿戴 + 背包）
        // 注意：只在有能量时分发
        if (getEnergy(stack) > 0) {
            distributeEnergyToPlayer(player, stack);
        }
    }

    /**
     * 将头盔的能量分发给玩家的电力装备
     * 优先给穿戴的装备充电，然后给背包中的物品充电
     * 每次分发固定能量，确保不超过太阳能充电速度
     * @param player 玩家
     * @param helmetStack 头盔物品堆
     */
    private void distributeEnergyToPlayer(Player player, ItemStack helmetStack) {
        long helmetEnergy = getEnergy(helmetStack);
        if (helmetEnergy <= 0) {
            return;
        }

        // 每次最多分发 1 EU/t，与太阳能充电速度匹配
        // 这样头盔可以存储能量供夜间或室内使用
        int distributeAmount = (int) Math.min(1, helmetEnergy);
        int remainingToDistribute = distributeAmount;

        // 第一步：给穿戴的装备充电（胸甲 -> 护腿 -> 靴子 -> 头盔）
        EquipmentSlot[] wearSlots = {
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.HEAD
        };
        
        for (EquipmentSlot slot : wearSlots) {
            if (remainingToDistribute <= 0) {
                break;
            }

            ItemStack equipped = player.getItemBySlot(slot);
            if (equipped.isEmpty()) {
                continue;
            }

            remainingToDistribute = chargeItem(equipped, remainingToDistribute, helmetStack);
        }

        // 第二步：给背包中的物品充电
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (remainingToDistribute <= 0) {
                break;
            }

            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            remainingToDistribute = chargeItem(stack, remainingToDistribute, helmetStack);
        }
    }

    /**
     * 给单个物品充电（支持电力装甲和电力工具）
     * @param stack 物品堆
     * @param maxEnergy 最大可充电量
     * @param helmetStack 头盔物品堆（用于扣除能量）
     * @return 实际未分配完的能量
     */
    private int chargeItem(ItemStack stack, int maxEnergy, ItemStack helmetStack) {
        var api = MioIcifAPI.instance().getItemAPI();
        
        // 尝试给电力装甲充电
        if (api.isElectricArmor(stack)) {
            long currentEnergy = api.getElectricArmorStored(stack);
            long maxEnergyCapacity = api.getElectricArmorMaxEnergy(stack);

            if (currentEnergy < maxEnergyCapacity) {
                long spaceAvailable = maxEnergyCapacity - currentEnergy;
                long toCharge = Math.min(maxEnergy, spaceAvailable);

                // 给背包中的装备充电
                api.chargeElectricArmor(stack, toCharge, false);
                
                // 从头盔扣除相应能量
                deductHelmetEnergy(helmetStack, toCharge);

                return maxEnergy - (int) toCharge;
            }
        }
        // 尝试给电池充电
        else if (api.isBattery(stack)) {
            long currentEnergy = api.getBatteryStored(stack);
            long maxEnergyCapacity = api.getBatteryCapacity(stack);

            if (currentEnergy < maxEnergyCapacity) {
                long spaceAvailable = maxEnergyCapacity - currentEnergy;
                long toCharge = Math.min(maxEnergy, spaceAvailable);

                // 给背包中的工具充电
                api.chargeBattery(stack, toCharge, false);
                
                // 从头盔扣除相应能量
                deductHelmetEnergy(helmetStack, toCharge);

                return maxEnergy - (int) toCharge;
            }
        }
        return maxEnergy;
    }

    /**
     * 从头盔扣除能量
     * @param helmetStack 头盔物品堆
     * @param amount 要扣除的能量
     */
    private void deductHelmetEnergy(ItemStack helmetStack, long amount) {
        long currentHelmetEnergy = getEnergy(helmetStack);
        // 允许能量降到0（与电池基类一致，物品不会因能量为0被销毁）
        long newHelmetEnergy = Math.max(0, currentHelmetEnergy - amount);
        setEnergy(helmetStack, newHelmetEnergy);
    }

    /**
     * 获取默认实例（满电状态）
     */
    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        setEnergy(stack, SOLAR_HELMET_MAX_ENERGY);
        return stack;
    }

    /**
     * 获取护甲纹理
     */
    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/armor/solar_1.png");
    }

    /**
     * 添加 tooltip 显示
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        getEnergy(stack);
        tooltip.add(Component.translatable("tooltip.mio_icif.solar_helmet.solar", SOLAR_CHARGE_PER_TICK)
                .withStyle(net.minecraft.ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.mio_icif.solar_helmet.distribute")
                .withStyle(net.minecraft.ChatFormatting.AQUA));
    }

    // ==================== ISolarHelmetItem API ====================

    @Override
    public long getGenerationRate() {
        return SOLAR_CHARGE_PER_TICK;
    }

    @Override
    public boolean requiresSky() {
        return true;
    }

    @Override
    public boolean isDayOnly() {
        return true;
    }
}