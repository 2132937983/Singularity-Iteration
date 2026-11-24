---
navigation:
  title: "スキャナーとマルチメーター"
  icon: mio_icif:item_tool_od_scanner
  parent: tools.md
  position: 4
item_ids:
  - mio_icif:item_tool_od_scanner
  - mio_icif:item_tool_ov_scanner
  - mio_icif:item_tool_meter
  - mio_icif:item_tool_windmeter
  - mio_icif:item_geomagnetic_detector
  - mio_icif:item_tool_area_scanner
---

# スキャナーとマルチメーター

## 鉱石スキャナー

| スキャナー | 範囲 | 1 回の消費電力 | 容量 |
|---|---|---|---|
| <ItemLink id="mio_icif:item_tool_od_scanner" /> | 13×13×13 | 48 EU | 100,000 EU |
| <ItemLink id="mio_icif:item_tool_ov_scanner" /> | 25×13×25 | 96 EU | 1,000,000 EU |

- 右クリックでスキャンする。画面に鉱石の種類ごとの数と、いちばん近いブロックの方角と距離が出る。
- `c:ores` に含まれるブロックだけを数え、`c:ores/<種類>` ごとにまとめる。
- OV スキャナーは OD スキャナーからクラフトし、電力は引き継がれる。この 2 つは採掘機の 2 段階のスキャナーでもある。

## マルチメーター

<ItemLink id="mio_icif:item_tool_meter" /> は電力を使わず、3 つのプローブを持つ（G キーで切り替え）：

- **電力。**電力網の装置を右クリックするとデータ画面が開く。スニーク右クリックで読み取り値（入力、出力、通過した電力など）を切り替える。
- **風力。**風力運動エネルギー発生器を右クリックすると、その有効風力を読む。それ以外を右クリックすると、足元の高さの風力を読む。風向きと天候係数も出る。
- **地磁気。**ブロックを右クリックすると、その場所の地磁気強度と現在の Kp 指数を読む。

<ItemLink id="mio_icif:item_tool_windmeter" /> と <ItemLink id="mio_icif:item_geomagnetic_detector" /> は置き換えられた：作業台に単独で置くとマルチメーターになる。

## 範囲スキャナー

<ItemLink id="mio_icif:item_tool_area_scanner" /> で機械を右クリックすると、その作業範囲をワールドに描く。スニーク右クリックで消す。
