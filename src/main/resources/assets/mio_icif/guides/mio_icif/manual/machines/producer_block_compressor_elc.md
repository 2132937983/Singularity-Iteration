---
navigation:
  title: "Compressor"
  icon: mio_icif:producer/block_compressor_elc
  parent: machines.md
  position: 17
item_ids:
  - mio_icif:producer/block_compressor_elc
---

# Compressor

<Row>
  <BlockImage id="mio_icif:producer/block_compressor_elc" scale="3" />
</Row>

## Function

The Compressor uses EU to compress items. Dusts become plates, ingots become blocks, and Empty Cells become Compressed Air Cells.

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
2. Put the item to compress into the input slot.
3. Install Overclocker Upgrades to process faster.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_compressor_elc" fallbackText="-" />
