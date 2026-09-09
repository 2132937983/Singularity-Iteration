package com.singularity_iteration.mio_icif.api.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 流体 API 实现
 *
 * <p>所有流体通过 {@link BuiltInRegistries#FLUID} 按 {@link ResourceLocation} 查找，
 * 不直接引用内部注册表持有者（如 {@code mio_icif_fluids}），避免内部重构影响 API 层。
 */
public class FluidAPIImpl implements IFluidAPI {

    private static final String MOD_ID = "mio_icif";

    private Fluid getModFluid(String name) {
        return BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(MOD_ID, name));
    }

    @Override
    public Fluid getFluid(ResourceLocation id) {
        return BuiltInRegistries.FLUID.get(id);
    }

    @Override
    public Fluid getBiogas() {
        return getModFluid("biogas");
    }

    @Override
    public Fluid getHotWater() {
        return getModFluid("hotwater");
    }

    @Override
    public Fluid getBiomass() {
        return getModFluid("biomass");
    }

    @Override
    public Fluid getConstructionFoam() {
        return getModFluid("constructionfoam");
    }

    @Override
    public Fluid getCoolant() {
        return getModFluid("coolant");
    }

    @Override
    public Fluid getDistilledWater() {
        return getModFluid("distilledwater");
    }

    @Override
    public Fluid getHotCoolant() {
        return getModFluid("hotcoolant");
    }

    @Override
    public Fluid getPahoehoeLava() {
        return getModFluid("pahoehoelava");
    }

    @Override
    public Fluid getSteam() {
        return getModFluid("steam");
    }

    @Override
    public Fluid getSuperheatedSteam() {
        return getModFluid("superheatedsteam");
    }

    @Override
    public Fluid getUUMatter() {
        return getModFluid("uumatter");
    }

    @Override
    public Fluid getAir() {
        return getModFluid("air");
    }

    @Override
    public Collection<ResourceLocation> getAllFluidIds() {
        return BuiltInRegistries.FLUID.keySet().stream()
            .filter(id -> id.getNamespace().equals(MOD_ID))
            .collect(Collectors.toList());
    }

    @Override
    public boolean isGasFluid(Fluid fluid) {
        if (fluid instanceof GasFluidSource || fluid instanceof GasFluidFlowing) {
            return true;
        }
        return fluid == getBiogas() || fluid == getSteam()
            || fluid == getSuperheatedSteam() || fluid == getAir();
    }
}