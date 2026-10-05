---
navigation:
  title: "Energy Management Terminal"
  icon: mio_icif:wiring/block_energy_terminal
  parent: power.md
  position: 11
item_ids:
  - mio_icif:wiring/block_energy_terminal
---

# Energy Management Terminal

<Row>
  <BlockImage id="mio_icif:wiring/block_energy_terminal" scale="3" />
</Row>

## 機能

端末が接続先ケーブルネットワークの発電、消費、蓄電を監視する。
端末はEUを使わず、ネットワークに負荷を加えない。
GUIで機器を停止できる。停止した機器は入力も出力もしない。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 端末をネットワークのケーブルの隣に置く。
2. 端末を右クリックし、GUIを開く。
3. ローカルでこのネットワークを、グローバルで変圧器の先のネットワークも表示する。
4. 機器のスイッチをクリックし、機器をネットワークから切り離す。

## レシピ

<RecipesFor id="mio_icif:wiring/block_energy_terminal" fallbackText="-" />
