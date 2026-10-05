---
navigation:
  title: "Canner"
  icon: mio_icif:producer/block_canner_elc
  parent: machines.md
  position: 15
item_ids:
  - mio_icif:producer/block_canner_elc
---

# Canner

<Row>
  <BlockImage id="mio_icif:producer/block_canner_elc" scale="3" />
</Row>

## Function

The Canner uses EU in four modes. Canning mode puts an item into a container, for example food into a Tin Can.
The other modes empty cells into the tank, fill cells from the tank, or mix a fluid with a solid.
Mix mode makes Coolant from Distilled Water and lapis dust.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 800 EU |
| Use while working | 4 EU/t |
| Operation time | 200 tick (10 s) |
| Energy per operation | 800 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an LV cable to the machine.
2. Select the mode in the GUI.
3. Put the container and the material into the input slots, or pump the fluid in.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_canner_elc" fallbackText="-" />
