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
    // 0.1.8.4 combat state: circling direction (+1/-1), melee peel-off, blade dash, Slash Art timer, blocked line
    private int strafeSign = 1, retreatTicks, dashTicks, artsCooldown, blockedTicks;
    /** Ticks between two Slash Arts of a blade-wielding Wingman. */
    private static final int ARTS_INTERVAL = 200;
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
        // 0.1.9.0: a retired suit piece (legacy shell) turns into its suit piece with the frame core
        if (stateTicks % 20 == 1) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST}) {
                ItemStack worn = getItemBySlot(slot);
                if (!com.miophas.singularity_iteration.common.suit.core.LegacyArmor.isLegacy(worn)) continue;
                var result = com.miophas.singularity_iteration.common.suit.core.LegacyArmor.convert(worn);
                if (result == null) continue;
                setItemSlot(slot, result.stack().getItem() instanceof net.minecraft.world.item.ArmorItem ? result.stack() : ItemStack.EMPTY);
                if (!(result.stack().getItem() instanceof net.minecraft.world.item.ArmorItem)) spawnAtLocation(result.stack());
                for (ItemStack extra : result.extras()) spawnAtLocation(extra);
            }
        }
        if (attackCooldown > 0) attackCooldown--;
        if (deflectCooldown > 0) deflectCooldown--;
        if (artsCooldown > 0) artsCooldown--;
        if (state() == ACTIVE) SlashBladeCombat.tick(this);   // a blade's combo actions run on the wielder's tick
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
        LivingEntity focus = ownerFocus(player);
        if (focus != null && focus != target) target = focus;          // the owner's new target first, at once
        else if (tickCount % 10 == 0 || !valid(target, player, LEASH)) target = pickOffensive(player);
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
            // 0.1.8.4: body-block a hostile that closes in on the owner, even before it strikes
            if (target == null) target = hunterOf(player, 6);
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

    // ---- combat (0.1.8.4 rework)
    //
    // Weapon kinds: a SlashBlade: Resharped blade runs its real combos and Slash Arts
    // (SlashBladeCombat); a ranged weapon (bow, crossbow, trident, any projectile weapon, and the
    // energy guns through IWeaponItem#fireFrom) keeps a stand-off band around the target, circles
    // it and fires with a lead on the target's motion; anything else is a melee weapon used with
    // hit-and-run passes. All kinds turn smoothly, keep apart from the other Wingmen, and never
    // shoot through the owner.

    enum WeaponKind { MELEE, BLADE, RANGED }

    /** Weapon kind of the main hand. */
    WeaponKind weaponKind() {
        ItemStack w = getMainHandItem();
        if (SlashBladeCombat.isBlade(w)) return WeaponKind.BLADE;
        var item = w.getItem();
        if (item instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem gun && gun.isRanged()) return WeaponKind.RANGED;
        if (item instanceof net.minecraft.world.item.ProjectileWeaponItem || item instanceof net.minecraft.world.item.TridentItem) return WeaponKind.RANGED;
        return WeaponKind.MELEE;
    }

    private void engage(LivingEntity enemy, ServerPlayer player) {
        switch (weaponKind()) {
            case RANGED -> engageRanged(enemy, player);
            case BLADE -> engageBlade(enemy, player);
            default -> engageMelee(enemy, player);
        }
    }

    /** Melee: close in on the predicted position, strike, then peel off sideways and come back. */
    private void engageMelee(LivingEntity enemy, ServerPlayer player) {
        Vec3 aim = enemy.position().add(0, enemy.getBbHeight() * 0.5, 0);
        faceSmooth(enemy.getEyePosition(), 35);
        if (retreatTicks > 0) {                                   // peel off: sideways and up, then return
            retreatTicks--;
            Vec3 side = sideOf(enemy).scale(3.2).add(0, 1.6, 0);
            steer(aim.add(side), COMBAT, player);
            return;
        }
        steer(aim.add(enemy.getDeltaMovement().scale(4)), COMBAT, player);
        if (attackCooldown == 0 && isWithinMeleeAttackRange(enemy)) {
            swing(InteractionHand.MAIN_HAND);
            doHurtTarget(enemy);
            double speed = Math.max(0.5, getAttributeValue(Attributes.ATTACK_SPEED));
            attackCooldown = Mth.ceil(20.0 / speed);
            if (getRandom().nextInt(3) == 0) { retreatTicks = 8; strafeSign = -strafeSign; }
        }
    }

    /**
     * SlashBlade: dash in with the rapid slash from 3.5 to 9 blocks, chain the combo in reach
     * (one input every 5 ticks, the timing window of the combo), release a Slash Art every
     * {@link #ARTS_INTERVAL} ticks of contact. The blade's own tick actions do the cutting.
     */
    private void engageBlade(LivingEntity enemy, ServerPlayer player) {
        Vec3 aim = enemy.position().add(0, enemy.getBbHeight() * 0.5, 0);
        faceSmooth(enemy.getEyePosition(), 45);
        double d = distanceTo(enemy);
        if (dashTicks > 0) {                                      // rapid slash: a straight burst through the target
            dashTicks--;
            Vec3 to = aim.subtract(position());
            if (to.lengthSqr() > 1.0E-4) setDeltaMovement(to.normalize().scale(1.6));
        } else {
            steer(aim.add(enemy.getDeltaMovement().scale(3)).add(sideOf(enemy).scale(0.8)), COMBAT, player);
        }
        if (attackCooldown > 0) return;
        if (artsCooldown == 0 && d < 7 && hasLineOfSight(enemy)) {
            if (SlashBladeCombat.slashArts(this, enemy) != null) {
                artsCooldown = ARTS_INTERVAL;
                attackCooldown = 20;
                return;
            }
        }
        if (d > 3.5 && d < 9 && hasLineOfSight(enemy)) {
            if (SlashBladeCombat.attack(this, enemy, SlashBladeCombat.Command.FORWARD, SlashBladeCombat.Command.SPRINT) != null) {
                dashTicks = 5;
                attackCooldown = 12;
                return;
            }
        }
        if (d <= 3.5) {
            if (SlashBladeCombat.attack(this, enemy) != null) {
                swing(InteractionHand.MAIN_HAND);
                attackCooldown = 5;
            } else if (isWithinMeleeAttackRange(enemy)) {        // bridge off: plain sword
                swing(InteractionHand.MAIN_HAND);
                doHurtTarget(enemy);
                attackCooldown = Mth.ceil(20.0 / Math.max(0.5, getAttributeValue(Attributes.ATTACK_SPEED)));
            }
        }
    }

    /**
     * Ranged: hold the stand-off band (from the weapon, 8 to 16 blocks for a bow), 3 to 5 blocks
     * above the target, circling it; fire with a lead when the line is clear and the owner is not
     * in it. A blocked line makes the Wingman climb and swap the circling direction.
     */
    private void engageRanged(LivingEntity enemy, ServerPlayer player) {
        ItemStack weapon = getMainHandItem();
        double[] band = rangeBand(weapon);
        Vec3 c = enemy.position();
        Vec3 flat = new Vec3(getX() - c.x, 0, getZ() - c.z);
        double r = flat.length();
        if (r < 1.0E-3) { flat = new Vec3(1, 0, 0); r = 1; }
        double want = Mth.clamp(r, band[0], band[1]);
        if (r < band[0]) want = band[0] + 1.5;                    // too close: open the distance
        else if (r > band[1]) want = band[1] - 1.5;               // too far: close in
        Vec3 radial = flat.scale(1 / r);
        Vec3 tangent = new Vec3(-radial.z, 0, radial.x);
        Vec3 stand = c.add(radial.scale(want)).add(tangent.scale(strafeSign * 2.5)).add(0, 3.5 + (blockedTicks > 0 ? 3 : 0), 0);
        steer(stand, CRUISE * 1.1, player);
        Vec3 muzzle = getEyePosition();
        Vec3 aim = lead(enemy, muzzle, projectileSpeed(weapon));
        faceSmooth(aim, 40);
        boolean clear = hasLineOfSight(enemy) && !ownerInLine(player, muzzle, aim);
        if (!clear) {
            if (++blockedTicks > 20) { strafeSign = -strafeSign; blockedTicks = 0; }
            return;
        }
        blockedTicks = 0;
        if (attackCooldown > 0 || facingError(aim) > 12) return;
        if (splash(weapon) && enemy.distanceToSqr(player) < 5.5 * 5.5) return;   // no blast next to the owner
        int cooldown = fire(weapon, aim.subtract(muzzle), enemy);
        if (cooldown >= 0) attackCooldown = Math.max(2, cooldown);
        else attackCooldown = 20;                                  // could not fire (no charge): try again later
    }

    /** Fires the held ranged weapon along {@code dir}. Returns the cooldown, or -1. */
    private int fire(ItemStack weapon, Vec3 dir, LivingEntity enemy) {
        var item = weapon.getItem();
        if (item instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem gun) {
            int cd = gun.fireFrom(weapon, this, dir, true);
            if (cd >= 0) swing(InteractionHand.MAIN_HAND);
            return cd;
        }
        if (item instanceof net.minecraft.world.item.TridentItem) {
            var trident = new net.minecraft.world.entity.projectile.ThrownTrident(level(), this, weapon.copyWithCount(1));
            trident.shoot(dir.x, dir.y, dir.z, 2.5F, 1.0F);
            trident.pickup = AbstractArrow.Pickup.DISALLOWED;
            level().addFreshEntity(trident);
            playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.0F);
            return 30;
        }
        // bows, crossbows and other projectile weapons: an arrow from the weapon, gravity-compensated
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), 1.0F, weapon);
        double flat = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float speed = item instanceof CrossbowItem ? 3.15F : 2.4F;
        arrow.shoot(dir.x, dir.y + flat * (item instanceof CrossbowItem ? 0.05 : 0.11), dir.z, speed, 1.0F);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        level().addFreshEntity(arrow);
        playSound(item instanceof CrossbowItem ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.ARROW_SHOOT, 1.0F,
            1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        return item instanceof CrossbowItem ? 22 : 16;
    }

    private static double[] rangeBand(ItemStack weapon) {
        var item = weapon.getItem();
        if (item instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem gun) return gun.engagementRange();
        if (item instanceof CrossbowItem) return new double[]{9, 16};
        if (item instanceof net.minecraft.world.item.TridentItem) return new double[]{6, 12};
        return new double[]{8, 16};
    }

    private static double projectileSpeed(ItemStack weapon) {
        var item = weapon.getItem();
        if (item instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem gun) return gun.projectileSpeed();
        if (item instanceof CrossbowItem) return 3.15;
        if (item instanceof net.minecraft.world.item.TridentItem) return 2.5;
        return 2.4;
    }

    /** True for a weapon whose shot explodes (rockets, the plasma gun). */
    private static boolean splash(ItemStack weapon) {
        return weapon.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.mio_icif_rocket_launcher
            || weapon.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.mio_icif_electric_plasma_gun;
    }

    /** Aim point with a lead on the target's motion (speed 0: the current position). */
    private static Vec3 lead(LivingEntity enemy, Vec3 from, double speed) {
        Vec3 at = enemy.position().add(0, enemy.getBbHeight() * 0.55, 0);
        if (speed <= 0) return at;
        Vec3 v = enemy.getDeltaMovement();
        double t = 0;
        for (int i = 0; i < 3; i++) t = at.add(v.scale(t)).distanceTo(from) / speed;   // fixed-point on the flight time
        return at.add(v.x * t, Math.max(-0.5, v.y) * t, v.z * t);
    }

    /** True if the owner stands near the shot line from {@code from} to {@code to}. */
    private static boolean ownerInLine(ServerPlayer owner, Vec3 from, Vec3 to) {
        Vec3 seg = to.subtract(from);
        double len2 = seg.lengthSqr();
        if (len2 < 1.0E-4) return false;
        Vec3 c = owner.getBoundingBox().getCenter();
        double t = Mth.clamp(c.subtract(from).dot(seg) / len2, 0, 1);
        return from.add(seg.scale(t)).distanceToSqr(c) < 1.3 * 1.3;
    }

    /** Horizontal unit vector to the side of the target the Wingman circles on. */
    private Vec3 sideOf(LivingEntity enemy) {
        Vec3 to = enemy.position().subtract(position());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        if (flat.lengthSqr() < 1.0E-4) return new Vec3(strafeSign, 0, 0);
        flat = flat.normalize();
        return new Vec3(-flat.z * strafeSign, 0, flat.x * strafeSign);
    }

    /** Degrees between the current look and the direction to {@code at}. */
    private double facingError(Vec3 at) {
        Vec3 d = at.subtract(getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        return Math.abs(Mth.wrapDegrees(yaw - getYRot())) + Math.abs(pitch - getXRot());
    }

    /** Turns toward {@code at} by at most {@code maxStep} degrees per tick (no snapping). */
    private void faceSmooth(Vec3 at, float maxStep) {
        Vec3 d = at.subtract(getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        float dy = Mth.clamp(Mth.wrapDegrees(yaw - getYRot()), -maxStep, maxStep);
        setYaw(getYRot() + dy);
        setXRot(getXRot() + Mth.clamp(pitch - getXRot(), -maxStep, maxStep));
    }

    /** The mob the owner hit in the last 2 s, if it is a valid target (0.1.8.4: owner focus). */
    @Nullable
    private LivingEntity ownerFocus(ServerPlayer player) {
        LivingEntity marked = player.getLastHurtMob();
        return marked != null && player.tickCount - player.getLastHurtMobTimestamp() < 40 && valid(marked, player, LEASH) ? marked : null;
    }

    /** Nearest hostile mob within {@code range} of the owner that hunts the owner. */
    @Nullable
    private LivingEntity hunterOf(ServerPlayer player, double range) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Mob mob : level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range), m -> m instanceof Enemy && m.getTarget() == player)) {
            if (!valid(mob, player, range + 2)) continue;
            double d = mob.distanceToSqr(player);
            if (d < bestD) { bestD = d; best = mob; }
        }
        return best;
    }

    /** Push away from the other Wingmen closer than 2.2 blocks, so the squad does not stack up. */
    private Vec3 separation() {
        Vec3 push = Vec3.ZERO;
        for (WingmanEntity other : level().getEntitiesOfClass(WingmanEntity.class, getBoundingBox().inflate(2.2), w -> w != this)) {
            Vec3 away = position().subtract(other.position());
            double d = away.length();
            if (d < 1.0E-3) away = new Vec3(getId() % 2 == 0 ? 1 : -1, 0, 0);
            push = push.add(away.normalize().scale((2.2 - Math.min(2.2, d)) * 0.08));
        }
        return push;
    }

    /** Escort place of this Wingman: behind and to the side of the owner, at shoulder height. */
    private Vec3 formation(ServerPlayer player) {
        double[][] offsets = {{-1.6, 1.2, -1.8}, {1.6, 1.2, -1.8}, {-2.9, 1.7, -0.6}, {2.9, 1.7, -0.6}};
        double[] o = offsets[Math.floorMod(slot, offsets.length)];
        // 0.1.8.4: tighter guard when the owner is below 40 % health
        double k = player.getHealth() < player.getMaxHealth() * 0.4F ? 0.6 : 1.0;
        o = new double[]{o[0] * k, o[1], o[2] * k};
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
        want = want.add(separation());
        Vec3 v = getDeltaMovement();
        Vec3 dv = want.subtract(v);
        if (dv.lengthSqr() > ACCEL * ACCEL) dv = dv.normalize().scale(ACCEL);
        v = v.add(dv);
        if (horizontalCollision) v = v.add(0, 0.3, 0);
        setDeltaMovement(v);
        if (target == null) {
            if (v.horizontalDistanceSqr() > 0.02) faceSmooth(getEyePosition().add(v.x, 0, v.z), 20);
            else if (player != null) setYaw(getYRot() + Mth.clamp(Mth.wrapDegrees(player.getYRot() - getYRot()), -15, 15));
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
