---
navigation:
  title: "TESU無線誘導ポート"
  icon: mio_icif:wiring/block_tesu_induction_port
  parent: power.md
  position: 20
item_ids:
  - mio_icif:wiring/block_tesu_induction_port
---

# TESU無線誘導ポート

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_induction_port" scale="3" />
</Row>

## 機能

完成した TESU のポート位置に置くと、[TESU無線出力ポート](wiring_block_tesu_wireless_port.md)の全機能（無線範囲の中心、中継器の接続先、
無線アップグレード付き機械の充電）に加えて、自分の周囲 ±16 マス内で：

* プレイヤー全員を充電します：防具・手持ち・インベントリの電気アイテムを毎 tick できるだけ満たします。
* 電力を使う機械は、無線アップグレードが**なくても**毎 tick 満充電され、電圧も問いません。ポートからそれぞれの機械へ粒子の光線が出ます。
  発電機・蓄電設備・ケーブル・ノードは対象外です。

電力は TESU コントローラーから引かれ、ポート自身は蓄電しません。

## 使い方

1. TESU 底部リングのポート位置に置きます。
2. 右クリックで、完成した TESU に接続しているか確認できます。

## レシピ

<RecipesFor id="mio_icif:wiring/block_tesu_induction_port" fallbackText="-" />
