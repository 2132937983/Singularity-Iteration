---
navigation:
  title: "Crop Manager"
  icon: mio_icif:producer/block_matron_elc
  parent: machines.md
  position: 19
item_ids:
  - mio_icif:producer/block_matron_elc
---

# Crop Manager

<Row>
  <BlockImage id="mio_icif:producer/block_matron_elc" scale="3" />
</Row>

## Function

The Crop Manager tends the crop sticks in a 9 x 3 x 9 area around the machine.
The machine adds Fertilizer, water and Weed-EX to each crop stick.

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
3. Put Fertilizer into the fertilizer slots.
4. Put Crop Manager Herbicide into the herbicide slots.
5. Supply water with Water Cells or with a pipe.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_matron_elc" fallbackText="-" />
