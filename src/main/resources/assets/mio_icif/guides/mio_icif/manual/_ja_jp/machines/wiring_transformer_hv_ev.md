---
navigation:
  title: "Transformer HV→EV"
  icon: mio_icif:wiring/transformer_hv_ev
  parent: power.md
  position: 32
item_ids:
  - mio_icif:wiring/transformer_hv_ev
---

# Transformer HV→EV

<Row>
  <BlockImage id="mio_icif:wiring/transformer_hv_ev" scale="3" />
</Row>

## 機能

変圧器がHVとEVの電圧Tierの間でEUを変換する。
正面がEV側だ。他の5面がHV側だ。
初期モードは降圧モードだ。レッドストーン制御モードは信号がある間に昇圧する。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 変圧器を置き、正面を自分の方に向ける。
2. 電源のEVケーブルを正面に接続する。
3. HVの機械を他の5面のどれかに接続する。
4. GUIを開き、降圧、昇圧、レッドストーン制御のどれかを選ぶ。
5. レンチで変圧器を右クリックし、正面の向きを変える。

## 注意

- 昇圧モードでは、EUがHVの面から入り、EVで正面から出る。

> **警告：** 入力側の電圧Tierを超える入力で変圧器が爆発する。


## レシピ

<RecipesFor id="mio_icif:wiring/transformer_hv_ev" fallbackText="-" />
