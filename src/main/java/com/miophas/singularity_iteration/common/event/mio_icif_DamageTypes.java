package com.miophas.singularity_iteration.common.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

@SuppressWarnings("null")
public class mio_icif_DamageTypes {
    public static final ResourceKey<DamageType> RADIATION = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "radiation")
    );

    // Tesla coil electric shock damage type.
public static final ResourceKey<DamageType> LASER_TOWER = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "laser_tower")
    );
public static final ResourceKey<DamageType> TESLA_COIL = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tesla_coil")
    );
}


