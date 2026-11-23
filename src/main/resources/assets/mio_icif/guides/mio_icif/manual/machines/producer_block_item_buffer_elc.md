---
navigation:
  title: "Item Buffer"
  icon: mio_icif:producer/block_item_buffer_elc
  parent: machines.md
  position: 33
item_ids:
  - mio_icif:producer/block_item_buffer_elc
---

# Item Buffer

<Row>
  <BlockImage id="mio_icif:producer/block_item_buffer_elc" scale="3" />
</Row>

## Function

The Item Buffer stores items in two halves of 24 slots each (left and right in the screen). The block uses no EU.
- The top and bottom faces reach all 48 slots, to put items in and to take them out.
- A side face puts items into the right half only. It takes items from the right half first, and from the left half once the right half is empty.
- The two upgrade slots take Ejector and Pulling Upgrades. Each upgrade works on all 48 slots.

## Power data

This block uses no EU.

## Procedure

1. Place the Item Buffer between two inventories.
2. Pipe items in through a side or the top, and take them out with a hopper or pipe under the buffer (or through a side).
3. Without pipes, put a Pulling Upgrade in one upgrade slot and an Ejector Upgrade in the other. To choose a side, hold the upgrade and right-click that face of an ordinary block (not a machine); right-clicking the same face again makes it any side.

## Recipe

<RecipesFor id="mio_icif:producer/block_item_buffer_elc" fallbackText="-" />
