---
navigation:
  title: "Advanced Cyclone Macerator"
  icon: mio_icif:producer/block_powder_advanced_elc
  parent: machines.md
  position: 0
item_ids:
  - mio_icif:producer/block_powder_advanced_elc
---

# Advanced Cyclone Macerator

<Row>
  <BlockImage id="mio_icif:producer/block_powder_advanced_elc" scale="3" />
</Row>

## Function

The Advanced Cyclone Macerator uses the Macerator recipes at MV.
The machine completes each operation in less time than the Macerator.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 750 EU |
| Use while working | 5 EU/t |
| Operation time | 150 tick (7.5 s) |
| Energy per operation | 750 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an MV cable to the machine.
2. Put the ore or the item to grind into the input slot.
3. Install Overclocker Upgrades to process faster.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_powder_advanced_elc" fallbackText="-" />
