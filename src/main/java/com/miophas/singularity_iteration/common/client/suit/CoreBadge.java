// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors;
import com.miophas.singularity_iteration.common.suit.core.FrameCore;
import com.miophas.singularity_iteration.common.suit.core.FrameCores;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/**
 * Corner badge of a socketed suit piece (0.1.9.0): a small diamond in the colour of the frame
 * core at the top left of the slot, with one pip per solar array tier. The icon itself stays the
 * plain helmet or chestplate.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class CoreBadge implements IItemDecorator {
    private static final CoreBadge INSTANCE = new CoreBadge();

    private CoreBadge() {}

    @SubscribeEvent
    public static void register(RegisterItemDecorationsEvent event) {
        event.register(mio_icif_items_armors.ARMOR_NANO_HELMET.get(), INSTANCE);
        event.register(mio_icif_items_armors.ARMOR_QUANTUM_HELMET.get(), INSTANCE);
        event.register(mio_icif_items_armors.ARMOR_QUANTUM_CHESTPLATE.get(), INSTANCE);
    }

    @Override
    public boolean render(GuiGraphics g, Font font, ItemStack stack, int x, int y) {
        FrameCore core = FrameCores.core(stack);
        if (core == null) return false;
        int color = 0xFF000000 | core.color();
        int dark = 0xE0101418;
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        // diamond 5 px wide with a dark rim, at the top left corner
        int cx = x + 3, cy = y + 3;
        for (int r = -3; r <= 3; r++) {
            int w = 3 - Math.abs(r);
            g.fill(cx - w, cy + r, cx + w + 1, cy + r + 1, dark);
        }
        for (int r = -2; r <= 2; r++) {
            int w = 2 - Math.abs(r);
            g.fill(cx - w, cy + r, cx + w + 1, cy + r + 1, color);
        }
        g.fill(cx, cy - 1, cx + 1, cy, 0xFFFFFFFF);
        // solar tiers: pips under the diamond
        if (core.isSolar()) {
            for (int i = 0; i < core.tier(); i++) {
                int px = x + 1 + i * 2;
                g.fill(px, y + 7, px + 1, y + 8, color);
            }
        }
        g.pose().popPose();
        return false;
    }
}
