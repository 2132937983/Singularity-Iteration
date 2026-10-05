---
navigation:
  title: "Recycler"
  icon: mio_icif:producer/block_recycler_elc
  parent: machines.md
  position: 56
item_ids:
  - mio_icif:producer/block_recycler_elc
---

# Recycler

<Row>
  <BlockImage id="mio_icif:producer/block_recycler_elc" scale="3" />
</Row>

## Function

The Recycler uses EU to turn items into Scrap. Each operation gives Scrap only by chance.
The machine consumes the input item in every operation.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 45 EU |
| Use while working | 1 EU/t |
| Operation time | 45 tick (2.2 s) |
| Energy per operation | 45 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an LV cable to the machine.
2. Put the unwanted items into the input slot.
3. Install Overclocker Upgrades to process faster.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_recycler_elc" fallbackText="-" />
