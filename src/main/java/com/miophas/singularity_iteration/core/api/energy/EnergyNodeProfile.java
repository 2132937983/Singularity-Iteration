package com.miophas.singularity_iteration.core.api.energy;

/**
 * <p><b>EN:</b> Immutable network settings of one block. A negative loss or fuse value means "no override".
 * {@code dynamicTier} marks a generator whose packet voltage follows the live source tier, like
 * IC2's {@code TileEntityConversionGenerator}: the engine uses
 * {@code min(currentOffer, getPowerFromTier(getSourceTier()))}, so the melt threshold follows the
 * output of the moment and not a fixed table value.
 * <p><b>ZH:</b> 单个方块的不可变电网设置。损耗或熔断值为负表示“不覆盖”。{@code dynamicTier} 表示发电机的电压包随实时源等级变化，
 * 与 IC2 的 {@code TileEntityConversionGenerator} 相同：引擎使用 {@code min(currentOffer, getPowerFromTier(getSourceTier()))}，
 * 因此熔毁阈值跟随当时的输出，而不是固定表值。
 * <p><b>JA:</b> 1 ブロックの不変の電力網設定。損失・ヒューズ値が負なら「上書きなし」。{@code dynamicTier} は、パケット電圧が現在の
 * ソースティアに追従する発電機を示す（IC2 の {@code TileEntityConversionGenerator} と同じ）。エンジンは
 * {@code min(currentOffer, getPowerFromTier(getSourceTier()))} を使うので、溶断しきい値は固定表ではなくその時の出力に従う。
 */
public record EnergyNodeProfile(Role role, long outputPacket, long conductorLossMilliEu,
        long fuseLimit, boolean standardProcessor, boolean specialCable, boolean measuredBlast,
        boolean dynamicTier) {
    public enum Role { CONSUMER, GENERATOR, STORAGE, CONDUCTOR }

    public EnergyNodeProfile {
        java.util.Objects.requireNonNull(role, "role");
        if (outputPacket < 0 || conductorLossMilliEu < -1 || fuseLimit < -1)
            throw new IllegalArgumentException("Invalid energy node profile");
        if (role == Role.CONDUCTOR && conductorLossMilliEu < 0)
            throw new IllegalArgumentException("Conductor loss is required");
        if (dynamicTier && role != Role.GENERATOR)
            throw new IllegalArgumentException("Only generators may declare a dynamic tier");
    }

    public static EnergyNodeProfile consumer() { return new EnergyNodeProfile(Role.CONSUMER, 0, -1, -1, false, false, false, false); }
    public static EnergyNodeProfile generator(long packet) { return generator(packet, false); }
    /** EN: Generator; dynamicTier = packet follows the live source tier. ZH: 发电机；dynamicTier = 电压包跟随实时源等级。JA: 発電機。dynamicTier = パケットが現在のソースティアに追従。 */
    public static EnergyNodeProfile generator(long packet, boolean dynamicTier) { return new EnergyNodeProfile(Role.GENERATOR, packet, -1, -1, false, false, false, dynamicTier); }
    public static EnergyNodeProfile storage(long packet) { return new EnergyNodeProfile(Role.STORAGE, packet, -1, -1, false, false, false, false); }
}
