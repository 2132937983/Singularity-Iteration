// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.advancement;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.energy.EnergyProductionListeners;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.80: the clean energy total behind "Clean Energy Ambassador" (2,147,483,647 EU from clean generators).
 * <ul>
 *   <li>Clean generators: the eight solar generators, the Wind Mill, the Water Mill, and the kinetic generators (basic,
 *       turbo, twin turbo) while a wind or water kinetic generator is the block in front of them.</li>
 *   <li>Owner: the player who placed the generator (a {@link #OWNER} attachment, saved with the block entity). A clean
 *       generator from before 0.1.11.80 has none; the first player to right-click it becomes its owner.</li>
 *   <li>Total: every EU a clean generator stores ({@link EnergyProductionListeners}) is added to its owner's total in
 *       {@link Ledger} (world saved data). Crossing {@link #TARGET} grants the advancement to the owner if online, else
 *       at the next login.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class CleanEnergy {
    private CleanEnergy() {}

    /** 2,147,483,647 EU. */
    public static final long TARGET = Integer.MAX_VALUE;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Singularity_Iteration.MOD_ID);
    private static final UUID NO_OWNER = new UUID(0L, 0L);
    /** Block entity attachment: the owner of a clean generator. */
    public static final Supplier<AttachmentType<UUID>> OWNER = ATTACHMENTS.register("clean_generator_owner",
        () -> AttachmentType.builder(() -> NO_OWNER).serialize(UUIDUtil.CODEC).build());

    private static final Set<String> CLEAN = Set.of("solar_generator", "advanced_solar_panel", "hybrid_solar_panel",
        "ultimate_hybrid_solar_panel", "quantum_solar_panel", "mets_advanced_solar_generator", "photon_resonance_solar_generator",
        "ultimate_photon_resonance_solar_generator", "wind_generator", "water_generator");
    private static final Set<String> KINETIC = Set.of("kinetic_generator", "turbo_kinetic_generator", "twin_turbo_kinetic_generator");
    private static final Set<String> CLEAN_KINETIC_SOURCES = Set.of("wind_kinetic_generator", "water_kinetic_generator");
    private static final int NOT_CLEAN = 0, ALWAYS = 1, BY_SOURCE = 2;
    private static final Map<BlockEntityType<?>, Integer> KIND = new ConcurrentHashMap<>();

    /** Registers the attachment and the production listener (mod constructor). */
    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        EnergyProductionListeners.register(CleanEnergy::onProduced);
    }

    private static String path(BlockEntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
        return id != null && Singularity_Iteration.MOD_ID.equals(id.getNamespace()) ? id.getPath() : "";
    }

    private static int kind(BlockEntityType<?> type) {
        return KIND.computeIfAbsent(type, t -> {
            String p = path(t);
            return CLEAN.contains(p) ? ALWAYS : KINETIC.contains(p) ? BY_SOURCE : NOT_CLEAN;
        });
    }

    /** True for a generator that can make clean energy (it gets an owner). */
    public static boolean isCleanGenerator(BlockEntity be) {
        return be != null && kind(be.getType()) != NOT_CLEAN;
    }

    /** True when what this generator makes now is clean (a kinetic generator only with a wind or water source in front). */
    public static boolean producesClean(BlockEntity be) {
        int k = kind(be.getType());
        if (k == ALWAYS) return true;
        if (k != BY_SOURCE || be.getLevel() == null) return false;
        BlockState state = be.getBlockState();
        if (!state.hasProperty(BlockStateProperties.FACING)) return false;
        BlockEntity source = be.getLevel().getBlockEntity(be.getBlockPos().relative(state.getValue(BlockStateProperties.FACING)));
        return source != null && CLEAN_KINETIC_SOURCES.contains(path(source.getType()));
    }

    public static @Nullable UUID owner(BlockEntity be) {
        if (!be.hasData(OWNER)) return null;
        UUID owner = be.getData(OWNER);
        return NO_OWNER.equals(owner) ? null : owner;
    }

    public static void setOwner(BlockEntity be, UUID owner) {
        be.setData(OWNER, owner);
        be.setChanged();
    }

    private static boolean realPlayer(@Nullable Object entity) {
        return entity instanceof ServerPlayer && !(entity instanceof FakePlayer);
    }

    static void onProduced(BlockEntity be, double eu) {
        if (kind(be.getType()) == NOT_CLEAN || !(be.getLevel() instanceof ServerLevel level)) return;
        UUID owner = owner(be);
        if (owner == null || !producesClean(be)) return;
        Ledger.get(level.getServer()).add(level.getServer(), owner, eu);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!realPlayer(event.getEntity())) return;
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (isCleanGenerator(be)) setOwner(be, event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !realPlayer(event.getEntity())) return;
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (isCleanGenerator(be) && owner(be) == null) setOwner(be, event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer server && Ledger.get(server.server).total(server.getUUID()) >= TARGET)
            SiAdvancements.grant(server, "clean_ambassador", "total");
    }

    /** Clean EU per player (overworld saved data). Fractions of an EU are carried in memory. */
    public static final class Ledger extends SavedData {
        public static final String NAME = "mio_icif_clean_energy";
        private final Map<UUID, Long> totals = new HashMap<>();
        private final Map<UUID, Double> carry = new HashMap<>();

        public static Ledger get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Ledger::new, Ledger::load, null), NAME);
        }

        public long total(UUID player) {
            return totals.getOrDefault(player, 0L);
        }

        /** Adds clean EU to a player's total; crossing {@link #TARGET} grants the advancement if the player is online. */
        public void add(MinecraftServer server, UUID player, double eu) {
            if (!(eu > 0)) return;
            double v = eu + carry.getOrDefault(player, 0.0);
            long whole = (long) Math.floor(v);
            carry.put(player, v - whole);
            if (whole <= 0) return;
            long before = total(player);
            long after = before > Long.MAX_VALUE - whole ? Long.MAX_VALUE : before + whole;
            totals.put(player, after);
            setDirty();
            if (before < TARGET && after >= TARGET) {
                ServerPlayer online = server.getPlayerList().getPlayer(player);
                if (online != null) SiAdvancements.grant(online, "clean_ambassador", "total");
            }
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            totals.forEach((player, eu) -> {
                CompoundTag e = new CompoundTag();
                e.putUUID("Player", player);
                e.putLong("EU", eu);
                list.add(e);
            });
            tag.put("Totals", list);
            return tag;
        }

        public static Ledger load(CompoundTag tag, HolderLookup.Provider registries) {
            Ledger ledger = new Ledger();
            for (Tag raw : tag.getList("Totals", Tag.TAG_COMPOUND)) {
                CompoundTag e = (CompoundTag) raw;
                if (e.hasUUID("Player")) ledger.totals.put(e.getUUID("Player"), Math.max(0, e.getLong("EU")));
            }
            return ledger;
        }
    }
}
