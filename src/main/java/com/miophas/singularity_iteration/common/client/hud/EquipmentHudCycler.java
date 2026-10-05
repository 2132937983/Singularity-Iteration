// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.hud;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.item.IEquipmentHudProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Equipment HUD cycler: one action-bar line for all held and worn items that implement
 * {@link IEquipmentHudProvider}.
 *
 * <p>Each slot has its own display window. A line shows for {@link #SHOW_TICKS} ticks when its
 * wording changes (item equipped, mode switch); a change of numbers only (energy) does not show it
 * again. A provider can ask to keep its own line on screen ({@code keepEquipmentHudVisible}, for
 * example a jetpack in the air). That request applies to that line only: other lines still fade
 * (0.1.7.35: a quantum sword line stayed on screen while the jetpack chestplate kept itself visible).
 * When no line is due, the cycler stops writing and the vanilla action bar fades out.
 *
 * <p>The tick path allocates no collections: the slot table is fixed and reused.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class EquipmentHudCycler {
    private EquipmentHudCycler() {}

    /** Ticks each due line stays before the next due line takes the action bar (2 s). */
    private static final int DWELL_TICKS = 40;
    /** Ticks a line stays on screen after its wording changes (3 s). */
    public static final int SHOW_TICKS = 60;

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Component[] TEXT = new Component[SLOTS.length];
    private static final String[] SIGNATURE = new String[SLOTS.length];
    private static final int[] VISIBLE = new int[SLOTS.length];
    private static final boolean[] KEEP = new boolean[SLOTS.length];
    private static final StringBuilder SCRATCH = new StringBuilder(64);

    private static int current = -1;
    private static int dwell;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            reset();
            return;
        }
        boolean any = false;
        for (int i = 0; i < SLOTS.length; i++) any |= update(i, player.getItemBySlot(SLOTS[i]), player);
        if (!any) {
            current = -1;
            return;
        }
        // stay on the current line for DWELL_TICKS, then move to the next due line
        if (current < 0 || !due(current) || --dwell <= 0) {
            current = next(current);
            dwell = DWELL_TICKS;
        }
        if (current >= 0) minecraft.gui.setOverlayMessage(TEXT[current], false);
    }

    /** Refreshes one slot; returns true when its line is due this tick. */
    private static boolean update(int i, ItemStack stack, Player player) {
        Component text = !stack.isEmpty() && stack.getItem() instanceof IEquipmentHudProvider provider
            ? provider.getEquipmentHudText(stack) : null;
        if (text == null) {
            TEXT[i] = null;
            SIGNATURE[i] = null;
            VISIBLE[i] = 0;
            KEEP[i] = false;
            return false;
        }
        String signature = maskNumbers(text.getString());
        if (!signature.equals(SIGNATURE[i])) {
            SIGNATURE[i] = signature;
            VISIBLE[i] = SHOW_TICKS;
        } else if (VISIBLE[i] > 0) {
            VISIBLE[i]--;
        }
        TEXT[i] = text;
        KEEP[i] = ((IEquipmentHudProvider) stack.getItem()).keepEquipmentHudVisible(stack, player);
        return due(i);
    }

    private static boolean due(int i) {
        return TEXT[i] != null && (KEEP[i] || VISIBLE[i] > 0);
    }

    private static int next(int from) {
        for (int k = 1; k <= SLOTS.length; k++) {
            int i = Math.floorMod(from + k, SLOTS.length);
            if (due(i)) return i;
        }
        return -1;
    }

    /** The wording with every run of digits (and its separators) replaced by '#'. */
    static String maskNumbers(String s) {
        SCRATCH.setLength(0);
        boolean inNumber = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean digit = c >= '0' && c <= '9';
            if (digit || (inNumber && (c == '.' || c == ','))) {
                if (!inNumber) SCRATCH.append('#');
                inNumber = true;
            } else {
                inNumber = false;
                SCRATCH.append(c);
            }
        }
        return SCRATCH.toString();
    }

    private static void reset() {
        for (int i = 0; i < SLOTS.length; i++) {
            TEXT[i] = null;
            SIGNATURE[i] = null;
            VISIBLE[i] = 0;
            KEEP[i] = false;
        }
        current = -1;
        dwell = 0;
    }
}
