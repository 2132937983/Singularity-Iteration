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
  - mio_icif:generator/block_steam_turbine_rotor_bronze
  - mio_icif:generator/block_steam_turbine_rotor_iron
  - mio_icif:generator/block_steam_turbine_rotor_adviron
  - mio_icif:generator/block_steam_turbine_rotor_titanium
  - mio_icif:generator/block_steam_turbine_rotor_iridium
  - mio_icif:generator/block_steam_turbine_rotor_netherite
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

- 1 tick に蒸気 1,024 mB を 512 EU に変える。最大 4 回（4,096 mB で 2,048 EU）。<ItemLink id="mio_icif:generator/block_steam_turbine" /> と同じ 1 EU あたり 2 mB。IC2 Classic と同じく、10,000 EU のバッファに 1 回分（512 EU）の空きがあれば動き、蒸気のある分だけ全部の回数を行うので、バッファは最大 1,536 EU まで超えることがある。内部に蒸気タービンローターを入れると 1 回あたりの EU が増える（「ローター」を参照）。
- 蒸気 100 mB ごとに水 1 mB を返す（蒸気原子炉が水を沸かす比率と同じなので、原子炉との循環で水は減らない。IC2 Classic も自身の 1:160 の比率で同じことをする）。水タンクは 2,000 mB で、入りきらない水は失われる。
- コントローラーには GUI がない。右クリックすると状態（稼働中、待機中、何がどこに足りないか）が出る。Jade でも見られる（蒸気、水、EU バッファ、出力）。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧ティア | HV（512 EU）、電力ポートから |
| 出力 | ローターなしで 2,048 EU/t、ネザライトのローター 3 個で最大 3,968 EU/t（1 tick に 512 EU の HV パケット 4～8 個） |
| 蒸気タンク | 16,000 mB |
| 水タンク | 2,000 mB |
| エネルギーバッファ | 10,000 EU |

## 構造

- コントローラーは前端の中央に置き、外を向く。トンネルはその後ろに伸びる。
- 辺の 28 ブロックは外装。残りの外殻 13 か所（4 つの長い側面の中央と後端の中心）は外装、流体ポートか電力ポート。内部の 3 ブロックは空気か蒸気タービンローター。
- 部品を外すとすぐに止まる。構造がそろうと自動で再開する（毎秒確認）。
- 隣り合う外装とポートはつながって 1 枚のパネルになる。構造が完成している間、前面には大きなファンが 1 つ表示される。制御装置が中心のハブ、周りの外装 8 個が羽根になる。稼働中はファンが回り、蒸気を吹き出す。ファンが消えたら構造が欠けている。

ドラッグすると回せる。

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:steam_tunnel" />
</GameScene>

<SiStructureInfo id="mio_icif:steam_tunnel" />

## ローター

トンネル内部の 3 ブロックに蒸気タービンローターを最大 3 個入れられる。1 個ごとに 1 回あたりの EU が増え、1 回に使う蒸気と戻る水は変わらない。種類は混ぜてもよく、空けたままでもよい。ローターがなければ今までどおり 1,024 mB で 512 EU。

<Row>
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_bronze" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_iron" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_adviron" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_titanium" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_iridium" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_netherite" scale="2" />
</Row>

| ローター | 1 回あたりの増加 | 3 個入れたとき |
|---|---|---|
| 青銅 | +32 (+6.25%) | 608 EU / 1,024 mB |
| 鉄 | +64 (+12.5%) | 704 EU / 1,024 mB |
| 精錬鉄 | +80 (+15.625%) | 752 EU / 1,024 mB |
| チタン | +96 (+18.75%) | 800 EU / 1,024 mB |
| イリジウム | +128 (+25%) | 896 EU / 1,024 mB |
| ネザライト | +160 (+31.25%) | 992 EU / 1,024 mB（3,968 EU/t） |

- 電力ポート 1 個で全出力を送れる。ローターに合わせて 1 tick に 512 EU の HV パケットを 4～8 個送る（最大 3,968 EU/t）。
- ローターの設置や撤去はすぐに反映され、ローターは 6 方向のどれにも向けられる。Jade に内部のローター、蒸気使用量（全出力には 4,096 mB/t）と最大出力が出て、蒸気待ちやバッファ満杯のときはそう表示される。コントローラーを右クリックしても同じことと 1 回あたりの EU が分かる。
- ローターはその金属のローターブレード 4 枚を作業台の四隅に、軸を中央に置いて作る（X 字形。十字形は運動エネルギー用のローターになる）。ネザライトのローターはイリジウムのローターを鍛冶台でネザライトインゴットとネザライト強化テンプレートで強化して作る。

## 使用手順

1. 外装で構造を作る（ストーンカッターで基本機械筐体 1 個から外装 1 個）。
2. 電力ポートを 1 個以上置き、HV ケーブルをつなぐ。
3. 流体ポートかコントローラーに蒸気を送る。流体ポートは隣のパイプやタンクへ水も押し出す。
4. 流体ポートを右クリックすると GUI が開く。流体原子炉の流体ポートと同じく、流体エジェクターアップグレード（アップグレードの方向と速度で水を出す）か流体吸入アップグレード（隣のタンクから蒸気を吸い込む）を入れられる。

## 注意

- 四連燃料棒とオーバークロック蒸気ベント 4 個の蒸気原子炉 1 基は 1 tick に約 192 mB の蒸気を作る。本機は 1 tick に最大 4,096 mB を処理する。多数の原子炉の蒸気を 1 か所で使うためのもので、効率が上がるわけではない。
- 電力ポートが複数あれば出力を分け合う。

## レシピ

<RecipesFor id="mio_icif:generator/block_steam_tunnel" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_casing" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_fluid_port" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_power_port" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_bronze" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_iron" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_adviron" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_titanium" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_iridium" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_netherite" fallbackText="-" />
