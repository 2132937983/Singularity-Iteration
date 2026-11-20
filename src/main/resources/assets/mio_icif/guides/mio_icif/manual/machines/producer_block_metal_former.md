---
navigation:
  title: "Metal Former"
  icon: mio_icif:producer/block_metal_former
  parent: machines.md
  position: 43
item_ids:
  - mio_icif:producer/block_metal_former
---

# Metal Former

<Row>
  <BlockImage id="mio_icif:producer/block_metal_former" scale="3" />
</Row>

## Function

The Metal Former uses EU to shape metal in three modes.
Rolling mode makes plates and casings. Cutting mode makes cables from plates. Extruding mode makes cables, shafts and cans.

Each recipe runs for its own operation time (for example, rolling an iron plate into an iron casing takes 300 tick). The power is the value set by the recipe, or 10 EU/t if none is set. The GUI draws a diagram of rolling, cutting or extruding for the current mode. The mode button is in the GUI.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 2,000 EU |
| Use while working | By recipe (default 10 EU/t) |
| Operation time | By recipe (default 200 tick) |
| Energy per operation | By recipe (default 2,000 EU) |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

## Procedure

1. Connect an LV cable to the machine.
2. Click the mode button in the GUI to select Rolling, Cutting or Extruding.
3. Put the ingot or the plate into the input slot.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_metal_former" fallbackText="-" />
