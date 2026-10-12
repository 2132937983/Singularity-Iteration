package com.miophas.singularity_iteration.core.api.machine;

/**
 * <p><b>EN:</b> Coarse machine state of the front status lamp, readable from a distance:
 * {@link #OFF} dark (idle, no work, no energy); {@link #RUNNING} green (working or generating);
 * {@link #NO_POWER} blinking amber (work but too little energy; a generator in low-output standby);
 * {@link #BLOCKED} red (output full, input missing, or stopped by redstone). The server evaluates it
 * ({@code AbstractEnergyBlockEntity#machineStatus()}) and syncs only changes. Since 0.1.8.1 the
 * client machine loop sounds also start on {@link #RUNNING}.
 * <p><b>ZH:</b> 正面状态灯显示的粗略机器状态，远处也可看清：{@link #OFF} 熄灭（空闲、无任务、无能量）；{@link #RUNNING} 绿色（工作或发电）；
 * {@link #NO_POWER} 琥珀色闪烁（有任务但能量不足；发电机低输出待机）；{@link #BLOCKED} 红色（输出已满、缺少输入或被红石停止）。
 * 由服务器计算（{@code AbstractEnergyBlockEntity#machineStatus()}），只同步变化。自 0.1.8.1 起，客户端机器循环音效也在 {@link #RUNNING} 时启动。
 * <p><b>JA:</b> 正面のステータスランプが示す大まかな機械状態。遠くからでも読める：{@link #OFF} 消灯（待機、作業なし、電力なし）、
 * {@link #RUNNING} 緑（稼働中・発電中）、{@link #NO_POWER} 琥珀の点滅（作業はあるが電力不足。発電機は低出力待機）、
 * {@link #BLOCKED} 赤（出力満杯、入力不足、レッドストーン停止）。サーバーが評価し（{@code AbstractEnergyBlockEntity#machineStatus()}）、
 * 変化だけを同期する。0.1.8.1 以降、クライアントの機械ループ音も {@link #RUNNING} で始まる。
 */
public enum MachineStatus {
    OFF, RUNNING, NO_POWER, BLOCKED;

    private static final MachineStatus[] VALUES = values();

    public static MachineStatus byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : OFF;
    }
}
