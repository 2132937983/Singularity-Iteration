// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentType;

/**
 * Server bridge to <b>SlashBlade: Resharped</b> for non-player wielders (0.1.8.4, Armory Wingman).
 * Only this fork is supported (mod id {@code slashblade}, display name with "Resharp"; checked
 * against 2.1.9). No compile dependency: the handles are looked up once; if a lookup fails the
 * bridge turns off, writes one log line, and the Wingman fights with the blade as a plain sword.
 *
 * <p>How Resharped runs a blade (from its 2.1.9 bytecode):
 * <ul>
 *   <li>The combo of a wielder is advanced by {@code ISlashBladeState#progressCombo(LivingEntity)},
 *       which reads the wielder's input commands from the {@code slashblade:input_state}
 *       attachment (a player's left click adds {@code L_CLICK}, then removes it).</li>
 *   <li>The attacks of a combo (sweeps, the rapid slash dash) run in
 *       {@code ItemSlashBlade#inventoryTick} through {@code ComboState#tickAction} for any
 *       {@link LivingEntity} that holds the blade in the main hand. Mobs never get an
 *       inventory tick, so the Wingman calls it every tick ({@link #tick}).</li>
 *   <li>Slash Arts (Judgement Cut, Drive, ...) start with
 *       {@code ISlashBladeState#doChargeAction(LivingEntity, chargeTicks)} after a full charge
 *       ({@code getFullChargeTicks}); the target is {@code setTargetEntityId(Entity)}.</li>
 * </ul>
 */
public final class SlashBladeCombat {
    private SlashBladeCombat() {}

    private static final String MOD_ID = "slashblade";
    private static final String FORK_MARK = "resharp";
    private static final int UNKNOWN = 0, READY = 1, OFF = -1;
    private static volatile int state = UNKNOWN;

    private static Class<?> bladeItem;
    private static MethodHandle stateOf, progressCombo, doChargeAction, fullChargeTicks, setTarget, isBroken, getCommands;
    private static Supplier<AttachmentType<?>> inputState;
    @SuppressWarnings("rawtypes")
    private static Class<? extends Enum> commandType;

    /** Input commands understood by Resharped (names of {@code mods.flammpfeil.slashblade.util.InputCommand}). */
    public enum Command { FORWARD, BACK, LEFT, RIGHT, SNEAK, L_CLICK, R_CLICK, SPRINT, JUMP }

    /** True for a usable (not broken) blade of SlashBlade: Resharped. */
    public static boolean isBlade(ItemStack stack) {
        if (stack.isEmpty() || !ready() || !bladeItem.isInstance(stack.getItem())) return false;
        Object s = state(stack);
        if (s == null) return false;
        try {
            return !(boolean) isBroken.invoke(s);
        } catch (Throwable t) {
            fail(t);
            return false;
        }
    }

    /** Runs the combo actions of the held blade for one tick (the inventory tick a mob never gets). */
    public static void tick(LivingEntity wielder) {
        ItemStack blade = wielder.getMainHandItem();
        if (!isBlade(blade)) return;
        blade.inventoryTick(wielder.level(), wielder, 0, true);
    }

    /**
     * Advances the combo as one attack input with the given extra commands (for example
     * {@code FORWARD, SPRINT} for the rapid slash dash). Returns the new combo id, or null.
     */
    public static ResourceLocation attack(LivingEntity wielder, Entity target, Command... extra) {
        ItemStack blade = wielder.getMainHandItem();
        Object s = isBlade(blade) ? state(blade) : null;
        if (s == null) return null;
        EnumSet<?> commands = commands(wielder);
        try {
            setTarget.invoke(s, target);
            if (commands != null) {
                add(commands, Command.L_CLICK);
                for (Command c : extra) add(commands, c);
            }
            return (ResourceLocation) progressCombo.invoke(s, wielder);
        } catch (Throwable t) {
            fail(t);
            return null;
        } finally {
            if (commands != null) {
                remove(commands, Command.L_CLICK);
                for (Command c : extra) remove(commands, c);
            }
        }
    }

