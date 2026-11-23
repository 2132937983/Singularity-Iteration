---
navigation:
  title: "TESUコントローラー"
  icon: mio_icif:wiring/block_tesu_controller
  parent: power.md
  position: 17
item_ids:
  - mio_icif:wiring/block_tesu_controller
---

# TESUコントローラー

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_controller" scale="3" />
</Row>

## 機能

TESUコントローラーは TESU（テラ級蓄電タンク）の中核で、7×7×13 の直方体の蓄電設備です。コントローラーが電力を蓄え、
底部リングの入力ポートと出力ポートが出し入れします。構造の外側を向き、構造はその向きに合わせて回ります。
コントローラー自身の面は電力を受け渡ししません。ケーブルはポートにつないでください。

## 電力データ

| 項目 | 値 |
|---|---|
| 蓄電量 | 227,000,000,000 EU |

## 手順

1. 下の構造を組み、コントローラーを底部リングの正面中央に置きます。
2. ポート位置に TESU入力ポート、TESU出力ポート、TESU無線出力ポートを置き、使わない位置は TESU外装でふさぎます。
3. GUI を開くと、蓄電量、ポート数と出力合計、または最初の誤ったブロックの座標と必要なブロックが表示されます。

## 注意

- コントローラーは毎秒構造を確認します。
- GUI にはアイテムを充電するスロットと電池から EU を取り出すスロットがあります。

## 構造

| 層 | ブロック |
|---|---|
| 1 | 底面：7×7 の TESU外装 |
| 2 | 正面中央にコントローラー、各面 2 か所ずつ計 8 か所のポート位置、残りは外装 |
| 3–11 | 胴体：外装の壁、各面中央に幅 3 列の防爆ガラスの窓。内部中央の 3-5-5-5-3 の 21 マスは TESU蓄電セル（計 189 個）、四隅の内側は外装 |
| 12 | 蓋：7×7 の外装 |
| 13 | 頂部：7×7 の外装 |

ビューをドラッグすると回転します。下の一覧はブロックとその規則です。JEI の「マルチブロック構造」カテゴリーにも同じ図があります。

<GameScene zoom="2" interactive={true}>
  <SiStructure id="mio_icif:tesu" />
</GameScene>

<SiStructureInfo id="mio_icif:tesu" />

## レシピ

<RecipesFor id="mio_icif:wiring/block_tesu_controller" fallbackText="-" />
