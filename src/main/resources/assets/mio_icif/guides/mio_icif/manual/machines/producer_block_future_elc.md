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
- Since 0.1.10.33 an instant sale to the market may name a limit under the price band (it is paid what the market's bid pays); orders that rest in the book stay within the band.
- **Treasury**: the market sells only what it bought before, plus a small daily import at a higher price. When a commodity runs short, the market posts a bounty for it (Bounties tab) and, while a normal or advanced bounty needs it, sells it at no less than the bounty's reward per item (since 0.1.10.33; before, it did not sell it at all), so buying it to deliver earns nothing.
- Since 0.1.10.34 the market buys a few goods back for much less than it sells them (their bid price): carbon plate, refined iron ingots, circuit boards and advanced alloy ingots, which are cheap to make from other goods of the market, so buying the materials and selling the product back earns little.
- **Commissions**: ask other players for items and pay coins, items or UU-matter. The reward is held until delivery. When several players ask for the same item, it gets listed on the exchange.
- Since 0.1.10.33 a commission with an item or UU-matter reward takes deliveries in whole lots, so every lot pays whole reward items: 64 items for 16 diamonds take lots of 4 items (1 diamond each), or all that is left. The tooltip of the row shows the lot.
- **Futures**: cash-settled contracts for 3 or 7 days. The contract price already includes the expected price movement (events too), so futures only pay off when you foresee more than the market.
- **Mailbox**: what the exchange owes you (sales, refunds, items that did not fit). Collect it in the Orders and Mail tab.
- **Sell page**: put items in the sell box (shift-click, or Fill takes every sellable stack outside the hotbar). The page shows what each commodity pays now and at the daily auction. Sell now, or give the items to the auction: it sells them together with everyone's at the change of the market day, so the order of the sellers does not matter. What stays in the box comes back when you close the panel.
- **Bounties**: the market posts system bounties in three tiers. Normal: a commodity it lacks, every day, for 3 days, reward 1.15 times the value. Advanced: a set of goods, two or three a week, for 7 days, reward 1.3 times; in multiplayer one player may deliver at most half of it until its last day. Special: a rare item, now and then, with a rare reward. Rewards are coins, goods of the market's stock, UU-matter or items made within a weekly limit; rare items delivered to special bounties go to a vault and pay later ones. Press Deliver to hand over what you carry: you are paid at once in proportion, or by contribution when the bounty says so. In single player you can complete every bounty alone.
- Since 0.1.10.33 the daily requests skip items that do not stack and ask at most two stacks of items that stack below 64; datapack bounty tables ask at most eight inventory slots of an item. A delivery that hands over nothing, or not all, says what is still needed.
- **Days and rarity** (since 0.1.10.34): each bounty has a rarity that sets how long it stays open: common 3-7 days (the daily requests), uncommon 2-5, rare 1-4, legendary 1-3 (special bounties). A large one gets at least 2 days from 8,000 coins of value, 3 from 16,000 and so on. A bounty open for fewer than 4 days pays a rush bonus of 5 % for each missing day (1 day: +15 %). The row shows the rarity and the bonus. Requests are valued halfway between the market's selling and buying price (before: the selling price).
- **Claim** (since 0.1.10.34): Claim, under Deliver, reserves a normal or advanced bounty: only you and your team can deliver to it. You hold one claim at a time; a bounty somebody delivered to already cannot be claimed; a claim with one day left adds a day (once per bounty). Abandon gives it back, and you cannot claim again that market day. The claim of a player away for two whole market days is given back at the next change of day. Special bounties cannot be claimed.
- **Refresh** (since 0.1.10.34, top right of the board): replaces the normal requests that nobody claimed or delivered to with as many new ones, never the same items. It costs 5 % of their rewards, at least 500 coins, and twice as much each further time: three times a market day per player, 5 minutes apart. When nothing new could be posted it is refused before you pay.
- **More players, more bounties** (since 0.1.10.34): the board grows with the active players (online, or used the exchange in the last three market days): normal requests open 3-5 with one player, 5-7 with two, 8-10 with four, 14-16 with eight (up to 16-18), more advanced bounties a week, more special ones at a time, and higher weekly limits of made items and UU-matter. During the day, when requests were completed, the board posts a new one after 6 minutes with one player online, 4 with two, 2.4 with four (at least 1 minute), up to 6 normal postings a market day and 4 more for each other active player. The budget tooltip shows the players, the postings and the next refill.
- **Settlement**: put a shulker box, a backpack, an AE2 storage cell or a container of UU-matter into the slot of the Settlement page. Its commodities sell now or at the daily auction like the sell box, its plain items deliver to bounties and commissions, its UU-matter goes onto your account, and purchases of the selected market can go straight into it. The container comes back when you close the panel. AE2 is optional.
- **UU-matter board**: players trade UU-matter of their accounts for coins on the market "UU-matter (lots of 100 mB)": one unit is 100 mB, the price is in coins per 100 mB, with the usual order book, fee and resting orders. The market itself never buys or sells UU-matter, so the board cannot turn energy into coins.
- **Scrap sink**: with "Rest to scrap" on the sell or settlement page, what an instant sale leaves goes to the recycler when the market pays almost nothing for it (the next bid below 5 % of the price or below 1 coin): scrap at the IC2 recycler's rate, 1 for 8 items. The coins of the sale stay the same.
- **Stabiliser** (off unless the server turns it on): when the market creates more coins in 7 days than its target, the sell depths and the daily allowance shrink a little each day; small sales keep nearly their price. The Treasury tab shows its state.
- **ID card**: use a blank card to make it your personal card; using it anywhere opens your account (mailbox, orders, auction orders, cancel; refunds go to the mailbox) and the delegate cards you wrote at your Trade Terminals.
- **Coin account**: the coins the exchange keeps for you (the coins of the mailbox). Deposit the coins of the inventory or of the settlement container, and withdraw them, in the Orders and Mail tab (amount 0 = all). Purchases, buy orders, commissions and futures collateral take from the inventory and the account together. In bank mode sales and payouts go to the account and payments take from it first, so large trades do not fill the inventory.
- **Minimum price**: the sell and settlement pages take a minimum, in percent of the bid without pressure or in coins per item. An instant sale sells of each commodity the most items whose own price is still at least the minimum, in one batch, and pays the sum at once; the page shows what each commodity sells for before you click. In the order panel a sale can name its limit in percent (the c / % button), and the panel shows what the market part of the sale pays now.
- **Number fields**: a click selects the number, so typing replaces it; the mouse wheel and the arrow keys change it (Shift: 64, Ctrl: 10).
- **Small windows** (since 0.1.10.33): the panel fits every window from 426 x 240 (after the GUI scale). The account line (market day, coins, account, UU-matter) is the status bar at the bottom; long lists scroll with the mouse wheel and say how many entries are not shown; a text that does not fit uses short numbers (1.73M) and shows all of it as tooltip, and a number is never cut. On narrow windows the tabs use short names, the last trades are not shown and the bounty budgets are the tooltip of the board title.

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
- Since 0.1.10.33 the market day counts the days the world lived: setting the world's time back moves it one day on (before, the market day went back with it and its bounties, orders and limits broke). /mio_icif market day shows the market day, the world day and the latest record; /mio_icif market day repair (it also runs at every load) moves a market that an older version set back up to its latest record, together with the records made since, which keep their remaining days.
- Operators can list, post and cancel bounties and see the datapack problems and the economy settings with /mio_icif bounty. Pack makers set bounty tables, reward pools, listings that are open from the start and economy defaults in the datapack folder data/(namespace)/si_exchange; a value changed in the server config wins.
- Since 0.1.10.34 the server config (or economy.json) sets bountyRandomDays (off: the fixed days of the tiers, no rush bonus), bountyMinDays and bountyMaxDays (the range of the drawn days, 1-7) and bountyAcceptMax (claims a player may hold, 1).

## Recipe

<RecipesFor id="mio_icif:producer/block_future_elc" fallbackText="-" />
