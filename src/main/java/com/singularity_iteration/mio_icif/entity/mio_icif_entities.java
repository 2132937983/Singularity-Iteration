package com.singularity_iteration.mio_icif.entity;

import com.singularity_iteration.mio_icif.Blocks.reactor.mio_icif_Entity_IC_TNT_Primed;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_laser_bullet;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_laser_miner;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_plasma_bullet;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_plasma_launcher;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_energy_bullet;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_electric_rifle;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_advanced_electric_rifle;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_tactical_laser_rifle;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_tachyon_disruptor;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_electric_plasma_gun;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_rocket_launcher;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_rocket_entity;
import com.singularity_iteration.mio_icif.entity.boat.mio_icif_carbon_boat;
import com.singularity_iteration.mio_icif.entity.boat.mio_icif_electric_boat;
import com.singularity_iteration.mio_icif.entity.boat.mio_icif_rubber_boat;
import com.singularity_iteration.mio_icif.entity.dynamite.mio_icif_dynamite_entity;
import com.singularity_iteration.mio_icif.entity.dynamite.mio_icif_sticky_dynamite_entity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 实体注册�? * 管理所有自定义实体的注册? */
@SuppressWarnings("null")
public class mio_icif_entities {

    // 创建实体类型的注册器
    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Singularity_Iteration.MOD_ID);

    // 注册激光子弹实�
