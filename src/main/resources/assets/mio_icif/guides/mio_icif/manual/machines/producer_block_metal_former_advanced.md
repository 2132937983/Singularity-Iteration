---
navigation:
  title: "Advanced Metal Former"
  icon: mio_icif:producer/block_metal_former_advanced
  parent: machines.md
  position: 5
item_ids:
  - mio_icif:producer/block_metal_former_advanced
---

# Advanced Metal Former

<Row>
  <BlockImage id="mio_icif:producer/block_metal_former_advanced" scale="3" />
</Row>

## Function

The Advanced Metal Former uses the Metal Former recipes at MV.
Rolling mode makes plates and casings. Cutting mode makes cables from plates. Extruding mode makes cables, shafts and cans.
The block texture shows the current mode.

The operation time is one fifth of the recipe time (rounded up), and the power is fixed at 50 EU/t. So each operation uses the same energy as the Metal Former and is 5 times as fast. Example: rolling an iron plate into an iron casing takes 60 tick.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 1,000 EU |
| Use while working | 50 EU/t |
| Operation time | 1/5 of the recipe time (default 40 tick) |
| Energy per operation | Same as the Metal Former (default 2,000 EU) |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

## Procedure

1. Connect an MV cable to the machine.
2. Click the mode button in the GUI to select Rolling, Cutting or Extruding.
3. Put the ingot or the plate into the input slot.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_metal_former_advanced" fallbackText="-" />
