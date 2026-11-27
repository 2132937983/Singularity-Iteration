// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.compat.slashblade;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.event.handler.FallHandler;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector3f;

/**
 * 0.1.11.234 (SlashBlade collab B2): the SlashBlade content of the collab blades, registered into SlashBlade's registries
 * under {@code mio_icif}: slash arts, special effects and their combo states. Each combo state reuses one of SlashBlade's
 * own motions and adds a timeline of effects (normal, just and super variants, like SlashBlade's own arts). Loaded only by
 * {@link SiSlashBlade} when SlashBlade is installed. The blades name them in {@code data/mio_icif/slashblade/named_blades/}.
 */
final class SiSlashBladeContent {
    private SiSlashBladeContent() {}

    static final DeferredRegister<SlashArts> SLASH_ARTS = DeferredRegister.create(SlashArts.REGISTRY_KEY, Singularity_Iteration.MOD_ID);
    static final DeferredRegister<SpecialEffect> SPECIAL_EFFECTS = DeferredRegister.create(SpecialEffect.REGISTRY_KEY, Singularity_Iteration.MOD_ID);
    static final DeferredRegister<ComboState> COMBO_STATES = DeferredRegister.create(ComboState.REGISTRY_KEY, Singularity_Iteration.MOD_ID);

    /** The nano blade's slash art (piezo cleave). */
    static final DeferredHolder<SlashArts, SlashArts> PIEZO_CLEAVE = SLASH_ARTS.register("piezo_cleave",
        () -> new SlashArts(user -> id("piezo_cleave")).setComboStateJust(user -> id("piezo_cleave_just"))
            .setComboStateSuper(user -> id("piezo_cleave_super")));

    /** The nano blade's special effect (piezo edge); level 0: it is the blade's own nature, not a reward. */
    static final DeferredHolder<SpecialEffect, SpecialEffect> PIEZO_EDGE = SPECIAL_EFFECTS.register("piezo_edge",
        () -> new SpecialEffect(0, false, false));

    static void register(IEventBus modEventBus) {
        for (Variant v : Variant.values()) COMBO_STATES.register("piezo_cleave" + v.suffix, () -> SiNanoBlade.piezoCleave(v));
        SLASH_ARTS.register(modEventBus);
        SPECIAL_EFFECTS.register(modEventBus);
        COMBO_STATES.register(modEventBus);
    }

    enum Variant {
        NORMAL(""), JUST("_just"), SUPER("_super");

        final String suffix;

        Variant(String suffix) { this.suffix = suffix; }
    }

    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path); }

    static ResourceLocation sb(String path) { return ResourceLocation.fromNamespaceAndPath("slashblade", path); }

    /** A combo state on SlashBlade's extra motion {@code start..end} with a timeline, ending like SlashBlade's drive. */
    static ComboState state(int start, int end, Timeline timeline, String timeoutState) {
        return ComboState.Builder.newInstance().startAndEnd(start, end).priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(user -> sb("none"))
            .nextOfTimeout(user -> sb(timeoutState))
            .addTickAction(timeline.build())
            .addTickAction(FallHandler::fallDecrease)
            .addHitEffect(StunManager::setStun)
            .build();
    }

    /** A timeline that runs every action put at a tick (SlashBlade's builder keeps one per tick). */
    static final class Timeline {
        private final TreeMap<Integer, Consumer<LivingEntity>> actions = new TreeMap<>();

        Timeline put(int tick, Consumer<LivingEntity> action) {
            actions.merge(tick, action, Consumer::andThen);
            return this;
        }

        Timeline server(int tick, Consumer<ServerLevel> action) {
            return put(tick, user -> {
                if (user.level() instanceof ServerLevel level) action.accept(level);
            });
        }

        ComboState.TimeLineTickAction build() {
            var builder = ComboState.TimeLineTickAction.getBuilder();
            actions.forEach(builder::put);
            return builder.build();
        }
    }

    /** Living things the user may hit within {@code radius} of {@code center}. */
    static List<LivingEntity> targets(LivingEntity user, Vec3 center, double radius) {
        List<LivingEntity> found = new ArrayList<>();
        for (Entity e : TargetSelector.getTargettableEntitiesWithinAABB(user.level(), user, new AABB(center, center).inflate(radius), radius + 8.0))
            if (e instanceof LivingEntity living && living != user && living.isAlive() && living.distanceToSqr(center) <= radius * radius + 4.0)
                found.add(living);
        return found;
    }

    static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F), scale);
    }

    static void particle(ServerLevel level, ParticleOptions p, Vec3 at) {
        level.sendParticles(p, at.x, at.y, at.z, 1, 0, 0, 0, 0);
    }

    static void sound(LivingEntity user, SoundEvent sound, float volume, float pitch) {
        user.level().playSound(null, user.getX(), user.getY(), user.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
