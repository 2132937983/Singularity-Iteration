---
navigation:
  title: "Heat Exchanger"
  icon: mio_icif:hugenerator/block_heat_source_fluid
  parent: generators.md
  position: 15
item_ids:
  - mio_icif:hugenerator/block_heat_source_fluid
---

# Heat Exchanger

<Row>
  <BlockImage id="mio_icif:hugenerator/block_heat_source_fluid" scale="3" />
</Row>

## 機能

機械は溶岩またはホットクーラントを熱（HU）に変換する。
溶岩はパホイホイ溶岩になる。ホットクーラントはクーラントになる。
熱伝導体1個ごとに熱出力が増える。機械は正面だけから熱を出す。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 正面が熱を使う機械に接するように機械を置く。
2. 熱伝導体を伝導体スロットに入れる。
3. 溶岩バケツかホットクーラントセルを高温流体入力スロットに入れる。
4. 空の容器を低温流体スロットに入れて出力流体を集める。

## 伝熱面

- 機械は正面、つまり銅の伝熱板がある面からだけ熱を出す。稼働中は伝熱板が赤くなる。
- 設置すると、伝熱板は自分の方を向く。
- 熱を使う機械の受熱面を伝熱板に接触させる：スターリング発電機は背面と正面（銅板の面。設置時に自分を向いていた面）、運動エネルギースターリング発電機はシャフト以外の全面、蒸気発生機は正面以外の全面で熱を受ける。
- 伝熱板を向けたい面にレンチを使う。スニークしてレンチを使うと反対側の面に向く。伝熱板そのものにレンチを使うと機械を回収する。
- 残りの 5 面は熱を出さない。配管・セル・自動化に使う。

## 注意

- 出力タンクが満杯になると、機械は停止する。
- 熱伝導体がないと、機械は熱を作らない。

## レシピ

<RecipesFor id="mio_icif:hugenerator/block_heat_source_fluid" fallbackText="-" />
