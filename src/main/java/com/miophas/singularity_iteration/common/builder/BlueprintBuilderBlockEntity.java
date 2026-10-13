// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Blueprint Builder (0.1.8.0): autonomous builder drones place a blueprint block by block.
 *
 * <h2>Specification</h2>
 * <ul>
 *   <li>Blueprint source: a deployed Create schematic in the schematic slot (anchor, rotation
 *       and mirror of the item), else a file of the {@code schematics} folder ({@code .nbt} or
 *       {@code .litematic}), placed with its minimum corner at the builder position plus the
 *       offset, with the rotation and mirror of the GUI.</li>
 *   <li>Every block of the placement must be within {@link #RANGE} blocks of the builder.</li>
 *   <li>{@link #DRONES} drones. A drone takes up to {@link #BATCH} blocks of one material from the
 *       nearest Material Interface within {@link #INTERFACE_RANGE} blocks that has it, flies to
 *       each position and places the block. Order: bottom layer first, near blocks first.</li>
 *   <li>Each placed block costs {@link #EU_PER_BLOCK} EU. Without energy the drones wait.</li>
 *   <li>A position that already has the target block counts as done. A position with a
 *       different, not replaceable block is skipped ("obstructed"); the builder never breaks a
 *       block. The upper half of a door or plant and the head of a bed need no material.</li>
 *   <li>Protection: each placement fires the NeoForge block place event as the owner (a fake
 *       player with the owner profile); a cancelled event skips the position. Spawn protection
 *       applies.</li>
 *   <li>Items are never lost: cargo is saved with the builder. A drone brings unused material
 *       back to the interface; when the builder is broken or stopped, cargo drops or goes back.</li>
 *   <li>0.1.8.4 UU supply: when no Material Interface has a material, the builder orders it from
 *       an idle Replicator within the supply range (common config {@code BlueprintBuilder}) that
 *       has the pattern (memory crystal or adjacent Pattern Storage). The replicator pays the copy
 *       with its own UU matter and energy at the usual price; the drones collect the copies from
 *       its output slot. One order covers the rest of that material in the plan, at most 64.
 *       Leftover cargo from a replicator goes to the nearest interface (without one it drops at the
 *       drone, as other cargo without a source does).</li>
 * </ul>
 *
 * <p>The drones are not entities: the builder simulates them, saves them, and sends each new
 * flight leg to the clients (start, end, start time, duration). Clients interpolate. Thus a
 * drone costs no entity tick, cannot be killed or unloaded on its own, and the network load is
 * one small update per leg.
 */
@SuppressWarnings("null")
public class BlueprintBuilderBlockEntity extends AbstractProcessingMachineBlockEntity
        implements com.miophas.singularity_iteration.common.machine.ClientRunState {
    /** EN: 0.1.8.1: drone-bay loop sound; "Running" is in the update tag. ZH: 0.1.8.1：无人机舱循环音效，“Running”在更新标签中。JA: 0.1.8.1：ドローン格納庫の音。「Running」は更新タグに含まれる。 */
    @Override public boolean clientRunning() { return running; }
    public static final int BATTERY_SLOT = 0, SCHEMATIC_SLOT = 1, SLOT_COUNT = 2;
    public static final long CAPACITY = 200_000L, MAX_RECEIVE = 512L, EU_PER_BLOCK = 40L;
    public static final int DRONES = 4, BATCH = 8, RANGE = 64, INTERFACE_RANGE = 16, SCAN_WINDOW = 512;
    public static final double SPEED = 0.8;
    public static final int DATA_COUNT = 23;
    public static final int NO_BLUEPRINT = 0, READY = 1, RUNNING = 2, PAUSED = 3, DONE = 4, TOO_FAR = 5, READ_ERROR = 6;

    private static final SlotLayout LAYOUT = SlotLayout.builder().battery().extra(1).build();

    // ---- settings (saved)
    @Nullable private UUID owner;
    private String ownerName = "";
    private String fileName = "";
    private int fileIndex;
    private int rotation, mirror;
    private int offX = 1, offY = 0, offZ = 1;
    private boolean useCreate, running, preview = true;
    private int status = NO_BLUEPRINT;
    // ---- plan (rebuilt from the settings after a load)
    @Nullable private List<Blueprint.Entry> plan;
    private boolean[] done = new boolean[0], claimed = new boolean[0];
    private int cursor, placed, obstructed, doneCount;
    private boolean reloadPlan;
    @Nullable private AABB bounds;
    private final Map<Item, Integer> missing = new HashMap<>();
    private final Map<Item, Long> missingUntil = new HashMap<>();
    /** 0.1.8.4: materials ordered from replicators, by item (open copies), for the screen. */
    private final Map<Item, Integer> replicating = new HashMap<>();
    private List<BlockPos> replicators = List.of();
    private List<String> clientReplicating = List.of();
    private List<String> files = List.of();
    // ---- drones (saved)
    private final Agent[] agents = new Agent[DRONES];
    private boolean syncDirty;
    private List<BlockPos> interfaces = List.of();
    private long interfacesScanned = -1000;

    /** One builder drone. Phases: docked, to source, placing, returning cargo, to dock. */
    static final class Agent {
        static final int DOCKED = 0, TO_SOURCE = 1, PLACING = 2, RETURNING = 3, TO_DOCK = 4;
        int phase = DOCKED;
        Vec3 from = Vec3.ZERO, to = Vec3.ZERO;
        long legStart;
        int legTicks = 1;
        ItemStack cargo = ItemStack.EMPTY;
        int[] tasks = new int[0];
        int taskIndex;
        @Nullable BlockPos source;

        CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag t = new CompoundTag();
            t.putInt("Phase", phase);
            putVec(t, "From", from); putVec(t, "To", to);
            t.putLong("LegStart", legStart); t.putInt("LegTicks", legTicks);
            if (!cargo.isEmpty()) t.put("Cargo", cargo.save(registries));
            t.putIntArray("Tasks", tasks); t.putInt("TaskIndex", taskIndex);
            if (source != null) t.putLong("Source", source.asLong());
            return t;
        }

        void load(CompoundTag t, HolderLookup.Provider registries) {
            phase = t.getInt("Phase");
            from = getVec(t, "From"); to = getVec(t, "To");
            legStart = t.getLong("LegStart"); legTicks = Math.max(1, t.getInt("LegTicks"));
            cargo = t.contains("Cargo") ? ItemStack.parse(registries, t.getCompound("Cargo")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
            tasks = t.getIntArray("Tasks"); taskIndex = t.getInt("TaskIndex");
            source = t.contains("Source") ? BlockPos.of(t.getLong("Source")) : null;
        }

        /** Client: position at {@code time} (game ticks with partial tick), smooth start and stop. */
        Vec3 position(double time) {
            double u = Math.max(0, Math.min(1, (time - legStart) / legTicks));
            u = u * u * (3 - 2 * u);
            return from.add(to.subtract(from).scale(u));
        }
    }

    private static void putVec(CompoundTag t, String key, Vec3 v) {
        t.putDouble(key + "X", v.x); t.putDouble(key + "Y", v.y); t.putDouble(key + "Z", v.z);
    }

    private static Vec3 getVec(CompoundTag t, String key) {
        return new Vec3(t.getDouble(key + "X"), t.getDouble(key + "Y"), t.getDouble(key + "Z"));
    }

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            long energy = getEnergyStorage().getAmount();
            return switch (i) {
                case 0 -> (int) (energy & 0xFFFF);
                case 1 -> (int) ((energy >>> 16) & 0xFFFF);
                case 2 -> (int) ((energy >>> 32) & 0xFFFF);
                case 3 -> status;
                case 4 -> placed & 0xFFFF; case 5 -> placed >>> 16;
                case 6 -> total() & 0xFFFF; case 7 -> total() >>> 16;
                case 8 -> obstructed & 0xFFFF; case 9 -> obstructed >>> 16;
                case 10 -> missingTotal() & 0xFFFF; case 11 -> missingTotal() >>> 16;
                case 12 -> rotation;
                case 13 -> mirror;
                case 14 -> offX & 0xFFFF; case 15 -> offY & 0xFFFF; case 16 -> offZ & 0xFFFF;
                case 17 -> fileIndex;
                case 18 -> useCreate ? 1 : fileName.isEmpty() ? 0 : 2;
                case 19 -> preview ? 1 : 0;
                case 20 -> activeDrones();
                case 21 -> doneCount & 0xFFFF; case 22 -> doneCount >>> 16;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public BlueprintBuilderBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, BuilderRegistry.BUILDER_ENTITY.get(), CAPACITY, MAX_RECEIVE, 0L, 100, LAYOUT, 0L, CableTier.MV);
        for (int i = 0; i < DRONES; i++) {
            agents[i] = new Agent();
            agents[i].from = agents[i].to = dock(i);
        }
    }

    private void markDone(int t) {
        if (!done[t]) { done[t] = true; doneCount++; }
    }

    // ------------------------------------------------------------------ accessors (client renderer, screen)
    public int total() { return plan == null ? 0 : plan.size(); }
    private int missingTotal() { int n = 0; for (int v : missing.values()) n += v; return n; }
    private int activeDrones() { int n = 0; for (Agent a : agents) if (a.phase != Agent.DOCKED) n++; return n; }
    public boolean preview() { return preview; }
    public boolean running() { return running; }
    @Nullable public AABB bounds() { return bounds; }
    Agent[] agents() { return agents; }
    public List<String> files() { return files; }
    public String blueprintName() { return clientName; }
    public List<String> missingNames() { return clientMissing; }
    /** Client: materials being made by replicators ("id=count"). */
    public List<String> replicatingNames() { return clientReplicating; }
    private String clientName = "";
    private List<String> clientMissing = List.of();

    void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        setChanged();
    }

    private Vec3 dock(int i) {
        double a = i * Math.PI / 2 + Math.PI / 4;
        return Vec3.atCenterOf(worldPosition).add(Math.cos(a) * 0.32, 0.78, Math.sin(a) * 0.32);
    }

    // ------------------------------------------------------------------ GUI actions
    public static final int A_FILE_PREV = 0, A_FILE_NEXT = 1, A_LOAD = 2, A_ROTATE = 3, A_MIRROR = 4, A_OFFSET = 5,
        A_START = 6, A_PREVIEW = 7, A_REFRESH = 8, A_STOP = 9;

    public void action(Player player, int action, int index) {
        if (!(level instanceof ServerLevel)) return;
        switch (action) {
            case A_REFRESH -> refreshFiles();
            case A_FILE_PREV, A_FILE_NEXT -> {
                if (files.isEmpty()) refreshFiles();
                if (!files.isEmpty()) fileIndex = Math.floorMod(fileIndex + (action == A_FILE_NEXT ? 1 : -1), files.size());
            }
            case A_LOAD -> { if (!running) load(player); }
            case A_ROTATE -> { if (!running && !useCreate) { rotation = (rotation + 1) % 4; rebuild(); } }
            case A_MIRROR -> { if (!running && !useCreate) { mirror = (mirror + 1) % 3; rebuild(); } }
            case A_OFFSET -> {
                if (running || useCreate) break;
                int step = (index & 1) == 0 ? -1 : 1;
                switch (index >> 1) {
                    case 0 -> offX = clampOffset(offX + step);
                    case 1 -> offY = clampOffset(offY + step);
                    default -> offZ = clampOffset(offZ + step);
                }
                rebuild();
            }
            case A_START -> {
                if (plan == null || status == TOO_FAR) break;
                running = !running;
                status = running ? RUNNING : PAUSED;
                missingUntil.clear();
            }
            case A_PREVIEW -> preview = !preview;
            case A_STOP -> stop();
            default -> { }
        }
        setChanged();
        markSync();
    }

    private static int clampOffset(int v) { return Math.max(-RANGE, Math.min(RANGE, v)); }

    private void refreshFiles() {
        files = BlueprintReader.list();
        if (fileIndex >= files.size()) fileIndex = 0;
    }

    private void load(Player player) {
        stop();
        ItemStack slot = getItemHandler().getStackInSlot(SCHEMATIC_SLOT);
        useCreate = BlueprintReader.isCreateSchematic(slot);
        if (!useCreate) {
            if (files.isEmpty()) refreshFiles();
            fileName = files.isEmpty() ? "" : files.get(Math.min(fileIndex, files.size() - 1));
        } else {
            fileName = "";
        }
        rebuild();
        if (status == READ_ERROR) player.displayClientMessage(Component.translatable("message.mio_icif.builder.read_error"), true);
    }

    /** Reads the blueprint of the current settings and builds the plan. */
    private void rebuild() {
        if (!(level instanceof ServerLevel world)) return;
        plan = null;
        bounds = null;
        status = NO_BLUEPRINT;
        missing.clear();
        var blocks = world.holderLookup(Registries.BLOCK);
        List<Blueprint.Entry> placed;
        String name;
        if (useCreate) {
            var create = BlueprintReader.readCreate(getItemHandler().getStackInSlot(SCHEMATIC_SLOT), blocks);
            if (create == null) { status = READ_ERROR; clientName = ""; return; }
            placed = create.blueprint().placeAnchored(create.anchor(), create.rotation(), create.mirror());
            name = create.blueprint().name();
        } else {
            if (fileName.isEmpty()) return;
            Blueprint bp = BlueprintReader.readFile(fileName, blocks);
            if (bp == null) { status = READ_ERROR; clientName = fileName; return; }
            placed = bp.place(worldPosition.offset(offX, offY, offZ), Rotation.values()[rotation], Mirror.values()[mirror]);
            name = bp.name();
        }
        clientName = name;
        // order: bottom layer first, then near the builder first (deterministic: saved drone tasks refer to it)
        List<Blueprint.Entry> ordered = new ArrayList<>(placed.size());
        for (Blueprint.Entry e : placed) if (material(e.state()) != null && world.isInWorldBounds(e.pos())) ordered.add(e);
        BlockPos c = worldPosition;
        ordered.sort((a, b) -> {
            if (a.pos().getY() != b.pos().getY()) return Integer.compare(a.pos().getY(), b.pos().getY());
            int d = Long.compare(a.pos().distManhattan(c), b.pos().distManhattan(c));
            if (d != 0) return d;
            return Long.compare(a.pos().asLong(), b.pos().asLong());
        });
        plan = List.copyOf(ordered);
        done = new boolean[plan.size()];
        claimed = new boolean[plan.size()];
        cursor = 0;
        this.placed = 0;
        obstructed = 0;
        doneCount = 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        boolean far = false;
        for (Blueprint.Entry e : plan) {
            BlockPos p = e.pos();
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
            far |= Math.abs(p.getX() - c.getX()) > RANGE || Math.abs(p.getY() - c.getY()) > RANGE || Math.abs(p.getZ() - c.getZ()) > RANGE;
        }
        if (!plan.isEmpty()) bounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        status = far ? TOO_FAR : plan.isEmpty() ? DONE : READY;
    }

    /** Stops: claims are released, drones bring their cargo back. */
    private void stop() {
        running = false;
        if (plan != null) java.util.Arrays.fill(claimed, false);
        for (int i = 0; i < DRONES; i++) {
            Agent a = agents[i];
            a.tasks = new int[0];
            a.taskIndex = 0;
            if (a.phase != Agent.DOCKED && a.phase != Agent.RETURNING) {
                if (!a.cargo.isEmpty() && a.source != null) leg(a, Agent.RETURNING, approach(a.source));
                else leg(a, Agent.TO_DOCK, dock(i));
            }
        }
        if (status == RUNNING) status = PAUSED;
        replicating.clear();
    }

    // ------------------------------------------------------------------ simulation
    @Override
    protected void tickProduction() {
        if (!(level instanceof ServerLevel world)) return;
        if (reloadPlan) {
            reloadPlan = false;
            boolean wasRunning = running;
            rebuild();
            running = wasRunning && plan != null && status != TOO_FAR;
            if (running) status = RUNNING;
            // claims of saved drones are valid again (the plan order is deterministic)
            for (Agent a : agents) for (int t : a.tasks) if (plan != null && t >= 0 && t < claimed.length) claimed[t] = true;
        }
        long now = world.getGameTime();
        for (int i = 0; i < DRONES; i++) {
            Agent a = agents[i];
            if (now < a.legStart + a.legTicks) continue;
            arrive(world, i, a, now);
        }
        if (running && plan != null) {
            while (cursor < done.length && done[cursor]) cursor++;
            boolean busy = false;
            for (Agent a : agents) busy |= a.taskIndex < a.tasks.length;
            if (cursor >= done.length && !busy) {
                running = false;
                status = DONE;
                world.playSound(null, worldPosition, com.miophas.singularity_iteration.common.registry.mio_icif_sounds.ARMORY_COMPLETE.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
                markSync();
            }
        }
        if (syncDirty && now % 2 == 0) {
            syncDirty = false;
            world.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private void arrive(ServerLevel world, int i, Agent a, long now) {
        switch (a.phase) {
            case Agent.TO_SOURCE -> fetch(world, i, a);
            case Agent.PLACING -> placeNext(world, i, a, now);
            case Agent.RETURNING -> {
                if (!a.cargo.isEmpty()) {
                    ItemStack rest = a.source != null && world.getBlockEntity(a.source) instanceof MaterialInterfaceBlockEntity mi ? mi.insert(a.cargo) : a.cargo;
                    if (!rest.isEmpty() && !interfaces.isEmpty() && world.getBlockEntity(interfaces.get(0)) instanceof MaterialInterfaceBlockEntity near) rest = near.insert(rest);
                    if (!rest.isEmpty()) net.minecraft.world.Containers.dropItemStack(world, a.to.x, a.to.y, a.to.z, rest);
                    a.cargo = ItemStack.EMPTY;
                }
                if (!claim(world, i, a)) leg(a, Agent.TO_DOCK, dock(i));
            }
            case Agent.TO_DOCK -> {
                a.phase = Agent.DOCKED;
                if (running) claim(world, i, a);
            }
            default -> { if (running && now % 10 == i) claim(world, i, a); }   // docked: look for work twice a second
        }
    }

    /** Claims the next blocks of one material and starts the flight (to a source, or straight to the work). */
    private boolean claim(ServerLevel world, int i, Agent a) {
        if (!running || plan == null) return false;
        long now = world.getGameTime();
        int first = -1;
        Item item = null;
        int scanned = 0;
        for (int t = cursor; t < plan.size() && scanned < SCAN_WINDOW; t++, scanned++) {
            if (done[t] || claimed[t]) continue;
            Blueprint.Entry e = plan.get(t);
            if (!world.isLoaded(e.pos())) continue;
            BlockState here = world.getBlockState(e.pos());
            BlockState target = target(e.state());
            if (here.equals(target)) { markDone(t); continue; }
            if (!here.canBeReplaced()) { markDone(t); obstructed++; continue; }
            Item m = material(target);
            if (m != null && m != Items.AIR && missingUntil.getOrDefault(m, 0L) > now) continue;
            first = t;
            item = m;
            break;
        }
        if (first < 0) return false;
        List<Integer> batch = new ArrayList<>();
        batch.add(first);
        int need = amount(target(plan.get(first).state()));
        BlockPos origin = plan.get(first).pos();
        for (int t = first + 1; t < plan.size() && batch.size() < BATCH && t - first < SCAN_WINDOW; t++) {
            if (done[t] || claimed[t]) continue;
            BlockState target = target(plan.get(t).state());
            if (material(target) != item || plan.get(t).pos().distManhattan(origin) > 12) continue;
            batch.add(t);
            need += amount(target);
        }
        for (int t : batch) claimed[t] = true;
        a.tasks = batch.stream().mapToInt(Integer::intValue).toArray();
        a.taskIndex = 0;
        if (item == null || item == Items.AIR) {                               // no material needed
            leg(a, Agent.PLACING, approach(plan.get(a.tasks[0]).pos()));
            return true;
        }
        BlockPos source = findSource(world, item);
        if (source == null) {
            release(a);
            if (orderFromReplicator(world, item, first)) {
                missingUntil.put(item, now + 40);                 // look again when the first copies may be ready
                markSync();
                return false;
            }
            missing.merge(item, need, Integer::sum);
            missingUntil.put(item, now + 100);
            markSync();
            return false;
        }
        a.source = source;
        leg(a, Agent.TO_SOURCE, approach(source));
        return true;
    }

    private void fetch(ServerLevel world, int i, Agent a) {
        if (a.tasks.length == 0 || plan == null) { leg(a, Agent.TO_DOCK, dock(i)); return; }
        Item item = material(target(plan.get(a.tasks[0]).state()));
        int need = 0;
        for (int t : a.tasks) need += amount(target(plan.get(t).state()));
        var sourceEntity = a.source == null || item == null ? null : world.getBlockEntity(a.source);
        ItemStack got = sourceEntity instanceof MaterialInterfaceBlockEntity mi ? mi.extract(item, need)
            : sourceEntity instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_replicator_elc rep ? rep.takeOutput(item, need) : ItemStack.EMPTY;
        if (got.isEmpty()) {
            release(a);
            if (item != null) { missing.merge(item, need, Integer::sum); missingUntil.put(item, world.getGameTime() + 100); }
            markSync();
            if (!claim(world, i, a)) leg(a, Agent.TO_DOCK, dock(i));
            return;
        }
        if (item != null && missing.remove(item) != null) markSync();
        if (item != null && sourceEntity instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_replicator_elc rep && rep.orderLeft(item) == 0 && replicating.remove(item) != null) markSync();
        a.cargo = got;
        leg(a, Agent.PLACING, approach(plan.get(a.tasks[0]).pos()));
    }

    private void placeNext(ServerLevel world, int i, Agent a, long now) {
        if (plan == null || a.taskIndex >= a.tasks.length) { afterBatch(world, i, a); return; }
        int t = a.tasks[a.taskIndex];
        Blueprint.Entry e = plan.get(t);
        BlockState target = target(e.state());
        BlockState here = world.getBlockState(e.pos());
        boolean finished = true;
        if (here.equals(target)) {
            // somebody else built it
        } else if (!here.canBeReplaced()) {
            obstructed++;
        } else {
            Item m = material(target);
            int cost = m == null || m == Items.AIR ? 0 : amount(target);
            if (cost > 0 && (!a.cargo.is(m) || a.cargo.getCount() < cost)) {
                // no cargo for this one: give it back to the plan
                claimed[t] = false;
                finished = false;
            } else if (apiGetStoredEnergy() < EU_PER_BLOCK) {
                a.from = a.to; a.legStart = now; a.legTicks = 10;               // hover in place, wait for power
                return;
            } else if (mayPlace(world, e.pos(), target)) {
                apiConsumeEnergy(EU_PER_BLOCK);
                if (cost > 0) a.cargo.shrink(cost);
                world.setBlock(e.pos(), target, flags(target));
                var sound = target.getSoundType(world, e.pos(), null);
                world.playSound(null, e.pos(), sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
                world.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.pos().getX() + 0.5, e.pos().getY() + 0.8, e.pos().getZ() + 0.5, 4, 0.25, 0.1, 0.25, 0.02);
                placed++;
            } else {
                obstructed++;                                                   // protected position
            }
        }
        if (finished) markDone(t);
        claimed[t] = !finished && claimed[t];
        a.taskIndex++;
        markSync();
        if (a.taskIndex < a.tasks.length) leg(a, Agent.PLACING, approach(plan.get(a.tasks[a.taskIndex]).pos()));
        else afterBatch(world, i, a);
    }

    private void afterBatch(ServerLevel world, int i, Agent a) {
        a.tasks = new int[0];
        a.taskIndex = 0;
        if (!a.cargo.isEmpty() && a.source != null && world.getBlockEntity(a.source) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_replicator_elc) {
            a.source = interfaces.isEmpty() ? null : interfaces.get(0);          // leftovers: nearest interface, else the dock
        }
        if (!a.cargo.isEmpty() && a.source != null) { leg(a, Agent.RETURNING, approach(a.source)); return; }
        if (!a.cargo.isEmpty()) { net.minecraft.world.Containers.dropItemStack(world, a.to.x, a.to.y, a.to.z, a.cargo); a.cargo = ItemStack.EMPTY; }
        if (!claim(world, i, a)) leg(a, Agent.TO_DOCK, dock(i));
    }

    private void release(Agent a) {
        for (int t : a.tasks) if (t >= 0 && t < claimed.length) claimed[t] = false;
        a.tasks = new int[0];
        a.taskIndex = 0;
    }

    private void leg(Agent a, int phase, Vec3 to) {
        Vec3 from = level == null ? to : a.position(level.getGameTime());
        a.phase = phase;
        a.from = from;
        a.to = to;
        a.legStart = level == null ? 0 : level.getGameTime();
        a.legTicks = Math.max(4, (int) Math.ceil(from.distanceTo(to) / SPEED));
        markSync();
    }

    private static Vec3 approach(BlockPos pos) { return Vec3.atCenterOf(pos).add(0, 0.95, 0); }

    private void markSync() { syncDirty = true; setChanged(); }

    @Nullable
    private BlockPos findSource(ServerLevel world, Item item) {
        long now = world.getGameTime();
        if (now - interfacesScanned > 100) {
            interfacesScanned = now;
            List<BlockPos> found = new ArrayList<>();
            int r = INTERFACE_RANGE;
            for (int cx = (worldPosition.getX() - r) >> 4; cx <= (worldPosition.getX() + r) >> 4; cx++) {
                for (int cz = (worldPosition.getZ() - r) >> 4; cz <= (worldPosition.getZ() + r) >> 4; cz++) {
                    if (!world.hasChunk(cx, cz)) continue;
                    for (var be : world.getChunk(cx, cz).getBlockEntities().values()) {
                        if (be instanceof MaterialInterfaceBlockEntity && be.getBlockPos().closerThan(worldPosition, r + 0.5)) found.add(be.getBlockPos());
                    }
                }
            }
            found.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(worldPosition)));
            interfaces = List.copyOf(found);
            replicators = supplyEnabled() ? scanReplicators(world) : List.of();
        }
        for (BlockPos p : interfaces) {
            if (world.getBlockEntity(p) instanceof MaterialInterfaceBlockEntity mi && mi.count(item) > 0) return p;
        }
        for (BlockPos p : replicators) {
            if (world.getBlockEntity(p) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_replicator_elc rep && rep.outputCount(item) > 0) return p;
        }
        return null;
    }

    // ------------------------------------------------------------------ 0.1.8.4 UU supply
    private static boolean supplyEnabled() {
        return com.miophas.singularity_iteration.common.Singularity_Iteration_Config.BUILDER_REPLICATOR_SUPPLY.get();
    }

    /** Replicators within the supply range, nearest first (loaded chunks only). */
    private List<BlockPos> scanReplicators(ServerLevel world) {
        int r = com.miophas.singularity_iteration.common.Singularity_Iteration_Config.BUILDER_REPLICATOR_RANGE.get();
        List<BlockPos> found = new ArrayList<>();
        for (int cx = (worldPosition.getX() - r) >> 4; cx <= (worldPosition.getX() + r) >> 4; cx++) {
            for (int cz = (worldPosition.getZ() - r) >> 4; cz <= (worldPosition.getZ() + r) >> 4; cz++) {
                if (!world.hasChunk(cx, cz)) continue;
                for (var be : world.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_replicator_elc && be.getBlockPos().closerThan(worldPosition, r + 0.5)) found.add(be.getBlockPos());
                }
            }
        }
        found.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(worldPosition)));
        return List.copyOf(found);
    }

    /**
     * Orders {@code item} from a replicator: one that already makes it, else the nearest idle one
     * with the pattern. The order covers the rest of that material in the plan from task
     * {@code from} (at most 64). Returns false when no replicator can make it.
     */
    private boolean orderFromReplicator(ServerLevel world, Item item, int from) {
        if (!supplyEnabled() || plan == null) return false;
        ItemStack want = new ItemStack(item);
        int open = 0;
        for (BlockPos p : replicators) {
            if (world.getBlockEntity(p) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_replicator_elc rep) open += rep.orderLeft(item);
        }
        int rest = 0;
        for (int t = from; t < plan.size() && rest < 64; t++) {
            if (done[t] || claimed[t]) continue;
            BlockState target = target(plan.get(t).state());
            if (material(target) == item) rest += amount(target);
        }
        rest = Math.min(64, rest);
        if (open >= rest && open > 0) { replicating.put(item, open); return true; }   // enough already on order
        for (BlockPos p : replicators) {
            if (world.getBlockEntity(p) instanceof com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_replicator_elc rep && rep.canTakeOrder(want) && rep.placeOrder(want, rest - open)) {
                replicating.put(item, rest);
                missing.remove(item);
                return true;
            }
        }
        return false;
    }

    /** Claim and protection checks with the owner identity. */
    private boolean mayPlace(ServerLevel world, BlockPos pos, BlockState state) {
        GameProfile profile = new GameProfile(owner != null ? owner : UUID.nameUUIDFromBytes("mio_icif_builder".getBytes()),
            ownerName.isEmpty() ? "[Blueprint Builder]" : ownerName);
        var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.get(world, profile);
        if (world.getServer().isUnderSpawnProtection(world, pos, fake)) return false;
        var snapshot = net.neoforged.neoforge.common.util.BlockSnapshot.create(world.dimension(), world, pos);
        var event = new net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent(snapshot, world.getBlockState(pos.below()), fake);
        return !net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    // ------------------------------------------------------------------ material rules
    /** The state to place: never a water source (waterlogged blocks are placed dry). */
    static BlockState target(BlockState s) {
        return s.hasProperty(BlockStateProperties.WATERLOGGED) ? s.setValue(BlockStateProperties.WATERLOGGED, false) : s;
    }

    /** The material item; {@link Items#AIR} for a free second half; null for a state that is not built (fluids, no item). */
    @Nullable
    static Item material(BlockState s) {
        if (s.getBlock() instanceof LiquidBlock || s.isAir()) return null;
        if (s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) return Items.AIR;
        if (s.hasProperty(BlockStateProperties.BED_PART) && s.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) return Items.AIR;
        Item item = s.getBlock().asItem();
        return item == Items.AIR ? null : item;
    }

    static int amount(BlockState s) {
        return s.hasProperty(BlockStateProperties.SLAB_TYPE) && s.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE ? 2 : 1;
    }

    /** Two-part blocks keep their exact shape: no neighbour update that would break the half placed first. */
    static int flags(BlockState s) {
        boolean twoPart = s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || s.hasProperty(BlockStateProperties.BED_PART);
        return twoPart ? Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE : Block.UPDATE_ALL;
    }

    // ------------------------------------------------------------------ removal
    /** Drops the cargo of all drones (the block is broken). */
    void dropCargo(Level world) {
        for (Agent a : agents) {
            if (!a.cargo.isEmpty()) net.minecraft.world.Containers.dropItemStack(world, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, a.cargo);
            a.cargo = ItemStack.EMPTY;
        }
    }

    // ------------------------------------------------------------------ machine plumbing
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) return isBattery(stack);
        return slot == SCHEMATIC_SLOT && BlueprintReader.isCreateSchematic(stack);
    }

    private static final int[] BATTERY_ONLY = {BATTERY_SLOT};

    @Override protected int[] getSlotsForDirection(Direction side) { return BATTERY_ONLY; }
    @Override protected int getBatterySlot() { return BATTERY_SLOT; }
    @Override protected boolean canWork() { return false; }
    @Override protected void doWork() { }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mio_icif.producer.block_blueprint_builder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (files.isEmpty()) refreshFiles();
        markSync();
        return new BlueprintBuilderMenu(containerId, inventory, this, data);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlueprintBuilderBlockEntity be) {
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, be);
    }

    // ------------------------------------------------------------------ persistence and sync
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("BuilderOwner", owner);
        tag.putString("BuilderOwnerName", ownerName);
        tag.putString("BuilderFile", fileName);
        tag.putInt("BuilderFileIndex", fileIndex);
        tag.putInt("BuilderRotation", rotation);
        tag.putInt("BuilderMirror", mirror);
        tag.putIntArray("BuilderOffset", new int[]{offX, offY, offZ});
        tag.putBoolean("BuilderCreate", useCreate);
        tag.putBoolean("BuilderRunning", running);
        tag.putBoolean("BuilderPreview", preview);
        ListTag list = new ListTag();
        for (Agent a : agents) list.add(a.save(registries));
        tag.put("BuilderDrones", list);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("BuilderOwner") ? tag.getUUID("BuilderOwner") : null;
        ownerName = tag.getString("BuilderOwnerName");
        fileName = tag.getString("BuilderFile");
        fileIndex = tag.getInt("BuilderFileIndex");
        rotation = Math.floorMod(tag.getInt("BuilderRotation"), 4);
        mirror = Math.floorMod(tag.getInt("BuilderMirror"), 3);
        int[] off = tag.getIntArray("BuilderOffset");
        if (off.length == 3) { offX = clampOffset(off[0]); offY = clampOffset(off[1]); offZ = clampOffset(off[2]); }
        useCreate = tag.getBoolean("BuilderCreate");
        running = tag.getBoolean("BuilderRunning");
        preview = !tag.contains("BuilderPreview") || tag.getBoolean("BuilderPreview");
        ListTag list = tag.getList("BuilderDrones", Tag.TAG_COMPOUND);
        for (int i = 0; i < DRONES; i++) {
            agents[i] = new Agent();
            if (i < list.size()) agents[i].load(list.getCompound(i), registries);
            if (agents[i].to.equals(Vec3.ZERO)) agents[i].from = agents[i].to = dock(i);
        }
        reloadPlan = useCreate || !fileName.isEmpty();
    }

    /** Clients get the drone legs, the work area, the name, the file list and the missing materials (no inventory). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag drones = new ListTag();
        for (Agent a : agents) {
            CompoundTag t = new CompoundTag();
            t.putInt("Phase", a.phase);
            putVec(t, "From", a.from); putVec(t, "To", a.to);
            t.putLong("LegStart", a.legStart); t.putInt("LegTicks", a.legTicks);
            if (!a.cargo.isEmpty()) t.putString("Cargo", BuiltInRegistries.ITEM.getKey(a.cargo.getItem()).toString());
            if (plan != null && a.phase == Agent.PLACING && a.taskIndex < a.tasks.length) t.putLong("Work", plan.get(a.tasks[a.taskIndex]).pos().asLong());
            drones.add(t);
        }
        tag.put("Drones", drones);
        if (bounds != null) {
            tag.putIntArray("Bounds", new int[]{(int) bounds.minX, (int) bounds.minY, (int) bounds.minZ, (int) bounds.maxX, (int) bounds.maxY, (int) bounds.maxZ});
        }
        tag.putBoolean("Preview", preview);
        tag.putBoolean("Running", running);
        tag.putString("Name", clientName);
        ListTag names = new ListTag();
        for (int i = 0; i < files.size() && i < 64; i++) names.add(StringTag.valueOf(files.get(i)));
        tag.put("Files", names);
        ListTag miss = new ListTag();
        missing.entrySet().stream().sorted((x, y) -> Integer.compare(y.getValue(), x.getValue())).limit(4)
            .forEach(en -> miss.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(en.getKey()) + "=" + en.getValue())));
        tag.put("Missing", miss);
        ListTag uu = new ListTag();
        replicating.entrySet().stream().sorted((x, y) -> Integer.compare(y.getValue(), x.getValue())).limit(4)
            .forEach(en -> uu.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(en.getKey()) + "=" + en.getValue())));
        tag.put("Replicating", uu);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readClient(tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        readClient(packet.getTag());
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- client read-only drone view (renderer)
    public int droneCount() { return DRONES; }
    public boolean droneDocked(int i) { return agents[i].phase == Agent.DOCKED; }
    /** Position at {@code time} (game ticks with partial tick). */
    public Vec3 dronePosition(int i, double time) { return agents[i].position(time); }
    /** Flight direction of the current leg (zero when hovering). */
    public Vec3 droneHeading(int i) { return agents[i].to.subtract(agents[i].from); }
    public String droneCargo(int i) { return clientCargo.get(i); }
    /** Block the drone works on now, or null. */
    @Nullable public BlockPos droneWork(int i) { return clientWork[i] == Long.MIN_VALUE ? null : BlockPos.of(clientWork[i]); }
    /** Ticks until the drone reaches the end of its leg. */
    public double droneArrivalIn(int i, double time) { return agents[i].legStart + agents[i].legTicks - time; }

    /** Client copies for the renderer: drone legs, cargo item and work position. */
    final List<String> clientCargo = new ArrayList<>(java.util.Collections.nCopies(DRONES, ""));
    final long[] clientWork = new long[DRONES];

    private void readClient(CompoundTag tag) {
        ListTag drones = tag.getList("Drones", Tag.TAG_COMPOUND);
        for (int i = 0; i < DRONES && i < drones.size(); i++) {
            CompoundTag t = drones.getCompound(i);
            Agent a = agents[i];
            a.phase = t.getInt("Phase");
            a.from = getVec(t, "From"); a.to = getVec(t, "To");
            a.legStart = t.getLong("LegStart"); a.legTicks = Math.max(1, t.getInt("LegTicks"));
            clientCargo.set(i, t.getString("Cargo"));
            clientWork[i] = t.contains("Work") ? t.getLong("Work") : Long.MIN_VALUE;
        }
        int[] b = tag.getIntArray("Bounds");
        bounds = b.length == 6 ? new AABB(b[0], b[1], b[2], b[3], b[4], b[5]) : null;
        preview = tag.getBoolean("Preview");
        running = tag.getBoolean("Running");
        clientName = tag.getString("Name");
        List<String> f = new ArrayList<>();
        ListTag names = tag.getList("Files", Tag.TAG_STRING);
        for (int i = 0; i < names.size(); i++) f.add(names.getString(i));
        files = List.copyOf(f);
        List<String> m = new ArrayList<>();
        ListTag miss = tag.getList("Missing", Tag.TAG_STRING);
        for (int i = 0; i < miss.size(); i++) m.add(miss.getString(i));
        clientMissing = List.copyOf(m);
        List<String> u = new ArrayList<>();
        ListTag uu = tag.getList("Replicating", Tag.TAG_STRING);
        for (int i = 0; i < uu.size(); i++) u.add(uu.getString(i));
        clientReplicating = List.copyOf(u);
    }
}
