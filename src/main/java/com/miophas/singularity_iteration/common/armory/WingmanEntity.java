// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitEnergyPool;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Wingman (0.1.8.0): a stored Armory suit that flies as an empty-suit drone next to its owner.
 *
 * <h2>Specification</h2>
 * <ul>
 *   <li>The Wingman wears the real items of one Armory suit (four armor pieces, main hand, off
 *       hand). The items are moved, not copied. Thus the Wingman has the armor value, the
 *       weapon damage, the enchantments and the electric armor protection of the suit, as for a
 *       player who wears it. The base values are the player base values (20 health, attack
 *       damage 1, attack speed 4).</li>
 *   <li>The items never disappear. Recall, owner logout, owner in a different dimension, low
 *       power and a removal (for example {@code /kill}) put the items back into the Armory. If
 *       the Armory has no free position, the items go to the owner inventory, else they drop
 *       at the Wingman.</li>
 *   <li>A hit that would kill the Wingman sets it to 1 health. It then returns to the Armory
 *       and is invulnerable on the way. Only void damage and {@code /kill} remove it at once.</li>
 *   <li>The owner and the other Wingmen of the owner cannot hurt it.</li>
 *   <li>Upkeep: {@link #UPKEEP_EU} EU per second. The electric pieces of the suit pay first (the
 *       full-set pool applies). If they cannot pay, the Armory pays when it is loaded. After
 *       {@link #LOW_POWER_LIMIT} seconds without power, the Wingman returns.</li>
 * </ul>
 */
@SuppressWarnings("null")
public class WingmanEntity extends Mob {
    /** EU per second of flight. */
    public static final long UPKEEP_EU = 160;
    /** EU of one Fulton recovery (paid by the Armory). */
    public static final long FULTON_EU = 4000;
    /** Seconds without power before the Wingman returns. */
    public static final int LOW_POWER_LIMIT = 5;
    /** Mobs that a Fulton recovery cannot take (data pack tag). */
    public static final TagKey<EntityType<?>> FULTON_IMMUNE = TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "wingman/fulton_immune"));

    public static final byte ACTIVE = 0, RETURNING = 1, LIFTING = 2;

    private static final double CRUISE = 0.9, COMBAT = 1.15, ACCEL = 0.14;
    private static final double SCAN_RANGE = 24, LEASH = 28, CATCH_UP = 64;

    private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.BYTE);

    // server state (saved)
    @Nullable private UUID owner;
    @Nullable private GlobalPos armory;
    private int set;
    // server state (not saved)
    @Nullable private LivingEntity target;
    private int attackCooldown, deflectCooldown, lowPowerSeconds, stateTicks, slot, slotCount = 1;
    private int lastFulton = -1;
    private boolean secured, protectedReturn;

    public WingmanEntity(EntityType<? extends WingmanEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0);   // items leave only through secure()
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.ATTACK_SPEED, 4.0)
            .add(Attributes.FLYING_SPEED, 0.6)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, SCAN_RANGE);
    }

    /** A new Wingman that wears {@code items} (indexed by {@link ArmoryPiece#ordinal()}). */
    static WingmanEntity create(ServerLevel level, ServerPlayer owner, GlobalPos armory, int set, WingmanMode mode, ItemStack[] items, Vec3 at) {
        WingmanEntity w = new WingmanEntity(ArmoryRegistry.WINGMAN.get(), level);
        w.owner = owner.getUUID();
        w.armory = armory;
        w.set = set;
        w.setMode(mode);
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            ItemStack stack = items[p.ordinal()];
            if (stack != null && !stack.isEmpty()) w.setItemSlot(p.slot, stack);   // first tick: no equip sound
        }
        w.moveTo(at.x, at.y, at.z, owner.getYRot(), 0);
        return w;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MODE, (byte) 0);
        builder.define(DATA_STATE, ACTIVE);
    }

    // ------------------------------------------------------------------ accessors
    public WingmanMode mode() { return WingmanMode.byId(entityData.get(DATA_MODE)); }
    public void setMode(WingmanMode mode) { entityData.set(DATA_MODE, (byte) mode.ordinal()); target = null; }
    public byte state() { return entityData.get(DATA_STATE); }
    private void setState(byte state) { entityData.set(DATA_STATE, state); stateTicks = 0; }
    @Nullable public UUID owner() { return owner; }
    public int set() { return set; }
    @Nullable GlobalPos armory() { return armory; }
    void setSlot(int slot, int count) { this.slot = slot; this.slotCount = Math.max(1, count); }

    /** Fraction of the stored energy of the electric pieces, or -1 without electric pieces. */
    public float energyFraction() {
        var api = MioIcifAPI.instance().getItemAPI();
        long stored = 0, capacity = 0;
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            ItemStack s = getItemBySlot(p.slot);
            if (s.isEmpty() || !api.isElectricArmor(s)) continue;
            stored += Math.max(0, api.getElectricArmorStored(s));
            capacity += Math.max(0, api.getElectricArmorMaxEnergy(s));
        }
        return capacity <= 0 ? -1 : (float) stored / capacity;
    }

    // ------------------------------------------------------------------ server logic
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel world) || secured) return;
        stateTicks++;
        if (attackCooldown > 0) attackCooldown--;
        if (deflectCooldown > 0) deflectCooldown--;
        ServerPlayer player = owner == null ? null : world.getServer().getPlayerList().getPlayer(owner);
        boolean ownerHere = player != null && player.isAlive() && player.level() == world && !player.isSpectator();
        if (state() != RETURNING && !ownerHere) {
            startReturn(null);
        }
        if (tickCount % 20 == 0 && state() != RETURNING) upkeep(world, player);
        switch (state()) {
            case RETURNING -> tickReturn(world);
            case LIFTING -> tickLift(world, player);
            default -> {
                if (!ownerHere) return;
                if (distanceToSqr(player) > CATCH_UP * CATCH_UP) catchUp(player);
                switch (mode()) {
                    case OFFENSIVE -> tickOffensive(player);
                    case DISPERSAL -> tickDispersal(player);
                    case ESCORT -> tickEscort(world, player);
                    case FULTON -> tickFulton(world, player);
                }
            }
        }
    }

    private void upkeep(ServerLevel world, @Nullable ServerPlayer player) {
        long paid = 0;
        var api = MioIcifAPI.instance().getItemAPI();
        for (EquipmentSlot s : SuitEnergyPool.ARMOR_SLOTS) {
            ItemStack piece = getItemBySlot(s);
            if (piece.isEmpty() || !api.isElectricArmor(piece) || SuitEnergyPool.available(this, piece) <= 0) continue;
            paid += SuitEnergyPool.pay(this, piece, UPKEEP_EU - paid);
            if (paid >= UPKEEP_EU) break;
        }
        if (paid < UPKEEP_EU) {
            mio_icif_armory home = loadedArmory(world);
            var payment = home == null ? null : home.getEnergyStorageInternal().scexReserveInternal(UPKEEP_EU - paid);
            if (payment != null) { payment.commit(); paid = UPKEEP_EU; }
        }
        if (paid >= UPKEEP_EU) { lowPowerSeconds = 0; return; }
        if (++lowPowerSeconds >= LOW_POWER_LIMIT) {
            startReturn(player == null ? null : Component.translatable("message.mio_icif.wingman.low_power"));
        }
    }

    /** The Armory of this Wingman when its chunk is loaded (no chunk load), else null. */
    @Nullable
    private mio_icif_armory loadedArmory(ServerLevel world) {
        if (armory == null) return null;
        ServerLevel level = world.getServer().getLevel(armory.dimension());
        if (level == null || !level.isLoaded(armory.pos())) return null;
        return level.getBlockEntity(armory.pos()) instanceof mio_icif_armory a ? a : null;
    }

    // ---- modes
    private void tickOffensive(ServerPlayer player) {
        if (tickCount % 10 == 0 || !valid(target, player, LEASH)) target = pickOffensive(player);
        if (target != null) engage(target, player);
        else steer(formation(player), CRUISE, player);
    }

    private void tickDispersal(ServerPlayer player) {
        double angle = Math.toRadians(player.getYRot()) + slot * (Math.PI * 2 / slotCount);
        Vec3 station = player.position().add(-Math.sin(angle) * 10, 3.5, Math.cos(angle) * 10);
        if (tickCount % 10 == 0 || !valid(target, player, LEASH)) {
            target = nearestHostile(station, 14, player);
            // decoy: the mobs near the station that hunt the owner turn to this Wingman
            for (Mob mob : level().getEntitiesOfClass(Mob.class, new AABB(station, station).inflate(12),
                    m -> m instanceof Enemy && m.getTarget() == player)) mob.setTarget(this);
        }
        if (target != null) engage(target, player);
        else steer(station, CRUISE, player);
    }

    private void tickEscort(ServerLevel world, ServerPlayer player) {
        // projectiles that fly at the owner are destroyed in flight
        Projectile threat = deflectCooldown > 0 ? null : nearestThreat(player);
        if (threat != null) {
            steer(threat.position(), COMBAT * 1.3, player);
            if (distanceToSqr(threat) < 2.6 * 2.6) {
                world.sendParticles(ParticleTypes.ELECTRIC_SPARK, threat.getX(), threat.getY(), threat.getZ(), 10, 0.15, 0.15, 0.15, 0.2);
                world.playSound(null, threat.getX(), threat.getY(), threat.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 0.8F, 1.3F);
                threat.discard();
                deflectCooldown = 3;
            }
            return;
        }
        if (tickCount % 10 == 0 || !valid(target, player, 10)) {
            LivingEntity attacker = player.getLastHurtByMob();
            target = attacker != null && player.tickCount - player.getLastHurtByMobTimestamp() < 100 && valid(attacker, player, 10) ? attacker : null;
        }
        if (target != null) engage(target, player);
        else steer(formation(player), CRUISE, player);
    }

    private void tickFulton(ServerLevel world, ServerPlayer player) {
        if (tickCount % 10 == 0 && !fultonCandidate(target, player)) target = pickFulton(player);
        if (target == null) { steer(formation(player), CRUISE, player); return; }
        steer(target.position().add(0, target.getBbHeight() + 0.7, 0), COMBAT, player);
        face(target.position());
        if (distanceToSqr(target.position().add(0, target.getBbHeight(), 0)) < 1.9 * 1.9) {
            mio_icif_armory home = fultonArmory(world);
            if (home == null) {
                player.displayClientMessage(Component.translatable("message.mio_icif.wingman.fulton_no_armory"), true);
                target = null;
                return;
            }
            var payment = home.getEnergyStorageInternal().scexReserveInternal(FULTON_EU);
            if (payment == null) {
                player.displayClientMessage(Component.translatable("message.mio_icif.armory.no_energy", FULTON_EU, home.storedEnergy()), true);
                target = null;
                return;
            }
            payment.commit();
            lastFulton = target.getId();
            target.startRiding(this, true);
            world.playSound(null, getX(), getY(), getZ(), mio_icif_sounds.ARMORY_LATCH.get(), SoundSource.NEUTRAL, 1.0F, 0.8F);
            setState(LIFTING);
        }
    }

    /** The paired Armory for a Fulton recovery: same dimension as this Wingman, loaded or not. */
    @Nullable
    private mio_icif_armory fultonArmory(ServerLevel world) {
        if (armory == null || armory.dimension() != world.dimension()) return null;
        return world.getBlockEntity(armory.pos()) instanceof mio_icif_armory a ? a : null;
    }

    private void tickLift(ServerLevel world, @Nullable ServerPlayer player) {
        Entity cargo = getFirstPassenger();
        if (cargo == null) { setState(ACTIVE); return; }
        setDeltaMovement(getDeltaMovement().scale(0.6).add(0, 0.18, 0));
        if (stateTicks % 3 == 0) world.sendParticles(ParticleTypes.CLOUD, getX(), getY() - 0.4, getZ(), 2, 0.2, 0.1, 0.2, 0.02);
        if (stateTicks < 30) return;
        mio_icif_armory home = fultonArmory(world);
        Vec3 drop = home == null ? null : dropPoint(world, home.getBlockPos(), cargo);
        ejectPassengers();
        if (drop != null) {
            cargo.teleportTo(drop.x, drop.y, drop.z);
            cargo.fallDistance = 0;
            if (cargo instanceof Mob mob) mob.setPersistenceRequired();
            world.playSound(null, drop.x, drop.y, drop.z, mio_icif_sounds.ARMORY_SEAL.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            if (player != null) player.displayClientMessage(Component.translatable("message.mio_icif.wingman.fulton_done", cargo.getDisplayName()), true);
        } else if (player != null) {
            cargo.teleportTo(player.getX(), player.getY(), player.getZ());
        }
        target = null;
        if (player != null && player.level() == world) catchUp(player);
        setState(ACTIVE);
    }

    /** A free place next to the Armory for a recovered mob: two-high air on solid ground. */
    @Nullable
    private static Vec3 dropPoint(ServerLevel world, BlockPos home, Entity cargo) {
        int height = Math.max(1, Mth.ceil(cargo.getBbHeight()));
        for (int r = 2; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    for (int dy = -1; dy <= 2; dy++) {
                        BlockPos feet = home.offset(dx, dy, dz);
                        if (!world.getBlockState(feet.below()).isFaceSturdy(world, feet.below(), net.minecraft.core.Direction.UP)) continue;
                        boolean free = true;
                        for (int h = 0; h < height && free; h++) free = world.getBlockState(feet.above(h)).getCollisionShape(world, feet.above(h)).isEmpty();
                        if (free) return Vec3.atBottomCenterOf(feet);
                    }
                }
            }
        }
        return Vec3.atCenterOf(home).add(0, 1.2, 0);
    }

    // ---- targets
    private boolean valid(@Nullable LivingEntity e, ServerPlayer player, double leash) {
        return e != null && e.isAlive() && e.level() == level() && !e.isRemoved() && e != player
            && !(e instanceof Player) && !(e instanceof WingmanEntity) && e.distanceToSqr(player) < leash * leash
            && !player.isAlliedTo(e) && !(e instanceof net.minecraft.world.entity.OwnableEntity own && player.getUUID().equals(own.getOwnerUUID()));
    }

    @Nullable
    private LivingEntity pickOffensive(ServerPlayer player) {
        LivingEntity marked = player.getLastHurtMob();
        if (marked != null && player.tickCount - player.getLastHurtMobTimestamp() < 200 && valid(marked, player, LEASH)) return marked;
        LivingEntity attacker = player.getLastHurtByMob();
        if (attacker != null && player.tickCount - player.getLastHurtByMobTimestamp() < 200 && valid(attacker, player, LEASH)) return attacker;
        return nearestHostile(player.position(), SCAN_RANGE, player);
    }

    @Nullable
    private LivingEntity nearestHostile(Vec3 around, double range, ServerPlayer player) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Mob mob : level().getEntitiesOfClass(Mob.class, new AABB(around, around).inflate(range), m -> m instanceof Enemy)) {
            if (!valid(mob, player, LEASH)) continue;
            double d = mob.distanceToSqr(around);
            if (d < bestD) { bestD = d; best = mob; }
        }
        return best;
    }

    @Nullable
    private Projectile nearestThreat(ServerPlayer player) {
        Vec3 c = player.getBoundingBox().getCenter();
        Projectile best = null;
        double bestD = Double.MAX_VALUE;
        for (Projectile p : level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(8))) {
            Entity shooter = p.getOwner();
            if (shooter == player || shooter instanceof WingmanEntity) continue;
            Vec3 v = p.getDeltaMovement();
            double v2 = v.lengthSqr();
            if (v2 < 0.01) continue;                                   // lying still (an arrow in the ground)
            Vec3 rel = c.subtract(p.position());
            double t = rel.dot(v) / v2;                                // ticks to the closest approach
            if (t <= 0 || t > 20) continue;
            if (p.position().add(v.scale(t)).distanceToSqr(c) > 2.2 * 2.2) continue;
            double d = p.distanceToSqr(this);
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    private boolean fultonCandidate(@Nullable LivingEntity e, ServerPlayer player) {
        if (e == null || !e.isAlive() || e.level() != level() || e instanceof Player || e instanceof WingmanEntity) return false;
        if (e.isPassenger() || e.isVehicle() || e.getId() == lastFulton) return false;
        if (e.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES) || e.getType().is(FULTON_IMMUNE)) return false;
        if (e.getBbWidth() > 1.6F || e.getBbHeight() > 2.8F || e.distanceToSqr(player) > LEASH * LEASH) return false;
        if (e instanceof net.minecraft.world.entity.OwnableEntity own && own.getOwnerUUID() != null && !player.getUUID().equals(own.getOwnerUUID())) return false;
        return !(e instanceof Enemy) || e.getHealth() <= e.getMaxHealth() * 0.5F;   // a hostile mob must be weakened first
    }

    @Nullable
    private LivingEntity pickFulton(ServerPlayer player) {
        LivingEntity marked = player.getLastHurtMob();
        if (marked != null && player.tickCount - player.getLastHurtMobTimestamp() < 100 && fultonCandidate(marked, player)) return marked;
        if (!player.isCrouching()) return null;
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getViewVector(1.0F).scale(24));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
            e -> e instanceof LivingEntity && !e.isSpectator(), 24 * 24);
        return hit != null && hit.getEntity() instanceof LivingEntity l && fultonCandidate(l, player) ? l : null;
    }

    // ---- combat and flight
    private void engage(LivingEntity enemy, ServerPlayer player) {
        ItemStack weapon = getMainHandItem();
        boolean ranged = weapon.getItem() instanceof BowItem || weapon.getItem() instanceof CrossbowItem;
        face(enemy.getEyePosition());
        if (ranged) {
            Vec3 away = position().subtract(enemy.position());
            Vec3 flat = new Vec3(away.x, 0, away.z);
            Vec3 stand = enemy.position().add(flat.lengthSqr() < 1.0E-4 ? new Vec3(9, 0, 0) : flat.normalize().scale(9)).add(0, 4, 0);
            steer(stand, CRUISE, player);
            if (attackCooldown == 0 && hasLineOfSight(enemy)) { shoot(enemy, weapon); attackCooldown = 25; }
            return;
        }
        steer(enemy.position().add(0, enemy.getBbHeight() * 0.5, 0), COMBAT, player);
        if (attackCooldown == 0 && isWithinMeleeAttackRange(enemy)) {
            swing(InteractionHand.MAIN_HAND);
            doHurtTarget(enemy);
            double speed = Math.max(0.5, getAttributeValue(Attributes.ATTACK_SPEED));
            attackCooldown = Mth.ceil(20.0 / speed);
        }
    }

    private void shoot(LivingEntity enemy, ItemStack weapon) {
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), 1.0F, weapon);
        double dx = enemy.getX() - getX(), dy = enemy.getY(0.3333) - arrow.getY(), dz = enemy.getZ() - getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + flat * 0.2, dz, 1.6F, 1.0F);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        level().addFreshEntity(arrow);
        playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
    }

    /** Escort place of this Wingman: behind and to the side of the owner, at shoulder height. */
    private Vec3 formation(ServerPlayer player) {
        double[][] offsets = {{-1.6, 1.2, -1.8}, {1.6, 1.2, -1.8}, {-2.9, 1.7, -0.6}, {2.9, 1.7, -0.6}};
        double[] o = offsets[Math.floorMod(slot, offsets.length)];
        double yaw = Math.toRadians(player.getYRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);       // forward
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);      // right
        return player.position().add(rx * o[0] + fx * o[2], o[1], rz * o[0] + fz * o[2]);
    }

    /** Steers the velocity to {@code goal} with a limited acceleration; climbs over obstacles. */
    private void steer(Vec3 goal, double speed, @Nullable ServerPlayer player) {
        Vec3 to = goal.subtract(position());
        double dist = to.length();
        Vec3 want = dist < 1.0E-3 ? Vec3.ZERO : to.scale(Math.min(speed, dist * 0.3) / dist);
        Vec3 v = getDeltaMovement();
        Vec3 dv = want.subtract(v);
        if (dv.lengthSqr() > ACCEL * ACCEL) dv = dv.normalize().scale(ACCEL);
        v = v.add(dv);
        if (horizontalCollision) v = v.add(0, 0.3, 0);
        setDeltaMovement(v);
        if (target == null) {
            if (v.horizontalDistanceSqr() > 0.02) face(position().add(v));
            else if (player != null) setYaw(player.getYRot());
        }
    }

    private void face(Vec3 at) {
        Vec3 d = at.subtract(getEyePosition());
        setYaw((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
    }

    private void setYaw(float yaw) {
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    private void catchUp(ServerPlayer player) {
        Vec3 at = formation(player).add(0, 2, 0);
        teleportTo(at.x, at.y, at.z);
        setDeltaMovement(Vec3.ZERO);
    }

    // ---- return home
    /** Starts the flight home. A message, if any, goes to the owner. */
    void startReturn(@Nullable Component message) {
        if (state() == RETURNING || secured) return;
        dropCargo();
        target = null;
        setState(RETURNING);
        if (message != null && level() instanceof ServerLevel world && owner != null) {
            ServerPlayer player = world.getServer().getPlayerList().getPlayer(owner);
            if (player != null) player.displayClientMessage(message, true);
        }
    }

    private void tickReturn(ServerLevel world) {
        mio_icif_armory home = loadedArmory(world);
        boolean near = home != null && home.getLevel() == world && home.launchTop().distanceToSqr(position()) < 160 * 160;
        if (near) {
            Vec3 top = home.launchTop();
            steer(top, CRUISE, null);
            if (top.distanceToSqr(position()) < 2.0 * 2.0 || stateTicks > 400) {
                world.playSound(null, top.x, top.y, top.z, mio_icif_sounds.ARMORY_LATCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                secure();
                discard();
            }
            return;
        }
        // far away or in a different dimension: climb out, then the Armory recalls the suit
        setDeltaMovement(getDeltaMovement().scale(0.8).add(0, 0.12, 0));
        if (stateTicks > 30) {
            world.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            world.playSound(null, getX(), getY(), getZ(), mio_icif_sounds.ARMORY_LAUNCH.get(), SoundSource.NEUTRAL, 1.0F, 1.2F);
            secure();
            discard();
        }
    }

    /**
     * Puts every item back: the Armory first (suit {@link #set}), then the owner inventory, then
     * the ground at this Wingman. Runs at most once. The Armory chunk can load for it.
     */
    /** Lets go of a Fulton cargo in the air; the cargo gets 10 s of Slow Falling. */
    private void dropCargo() {
        for (Entity cargo : getPassengers()) {
            if (cargo instanceof LivingEntity living) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 200));
            }
        }
        ejectPassengers();
    }

    void secure() {
        if (secured || !(level() instanceof ServerLevel world)) return;
        secured = true;
        dropCargo();
        mio_icif_armory home = null;
        if (armory != null) {
            ServerLevel level = world.getServer().getLevel(armory.dimension());
            if (level != null && level.isInWorldBounds(armory.pos()) && level.getBlockEntity(armory.pos()) instanceof mio_icif_armory a) home = a;
        }
        ServerPlayer player = owner == null ? null : world.getServer().getPlayerList().getPlayer(owner);
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            ItemStack stack = getItemBySlot(p.slot);
            if (stack.isEmpty()) continue;
            setItemSlot(p.slot, ItemStack.EMPTY);
            ItemStack rest = home != null ? home.store(set, p, stack) : stack;
            if (!rest.isEmpty() && player != null) {
                player.getInventory().add(rest);
            }
            if (!rest.isEmpty()) spawnAtLocation(rest);
        }
        if (home != null) home.markStorageChanged();
    }

    // ------------------------------------------------------------------ damage, death, removal
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (protectedReturn && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        Entity attacker = source.getEntity();
        if (attacker != null && owner != null && (owner.equals(attacker.getUUID())
                || attacker instanceof WingmanEntity w && owner.equals(w.owner))) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !secured) {
            // a fatal hit is not fatal: the suit breaks off and limps home
            setHealth(1.0F);
            protectedReturn = true;
            startReturn(Component.translatable("message.mio_icif.wingman.damaged"));
            return;
        }
        secure();
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) secure();
        super.remove(reason);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) { }

    // ------------------------------------------------------------------ movement
    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance()) move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public boolean isPushable() { return false; }

    /** No portals: the owner registry and the item return work in one dimension. */
    @Override
    public boolean canUsePortal(boolean allowPassengers) { return false; }

    @Override
    public boolean canBeLeashed() { return false; }

    @Override
    public boolean canTakeItem(ItemStack stack) { return false; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() { return null; }

    @Override
    protected boolean canAddPassenger(Entity passenger) { return getPassengers().isEmpty() && state() == ACTIVE; }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0, -passenger.getBbHeight() - 0.15, 0);   // the cargo hangs below
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (owner != null && (owner.equals(other.getUUID()) || other instanceof WingmanEntity w && owner.equals(w.owner))) return true;
        return super.isAlliedTo(other);
    }

    /** The owner crouches and uses an empty hand on the Wingman: it returns home. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (owner != null && owner.equals(player.getUUID()) && player.isSecondaryUseActive() && player.getItemInHand(hand).isEmpty()) {
            if (!level().isClientSide) startReturn(Component.translatable("message.mio_icif.wingman.recalled", 1));
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------ client effects
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && isAlive()) {
            // boot thrusters: two jets, stronger when the drone climbs or moves fast
            Vec3 v = getDeltaMovement();
            double yaw = Math.toRadians(yBodyRot);
            double rx = -Math.cos(yaw) * 0.13, rz = -Math.sin(yaw) * 0.13;
            for (int side = -1; side <= 1; side += 2) {
                level().addParticle(ArmoryRegistry.THRUST.get(), getX() + rx * side, getY() + 0.05, getZ() + rz * side,
                    -v.x * 0.3, -0.22 - Math.max(0, v.y) * 0.4, -v.z * 0.3);
            }
            if (tickCount % 3 == 0 && v.lengthSqr() > 0.3) level().addParticle(ArmoryRegistry.TRAIL.get(), getX(), getY() + 0.9, getZ(), 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ save / load
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Owner", owner);
        if (armory != null) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, armory).result().ifPresent(t -> tag.put("Armory", t));
        tag.putInt("Set", set);
        tag.putByte("Mode", entityData.get(DATA_MODE));
        tag.putByte("State", state());
        tag.putBoolean("Protected", protectedReturn);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        armory = tag.contains("Armory") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Armory")).result().orElse(null) : null;
        set = tag.getInt("Set");
        entityData.set(DATA_MODE, tag.getByte("Mode"));
        entityData.set(DATA_STATE, tag.getByte("State"));
        protectedReturn = tag.getBoolean("Protected");
        setNoGravity(true);
    }

    // ------------------------------------------------------------------ owner registry
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) WingmanService.track(this);
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (!level().isClientSide) WingmanService.untrack(this);
    }

    /** Live list of the pieces this Wingman wears, in Armory column order (for the console). */
    List<ItemStack> items() {
        List<ItemStack> out = new java.util.ArrayList<>();
        for (ArmoryPiece p : ArmoryPiece.COLUMNS) out.add(getItemBySlot(p.slot));
        return out;
    }
}
