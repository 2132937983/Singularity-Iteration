---
navigation:
  title: "反应堆流体接口"
  icon: mio_icif:reactor/block_reactor_fluid_port
  parent: heavy.md
  position: 12
item_ids:
  - mio_icif:reactor/block_reactor_fluid_port
---

# 反应堆流体接口

<Row>
  <BlockImage id="mio_icif:reactor/block_reactor_fluid_port" scale="3" />
</Row>

## 功能

反应堆流体接口把管道连接到流体反应堆的冷却液储罐。冷却液输入，热冷却液输出。
接口有一个升级槽位，可放流体弹出升级或流体抽入升级。

## 电力数据

该方块不使用EU。

## 使用步骤

1. 把反应堆外壳中的一个压力容器换成流体接口。
2. 把冷却液供给管道连接到接口。
3. 在接口槽位中放入流体弹出升级。
4. 从接口向储罐或使用热冷却液的机器接管道。

## 注意

- 接口可连接2格内处于流体模式的反应堆。

> **警告：** 保持热冷却液输出畅通。热冷却液储罐满时冷却停止，反应堆可能过热。


## 配方

<RecipesFor id="mio_icif:reactor/block_reactor_fluid_port" fallbackText="-" />
