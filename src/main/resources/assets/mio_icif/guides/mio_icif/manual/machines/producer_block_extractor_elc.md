---
navigation:
  title: "Extractor"
  icon: mio_icif:producer/block_extractor_elc
  parent: machines.md
  position: 25
item_ids:
  - mio_icif:producer/block_extractor_elc
---

# Extractor

<Row>
  <BlockImage id="mio_icif:producer/block_extractor_elc" scale="3" />
</Row>

## Function

The Extractor uses EU to extract materials from items. Sticky Resin becomes Rubber.
The machine has one input slot, one output slot, one battery slot and four upgrade slots.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 600 EU |
| Use while working | 2 EU/t |
| Operation time | 300 tick (15 s) |
| Energy per operation | 600 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an LV cable to the machine.
2. Put the item into the input slot.
3. Install Overclocker Upgrades to process faster.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_extractor_elc" fallbackText="-" />
