---
navigation:
  title: "チャンクローダー"
  icon: mio_icif:producer/block_chunk_loader
  parent: machines.md
  position: 16
item_ids:
  - mio_icif:producer/block_chunk_loader
---

# チャンクローダー

<Row>
  <BlockImage id="mio_icif:producer/block_chunk_loader" scale="3" />
</Row>

## 機能

チャンクローダーは選んだチャンクを読み込んだまま保つ。機械は周囲9×9チャンクの範囲で最大25チャンクを読み込む。
読み込むチャンクごとにEUを使う。EUがなくなると機械は止まる。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 2,500 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. MVケーブルを機械に接続する。
2. GUIを開き、読み込むチャンクをクリックする。
3. 機械へのEU供給を安定させる。

## 注意

- チャンクローダーのあるチャンクも選択グリッドに含まれる。

> **警告：** MVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。

## サーバールール

ローダーを設置したプレイヤーが所有者になる。サーバーはコンフィグの `ChunkLoaders` 節でローダーを制限できる。

| 設定 | 既定値 | 効果 |
|---|---|---|
| `enabled` | true | false: すべてのローダーがチャンクを解放する |
| `maxChunksPerLoader` | 25 | ローダー 1 台のチャンク数 |
| `maxChunksPerPlayer` | 0（無制限） | 1 人のプレイヤーの全ローダー合計のチャンク数 |
| `maxLoadersPerPlayer` | 0（無制限） | プレイヤーごとの稼働ローダー数 |
| `offlineOwnerMinutes` | -1（常に稼働） | 所有者のログアウト後もローダーが動き続ける分数 |
| `euPerChunkPerTick` | 1.0 | チャンク 1 つ・1 tick あたりの EU |
| `randomTicks` | true | false: 読み込んだチャンクでランダムティックと自然スポーンを行わない。機械は動き続ける |
| `ownerOnlyEdit` | false | 選択を変えられるのは所有者とオペレーターだけ |

- チャンク上限を超えると、自分のチャンクを最優先し、次に近い選択チャンクから読み込む。
- マップ上部の赤い帯が、ローダーがチャンクを（すべて）読み込まない理由を示す。詳細はチャンク数の表示にマウスを重ねると出る。
- ルールで止まったローダーは、理由がなくなると自動で再開する（例: 所有者のログイン）。
- オペレーター: `/mio_icif chunkloaders` で全ローダーを一覧表示する。`disable` と `enable` で 1 台を停止・解除し、`disable_all` で全台を止める。


## レシピ

<RecipesFor id="mio_icif:producer/block_chunk_loader" fallbackText="-" />
