package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.ArmoryFlight;
import com.miophas.singularity_iteration.common.armory.ArmoryPieceEntity;
import com.miophas.singularity_iteration.common.armory.ArmoryRegistry;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Sound and particle choreography of the suit-up, run on each client from the piece's
 * own clock so every viewer sees and hears the same beats.
 *
 * <pre>
 * hatch:   steam vents, sparks                        (Armory: alarm + hatch, server side)
 * launch:  ignition roar, flash + smoke ring
 * cruise:  turbine whine + jet roar loops, three-layer exhaust:
 *          shock-diamond core / blue-white ion jet / thin vapour trail
 * brake:   retro-thrust blast, exhaust thrown forward, attitude pulses
 * hover:   downward lift jets, pulse-jet pops
 * snap:    rush; latch (clank) -> lock (bolts) -> hiss (seal)
 * return:  unlock -> purge pop -> boost home on full thrust
 * </pre>
 */
public final class ArmoryClientEffects implements ArmoryPieceEntity.Effects {
    private static final int JET = 1, FLYBY = 2, LATCH = 4, LOCK = 8, HISS = 16, UNLOCK = 32, PURGE = 64,
        LAUNCH = 128, BRAKE = 256, PULSE_A = 512, PULSE_B = 1024, PULSE_C = 2048, SNAP = 4096,
        UNFOLD = 8192, SEGMENT = 16384 /* ..1<<17: one bit per segment */, POOF = 1 << 19;
    private static final RandomSource RANDOM = RandomSource.create();

    @Override
    public void tick(ArmoryPieceEntity piece) {
        if (piece.item().isEmpty() && !piece.isReturn()) return;
        float age = piece.age(0);
        if (piece.isReturn()) {
            if (age >= 0) tickReturn(piece, age);
        } else {
            tickDeliver(piece, age);
        }
    }

