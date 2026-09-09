package com.singularity_iteration.mio_icif.Items.Reactor;

import com.singularity_iteration.mio_icif.Items.DataComponent.FuelRodDurability;
import com.singularity_iteration.mio_icif.Items.DataComponent.ReactorComponentData;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_data_components;
import com.singularity_iteration.mio_icif.api.reactor.IBaseReactorComponent;
import com.singularity_iteration.mio_icif.api.reactor.ReactorComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * ?��??��?��?��??????���?
 * 
 * ?��??��??
 * - ?��??��?��??度�??使用寿命�?
 * - ?????�核??��?��???��??��?��??�??????��?��???��??��???????�却?��等�?��?�继?��此�??
 * - ?��????��?��??��?��??中�?�周?��??��??交换?��????��??/?��?��中�?��?��?��???��??��?��??
 * - ?????��?�使?��DataComponent存�?��?��??度�?�使?��?????��?�使?��次数????????��?�可以�?????
 */
@SuppressWarnings("null")
public class mio_icif_reactor extends Item implements IBaseReactorComponent {
    
    // ???大�?��??度�??使用寿命�?
    protected final int maxDurability;

    // ?��?��使用FuelRodDurability DataComponent存�?��?��??度�???????��?�使?���?
    protected final boolean useDataComponentDurability;

    // ?��?��使用ReactorComponentData DataComponent存�?�状???�??��??��???????�交?��?��等使?���?
    protected final boolean useReactorComponentData;
    
    // ==================== ??��?��??件类??? ====================
    
    // ???件类???
    protected final ReactorComponentType componentType;
    
    // ==================== 中�?��?��?�相??? ====================
    
    // ??��?�周??��?????中�?��?��?�数据??????��?�使?���?
    protected final int neutronPulseOutput;
    
    // ?��?��??�接?��中�?��?��??
    protected final boolean canReceiveNeutronPulse;
    
    // ?��?��中�?��?��?��?��????��?��??�?MOX?????��?��?�根?��???温�?��?��?�电力?
    protected final boolean isMoxFuel;
    
    // ==================== ??��?�相??? ====================
    
    // 产�?��????��?��???????��?�为�?，散??��??为�?�表示吸??��?��?��??
    protected final int heatOutput;
    
    // ???大�?��?��?��?��???��??��???????�却?��使用�?
    protected final int maxHeatStorage;
    
    // ??��?��?��?��?????�???�相??��??事件?��?��?��?��????��?��??-100�?
    protected final int heatTransferEfficiency;
    
    // ==================== ?????�函???====================
    
    /**
     * ?���??????�函???
     * @param properties ??��??属性??
     * @param maxDurability ???大�?��??�?
     */
    public mio_icif_reactor(Properties properties, int maxDurability) {
        this(properties, maxDurability, ReactorComponentType.OTHER);
    }
    
    /**
     * 带类??��???????�函???
     * @param properties ??��??属性??
     * @param maxDurability ???大�?��??�?
     * @param componentType ???件类???
     */
    public mio_icif_reactor(Properties properties, int maxDurability, ReactorComponentType componentType) {
        super(properties.durability(maxDurability));
        this.maxDurability = maxDurability;
        this.componentType = componentType;
        this.useDataComponentDurability = false;
        this.useReactorComponentData = false;
        
        // 默认中�?��?��?��?��??
        this.neutronPulseOutput = 0;
        this.canReceiveNeutronPulse = false;
        this.isMoxFuel = false;
        
        // 默认??��?��?��??
        this.heatOutput = 0;
        this.maxHeatStorage = 0;
        this.heatTransferEfficiency = 0;
    }

