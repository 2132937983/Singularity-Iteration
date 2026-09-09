/**
 * mio_icif API 包
 *
 * <p>这是附属模组与 mio_icif 交互的主要接口。通过 {@link com.singularity_iteration.mio_icif.api.MioIcifAPI}
 * 获取所有 API 实例。
 *
 * <h2>使用指南</h2>
 * <ul>
 *   <li>始终通过接口访问 API，不要直接依赖实现类（{@code *Impl}）</li>
 *   <li>{@link com.singularity_iteration.mio_icif.api.internal.machine.GenericMachineBlockEntity}
 *       位于 {@code api.internal.machine} 包中，虽然可供附属模组继承，但强烈建议通过
 *       {@link com.singularity_iteration.mio_icif.api.machine.builder.IMachineBuilderAPI}
 *       的回调机制自定义行为，而非直接继承</li>
 *   <li>实现类可能在任何版本中变更或移除，不保证向后兼容</li>
 * </ul>
 *
 * <h2>包结构</h2>
 * <ul>
 *   <li>{@code api} - 核心 API 入口和接口</li>
 *   <li>{@code api.machine} - 机器 API</li>
 *   <li>{@code api.machine.builder} - 机器构建器 API</li>
 *   <li>{@code api.energy} - 能源网络 API</li>
 *   <li>{@code api.item} - 物品 API</li>
 *   <li>{@code api.recipe} - 配方 API</li>
 *   <li>{@code api.fluid} - 流体 API</li>
 *   <li>{@code api.crop} - 作物 API</li>
 *   <li>{@code api.block} - 方块 API</li>
 *   <li>{@code api.registry} - 注册表 API</li>
 *   <li>{@code api.heat} - 热能 API</li>
 *   <li>{@code api.kinetic} - 动能 API</li>
 *   <li>{@code api.reactor} - 核反应堆 API</li>
 *   <li>{@code api.upgrade} - 升级插件 API</li>
 *   <li>{@code api.capability} - 能力系统 API</li>
 *   <li>{@code api.event} - 事件 API</li>
 *   <li>{@code api.example} - 使用示例</li>
 * </ul>
 */
package com.singularity_iteration.mio_icif.api;
