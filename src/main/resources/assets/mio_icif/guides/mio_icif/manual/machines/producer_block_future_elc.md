---
navigation:
  title: "Futures Machine"
  icon: mio_icif:producer/block_future_elc
  parent: machines.md
  position: 29
item_ids:
  - mio_icif:producer/block_future_elc
---

# Futures Machine

<Row>
  <BlockImage id="mio_icif:producer/block_future_elc" scale="3" />
</Row>

## Function

The Futures Machine buys and sells commodities for Industrial Coins at prices that change every day.
Each trade costs EU. The machine takes the items and the coins from your inventory. A daily limit applies.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 100 EU/t |
| Energy storage | 10,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an LV cable to the machine.
2. Open the GUI and select a category and a commodity.
3. Set the quantity of the trade.
4. Click Buy or Sell.

## Notes

- Market events change the price trend for a number of days.
- Some commodities stay locked until you get the advancement that the GUI shows.

## Recipe

<RecipesFor id="mio_icif:producer/block_future_elc" fallbackText="-" />
