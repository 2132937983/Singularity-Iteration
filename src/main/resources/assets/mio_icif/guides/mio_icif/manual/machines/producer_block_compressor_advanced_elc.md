---
navigation:
  title: "Advanced High-Pressure Compressor"
  icon: mio_icif:producer/block_compressor_advanced_elc
  parent: machines.md
  position: 2
item_ids:
  - mio_icif:producer/block_compressor_advanced_elc
---

# Advanced High-Pressure Compressor

<Row>
  <BlockImage id="mio_icif:producer/block_compressor_advanced_elc" scale="3" />
</Row>

## Function

The Advanced High-Pressure Compressor uses the Compressor recipes at MV.

**Principle:** The machine has a hydraulic accumulator. The oil pump charges the accumulator from the grid. At the full pressure of 32 MPa it stores about 20,000 EU. During a press stroke the accumulator releases its pressure, so the press power can be much higher than the grid input.

**Press stroke:** Each stroke does the same work as the Compressor, but takes only one quarter of the recipe time, at 4 times the recipe power. Examples: dense plates 100 tick × 80 EU/t, Industrial Diamond 150 tick × 120 EU/t. Recipes without values: 75 tick × 8 EU/t.

**Charge threshold:** A stroke starts only when the accumulator holds enough energy for the whole stroke (the oil pressure reaches what this stroke needs). Once started, it finishes. When the energy is not enough, the status shows "Charging accumulator". With a strong enough supply it is 4 times as fast as the Compressor. With a weak supply, it runs at the rate at which the oil pump restores the pressure.

The GUI draws the hydraulic press and the accumulator oil bottle. The oil level is the current oil pressure. The red line is the oil pressure the next stroke needs.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage (accumulator) | 20,000 EU (full pressure 32 MPa) |
| Press power | 4 times the recipe (default 8 EU/t) |
| Operation time | 1/4 of the recipe (default 75 tick) |
| Energy per operation | Same as the Compressor (default 600 EU) |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

## Procedure

1. Connect an MV cable to the machine.
2. Put the item to compress into the input slot.
3. Install Overclocker Upgrades to process faster.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_compressor_advanced_elc" fallbackText="-" />
