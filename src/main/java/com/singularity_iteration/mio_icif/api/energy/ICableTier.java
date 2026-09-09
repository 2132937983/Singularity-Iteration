package com.singularity_iteration.mio_icif.api.energy;

/**
 * API-level cable tier interface
 * Decouples addon developers from internal CableTier implementation
 * <p>
 * 要获取默认电缆等级，请使用
 * {@code MioIcifAPI.instance().getEnergyNetAPI().getDefaultCableTier()}，
 * 而非直接引用内部实现类。
 */
public interface ICableTier {
    
    /**
     * Get the tier name (lowercase)
     * @return tier name
     */
    String getName();
    
    /**
     * Get the tier display name
     * @return display name
     */
    String getDisplayName();
    
    /**
     * Get the tier full name
     * @return full name
     */
    String getFullName();
    
    /**
     * Get the power rating (EU/t)
     * @return power rating
     */
    long getPowerRating();
    
    /**
     * Get the electric damage value
     * @return electric damage
     */
    float getElectricDamage();
    
    /**
     * Get the conductor breakdown energy threshold
     * @return breakdown energy (EU)
     */
    long getConductorBreakdownEnergy();
    
    /**
     * Get the insulation breakdown energy threshold
     * @return insulation breakdown energy (EU)
     */
    long getInsulationBreakdownEnergy();
    
    /**
     * Get the tier ordinal
     * @return tier ordinal
     */
    int getTier();
}