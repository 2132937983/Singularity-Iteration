package com.miophas.singularity_iteration.common.client.energy;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyTerminalBlockEntity;
import com.miophas.singularity_iteration.common.client.screen.DspUi;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_eu_meter;
import com.miophas.singularity_iteration.common.menu.tool.MeterHudPacket;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractWireBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Voltage HUD: a small holographic read-out beside the crosshair.
 * <ul>
 *   <li>Holding the EU meter / voltage detector and aiming at a cable, transformer, terminal or
 *       energy machine: live packet voltage + tier, current (A), throughput, cables and
 *       endpoints of that network (asked from the server twice a second).</li>
 *   <li>Aiming at a cable without the meter: its rated tier and packet size (known locally).</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class VoltageHud {
    private VoltageHud() {}

    private static int timer;

    private static boolean holdingMeter(Minecraft mc) {
        return mc.player != null && (mc.player.getMainHandItem().getItem() instanceof mio_icif_eu_meter
            || mc.player.getOffhandItem().getItem() instanceof mio_icif_eu_meter);
    }

    private static BlockPos target(Minecraft mc) {
        if (mc.level == null || mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return null;
        return ((BlockHitResult) mc.hitResult).getBlockPos();
    }

    private static boolean energyBlock(BlockEntity be) {
        return be instanceof AbstractEnergyBlockEntity || be instanceof EnergyTerminalBlockEntity
            || be instanceof com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity
            || be instanceof com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSpecialCableBlockEntity
            || be instanceof com.miophas.singularity_iteration.common.blockentity.transformer.mio_icif_transformer;
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || !holdingMeter(mc)) return;
        BlockPos pos = target(mc);
        if (pos == null || !energyBlock(mc.level.getBlockEntity(pos))) return;
        if (timer-- > 0) return;
        timer = 10;
        try {
            PacketDistributor.sendToServer(new MeterHudPacket.Request(pos));
        } catch (RuntimeException ignored) { }
    }

    @SubscribeEvent
    static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) return;
        BlockPos pos = target(mc);
        if (pos == null) return;
        BlockEntity be = mc.level.getBlockEntity(pos);
        List<String[]> lines = new ArrayList<>();      // {text, colour}
        if (holdingMeter(mc) && energyBlock(be)) {
            var r = MeterHudPacket.latest();
            if (r != null && r.pos().equals(pos)) {
                String tier = com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker.tierName(r.voltage());
                float amps = r.voltage() > 0 ? r.throughput() / r.voltage() : 0;
                lines.add(new String[]{tier + "  " + (r.voltage() > 0 ? DspUi.compact(r.voltage()) + " V" : "0 V") + "   " + String.format("%.2f A", amps), "o"});
                lines.add(new String[]{DspUi.compact(r.throughput()) + " EU/t  ·  " + Component.translatable("hud.mio_icif.voltage.nodes",
                    r.conductors(), r.endpoints()).getString(), "c"});
                if (r.ratedPacket() > 0) lines.add(new String[]{Component.translatable("hud.mio_icif.voltage.rated",
                    com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker.tierName(r.ratedPacket()),
                    DspUi.compact(r.ratedPacket())).getString(), r.voltage() > r.ratedPacket() ? "r" : "d"});
            } else {
                lines.add(new String[]{Component.translatable("hud.mio_icif.voltage.measuring").getString(), "d"});
            }
        } else if (be instanceof AbstractWireBlockEntity wire && wire.getCableTier() != null) {
            long rated = wire.getCableTier().getMaxTransfer();
            lines.add(new String[]{Component.translatable("hud.mio_icif.voltage.cable",
                com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker.tierName(rated), DspUi.compact(rated)).getString(), "c"});
        } else {
            return;
        }
        draw(event.getGuiGraphics(), mc, lines);
    }

    private static void draw(GuiGraphics g, Minecraft mc, List<String[]> lines) {
        int x = mc.getWindow().getGuiScaledWidth() / 2 + 14, y = mc.getWindow().getGuiScaledHeight() / 2 + 8;
        int w = 0;
        for (String[] l : lines) w = Math.max(w, mc.font.width(l[0]));
        int h = lines.size() * 10 + 4;
        g.fill(x - 3, y - 3, x + w + 4, y + h - 1, 0xC00C1622);
        g.fill(x - 3, y - 3, x - 2, y + h - 1, DspUi.CYAN);
        g.fill(x - 3, y - 3, x + 8, y - 2, DspUi.CYAN);
        for (int i = 0; i < lines.size(); i++) {
            int color = switch (lines.get(i)[1]) {
                case "o" -> DspUi.ORANGE;
                case "c" -> DspUi.CYAN;
                case "r" -> DspUi.RED;
                default -> DspUi.TEXT_DIM;
            };
            g.drawString(mc.font, lines.get(i)[0], x, y + i * 10, color, true);
        }
    }
}
