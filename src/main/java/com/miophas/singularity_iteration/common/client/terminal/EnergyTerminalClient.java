package com.miophas.singularity_iteration.common.client.terminal;

import com.miophas.singularity_iteration.common.block.wiring.mio_icif_block_energy_terminal;
import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyTerminalBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Client-only behaviour of the energy terminal (ambient hum near an active terminal). */
public final class EnergyTerminalClient {
    private EnergyTerminalClient() {}

    private static final int AMBIENT_PERIOD = 40;   // the hum clip is 2 s long

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyTerminalBlockEntity terminal) {
        if (!state.hasProperty(mio_icif_block_energy_terminal.ACTIVE) || !state.getValue(mio_icif_block_energy_terminal.ACTIVE)) return;
        if (--terminal.clientAmbientTimer > 0) return;
        terminal.clientAmbientTimer = AMBIENT_PERIOD;
        var player = Minecraft.getInstance().player;
        if (player == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 144) return;
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, mio_icif_sounds.TERMINAL_AMBIENT.get(),
            SoundSource.BLOCKS, 0.25F, 1.0F, false);
    }
}
