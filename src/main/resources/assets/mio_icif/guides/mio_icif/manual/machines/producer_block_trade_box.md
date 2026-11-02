---
navigation:
  title: "Trade Boxes"
  icon: mio_icif:producer/block_trade_box
  parent: machines.md
  position: 29
item_ids:
  - mio_icif:producer/block_trade_box
  - mio_icif:producer/block_trade_box_advanced
  - mio_icif:normal/item_trade_box_upgrade
---

# Trade Boxes

<Row>
  <BlockImage id="mio_icif:producer/block_trade_box" scale="3" />
  <BlockImage id="mio_icif:producer/block_trade_box_advanced" scale="3" />
</Row>

## Function

A Trade Box sells for its owner, the player who placed it, while its chunk is loaded. It needs no energy.
Every sale is the owner's: the owner's sell depth, daily allowance, unlocks and records, the coins on the owner's coin account. Only the owner and operators open it.

### Basic Trade Box

- **27 slots** that hoppers and pipes fill with commodities, also items of a commodity's tag. Nothing comes out by pipe.
- **Daily auction**: every 10 seconds its commodities join the owner's daily auction order with the box's minimum in percent of the bid (50 % to start). What the auction does not sell goes to the owner's mailbox, as with every auction order.
- Made from a chest, an emerald, iron ingots and redstone: no circuit.

### Advanced Trade Box

- **54 slots**, **9 return slots** that pipes empty and a **filter** of 9 items: click a filter cell with an item on the cursor to take only that commodity; an empty filter takes every commodity, a click with an empty cursor clears a cell. Items the filter does not take move to the return slots.
- **Modes**: Auction (the daily auction), Instant (sold at once along the owner's sell depth) or Scrap rest (sold at once; what the market no longer pays for goes to the scrap sink, and the scrap comes out of the return slots). The minimum is in percent of the bid or, for instant sales, in coins per item (the c / % button).
- **Owner offline**: instant sales follow the Trade Terminal: they run only while the owner is online and wait for a batch of 64 items unless the box is full. With "Offline: auction" on, the box uses the daily auction while the owner is away.
- **Bounties**: with "Bounties" on, the box first delivers to the bounties the owner claimed (Claim on the Bounties tab), within the owner's share. The rewards go to the owner's coin account, UU account and mailbox.
- **Upgrade kit**: used by the owner on a placed Basic Trade Box, it makes an Advanced Trade Box with the same contents and settings.

### Both boxes

- The panel shows the results of the market day (sold at once and their coins, put into the auction, scrapped, delivered to bounties) and the status of the last run.
- Breaking a box sends its contents to the owner's mailbox; items with names, enchantments or damage drop.
- Since 0.1.10.42 the recipe of the Trade Terminal takes an Advanced Trade Box instead of a chest.

## Recipe

<RecipesFor id="mio_icif:producer/block_trade_box" fallbackText="-" />

<RecipesFor id="mio_icif:producer/block_trade_box_advanced" fallbackText="-" />

<RecipesFor id="mio_icif:normal/item_trade_box_upgrade" fallbackText="-" />
