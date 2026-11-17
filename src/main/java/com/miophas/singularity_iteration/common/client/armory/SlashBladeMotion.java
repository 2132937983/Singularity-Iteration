// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.48 (Wingman motion audit M3, user decision one A): the arms and legs of a blade-wielding Wingman follow
 * SlashBlade: Resharped's own player motion, so the blade it draws (from the same combo state) is in the hand, and the
 * draw, the combos and the sheathing play as they do for a player.
 *
 * <h2>How SlashBlade animates</h2>
 * The blade layer draws the blade and the sheath from the current combo state of the blade
 * ({@code ISlashBladeState#peekCurrentComboStateTicks}: the combo id and its ticks) for any living entity. The arms come
 * from the PlayerAnimator compat ({@code PlayerAnimationOverrider}: a {@code VmdAnimation} per combo id, played from the
 * {@code player_motion.vmd}), which SlashBlade starts for players only. This bridge does the same for a Wingman: it reads
 * the combo of the held blade (the blade state is synced with the item), keeps one animation of SlashBlade per Wingman,
 * restarts it at the combo's tick when the combo changes, ticks it with the Wingman, and copies its rotations of the arms
 * and legs onto the Wingman model.
 *
 * <p>No compile dependency: the classes of SlashBlade: Resharped (checked against 2.1.9) and PlayerAnimator (2.0.4) are
 * looked up once. Without PlayerAnimator, with another SlashBlade build, or after any error the bridge turns off with one
 * log line and the Wingman uses its own key frames ({@code WingmanPose#slash}).
 */
@OnlyIn(Dist.CLIENT)
public final class SlashBladeMotion {
    private SlashBladeMotion() {}

    private static final int UNKNOWN = 0, READY = 1, OFF = -1;
    private static volatile int state = UNKNOWN;

    private static MethodHandle stateOf, peek, animations, tick, setupAnim, get3D, isActive, play, vec, vx, vy, vz;
    private static Class<?> playerAnimationBase;
    private static Object rotation, position;

    /** The SlashBlade animation of one Wingman and the combo it plays. */
    private static final class Track {
        @Nullable ResourceLocation combo;
        @Nullable Object animation;
        int tick = Integer.MIN_VALUE;
    }

    private static final Map<LivingEntity, Track> TRACKS = new WeakHashMap<>();

    /** True when the bridge works (SlashBlade: Resharped and PlayerAnimator are there and match). */
    public static boolean available() {
        int s = state;
        if (s == UNKNOWN) s = init();
        return s == READY;
    }

    /**
     * The running SlashBlade animation of {@code wielder} for this frame (advanced to its tick), or null (no combo with a
     * player motion, or the bridge is off). Call once per frame before {@link #apply}.
     */
    @Nullable
    public static Object update(LivingEntity wielder) {
        if (!available()) return null;
        try {
            ItemStack blade = wielder.getMainHandItem();
            Object bladeState = ((Optional<?>) stateOf.invoke(blade)).orElse(null);
            if (bladeState == null) return null;
            Map.Entry<?, ?> current = (Map.Entry<?, ?>) peek.invoke(bladeState, wielder);
            if (current == null || !(current.getValue() instanceof ResourceLocation combo)) return null;
            int ticks = current.getKey() instanceof Number n ? n.intValue() : 0;
            Track track = TRACKS.computeIfAbsent(wielder, w -> new Track());
            int now = wielder.tickCount;
            if (!combo.equals(track.combo) || track.tick == Integer.MIN_VALUE || now < track.tick || now - track.tick > 4) {
                // a new combo (or the track was lost): start SlashBlade's animation of it at the combo's tick
                track.combo = combo;
                track.animation = null;
                @SuppressWarnings("unchecked")
                Map<ResourceLocation, Supplier<?>> map = (Map<ResourceLocation, Supplier<?>>) animations.invoke();
                Supplier<?> supplier = map == null ? null : map.get(combo);
                Object animation = supplier == null ? null : supplier.get();
                if (animation != null && playerAnimationBase.isInstance(animation)) {
                    play.invoke(animation, Math.max(0, ticks) + 1);
                    track.animation = animation;
                }
                track.tick = now;
            }
            while (track.tick < now && track.animation != null) {
                tick.invoke(track.animation);
                track.tick++;
            }
            track.tick = now;
            return track.animation != null && (boolean) isActive.invoke(track.animation) ? track.animation : null;
        } catch (Throwable t) {
            turnOff("update", t);
            return null;
        }
    }

    /**
     * Copies the rotations of SlashBlade's animation onto the arms and legs ({@code animation} from {@link #update}).
     * Returns false when nothing was applied.
     */
    public static boolean apply(Object animation, float partialTick, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        return apply(animation, partialTick, rightArm, leftArm, rightLeg, leftLeg, null, true);
    }

    /**
     * 0.1.11.61 (hit feel and constructivist FX phase Q0, decision two A): with the torso ({@code body} part: its bend) too,
     * and the legs only when {@code legs} (on the ground; in the air the Wingman keeps its flight legs, which before stepped
     * as if walking).
     */
    public static boolean apply(Object animation, float partialTick, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg,
                                @org.jetbrains.annotations.Nullable ModelPart torso, boolean legs) {
        if (animation == null || state != READY) return false;
        try {
            setupAnim.invoke(animation, partialTick);
            part(animation, "rightArm", rightArm, partialTick);
            part(animation, "leftArm", leftArm, partialTick);
            if (torso != null) part(animation, "torso", torso, partialTick);
            if (legs) {
                part(animation, "rightLeg", rightLeg, partialTick);
                part(animation, "leftLeg", leftLeg, partialTick);
            }
            return true;
        } catch (Throwable t) {
            turnOff("apply", t);
            return false;
        }
    }

    /**
     * 0.1.11.61 (Q0): the whole-body transform of the animation ({@code body}): {position x, y, z, rotation x, y, z}, as
     * PlayerAnimator applies it to a player (in {@code setupRotations}: a translate by the position and 0.7 up, the rotations
     * about z, y, x, a translate 0.7 down). Null when the bridge is off.
     */
    @org.jetbrains.annotations.Nullable
    public static float[] bodyTransform(Object animation, float partialTick) {
        if (animation == null || state != READY) return null;
        try {
            setupAnim.invoke(animation, partialTick);
            Object zero = vec.invoke(0F, 0F, 0F);
            Object p = get3D.invoke(animation, "body", position, partialTick, zero);
            Object r = get3D.invoke(animation, "body", rotation, partialTick, zero);
            float[] out = {num(vx.invoke(p)), num(vy.invoke(p)), num(vz.invoke(p)), num(vx.invoke(r)), num(vy.invoke(r)), num(vz.invoke(r))};
            for (float f : out) if (!Float.isFinite(f)) return null;
            return out;
        } catch (Throwable t) {
            turnOff("body", t);
            return null;
        }
    }

    private static float num(Object o) { return o instanceof Number n ? n.floatValue() : 0F; }

    private static void part(Object animation, String name, ModelPart part, float partialTick) throws Throwable {
        Object current = vec.invoke(part.xRot, part.yRot, part.zRot);
        Object v = get3D.invoke(animation, name, rotation, partialTick, current);
        if (v == null) return;
        float x = ((Number) vx.invoke(v)).floatValue(), y = ((Number) vy.invoke(v)).floatValue(), z = ((Number) vz.invoke(v)).floatValue();
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) return;
        part.xRot = x;
        part.yRot = y;
        part.zRot = z;
    }

    private static synchronized int init() {
        if (state != UNKNOWN) return state;
        ModList mods = ModList.get();
        boolean resharped = mods.getModContainerById("slashblade")
            .map(c -> c.getModInfo().getDisplayName().toLowerCase(java.util.Locale.ROOT).contains("resharp")).orElse(false);
        if (!resharped || !mods.isLoaded("playeranimator")) return state = OFF;
        try {
            ClassLoader cl = SlashBladeMotion.class.getClassLoader();
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Class<?> access = Class.forName("mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess", false, cl);
            Class<?> bladeState = Class.forName("mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState", false, cl);
            Class<?> overrider = Class.forName("mods.flammpfeil.slashblade.compat.playerAnim.PlayerAnimationOverrider", false, cl);
            playerAnimationBase = Class.forName("mods.flammpfeil.slashblade.compat.playerAnim.PlayerAnimationBase", false, cl);
            Class<?> animation = Class.forName("dev.kosmx.playerAnim.api.layered.IAnimation", false, cl);
            Class<?> transform = Class.forName("dev.kosmx.playerAnim.api.TransformType", false, cl);
            Class<?> vec3f = Class.forName("dev.kosmx.playerAnim.core.util.Vec3f", false, cl);
            Class<?> vector3 = Class.forName("dev.kosmx.playerAnim.core.util.Vector3", false, cl);
            stateOf = lookup.findStatic(access, "of", MethodType.methodType(Optional.class, ItemStack.class));
            peek = lookup.findVirtual(bladeState, "peekCurrentComboStateTicks", MethodType.methodType(Map.Entry.class, LivingEntity.class));
            Object instance = lookup.findStatic(overrider, "getInstance", MethodType.methodType(overrider)).invoke();
            animations = lookup.findVirtual(overrider, "getAnimation", MethodType.methodType(Map.class)).bindTo(instance);
            tick = lookup.findVirtual(animation, "tick", MethodType.methodType(void.class));
            setupAnim = lookup.findVirtual(animation, "setupAnim", MethodType.methodType(void.class, float.class));
            isActive = lookup.findVirtual(animation, "isActive", MethodType.methodType(boolean.class));
            get3D = lookup.findVirtual(animation, "get3DTransform", MethodType.methodType(vec3f, String.class, transform, float.class, vec3f));
            play = lookup.findVirtual(playerAnimationBase, "play", MethodType.methodType(void.class, int.class));
            vec = lookup.findConstructor(vec3f, MethodType.methodType(void.class, float.class, float.class, float.class));
            vx = lookup.findVirtual(vector3, "getX", MethodType.methodType(Number.class));
            vy = lookup.findVirtual(vector3, "getY", MethodType.methodType(Number.class));
            vz = lookup.findVirtual(vector3, "getZ", MethodType.methodType(Number.class));
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object rot = Enum.valueOf((Class) transform, "ROTATION");
            rotation = rot;
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object pos = Enum.valueOf((Class) transform, "POSITION");
            position = pos;
            Singularity_Iteration.LOGGER.info("Wingman: SlashBlade: Resharped and PlayerAnimator found, Wingmen move with the blade's own motion");
            return state = READY;
        } catch (Throwable t) {
            turnOff("link", t);
            return OFF;
        }
    }

    private static void turnOff(String step, Throwable t) {
        if (state == OFF) return;
        state = OFF;
        TRACKS.clear();
        Singularity_Iteration.LOGGER.warn("Wingman: SlashBlade motion bridge {} failed, Wingmen use their own blade key frames ({})", step, t.toString());
    }
}
