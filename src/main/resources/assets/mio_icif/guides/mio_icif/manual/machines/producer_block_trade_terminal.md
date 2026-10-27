---
navigation:
  title: "Trade Terminal"
  icon: mio_icif:producer/block_trade_terminal
  parent: machines.md
  position: 29
item_ids:
  - mio_icif:producer/block_trade_terminal
---

# Trade Terminal

<Row>
  <BlockImage id="mio_icif:producer/block_trade_terminal" scale="3" />
</Row>

## Function

The Trade Terminal trades on the exchange for its owner, the player who placed it, while its chunk is loaded.
Every trade it makes is the owner's: the same rules as a manual trade (sell depth, daily allowance, buy limit, ledger), coins from and to the owner's mailbox.
Each trade costs the EU of one trade, like the Futures Machine. A terminal also starts the new market day, so a base without a Futures Machine still has its daily auction.

- **Sell buffer** (27 slots): pipes and hoppers put commodities in, also items of a commodity's tag. Pipes cannot take anything out of it.
- **Output buffer** (9 slots): with the output mode on, purchases go here and pipes can take them out. With it off, purchases go to the owner's mailbox.
- **Standing orders** (up to 8, every 10 seconds): sell what is above a kept amount, either into the daily auction (with a minimum in percent of the bid) or at once (with a minimum price); or buy up to a target stock at a highest price, paid from the owner's mailbox coins. Instant sales and purchases wait for a batch of 64 items, because every trade rounds its coins. The Orders page shows why an order did not trade.
- **Owner offline**: only auction sell orders run, and not for commodities that need an unlock advancement. Instant sales and purchases wait for the owner.
- **Access**: the owner gives players View, Trade or Configure, and can give the members of the owner's scoreboard team a level. Trade fills the sell buffer and uses the settlement slot; Configure edits orders and takes items out. Operators count as Configure.
- **Settlement slot**: put in a shulker box, a backpack, an AE2 storage cell or a container of UU-matter. Sell its commodities as yourself, put its UU-matter on your account, load it into the sell buffer or unload the output buffer into it. The Settlement page delivers its items to bounties and commissions and buys into it. The Futures Machine has the same slot on its Settlement page. AE2 is optional; the container comes back when you close the panel.
- **Breaking** the terminal sends the buffers to the owner's mailbox; items with names, enchantments or damage drop.
- **Scrap mode**: a sell order in the mode "Scrap rest" sells at once like an instant order and sends what the market no longer pays for to the scrap sink; the scrap goes to the output buffer (output mode) or the owner's mailbox.
- **ID cards**: on the Cards page the owner writes a blank ID card for a player, with a level (view, trade or configure) and up to 30 days. The card gives that player the level at all the owner's terminals; it works only for that player, and the owner revokes it here or with the personal card.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 100 EU/t |
| Energy storage | 10,000 EU |

The values are those of the Futures Machine (the terminal uses its config entries).
