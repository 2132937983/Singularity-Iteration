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
- **Treasury**: the market sells only what it bought before, plus a small daily import at a higher price. When a commodity runs short, the market posts a bounty for it (Bounties tab) and does not sell it while a normal or advanced bounty needs it.
- **Commissions**: ask other players for items and pay coins, items or UU-matter. The reward is held until delivery. When several players ask for the same item, it gets listed on the exchange.
- **Futures**: cash-settled contracts for 3 or 7 days. The contract price already includes the expected price movement (events too), so futures only pay off when you foresee more than the market.
- **Mailbox**: what the exchange owes you (sales, refunds, items that did not fit). Collect it in the Orders and Mail tab.
- **Sell page**: put items in the sell box (shift-click, or Fill takes every sellable stack outside the hotbar). The page shows what each commodity pays now and at the daily auction. Sell now, or give the items to the auction: it sells them together with everyone's at the change of the market day, so the order of the sellers does not matter. What stays in the box comes back when you close the panel.
- **Bounties**: the market posts system bounties in three tiers. Normal: a commodity it lacks, every day, for 3 days, reward 1.15 times the value. Advanced: a set of goods, two or three a week, for 7 days, reward 1.3 times; in multiplayer one player may deliver at most half of it until its last day. Special: a rare item, now and then, with a rare reward. Rewards are coins, goods of the market's stock, UU-matter or items made within a weekly limit; rare items delivered to special bounties go to a vault and pay later ones. Press Deliver to hand over what you carry: you are paid at once in proportion, or by contribution when the bounty says so. In single player you can complete every bounty alone.
- **Settlement**: put a shulker box, a backpack, an AE2 storage cell or a container of UU-matter into the slot of the Settlement page. Its commodities sell now or at the daily auction like the sell box, its plain items deliver to bounties and commissions, its UU-matter goes onto your account, and purchases of the selected market can go straight into it. The container comes back when you close the panel. AE2 is optional.
- **UU-matter board**: players trade UU-matter of their accounts for coins on the market "UU-matter (lots of 100 mB)": one unit is 100 mB, the price is in coins per 100 mB, with the usual order book, fee and resting orders. The market itself never buys or sells UU-matter, so the board cannot turn energy into coins.
- **Scrap sink**: with "Rest to scrap" on the sell or settlement page, what an instant sale leaves goes to the recycler when the market pays almost nothing for it (the next bid below 5 % of the price or below 1 coin): scrap at the IC2 recycler's rate, 1 for 8 items. The coins of the sale stay the same.
- **Stabiliser** (off unless the server turns it on): when the market creates more coins in 7 days than its target, the sell depths and the daily allowance shrink a little each day; small sales keep nearly their price. The Treasury tab shows its state.
- **ID card**: use a blank card to make it your personal card; using it anywhere opens your account (mailbox, orders, auction orders, cancel; refunds go to the mailbox) and the delegate cards you wrote at your Trade Terminals.
- **Coin account**: the coins the exchange keeps for you (the coins of the mailbox). Deposit the coins of the inventory or of the settlement container, and withdraw them, in the Orders and Mail tab (amount 0 = all). Purchases, buy orders, commissions and futures collateral take from the inventory and the account together. In bank mode sales and payouts go to the account and payments take from it first, so large trades do not fill the inventory.
- **Minimum price**: the sell and settlement pages take a minimum, in percent of the bid without pressure or in coins per item. An instant sale sells of each commodity the most items whose own price is still at least the minimum, in one batch, and pays the sum at once; the page shows what each commodity sells for before you click. In the order panel a sale can name its limit in percent (the c / % button), and the panel shows what the market part of the sale pays now.
- **Number fields**: a click selects the number, so typing replaces it; the mouse wheel and the arrow keys change it (Shift: 64, Ctrl: 10).

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
- Operators can list, post and cancel bounties and see the datapack problems and the economy settings with /mio_icif bounty. Pack makers set bounty tables, reward pools, listings that are open from the start and economy defaults in the datapack folder data/(namespace)/si_exchange; a value changed in the server config wins.

## Recipe

<RecipesFor id="mio_icif:producer/block_future_elc" fallbackText="-" />
