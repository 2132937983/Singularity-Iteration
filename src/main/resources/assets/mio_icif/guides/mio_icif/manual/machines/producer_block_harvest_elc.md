---
navigation:
  title: "Harvester"
  icon: mio_icif:producer/block_harvest_elc
  parent: machines.md
  position: 30
item_ids:
  - mio_icif:producer/block_harvest_elc
---

# Harvester

<Row>
  <BlockImage id="mio_icif:producer/block_harvest_elc" scale="3" />
</Row>

## Function

The Harvester collects ripe crops from the crop sticks in a 9 x 3 x 9 area around the machine.
The machine puts the drops into its storage slots.
With a Crop Analyzer installed, the machine harvests only at the optimal size.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 10,000 EU |
| Use while working | 1 EU/t |
| Operation time | 10 tick (0.5 s) |
| Energy per operation | 10 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the machine in the center of the crop field.
2. Connect an LV cable to the machine.
3. Put a Crop Analyzer into the analyzer slot to harvest at the optimal size.
4. Take the crops from the storage slots.

## Notes

- The Crop Analyzer increases the EU cost of each harvest.
- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_harvest_elc" fallbackText="-" />
