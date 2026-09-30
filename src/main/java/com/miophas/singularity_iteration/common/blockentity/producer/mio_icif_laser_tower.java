package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.block.producer.mio_icif_block_laser_tower;
import com.miophas.singularity_iteration.common.event.mio_icif_DamageTypes;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
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
 * <p>Every {@link Spec#volleyInterval()} ticks the tower scans a box of
 * &plusmn;horizontal (X/Z) and &plusmn;vertical (Y) blocks, adjustable in its GUI, and
 * fires at up to {@link Spec#maxTargets()} hostile mobs in line of sight, nearest
 * first. Only {@link Enemy} mobs are targeted (never players or tamed animals).
 * Each target costs {@link Spec#energyPerTarget()}; the charge is committed only if
 * the hit is accepted, and the volley stops when the buffer runs dry.
 *
 * <p>Visuals cost the server one small packet per volley to players tracking the
 * chunk (tower position + hit entity ids); beams are drawn and animated client side.
 * A redstone signal switches the tower off (the redstone-inverter upgrade flips it).
 */
@SuppressWarnings("null")
public class mio_icif_laser_tower extends AbstractProcessingMachineBlockEntity {

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

    private static final SlotLayout LAYOUT = SlotLayout.builder().battery().build();
    public static final int BATTERY_SLOT = 0;

    private final Spec spec;
    private int horizontalRange;
    private int verticalRange;
    private int cooldown;
    private int lastVolley;       // targets hit by the latest volley (GUI read-out)
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
                case LaserTowerMenu.DATA_COST -> (int) spec.energyPerTarget();
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

    /** Clamped range change, used by the menu buttons. */
    public void adjustRange(int horizontalDelta, int verticalDelta) {
        horizontalRange = Math.clamp(horizontalRange + horizontalDelta, 1, spec.maxHorizontal());
        verticalRange = Math.clamp(verticalRange + verticalDelta, 1, spec.maxVertical());
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
        return slot == BATTERY_SLOT && isBattery(stack);
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
        return getEnergyStorage().getAmount() >= spec.energyPerTarget();
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
        if (tower.cooldown > 0) tower.cooldown--;
        if (!armed || tower.cooldown > 0) return;
        tower.volley(server, pos);
    }

    public Vec3 muzzle() {
        BlockPos pos = getBlockPos();
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5);
    }

    /** The scanned box: &plusmn;horizontal on X/Z and &plusmn;vertical on Y around the tower. */
    public AABB scanBox() {
        Vec3 c = Vec3.atCenterOf(getBlockPos());
        return new AABB(c.x - horizontalRange, c.y - verticalRange, c.z - horizontalRange,
            c.x + horizontalRange, c.y + verticalRange, c.z + horizontalRange);
    }

    private void volley(ServerLevel level, BlockPos pos) {
        if (firing || isRemoved() || level.getBlockEntity(pos) != this) return;
        List<LivingEntity> targets = selectTargets(level);
        if (targets.isEmpty()) {
            cooldown = Math.max(1, spec.volleyInterval() / 2);   // idle rescan
            return;
        }
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(mio_icif_DamageTypes.LASER_TOWER), muzzle());
        int[] hitIds = new int[targets.size()];
        int hits = 0;
        firing = true;
        try {
            for (LivingEntity target : targets) {
                var payment = getEnergyStorageInternal().scexReserveInternal(spec.energyPerTarget());
                if (payment == null) break;   // buffer exhausted: end the volley
                try {
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
                    }
                } finally {
                    if (payment.isPending()) payment.cancel();
                }
            }
        } finally {
            firing = false;
        }
        lastVolley = hits;
        cooldown = spec.volleyInterval();
        if (hits == 0) return;
        Vec3 m = muzzle();
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos),
            new LaserTowerBeamPacket(m.x, m.y, m.z, spec.beamStyle(), java.util.Arrays.copyOf(hitIds, hits)));
        level.playSound(null, m.x, m.y, m.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS,
            0.45F, spec.beamStyle() == LaserTowerBeamPacket.STYLE_SKY ? 1.6F : 1.9F);
    }

    /** Nearest-first hostile mobs in the box with line of sight, at most maxTargets. */
    private List<LivingEntity> selectTargets(ServerLevel level) {
        Vec3 origin = muzzle();
        List<Mob> candidates = level.getEntitiesOfClass(Mob.class, scanBox(),
            mob -> mob instanceof Enemy && mob.isAlive() && !mob.isInvulnerable() && !mob.isSpectator());
        candidates.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(origin)));
        List<LivingEntity> chosen = new ArrayList<>(Math.min(candidates.size(), spec.maxTargets()));
        for (Mob mob : candidates) {
            if (chosen.size() >= spec.maxTargets()) break;
            if (hasLineOfSight(level, origin, aimPoint(mob))) chosen.add(mob);
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TowerCooldown", cooldown);
        tag.putInt("TowerRangeH", horizontalRange);
        tag.putInt("TowerRangeV", verticalRange);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cooldown = Math.max(0, tag.getInt("TowerCooldown"));
        if (tag.contains("TowerRangeH")) horizontalRange = Math.clamp(tag.getInt("TowerRangeH"), 1, spec.maxHorizontal());
        if (tag.contains("TowerRangeV")) verticalRange = Math.clamp(tag.getInt("TowerRangeV"), 1, spec.maxVertical());
    }
}
