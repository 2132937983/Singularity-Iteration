---
navigation:
  title: "Transformer MV→HV"
  icon: mio_icif:wiring/transformer_mv_hv
  parent: power.md
  position: 36
item_ids:
  - mio_icif:wiring/transformer_mv_hv
---

# Transformer MV→HV

<Row>
  <BlockImage id="mio_icif:wiring/transformer_mv_hv" scale="3" />
</Row>

## 機能

変圧器がMVとHVの電圧Tierの間でEUを変換する。
正面がHV側だ。他の5面がMV側だ。
初期モードは降圧モードだ。レッドストーン制御モードは信号がある間に昇圧する。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 変圧器を置き、正面を自分の方に向ける。
2. 電源のHVケーブルを正面に接続する。
3. MVの機械を他の5面のどれかに接続する。
4. GUIを開き、降圧、昇圧、レッドストーン制御のどれかを選ぶ。
5. レンチで変圧器を右クリックし、正面の向きを変える。

## 注意

- 昇圧モードでは、EUがMVの面から入り、HVで正面から出る。

> **警告：** 入力側の電圧Tierを超える入力で変圧器が爆発する。


## レシピ

<RecipesFor id="mio_icif:wiring/transformer_mv_hv" fallbackText="-" />
