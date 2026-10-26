package com.miophas.singularity_iteration.common.future.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;


/**
 * Rising trend event.
 * 
 * Effect: the price rises continuously for the next N days. Each day the rise is between 5% and 15% of the base price.
 * Intensity: a higher intensity gives a larger rise.
 */
@SuppressWarnings("null")
public class RisingTrendEvent extends FuturePriceEvent {
    
    
    public RisingTrendEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }
    
    @Override
    public EventType getType() {
        return EventType.RISING_TREND;
    }
    
    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset, java.util.function.DoubleSupplier random) {
        // Base rise is 5% - 15%. The intensity changes it.
        double baseRise = 0.05 + (0.10 * intensity);
        // The rise increases each day. The last day has the largest rise.
    double dayMultiplier = 1.0 + (dayOffset * 0.1);
        double totalChange = baseRise * dayMultiplier;
        
        // Add a random fluctuation (-20% to +20%).
        double randomFactor = 0.8 + (random.getAsDouble() * 0.4);
        totalChange *= randomFactor;
        
        int newPrice = (int) (currentPrice * (1.0 + totalChange));
        
        // Limit the price to the configured price range.
    double maxMultiplier = Singularity_Iteration_Config.FUTURE_MAX_PRICE_MULTIPLIER.get();
        int maxPrice = (int) (basePrice * maxMultiplier);
        
        return Math.min(newPrice, maxPrice);
    }
}


