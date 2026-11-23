---
navigation:
  title: "TESU無線出力ポート"
  icon: mio_icif:wiring/block_tesu_wireless_port
  parent: power.md
  position: 20
item_ids:
  - mio_icif:wiring/block_tesu_wireless_port
---

# TESU無線出力ポート

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_wireless_port" scale="3" />
</Row>

## 機能

完成した TESU のポート位置に置くと、無線出力ポートは周囲 ±16 マスの立方体の無線範囲の中心になります。
範囲内でアップグレードスロットに無線アップグレードを入れた機械は、電圧に関係なく毎 tick TESU から満充電されます（ケーブル不要、過電圧なし）。
ポート自身は蓄電しません。TESU中継器で範囲をさらに広げられます。
機械を充電している間、ポートからそれぞれの機械へ粒子の光線が出ます（無線送電ノードと同じ）。

## 使い方

1. ポートを TESU 底部リングのポート位置に置きます。
2. 充電したい機械に無線アップグレードを入れます（アップグレードスロットがあり電力を使う機械）。
3. ポートを右クリックすると、完成した TESU に接続しているか確認できます。

## 注意

- 複数の範囲に入っている機械は、電力のある最初の範囲から受け取ります。
- 変圧器アップグレードか蓄電アップグレードを付けられる機械に無線アップグレードを付けられます。

## レシピ

<RecipesFor id="mio_icif:wiring/block_tesu_wireless_port" fallbackText="-" />
