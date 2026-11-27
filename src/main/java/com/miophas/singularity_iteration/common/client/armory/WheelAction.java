// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets;
import com.miophas.singularity_iteration.common.armory.WingmanCommands;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.client.suit.FcsState;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.230 (key consolidation, decisions one to four A): every item of the command wheel's fans. The wheel is the one way
 * in to the features that had keys of their own (the tactical map N, the piece call J, the remote pilot U, the jetpack mode
 * K, the HUD H); those keys stay, unbound by default. Items of the {@link Group#SQUAD} need an orderable Wingman (and the
 * command link); the {@link Group#GEAR} items work for anyone. Any item can be pinned to the favourites fan
 * ({@link #favorites}, at most {@link #MAX_FAVORITES}, saved in the client config).
 */
@OnlyIn(Dist.CLIENT)
public enum WheelAction {
    // the control fan: the three modes, the way home, the stance (G3b)
    MODE_OFFENSIVE("mode.offensive", "gui.mio_icif.wingman.mode.offensive", 0xFF5238, Group.CONTROL),
    MODE_DISPERSAL("mode.dispersal", "gui.mio_icif.wingman.mode.dispersal", 0xFFB333, Group.CONTROL),
    MODE_ESCORT("mode.escort", "gui.mio_icif.wingman.mode.escort", 0x4DE6FF, Group.CONTROL),
    RETURN("squad.return", "gui.mio_icif.command.sub.return", 0x9B7CFF, Group.CONTROL),
    STANCE_AUTO("stance.auto", "gui.mio_icif.command.sub.stance_auto", 0xC8D2DC, Group.CONTROL),
    STANCE_AIR("stance.air", "gui.mio_icif.command.sub.stance_air", 0x8FD3FF, Group.CONTROL),
    STANCE_GROUND("stance.ground", "gui.mio_icif.command.sub.stance_ground", 0xC9A36B, Group.CONTROL),
    // the suit fan (0.1.11.65) with the piece call and the quantum recall (each a sector or a key of its own before)
    SUIT_UP("suit.swap", "gui.mio_icif.command.sub.suit_up", 0xFFD24D, Group.SUIT),
    EJECT("suit.eject", "gui.mio_icif.command.sub.eject", 0xFF8A3D, Group.SUIT),
    PILOT("suit.pilot", "gui.mio_icif.command.sub.pilot", 0x6FF3D6, Group.SUIT),
    CLEAN_SLATE("suit.clean_slate", "gui.mio_icif.command.sub.clean_slate", 0xFF4D8A, Group.SUIT),
    PIECE_CALL("suit.piece_call", "gui.mio_icif.command.sub.piece_call", 0xB98CFF, Group.SUIT),
    QUANTUM_RECALL("suit.recall", "gui.mio_icif.command.sub.quantum_recall", 0x9B7CFF, Group.SUIT),
    // the gear fan: the suit's own screens and switches
    MAP("gear.map", "gui.mio_icif.command.sub.map", 0x4DE1C1, Group.GEAR),
    HOLO("gear.holo", "gui.mio_icif.command.sub.holo", 0x4DE1C1, Group.GEAR),
    JETPACK("gear.jetpack", "gui.mio_icif.command.sub.jetpack", 0xFFB333, Group.GEAR),
    HUD("gear.hud", "gui.mio_icif.command.sub.hud", 0x8FD3FF, Group.GEAR),
    HUD_LAYOUT("gear.hud_layout", "gui.mio_icif.command.sub.hud_layout", 0x8FD3FF, Group.GEAR),
    PANEL("gear.panel", "gui.mio_icif.command.sub.panel", 0xDDE7EC, Group.GEAR);

    /** Which fan an item is in. */
    public enum Group { CONTROL, SUIT, GEAR }

    public static final int MAX_FAVORITES = 3;
    /** Favourites of a new player. */
    public static final List<String> DEFAULT_FAVORITES = List.of("gear.map", "gear.jetpack", "suit.pilot");

    public final String id, key;
    public final int color;
    public final Group group;

    WheelAction(String id, String key, int color, Group group) {
        this.id = id;
        this.key = key;
        this.color = color;
        this.group = group;
    }

    @Nullable
    public static WheelAction byId(String id) {
        for (WheelAction a : values()) if (a.id.equals(id)) return a;
        return null;
    }

    /** The items of a fan in order. */
    public static List<WheelAction> of(Group group) {
        List<WheelAction> out = new ArrayList<>();
        for (WheelAction a : values()) if (a.group == group) out.add(a);
        return out;
    }

    /** The favourites (unknown ids of an older or newer version are left out). */
    public static List<WheelAction> favorites() {
        List<WheelAction> out = new ArrayList<>();
        for (String id : FcsClientConfig.wheelFavorites()) {
            WheelAction a = byId(id);
            if (a != null && !out.contains(a) && out.size() < MAX_FAVORITES) out.add(a);
        }
        return out;
    }

    /** Pins or unpins this item; false when the favourites are full. */
    public boolean toggleFavorite() {
        List<String> ids = new ArrayList<>();
        for (WheelAction a : favorites()) ids.add(a.id);
        if (!ids.remove(id)) {
            if (ids.size() >= MAX_FAVORITES) return false;
            ids.add(id);
        }
        FcsClientConfig.setWheelFavorites(ids);
        return true;
    }

    public boolean favorite() { return favorites().contains(this); }

    /** True when the item works now ({@code orderable}: the selection has a Wingman that takes orders). */
    public boolean enabled(boolean orderable) {
        return switch (this) {
            case MODE_OFFENSIVE, MODE_DISPERSAL, MODE_ESCORT, RETURN, STANCE_AUTO, STANCE_AIR, STANCE_GROUND, SUIT_UP, PILOT, CLEAN_SLATE -> orderable;
            case QUANTUM_RECALL -> SquadClient.recallCooldown() <= 0 && !SquadClient.units().isEmpty();
            case MAP -> FcsState.active(SuitModuleType.HOLOMAP);
            case HOLO -> HoloTable.available();
            default -> true;
        };
    }

    /** Runs the item (the wheel is closed first by the caller when this opens a screen). */
    public void run(UUID unit) {
        Minecraft mc = Minecraft.getInstance();
        switch (this) {
            case MODE_OFFENSIVE, MODE_DISPERSAL, MODE_ESCORT -> SquadClient.send(WingmanCommands.A_MODE, unit, (byte) ordinal(), null, -1);
            case RETURN -> SquadClient.send(WingmanCommands.A_RETURN, unit, (byte) 0, null, -1);
            case STANCE_AUTO, STANCE_AIR, STANCE_GROUND ->
                SquadClient.send(WingmanCommands.A_STANCE, unit, (byte) (ordinal() - STANCE_AUTO.ordinal()), null, -1);
            case SUIT_UP -> SquadClient.send(WingmanCommands.A_SUIT_UP, unit, (byte) 0, null, -1);
            case EJECT -> SquadClient.send(WingmanCommands.A_EJECT, WingmanCommandPackets.ALL, (byte) 0, null, -1);
            case PILOT -> SquadClient.send(WingmanCommands.A_PILOT, unit, (byte) 0, null, -1);
            case CLEAN_SLATE -> SquadClient.send(WingmanCommands.A_CLEAN_SLATE, WingmanCommandPackets.ALL, (byte) 0, null, -1);
            case QUANTUM_RECALL -> SquadClient.send(WingmanCommands.A_QUANTUM_RECALL, WingmanCommandPackets.ALL, (byte) 0, null, -1);
            case PIECE_CALL -> mc.setScreen(new PieceCallScreen());
            case MAP -> mc.setScreen(new com.miophas.singularity_iteration.common.client.suit.TacticalMapScreen());
            case HOLO -> HoloTable.open();
            case JETPACK -> com.miophas.singularity_iteration.common.network.mio_icif_Network.sendJetpackModeSwitch();
            case HUD -> {
                if (mc.player != null) com.miophas.singularity_iteration.common.client.suit.FcsClient.toggleHud(mc.player);
            }
            case HUD_LAYOUT -> mc.setScreen(new com.miophas.singularity_iteration.common.client.suit.HudLayoutScreen(null));
            case PANEL -> mc.setScreen(new com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_armor_features());
        }
    }

    /** Why the item does not work now (for the action bar). */
    public Component refusal() {
        return switch (this) {
            case MAP -> Component.translatable("gui.mio_icif.tactical_map.need_unit");
            case QUANTUM_RECALL -> Component.translatable("gui.mio_icif.command.recall_cd", (SquadClient.recallCooldown() + 19) / 20);
            default -> Component.translatable("message.mio_icif.command.no_unit");
        };
    }
}