public static final DeferredHolder<EntityType<?>, EntityType<mio_icif_laser_bullet>> LASER_BULLET =
        ENTITIES.register("laser_bullet",
            () -> EntityType.Builder.<mio_icif_laser_bullet>of(mio_icif_laser_bullet::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(4)
                .updateInterval(10)
                .build("laser_bullet"));

    // 注册等离子子弹实�
public static final DeferredHolder<EntityType<?>, EntityType<mio_icif_plasma_bullet>> PLASMA_BULLET =
        ENTITIES.register("plasma_bullet",
            () -> EntityType.Builder.<mio_icif_plasma_bullet>of(mio_icif_plasma_bullet::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(8)
                .updateInterval(10)
                 .build("plasma_bullet"));

    // 注册通用能量子弹实体
    public static final DeferredHolder<EntityType<?>, EntityType<mio_icif_energy_bullet>> ENERGY_BULLET =
        ENTITIES.register("energy_bullet",
            () -> EntityType.Builder.<mio_icif_energy_bullet>of(mio_icif_energy_bullet::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(8)
                .updateInterval(10)
                .build("energy_bullet"));

    // 注册火箭弹实体
    public static final DeferredHolder<EntityType<?>, EntityType<mio_icif_rocket_entity>> ROCKET_ENTITY =
        ENTITIES.register("rocket_entity",
            () -> EntityType.Builder.<mio_icif_rocket_entity>of(mio_icif_rocket_entity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(8)
                .updateInterval(10)
                .build("rocket_entity"));

    // 注册工业TNT点燃实体
    public static final DeferredHolder<EntityType<?>, EntityType<mio_icif_Entity_IC_TNT_Primed>> IC_TNT_PRIMED =
        ENTITIES.register("ic_tnt_primed",
            () -> EntityType.Builder.<mio_icif_Entity_IC_TNT_Primed>of(mio_icif_Entity_IC_TNT_Primed::new, MobCategory.MISC)
                .fireImmune()
                .sized(0.98F, 0.98F)
                .clientTrackingRange(10)
                .updateInterval(10)
                .build("ic_tnt_primed"));

    // 注册核弹点燃实体
    public static final DeferredHolder<EntityType<?>, EntityType<mio_icif_Entity_Nuke_Primed>> NUKE_PRIMED =
        ENTITIES.register("nuke_primed",
            () -> EntityType.Builder.<mio_icif_Entity_Nuke_Primed>of(mio_icif_Entity_Nuke_Primed::new, MobCategory.MISC)
                .fireImmune()
                .sized(0.98F, 0.98F)
                .clientTrackingRange(10)
                .updateInterval(10)
                .build("nuke_primed"));

    // 注册电动船实�
public static final DeferredHolder<EntityType<?>, EntityType<mio_icif_electric_boat>> ELECTRIC_BOAT =
        ENTITIES.register("electric_boat",
            () -> EntityType.Builder.<mio_icif_electric_boat>of(mio_icif_electric_boat::new, MobCategory.MISC)
                .sized(1.375F, 0.5625F)
                .clientTrackingRange(10)
                .build("electric_boat"));

    // 注册橡胶船实�
public static final DeferredHolder<EntityType<?>, EntityType<mio_icif_rubber_boat>> RUBBER_BOAT =
        ENTITIES.register("rubber_boat",
            () -> EntityType.Builder.<mio_icif_rubber_boat>of(mio_icif_rubber_boat::new, MobCategory.MISC)
                .sized(1.375F, 0.5625F)
                .clientTrackingRange(10)
                .build("rubber_boat"));

    // 注册碳纤维船实体
    public static final DeferredHolder<EntityType<?>, EntityType<mio_icif_carbon_boat>> CARBON_BOAT =
        ENTITIES.register("carbon_boat",
            () -> EntityType.Builder.<mio_icif_carbon_boat>of(mio_icif_carbon_boat::new, MobCategory.MISC)
                .sized(1.375F, 0.5625F)
                .clientTrackingRange(10)
                .build("carbon_boat"));

    // 注册炸药实体
    public static final DeferredHolder<EntityType<?>, EntityType<mio_icif_dynamite_entity>> DYNAMITE_ENTITY =
        ENTITIES.register("dynamite_entity",
            () -> EntityType.Builder.<mio_icif_dynamite_entity>of(mio_icif_dynamite_entity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(10)
                .updateInterval(10)
                .build("dynamite_entity"));

    // 注册粘性炸药实�
public static final DeferredHolder<EntityType<?>, EntityType<mio_icif_sticky_dynamite_entity>> STICKY_DYNAMITE_ENTITY =
        ENTITIES.register("sticky_dynamite_entity",
            () -> EntityType.Builder.<mio_icif_sticky_dynamite_entity>of(mio_icif_sticky_dynamite_entity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(10)
                .updateInterval(10)
                .build("sticky_dynamite_entity"));

    /**
     * 注册实体到事件总线
     * @param eventBus 事件总线
     */
    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);

        // 注册完成后，将实体类型设置给镭射�
    eventBus.addListener(mio_icif_entities::onCommonSetup);
    }

    /**
     * 在通用设置阶段设置实体类型
     * @param event 通用设置事件
     */
    private static void onCommonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        // 将注册的实体类型设置给镭射枪
        mio_icif_laser_miner.setLaserBulletEntity(LASER_BULLET.get());
        // 将注册的实体类型设置给等离子射线枪
        mio_icif_plasma_launcher.setPlasmaBulletEntity(PLASMA_BULLET.get());
        // 将注册的实体类型设置给各类电力步枪
        mio_icif_electric_rifle.setRifleBulletEntity(ENERGY_BULLET.get());
        mio_icif_advanced_electric_rifle.setRifleBulletEntity(ENERGY_BULLET.get());
        mio_icif_tactical_laser_rifle.setRifleBulletEntity(ENERGY_BULLET.get());
        mio_icif_electric_plasma_gun.setRifleBulletEntity(ENERGY_BULLET.get());
        mio_icif_tachyon_disruptor.setRifleBulletEntity(ENERGY_BULLET.get());
        mio_icif_rocket_launcher.setRocketEntity(ROCKET_ENTITY.get());
        Singularity_Iteration.LOGGER.info("Laser bullet entity registered and set to laser miner");
        Singularity_Iteration.LOGGER.info("Plasma bullet entity registered and set to plasma launcher");
        Singularity_Iteration.LOGGER.info("Energy bullet entity registered and set to electric rifles");
    }
}

