---
navigation:
  title: "TESU 输入端口"
  icon: mio_icif:wiring/block_tesu_input
  parent: power.md
  position: 18
item_ids:
  - mio_icif:wiring/block_tesu_input
---

# TESU 输入端口

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_input" scale="3" />
</Row>

## 功能

TESU 输入端口接受任何电压（最高 MAX）的电，存进两个 MAX 包大小的缓存，每 tick 最多把一个 MAX 包转交给控制器。
它要装在 TESU 底圈的端口位上才工作。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压 | 任何，最高 MAX（单包 2,147,483,647 EU） |
| 转交给控制器 | 最多 2,147,483,647 EU/t |

## 配方

<RecipesFor id="mio_icif:wiring/block_tesu_input" fallbackText="-" />
