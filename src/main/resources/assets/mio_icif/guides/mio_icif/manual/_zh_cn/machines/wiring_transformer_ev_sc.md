---
navigation:
  title: "超高压变压器"
  icon: mio_icif:wiring/transformer_ev_sc
  parent: power.md
  position: 31
item_ids:
  - mio_icif:wiring/transformer_ev_sc
---

# 超高压变压器

<Row>
  <BlockImage id="mio_icif:wiring/transformer_ev_sc" scale="3" />
</Row>

## 功能

变压器在EV和IV电压等级之间转换EU。
正面为IV侧。其余五个面为EV侧。
默认为降压模式。红石控制模式在有红石信号时升压。

## 电力数据

该方块不使用EU。

## 使用步骤

1. 放置变压器，使正面朝向你。
2. 把电源的IV导线接到正面。
3. 把EV机器接到其余五个面之一。
4. 打开界面，选择降压、升压或红石控制。
5. 用扳手右键点击变压器，转动正面。

## 注意

- 升压模式下，EU从EV面输入，以IV从正面输出。

> **警告：** 输入超过输入侧电压等级时，变压器爆炸。


## 配方

<RecipesFor id="mio_icif:wiring/transformer_ev_sc" fallbackText="-" />