    // ------------------------------------------------------------------ delivery
    private void tickDeliver(ArmoryPieceEntity piece, float age) {
        int flight = piece.duration();
        if (age < 0) {
            if (age >= -ArmoryFlight.HATCH_TICKS) {
                // pressurising in the hatch
                Vec3 at = piece.flightPosition(0);
                if (RANDOM.nextInt(2) == 0) spawn(piece, ParticleTypes.CLOUD, at.add(jitter(0.25)).add(0, -0.2, 0), new Vec3(0, 0.04, 0).add(jitter(0.03)));
                if (RANDOM.nextInt(3) == 0) spawn(piece, ParticleTypes.ELECTRIC_SPARK, at.add(jitter(0.2)), jitter(0.08));
            }
            return;
        }
        if (age < flight) {
            once(piece, LAUNCH, () -> {
                play(piece, mio_icif_sounds.ARMORY_LAUNCH.get(), 1.0F, 0.95F + RANDOM.nextFloat() * 0.1F);
                Vec3 at = piece.flightPosition(0);
                for (int i = 0; i < 6; i++) spawn(piece, ArmoryRegistry.CORE.get(), at.add(jitter(0.1)), jitter(0.05));
                for (int i = 0; i < 14; i++) {
                    double a = i / 14.0 * Math.PI * 2;
                    spawn(piece, ParticleTypes.CLOUD, at.add(0, -0.3, 0), new Vec3(Math.cos(a) * 0.18, 0.01, Math.sin(a) * 0.18));
                }
            });
            once(piece, JET, () -> {
                Minecraft.getInstance().getSoundManager().play(new JetSound(piece, mio_icif_sounds.ARMORY_JET_LOOP.get(), 0.7F, false));
                Minecraft.getInstance().getSoundManager().play(new JetSound(piece, mio_icif_sounds.ARMORY_TURBINE_LOOP.get(), 0.45F, true));
            });
            double t = age / flight;
            if (flight - age <= 26) once(piece, FLYBY, () -> play(piece, mio_icif_sounds.ARMORY_FLYBY.get(), 0.8F, 1.0F));
            if (t >= ArmoryFlight.BRAKE_AT) {
                once(piece, BRAKE, () -> {
                    play(piece, mio_icif_sounds.ARMORY_BRAKE.get(), 1.0F, 1.0F);
                    retroBlast(piece, 14);
                });
                retroBlast(piece, 3);
                if (t > 0.9) once(piece, PULSE_A, () -> pulse(piece));
            } else {
                exhaust(piece, t < 0.12 ? 2.0 : 1.0);
            }
            return;
        }
        float hover = age - flight;
        // segments swing shut onto their joints one after another: a heavy clank each
        float sinceSnap = hover - ArmoryFlight.HOVER_TICKS;
        int n = ArmoryFlight.segments(piece.piece());
        for (int j = 0; j < n && n > 1; j++) {
            if (sinceSnap < ArmoryFlight.segmentLockTick(j, n)) continue;
            int seg = j;
            once(piece, SEGMENT << j, () -> {
                play(piece, mio_icif_sounds.ARMORY_LOCK.get(), 0.85F, 0.72F + 0.14F * seg);
                burst(piece, ParticleTypes.ELECTRIC_SPARK, 6, 0.18);
                burst(piece, ArmoryRegistry.CORE.get(), 2, 0.05);
            });
        }
        if (hover < ArmoryFlight.HOVER_TICKS) {
            // the closed piece unfolds into its segments in front of the wearer
            once(piece, UNFOLD, () -> {
                play(piece, mio_icif_sounds.ARMORY_UNLOCK.get(), 0.8F, 0.7F);
                burst(piece, ParticleTypes.ELECTRIC_SPARK, 5, 0.12);
            });
            liftJets(piece);
            if (hover >= 2) once(piece, PULSE_B, () -> pulse(piece));
            if (hover >= 6) once(piece, PULSE_C, () -> pulse(piece));
            return;
        }
        float snap = hover - ArmoryFlight.HOVER_TICKS;
        if (snap < ArmoryFlight.SNAP_TICKS) {
            once(piece, SNAP, () -> play(piece, mio_icif_sounds.ARMORY_PULSE.get(), 0.8F, 0.75F));
            exhaust(piece, 1.0);
            return;
        }
        float latch = snap - ArmoryFlight.SNAP_TICKS;
        once(piece, LATCH, () -> {
            play(piece, mio_icif_sounds.ARMORY_LATCH.get(), 1.0F, 0.95F + RANDOM.nextFloat() * 0.1F);
            burst(piece, ParticleTypes.ELECTRIC_SPARK, 10, 0.3);
            burst(piece, ArmoryRegistry.CORE.get(), 4, 0.06);
        });
        if (latch >= 3) once(piece, LOCK, () -> {
            play(piece, mio_icif_sounds.ARMORY_LOCK.get(), 0.9F, 1.0F);
            burst(piece, ParticleTypes.ELECTRIC_SPARK, 4, 0.12);
        });
        if (latch >= ArmoryFlight.LATCH_TICKS - 1) once(piece, HISS, () -> {
            play(piece, mio_icif_sounds.ARMORY_HISS.get(), 0.7F, 1.0F);
            burst(piece, ParticleTypes.CLOUD, 3, 0.05);
        });
    }

    // ------------------------------------------------------------------ return
    private void tickReturn(ArmoryPieceEntity piece, float age) {
        once(piece, UNLOCK, () -> {
            play(piece, mio_icif_sounds.ARMORY_UNLOCK.get(), 0.9F, 1.0F);
            burst(piece, ParticleTypes.ELECTRIC_SPARK, 6, 0.2);
        });
        // every lock blows at once: a "whump" of vented pressure around each piece
        once(piece, POOF, () -> {
            play(piece, mio_icif_sounds.ARMORY_PULSE.get(), 1.0F, 0.55F);
            Vec3 at = piece.flightPosition(0);
            for (int i = 0; i < 16; i++) {
                double a = i / 16.0 * Math.PI * 2;
                spawn(piece, ParticleTypes.CLOUD, at, new Vec3(Math.cos(a) * 0.22, (RANDOM.nextDouble() - 0.3) * 0.12, Math.sin(a) * 0.22));
            }
            burst(piece, ParticleTypes.POOF, 6, 0.1);
            burst(piece, ArmoryRegistry.CORE.get(), 3, 0.08);
        });
        if (age < ArmoryFlight.PURGE_TICKS) {
            if (age < 4) burst(piece, ParticleTypes.CLOUD, 1, 0.03);
            return;
        }
        once(piece, PURGE, () -> {
            play(piece, mio_icif_sounds.ARMORY_PURGE.get(), 1.0F, 1.05F);
            Minecraft.getInstance().getSoundManager().play(new JetSound(piece, mio_icif_sounds.ARMORY_JET_LOOP.get(), 0.6F, false));
            burst(piece, ArmoryRegistry.CORE.get(), 5, 0.08);
            burst(piece, ArmoryRegistry.THRUST.get(), 8, 0.12);
        });
        exhaust(piece, 1.6);
    }