    /**
     * 带DataComponent??��??度�?�项????????�函???
     * ?????��?�使?��FuelRodDurability�???��?��??件使?��ReactorComponentData
     * @param properties ??��??属性??
     * @param maxDurability ???大�?��??�?
     * @param componentType ???件类???
     * @param useDataComponentDurability ?��?��使用FuelRodDurability DataComponent�??????��?��??
     * @param useReactorComponentData ?��?��使用ReactorComponentData DataComponent�???��?��??事件??
     */
    protected mio_icif_reactor(Properties properties, int maxDurability, ReactorComponentType componentType, boolean useDataComponentDurability, boolean useReactorComponentData) {
        super((useDataComponentDurability || useReactorComponentData) ? properties.stacksTo(64) : properties.durability(maxDurability));
        this.maxDurability = maxDurability;
        this.componentType = componentType;
        this.useDataComponentDurability = useDataComponentDurability;
        this.useReactorComponentData = useReactorComponentData;
        
        // 默认中�?��?��?��?��??
        this.neutronPulseOutput = 0;
        this.canReceiveNeutronPulse = false;
        this.isMoxFuel = false;
        
        // 默认??��?��?��??
        this.heatOutput = 0;
        this.maxHeatStorage = 0;
        this.heatTransferEfficiency = 0;
    }
    
    /**
     * 完整?????�函?���??��于�????��?��??
     * @param properties ??��??属性??
     * @param maxDurability ???大�?��??�?
     * @param componentType ???件类???
     * @param neutronPulseOutput ??��?????中�?��?��?�数
     * @param canReceiveNeutronPulse ?��?��??�接?��中�?��?��??
     * @param isMoxFuel ?��?��?��MOX??????
     * @param heatOutput 产�?��????��??
     * @param maxHeatStorage ???大�?��?��?��??
     * @param heatTransferEfficiency ??��?��?��?��?????
     */
    public mio_icif_reactor(Properties properties, int maxDurability, 
                            ReactorComponentType componentType,
                            int neutronPulseOutput, boolean canReceiveNeutronPulse, boolean isMoxFuel,
                            int heatOutput, int maxHeatStorage, int heatTransferEfficiency) {
        super(properties.durability(maxDurability));
        this.maxDurability = maxDurability;
        this.componentType = componentType;
        this.useDataComponentDurability = false;
        this.useReactorComponentData = false;
        this.neutronPulseOutput = neutronPulseOutput;
        this.canReceiveNeutronPulse = canReceiveNeutronPulse;
        this.isMoxFuel = isMoxFuel;
        this.heatOutput = heatOutput;
        this.maxHeatStorage = maxHeatStorage;
        this.heatTransferEfficiency = heatTransferEfficiency;
    }
    
    /**
     * ?��??��??大�?��??�?
     */
    public int getMaxDurability() {
        return maxDurability;
    }
    
    // ==================== ???件类??�相??�方法? ====================
    
    /**
     * ?��??��??件类???
     */
    @Override
    public ReactorComponentType getComponentType() {
        return componentType;
    }
    
    /**
     * ?��?��?��?????��??
     */
    @Override
    public boolean isFuelRod() {
        return componentType == ReactorComponentType.FUEL_ROD;
    }
    
    /**
     * ?��?��?��?��??��??
     */
    public boolean isHeatSink() {
        return componentType == ReactorComponentType.HEAT_SINK;
    }
    
    /**
     * ?��?��?��??�却?��
     */
    public boolean isCooler() {
        return componentType == ReactorComponentType.COOLER;
    }
    
    /**
     * ?��?��?��中�?��?��???��
     */
    public boolean isNeutronReflector() {
        return componentType == ReactorComponentType.NEUTRON_REFLECTOR;
    }
    
    /**
     * ?���U�ƴΪ��m?�]?�m/�șm/�|�m�^�C
     * ��W�q?��^ 1�]?�m�^�C�l�W�]�p�ֿU�ƴΡ^�i�Пª�^�b�̙m?�C
     *
     * @return �m?�]1=?�m, 2=�șm, 4=�|�m�^
     */
    public int getNumberOfCells() {
        return 1;
    }
    
    // ==================== 中�?��?��?�相??�方法? ====================
    
    /**
     * ?��??��?��?�周??��?????中�?��?��?��??
     */
    @Override
    public int getNeutronPulseOutput() {
        return neutronPulseOutput;
    }
    
    /**
     * ?��?��??�接?��中�?��?��??
     */
    public boolean canReceiveNeutronPulse() {
        return canReceiveNeutronPulse;
    }
    
