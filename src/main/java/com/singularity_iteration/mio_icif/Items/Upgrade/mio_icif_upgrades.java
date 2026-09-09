package com.singularity_iteration.mio_icif.Items.Upgrade;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 工业2升级插件注册�? */
@SuppressWarnings("null")
public class mio_icif_upgrades {

    public static final DeferredRegister.Items UPGRADES = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    // 超频升级
    public static final DeferredItem<mio_icif_upgrade> OVERCLOCKER_UPGRADE = UPGRADES.register("upgrade/overclocker_upgrade",
        () -> new mio_icif_upgrade(new net.minecraft.world.item.Item.Properties(), mio_icif_upgrade.UpgradeType.OVERCLOCKER));

    // 能量储存升级
    public static final DeferredItem<mio_icif_upgrade> ENERGY_STORAGE_UPGRADE = UPGRADES.register("upgrade/energy_storage_upgrade",
        () -> new mio_icif_upgrade(new net.minecraft.world.item.Item.Properties(), mio_icif_upgrade.UpgradeType.ENERGY_STORAGE));

    // 变压器升级
public static final DeferredItem<mio_icif_upgrade> TRANSFORMER_UPGRADE = UPGRADES.register("upgrade/transformer_upgrade",
        () -> new mio_icif_upgrade(new net.minecraft.world.item.Item.Properties(), mio_icif_upgrade.UpgradeType.TRANSFORMER));

    // 弹出升级
    public static final DeferredItem<mio_icif_upgrade> EJECTOR_UPGRADE = UPGRADES.register("upgrade/ejector_upgrade",
        () -> new mio_icif_upgrade(new net.minecraft.world.item.Item.Properties(), mio_icif_upgrade.UpgradeType.EJECTOR));

    // 抽取升级
    public static final DeferredItem<mio_icif_upgrade> PULLING_UPGRADE = UPGRADES.register("upgrade/pulling_upgrade",
        () -> new mio_icif_upgrade(new net.minecraft.world.item.Item.Properties(), mio_icif_upgrade.UpgradeType.PULLING));

    // 流体弹出升级（对�?mcmod item/5409�
public static final DeferredItem<mio_icif_upgrade> FLUID_EJECTOR_UPGRADE = UPGRADES.register("upgrade/fluid_ejector_upgrade",
        () -> new mio_icif_upgrade(new net.minecraft.world.item.Item.Properties(), mio_icif_upgrade.UpgradeType.FLUID_EJECTOR));

    // 红石信号反转升级
    public static final DeferredItem<mio_icif_upgrade> REDSTONE_INVERTER_UPGRADE = UPGRADES.register("upgrade/redstone_inv_upgrade",
        () -> new mio_icif_upgrade(new net.minecraft.world.item.Item.Properties(), mio_icif_upgrade.UpgradeType.REDSTONE_INVERTER));

    // 流体抽入升级（对�?mcmod item/37497，新增）
    public static final DeferredItem<mio_icif_upgrade> FLUID_PULLING_UPGRADE = UPGRADES.register("upgrade/fluid_pulling_upgrade",
        () -> new mio_icif_upgrade(new net.minecraft.world.item.Item.Properties(), mio_icif_upgrade.UpgradeType.FLUID_PULLING));

    public static void register(IEventBus eventBus) {
        UPGRADES.register(eventBus);
    }
}