    // ------------------------------------------------------------------ emitters
    private static Vec3 velocity(ArmoryPieceEntity piece) {
        return piece.flightPosition(1).subtract(piece.flightPosition(0));
    }

    /**
     * Main engine plume opposite to the direction of travel, in three layers:
     * shock-diamond core at the nozzle, blue-white ion jet, thin vapour trail.
     */
    private static void exhaust(ArmoryPieceEntity piece, double power) {
        Vec3 now = piece.flightPosition(0);
        Vec3 velocity = velocity(piece);
        double speed = velocity.length();
        if (speed < 1.0E-3) return;
        Vec3 back = velocity.scale(-1 / speed);
        Vec3 nozzle = now.add(back.scale(0.28));
        // 1) shock diamonds: three bright knots spaced down the plume
        for (int i = 0; i < 3; i++) {
            Vec3 at = nozzle.add(back.scale(0.12 * i)).add(velocity.scale(RANDOM.nextDouble()));
            spawn(piece, ArmoryRegistry.CORE.get(), at, back.scale(0.02 + speed * 0.1));
        }
        // 2) ion jet: fast, spreading blue-white flame
        int jet = (int) Math.round(4 * power);
        for (int i = 0; i < jet; i++) {
            Vec3 at = nozzle.add(velocity.scale(RANDOM.nextDouble())).add(jitter(0.03));
            Vec3 v = back.scale(0.18 + RANDOM.nextDouble() * 0.12 + speed * 0.15).add(jitter(0.025));
            spawn(piece, ArmoryRegistry.THRUST.get(), at, v);
        }
        // 3) compressed-air vapour trail, lingering behind
        spawn(piece, ArmoryRegistry.TRAIL.get(), now.add(back.scale(0.5)), back.scale(0.01).add(0, 0.004, 0));
        if (RANDOM.nextInt(3) == 0) spawn(piece, ParticleTypes.ELECTRIC_SPARK, nozzle, back.scale(0.25).add(jitter(0.05)));
    }

