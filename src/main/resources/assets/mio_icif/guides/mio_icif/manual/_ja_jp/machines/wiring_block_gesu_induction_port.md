---
navigation:
  title: "GESU無線誘導ポート"
  icon: mio_icif:wiring/block_gesu_induction_port
  parent: power.md
  position: 16
item_ids:
  - mio_icif:wiring/block_gesu_induction_port
---

# GESU無線誘導ポート

<Row>
  <BlockImage id="mio_icif:wiring/block_gesu_induction_port" scale="3" />
</Row>

## 機能

GESU モジュールです。GESU コアのどの面にも、入力・出力モジュールの代わりに置けます。GESU が完成していると、GESU から電力を取り、
自分の周囲 ±8 マス内のプレイヤー全員を無線で充電します：防具・手持ち・インベントリの電気アイテム（充電パッドと同じ順）を、
1 個あたり 1 tick に最大 32,768 EU（LuV 1 パケット）。ケーブルにはつながらず、電力網にも送りません。

## 使い方

1. GESU を組むとき、コアの 6 面のどれかにこのポートを置きます。
2. ポートから 8 マス以内に立つと、電気装備が充電されます。ときどき出る火花が充電中のしるしです。
3. ポートを右クリックすると GESU の画面が開きます。

## レシピ

<RecipesFor id="mio_icif:wiring/block_gesu_induction_port" fallbackText="-" />
