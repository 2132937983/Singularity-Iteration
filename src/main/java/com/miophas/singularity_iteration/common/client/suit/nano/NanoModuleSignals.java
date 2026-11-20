// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.suit.SensorTracker;
import com.miophas.singularity_iteration.common.suit.SuitEnergyPool;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitSensorData;
import com.miophas.singularity_iteration.common.suit.nano.NanoConfig;
import com.miophas.singularity_iteration.common.suit.nano.NanoPackets;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureState;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Maps the suit's synced actions and local sensor readings to indicator intensities. */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
final class NanoModuleSignals {
    private static final Map<LivingEntity, EnumMap<SuitModuleType, Long>> PULSES = new WeakHashMap<>();
    private static final ResourceLocation REPAIR = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "nano.repair");

    private NanoModuleSignals() {}

    private static void pulse(LivingEntity entity, SuitModuleType type, int ticks) {
        PULSES.computeIfAbsent(entity, key -> new EnumMap<>(SuitModuleType.class)).put(type, entity.level().getGameTime() + ticks);
    }

    static void onFx(NanoPackets.Fx fx) {
        var world = Minecraft.getInstance().level;
        if (world == null || !(world.getEntity(fx.entityId()) instanceof LivingEntity entity)) return;
        switch (fx.kind()) {
            case NanoPackets.FX_DASH -> pulse(entity, SuitModuleType.MYOFIBER, 6);
            case NanoPackets.FX_EMP -> pulse(entity, SuitModuleType.EMP_EMITTER, 7);
            case NanoPackets.FX_CAMO_ON -> pulse(entity, SuitModuleType.OPTICAL_CAMO, 4);
            case NanoPackets.FX_CAMO_KEEP, NanoPackets.FX_CAMO_OFF -> pulse(entity, SuitModuleType.OPTICAL_CAMO, 0);
            case NanoPackets.FX_RECON_CLOUD -> pulse(entity, SuitModuleType.RECON_CLOUD, 9);
            case NanoPackets.FX_MESH_ON -> pulse(entity, SuitModuleType.KINETIC_MESH, Math.max(1, (int) fx.a()));
            case NanoPackets.FX_MESH_OFF -> pulse(entity, SuitModuleType.KINETIC_MESH, 0);
            case NanoPackets.FX_REBOUND -> pulse(entity, SuitModuleType.SHOCK_GEL, 7);
        }
    }

    @SubscribeEvent
    static void onSound(PlaySoundEvent event) {
        var sound = event.getSound();
        if (sound == null || !REPAIR.equals(sound.getLocation())) return;
        var client = Minecraft.getInstance();
        var level = client.level;
        if (level == null) return;
        double x = sound.getX(), y = sound.getY(), z = sound.getZ();
        client.execute(() -> {
            if (client.level != level) return;
            Player owner = null;
            for (var player : level.players()) {
                if (player.distanceToSqr(x, y, z) > .07 || SuitModules.wornPieceWith(player, SuitModuleType.SELF_REPAIR).isEmpty()) continue;
                if (owner != null) return; // This positional sound cannot distinguish two overlapping wearers.
                owner = player;
            }
            if (owner != null) pulse(owner, SuitModuleType.SELF_REPAIR, 8);
        });
    }

    static float level(LivingEntity entity, ItemStack stack, SuitModuleType type, float partial) {
        if (!NanoConfig.enabled(type)) return 0;
        if (type == SuitModuleType.MICRO_CAPACITOR) return SuitModules.energy(stack) > 0 ? .55F : 0;
        if (!ArmorFeatureState.isEnabled(stack, type.featureKey())) return 0;
        if (type.needsVisor() && !SuitModules.isSuitPiece(entity.getItemBySlot(EquipmentSlot.HEAD))) return 0;
        if (!(entity instanceof Player player)) return 0;
        var pulses = PULSES.get(player);
        if (pulses != null && pulses.getOrDefault(type, Long.MIN_VALUE) > player.level().getGameTime()) return 1;
        if (type.needsPower() && !SuitEnergyPool.canPay(player, stack, Math.max(1, type.drainPerTick()))) return 0;
        boolean local = Minecraft.getInstance().player == player;
        return switch (type) {
            case MYOFIBER, EMP_EMITTER, OPTICAL_CAMO, RECON_CLOUD, KINETIC_MESH, SHOCK_GEL, SELF_REPAIR -> 0;
            case BLAST_WARNING -> {
                if (!local || SensorTracker.blasts().isEmpty()) yield .17F;
                float seconds = Float.MAX_VALUE;
                for (var blast : SensorTracker.blasts()) seconds = Math.min(seconds, blast.seconds());
                float period = seconds < 1 ? 3 : seconds < 3 ? 6 : 12;
                yield (player.tickCount + partial) % period < period * .5F ? 1F : .2F;
            }
            case THREAT_SENSOR -> local && !SuitSensorData.threats().isEmpty() ? .95F : .17F;
            case SOLAR_SKIN -> {
                var pos = player.blockPosition().atY(Mth.floor(player.getEyeY()));
                yield player.level().isDay() && !player.level().isRainingAt(pos) && player.level().canSeeSky(pos) ? .85F : 0;
            }
            case KINETIC_GENERATOR -> {
                double dx = player.getX() - player.xo, dz = player.getZ() - player.zo;
                double step = dx * dx + dz * dz;
                yield player.onGround() && !player.isPassenger() && step > .0001 && step < 2.25 ? .75F : 0;
            }
            case THERMAL_CAMO -> player.isShiftKeyDown() ? .12F : .48F;
            case HYDROPHOBIC -> player.isInWater() ? .86F : .23F;
            case GECKO_SETAE -> player.isShiftKeyDown() && player.horizontalCollision ? .85F : .22F;
            default -> .56F;
        };
    }
}
