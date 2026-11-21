package com.miophas.singularity_iteration.common.blockentity.fusion;

import com.miophas.singularity_iteration.common.block.fusion.mio_icif_block_fusion_controller;
import com.miophas.singularity_iteration.common.event.mio_icif_DamageTypes;
import com.miophas.singularity_iteration.common.fusion.FusionBlanket;
import com.miophas.singularity_iteration.common.fusion.FusionPlasma;
import com.miophas.singularity_iteration.common.fusion.FusionStructure;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.menu.fusion.FusionControllerMenu;
import com.miophas.singularity_iteration.common.reactor.ReactorInventory;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.util.RadiationProtectionUtil;
import com.miophas.singularity_iteration.common.util.RadiationShielding;
import com.miophas.singularity_iteration.core.api.advancement.MioAchievements;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergyEmitter;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.TickMarks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.92: the fusion reactor's controller. It checks the structure ({@link FusionStructure}), runs the plasma
 * ({@link FusionPlasma}) on the EU it receives (LuV, from cables on any face), runs the blanket ({@link FusionBlanket}) every 20
 * ticks, and keeps the output for the power output ports, which take it as the network asks.
 *
 * <ul>
 *   <li>It runs with a redstone signal, like the fission reactor. It needs a cryogenic port (the magnets), a heating port to
 *       reach ignition, an exhaust port (else helium ash builds up and the plasma disrupts) and a power output port.</li>
 *   <li>Fuel: deuterium fuel rods (0.1.11.94: canned from unstable hot water) and tritium fuel rods (bred from lithium cells,
 *       in the blanket or in a fission reactor) in its fuel slots or through a fuel injection port; a fresh rod gives 10,000
 *       units (a used one the share it has left) and its empty fuel rod back. The plasma uses one unit of each per feed step
 *       and tick at full temperature.</li>
 *   <li>0.1.11.97: the fuel injection ports and the disruption mitigation ports have slots of their own
 *       ({@link FusionItemPortBlockEntity}); every tick the controller takes one rod or cell from them into each of its fuel and
 *       mitigation slots that is empty, and hands its empty fuel rods to the fuel ports.</li>
 *   <li>Once the plasma makes power, the controller keeps {@link #SELF_FEED_TICKS} ticks of magnet upkeep in its input store
 *       from its own output.</li>
 *   <li>A disruption throws the plasma's heat onto the first wall: every component in the blanket's first column is
 *       destroyed and the blanket heats by half its maximum, unless the disruption mitigation slot holds a reactor coolant cell
 *       (0.1.11.94, was ice): one is injected (used up), the components stay whole and the blanket takes
 *       {@link #mitigatedHeatShare} of its maximum, the less the more cold the cell has left (10k 30 %, 30k 20 %, 60k 5 %).
 *       The structure is never damaged by a disruption.</li>
 *   <li>0.1.11.94: a blanket at its maximum heat (from its own cycle or a disruption) melts down: the reactor explodes as a
 *       nuclear blast of {@link #explosionPower()} (config fusionExplosionMultiplier x reactorExplosionPowerLimit, by default
 *       50 x 45 = 2,250: 50 times the fission reactor's strongest; the blast's radius is twice that, at most 2,000 blocks),
 *       centred on the torus. With the multiplier at 0 the overheated blanket only disrupts the plasma, as before.</li>
 *   <li>While the plasma burns, living things within {@link #RADIATION_RANGE} blocks of the torus take radiation (0-3 a second,
 *       less through shielding; full hazmat and quantum armour protect).</li>
 * </ul>
 */
public class FusionControllerBlockEntity extends AbstractEnergyBlockEntity implements MenuProvider {
    public static final long INPUT_CAPACITY = 20_000_000L, INPUT_RATE = 64L * 32_768L;
    public static final int SLOT_DEUTERIUM = 54, SLOT_TRITIUM = 55, SLOT_EMPTY_CELL = 56, SLOT_EMPTY_ROD = 57, SLOT_MITIGATION = 58,
        SLOTS = 59;
    public static final long FUEL_PER_CELL = 10_000L, TANK = 50_000L;
    public static final int CHECK_FORMED = 100, CHECK_UNFORMED = 40, CYCLE_TICKS = 20, SELF_FEED_TICKS = 40, RADIATION_RANGE = 16;

    /** Problems shown by the screen (the first that applies). */
    public enum Status { OK, UNFORMED, NO_SIGNAL, NO_CRYO, NO_HEATER, NO_EXHAUST, NO_POWER_PORT, NO_FUEL, BLANKET_INVALID }

    /** A port that serves a controller: it is told when the controller takes it in or lets it go. */
    public interface FusionPart {
        void link(BlockPos controller);
        void unlink(BlockPos controller);
    }

    private static final Map<Level, Set<BlockPos>> LOADED = Collections.synchronizedMap(new WeakHashMap<>());

    private final FusionPlasma plasma = new FusionPlasma();
    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override public int getSlotLimit(int slot) { return slot < FusionBlanket.SLOTS ? 1 : 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return valid(slot, stack); }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private int feed = FusionPlasma.MIN_FEED;
    private long deuterium, tritium, hull, maxHull = 10_000, output, buffer;
    private double fissionEu, lithiumShare;
    /** 0.1.11.135: the blanket's heat (EU/t), energy multiplication, fissions per neutron, tritium breeding ratio. */
    private long thermal;
    private double multiplication, fissionsPerNeutron, tbr;
    private long cycleFuel;
    private boolean formed, blanketOverheated, blanketInvalid, recheck = true;
    private int checkIn, cycleIn, radiationIn = 20;
    private final List<BlockPos> ports = new ArrayList<>();
    /** 0.1.11.97: the fuel injection and disruption mitigation ports among {@link #ports} (they have slots). */
    private final List<BlockPos> itemPorts = new ArrayList<>();
    private int powerPorts, heaters, cyclotron, fuelPorts, exhausts, cryos, mitigations;
    private @Nullable BlockPos wrongAt;
    private @Nullable FusionStructure.Part expected;
    private long lastFusion, lastHeating, lastUsed;

    public FusionControllerBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.FUSION_CONTROLLER_ENTITY_TYPE.get(), INPUT_CAPACITY, INPUT_RATE, 0, CableTier.LuV);
    }

    /** 0.1.11.94: the reactor coolant cells (10k, 30k, 60k) the disruption mitigation takes (it took ice before). */
    public static final List<String> COOLANT_CELLS = List.of("reactor/item_reactor_collant_simple", "reactor/item_reactor_collant_triple",
        "reactor/item_reactor_coolant_six");
    /** The cold of a fresh 60k coolant cell: the best mitigation. */
    public static final long BEST_COLD = 60_000L;
    public static final double UNMITIGATED_HEAT_SHARE = 0.5;

    public static boolean isCoolant(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("mio_icif") && COOLANT_CELLS.contains(id.getPath());
    }

    /** The cold a coolant cell has left: its capacity less the heat it holds (0 for anything else). */
    public static long coldOf(ItemStack stack) {
        if (!isCoolant(stack)) return 0;
        try {
            var part = ReactorInventory.read(stack.copyWithCount(1));
            return part == null ? 0 : Math.max(0, part.profile().capacity() - part.stored());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return 0;
        }
    }

    /** The share of the blanket's maximum heat a disruption mitigated by a cell with {@code cold} left gives: 30 % to 5 %. */
    public static double mitigatedHeatShare(long cold) {
        double s = Math.max(0, Math.min(1, cold / (double) BEST_COLD));
        return 0.30 - 0.25 * s;
    }

    /** 0.1.11.94: the meltdown's blast power: the multiplier times the fission reactor's power limit (0: no blast). */
    public static float explosionPower(double multiplier, int fissionLimit) {
        if (!(multiplier > 0) || fissionLimit <= 0) return 0;
        return (float) (multiplier * fissionLimit);
    }

    public static float explosionPower() {
        return explosionPower(com.miophas.singularity_iteration.common.Singularity_Iteration_Config.FUSION_EXPLOSION_MULTIPLIER.get(),
            com.miophas.singularity_iteration.common.Singularity_Iteration_Config.REACTOR_EXPLOSION_POWER_LIMIT.get());
    }

    static boolean valid(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (slot < FusionBlanket.SLOTS) return ReactorInventory.supported(stack);
        return switch (slot) {
            case SLOT_DEUTERIUM -> stack.is(mio_icif_normal.DEUTERIUM_CELL.get());
            case SLOT_TRITIUM -> stack.is(mio_icif_normal.TRITIUM_CELL.get());
            case SLOT_MITIGATION -> isCoolant(stack);
            default -> false;   // the empty cells only come out
        };
    }

    // ------------------------------------------------------------------ structure

    /** A fusion part next to {@code pos} was broken or replaced: the controllers in reach check their structure on their next tick. */
    public static void partRemoved(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        Set<BlockPos> at = LOADED.get(level);
        if (at == null) return;
        List<BlockPos> copy;
        synchronized (LOADED) { copy = new ArrayList<>(at); }
        for (BlockPos c : copy)
            if (c.distSqr(pos) <= 30 * 30 && level.isLoaded(c) && level.getBlockEntity(c) instanceof FusionControllerBlockEntity controller) controller.recheck = true;
    }

    private Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(mio_icif_block_fusion_controller.FACING) ? s.getValue(mio_icif_block_fusion_controller.FACING) : Direction.EAST;
    }

    public void validate() {
        if (level == null || level.isClientSide) return;
        FusionStructure.Check check = FusionStructure.tokamak().check(level, worldPosition, facing());
        List<BlockPos> found = check.formed() ? check.ports() : List.of();
        boolean changed = check.formed() != formed || !found.equals(ports);
        wrongAt = check.wrongAt();
        expected = check.expected();
        if (changed) {
            for (BlockPos old : ports) {
                if (!found.contains(old) && level.isLoaded(old)) {
                    if (level.getBlockEntity(old) instanceof FusionPart part) part.unlink(worldPosition);
                    FusionPortRegistry.unlink(level, old);
                }
            }
            for (BlockPos now : found) {
                if (level.getBlockEntity(now) instanceof FusionPart part) part.link(worldPosition);
                FusionPortRegistry.link(level, now, worldPosition);
            }
            boolean wasFormed = formed;
            ports.clear();
            ports.addAll(found);
            formed = check.formed();
            if (formed && !wasFormed) MioAchievements.at(level, worldPosition, "fusion_formed", "formed");
            setChanged();
        }
        countPorts();
        BlockState own = getBlockState();
        if (own.hasProperty(mio_icif_block_fusion_controller.FORMED) && own.getValue(mio_icif_block_fusion_controller.FORMED) != formed)
            level.setBlock(worldPosition, own.setValue(mio_icif_block_fusion_controller.FORMED, formed), Block.UPDATE_CLIENTS);
    }

    private void countPorts() {
        powerPorts = heaters = cyclotron = fuelPorts = exhausts = cryos = mitigations = 0;
        itemPorts.clear();
        if (level == null || !formed) return;
        for (BlockPos p : ports) {
            BlockState s = level.getBlockState(p);
            if (s.is(mio_icif_blocks.FUSION_POWER_PORT.get())) powerPorts++;
            else if (s.is(mio_icif_blocks.FUSION_NBI_PORT.get())) heaters++;
            else if (s.is(mio_icif_blocks.FUSION_ECRH_PORT.get())) { heaters++; cyclotron++; }
            else if (s.is(mio_icif_blocks.FUSION_FUEL_PORT.get())) { fuelPorts++; itemPorts.add(p); }
            else if (s.is(mio_icif_blocks.FUSION_EXHAUST_PORT.get())) exhausts++;
            else if (s.is(mio_icif_blocks.FUSION_CRYO_PORT.get())) cryos++;
            else if (s.is(mio_icif_blocks.FUSION_MITIGATION_PORT.get())) { mitigations++; itemPorts.add(p); }
        }
    }

    /** 0.1.11.97: rods and cells from the fuel and mitigation ports into the empty slots; the empty fuel rods out to them. */
    private void servePorts() {
        if (level == null || !formed) return;
        for (BlockPos p : itemPorts)
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof FusionItemPortBlockEntity port) port.serve(items);
    }

    public boolean serves(BlockPos port) { return formed && ports.contains(port); }

    // ------------------------------------------------------------------ tick

    public static void tick(Level level, BlockPos pos, BlockState state, FusionControllerBlockEntity c) {
        if (level.isClientSide) return;
        if (c.recheck || --c.checkIn <= 0) {
            c.recheck = false;
            c.checkIn = c.formed ? CHECK_FORMED : CHECK_UNFORMED;
            c.validate();
        }
        c.servePorts();
        c.takeFuel();
        boolean signal = level.hasNeighborSignal(pos);
        boolean run = signal && c.cryos > 0 && !c.blanketInvalid;
        boolean hadPlasma = c.plasma.hasPlasma();
        double kevBefore = c.plasma.kev();
        FusionPlasma.Result r = c.plasma.tick(new FusionPlasma.Inputs(c.formed, run, c.feed, c.heaters, c.cyclotron, c.apiGetStoredEnergy(),
            c.deuterium, c.tritium, c.exhausts > 0, c.blanketOverheated));
        if (r.energyUsed() > 0) c.apiUseEnergy(r.energyUsed(), false);
        c.deuterium -= r.fuelUsed();
        c.cycleFuel += r.fuelUsed();
        c.tritium -= r.fuelUsed();
        c.lastFusion = r.fusionPower();
        c.lastHeating = r.heating();
        c.lastUsed = r.energyUsed();
        switch (r.event()) {
            case IGNITION -> MioAchievements.at(level, pos, "fusion_ignition", "ignited");
            case DISRUPTION -> c.disrupt(hadPlasma, kevBefore);
            default -> { }
        }
        if (!c.plasma.hasPlasma()) c.output = 0;

        if (--c.cycleIn <= 0) {
            c.cycleIn = CYCLE_TICKS;
            c.blanketCycle(r.neutronPower());
            c.cycleFuel = 0;
        }
        // 0.1.11.94: a blanket at its maximum heat melts down (with the blast off, the plasma only disrupts, as before)
        if (c.hull >= c.maxHull && c.formed && c.meltdown((ServerLevel) level)) return;
        // the output of this tick, and the magnets' upkeep kept from it
        if (c.output > 0) {
            c.buffer = Math.min(c.bufferLimit(), c.buffer + c.output);
            long want = FusionPlasma.MAGNET_UPKEEP * SELF_FEED_TICKS - c.apiGetStoredEnergy();
            long feed = Math.min(c.buffer, Math.max(0, Math.min(want, c.apiGetMaxEnergy() - c.apiGetStoredEnergy())));
            if (feed > 0) {
                long added = c.apiGenerateEnergy(feed, false);
                c.buffer -= added;
            }
            if (c.output >= FusionPlasma.FULL_POWER && level.getGameTime() % 20 == 0) MioAchievements.at(level, pos, "fusion_full_power", "full");
        }
        if (c.plasma.state() == FusionPlasma.State.BURNING && --c.radiationIn <= 0) {
            c.radiationIn = 20;
            c.radiate((ServerLevel) level);
        }
        boolean active = c.plasma.hasPlasma();
        if (state.hasProperty(mio_icif_block_fusion_controller.ACTIVE) && state.getValue(mio_icif_block_fusion_controller.ACTIVE) != active)
            level.setBlock(pos, state.setValue(mio_icif_block_fusion_controller.ACTIVE, active), Block.UPDATE_CLIENTS);
        TickMarks.saved(c);
    }

    private void blanketCycle(long neutronPower) {
        ItemStack[] grid = new ItemStack[FusionBlanket.SLOTS];
        for (int i = 0; i < FusionBlanket.SLOTS; i++) grid[i] = items.getStackInSlot(i);
        FusionBlanket.Cycle cycle = FusionBlanket.cycle(grid, hull, plasma.hasPlasma() ? neutronPower : 0);
        blanketInvalid = cycle.invalid();
        if (cycle.invalid()) {
            output = 0;
            thermal = 0;
            return;
        }
        for (int i = 0; i < FusionBlanket.SLOTS; i++)
            if (!ItemStack.isSameItemSameComponents(grid[i], cycle.stacks()[i]) || grid[i].getCount() != cycle.stacks()[i].getCount())
                items.setStackInSlot(i, cycle.stacks()[i]);
        hull = cycle.hull();
        maxHull = cycle.maxHull();
        blanketOverheated = cycle.overheated();
        fissionEu = cycle.fissionEu();
        lithiumShare = cycle.lithiumShare();
        output = plasma.hasPlasma() ? cycle.output() : 0;
        // 0.1.11.135: the plant's readouts
        long neutrons = plasma.hasPlasma() ? neutronPower : 0;
        thermal = plasma.hasPlasma() ? cycle.thermal() : 0;
        multiplication = FusionBlanket.multiplication(neutrons, lithiumShare, fissionEu);
        fissionsPerNeutron = FusionBlanket.fissionsPerNeutron(neutrons, fissionEu);
        tbr = cycleFuel > 0 ? (double) cycle.bred() / cycleFuel : 0;
    }

    /** What a disruption does to the blanket (the structure is never damaged). */
    private void disrupt(boolean hadPlasma, double kev) {
        output = 0;
        if (!hadPlasma || level == null) return;
        ItemStack coolant = items.getStackInSlot(SLOT_MITIGATION);
        long cold = coldOf(coolant);
        boolean mitigated = cold > 0;
        Component coolantName = coolant.getHoverName();
        long heat;
        if (mitigated) {
            items.extractItem(SLOT_MITIGATION, 1, false);
            heat = Math.round(maxHull * mitigatedHeatShare(cold));
        } else {
            for (int row = 0; row < FusionBlanket.ROWS; row++) items.setStackInSlot(row * FusionBlanket.COLUMNS, ItemStack.EMPTY);
            heat = Math.round(maxHull * UNMITIGATED_HEAT_SHARE);
        }
        // 0.1.11.94: the heat may take the blanket to its maximum: a meltdown on the next tick (with the blast off, it stops
        // just below, as before)
        hull = explosionPower() > 0 ? Math.min(maxHull, hull + heat) : Math.min(maxHull - 1, hull + heat);
        blanketOverheated = false;
        BlockPos centre = FusionStructure.tokamak().centre(worldPosition, facing());
        level.playSound(null, centre, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.0F, mitigated ? 1.4F : 0.7F);
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(centre).inflate(24)))
            p.displayClientMessage(mitigated ? Component.translatable("message.mio_icif.fusion.disruption_mitigated", coolantName)
                : Component.translatable("message.mio_icif.fusion.disruption"), true);
        setChanged();
    }

    /**
     * 0.1.11.94: the blanket reached its maximum heat. With the blast on (config), the controller and its contents go (the fuel
     * is not scattered) and a nuclear blast of {@link #explosionPower()} goes off at the torus's centre; with the nuclear
     * terrain damage off (enableNuclearExplosion), a local explosion that breaks nothing around. Returns false when the blast is
     * off (the plasma disrupts instead); true when it went off or the explosion queue is full (the next tick tries again).
     */
    private boolean meltdown(ServerLevel server) {
        float power = explosionPower();
        if (power <= 0) return false;
        BlockPos centre = FusionStructure.tokamak().centre(worldPosition, facing());
        boolean nuclear = com.miophas.singularity_iteration.common.Singularity_Iteration_Config.ENABLE_NUCLEAR_EXPLOSION.get();
        com.miophas.singularity_iteration.common.reactor.ReactorBlastEffects.Prepared blast = null;
        if (nuclear) {
            try {
                blast = com.miophas.singularity_iteration.common.reactor.ReactorBlastEffects.tryPrepare(server, centre, power);
            } catch (RuntimeException e) {
                com.miophas.singularity_iteration.common.Singularity_Iteration.LOGGER.error("Fusion blast admission failed at {}", worldPosition, e);
                return false;
            }
            if (blast == null) return true;   // the queue is full: the next tick tries again
        }
        for (Player p : server.getEntitiesOfClass(Player.class, new AABB(centre).inflate(64)))
            p.displayClientMessage(Component.translatable("message.mio_icif.fusion.meltdown"), false);
        for (int i = 0; i < SLOTS; i++) items.setStackInSlot(i, ItemStack.EMPTY);
        deuterium = tritium = 0;
        BlockPos at = worldPosition;
        server.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (blast != null) blast.present();
        else server.explode(null, centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.NONE);
        MioAchievements.at(server, at, "meltdown", "reactor_meltdown");
        return true;
    }

    private void radiate(ServerLevel server) {
        BlockPos centre = FusionStructure.tokamak().centre(worldPosition, facing());
        var source = RadiationShielding.centre(centre);
        for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, new AABB(centre).inflate(RADIATION_RANGE))) {
            int dose = server.random.nextInt(4);
            if (dose <= 0 || RadiationProtectionUtil.isRadiationProtected(entity)) continue;
            double through = RadiationShielding.transmission(server, source, entity, centre);
            float damage = (float) (dose * through);
            if (damage < 0.25F) continue;
            entity.hurt(new DamageSource(server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(mio_icif_DamageTypes.RADIATION)), damage);
        }
    }

    /**
     * Moves fuel rods from the fuel slots into the tanks while there is room, and puts the empty fuel rods out (0.1.11.94: the
     * deuterium rod too; both output slots take empty rods, each fuel's own first).
     */
    private void takeFuel() {
        ItemStack d = items.getStackInSlot(SLOT_DEUTERIUM);
        if (!d.isEmpty()) {
            long units = fuelUnits(d);
            if (deuterium + units <= TANK && putOutRod(SLOT_EMPTY_CELL, SLOT_EMPTY_ROD)) {
                items.extractItem(SLOT_DEUTERIUM, 1, false);
                deuterium += units;
            }
        }
        ItemStack t = items.getStackInSlot(SLOT_TRITIUM);
        if (!t.isEmpty()) {
            long units = fuelUnits(t);
            if (tritium + units <= TANK && putOutRod(SLOT_EMPTY_ROD, SLOT_EMPTY_CELL)) {
                items.extractItem(SLOT_TRITIUM, 1, false);
                tritium += units;
            }
        }
    }

    /** The fuel units of a deuterium or tritium rod: 10,000 when fresh, the share it has left when used in a fission reactor. */
    public static long fuelUnits(ItemStack stack) {
        try {
            var part = ReactorInventory.read(stack.copyWithCount(1));
            if (part == null || part.profile().capacity() <= 0) return 0;
            long left = Math.max(0, part.profile().capacity() - part.stored());
            return FUEL_PER_CELL * left / part.profile().capacity();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return FUEL_PER_CELL;
        }
    }

    /** The undecayed part of a tritium rod (0.1.11.92 name). */
    public static long tritiumUnits(ItemStack stack) {
        return fuelUnits(stack);
    }

    private boolean putOutRod(int first, int second) {
        ItemStack rod = new ItemStack(mio_icif_normal.FUEL_ROD.get());
        return putOut(first, rod) || putOut(second, rod.copy());
    }

    private boolean putOut(int slot, ItemStack empty) {
        ItemStack there = items.getStackInSlot(slot);
        if (there.isEmpty()) {
            items.setStackInSlot(slot, empty);
            return true;
        }
        if (ItemStack.isSameItemSameComponents(there, empty) && there.getCount() < there.getMaxStackSize()) {
            items.setStackInSlot(slot, there.copyWithCount(there.getCount() + 1));
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ for the ports and the screen

    /**
     * The most the output store keeps: one tick of the output, at least {@link FusionPlasma#FULL_POWER} (0.1.11.98: it was the
     * cap).
     */
    private long bufferLimit() {
        return Math.max(FusionPlasma.FULL_POWER, Math.min(FusionPlasma.OUTPUT_LIMIT, output));
    }

    /** EU a power output port may take this tick: its share of the output (or of what is buffered). */
    public long portShare() {
        long per = Math.max(output, Math.min(buffer, bufferLimit()));
        return powerPorts <= 0 ? 0 : Math.max(1, per / powerPorts);
    }

    public long takeEnergy(long amount) {
        long taken = Math.max(0, Math.min(amount, buffer));
        buffer -= taken;
        return taken;
    }

    public void returnEnergy(long amount) {
        if (amount > 0) buffer = Math.min(bufferLimit(), buffer + amount);
    }

    public Status status() {
        if (!formed) return Status.UNFORMED;
        if (blanketInvalid) return Status.BLANKET_INVALID;
        if (cryos <= 0) return Status.NO_CRYO;
        if (level != null && !level.hasNeighborSignal(worldPosition)) return Status.NO_SIGNAL;
        if (heaters <= 0) return Status.NO_HEATER;
        if (exhausts <= 0) return Status.NO_EXHAUST;
        if (powerPorts <= 0) return Status.NO_POWER_PORT;
        if (deuterium <= 0 || tritium <= 0) return Status.NO_FUEL;
        return Status.OK;
    }

    public int getFeed() { return feed; }

    public void setFeed(int value) {
        int v = FusionPlasma.clampFeed(value);
        if (v != feed) {
            feed = v;
            setChanged();
        }
    }

    public IItemHandler getItems() { return items; }
    public FusionPlasma plasma() { return plasma; }
    public boolean isFormed() { return formed; }
    public long output() { return output; }
    public long hull() { return hull; }
    public long maxHull() { return maxHull; }
    public long deuterium() { return deuterium; }
    public long tritium() { return tritium; }
    public long buffer() { return buffer; }
    public int powerPorts() { return powerPorts; }
    public int heaters() { return heaters; }
    public List<BlockPos> ports() { return List.copyOf(ports); }

    /** The values the screen shows (sent as longs). */
    public long[] screenValues() {
        double q = plasma.q();
        return new long[]{
            plasma.state().ordinal(), Math.round(plasma.kev() * 100), feed, lastFusion, output, apiGetStoredEnergy(), apiGetMaxEnergy(),
            buffer, deuterium, tritium, hull, maxHull, plasma.cooled(), plasma.charged(), plasma.burnTicks(), plasma.ash(),
            Double.isInfinite(q) ? -1 : Math.round(q * 100), formed ? 1 : 0, powerPorts, heaters, cyclotron, fuelPorts, exhausts, cryos,
            mitigations, status().ordinal(), wrongAt == null ? 0 : wrongAt.getX(), wrongAt == null ? 0 : wrongAt.getY(),
            wrongAt == null ? 0 : wrongAt.getZ(), expected == null ? -1 : expected.ordinal(), plasma.timer(), Math.round(fissionEu),
            Math.round(lithiumShare * 1000), lastHeating, lastUsed,
            // 0.1.11.133: the plasma's power balance: density (10^17 m^-3), confinement time (ms), triple product (10^18 keV s/m^3),
            // alpha heating and losses (EU/t)
            Math.round(plasma.density() / 1.0e17), Math.round(plasma.tauE() * 1000), Math.round(plasma.tripleProduct() / 1.0e18),
            plasma.lastAlpha(), plasma.lastLoss(),
            // 0.1.11.134: the operating limits: Greenwald and beta fractions (per mille), H-mode, the last disruption's cause, ELMs
            Math.round(plasma.greenwaldFraction() * 1000), Math.round(plasma.betaFraction() * 1000), plasma.hMode() ? 1 : 0,
            plasma.cause().ordinal(), plasma.elms(),
            // 0.1.11.135: the plant: heat (EU/t), energy multiplication, fissions per neutron, tritium breeding ratio, engineering Q
            // (all x100)
            thermal, Math.round(multiplication * 100), Math.round(fissionsPerNeutron * 100), Math.round(tbr * 100),
            lastUsed > 0 ? Math.round(output * 100.0 / lastUsed) : 0,
            // 0.1.11.136: the fuel cycle: units of each injected and used a tick (x100)
            Math.round(plasma.injected() * 100), Math.round(plasma.injected() * (exhausts > 0 ? FusionPlasma.BURN_FRACTION : 1) * 100)};
    }

    /**
     * The controller's own faces, for pipes and hoppers: deuterium and tritium fuel rods in, the empty fuel rods out; nothing
     * reaches the blanket or the mitigation slot (0.1.11.97: the fuel injection and disruption mitigation ports have slots of
     * their own, {@link FusionItemPortBlockEntity}).
     */
    public IItemHandler fuelHandler() {
        return new SlotView(new int[]{SLOT_DEUTERIUM, SLOT_TRITIUM, SLOT_EMPTY_CELL, SLOT_EMPTY_ROD});
    }

    /** Some of the controller's slots, for pipes and hoppers. */
    private final class SlotView implements IItemHandler {
        private final int[] slots;
        SlotView(int[] slots) { this.slots = slots; }
        @Override public int getSlots() { return slots.length; }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slots[slot]); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return items.insertItem(slots[slot], stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int s = slots[slot];
            return s == SLOT_EMPTY_CELL || s == SLOT_EMPTY_ROD ? items.extractItem(s, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slots[slot]); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return valid(slots[slot], stack); }
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mio_icif.fusion.block_fusion_controller");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FusionControllerMenu(id, inventory, this);
    }

    // ------------------------------------------------------------------ lifecycle and saves

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            synchronized (LOADED) { LOADED.computeIfAbsent(level, l -> new HashSet<>()).add(worldPosition.immutable()); }
            recheck = true;
            for (BlockPos p : ports) FusionPortRegistry.link(level, p, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            synchronized (LOADED) {
                Set<BlockPos> at = LOADED.get(level);
                if (at != null) at.remove(worldPosition);
            }
            for (BlockPos p : ports) {
                if (level.isLoaded(p) && level.getBlockEntity(p) instanceof FusionPart part) part.unlink(worldPosition);
                FusionPortRegistry.unlink(level, p);
            }
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) {
            synchronized (LOADED) {
                Set<BlockPos> at = LOADED.get(level);
                if (at != null) at.remove(worldPosition);
            }
        }
        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.put("Plasma", plasma.save());
        tag.putInt("Feed", feed);
        tag.putLong("Deuterium", deuterium);
        tag.putLong("Tritium", tritium);
        tag.putLong("Hull", hull);
        tag.putLong("MaxHull", maxHull);
        tag.putLong("Output", output);
        tag.putLong("Buffer", buffer);
        tag.putBoolean("Formed", formed);
        ListTag list = new ListTag();
        for (BlockPos p : ports) list.add(NbtUtils.writeBlockPos(p));
        tag.put("Ports", list);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) items.deserializeNBT(registries, tag.getCompound("Items"));
        plasma.load(tag.getCompound("Plasma"));
        feed = FusionPlasma.clampFeed(tag.contains("Feed") ? tag.getInt("Feed") : FusionPlasma.MIN_FEED);
        deuterium = Math.max(0, Math.min(TANK, tag.getLong("Deuterium")));
        tritium = Math.max(0, Math.min(TANK, tag.getLong("Tritium")));
        hull = Math.max(0, tag.getLong("Hull"));
        maxHull = Math.max(1, tag.contains("MaxHull") ? tag.getLong("MaxHull") : 10_000);
        output = Math.max(0, Math.min(FusionPlasma.OUTPUT_LIMIT, tag.getLong("Output")));
        buffer = Math.max(0, Math.min(FusionPlasma.OUTPUT_LIMIT, tag.getLong("Buffer")));
        formed = tag.getBoolean("Formed");
        ports.clear();
        for (Tag t : tag.getList("Ports", Tag.TAG_INT_ARRAY)) NbtUtils.readBlockPos(wrap(t), "P").ifPresent(ports::add);
    }

    private static CompoundTag wrap(Tag t) {
        CompoundTag c = new CompoundTag();
        c.put("P", t);
        return c;
    }

    // ---- legacy grid API: it only takes EU
    @Override public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) { return true; }
}
