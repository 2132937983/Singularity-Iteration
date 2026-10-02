package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.block.producer.mio_icif_block_laser_tower;
import com.miophas.singularity_iteration.common.blockentity.producer.tower.LaserTowerStats;
import com.miophas.singularity_iteration.common.blockentity.producer.tower.TargetFilter;
import com.miophas.singularity_iteration.common.event.mio_icif_DamageTypes;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.menu.producer.LaserTowerMenu;
import com.miophas.singularity_iteration.common.network.LaserTowerBeamPacket;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Laser defence towers: multi-target hitscan turrets paid per target in EU.
 *
 * <p>Combat cycle: when targets appear the tower <i>locks on</i> (turret slews,
 * lock-on and charge sounds) and fires {@link #CHARGE_TICKS} later; while targets
 * remain it fires a volley every {@link Spec#volleyInterval()} ticks at up to
 * {@link Spec#maxTargets()} targets in line of sight, nearest first; when none are
 * left it returns to idle. Each hit costs {@link Spec#energyPerTarget()}, committed
 * only if the hit lands; the volley stops when the buffer runs dry.
 *
 * <p>Targets come from the {@link TargetFilter} (blacklist: hostiles except listed;
 * whitelist: only listed mobs/players) inside a box of &plusmn;horizontal (X/Z) and
 * &plusmn;vertical (Y) blocks. {@link LaserTowerStats} keeps totals for the GUI.
 *
 * <p>Network cost: one small packet per state change or volley to players tracking
 * the chunk; turret animation and beams are client side. A redstone signal switches
 * the tower off (the redstone-inverter upgrade flips it).
 */
@SuppressWarnings("null")
public class mio_icif_laser_tower extends AbstractProcessingMachineBlockEntity implements com.miophas.singularity_iteration.common.area.WorkAreaProvider {

    /** Tower tuning. Ranges in blocks, energy in EU, intervals in ticks. */
    public record Spec(String id, long capacity, long maxReceive, CableTier tier, int maxTargets,
                       long energyPerTarget, float damage, int volleyInterval,
                       int defaultHorizontal, int defaultVertical, int maxHorizontal, int maxVertical,
                       int beamStyle) { }

    /** Sky Patrol Laser Defense Station: Tier 3 (HV), 500 000 EU, up to 10 targets, 2 500 EU each. */
    public static final Spec SKY_PATROL = new Spec("sky_patrol_laser_tower", 500_000L, 512L, CableTier.HV,
        10, 2_500L, 20.0F, 15, 32, 32, 64, 64, LaserTowerBeamPacket.STYLE_SKY);
    /** Laser Defense Tower: half of the Sky Patrol station. Tier 2 (MV). */
    public static final Spec GROUND = new Spec("laser_defense_tower", 250_000L, 128L, CableTier.MV,
        5, 1_250L, 10.0F, 15, 16, 16, 32, 32, LaserTowerBeamPacket.STYLE_GROUND);

    /** Delay between lock-on and the first volley (turret slew + charge-up). */
    public static final int CHARGE_TICKS = 10;
    private static final int MAX_HIT_SOUNDS = 3;

    /** Battery first so saves from before the upgrade bay keep their battery in slot 0. */
    private static final SlotLayout LAYOUT = SlotLayout.builder().battery().upgrade(4).build();
    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_SLOT_START = 1;
    public static final int UPGRADE_SLOT_COUNT = 4;
    /** Overclockers beyond this count are ignored (keeps the EU cost and fire rate bounded). */
    public static final int MAX_EFFECTIVE_OVERCLOCKERS = 4;
    /** Fastest volley interval reachable with overclockers, in ticks. */
    public static final int MIN_VOLLEY_INTERVAL = 4;

    private final Spec spec;
    private final TargetFilter filter = new TargetFilter();
    private final LaserTowerStats stats = new LaserTowerStats();
    private int configVersion;
    private int horizontalRange;
    private int verticalRange;
    private int cooldown;
    private int lastVolley;       // targets hit by the latest volley (GUI read-out)
    private boolean engaged;      // locked on / firing (client turret tracks targets)
    private boolean firing;

    /** Server -> menu sync. Values are sent as shorts, so the energy is split in two halves. */
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            long energy = getEnergyStorage().getAmount();
            long capacity = getEnergyStorage().getCapacity();
            return switch (index) {
                case LaserTowerMenu.DATA_H -> horizontalRange;
                case LaserTowerMenu.DATA_V -> verticalRange;
                case LaserTowerMenu.DATA_MAX_H -> spec.maxHorizontal();
                case LaserTowerMenu.DATA_MAX_V -> spec.maxVertical();
                case LaserTowerMenu.DATA_TARGETS -> spec.maxTargets();
                case LaserTowerMenu.DATA_LAST_VOLLEY -> lastVolley;
                case LaserTowerMenu.DATA_ENERGY_LO -> (int) (energy & 0xFFFF);
                case LaserTowerMenu.DATA_ENERGY_HI -> (int) (energy >>> 16);
                case LaserTowerMenu.DATA_CAPACITY_LO -> (int) (capacity & 0xFFFF);
                case LaserTowerMenu.DATA_CAPACITY_HI -> (int) (capacity >>> 16);
                case LaserTowerMenu.DATA_COST -> (int) Math.min(0xFFFF, energyPerTarget());
                case LaserTowerMenu.DATA_INTERVAL -> volleyInterval();
                case LaserTowerMenu.DATA_TIER -> getEffectiveCableTier().getTier();
                case LaserTowerMenu.DATA_STATE -> !canWorkRedstone() ? 3 : !canWork() ? 2 : engaged ? 1 : 0;
                case LaserTowerMenu.DATA_STYLE -> spec.beamStyle();
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return LaserTowerMenu.DATA_COUNT; }
    };

    public mio_icif_laser_tower(BlockEntityType<?> type, BlockPos pos, BlockState state, Spec spec) {
        super(pos, state, type, spec.capacity(), spec.maxReceive(), 0L, 100, LAYOUT, spec.energyPerTarget(), spec.tier());
        this.spec = spec;
        this.horizontalRange = spec.defaultHorizontal();
        this.verticalRange = spec.defaultVertical();
    }

    public Spec spec() { return spec; }
    public int horizontalRange() { return horizontalRange; }
    public int verticalRange() { return verticalRange; }
    public TargetFilter filter() { return filter; }
    public LaserTowerStats stats() { return stats; }
    /** Changes whenever stats or the target list change; lets menus resend lazily. */
    public long syncVersion() { return ((long) configVersion << 32) | (stats.version() & 0xFFFFFFFFL); }

    /** Clamped range change, used by the menu buttons. */
    public void adjustRange(int horizontalDelta, int verticalDelta) {
        horizontalRange = Math.clamp(horizontalRange + horizontalDelta, 1, spec.maxHorizontal());
        verticalRange = Math.clamp(verticalRange + verticalDelta, 1, spec.maxVertical());
        setChanged();
    }

    public boolean addTargetEntry(String entry) {
        boolean ok = filter.add(entry);
        if (ok) configChanged();
        return ok;
    }

    public boolean removeTargetEntry(int index) {
        boolean ok = filter.remove(index);
        if (ok) configChanged();
        return ok;
    }

    public void toggleFilterMode() {
        filter.toggleMode();
        configChanged();
    }

    public void resetStats() {
        stats.reset();
        setChanged();
    }

    private void configChanged() {
        configVersion++;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif." + spec.id());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LaserTowerMenu(containerId, playerInventory, this, data);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) return isBattery(stack);
        if (slot >= UPGRADE_SLOT_START && slot < UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT) return isTowerUpgrade(stack);
        return false;
    }

    @Override
    public boolean acceptsUpgrade(int slot, ItemStack stack) {
        return isTowerUpgrade(stack);
    }

    /** Upgrades that mean something to a tower: overclocker, transformer (HV), energy storage, redstone inverter. */
    public static boolean isTowerUpgrade(ItemStack stack) {
        String type = com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getItemAPI().getUpgradeType(stack);
        return type != null && switch (type) {
            case "overclocker", "transformer", "energy_storage", "redstone_inverter" -> true;
            default -> false;
        };
    }

    /** Overclockers in the bay, capped at {@link #MAX_EFFECTIVE_OVERCLOCKERS}. */
    public int effectiveOverclockers() {
        recalculateUpgradeStats();
        return Math.min(MAX_EFFECTIVE_OVERCLOCKERS, getUpgradeStats().getOverclockerCount());
    }

    /** EU per target: IC2 overclocker rule, x1.6 per overclocker. */
    public long energyPerTarget() {
        double factor = Math.pow(com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats.OVERCLOCKER_ENERGY_MULTIPLIER,
            effectiveOverclockers());
        return Math.max(1L, (long) Math.ceil(spec.energyPerTarget() * factor - 1e-6));
    }

    /** Ticks between volleys: IC2 overclocker rule, x0.7 per overclocker. */
    public int volleyInterval() {
        double factor = Math.pow(com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats.OVERCLOCKER_SPEED_MULTIPLIER,
            effectiveOverclockers());
        return Math.max(MIN_VOLLEY_INTERVAL, (int) Math.round(spec.volleyInterval() * factor));
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{BATTERY_SLOT};
    }

    @Override
    protected int getBatterySlot() {
        return BATTERY_SLOT;
    }

    @Override
    protected boolean canWork() {
        return getEnergyStorage().getAmount() >= energyPerTarget();
    }

    @Override
    protected void doWork() {
        // Volleys are driven by tick(); the processing-progress system is unused.
    }

    @Override
    protected void tickProduction() {
        // No recipe progress: keep the base class from toggling work state.
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_laser_tower tower) {
        if (!(level instanceof ServerLevel server)) return;
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, tower);

        boolean armed = tower.canWorkRedstone() && tower.canWork();
        if (state.getValue(mio_icif_block_laser_tower.ACTIVE) != armed) {
            level.setBlock(pos, state.setValue(mio_icif_block_laser_tower.ACTIVE, armed), 3);
        }
        if (!armed) {
            tower.disengage(server, pos);
            return;
        }
        if (tower.cooldown > 0 && --tower.cooldown > 0) return;
        tower.engage(server, pos);
    }

    private void engage(ServerLevel level, BlockPos pos) {
        if (firing || isRemoved() || level.getBlockEntity(pos) != this) return;
        List<LivingEntity> targets = selectTargets(level);
        if (targets.isEmpty()) {
            disengage(level, pos);
            cooldown = Math.max(1, volleyInterval() / 2);   // idle rescan
            return;
        }
        if (!engaged) {
            // Lock-on: turret slews and charges, the first volley follows CHARGE_TICKS later.
            engaged = true;
            cooldown = CHARGE_TICKS;
            broadcast(level, pos, LaserTowerBeamPacket.KIND_LOCK, ids(targets, targets.size()));
            Vec3 m = muzzle();
            level.playSound(null, m.x, m.y, m.z, mio_icif_sounds.TOWER_LOCK.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            level.playSound(null, m.x, m.y, m.z, mio_icif_sounds.TOWER_CHARGE.get(), SoundSource.BLOCKS, 0.7F,
                spec.beamStyle() == LaserTowerBeamPacket.STYLE_SKY ? 0.9F : 1.15F);
            return;
        }
        volley(level, pos, targets);
    }

    private void disengage(ServerLevel level, BlockPos pos) {
        if (!engaged) return;
        engaged = false;
        broadcast(level, pos, LaserTowerBeamPacket.KIND_IDLE, new int[0]);
    }

    private void volley(ServerLevel level, BlockPos pos, List<LivingEntity> targets) {
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(mio_icif_DamageTypes.LASER_TOWER), muzzle());
        int[] hitIds = new int[targets.size()];
        int hits = 0;
        long cost = energyPerTarget();
        long now = level.getGameTime();
        firing = true;
        try {
            for (LivingEntity target : targets) {
                var payment = getEnergyStorageInternal().scexReserveInternal(cost);
                if (payment == null) break;   // buffer exhausted: end the volley
                try {
                    float before = target.getHealth() + target.getAbsorptionAmount();
                    boolean accepted;
                    try {
                        accepted = target.hurt(source, spec.damage());
                    } catch (RuntimeException | Error failure) {
                        payment.commit();    // the effect may have partly applied; never refund it
                        throw failure;
                    }
                    if (accepted) {
                        payment.commit();
                        hitIds[hits++] = target.getId();
                        float dealt = Math.max(0.0F, before - Math.max(0.0F, target.getHealth() + target.getAbsorptionAmount()));
                        stats.recordHit(target, dealt, target.isDeadOrDying() || !target.isAlive(), cost, now);
                    }
                } finally {
                    if (payment.isPending()) payment.cancel();
                }
            }
        } finally {
            firing = false;
        }
        lastVolley = hits;
        cooldown = volleyInterval();
        if (hits == 0) return;
        stats.recordVolley();
        setChanged();
        int[] fired = java.util.Arrays.copyOf(hitIds, hits);
        broadcast(level, pos, LaserTowerBeamPacket.KIND_FIRE, fired);
        Vec3 m = muzzle();
        float pitch = spec.beamStyle() == LaserTowerBeamPacket.STYLE_SKY ? 0.85F : 1.1F;
        level.playSound(null, m.x, m.y, m.z, mio_icif_sounds.TOWER_FIRE.get(), SoundSource.BLOCKS, 1.0F,
            pitch + level.random.nextFloat() * 0.1F);
        for (int i = 0; i < Math.min(hits, MAX_HIT_SOUNDS); i++) {
            var t = level.getEntity(fired[i]);
            if (t != null) level.playSound(null, t.getX(), t.getY(0.5), t.getZ(), mio_icif_sounds.TOWER_HIT.get(),
                SoundSource.BLOCKS, 0.6F, 0.9F + level.random.nextFloat() * 0.3F);
        }
    }

    private void broadcast(ServerLevel level, BlockPos pos, int kind, int[] targetIds) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos),
            new LaserTowerBeamPacket(pos, spec.beamStyle(), kind, targetIds));
    }

    private static int[] ids(List<LivingEntity> list, int n) {
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = list.get(i).getId();
        return out;
    }

    /** Turret pivot in world space (the beam origin used for line-of-sight checks). */
    public Vec3 muzzle() {
        BlockPos pos = getBlockPos();
        double pivotY = spec.beamStyle() == LaserTowerBeamPacket.STYLE_SKY ? 13.5 / 16.0 : 12.25 / 16.0;
        return new Vec3(pos.getX() + 0.5, pos.getY() + pivotY, pos.getZ() + 0.5);
    }

    /** The scanned box: &plusmn;horizontal on X/Z and &plusmn;vertical on Y around the tower. */
    public AABB scanBox() {
        Vec3 c = Vec3.atCenterOf(getBlockPos());
        return new AABB(c.x - horizontalRange, c.y - verticalRange, c.z - horizontalRange,
            c.x + horizontalRange, c.y + verticalRange, c.z + horizontalRange);
    }

    /** Nearest-first permitted targets in the box with line of sight, at most maxTargets. */
    private List<LivingEntity> selectTargets(ServerLevel level) {
        Vec3 origin = muzzle();
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, scanBox(),
            e -> e.isAlive() && !e.isInvulnerable() && !e.isSpectator() && filter.allows(e));
        if (candidates.isEmpty()) return candidates;
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(origin)));
        List<LivingEntity> chosen = new ArrayList<>(Math.min(candidates.size(), spec.maxTargets()));
        for (LivingEntity e : candidates) {
            if (chosen.size() >= spec.maxTargets()) break;
            if (hasLineOfSight(level, origin, aimPoint(e))) chosen.add(e);
        }
        return chosen;
    }

    public static Vec3 aimPoint(LivingEntity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.6, 0);
    }

    private static boolean hasLineOfSight(ServerLevel level, Vec3 origin, Vec3 aim) {
        Vec3 direction = aim.subtract(origin);
        double length = direction.length();
        if (length < 1.0E-3) return true;
        // Start just outside the tower's own head so it never blocks itself.
        Vec3 start = origin.add(direction.scale(0.8 / length));
        HitResult hit = level.clip(new ClipContext(start, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
            CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS;
    }

    // ------------------------------------------------------------------ client-side core state
    // Written by the beam packet handler and the block entity renderer on the client only.
    /** Entity id of the latest locked target, or -1. */
    public int clientAimTarget = -1;
    /** Game time of the last lock/fire event (the core runs "alert" for a while after it). */
    public long clientAimTime = Long.MIN_VALUE;
    /** Game time of the last discharge (drives the flash and the omnidirectional shockwave). */
    public long clientFireTime = Long.MIN_VALUE;
    /** Discharge emitter points in world space, refreshed every rendered frame (x,y,z each). */
    public final double[] clientMuzzles = new double[12];
    public int clientMuzzleCount;
    public long clientMuzzleFrame = -1;

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TowerCooldown", cooldown);
        tag.putInt("TowerRangeH", horizontalRange);
        tag.putInt("TowerRangeV", verticalRange);
        tag.put("TowerFilter", filter.save());
        tag.put("TowerStats", stats.save());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cooldown = Math.max(0, tag.getInt("TowerCooldown"));
        if (tag.contains("TowerRangeH")) horizontalRange = Math.clamp(tag.getInt("TowerRangeH"), 1, spec.maxHorizontal());
        if (tag.contains("TowerRangeV")) verticalRange = Math.clamp(tag.getInt("TowerRangeV"), 1, spec.maxVertical());
        if (tag.contains("TowerFilter")) { filter.load(tag.getCompound("TowerFilter")); configVersion++; }
        if (tag.contains("TowerStats")) stats.load(tag.getCompound("TowerStats"));
    }

    @Override
    public java.util.List<com.miophas.singularity_iteration.common.area.WorkArea> workAreas() {
        AABB box = scanBox();
        return java.util.List.of(new com.miophas.singularity_iteration.common.area.WorkArea(new AABB(Math.floor(box.minX), Math.floor(box.minY), Math.floor(box.minZ),
            Math.ceil(box.maxX), Math.ceil(box.maxY), Math.ceil(box.maxZ)), com.miophas.singularity_iteration.common.area.WorkArea.DEFENCE, com.miophas.singularity_iteration.common.area.WorkArea.PRIMARY));
    }
}
