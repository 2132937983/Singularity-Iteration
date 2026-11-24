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
import net.minecraft.resources.ResourceLocation;
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
 *   <li>0.1.11.182: or a blueprint the player uploaded from their own computer's {@code schematics} folder
 *       ({@link BlueprintUploads}, the Upload button of the screen), placed as a file is. On a dedicated server the
 *       folder above is the server's, so this is how a player builds their own schematics there.</li>
 *   <li>Every block of the placement must be within {@link #RANGE} blocks of the builder. 0.1.11.186: no longer: a plan
 *       of any size builds (the offset up to {@link #OFFSET_LIMIT}); a block in a chunk that is not loaded waits for it.</li>
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
    /**
     * 0.1.11.189: the check after the last block waits {@link #CHECK_DELAY} ticks, reads {@link #CHECK_PER_TICK} positions a
     * tick and runs at most {@link #CHECK_PASSES} times a start (a block that can never stay must not keep the drones busy).
     */
    public static final int CHECK_DELAY = 40, CHECK_PER_TICK = 16384, CHECK_PASSES = 3;
    /** 0.1.11.186: the largest offset of a file's blueprint from the builder, each axis (the range limit went). */
    public static final int OFFSET_LIMIT = 1024;
    public static final double SPEED = 0.8;
    public static final int DATA_COUNT = 23;
    public static final int NO_BLUEPRINT = 0, READY = 1, RUNNING = 2, PAUSED = 3, DONE = 4, TOO_FAR = 5, READ_ERROR = 6;
    /** 0.1.11.187: the blueprint is too large for the server's free memory (there is no fixed block limit any more). */
    public static final int TOO_LARGE = 7;

    private static final SlotLayout LAYOUT = SlotLayout.builder().battery().extra(1).build();

    // ---- settings (saved)
    @Nullable private UUID owner;
    private String ownerName = "";
    private String fileName = "";
    /** 0.1.11.182: the uploaded blueprint in use ({@code "<uuid>/<name>"}, {@link BlueprintUploads#resolve}), or empty. */
    private String upload = "";
    /** 0.1.11.188: the multiblock structure in use (its id in {@link BuilderStructures}), or empty. */
    private String structure = "";
    private int fileIndex;
    private int rotation, mirror;
    private int offX = 1, offY = 0, offZ = 1;
    private boolean useCreate, running, preview = true;
    private int status = NO_BLUEPRINT;
    // ---- plan (rebuilt from the settings after a load)
    @Nullable private List<Blueprint.Entry> plan;
    private boolean[] done = new boolean[0], claimed = new boolean[0];
    private int cursor, placed, obstructed, doneCount;
    /**
     * 0.1.11.189: the check after the last block: from when (game time) it may start (blocks that fall for want of support
     * have fallen by then), where it is in the plan (-1: not checking), the passes made and the blocks it found missing.
     */
    private long checkFrom = -1;
    private int checkAt = -1, checkPasses, checkFound, cleared;
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
                case 18 -> useCreate ? 1 : !structure.isEmpty() ? 4 : !upload.isEmpty() ? 3 : fileName.isEmpty() ? 0 : 2;
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

    /**
     * 0.1.11.3: a change of rotation, mirror or offset rebuilds the plan after a short pause (one read for a burst of
     * clicks), a file list refresh runs at most once a second, and a load at most every half second. Each button packet
     * used to read the blueprint (up to 16 MB of NBT) or list the folder on the server thread at once.
     */
    static final int REBUILD_DELAY = 2, REFRESH_GAP = 20, LOAD_GAP = 10;   // 0.1.11.185: 2 ticks (the blueprint is kept, a rebuild only places it)
    private long rebuildAt = -1, lastRefresh = Long.MIN_VALUE / 2, lastLoad = Long.MIN_VALUE / 2;

    private void scheduleRebuild() {
        if (level != null) rebuildAt = level.getGameTime() + REBUILD_DELAY;
    }

    /** Runs a scheduled rebuild that is due (or any scheduled one when {@code now} is true). */
    private void runScheduledRebuild(boolean now) {
        if (rebuildAt < 0 || level == null || (!now && level.getGameTime() < rebuildAt)) return;
        rebuildAt = -1;
        rebuild();
        markSync();   // 0.1.11.185: the new box and plan version to the clients at once (they waited for the next sync)
    }

    private void refreshFilesLimited() {
        if (level == null || level.getGameTime() - lastRefresh < REFRESH_GAP) return;
        lastRefresh = level.getGameTime();
        refreshFiles();
    }

    public void action(Player player, int action, int index) {
        if (!(level instanceof ServerLevel)) return;
        switch (action) {
            case A_REFRESH -> refreshFilesLimited();
            case A_FILE_PREV, A_FILE_NEXT -> {
                if (files.isEmpty()) refreshFilesLimited();
                if (!files.isEmpty()) fileIndex = Math.floorMod(fileIndex + (action == A_FILE_NEXT ? 1 : -1), files.size());
            }
            case A_LOAD -> {
                if (!running && level.getGameTime() - lastLoad >= LOAD_GAP) {
                    lastLoad = level.getGameTime();
                    rebuildAt = -1;
                    load(player);
                }
            }
            case A_ROTATE -> { if (!running && !useCreate) { rotation = (rotation + 1) % 4; scheduleRebuild(); } }
            case A_MIRROR -> { if (!running && !useCreate) { mirror = (mirror + 1) % 3; scheduleRebuild(); } }
            case A_OFFSET -> {
                if (running || useCreate) break;
                int step = (index & 1) == 0 ? -1 : 1;
                switch (index >> 1) {
                    case 0 -> offX = clampOffset(offX + step);
                    case 1 -> offY = clampOffset(offY + step);
                    default -> offZ = clampOffset(offZ + step);
                }
                scheduleRebuild();
            }
            case A_START -> {
                runScheduledRebuild(true);   // a start right after a change builds the new plan first
                if (plan == null || status == TOO_FAR) break;
                running = !running;
                status = running ? RUNNING : PAUSED;
                missingUntil.clear();
                if (running) { checkPasses = 0; checkAt = -1; checkFrom = -1; }   // 0.1.11.189: a start checks the whole build again
            }
            case A_PREVIEW -> preview = !preview;
            case A_STOP -> stop();
            default -> { }
        }
        setChanged();
        markSync();
    }

    private static int clampOffset(int v) { return Math.max(-OFFSET_LIMIT, Math.min(OFFSET_LIMIT, v)); }

    private void refreshFiles() {
        files = BlueprintReader.list();
        if (fileIndex >= files.size()) fileIndex = 0;
    }

    private void load(Player player) {
        stop();
        ItemStack slot = getItemHandler().getStackInSlot(SCHEMATIC_SLOT);
        useCreate = BlueprintReader.isCreateSchematic(slot);
        upload = "";   // 0.1.11.182: the load button takes the slot or the server's list
        structure = "";   // 0.1.11.188: and leaves a multiblock structure
        keptKey = null;   // 0.1.11.185: the load button reads the file again (it may have changed)
        if (!useCreate) {
            if (files.isEmpty()) refreshFiles();
            fileName = files.isEmpty() ? "" : files.get(Math.min(fileIndex, files.size() - 1));
        } else {
            fileName = "";
        }
        rebuild();
        readMessage(player);
    }

    /** The line over the hotbar after a read that failed (0.1.11.187: or found the blueprint too large). */
    private void readMessage(Player player) {
        if (status == READ_ERROR) player.displayClientMessage(Component.translatable("message.mio_icif.builder.read_error"), true);
        if (status == TOO_LARGE) player.displayClientMessage(Component.translatable("message.mio_icif.builder.too_large"), true);
    }

    /**
     * Reads the blueprint of the current settings and builds the plan. 0.1.11.187: a blueprint the free memory cannot hold
     * (with its plan) is {@link #TOO_LARGE}; nothing of it stays in memory.
     */
    private void rebuild() {
        try {
            build();
        } catch (Blueprint.TooLarge e) {
            plan = null;
            bounds = null;
            kept = null;
            keptKey = null;
            status = TOO_LARGE;
            clientName = !upload.isEmpty() ? upload.substring(upload.indexOf('/') + 1) : fileName;
        }
    }

    private void build() {
        if (!(level instanceof ServerLevel world)) return;
        planVersion++;   // 0.1.11.184: the clients' block holograms ask for the new plan (BuilderPlanPackets)
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
            boolean uploaded = !upload.isEmpty(), preset = !structure.isEmpty();
            if (!uploaded && !preset && fileName.isEmpty()) return;
            String key = preset ? "s:" + structure : uploaded ? "u:" + upload : "f:" + fileName;
            ResourceLocation structureId = preset ? ResourceLocation.tryParse(structure) : null;
            Blueprint bp = key.equals(keptKey) ? kept : preset ? (structureId == null ? null : BuilderStructures.blueprint(structureId))
                : uploaded ? BlueprintUploads.read(upload, blocks) : BlueprintReader.readFile(fileName, blocks);
            kept = bp;
            keptKey = bp != null ? key : null;
            if (bp == null) { status = READ_ERROR; clientName = preset ? structure : uploaded ? upload.substring(upload.indexOf('/') + 1) : fileName; return; }
            placed = bp.place(worldPosition.offset(offX, offY, offZ), Rotation.values()[rotation], Mirror.values()[mirror]);
            planOffset = new int[]{offX, offY, offZ};
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
        for (Blueprint.Entry e : plan) {
            BlockPos p = e.pos();
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        if (!plan.isEmpty()) bounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        status = plan.isEmpty() ? DONE : READY;   // 0.1.11.186: no "too far" any more (TOO_FAR stays for old saves)
    }

    /**
     * 0.1.11.182: uses the blueprint {@code player} uploaded ({@code ref} is {@code "<uuid>/<name>"}) with the placement
     * settings of the screen; false (nothing changed) while the builder runs. The read error shows as for a file.
     */
    boolean selectUpload(Player player, String ref) {
        return selectUpload(player, ref, null);
    }

    /** Overload accepting an already-parsed blueprint (avoids double-parsing on upload finish). */
    boolean selectUpload(Player player, String ref, @Nullable Blueprint preloaded) {
        if (running || !(level instanceof ServerLevel)) return false;
        if (preloaded != null) {
            kept = preloaded;
            keptKey = "u:" + ref;
        } else {
            keptKey = null;   // read it again
        }
        stop();
        useCreate = false;
        structure = "";
        upload = ref;
        rebuildAt = -1;
        rebuild();
        readMessage(player);
        setChanged();
        markSync();
        return status != READ_ERROR && status != TOO_LARGE;
    }

    /**
     * 0.1.11.188: builds the multiblock structure {@code id} ({@link BuilderStructures}: the build plans of JEI's "Multiblock
     * Structures") with the placement settings of the screen; false (nothing changed) while the builder runs or for an id
     * that names no structure.
     */
    boolean selectStructure(Player player, ResourceLocation id) {
        if (running || !(level instanceof ServerLevel)
            || com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprintRegistry.get(id) == null) return false;
        keptKey = null;
        stop();
        useCreate = false;
        upload = "";
        structure = id.toString();
        rebuildAt = -1;
        rebuild();
        readMessage(player);
        setChanged();
        markSync();
        return status != READ_ERROR && status != TOO_LARGE;
    }

    /** 0.1.11.188: the multiblock structure in use, or empty. */
    public String structure() { return structure; }

    /**
     * 0.1.11.184: the plan's version, raised by each rebuild and sent in the update tag: a client whose block hologram holds
     * another version asks for the plan again ({@link BuilderPlanPackets}).
     */
    private int planVersion = java.util.concurrent.ThreadLocalRandom.current().nextInt(1 << 24);   // a new builder at the same place is another plan

    public int planVersion() { return planVersion; }

    /** 0.1.11.185: the blueprint read last, kept for the rebuilds of a move or turn (its key: "f:" file or "u:" upload). */
    @Nullable private Blueprint kept;
    @Nullable private String keptKey;
    /** 0.1.11.185: the offset the plan was placed with. */
    private int[] planOffset = {1, 0, 1};
    private boolean clientCreate;

    /** 0.1.11.185: the offset the plan (and its box) was placed with (client: from the update tag). */
    public int[] planOffset() { return planOffset; }

    /** 0.1.11.185: the rotation (quarter turns) and mirror (0 none, 1 left-right, 2 front-back) set (client: from the update tag). */
    public int rotationSteps() { return rotation; }

    public int mirrorMode() { return mirror; }

    /** 0.1.11.185: whether the plan is a Create schematic (placed by its own anchor; the offset does not move it). */
    public boolean placedByCreate() { return level != null && level.isClientSide ? clientCreate : useCreate; }

    /** 0.1.11.184: the plan (world positions and target states), or null; for the plan packets (server side). */
    @Nullable public List<Blueprint.Entry> plan() { return plan; }

    /** 0.1.11.182: the uploaded blueprint in use ({@code "<uuid>/<name>"}), or empty (the tests). */
    public String upload() { return upload; }

    /** 0.1.11.182: the state of the builder for the tests: {@link #status} codes. */
    public int status() { return status; }

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
            if (cursor >= done.length && !busy && !check(world, now)) {
                running = false;
                status = DONE;
                checkFrom = -1;
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

    /**
     * 0.1.11.189: the check after the last block (a scaffold placed before its support falls a tick later, a block may have been
     * broken meanwhile): {@link #CHECK_DELAY} ticks after the last block, the plan is read again, {@link #CHECK_PER_TICK}
     * positions a tick; a position that does not hold its block and can take it (free, or a block the drones may clear) goes
     * back to the plan, and the drones build it. At most {@link #CHECK_PASSES} passes a start. True while checking or when the
     * check gave work back (the build goes on); false when it is done.
     */
    private boolean check(ServerLevel world, long now) {
        if (plan == null || checkPasses >= CHECK_PASSES) return false;
        if (checkFrom < 0) { checkFrom = now + CHECK_DELAY; return true; }
        if (now < checkFrom) return true;
        if (checkAt < 0) { checkAt = 0; checkFound = 0; }
        int end = Math.min(plan.size(), checkAt + CHECK_PER_TICK);
        long deadline = System.nanoTime() + 500_000L; // <= 0.5 ms per tick budget
        int t = checkAt;
        for (; t < end; t++) {
            if ((t & 63) == 0 && t > checkAt && System.nanoTime() >= deadline) break;
            Blueprint.Entry e = plan.get(t);
            if (!done[t] || !world.isLoaded(e.pos())) continue;
            BlockState here = world.getBlockState(e.pos()), target = target(e.state());
            if (built(here, target) || !here.canBeReplaced() && !clearable(world, e.pos(), here)) continue;
            done[t] = false;
            claimed[t] = false;
            doneCount--;
            checkFound++;
            cursor = Math.min(cursor, t);
        }
        checkAt = t;
        if (checkAt < plan.size()) return true;
        checkPasses++;
        checkAt = -1;
        checkFrom = -1;
        if (checkFound > 0) markSync();
        return checkFound > 0;
    }

    /** 0.1.11.189: whether the position holds the plan's block (its running or lit states, its joins aside). */
    static boolean built(BlockState here, BlockState target) {
        return here.equals(target) || target(here).equals(target) || here.getBlock() == target.getBlock();
    }

    private static boolean clearing() {
        return com.miophas.singularity_iteration.common.Singularity_Iteration_Config.BUILDER_CLEAR_OBSTRUCTIONS.get();
    }

    /**
     * 0.1.11.189: whether the drones may take the block {@code here} away to put the plan's block in its place: the option is on,
     * the block can be broken (not bedrock and the like) and holds no block entity (a machine, a chest: those stay, as before).
     * The claim and protection mods are asked when the drone gets there ({@link #mayBreak}).
     */
    private boolean clearable(ServerLevel world, BlockPos pos, BlockState here) {
        return clearing() && !here.hasBlockEntity() && here.getDestroySpeed(world, pos) >= 0;
    }

    /** Claim and protection checks for taking a block away, with the owner identity. */
    private boolean mayBreak(ServerLevel world, BlockPos pos, BlockState here) {
        var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(world, pos, here, placer(world));
        return !net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    /**
     * 0.1.11.189: takes the block in the way away, as mined with a pickaxe: its drops go to the nearest Armory Material
     * Interface (what does not fit falls there; an infinite interface's drones keep nothing), with the breaking sound and dust.
     */
    private void clear(ServerLevel world, BlockPos pos, BlockState here) {
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(here, world, pos, null, placer(world), new ItemStack(Items.NETHERITE_PICKAXE));
        world.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(here));
        boolean infinite = false;
        for (ItemStack drop : drops) {
            ItemStack rest = drop;
            for (BlockPos p : interfaces) {
                if (rest.isEmpty()) break;
                net.minecraft.world.level.block.entity.BlockEntity be = world.getBlockEntity(p);
                if (be instanceof MaterialInterfaceBlockEntity mi) rest = mi.insert(rest);
                else if (be instanceof InfiniteMaterialInterfaceBlockEntity) infinite = true;
            }
            if (!rest.isEmpty() && !infinite) net.minecraft.world.level.block.Block.popResource(world, pos, rest);
        }
        cleared++;
    }

    private void arrive(ServerLevel world, int i, Agent a, long now) {
        switch (a.phase) {
            case Agent.TO_SOURCE -> fetch(world, i, a);
            case Agent.PLACING -> placeNext(world, i, a, now);
            case Agent.RETURNING -> {
                if (!a.cargo.isEmpty()) {
                    ItemStack rest = a.source != null && world.getBlockEntity(a.source) instanceof MaterialSource mi ? mi.insert(a.cargo) : a.cargo;
                    if (!rest.isEmpty() && !interfaces.isEmpty() && world.getBlockEntity(interfaces.get(0)) instanceof MaterialSource near) rest = near.insert(rest);
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
            if (built(here, target)) { markDone(t); continue; }
            if (!here.canBeReplaced() && !clearable(world, e.pos(), here)) { markDone(t); obstructed++; continue; }   // 0.1.11.189: else cleared
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
        ItemStack got = sourceEntity instanceof MaterialSource mi ? mi.extract(item, need)   // 0.1.11.187: or the infinite one
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
        if (!world.isLoaded(e.pos())) {
            // 0.1.11.3: the chunk unloaded after the claim; the block waits for it (reading it here loaded the chunk)
            claimed[t] = false;
            a.taskIndex++;
            return;
        }
        BlockState target = target(e.state());
        BlockState here = world.getBlockState(e.pos());
        boolean finished = true;
        boolean inTheWay = !here.canBeReplaced();
        if (built(here, target)) {
            // somebody else built it (0.1.11.188: its running, lit or similar states may differ)
        } else if (inTheWay && !clearable(world, e.pos(), here)) {
            obstructed++;
        } else {
            Item m = material(target);
            int cost = m == null || m == Items.AIR ? 0 : amount(target);
            if (cost > 0 && (!a.cargo.is(m) || a.cargo.getCount() < cost)) {
                // no cargo for this one: give it back to the plan
                claimed[t] = false;
                finished = false;
            } else if (apiGetStoredEnergy() < EU_PER_BLOCK * (inTheWay ? 2 : 1)) {
                a.from = a.to; a.legStart = now; a.legTicks = 10;               // hover in place, wait for power
                return;
            } else if (mayPlace(world, e.pos(), target) && (!inTheWay || mayBreak(world, e.pos(), here))) {
                if (inTheWay) {   // 0.1.11.189: the block in the way goes first (its drops to the interface)
                    apiConsumeEnergy(EU_PER_BLOCK);
                    clear(world, e.pos(), here);
                }
                apiConsumeEnergy(EU_PER_BLOCK);
                if (cost > 0) a.cargo.shrink(cost);
                world.setBlock(e.pos(), target, flags(target));
                BuiltBlocks.afterPlace(world, e.pos(), target, placer(world));   // 0.1.11.188: ports, owner, extra parts
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
                        if (be instanceof MaterialSource && be.getBlockPos().closerThan(worldPosition, r + 0.5)) found.add(be.getBlockPos());
                    }
                }
            }
            found.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(worldPosition)));
            interfaces = List.copyOf(found);
            replicators = supplyEnabled() ? scanReplicators(world) : List.of();
        }
        for (BlockPos p : interfaces) {
            if (world.getBlockEntity(p) instanceof MaterialSource mi && mi.hasAny(item)) return p;
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
        int scanEnd = Math.min(plan.size(), from + 4096);
        for (int t = from; t < scanEnd && rest < 64; t++) {
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

    /** The builder's owner as the player who places the blocks (claims, protection, 0.1.11.188: the placed block's owner). */
    private net.neoforged.neoforge.common.util.FakePlayer placer(ServerLevel world) {
        GameProfile profile = new GameProfile(owner != null ? owner : UUID.nameUUIDFromBytes("mio_icif_builder".getBytes()),
            ownerName.isEmpty() ? "[Blueprint Builder]" : ownerName);
        return net.neoforged.neoforge.common.util.FakePlayerFactory.get(world, profile);
    }

    /** Claim and protection checks with the owner identity. */
    private boolean mayPlace(ServerLevel world, BlockPos pos, BlockState state) {
        var fake = placer(world);
        if (world.getServer().isUnderSpawnProtection(world, pos, fake)) return false;
        var snapshot = net.neoforged.neoforge.common.util.BlockSnapshot.create(world.dimension(), world, pos);
        var event = new net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent(snapshot, world.getBlockState(pos.below()), fake);
        return !net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    // ------------------------------------------------------------------ material rules
    /**
     * The state to place: never a water source (waterlogged blocks are placed dry), and 0.1.11.3: never a state that
     * holds more than its item. A full composter, a beehive with honey, a charged respawn anchor, a filled cauldron,
     * a potted plant, a ripe crop or a bush with berries cost one plain item but gave bone meal, honey, a bucket of
     * lava, the plant or the harvest when broken; they are placed empty (or unripe).
     */
    private static final java.util.concurrent.ConcurrentHashMap<BlockState, BlockState> TARGET_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    static BlockState target(BlockState s) {
        return TARGET_CACHE.computeIfAbsent(s, BlueprintBuilderBlockEntity::computeTarget);
    }

    private static BlockState computeTarget(BlockState s) {
        if (s.getBlock() instanceof net.minecraft.world.level.block.AbstractCauldronBlock && !s.is(net.minecraft.world.level.block.Blocks.CAULDRON))
            return net.minecraft.world.level.block.Blocks.CAULDRON.defaultBlockState();
        if (s.getBlock() instanceof net.minecraft.world.level.block.FlowerPotBlock && !s.is(net.minecraft.world.level.block.Blocks.FLOWER_POT))
            return net.minecraft.world.level.block.Blocks.FLOWER_POT.defaultBlockState();
        if (s.hasProperty(BlockStateProperties.WATERLOGGED)) s = s.setValue(BlockStateProperties.WATERLOGGED, false);
        if (s.hasProperty(BlockStateProperties.LEVEL_COMPOSTER)) s = s.setValue(BlockStateProperties.LEVEL_COMPOSTER, 0);
        if (s.hasProperty(BlockStateProperties.LEVEL_HONEY)) s = s.setValue(BlockStateProperties.LEVEL_HONEY, 0);
        if (s.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)) s = s.setValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES, 0);
        if (s.hasProperty(BlockStateProperties.BERRIES)) s = s.setValue(BlockStateProperties.BERRIES, false);
        for (net.minecraft.world.level.block.state.properties.Property<?> p : s.getProperties()) {
            if (p instanceof net.minecraft.world.level.block.state.properties.IntegerProperty age && "age".equals(age.getName()))
                s = s.setValue(age, java.util.Collections.min(age.getPossibleValues()));
        }
        if (Blueprint.ours(s)) {   // 0.1.11.188: a state the blueprint caught at work is placed as the block's default (the block sets it)
            BlockState plain = s.getBlock().defaultBlockState();
            for (net.minecraft.world.level.block.state.properties.Property<?> p : s.getProperties())
                if (RUNTIME.contains(p.getName())) s = copy(s, plain, p);
        }
        return s;
    }

    /**
     * 0.1.11.188: the states of this mod's blocks that show what the block is doing (running, lit, formed, charging, a
     * redstone signal...), not how it was built. A blueprint saved while a machine ran had it running for ever when nothing
     * set it back (a machine changes the state only when its work starts or stops); these go back to the block's default.
     */
    static final java.util.Set<String> RUNTIME = java.util.Set.of("lit", "active", "working", "spinning", "charging", "discharging",
        "scanning", "replicating", "formed", "complete", "has_fluid", "powered");

    private static <T extends Comparable<T>> BlockState copy(BlockState to, BlockState from, net.minecraft.world.level.block.state.properties.Property<T> p) {
        return to.setValue(p, from.getValue(p));
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

    /** Items one block costs: 0.1.11.3 counts the candles, pickles, eggs, petals and snow layers it holds (it was always one). */
    static int amount(BlockState s) {
        if (s.hasProperty(BlockStateProperties.SLAB_TYPE)) return s.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE ? 2 : 1;
        if (s.hasProperty(BlockStateProperties.CANDLES)) return s.getValue(BlockStateProperties.CANDLES);
        if (s.hasProperty(BlockStateProperties.PICKLES)) return s.getValue(BlockStateProperties.PICKLES);
        if (s.hasProperty(BlockStateProperties.EGGS)) return s.getValue(BlockStateProperties.EGGS);
        if (s.hasProperty(BlockStateProperties.FLOWER_AMOUNT)) return s.getValue(BlockStateProperties.FLOWER_AMOUNT);
        if (s.getBlock() instanceof net.minecraft.world.level.block.SnowLayerBlock) return s.getValue(BlockStateProperties.LAYERS);
        return 1;
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
        if (!level.isClientSide) be.runScheduledRebuild(false);   // 0.1.11.3
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, be);
    }

    // ------------------------------------------------------------------ persistence and sync
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("BuilderOwner", owner);
        tag.putString("BuilderOwnerName", ownerName);
        tag.putString("BuilderFile", fileName);
        tag.putString("BuilderUpload", upload);
        tag.putString("BuilderStructure", structure);   // 0.1.11.188
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
        upload = BlueprintUploads.resolve(tag.getString("BuilderUpload")) != null ? tag.getString("BuilderUpload") : "";
        ResourceLocation saved = ResourceLocation.tryParse(tag.getString("BuilderStructure"));
        structure = saved != null && com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprintRegistry.get(saved) != null ? saved.toString() : "";
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
        reloadPlan = useCreate || !fileName.isEmpty() || !upload.isEmpty() || !structure.isEmpty();
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
        tag.putInt("PlanVersion", planVersion);   // 0.1.11.184
        tag.putIntArray("PlanOffset", planOffset);   // 0.1.11.185: the in-world placing's prediction
        tag.putBoolean("Create", useCreate);
        tag.putInt("Rot", rotation);
        tag.putInt("Mir", mirror);
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
        planVersion = tag.getInt("PlanVersion");   // 0.1.11.184
        int[] po = tag.getIntArray("PlanOffset");
        if (po.length == 3) planOffset = po;
        clientCreate = tag.getBoolean("Create");
        rotation = Math.floorMod(tag.getInt("Rot"), 4);
        mirror = Math.floorMod(tag.getInt("Mir"), 3);
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
