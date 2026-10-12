/**
 * <p><b>EN:</b> Public API of Singularity Iteration ({@code mio_icif}). Get every sub-API from
 * {@link com.miophas.singularity_iteration.core.api.MioIcifAPI}.
 * <ul>
 *   <li>Use the interfaces only. Do not depend on implementation classes ({@code *Impl}); they can
 *       change or go away in any version.</li>
 *   <li>Do not extend {@link com.miophas.singularity_iteration.core.prefab.blockentity.GenericMachineBlockEntity}
 *       (package {@code core.prefab.blockentity}). Change its behavior with the callbacks of
 *       {@link com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI}.</li>
 *   <li>The API jar ({@code mio_icif-api}) contains only this package tree. The build checks that
 *       it refers to no runtime, prefab or {@code common} class.</li>
 *   <li>Comments in this package follow ASD-STE100 in English, with Chinese and Japanese
 *       translations of the same content (see {@code docs/dev/COMMENT_STYLE.md}).</li>
 * </ul>
 * <p><b>ZH:</b> 奇点迭代（{@code mio_icif}）的公共 API。所有子 API 都从 {@link com.miophas.singularity_iteration.core.api.MioIcifAPI} 获取。
 * <ul>
 *   <li>只使用接口。不要依赖实现类（{@code *Impl}），它们在任何版本中都可能改变或移除。</li>
 *   <li>不要继承 {@code GenericMachineBlockEntity}（{@code core.prefab.blockentity} 包），请用 {@code IMachineBuilderAPI} 的回调改变其行为。</li>
 *   <li>API JAR（{@code mio_icif-api}）只包含本包树。构建会检查它不引用任何运行时、预制或 {@code common} 类。</li>
 *   <li>本包的注释以 ASD-STE100 英文为准，并附相同内容的中文与日文翻译（见 {@code docs/dev/COMMENT_STYLE.md}）。</li>
 * </ul>
 * <p><b>JA:</b> 奇点迭代（{@code mio_icif}）の公開 API。すべてのサブ API は {@link com.miophas.singularity_iteration.core.api.MioIcifAPI} から得る。
 * <ul>
 *   <li>インターフェースだけを使う。実装クラス（{@code *Impl}）に依存しない。どのバージョンでも変更・削除されうる。</li>
 *   <li>{@code GenericMachineBlockEntity}（{@code core.prefab.blockentity} パッケージ）を継承しない。動作は {@code IMachineBuilderAPI} の
 *       コールバックで変える。</li>
 *   <li>API JAR（{@code mio_icif-api}）はこのパッケージ階層だけを含む。ビルドは、ランタイム・プレハブ・{@code common} のクラスを
 *       参照していないことを検査する。</li>
 *   <li>このパッケージのコメントは ASD-STE100 の英語を正とし、同じ内容の中国語・日本語訳を付ける（{@code docs/dev/COMMENT_STYLE.md} 参照）。</li>
 * </ul>
 *
 * <h2>Packages / 包结构 / パッケージ構成</h2>
 * <ul>
 *   <li>{@code api}: entry point / 入口 / 入口</li>
 *   <li>{@code api.machine}, {@code api.machine.builder}: machines / 机器 / 機械</li>
 *   <li>{@code api.energy}: EU network / EU 电网 / EU 電力網</li>
 *   <li>{@code api.item}: items, ballistics, jetpacks / 物品、弹道、喷气背包 / アイテム、弾道、ジェットパック</li>
 *   <li>{@code api.recipe}: recipes / 配方 / レシピ</li>
 *   <li>{@code api.fluid}: fluids / 流体 / 液体</li>
 *   <li>{@code api.crop}: crops / 作物 / 作物</li>
 *   <li>{@code api.block}: blocks (rubber wood {@link com.miophas.singularity_iteration.core.api.block.IRubberWood}) / 方块 / ブロック</li>
 *   <li>{@code api.world}: wind, rubber trees / 风力、橡胶树 / 風、ゴムの木</li>
 *   <li>{@code api.registry}: registries / 注册表 / レジストリ</li>
 *   <li>{@code api.heat}, {@code api.kinetic}: HU, KU / 热能、动能 / 熱、運動エネルギー</li>
 *   <li>{@code api.reactor}: nuclear reactor / 核反应堆 / 原子炉</li>
 *   <li>{@code api.upgrade}: upgrades / 升级 / アップグレード</li>
 *   <li>{@code api.capability}: capabilities / 能力 / ケイパビリティ</li>
 *   <li>{@code api.suit}, {@code api.armory}: suit units, armory events / 套装单元、军械库事件 / スーツユニット、武装庫イベント</li>
 *   <li>{@code api.event}: events / 事件 / イベント</li>
 * </ul>
 */
package com.miophas.singularity_iteration.core.api;