    /**
     * ?��?��?��MOX?????��??会根?��???温�?��?��?�电力?
     */
    @Override
    public boolean isMoxFuel() {
        return isMoxFuel;
    }
    
    /**
     * ?��?��中�?��?��?��?��??�????
     * 子类?��以�?��?�此?��法来实现?��殊�????��??�?MOX?????�根?��???温�?��?��?�电力?
     * @param stack ??��?????
     * @param pulseCount ?��?��??��??中�?��?��?��??
     * @param reactorHeat 当�?��?��?��????��??
     * @param maxReactorHeat ??��?��?????大�?��??
     * @return �??????��??额�?��?�电??��?�EU/tick）?
     */
    public int onReceiveNeutronPulse(ItemStack stack, int pulseCount, int reactorHeat, int maxReactorHeat) {
        // 默认不产??��?��?��?�电力?子类?��以�?��??
        return 0;
    }
    
    // ==================== ??��?�相??�方法? ====================
    
    /**
     * ?��??�产??��????��?��???????��?�为�?，散??��??为�?�表示吸??��?��?��??
     */
    @Override
    public int getHeatOutput() {
        return heatOutput;
    }
    
    /**
     * ?��??��??大�?��?��?��?��???��??��???????�却?��使用�?
     */
    @Override
    public int getMaxHeatStorage() {
        return maxHeatStorage;
    }
    
    /**
     * ?��??��?��?��?��?��?????�???�相??��??事件?��?��?��?��????��?��??-100�?
     */
    @Override
    public int getHeatTransferEfficiency() {
        return heatTransferEfficiency;
    }
    
    /**
     * ?��?��??��?��???��??��???????�却?��使用�?
     * @param stack ??��?????
     * @param heatToAbsorb �??��?��?????��??
     * @return 实�???��?��?????��??
     */
    public int absorbHeat(ItemStack stack, int heatToAbsorb) {
        // 默认实现有?子类?��以�?��??
        // ?��??��???????�却?��应该??��?�此?��法来实现?��??��?��??
        return 0;
    }
    
    /**
     * ?��??��?��?��?��?��????��??
     * @param stack ??��?????
     * @return 当�?��?��??
     */
    public int getStoredHeat(ItemStack stack) {
        // 默认返回??0，�?��?��?��?��?��?????事件??�??��??��??）�?�该??��?�此?���?
        return 0;
    }
    
    /**
     * 设置存�?��????��??
     * @param stack ??��?????
     * @param heat ??��?��??
     */
    public void setStoredHeat(ItemStack stack, int heat) {
        // 默认空气?�现，�?��?��?��?��?��?????事件?�该??��?�此?���?
    }
    
    /**
     * ??�相??��??事件?��?��?��??
     * @param stack ??��?????
     * @param heatToTransfer �?传�?��????��??
     * @return 实�??传�?��????��??
     */
    public int transferHeatToAdjacent(ItemStack stack, int heatToTransfer) {
        // ?��?��??��?��?��?��?????计算?��?��??传�?��????��??
        int actualTransfer = (int) (heatToTransfer * heatTransferEfficiency / 100.0);
        return actualTransfer;
    }
    
    /**
     * ?��??��?��?��?��??�?
     */
    public int getCurrentDurability(ItemStack stack) {
        if (useDataComponentDurability) {
            FuelRodDurability data = stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            return data != null ? data.remainingUses() : maxDurability;
        }
        if (useReactorComponentData) {
            ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
            return data != null ? data.getRemaining() : maxDurability;
        }
        return stack.getMaxDamage() - stack.getDamageValue();
    }
    
