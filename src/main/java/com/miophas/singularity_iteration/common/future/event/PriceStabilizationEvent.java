// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.future.event;


/**
 * Price stabilization event.
 *
 * Effect: during the next N days, the price moves back to the base price. The price then stays near the base price.
 * Intensity: a higher intensity makes the price move back faster.
 */
@SuppressWarnings("null")
public class PriceStabilizationEvent extends FuturePriceEvent {


    public PriceStabilizationEvent(String commodityId, long startDay, int duration, double intensity) {
        super(commodityId, startDay, duration, intensity);
    }

    @Override
    public EventType getType() {
        return EventType.PRICE_STABILIZATION;
    }

    @Override
    public int applyEffect(int currentPrice, int basePrice, int dayOffset, java.util.function.DoubleSupplier random) {
        // Calculate the deviation from the base price.
        double deviation = (double) (currentPrice - basePrice) / basePrice;

        // The regression strength increases with the day count and the intensity.
        double regressionStrength = 0.2 + (intensity * 0.3);
        double dayProgress = (double) dayOffset / duration;
        double totalRegression = deviation * regressionStrength * (1 + dayProgress);

        // Add a small random fluctuation (-2% to +2%).
        double randomFactor = 0.98 + (random.getAsDouble() * 0.04);

        // Calculate the new price.
        int newPrice = (int) (currentPrice * (1.0 - totalRegression) * randomFactor);

        // Make sure that the price stays near the base price (50%-150% of the base price).
        int minPrice = (int) (basePrice * 0.5);
        int maxPrice = (int) (basePrice * 1.5);

        return Math.max(minPrice, Math.min(maxPrice, newPrice));
    }
}


