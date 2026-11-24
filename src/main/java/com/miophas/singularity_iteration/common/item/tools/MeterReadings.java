// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Wind_Kinetic_Generator;
import com.miophas.singularity_iteration.common.energy.GeomagPhysics;
import com.miophas.singularity_iteration.core.runtime.world.WindSim;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The wind and geomagnetic readings, 0.1.11.200 (tool overhaul T5c, decision D7). The multimeter (the EU meter's wind and
 * geomagnetic probes), the old wind meter and the old geomagnetic detector give the same readings from here. A reading is
 * one action-bar line (U-20: the two old tools wrote two chat lines each).
 */
public final class MeterReadings {
    private MeterReadings() {}

    /** The wind at the player's height (IC2 wind meter in the air). */
    public static void windHere(Player player) {
        if (!(player.level() instanceof ServerLevel server)) return;
        wind(player, WindSim.get(server).getWindAt(player.blockPosition().getY() + 0.5));
    }

    /**
     * The effective wind of a wind kinetic generator (IC2 wind meter on the generator): the global field, height and rotor
     * shading. A generator without a rotor, or one whose rotor is blocked, is reported as such.
     *
     * @return false when there was nothing to read (no rotor, blocked), so a caller charges nothing
     */
    public static boolean windOf(Player player, mio_icif_Wind_Kinetic_Generator generator) {
        if (generator.getRotorStack().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.mio_icif.windmeter.rotor.none").withStyle(ChatFormatting.RED), true);
            return false;
        }
        int status = generator.getContainerData().get(0);
        if (!generator.isGenerating() && status != mio_icif_Wind_Kinetic_Generator.STATUS_FEATHERED
                && status != mio_icif_Wind_Kinetic_Generator.STATUS_WEAK_WIND) {
            player.displayClientMessage(Component.translatable("message.mio_icif.windmeter.rotor.blocked").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        wind(player, generator.getWindStrength());
        return true;
    }

    private static void wind(Player player, double strength) {
        com.miophas.singularity_iteration.common.advancement.SiAdvancements.grant(player, "wind_meter", "used");
        MutableComponent line = Component.translatable("message.mio_icif.windmeter.info", String.format("%.2f", Math.max(0.0, strength)))
            .withStyle(ChatFormatting.AQUA);
        if (player.level() instanceof ServerLevel server) {
            int bearing = WindSim.get(server).getWindDirection();
            String weather = server.isThundering() ? "×1.50" : server.isRaining() ? "×1.25" : "×1.00";
            line.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("message.mio_icif.windmeter.direction", bearing,
                    Component.translatable("gui.mio_icif.wind.point." + com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.WindPhysics.point(bearing)),
                    weather).withStyle(ChatFormatting.DARK_AQUA));
        }
        player.displayClientMessage(line, true);
    }

    /** The geomagnetic source strength at {@code pos} and the field's disturbance now (Kp). */
    public static void geomagnetic(Player player, Level level, BlockPos pos) {
        float ratio = GeomagPhysics.source(level, pos);
        String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("");
        if (biome.contains("nether") || biome.contains("hell")) ratio *= 1.2F;
        else if (biome.contains("mountain") || biome.contains("hill") || biome.contains("peaks")) ratio *= 1.1F;
        long seed = GeomagPhysics.seed(level);
        MutableComponent line = Component.translatable("message.mio_icif.geomagnetic_detector.value", String.format("%.2f", ratio));
        line.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.translatable("message.mio_icif.geomagnetic_detector.kp",
                String.format("%.1f", GeomagPhysics.kp(seed, level.getDayTime())),
                String.format("%.2f", GeomagPhysics.disturbance(seed, level.getDayTime()))));
        player.displayClientMessage(line, true);
    }
}
