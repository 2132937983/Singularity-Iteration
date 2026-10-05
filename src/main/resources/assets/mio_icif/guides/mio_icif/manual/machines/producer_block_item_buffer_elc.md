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

The Item Buffer stores items in two halves of 24 slots each. The block uses no EU.
The top and bottom faces access the left half. The four side faces access the right half.
Each half has one upgrade slot for an Overclocker, Ejector or Pulling Upgrade.

## Power data

This block uses no EU.

## Procedure

1. Place the Item Buffer between two inventories.
2. Insert items from the top to fill the left half.
3. Install an Ejector Upgrade in the upgrade slot of the half to empty.

## Recipe

<RecipesFor id="mio_icif:producer/block_item_buffer_elc" fallbackText="-" />
