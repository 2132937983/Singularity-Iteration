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
 * 混合太阳能头盔 (Hybrid Solar Helmet)
 * 参考 ASP 配置:
 * - 白天发电: 64 EU/t
 * - 夜晚发电: 8 EU/t
 * - 能量存储: 10,000,000 EU
 * - 传输限制: 10000 EU/t
 * - Tier: 4
 * - 伤害吸收: 100%
 * - 额外功能: 水下呼吸
 */
@SuppressWarnings({"null", "deprecation"})
public class mio_icif_hybrid_solar_helmet extends mio_icif_armor_elc implements ISolarHelmetItem {

    // 能量配置 (转换为 FE，1 EU = 4 FE)
    public static final int MAX_ENERGY = 10_000_000 * 4; // 40,000,000 FE
    public static final int DAY_GENERATION = 64 * 4; // 256 FE/t
    public static final int NIGHT_GENERATION = 8 * 4; // 32 FE/t
    public static final int TRANSFER_LIMIT = 10000 * 4; // 40000 FE/t
    public static final int TIER = 4;
    public static final double DAMAGE_ABSORPTION = 1.0;

    /**
     * 构造函数
     * @param material 护甲材料
     * @param properties 物品属性
     */
    public mio_icif_hybrid_solar_helmet(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties, MAX_ENERGY, 0, "hybrid_solar_helmet", TRANSFER_LIMIT, 0, TIER);
    }

    /**
     * 检查当前光照下是否可以发电
     */
    private boolean canGenerate(Level level, Entity entity) {
        if (!(entity instanceof Player)) {
            return false;
        }

        // 检查玩家y+1位置的天空光照等级
        int skyLightLevel = level.getBrightness(net.minecraft.world.level.LightLayer.SKY,
            net.minecraft.core.BlockPos.containing(entity.position().x, entity.position().y + 1, entity.position().z));
        return skyLightLevel >= 10;
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
        boolean isRaining = level.isRaining() || level.isThundering();

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
        if (!isWearingThisArmor(player, EquipmentSlot.HEAD)) {
            return;
        }

        // 1. 太阳能发电
        int generation = getGenerationRate(level, entity);
        if (generation > 0) {
            long currentEnergy = getEnergy(stack);
            if (currentEnergy < MAX_ENERGY) {
                long newEnergy = Math.min(MAX_ENERGY, currentEnergy + generation);
                setEnergy(stack, newEnergy);
            }
        }

        // 2. 分发能量给玩家的电力装备
        if (getEnergy(stack) > 0) {
            distributeEnergyToPlayer(player, stack);
        }

        // 3. 水下呼吸功能
        if (getEnergy(stack) >= 1000 * 4) { // 需要至少 1000 EU
            int airLevel = player.getAirSupply();
            if (airLevel < 100) {
                player.setAirSupply(airLevel + 200);
                extractEnergy(stack, 1000 * 4);
            }
        }
    }

    /**
     * 将头盔的能量分发给玩家的电力装备
     * 优先给穿戴的装备充电，然后给背包中的物品充电
     * @param player 玩家
     * @param helmetStack 头盔物品堆
     */
    private void distributeEnergyToPlayer(Player player, ItemStack helmetStack) {
        long helmetEnergy = getEnergy(helmetStack);
        if (helmetEnergy <= 0) {
            return;
        }

        // 每次最多分发 TRANSFER_LIMIT
        int distributeAmount = (int) Math.min(TRANSFER_LIMIT, helmetEnergy);
        int remainingToDistribute = distributeAmount;

        // 第一步：给穿戴的装备充电（胸甲 -> 护腿 -> 靴子）
        EquipmentSlot[] wearSlots = {
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
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

                // 给装备充电
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

                // 给工具充电
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
        long newHelmetEnergy = Math.max(0, currentHelmetEnergy - amount);
        setEnergy(helmetStack, newHelmetEnergy);
    }

    /**
     * 获取护甲纹理
     */
    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/armor/hybrid_solar_helmet_1.png");
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

        tooltip.add(Component.translatable("tooltip.mio_icif.hybrid_solar_helmet.day", DAY_GENERATION / 4)
                .withStyle(net.minecraft.ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.mio_icif.hybrid_solar_helmet.night", NIGHT_GENERATION / 4)
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.mio_icif.hybrid_solar_helmet.transfer", TRANSFER_LIMIT / 4)
                .withStyle(net.minecraft.ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.mio_icif.hybrid_solar_helmet.water_breathing")
                .withStyle(net.minecraft.ChatFormatting.BLUE));
    }

    /**
     * 获取每点伤害消耗的能量
     */
    @Override
    public long getEnergyPerDamage() {
        return (int) (2000 * DAMAGE_ABSORPTION);
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