    /**
     * �???��?��??�?
     * @param stack ??��?????
     * @param amount �???��??
     * @return ?��?���???��?��??
     */
    public boolean damageItem(ItemStack stack, int amount) {
        if (useDataComponentDurability) {
            FuelRodDurability data = stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            if (data == null) {
                return true;
            }
            int newRemaining = data.remainingUses() - amount;
            if (newRemaining <= 0) {
                stack.set(mio_icif_data_components.FUEL_ROD_DURABILITY.get(), data.withRemainingUses(0));
                return true;
            } else {
                stack.set(mio_icif_data_components.FUEL_ROD_DURABILITY.get(), data.withRemainingUses(newRemaining));
                return false;
            }
        }
        if (useReactorComponentData) {
            ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
            if (data == null) {
                return true;
            }
            int newStored = data.storedValue() + amount;
            if (newStored >= data.maxValue()) {
                stack.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), data.withStoredValue(data.maxValue()));
                return true;
            } else {
                stack.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), data.withStoredValue(newStored));
                return false;
            }
        }
        int newDamage = stack.getDamageValue() + amount;
        if (newDamage >= stack.getMaxDamage()) {
            stack.setDamageValue(stack.getMaxDamage());
            return true;
        } else {
            stack.setDamageValue(newDamage);
            return false;
        }
    }
    
    /**
     * ?��?��??��?��???��??��???????�却?��使用�?
     */
    @Override
    public boolean isDepleted(ItemStack stack) {
        if (useDataComponentDurability) {
            FuelRodDurability data = stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            return data == null || data.isDepleted();
        }
        if (useReactorComponentData) {
            ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
            return data == null || data.isDepleted();
        }
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    /**
     * ?��??��?��????�尽??��???��竭�?��?��?��??
     * 子类?��??��?�此?��法以返回?�对应�???��竭�?��??�?�??��竭�????��?��??
     * @return ?��竭�?��?��?��??�?�???��?��?�在??��?��?�null
     */
    @Override
    @Nullable
    public Item getDepletedItem() {
        return null;
    }

    /**
     * �??��??��???��?��??��??�??��于散??��???????�却??��??�?
     * 默认实现：�????��?��??度�?�尽??��??为�?��??
     * @param stack ??��?????
     * @return ?��?��??��??
     */
    public boolean isMelted(ItemStack stack) {
        return isDepleted(stack);
    }

    /**
     * �??��??��???��?��应该??��??�??��于散??��???????�却??��??�?
     * 默认实现：�?��?�false，表示�?��?��?��??
     * @param stack ??��?????
     * @return ?��?��应该??��??
     */
    public boolean shouldMelt(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (useDataComponentDurability) {
            return stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get()) != null;
        }
        if (useReactorComponentData) {
            return stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get()) != null;
        }
        return true;
    }
    
    @Override
    public int getBarWidth(ItemStack stack) {
        if (useDataComponentDurability) {
            FuelRodDurability data = stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            if (data == null || data.maxUses() <= 0) return 13;
            return Math.round(13.0F * data.getDurabilityRatio());
        }
        if (useReactorComponentData) {
            ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
            if (data == null || data.maxValue() <= 0) return 13;
            return Math.round(13.0F * data.getDurabilityRatio());
        }
        if (stack == null || stack.isEmpty()) {
            return 13;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 13;
        }
        return Math.round(13.0F - (float)stack.getDamageValue() * 13.0F / (float)maxDamage);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (useDataComponentDurability) {
            FuelRodDurability data = stack.get(mio_icif_data_components.FUEL_ROD_DURABILITY.get());
            if (data == null) return 0x00FF00;
            float ratio = data.getDurabilityRatio();
            if (ratio > 0.6F) {
                return 0x00FF00;
            } else if (ratio > 0.3F) {
                return 0xFFFF00;
            } else {
                return 0xFF0000;
            }
        }
        if (useReactorComponentData) {
            ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
            if (data == null) return 0x00FF00;
            float ratio = data.getDurabilityRatio();
            if (ratio > 0.6F) {
                return 0x00FF00;
            } else if (ratio > 0.3F) {
                return 0xFFFF00;
            } else {
                return 0xFF0000;
            }
        }
        if (stack == null || stack.isEmpty()) {
            return 0x00FF00;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 0x00FF00;
        }
        float durabilityRatio = 1.0F - (float)stack.getDamageValue() / (float)maxDamage;
        if (durabilityRatio > 0.6F) {
            return 0x00FF00;
        } else if (durabilityRatio > 0.3F) {
            return 0xFFFF00;
        } else {
            return 0xFF0000;
        }
    }
}