    /** Releases a fully charged Slash Art at {@code target}. Returns the combo id, or null. */
    public static ResourceLocation slashArts(LivingEntity wielder, Entity target) {
        ItemStack blade = wielder.getMainHandItem();
        Object s = isBlade(blade) ? state(blade) : null;
        if (s == null) return null;
        try {
            setTarget.invoke(s, target);
            int charge = (int) fullChargeTicks.invoke(s, wielder);
            return (ResourceLocation) doChargeAction.invoke(s, wielder, Math.max(1, charge) + 1);
        } catch (Throwable t) {
            fail(t);
            return null;
        }
    }

    // ------------------------------------------------------------------ reflection
    private static boolean ready() {
        if (state == UNKNOWN) init();
        return state == READY;
    }

    private static synchronized void init() {
        if (state != UNKNOWN) return;
        try {
            var mod = ModList.get().getModContainerById(MOD_ID);
            if (mod.isEmpty() || !mod.get().getModInfo().getDisplayName().toLowerCase(java.util.Locale.ROOT).contains(FORK_MARK)) {
                state = OFF;
                return;
            }
            ClassLoader loader = SlashBladeCombat.class.getClassLoader();
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            bladeItem = Class.forName("mods.flammpfeil.slashblade.item.ItemSlashBlade", false, loader);
            Class<?> access = Class.forName("mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess", false, loader);
            Class<?> bladeState = Class.forName("mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState", false, loader);
            Class<?> capability = Class.forName("mods.flammpfeil.slashblade.capability.inputstate.CapabilityInputState", false, loader);
            Class<?> input = Class.forName("mods.flammpfeil.slashblade.capability.inputstate.IInputState", false, loader);
            Class<?> command = Class.forName("mods.flammpfeil.slashblade.util.InputCommand", false, loader);
            stateOf = lookup.findStatic(access, "of", MethodType.methodType(Optional.class, ItemStack.class));
            progressCombo = lookup.findVirtual(bladeState, "progressCombo", MethodType.methodType(ResourceLocation.class, LivingEntity.class));
            doChargeAction = lookup.findVirtual(bladeState, "doChargeAction", MethodType.methodType(ResourceLocation.class, LivingEntity.class, int.class));
            fullChargeTicks = lookup.findVirtual(bladeState, "getFullChargeTicks", MethodType.methodType(int.class, LivingEntity.class));
            setTarget = lookup.findVirtual(bladeState, "setTargetEntityId", MethodType.methodType(void.class, Entity.class));
            isBroken = lookup.findVirtual(bladeState, "isBroken", MethodType.methodType(boolean.class));
            getCommands = lookup.findVirtual(input, "getCommands", MethodType.methodType(EnumSet.class));
            @SuppressWarnings("unchecked")
            Supplier<AttachmentType<?>> supplier = (Supplier<AttachmentType<?>>) capability.getField("INPUT_STATE").get(null);
            inputState = supplier;
            @SuppressWarnings({"unchecked", "rawtypes"})
            Class<? extends Enum> enumType = (Class<? extends Enum>) command;
            commandType = enumType;
            for (Command c : Command.values()) enumConstant(c);   // every name must exist
            state = READY;
        } catch (Throwable t) {
            state = OFF;
            Singularity_Iteration.LOGGER.warn("SlashBlade: Resharped combat bridge is off (API mismatch): {}", t.toString());
        }
    }

    private static Object state(ItemStack stack) {
        try {
            Optional<?> s = (Optional<?>) stateOf.invoke(stack);
            return s.orElse(null);
        } catch (Throwable t) {
            fail(t);
            return null;
        }
    }

    private static EnumSet<?> commands(LivingEntity wielder) {
        try {
            Object input = wielder.getData(inputState.get());
            return (EnumSet<?>) getCommands.invoke(input);
        } catch (Throwable t) {
            fail(t);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Enum<?> enumConstant(Command c) {
        return Enum.valueOf(commandType, c.name());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void add(EnumSet set, Command c) { set.add(enumConstant(c)); }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void remove(EnumSet set, Command c) { set.remove(enumConstant(c)); }

    private static void fail(Throwable t) {
        if (state == READY) {
            state = OFF;
            Singularity_Iteration.LOGGER.warn("SlashBlade: Resharped combat bridge turned off after an error: {}", t.toString());
        }
    }
}
