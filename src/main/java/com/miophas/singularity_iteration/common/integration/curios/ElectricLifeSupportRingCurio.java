package com.miophas.singularity_iteration.common.integration.curios;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import top.theillusivec4.curios.api.SlotContext;

public class ElectricLifeSupportRingCurio extends MioIcifTrinketBase implements com.miophas.singularity_iteration.common.item.tuning.ITunableItem {

    private static final int STORAGE_ENERGY = 100000000;
    private static final int TIER = 5;

    private static final int HEAL_COST = 10000;
    private static final int FOOD_COST = 200;
    private static final int ABSORB_COST = 5000;

    public ElectricLifeSupportRingCurio() {
        super(new Item.Properties().stacksTo(1), STORAGE_ENERGY, 0, "electric_life_support_ring", STORAGE_ENERGY, 0, TIER, "ring");
    }

    // ---- efficiency slider: 500% = the original every-tick behaviour, 100% (default) = one fifth of it
    private static final java.util.List<Spec> SPECS = java.util.List.of(new Spec("efficiency", 100, 500, 25, 100, "%"));
    private static final java.util.Map<java.util.UUID, Float> PULSE = new java.util.concurrent.ConcurrentHashMap<>();

    @Override public java.util.List<Spec> tuningSpecs(ItemStack stack) { return SPECS; }

    /** Fraction of ticks that run a support pulse (500% -> every tick). */
    public static float rate(int efficiency) { return efficiency / 500F; }

    /** EU multiplier per pulse: (rate)^0.6, so 500% costs exactly the old amount and 100% about 38% per pulse. */
    public static float costFactor(int efficiency) { return (float) Math.pow(rate(efficiency), 0.6); }

    private long cost(int base, int efficiency) { return Math.max(1, Math.round(base * costFactor(efficiency))); }

    @Override
    public java.util.List<Component> tuningSummary(ItemStack stack) {
        int e = tuning(stack, "efficiency");
        float pulsesPerSecond = 20 * rate(e);
        double euPerSecondFull = pulsesPerSecond * (HEAL_COST + FOOD_COST + ABSORB_COST) * costFactor(e);
        return java.util.List.of(
            Component.translatable("tuning.mio_icif.ring.heal", String.format("%.1f", pulsesPerSecond * 0.5F)),
            Component.translatable("tuning.mio_icif.ring.cost", String.format("%,.0f", euPerSecondFull)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tuning.mio_icif.ring.current", tuning(stack, "efficiency")).withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.tuning_hint").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof Player player) || entity.level().isClientSide()) return;

        if (!player.hasEffect(MobEffects.ABSORPTION)) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Integer.MAX_VALUE, 4, false, false, false));
        }
        // rate limiter: accumulate the per-tick share and run one pulse whenever it reaches 1
        int efficiency = tuning(stack, "efficiency");
        if (PULSE.size() > 512) PULSE.clear();   // players who logged out wearing the ring; losing a sub-tick fraction is harmless
        float pulse = PULSE.getOrDefault(player.getUUID(), 0F) + rate(efficiency);
        if (pulse < 1F) {
            PULSE.put(player.getUUID(), pulse);
            return;
        }
        PULSE.put(player.getUUID(), Math.min(1F, pulse - 1F));

        boolean updated = false;

        float currentHealth = player.getHealth();
        if (currentHealth < player.getMaxHealth()) {
            if (consumeEnergy(stack, cost(HEAL_COST, efficiency))) {
                player.setHealth(Math.min(currentHealth + 0.5f, player.getMaxHealth()));
                updated = true;
            }
        }

        FoodData foodData = player.getFoodData();
        if (foodData.needsFood()) {
            if (consumeEnergy(stack, cost(FOOD_COST, efficiency))) {
                foodData.eat(1, 0.2f);
                updated = true;
            }
        }

        float currentAbsorb = player.getAbsorptionAmount();
        if (currentAbsorb < 20.0f) {
            if (consumeEnergy(stack, cost(ABSORB_COST, efficiency))) {
                player.setAbsorptionAmount(currentAbsorb + 0.5f);
                updated = true;
            }
        }

        if (updated) {
            player.containerMenu.broadcastChanges();
        }
    }

    @Override
    protected void onTrinketEquipped(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Integer.MAX_VALUE, 4, false, false, false));
        }
    }

    @Override
    protected void onTrinketUnequipped(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        PULSE.remove(entity.getUUID());
        if (entity instanceof Player player) {
            player.removeEffect(MobEffects.ABSORPTION);
            player.setAbsorptionAmount(0.0f);
        }
    }
}