---
navigation:
  title: "スチームトンネル"
  icon: mio_icif:generator/block_steam_tunnel
  parent: generators.md
  position: 32
item_ids:
  - mio_icif:generator/block_steam_tunnel
  - mio_icif:generator/block_steam_tunnel_casing
  - mio_icif:generator/block_steam_tunnel_fluid_port
---

# スチームトンネル

<Row>
  <BlockImage id="mio_icif:generator/block_steam_tunnel" scale="3" />
  <BlockImage id="mio_icif:generator/block_steam_tunnel_casing" scale="3" />
  <BlockImage id="mio_icif:generator/block_steam_tunnel_fluid_port" scale="3" />
  <BlockImage id="mio_icif:generator/block_steam_tunnel_power_port" scale="3" />
</Row>

## 機能

IC2 Classic の大型蒸気タービン。3 x 3 x 5 の中空構造で、大量の蒸気を 1 か所で EU に変える。

- 1 tick に蒸気 1,024 mB を 512 EU に変える。バッファに空きがあれば最大 4 回（4,096 mB で 2,048 EU）。<ItemLink id="mio_icif:generator/block_steam_turbine" /> と同じ 1 EU あたり 2 mB。
- 蒸気 100 mB ごとに水 1 mB を返す（蒸気原子炉が水を沸かす比率と同じ）。水タンクは 2,000 mB で、入りきらない水は失われる。
- GUI はない。コントローラーを右クリックすると状態（稼働中、待機中、何がどこに足りないか）が出る。Jade でも見られる。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧ティア | HV（512 EU）、電力ポートから |
| 出力 | 最大 2,048 EU/t（1 tick に 512 EU のパケット 4 個） |
| 蒸気タンク | 16,000 mB |
| 水タンク | 2,000 mB |
| エネルギーバッファ | 10,000 EU |

## 構造

- コントローラーは前端の中央に置き、外を向く。トンネルはその後ろに伸びる。
- 辺の 28 ブロックは外装。残りの外殻 13 か所（4 つの長い側面の中央と後端の中心）は外装、流体ポートか電力ポート。内部の 3 ブロックは空気。
- 部品を外すとすぐに止まる。構造がそろうと自動で再開する（毎秒確認）。
- 隣り合う外装はつながって 1 枚のパネルになる。構造が完成している間、前面には大きなファンが 1 つ表示される。制御装置が中心のハブ、周りの外装 8 個が羽根になる。ファンが消えたら構造が欠けている。

ドラッグすると回せる。

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:steam_tunnel" />
</GameScene>

<SiStructureInfo id="mio_icif:steam_tunnel" />

## 使用手順

1. 外装で構造を作る（ストーンカッターで基本機械筐体 1 個から外装 1 個）。
2. 電力ポートを 1 個以上置き、HV ケーブルをつなぐ。
3. 流体ポートかコントローラーに蒸気を送る。流体ポートは隣のパイプやタンクへ水も押し出す。

## 注意

- 四連燃料棒とオーバークロック蒸気ベント 4 個の蒸気原子炉 1 基は 1 tick に約 192 mB の蒸気を作る。本機は 1 tick に最大 4,096 mB を処理する。多数の原子炉の蒸気を 1 か所で使うためのもので、効率が上がるわけではない。
- 電力ポートが複数あれば出力を分け合う。

## レシピ

<RecipesFor id="mio_icif:generator/block_steam_tunnel" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_casing" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_fluid_port" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_power_port" fallbackText="-" />
