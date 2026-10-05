<p align="center">
  <img src="logo.png" alt="Singularity Iteration" width="100%">
</p>

<h1 align="center">Singularity Iteration</h1>

<p align="center">
  An IC2-style tech mod for <b>Minecraft 1.21.1</b> / <b>NeoForge</b>
</p>

<p align="center">
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.21.1-brightgreen">
  <img alt="NeoForge" src="https://img.shields.io/badge/NeoForge-21.1.218-orange">
  <img alt="License" src="https://img.shields.io/badge/license-Apache--2.0-lightgrey">
</p>

---

## About

This is a mod that aims to implement IC2-style gameplay in high versions, with code and most of the artwork independently developed by **MioPha** **Magellan67** (a small portion was drawn by community members).

Currently, 99% of IC2's content has been fully and independently implemented, including the power grid system, voltage system, agricultural system, various machine blocks, electric tools, and calculations for thermal, kinetic, electrical, and wind energy.

That said, there is still a fair number of bugs, so please back up your saves at any time.

## Features

### Power Grid

The mod uses a pipe-style power grid block system. You can build efficient and tidy power networks using cables of various tiers, and you can use different dyes to block connections between different cables. However, please be aware that if the voltage exceeds the cable's capacity, the wire will melt.

### Mining & Ores

You can use drill tools like the iron drill to explore your mines. The mod adds three new ores — **Tin**, **Lead**, and **Uranium** — which are the most basic resources for various materials within the mod.

### Ore Processing

To process the ores you mine, the mod includes a **Macerator** that can turn one ore into two ore dusts, giving you double yield through smelting. The **Ore Washing Plant** and **Thermal Centrifuge** can take this efficiency even further.

### Power Generation

As machines multiply, you'll need more power generation. Beyond the basic **steam generator**, there are eco-friendly **wind** and **water generators**, **geothermal** and **semifluid generators**, or more cost-effective **kinetic generators**. But if you're after the ultimate power generation efficiency, **nuclear power** will be your best choice.

### Nuclear Power

To develop nuclear power, you'll need not only nuclear fuel but also reactor components such as **heat vents**, **heat exchangers**, and **cooling cells**. Their arrangement inside the nuclear reactor not only determines power generation efficiency but also the fate of the surrounding area.

### Late-Game Gear

Once you've completed your basic industrial production line, you'll have access to equipment like **Nano Armor** and **Quantum Armor**, along with the **Nano Saber** weapon — these will be your go-to gear for exploring the world.

**Welcome to the game!**

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for the per-version changes from 0.1.7.16 through 0.1.7.34, migration notes, and validation scope.

## Requirements

| Requirement | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Mod Loader | NeoForge 21.1+ (build pin: 21.1.218) |
| Bundled Components | Singularity Iteration Core and SCEX Independent Energy Platform |

The mod targets Minecraft 1.21.1 on NeoForge. The runtime loader range is `[21.1,)`; 21.1.218 is the reproducible build version. Modern Industrialization, GregTech Modern, AE2, Botania and other integrations are optional. The Core is bundled in the main mod JAR and does not need a second runtime installation.

## Reporting Bugs

If you find any bugs, feel free to leave an [issue](https://github.com/2132937983/Singularity-Iteration/issues) — it would be very helpful to me.

## License

Licensed under the [Apache-2.0](LICENSE) license.

---
---

# 中文说明

这是一个想要在高版本实现 IC2 玩法的 MOD，由 **MioPha** **Magellan67** 独立实现代码与大部分美术（小部分为群友绘制）。

目前完整独立实现了 IC2 里 99% 的内容，包括电网系统、电压系统、农业系统，以及各类机器方块、电力工具、热能、动能、电能与风能的计算。

但是Bug也拥有一定一定数量级，请随时备份好你的存档。

## 核心内容

### 管道型电网

模组使用管道型电网方块，你可以使用各级电缆搭建出高效与整洁的电能网络，你可以使用不同的染料来阻断不同电缆之间的连接。但是请注意，如果遇到超过了承载能力的电压，电线会被熔断。

### 矿物与开采

您可以使用铁钻头等钻头工具来探索你的矿洞。模组新增了「**锡**」「**铅**」「**铀**」三种矿石，它们是模组内各个材料最基础的资源。

### 矿石处理

为了处理你开采的矿石，模组内拥有**打粉机**，可以帮助你将一个矿石变成两个矿石粉，通过冶炼你可以获得两倍的收益；而**洗矿机**与**热能离心机**能将收益更上一层。

### 发电

机器变多，我们需要更多的发电。除了最基础的**火力发电机**，我们还有环保的**风力**与**水力发电机**，或者**地热**与**半流质发电机**，或者性价比更高的**动能发电机**。但如果你追求更极致的发电效率，**核电**将会是你的最佳选择。

### 核电

为了发展核电，你除了需要准备核燃料，还需要准备**散热片**、**热交换器**与**冷却单元**等反应元件。它们在核反应堆内的摆放不仅决定了核能发电效率，还决定了所在地区以后的命运。

### 后期装备

如果你完成了基础工业产线，那么还有**纳米装甲**、**量子装甲**等装备，再加上配备**纳米剑**武器，它们将会是你探索世界的不二选择。

**欢迎你的游玩！**

## 版本变更

各版本功能、修复、迁移说明与验证范围见 [CHANGELOG.md](CHANGELOG.md)，本轮覆盖 0.1.7.16 至 0.1.7.34。

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| 模组加载器 | NeoForge 21.1+（构建锁定 21.1.218） |
| 内置组件 | Singularity Iteration Core、SCEX 独立能源平台 |

模组面向 Minecraft 1.21.1 NeoForge；运行加载器范围为 `[21.1,)`，21.1.218 用于可复现构建。Modern Industrialization、GregTech Modern、AE2、Botania 等联动均为可选。Core 已内置于主模组 JAR，运行时无需再单独安装。游戏内手册可选安装 GuideME；未安装时不影响其他功能。

## 问题反馈

如果你发现了 BUG，欢迎留下你的 [ISSUE](https://github.com/2132937983/Singularity-Iteration/issues)，这对我很有帮助。

## 许可证

本项目采用 [Apache-2.0](LICENSE) 许可证。
