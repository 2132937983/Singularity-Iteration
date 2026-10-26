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

The Futures Machine opens the exchange of the server: one market for all players and all machines.
Each action costs the EU of one trade.

- **Market**: the datapack commodities, which the market itself buys and sells, and player listings, which only players trade. The chart shows daily candles with volume, the 5-day mean and the fundamental value (from the UU-matter value).
- **Order book**: an order trades with the best orders of other players first, then with the market. An order that does not fill can wait in the book for 7 days. Orders of one player or one team never trade with each other.
- **Treasury**: the market sells only what it bought before, plus a small daily import at a higher price. When a commodity runs short, the market posts commissions for it and stops selling it until they end.
- **Commissions**: ask other players for items and pay coins, items or UU-matter. The reward is held until delivery. When several players ask for the same item, it gets listed on the exchange.
- **Futures**: cash-settled contracts for 3 or 7 days. The contract price already includes the expected price movement (events too), so futures only pay off when you foresee more than the market.
- **Mailbox**: what the exchange owes you (sales, refunds, items that did not fit). Collect it in the Orders and Mail tab.
- **Sell page**: put items in the sell box (shift-click, or Fill takes every sellable stack outside the hotbar). The page shows what each commodity pays now and at the daily auction. Sell now, or give the items to the auction: it sells them together with everyone's at the change of the market day, so the order of the sellers does not matter. What stays in the box comes back when you close the panel.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 100 EU/t |
| Energy storage | 10,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an LV cable to the machine.
2. Open the GUI and pick a market in the watchlist on the left.
3. In the order panel on the right, choose Buy or Sell, Now or Order, the limit price and the quantity.
4. Click the place button. Collect sales and refunds in the Orders and Mail tab.

## Notes

- Selling to the market has no item limit. Each item you sold of a commodity today, and each item everyone sold that the market has not passed on yet, lowers the bid; half of that sell pressure leaves each day. Small sales get almost the full price, bulk sales a lower average price.
- In multiplayer the market pays each player at most 50,000 coins a day for sales (unused coins are kept up to 3 days). Buying from the market has no daily limit; its stock limits it.
- Player listings of complex items (by default 3 crafting steps or more) trade in UU-matter (mB). Deposit UU-matter from cells or buckets in the Orders and Mail tab.
- Market events change the price trend for a number of days.
- Some commodities stay locked until you get the advancement.
- Operators can check the exchange with /mio_icif market audit, and price a sale (simulate), look for recipe loops (arbitrage), compare prices with UU-matter values (calibrate) and see the coins created and destroyed (ledger) with /mio_icif market.

## Recipe

<RecipesFor id="mio_icif:producer/block_future_elc" fallbackText="-" />