    /** Retro-thrust: the plume is thrown forward, ahead of the piece, as it brakes. */
    private static void retroBlast(ArmoryPieceEntity piece, int count) {
        Vec3 now = piece.flightPosition(0);
        Vec3 velocity = velocity(piece);
        Vec3 fwd = velocity.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 0) : velocity.normalize();
        for (int i = 0; i < count; i++) {
            double side = RANDOM.nextBoolean() ? 1 : -1;
            Vec3 lateral = new Vec3(-fwd.z, 0, fwd.x).scale(0.18 * side);
            Vec3 at = now.add(lateral).add(fwd.scale(0.2));
            Vec3 v = fwd.scale(0.28 + RANDOM.nextDouble() * 0.15).add(lateral.scale(0.4)).add(jitter(0.03));
            spawn(piece, i % 3 == 0 ? ArmoryRegistry.CORE.get() : ArmoryRegistry.THRUST.get(), at, v);
        }
        if (RANDOM.nextInt(2) == 0) spawn(piece, ArmoryRegistry.TRAIL.get(), now.add(fwd.scale(0.6)), fwd.scale(0.05));
    }

    /** Hover: small downward lift jets keep the piece floating. */
    private static void liftJets(ArmoryPieceEntity piece) {
        Vec3 at = piece.flightPosition(0).add(0, -0.25, 0);
        spawn(piece, ArmoryRegistry.CORE.get(), at.add(jitter(0.05)), new Vec3(0, -0.06, 0));
        for (int i = 0; i < 2; i++) spawn(piece, ArmoryRegistry.THRUST.get(), at.add(jitter(0.08)), new Vec3(0, -0.15, 0).add(jitter(0.03)));
        if (RANDOM.nextInt(3) == 0) spawn(piece, ArmoryRegistry.TRAIL.get(), at.add(0, -0.3, 0), new Vec3(0, -0.01, 0));
    }

    /** Attitude-control pulse: a sharp pop and a sideways puff. */
    private static void pulse(ArmoryPieceEntity piece) {
        play(piece, mio_icif_sounds.ARMORY_PULSE.get(), 0.7F, 0.9F + RANDOM.nextFloat() * 0.3F);
        Vec3 at = piece.flightPosition(0);
        Vec3 dir = jitter(1).multiply(1, 0.3, 1).normalize();
        spawn(piece, ArmoryRegistry.CORE.get(), at.add(dir.scale(0.25)), dir.scale(0.05));
        for (int i = 0; i < 5; i++) spawn(piece, ArmoryRegistry.THRUST.get(), at.add(dir.scale(0.25)), dir.scale(0.2).add(jitter(0.04)));
        spawn(piece, ArmoryRegistry.TRAIL.get(), at.add(dir.scale(0.45)), dir.scale(0.03));
    }

    private static void burst(ArmoryPieceEntity piece, ParticleOptions type, int count, double speed) {
        Vec3 at = piece.flightPosition(0);
        for (int i = 0; i < count; i++) {
            Vec3 v = jitter(speed);
            spawn(piece, type, at.add(v), v);
        }
    }

    private static void spawn(ArmoryPieceEntity piece, ParticleOptions type, Vec3 at, Vec3 v) {
        ((ClientLevel) piece.level()).addParticle(type, at.x, at.y, at.z, v.x, v.y, v.z);
    }

    private static void once(ArmoryPieceEntity piece, int flag, Runnable action) {
        if ((piece.fxFlags & flag) != 0) return;
        piece.fxFlags |= flag;
        action.run();
    }

    private static void play(ArmoryPieceEntity piece, SoundEvent sound, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(new EntityBoundSoundInstance(sound, SoundSource.PLAYERS, volume, pitch, piece, RANDOM.nextLong()));
    }

    private static Vec3 jitter(double scale) {
        return new Vec3(RANDOM.nextGaussian() * scale, RANDOM.nextGaussian() * scale, RANDOM.nextGaussian() * scale);
    }

    /**
     * Engine loop riding on its piece. The roar follows thrust (loud on launch, swelling in
     * the air brake, low in the hover); the turbine whine follows speed (pitch climbs with
     * speed and falls as the piece brakes).
     */
    static final class JetSound extends AbstractTickableSoundInstance {
        private final ArmoryPieceEntity piece;
        private final float gain;
        private final boolean turbine;
        private Vec3 last;

        JetSound(ArmoryPieceEntity piece, SoundEvent sound, float gain, boolean turbine) {
            super(sound, SoundSource.PLAYERS, RandomSource.create());
            this.piece = piece;
            this.gain = gain;
            this.turbine = turbine;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.pitch = 1.0F;
            Vec3 at = piece.flightPosition(0);
            this.x = at.x; this.y = at.y; this.z = at.z;
            this.last = at;
        }

        @Override
        public void tick() {
            if (piece.isRemoved()) { stop(); return; }
            float age = piece.age(0);
            int flight = piece.duration();
            boolean ret = piece.isReturn();
            float end = ret ? flight : piece.snapEnd();
            if (age >= end) { stop(); return; }
            Vec3 at = piece.flightPosition(0);
            float speed = (float) at.distanceTo(last);
            last = at;
            x = at.x; y = at.y; z = at.z;
            float t = ret ? (age - ArmoryFlight.PURGE_TICKS) / Math.max(1, flight - ArmoryFlight.PURGE_TICKS) : age / flight;
            float level;
            if (ret) level = Mth.clamp(t * 6, 0, 1) * (1 - Mth.clamp((t - 0.8F) / 0.2F, 0, 1));
            else if (age < 3) level = 1.15F;                                                    // ignition
            else if (t < ArmoryFlight.BRAKE_AT) level = 0.85F;
            else if (t < 1) level = turbine ? 0.7F : 1.1F;                                       // air brake roar
            else level = turbine ? 0.35F : 0.45F;                                                 // hover / snap
            volume = gain * level + 0.01F;
            pitch = turbine ? 0.75F + Mth.clamp(speed, 0, 1.6F) * 0.45F : 0.85F + Mth.clamp(speed, 0, 1.6F) * 0.15F;
        }
    }

    /** Ion-jet puff: white-hot core cooling to blue, shrinking and fading. */
    public static final class ThrustParticle extends TextureSheetParticle {
        private final SpriteSet sprites;

        ThrustParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
            super(level, x, y, z, vx, vy, vz);
            this.sprites = sprites;
            this.xd = vx; this.yd = vy; this.zd = vz;
            this.friction = 0.84F;
            this.gravity = 0;
            this.lifetime = 7 + random.nextInt(5);
            this.quadSize = 0.12F + random.nextFloat() * 0.07F;
            this.hasPhysics = false;
            setSpriteFromAge(sprites);
            setColor(1, 1, 1);
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            float k = age / (float) lifetime;
            setColor(1 - 0.6F * k, 1 - 0.3F * k, 1);
            alpha = 1 - k * k;
            quadSize *= 1.02F - 0.08F * k;
        }

        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
        @Override protected int getLightColor(float partialTick) { return 0xF000F0; }

        public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
            @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
                return new ThrustParticle(level, x, y, z, vx, vy, vz, sprites);
            }
        }
    }

    /** Shock-diamond knot: tiny, white-hot, flickering, gone in a few ticks. */
    public static final class CoreParticle extends TextureSheetParticle {
        private final SpriteSet sprites;

        CoreParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
            super(level, x, y, z, vx, vy, vz);
            this.sprites = sprites;
            this.xd = vx; this.yd = vy; this.zd = vz;
            this.friction = 0.6F;
            this.gravity = 0;
            this.lifetime = 3 + random.nextInt(2);
            this.quadSize = 0.10F + random.nextFloat() * 0.05F;
            this.hasPhysics = false;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            quadSize *= 0.82F + random.nextFloat() * 0.2F;   // flicker
            setColor(1, 1, 0.92F + random.nextFloat() * 0.08F);
        }

        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
        @Override protected int getLightColor(float partialTick) { return 0xF000F0; }

        public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
            @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
                return new CoreParticle(level, x, y, z, vx, vy, vz, sprites);
            }
        }
    }

    /** Thin vapour trail: pale, translucent, slowly expanding and fading. */
    public static final class TrailParticle extends TextureSheetParticle {
        private final SpriteSet sprites;

        TrailParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
            super(level, x, y, z, vx, vy, vz);
            this.sprites = sprites;
            this.xd = vx; this.yd = vy; this.zd = vz;
            this.friction = 0.92F;
            this.gravity = -0.002F;
            this.lifetime = 22 + random.nextInt(12);
            this.quadSize = 0.14F + random.nextFloat() * 0.06F;
            this.hasPhysics = false;
            this.alpha = 0.45F;
            setSpriteFromAge(sprites);
            float g = 0.88F + random.nextFloat() * 0.08F;
            setColor(g, g, Math.min(1, g + 0.04F));
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            float k = age / (float) lifetime;
            alpha = 0.45F * (1 - k);
            quadSize *= 1.035F;
        }

        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

        public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
            @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
                return new TrailParticle(level, x, y, z, vx, vy, vz, sprites);
            }
        }
    }
}
