package com.singularity_iteration.mio_icif.api.item;

import com.singularity_iteration.mio_icif.api.reactor.IBaseReactorComponent;
import com.singularity_iteration.mio_icif.api.reactor.ReactorComponentType;
import com.singularity_iteration.mio_icif.api.upgrade.tile.IUpgradeItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 物品 API 实现
 */
@SuppressWarnings("null")
public class ItemAPIImpl implements IItemAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemAPIImpl.class);

    @Override
    public Item getItem(ResourceLocation id) {
        return BuiltInRegistries.ITEM.get(id);
    }

    @Override
    public boolean isElectricTool(ItemStack stack) {
        return stack.getItem() instanceof IElectricToolItem;
    }

    @Override
    public boolean isBattery(ItemStack stack) {
        return stack.getItem() instanceof IBatteryItem;
    }

    @Override
    public boolean isElectricArmor(ItemStack stack) {
        return stack.getItem() instanceof IElectricArmorItem;
    }

    @Override
    public boolean isReactorComponent(ItemStack stack) {
        return stack.getItem() instanceof IBaseReactorComponent;
    }

    @Override
    public boolean isUpgrade(ItemStack stack) {
        return stack.getItem() instanceof IUpgradeItem;
    }

    @Override
    public boolean isLatheItem(ItemStack stack) {
        return stack.getItem() instanceof ILatheItem;
    }

    @Override
    public boolean hasCustomDamage(ItemStack stack) {
        return stack.getItem() instanceof ICustomDamageItem;
    }

    @Override
    public int getCustomDamage(ItemStack stack) {
        if (stack.getItem() instanceof ICustomDamageItem customItem) {
            return customItem.getCustomDamage(stack);
        }
        return 0;
    }

    @Override
    public int getMaxCustomDamage(ItemStack stack) {
        if (stack.getItem() instanceof ICustomDamageItem customItem) {
            return customItem.getMaxCustomDamage(stack);
        }
        return 0;
    }

    @Override
    public long getBatteryCapacity(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.getMaxEnergy();
        }
        if (stack.getItem() instanceof IElectricToolItem tool) {
            return tool.getMaxEnergy();
        }
        if (stack.getItem() instanceof IBatteryItem bat) {
            return bat.getMaxEnergy();
        }
        return 0L;
    }

    @Override
    public long getBatteryStored(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.getEnergy(stack);
        }
        if (stack.getItem() instanceof IElectricToolItem tool) {
            return tool.getEnergy(stack);
        }
        if (stack.getItem() instanceof IBatteryItem bat) {
            return bat.getEnergy(stack);
        }
        return 0L;
    }

    @Override
    public long chargeBattery(ItemStack stack, long amount, boolean simulate) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            long stored = armor.getEnergy(stack);
            long capacity = armor.getMaxEnergy();
            long space = capacity - stored;
            long accepted = Math.min(amount, space);
            if (!simulate && accepted > 0) {
                armor.addEnergy(stack, accepted);
            }
            return accepted;
        }
        if (stack.getItem() instanceof IElectricToolItem tool) {
            long stored = tool.getEnergy(stack);
            long capacity = tool.getMaxEnergy();
            long space = capacity - stored;
            long accepted = Math.min(amount, space);
            if (!simulate && accepted > 0) {
                tool.addEnergy(stack, accepted);
            }
            return accepted;
        }
        if (stack.getItem() instanceof IBatteryItem bat) {
            long stored = bat.getEnergy(stack);
            long capacity = bat.getMaxEnergy();
            long space = capacity - stored;
            long accepted = Math.min(amount, space);
            if (!simulate && accepted > 0) {
                bat.addEnergy(stack, accepted);
            }
            return accepted;
        }
        return 0L;
    }

    @Override
    public long dischargeBattery(ItemStack stack, long amount, boolean simulate) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            long stored = armor.getEnergy(stack);
            long extracted = Math.min(amount, stored);
            if (!simulate && extracted > 0) {
                armor.extractEnergy(stack, extracted);
            }
            return extracted;
        }
        if (stack.getItem() instanceof IElectricToolItem tool) {
            long stored = tool.getEnergy(stack);
            long extracted = Math.min(amount, stored);
            if (!simulate && extracted > 0) {
                tool.extractEnergy(stack, extracted);
            }
            return extracted;
        }
        if (stack.getItem() instanceof IBatteryItem bat) {
            long stored = bat.getEnergy(stack);
            long extracted = Math.min(amount, stored);
            if (!simulate && extracted > 0) {
                bat.extractEnergy(stack, extracted);
            }
            return extracted;
        }
        return 0L;
    }

    // ========== 电动工具 API ==========

    @Override
    public long getElectricToolMaxEnergy(ItemStack stack) {
        if (stack.getItem() instanceof IElectricToolItem tool) {
            return tool.getMaxEnergy();
        }
        return 0L;
    }

    @Override
    public long getElectricToolStored(ItemStack stack) {
        if (stack.getItem() instanceof IElectricToolItem tool) {
            return tool.getEnergy(stack);
        }
        return 0L;
    }

    @Override
    public long getElectricToolEnergyPerUse(ItemStack stack) {
        if (stack.getItem() instanceof IElectricToolItem tool) {
            return tool.getEnergyPerUse();
        }
        return 0L;
    }

    @Override
    public int getElectricToolTier(ItemStack stack) {
        if (stack.getItem() instanceof IElectricToolItem tool) {
            return tool.getToolTier();
        }
        return 0;
    }

    @Override
    public long chargeElectricTool(ItemStack stack, long amount, boolean simulate) {
        if (!(stack.getItem() instanceof IElectricToolItem tool)) return 0L;
        long stored = tool.getEnergy(stack);
        long capacity = tool.getMaxEnergy();
        long space = capacity - stored;
        long accepted = Math.min(amount, space);
        if (!simulate && accepted > 0) {
            tool.addEnergy(stack, accepted);
        }
        return accepted;
    }

    @Override
    public long dischargeElectricTool(ItemStack stack, long amount, boolean simulate) {
        if (!(stack.getItem() instanceof IElectricToolItem tool)) return 0L;
        long stored = tool.getEnergy(stack);
        long extracted = Math.min(amount, stored);
        if (!simulate && extracted > 0) {
            if (extracted > Integer.MAX_VALUE) {
                LOGGER.warn("Tool energy extraction truncated from {} to {} for item {}", 
                    extracted, Integer.MAX_VALUE, stack.getItem().getDescriptionId());
            }
            tool.extractEnergy(stack, Math.min(extracted, Integer.MAX_VALUE));
        }
        return extracted;
    }

    @Override
    public boolean hasEnoughToolEnergy(ItemStack stack) {
        if (stack.getItem() instanceof IElectricToolItem tool) {
            return tool.hasEnoughEnergy(stack);
        }
        return false;
    }

    // ========== 电力装甲 API ==========

    @Override
    public long getElectricArmorMaxEnergy(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.getMaxEnergy();
        }
        return 0L;
    }

    @Override
    public long getElectricArmorStored(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.getEnergy(stack);
        }
        return 0L;
    }

    @Override
    public long getElectricArmorEnergyPerTick(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.getEnergyPerTick();
        }
        return 0L;
    }

    @Override
    public int getElectricArmorTier(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.getArmorTier();
        }
        return 0;
    }

    @Override
    public long chargeElectricArmor(ItemStack stack, long amount, boolean simulate) {
        if (!(stack.getItem() instanceof IElectricArmorItem armor)) return 0L;
        long stored = armor.getEnergy(stack);
        long capacity = armor.getMaxEnergy();
        long space = capacity - stored;
        long accepted = Math.min(amount, space);
        if (!simulate && accepted > 0) {
            armor.addEnergy(stack, accepted);
        }
        return accepted;
    }

    @Override
    public long dischargeElectricArmor(ItemStack stack, long amount, boolean simulate) {
        if (!(stack.getItem() instanceof IElectricArmorItem armor)) return 0L;
        long stored = armor.getEnergy(stack);
        long extracted = Math.min(amount, stored);
        if (!simulate && extracted > 0) {
            if (extracted > Integer.MAX_VALUE) {
                LOGGER.warn("Armor energy extraction truncated from {} to {} for item {}", 
                    extracted, Integer.MAX_VALUE, stack.getItem().getDescriptionId());
            }
            armor.extractEnergy(stack, Math.min(extracted, Integer.MAX_VALUE));
        }
        return extracted;
    }

    @Override
    public boolean hasEnoughArmorEnergy(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.hasEnoughEnergy(stack);
        }
        return false;
    }

    @Override
    public boolean consumeArmorEnergy(ItemStack stack, long amount) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.consumeEnergy(stack, amount);
        }
        return false;
    }

    // ========== 反应堆组件 API ==========

    @Override
    public String getReactorComponentType(ItemStack stack) {
        if (stack.getItem() instanceof IBaseReactorComponent component) {
            ReactorComponentType type = component.getComponentType();
            if (type != null) {
                return type.name();
            }
        }
        return null;
    }

    // ========== 升级插件 API ==========

    @Override
    public String getUpgradeType(ItemStack stack) {
        if (stack.getItem() instanceof IUpgradeItem upgrade) {
            return upgrade.getUpgradeTypeName();
        }
        return null;
    }

    @Override
    public boolean isUpgradeDirectional(ItemStack stack) {
        if (stack.getItem() instanceof IUpgradeItem upgrade) {
            return upgrade.isDirectional();
        }
        return false;
    }

    @Override
    public net.minecraft.core.Direction getUpgradeDirection(ItemStack stack) {
        if (stack.getItem() instanceof IUpgradeItem upgrade) {
            if (upgrade.isDirectional()) {
                net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                net.minecraft.nbt.CompoundTag tag = customData != null ? customData.copyTag() : new net.minecraft.nbt.CompoundTag();
                if (tag.contains("dir")) {
                    int rawDir = tag.getByte("dir");
                    if (rawDir >= 1 && rawDir <= 6) {
                        return net.minecraft.core.Direction.values()[rawDir - 1];
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Collection<ResourceLocation> getAllItemIds() {
        return BuiltInRegistries.ITEM.keySet().stream()
            .filter(id -> id.getNamespace().equals("mio_icif"))
            .collect(Collectors.toList());
    }

    // ========== 电池充放电速率 API 实现 ==========

    @Override
    public long getChargeRate(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.getChargeRate(stack);
        }
        if (stack.getItem() instanceof IBatteryItem bat) {
            return bat.getChargeRate(stack);
        }
        if (stack.getItem() instanceof IElectricToolItem) {
            return 100;
        }
        return 0;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.getEnergyPerTick();
        }
        if (stack.getItem() instanceof IBatteryItem bat) {
            return bat.getChargeRate(stack);
        }
        if (stack.getItem() instanceof IElectricToolItem tool) {
            return tool.getEnergyPerUse();
        }
        return 0;
    }

    @Override
    public boolean isBatteryFull(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.isFull(stack);
        }
        if (stack.getItem() instanceof IElectricToolItem tool) {
            return tool.getEnergy(stack) >= tool.getMaxEnergy();
        }
        if (stack.getItem() instanceof IBatteryItem bat) {
            return bat.isFull(stack);
        }
        return false;
    }

    @Override
    public boolean isBatteryEmpty(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.isEmpty(stack);
        }
        if (stack.getItem() instanceof IElectricToolItem tool) {
            return tool.getEnergy(stack) <= 0;
        }
        if (stack.getItem() instanceof IBatteryItem bat) {
            return bat.isEmpty(stack);
        }
        return true;
    }

    // ========== 升级插件效果 API 实现 ==========

    @Override
    public float getUpgradeSpeedBonus(ItemStack stack) {
        if (stack.getItem() instanceof IUpgradeItem upgrade) {
            return upgrade.getSpeedBonus();
        }
        return 1.0f;
    }

    @Override
    public float getUpgradeEnergyBonus(ItemStack stack) {
        if (stack.getItem() instanceof IUpgradeItem upgrade) {
            return upgrade.getEnergyBonus();
        }
        return 1.0f;
    }

    @Override
    public int getUpgradeTier(ItemStack stack) {
        if (stack.getItem() instanceof IUpgradeItem upgrade) {
            return upgrade.getUpgradeTier(stack);
        }
        return 0;
    }

    // ========== 新物品类型查询 API ==========

    @Override
    public boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof IWeaponItem;
    }

    @Override
    public boolean isSolarHelmet(ItemStack stack) {
        return stack.getItem() instanceof ISolarHelmetItem;
    }

    @Override
    public boolean isJetpack(ItemStack stack) {
        return stack.getItem() instanceof IJetpackItem;
    }

    @Override
    public boolean isFluidCell(ItemStack stack) {
        return stack.getItem() instanceof IFluidCellItem;
    }

    @Override
    public float getWeaponDamage(ItemStack stack) {
        if (stack.getItem() instanceof IWeaponItem weapon) {
            return weapon.getDamage();
        }
        return 0.0F;
    }

    @Override
    public float getWeaponRange(ItemStack stack) {
        if (stack.getItem() instanceof IWeaponItem weapon) {
            return weapon.getRange();
        }
        return 0.0F;
    }

    @Override
    public long getWeaponEnergyPerShot(ItemStack stack) {
        if (stack.getItem() instanceof IWeaponItem weapon) {
            return weapon.getEnergyPerShot();
        }
        return 0L;
    }

    @Override
    public long getSolarGenerationRate(ItemStack stack) {
        if (stack.getItem() instanceof ISolarHelmetItem helmet) {
            return helmet.getGenerationRate();
        }
        return 0L;
    }

    @Override
    public float getJetpackThrust(ItemStack stack) {
        if (stack.getItem() instanceof IJetpackItem jetpack) {
            return jetpack.getThrust();
        }
        return 0.0F;
    }

    @Override
    public long getJetpackEnergyPerTick(ItemStack stack) {
        if (stack.getItem() instanceof IJetpackItem jetpack) {
            return jetpack.getEnergyPerTickFlying();
        }
        return 0L;
    }

    @Override
    public IJetpackItem.JetpackMode getJetpackMode(ItemStack stack) {
        if (stack.getItem() instanceof IJetpackItem jetpack) {
            return jetpack.getMode(stack);
        }
        return IJetpackItem.JetpackMode.OFF;
    }

    @Override
    public void setJetpackMode(ItemStack stack, IJetpackItem.JetpackMode mode) {
        if (stack.getItem() instanceof IJetpackItem jetpack) {
            jetpack.setMode(stack, mode);
        }
    }

    @Override
    public int getFluidCellCapacity(ItemStack stack) {
        if (stack.getItem() instanceof IFluidCellItem cell) {
            return cell.getCapacity();
        }
        return 0;
    }

    @Override
    public net.neoforged.neoforge.fluids.FluidStack getFluidCellContent(ItemStack stack) {
        if (stack.getItem() instanceof IFluidCellItem cell) {
            return cell.getFluid(stack);
        }
        return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
    }

    @Override
    public boolean canFluidCellHold(ItemStack stack, net.minecraft.world.level.material.Fluid fluid) {
        if (stack.getItem() instanceof IFluidCellItem cell) {
            return cell.canHoldFluid(fluid);
        }
        return false;
    }

    @Override
    public int fillFluidCell(ItemStack stack, net.neoforged.neoforge.fluids.FluidStack fluid, boolean simulate) {
        if (stack.getItem() instanceof IFluidCellItem cell) {
            return cell.fill(stack, fluid, simulate);
        }
        return 0;
    }

    @Override
    public net.neoforged.neoforge.fluids.FluidStack drainFluidCell(ItemStack stack, int amount, boolean simulate) {
        if (stack.getItem() instanceof IFluidCellItem cell) {
            return cell.drain(stack, amount, simulate);
        }
        return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
    }

    @Override
    public ItemStack getFluidCellEmptyContainer(ItemStack stack) {
        if (stack.getItem() instanceof IFluidCellItem cell) {
            return cell.getEmptyContainer(stack);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getFluidCellFilledContainer(ItemStack stack, net.minecraft.world.level.material.Fluid fluid) {
        if (stack.getItem() instanceof IFluidCellItem cell) {
            return cell.getFilledContainer(stack, fluid);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamicFluidCell(ItemStack stack) {
        return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Cell.mio_icif_dynamic_cell;
    }

    @Override
    public ItemStack createDynamicFluidCell(net.minecraft.world.level.material.Fluid fluid, int amount) {
        if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) {
            return ItemStack.EMPTY;
        }
        
        var dynamicCellItem = com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells.CELL_EMPTY.get();
        if (dynamicCellItem == null) {
            return ItemStack.EMPTY;
        }
        
        ItemStack stack = new ItemStack(dynamicCellItem);
        int fillAmount = Math.min(amount, dynamicCellItem.getCapacity());
        
        if (dynamicCellItem instanceof com.singularity_iteration.mio_icif.Items.Cell.mio_icif_dynamic_cell dynamicCell) {
            dynamicCell.writeFluidToNBT(stack, new net.neoforged.neoforge.fluids.FluidStack(fluid, fillAmount));
        }
        
        return stack;
    }

    // ========== 装甲特性 API 实现 ==========

    @Override
    public java.util.List<ArmorFeatureInfo> getArmorFeatures(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.getFeatures(stack);
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean toggleArmorFeature(ItemStack stack, String featureKey) {
        return com.singularity_iteration.mio_icif.Items.Armor.ArmorFeatureToggle.toggle(stack, featureKey);
    }

    @Override
    public long getEnergyPerDamage(ItemStack stack) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.getEnergyPerDamage();
        }
        return 0L;
    }

    @Override
    public float getDamageAbsorptionRatio(ItemStack stack, EquipmentSlot slot) {
        if (stack.getItem() instanceof IElectricArmorItem armor) {
            return armor.getDamageAbsorptionRatio(slot);
        }
        return 0.0F;
    }

    // ========== 能量设置 API 实现 ==========

    @Override
    public void setBatteryEnergy(ItemStack stack, long energy) {
        if (stack.getItem() instanceof IBatteryItem bat) {
            bat.setEnergy(stack, energy);
        }
    }

    @Override
    public void setElectricToolEnergy(ItemStack stack, long energy) {
        if (stack.getItem() instanceof IElectricToolItem tool) {
            tool.setEnergy(stack, energy);
        }
    }
}