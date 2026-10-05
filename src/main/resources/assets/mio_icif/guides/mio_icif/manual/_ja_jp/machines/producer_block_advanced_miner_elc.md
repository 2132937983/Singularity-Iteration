---
navigation:
  title: "Advanced Miner"
  icon: mio_icif:producer/block_advanced_miner_elc
  parent: machines.md
  position: 6
item_ids:
  - mio_icif:producer/block_advanced_miner_elc
---

# Advanced Miner

<Row>
  <BlockImage id="mio_icif:producer/block_advanced_miner_elc" scale="3" />
</Row>

## 機能

高度採掘機は採掘パイプもドリルも使わず、機械の下の鉱石を掘る。
ODスキャナーの半径は16ブロックだ。OVスキャナーは32ブロックだ。スキャナーがないと、機械は真下だけを掘る。
機械は収穫物を隣のインベントリに入れる。空きがないとき、収穫物は機械の上に落ちる。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | HV (512 EU) |
| 最大入力 | 512 EU/t |
| 蓄電容量 | 4,000,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. HVケーブルを機械に接続する。
2. ODスキャナーかOVスキャナーをスキャナースロットに入れる。
3. 鉱石を集めるため、機械の隣にチェストを置く。
4. GUIでフィルター、ホワイトリストかブラックリスト、シルクタッチを設定する。

## 注意

- リセットボタンは採掘位置を最上部からやり直す。
- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** HVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_advanced_miner_elc" fallbackText="-" />
