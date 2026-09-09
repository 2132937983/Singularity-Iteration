package com.singularity_iteration.mio_icif.Items.Upgrade;

import com.singularity_iteration.mio_icif.api.upgrade.tile.IUpgradeItem;
import com.singularity_iteration.mio_icif.api.upgrade.tile.IUpgradableBlock;
import com.singularity_iteration.mio_icif.api.upgrade.tile.UpgradableProperty;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@SuppressWarnings("null")
public class mio_icif_upgrade extends Item implements IUpgradeItem {

    private static final String NBT_KEY_DIR = "dir";

    private final UpgradeType type;

    public mio_icif_upgrade(Properties properties, UpgradeType type) {
        super(properties);
        this.type = type;
    }

    public UpgradeType getLocalUpgradeType() {
        return type;
    }

    @Override
    public String getUpgradeTypeName() {
        return type.getName();
    }

    @Override
    public com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType getApiUpgradeType() {
        return switch (type) {
            case OVERCLOCKER -> com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.OVERCLOCKER;
            case TRANSFORMER -> com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.TRANSFORMER;
            case ENERGY_STORAGE -> com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.ENERGY_STORAGE;
            case EJECTOR -> com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.EJECTOR;
            case PULLING -> com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.IMPORT;
            case FLUID_EJECTOR -> com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.FLUID_EJECTOR;
            case FLUID_PULLING -> com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.FLUID_IMPORT;
            case REDSTONE_INVERTER -> com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.REDSTONE_SIGNAL;
            default -> com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.CUSTOM;
        };
    }

    @Override
    public boolean isDirectional() {
        return type.directional;
    }

    @Override
    public float getSpeedBonus() {
        if (type == UpgradeType.OVERCLOCKER) {
            return 1.428f;
        }
        return 1.0f;
    }

    @Override
    public float getEnergyBonus() {
        if (type == UpgradeType.OVERCLOCKER) {
            return 1.6f;
        }
        if (type == UpgradeType.ENERGY_STORAGE) {
            return 0.9f;
        }
        return 1.0f;
    }

    @Override
    public int getUpgradeTier(ItemStack stack) {
        try {
            var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (customData != null) {
                var nbt = customData.copyTag();
                if (nbt.contains("tier", net.minecraft.nbt.Tag.TAG_BYTE)) {
                    return nbt.getByte("tier");
                }
                if (nbt.contains("tier", net.minecraft.nbt.Tag.TAG_INT)) {
                    return nbt.getInt("tier");
                }
            }
        } catch (Exception e) {
        }
        return 1;
    }

    @Override
    public boolean hasAutoEject() {
        return type == UpgradeType.EJECTOR || type == UpgradeType.FLUID_EJECTOR;
    }

    @Override
    public boolean hasAutoImport() {
        return type == UpgradeType.PULLING || type == UpgradeType.FLUID_PULLING;
    }

    @Override
    public boolean hasRedstoneControl() {
        return type == UpgradeType.REDSTONE_INVERTER;
    }

    @Override
    public long getExtraCapacity() {
        if (type == UpgradeType.ENERGY_STORAGE) {
            return 10000;
        }
        return 0;
    }

    @Override
    public int getExtraOutputSlots() {
        return 0;
    }

    @Override
    public int getTierUpgrade() {
        if (type == UpgradeType.TRANSFORMER) {
            return 1;
        }
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.mio_icif.upgrade." + type.getName() + ".tooltip"));

        if (type.directional) {
            Direction dir = getDirection(stack);
            if (dir == null) {
                tooltipComponents.add(Component.translatable("item.mio_icif.upgrade.direction.anyside"));
            } else {
                tooltipComponents.add(Component.translatable("item.mio_icif.upgrade.direction.set",
                    Component.translatable("item.mio_icif.upgrade.direction." + dir.getName())));
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!type.directional) return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Direction side = context.getClickedFace();
        ItemStack stack = context.getItemInHand();
        int dir = 1 + side.ordinal();

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = customData.copyTag();

        if (nbt.getByte(NBT_KEY_DIR) == dir) {
            nbt.putByte(NBT_KEY_DIR, (byte) 0);
        } else {
            nbt.putByte(NBT_KEY_DIR, (byte) dir);
        }

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

        if (context.getLevel().isClientSide()) {
            Direction currentDir = getDirection(stack);
            if (currentDir == null) {
                player.displayClientMessage(
                    Component.translatable("item.mio_icif.upgrade." + type.getName() + ".direction",
                        Component.translatable("item.mio_icif.upgrade.direction.anyside")), true);
            } else {
                player.displayClientMessage(
                    Component.translatable("item.mio_icif.upgrade." + type.getName() + ".direction",
                        Component.translatable("item.mio_icif.upgrade.direction." + currentDir.getName())), true);
            }
        }

        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    public static Direction getDirection(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) return null;
        CompoundTag nbt = customData.copyTag();
        int rawDir = nbt.getByte(NBT_KEY_DIR);
        if (rawDir < 1 || rawDir > 6) return null;
        return Direction.values()[rawDir - 1];
    }

    @SuppressWarnings("null")
    public enum UpgradeType {
        OVERCLOCKER("overclocker", false),
        ENERGY_STORAGE("energy_storage", false),
        TRANSFORMER("transformer", false),
        EJECTOR("ejector", true),
        PULLING("pulling", true),
        FLUID_EJECTOR("fluid_ejector", true),
        REDSTONE_INVERTER("redstone_inverter", false),
        FLUID_PULLING("fluid_pulling", true);

        private final String name;
        public final boolean directional;

        UpgradeType(String name, boolean directional) {
            this.name = name;
            this.directional = directional;
        }

        public String getName() {
            return name;
        }
    }

    // ==================== IUpgradeItem 接口实现 ====================

    @Override
    public boolean isSuitableFor(ItemStack stack, Set<UpgradableProperty> properties) {
        // 根据升级类型检查是否适用于机器的属性
        return switch (type) {
            case OVERCLOCKER -> properties.contains(UpgradableProperty.PROCESSING);
            case ENERGY_STORAGE -> properties.contains(UpgradableProperty.ENERGY_STORAGE);
            case TRANSFORMER -> properties.contains(UpgradableProperty.TRANSFORMER);
            case EJECTOR, FLUID_EJECTOR -> properties.contains(UpgradableProperty.ITEM_PRODUCING);
            case PULLING, FLUID_PULLING -> properties.contains(UpgradableProperty.ITEM_CONSUMING);
            case REDSTONE_INVERTER -> properties.contains(UpgradableProperty.REDSTONE_SENSITIVE);
        };
    }

    @Override
    public boolean onTick(ItemStack stack, IUpgradableBlock block) {
        // 升级物品的 tick 逻辑由机器的 MachineUpgradeStats 处理
        // 这里不需要额外逻辑
        return false;
    }

    @Override
    public Collection<ItemStack> onProcessEnd(ItemStack stack, IUpgradableBlock block, Collection<ItemStack> outputs) {
        // 超频升级不修改输出，由机器逻辑处理
        return outputs;
    }
}