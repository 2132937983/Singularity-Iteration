// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory.blade;

/**
 * 0.1.11.233 (SlashBlade collab B1): the rules of the collab blades' resources, on {@link BladeCore} values only (no
 * SlashBlade, no player), so the GameTests check them without SlashBlade. {@code common.compat.slashblade} calls them from
 * SlashBlade's events.
 */
public final class BladeResources {
    private BladeResources() {}

    /** Why a slash art is refused ({@link #OK} when it is not). */
    public enum Refusal {
        OK, NO_EU, OVERHEATED, NO_MASS, NO_COHERENCE;

        public String langKey() { return "message.mio_icif.blade." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    /** A slash art's outcome: the refusal (or OK) and the core after paying. */
    public record Spend(Refusal refusal, BladeCore core) {
        public boolean ok() { return refusal == Refusal.OK; }
    }

    /** One tick of a blade that a living entity carries: the nano edge vents, the singularity's containment takes EU. */
    public static BladeCore tick(BladeCore c, CollabBlade kind) {
        switch (kind) {
            case NANO -> {
                if (c.heat() <= 0 && !c.overheated()) return c;
                float heat = Math.max(0, c.heat() - CollabBlade.NANO_VENT);
                boolean over = c.overheated() && heat > CollabBlade.NANO_RESUME;
                return c.withHeat(heat, over);
            }
            case SINGULARITY -> {
                if (c.mass() <= 0) return c.unstable() ? c.withMass(0, false) : c;
                if (c.eu() >= CollabBlade.CONTAINMENT_EU) {
                    BladeCore paid = c.withEu(c.eu() - CollabBlade.CONTAINMENT_EU);
                    return c.unstable() ? paid.withMass(c.mass(), false) : paid;
                }
                return c.withEu(0).withMass(Math.max(0, c.mass() - CollabBlade.MASS_LEAK), true);
            }
            default -> {
                return c;
            }
        }
    }

    /**
     * Pays one slash art. {@code coherence} says how the quantum blade is paid: {@link Coherence#PAID} (the suit paid),
     * {@link Coherence#SHORT} (the wearer has a quantum core but too little coherence: refused) or {@link Coherence#NO_CORE}
     * (no quantum core: paid with EU). The other blades ignore it.
     */
    public static Spend slashArt(BladeCore c, CollabBlade kind, Coherence coherence) {
        switch (kind) {
            case NANO -> {
                if (c.overheated()) return new Spend(Refusal.OVERHEATED, c);
                if (c.eu() < kind.saEu) return new Spend(Refusal.NO_EU, c);
                float heat = c.heat() + CollabBlade.NANO_SA_HEAT;
                boolean over = heat >= CollabBlade.NANO_HEAT_CAP;
                return new Spend(Refusal.OK, c.withEu(c.eu() - kind.saEu).withHeat(Math.min(heat, CollabBlade.NANO_HEAT_CAP), over));
            }
            case QUANTUM -> {
                return switch (coherence) {
                    case PAID -> new Spend(Refusal.OK, c);
                    case SHORT -> new Spend(Refusal.NO_COHERENCE, c);
                    case NO_CORE -> c.eu() < kind.saEu ? new Spend(Refusal.NO_EU, c) : new Spend(Refusal.OK, c.withEu(c.eu() - kind.saEu));
                };
            }
            default -> {
                if (c.mass() < CollabBlade.SINGULARITY_SA_MASS) return new Spend(Refusal.NO_MASS, c);
                if (c.eu() < kind.saEu) return new Spend(Refusal.NO_EU, c);
                return new Spend(Refusal.OK, c.withEu(c.eu() - kind.saEu).withMass(c.mass() - CollabBlade.SINGULARITY_SA_MASS, c.unstable()));
            }
        }
    }

    /** How the quantum blade's slash art is paid. */
    public enum Coherence { PAID, SHORT, NO_CORE }

    /** A hit: the nano edge heats up and draws a little EU (only while it has some). */
    public static BladeCore hit(BladeCore c, CollabBlade kind) {
        if (kind != CollabBlade.NANO || c.eu() <= 0) return c;
        float heat = c.heat() + CollabBlade.NANO_HIT_HEAT;
        boolean over = c.overheated() || heat >= CollabBlade.NANO_HEAT_CAP;
        return c.withEu(Math.max(0, c.eu() - CollabBlade.NANO_HIT_EU)).withHeat(Math.min(heat, CollabBlade.NANO_HEAT_CAP), over);
    }

    /** A kill: the singularity takes in mass ({@code big}: a boss or a player). */
    public static BladeCore kill(BladeCore c, CollabBlade kind, boolean big) {
        if (kind != CollabBlade.SINGULARITY) return c;
        float add = big ? CollabBlade.MASS_PER_BIG_KILL : CollabBlade.MASS_PER_KILL;
        return c.withMass(Math.min(CollabBlade.MASS_MAX, c.mass() + add), c.unstable());
    }

    /** EU the blade can still take. */
    public static long room(BladeCore c, CollabBlade kind) {
        return Math.max(0, kind.euMax - c.eu());
    }

    /** Ticks the containment lasts on the EU in the blade (Long.MAX_VALUE without mass). */
    public static long containmentTicks(BladeCore c) {
        return c.mass() <= 0 ? Long.MAX_VALUE : c.eu() / CollabBlade.CONTAINMENT_EU;
    }
}
