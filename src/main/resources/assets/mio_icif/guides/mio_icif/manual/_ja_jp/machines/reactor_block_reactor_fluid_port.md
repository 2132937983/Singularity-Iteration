---
navigation:
  title: "Reactor Fluid Port"
  icon: mio_icif:reactor/block_reactor_fluid_port
  parent: heavy.md
  position: 12
item_ids:
  - mio_icif:reactor/block_reactor_fluid_port
---

# Reactor Fluid Port

<Row>
  <BlockImage id="mio_icif:reactor/block_reactor_fluid_port" scale="3" />
</Row>

## 機能

原子炉流体ポートはパイプを流体原子炉の冷却材タンクにつなぐ。冷却材が入り、高温の冷却材が出る。
ポートには流体排出か流体吸入のアップグレード用スロットが1個ある。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 原子炉外殻の圧力容器1個を流体ポートに替える。
2. 冷却材の供給パイプをポートに接続する。
3. ポートのスロットに流体排出アップグレードを入れる。
4. ポートから、タンクか高温冷却材を使う機械へパイプをつなぐ。

## 注意

- ポートは2ブロック以内の流体モードの原子炉と連携する。

> **警告：** 高温冷却材の出口を空けておく。タンクが満杯だと冷却が止まり、原子炉が過熱することがある。


## レシピ

<RecipesFor id="mio_icif:reactor/block_reactor_fluid_port" fallbackText="-" />
