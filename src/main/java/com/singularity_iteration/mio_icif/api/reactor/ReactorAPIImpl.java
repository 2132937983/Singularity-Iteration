package com.singularity_iteration.mio_icif.api.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("null")
public class ReactorAPIImpl implements IReactorAPI {

    @Override
    public boolean isReactorComponent(ItemStack stack) {
        return stack.getItem() instanceof IBaseReactorComponent;
    }

    @Override
    public ReactorComponentType getComponentType(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return ReactorComponentType.OTHER;
        ReactorComponentType type = component.getComponentType();
        return type != null ? type : ReactorComponentType.OTHER;
    }

    @Override
    public int getNeutronPulseOutput(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return 0;
        return component.getNeutronPulseOutput();
    }

    @Override
    public int getHeatOutput(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return 0;
        return component.getHeatOutput();
    }

    @Override
    public int getMaxHeatStorage(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return 0;
        return component.getMaxHeatStorage();
    }

    @Override
    public int getHeatTransferEfficiency(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return 0;
        return component.getHeatTransferEfficiency();
    }

    @Override
    public boolean isMoxFuel(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return false;
        return component.isMoxFuel();
    }

    @Override
    public boolean isFuelRod(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return false;
        return component.isFuelRod();
    }

    @Override
    public Collection<ResourceLocation> getAllReactorComponentIds() {
        return BuiltInRegistries.ITEM.entrySet().stream()
            .filter(entry -> entry.getValue() instanceof IBaseReactorComponent)
            .map(entry -> entry.getKey().location())
            .collect(Collectors.toList());
    }

    // ========== 反应堆监控 API 实现 ==========

    @Override
    public boolean isReactorRunning(Level world, BlockPos pos) {
        if (world == null || pos == null) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IReactorController reactor) {
            return reactor.isRunning();
        }
        return false;
    }

    @Override
    public long getReactorCurrentHeat(Level world, BlockPos pos) {
        if (world == null || pos == null) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IReactorController reactor) {
            return reactor.getCurrentHeat();
        }
        return 0;
    }

    @Override
    public long getReactorMaxHeat(Level world, BlockPos pos) {
        if (world == null || pos == null) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IReactorController reactor) {
            return reactor.getMaxHeat();
        }
        return 0;
    }

    @Override
    public double getReactorCurrentTemperature(Level world, BlockPos pos) {
        if (world == null || pos == null) return 20.0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IReactorController reactor) {
            return reactor.getCurrentTemperature();
        }
        return 20.0;
    }

    @Override
    public long getReactorCurrentEnergyGeneration(Level world, BlockPos pos) {
        if (world == null || pos == null) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IReactorController reactor) {
            return reactor.getCurrentEnergyGeneration();
        }
        return 0;
    }

    @Override
    public int getReactorAvailableColumns(Level world, BlockPos pos) {
        if (world == null || pos == null) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IReactorController reactor) {
            return reactor.getAvailableColumns();
        }
        return 0;
    }

    @Override
    public ReactorMode getReactorMode(Level world, BlockPos pos) {
        if (world == null || pos == null) return ReactorMode.GENERATOR;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IReactorController reactor) {
            return reactor.getApiReactorMode() == ReactorMode.FLUID
                ? ReactorMode.FLUID : ReactorMode.GENERATOR;
        }
        return ReactorMode.GENERATOR;
    }

    @Override
    public ReactorStructureStatus getReactorStructureStatusDetailed(Level world, BlockPos pos) {
        if (world == null || pos == null) return ReactorStructureStatus.invalid(List.of("Invalid world or position"));
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof IReactorController reactor)) {
            return ReactorStructureStatus.invalid(List.of("Not a nuclear reactor"));
        }
        
        ReactorMode mode = getReactorMode(world, pos);
        boolean isValid;
        int chambers;
        
        if (mode == ReactorMode.FLUID) {
            isValid = reactor.isValidFluidReactorStructure();
            chambers = reactor.getSubTiles().size() - 1;
        } else {
            isValid = true;
            chambers = 0;
        }
        
        if (isValid) {
            return ReactorStructureStatus.valid(chambers);
        } else {
            return ReactorStructureStatus.invalid(List.of("Structure incomplete"));
        }
    }

    // ========== 燃料棒 API 实现 ==========

    @Override
    public int getFuelRodPulses(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return 0;
        if (!component.isFuelRod()) return 0;
        return component.getNeutronPulseOutput();
    }

    @Override
    public int getFuelRodCells(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return 0;
        if (!component.isFuelRod()) return 0;
        return component.getNumberOfCells();
    }

    @Override
    public boolean isFuelRodDepleted(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return false;
        if (!component.isFuelRod()) return false;
        return component.isDepleted(stack);
    }

    @Override
    public long getFuelRodHeatOutput(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return 0;
        if (!component.isFuelRod()) return 0;
        return component.getHeatOutput();
    }

    @Override
    public ItemStack getFuelRodDepletedItem(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return ItemStack.EMPTY;
        if (!component.isFuelRod()) return ItemStack.EMPTY;
        Item depletedItem = component.getDepletedItem();
        if (depletedItem == null) return ItemStack.EMPTY;
        return new ItemStack(depletedItem);
    }

    @Override
    public FuelRodType getFuelRodType(ItemStack stack) {
        if (!(stack.getItem() instanceof IBaseReactorComponent component)) return FuelRodType.SINGLE;
        if (!component.isFuelRod()) return FuelRodType.SINGLE;
        int cells = component.getNumberOfCells();
        return switch (cells) {
            case 4 -> FuelRodType.QUAD;
            case 2 -> FuelRodType.DUAL;
            default -> FuelRodType.SINGLE;
        };
    }
}