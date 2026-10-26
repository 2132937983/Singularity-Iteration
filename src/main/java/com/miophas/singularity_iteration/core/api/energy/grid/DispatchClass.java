// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.grid;

/**
 * <p><b>EN:</b> Dispatch class of a generator for the merit order of the grid (public API since 0.1.10.22). In each
 * round the grid takes energy first from must-run sources (their energy is lost when nobody takes it), then from
 * dispatchable sources. Their fuel stays in them when nobody takes their energy. Storage blocks and transformers have
 * their own places in the order; they do not use this class. The server config {@code EnergyNet.dispatchMode} chooses the
 * order. A generator declares its class with {@link IGridDispatch}; a generator without it is must-run (the
 * behaviour before 0.1.10.22).
 * <p><b>ZH:</b> 发电机在电网经济次序中的调度类别（0.1.10.22 起的公共 API）。每一轮电网先从必须运行的电源取电（无人取用时其能量损失），
 * 再从可调度电源取电（无人取用时燃料留在其中）。储能与变压器在次序中有各自的位置，不使用本类别。服务器配置 {@code EnergyNet.dispatchMode}
 * 选择次序。发电机用 {@link IGridDispatch} 声明类别；未声明的发电机为必须运行（0.1.10.22 之前的行为）。
 * <p><b>JA:</b> 電力網のメリットオーダーにおける発電機の給電種別（0.1.10.22 以降の公開 API）。各ラウンドで電力網は、まず必ず使う電源
 * （誰も取らないとエネルギーが失われる）から取り、次に調整できる電源（誰も取らないと燃料が残る）から取る。蓄電と変圧器は順序の中に
 * それぞれの位置があり、この種別を使わない。順序はサーバー設定 {@code EnergyNet.dispatchMode} で選ぶ。発電機は {@link IGridDispatch} で種別を
 * 申告する。申告のない発電機は必ず使う電源になる（0.1.10.22 より前の動作）。
 */
public enum DispatchClass {
    /**
     * <p><b>EN:</b> Must-run: solar, wind, water, kinetic, geomagnetic, radioisotope and nuclear generators. Their
     * output is lost when the grid does not take it.
     * <p><b>ZH:</b> 必须运行：太阳能、风力、水力、动能、地磁、放射性同位素与核能发电机。电网不取用时其输出损失。
     * <p><b>JA:</b> 必ず使う電源：太陽光・風力・水力・運動・地磁気・放射性同位体・原子力の発電機。電力網が取らないと出力は失われる。
     */
    MUST_RUN,
    /**
     * <p><b>EN:</b> Dispatchable: generators that burn fuel only while their energy is taken (fuel, lava, semifluid,
     * diesel, heat, experience and drop generators).
     * <p><b>ZH:</b> 可调度：只在能量被取用时才消耗燃料的发电机（燃料、岩浆、半流体、柴油、热能、经验与掉落物发电机）。
     * <p><b>JA:</b> 調整できる電源：エネルギーが取られている間だけ燃料を使う発電機（燃料・溶岩・半流体・ディーゼル・熱・経験値・ドロップの発電機）。
     */
    DISPATCHABLE
}
