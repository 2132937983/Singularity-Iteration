package com.singularity_iteration.mio_icif.event;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
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

    // 特斯拉线圈电击伤害类�
public static final ResourceKey<DamageType> TESLA_COIL = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tesla_coil")
    );
}


