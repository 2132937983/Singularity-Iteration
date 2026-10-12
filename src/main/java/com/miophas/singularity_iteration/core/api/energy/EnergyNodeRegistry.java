package com.miophas.singularity_iteration.core.api.energy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <p><b>EN:</b> Registry of energy node profiles (the role of a block in the EU network: consumer,
 * generator, storage or conductor). Register during mod setup, before the first server starts;
 * the registry freezes at server start. No namespace is reserved. Reads use immutable
 * snapshots, so lookups from the network thread need no lock.
 * <p><b>ZH:</b> 能源节点配置注册表（方块在 EU 电网中的角色：用电器、发电机、储能或导体）。请在模组初始化期间、首个服务器启动前注册；
 * 服务器启动时注册表冻结。不保留任何命名空间。读取使用不可变快照，因此电网线程查询无需加锁。
 * <p><b>JA:</b> エネルギーノード設定のレジストリ（EU 電力網でのブロックの役割：消費機器、発電機、蓄電、導体）。MOD の初期化中、
 * 最初のサーバー起動前に登録する。サーバー起動時に凍結される。名前空間の予約はない。読み取りは不変スナップショットを使うので、
 * 電力網スレッドからの参照にロックは不要。
 */
public final class EnergyNodeRegistry {
    private static final Map<ResourceLocation, EnergyNodeProfile> PROFILES = new LinkedHashMap<>();
    private static final List<Predicate<BlockState>> ADMISSION = new ArrayList<>();
    private static volatile Map<ResourceLocation, EnergyNodeProfile> snapshot = Map.of();
    private static volatile Map<ResourceLocation, Long> conductors = Map.of();
    private static volatile List<Predicate<BlockState>> admission = List.of();
    private static boolean frozen;

    private EnergyNodeRegistry() {}

    /**
     * <p><b>EN:</b> Registers the profile of a block. A second profile for the same block throws
     * {@link IllegalArgumentException}; a call after the freeze throws {@link IllegalStateException}.
     * <p><b>ZH:</b> 注册方块的配置。同一方块的第二个配置会抛出 {@link IllegalArgumentException}；冻结后调用会抛出 {@link IllegalStateException}。
     * <p><b>JA:</b> ブロックの設定を登録する。同じブロックへの 2 つ目は {@link IllegalArgumentException}、凍結後の呼び出しは
     * {@link IllegalStateException} を投げる。
     */
    public static synchronized void register(ResourceLocation blockId, EnergyNodeProfile profile) {
        if (frozen) throw new IllegalStateException("Energy profiles are frozen after server startup");
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(profile, "profile");
        if (PROFILES.putIfAbsent(blockId, profile) != null)
            throw new IllegalArgumentException("Duplicate energy profile: " + blockId);
        snapshot = Map.copyOf(PROFILES);
        var losses = new LinkedHashMap<ResourceLocation, Long>();
        PROFILES.forEach((id, value) -> { if (value.conductorLossMilliEu() >= 0) losses.put(id, value.conductorLossMilliEu()); });
        conductors = Map.copyOf(losses);
    }

    /**
     * <p><b>EN:</b> Migration policy of the host for existing content. A new addon must register explicit profiles instead.
     * <p><b>ZH:</b> 宿主为现有内容提供的迁移策略。新附属必须改为注册明确的配置。
     * <p><b>JA:</b> 既存コンテンツ向けのホストの移行ポリシー。新しいアドオンは明示的な設定を登録すること。
     */
    public static synchronized void registerAdmissionPolicy(Predicate<BlockState> policy) {
        if (frozen) throw new IllegalStateException("Energy profiles are frozen after server startup");
        ADMISSION.add(Objects.requireNonNull(policy));
        admission = List.copyOf(ADMISSION);
    }

    /** EN: Freezes the registry (server start). ZH: 冻结注册表（服务器启动时）。JA: レジストリを凍結する（サーバー起動時）。 */
    public static synchronized void freeze() { frozen = true; }
    /**
     * <p><b>EN:</b> True when the block state can join the EU network: it has a block entity and a profile or an admission policy accepts it.
     * <p><b>ZH:</b> 方块状态可加入 EU 电网时为真：它有方块实体，并且有配置或被准入策略接受。
     * <p><b>JA:</b> ブロックステートが EU 電力網に参加できるなら真：ブロックエンティティがあり、設定があるか、参加ポリシーが受け入れる。
     */
    public static boolean admits(BlockState state) {
        if (!state.hasBlockEntity()) return false;
        if (snapshot.containsKey(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) return true;
        for (var policy : admission) if (policy.test(state)) return true;
        return false;
    }

    public static EnergyNodeProfile profile(ResourceLocation id) { return snapshot.get(id); }
    public static Map<ResourceLocation, Long> conductors() { return conductors; }
    public static Long conductorLoss(ResourceLocation id) { return conductors.get(id); }
    public static boolean isGenerator(ResourceLocation id) { var p = profile(id); return p != null && p.role() == EnergyNodeProfile.Role.GENERATOR; }
    /**
     * <p><b>EN:</b> True when the profile role is storage. The fair-share distributor charges storage after loads.
     * <p><b>ZH:</b> 配置角色为储能时为真。公平分配器在用电器之后为储能充电。
     * <p><b>JA:</b> 設定の役割が蓄電なら真。公平分配器は消費機器の後に蓄電へ充電する。
     */
    public static boolean isStorage(ResourceLocation id) { var p = profile(id); return p != null && p.role() == EnergyNodeProfile.Role.STORAGE; }
    public static boolean standardProcessor(ResourceLocation id) { var p = profile(id); return p != null && p.standardProcessor(); }
    public static boolean specialCable(ResourceLocation id) { var p = profile(id); return p != null && p.specialCable(); }
    public static boolean measuredBlast(ResourceLocation id) { var p = profile(id); return p != null && p.measuredBlast(); }
    public static Long fuseLimit(ResourceLocation id) { var p = profile(id); return p != null && p.fuseLimit() >= 0 ? p.fuseLimit() : null; }
    public static Long storagePacket(ResourceLocation id) { var p = profile(id); return p != null && p.role() == EnergyNodeProfile.Role.STORAGE && p.outputPacket() > 0 ? p.outputPacket() : null; }
    public static long generatorPacket(ResourceLocation id, long fallback) { var p = profile(id); return p != null && p.role() == EnergyNodeProfile.Role.GENERATOR && p.outputPacket() > 0 ? p.outputPacket() : fallback; }
    /**
     * <p><b>EN:</b> True when the packet of the generator follows its live source tier (computed again at each refresh).
     * <p><b>ZH:</b> 发电机的电压包随其实时源等级变化（每次刷新重新计算）时为真。
     * <p><b>JA:</b> 発電機のパケットが現在のソースティアに追従する（更新ごとに再計算する）なら真。
     */
    public static boolean dynamicTier(ResourceLocation id) { var p = profile(id); return p != null && p.dynamicTier(); }
    public static long storagePacket(ResourceLocation id, long fallback) { Long packet = storagePacket(id); return packet == null ? fallback : packet; }
}
