---
navigation:
  title: "重火器"
  icon: mio_icif:item_tool_rocket_launcher
  parent: tools.md
  position: 6
item_ids:
  - mio_icif:item_tool_rocket_launcher
  - mio_icif:item_rocket
  - mio_icif:item_tool_plasma_launcher
  - mio_icif:item_tool_plasma_air_cannon
---

# 重火器

## ロケットランチャー

<ItemLink id="mio_icif:item_tool_rocket_launcher" /> は 1 発ごとに <ItemLink id="mio_icif:item_rocket" /> 1 個と 20,000 EU を使い、1 秒に 1 発撃てる。ロケットは重力を受け、爆発の威力は 3（中心で約 43 ダメージ）。クリエイティブではロケット弾を使わない。

## プラズマランチャー

<ItemLink id="mio_icif:item_tool_plasma_launcher" /> は道を開き、場所を片づけるための武器である。1 発 2,000 EU、1 秒に 1 発、満充電で 20 発。

- 弾は通り道の製錬できるブロックを製錬し、水と溶岩を消し、燃えるブロックに火をつける。
- 命中すると熱爆発を起こす：爆発耐性の低いブロックだけを壊す。エンティティへのダメージは中心で約 24、外側ほど小さい。射手はダメージを受けない。

## プラズマ空気砲

<ItemLink id="mio_icif:item_tool_plasma_air_cannon" /> は押し続けてチャージし、離すと円錐形の空気の波（半角 15°）を撃つ。主にノックバック用である。ダメージはチャージに応じて 4–10、遠い端では半分になる。1 発 1,000 EU。飼いならした動物、村人、ゴーレム、同じチームのプレイヤーには当たらない。

## 地形破壊

サーバー設定 `Weapons.weaponsDamageTerrain`（既定でオン）は、電動プラズマガン、ロケット、プラズマランチャーがブロックを壊すかどうかを決める。オフのときはエンティティにだけダメージを与える。保護区域は常に有効である。
