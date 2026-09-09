package com.singularity_iteration.mio_icif.integration.curios;

import com.singularity_iteration.mio_icif.util.JetpackKeyHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForgeMod;
import top.theillusivec4.curios.api.SlotContext;

public class ElectricFlightRingCurio extends MioIcifTrinketBase {

    private static final int STORAGE_ENERGY = 10000000;
    private static final int TIER = 4;

    private static final int FLIGHT_ENERGY_COST = 10;
    private static final int BOOST_ENERGY_COST = 10;

    private static final ResourceLocation FLIGHT_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "electric_flight_ring");
    private static final AttributeModifier FLIGHT_MODIFIER = new AttributeModifier(
        FLIGHT_MODIFIER_ID,
        1.0,
        AttributeModifier.Operation.ADD_VALUE
    );

    public ElectricFlightRingCurio() {
        super(new Item.Properties().stacksTo(1), STORAGE_ENERGY, 0, "electric_flight_ring", STORAGE_ENERGY, 0, TIER, "ring");
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (newStack.getItem() == this) {
            return;
        }
        super.onUnequip(slotContext, newStack, stack);
    }

    @Override
    protected void onTrinketUnequipped(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof Player player) {
            AttributeInstance flightAttr = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
            disableFlight(player, flightAttr);
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof Player player)) return;
        if (player.isSpectator() || player.isCreative()) return;

        if (entity.level().isClientSide()) {
            curioTickClient(player, stack);
            return;
        }

        curioTickServer(player, stack);
    }

    @SuppressWarnings("deprecation")
    private void curioTickServer(Player player, ItemStack stack) {
        boolean hasEnergy = hasEnoughEnergy(stack, FLIGHT_ENERGY_COST);
        AttributeInstance flightAttr = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);

        if (hasEnergy) {
            if (flightAttr != null && !flightAttr.hasModifier(FLIGHT_MODIFIER_ID)) {
                flightAttr.addTransientModifier(FLIGHT_MODIFIER);
            }
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }

            if (player.getAbilities().flying) {
                boolean isBoosting = JetpackKeyHandler.isBoostKeyDown(player);
                int cost = isBoosting ? FLIGHT_ENERGY_COST + BOOST_ENERGY_COST : FLIGHT_ENERGY_COST;
                if (hasEnoughEnergy(stack, cost)) {
                    consumeEnergy(stack, cost);
                } else {
                    disableFlight(player, flightAttr);
                }
            }
        } else {
            disableFlight(player, flightAttr);
        }
    }

    private void curioTickClient(Player player, ItemStack stack) {
        if (!player.getAbilities().flying) return;

        boolean isBoosting = JetpackKeyHandler.isBoostKeyDown(player);
        if (!isBoosting) return;

        var lookVec = player.getLookAngle();
        player.setDeltaMovement(
            player.getDeltaMovement().x + lookVec.x * 0.4D + (lookVec.x * 1.5D - player.getDeltaMovement().x) * 0.5D,
            player.getDeltaMovement().y + lookVec.y * 0.4D + (lookVec.y * 1.5D - player.getDeltaMovement().y) * 0.5D,
            player.getDeltaMovement().z + lookVec.z * 0.4D + (lookVec.z * 1.5D - player.getDeltaMovement().z) * 0.5D
        );
        player.hasImpulse = true;

        float pitch = player.getXRot();
        float yaw = player.getYRot();
        float f1 = Mth.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = Mth.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f3 = -Mth.cos(-pitch * 0.017453292F);
        float f4 = Mth.sin(-pitch * 0.017453292F);
        float f5 = Mth.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f6 = Mth.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f7 = -Mth.cos(-pitch * 0.017453292F);

        for (int i = 0; i < 2; ++i) {
            player.level().addParticle(
                ParticleTypes.FIREWORK,
                player.getX() + (double) (f2 * f3 * 0.0625D - f5 * 0.0625D),
                player.getY() + (double) (f4 * 0.125D),
                player.getZ() + (double) (f6 * f7 * 0.0625D - f1 * 0.0625D),
                0.0D, -0.1D, 0.0D
            );
        }
    }

    @SuppressWarnings("deprecation")
    private void disableFlight(Player player, AttributeInstance flightAttr) {
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        if (player.getAbilities().mayfly) {
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }
        if (flightAttr != null && flightAttr.hasModifier(FLIGHT_MODIFIER_ID)) {
            flightAttr.removeModifier(FLIGHT_MODIFIER_ID);
        }
    }
}