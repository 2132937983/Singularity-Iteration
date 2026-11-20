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

**Principle:** The compaction work of a hydraulic press is W = ∫P dV. Harder materials need a higher pressure and a longer stroke. The power of a hydraulic press equals pressure times flow, so each recipe has its own operation time and power.

| Recipe type | Time × power | Energy per operation |
|---|---|---|
| Soft materials (Snow Block etc.) | 100 tick × 5 EU/t | 500 EU |
| Dense plates | 400 tick × 20 EU/t | 8,000 EU |
| Industrial Diamond | 600 tick × 30 EU/t | 18,000 EU |
| Recipes without values | 300 tick × 2 EU/t | 600 EU |

The GUI draws a side view of the hydraulic press. The ram presses down and returns with the progress.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 18,600 EU |
| Use while working | By recipe (default 2 EU/t) |
| Operation time | By recipe (default 300 tick) |
| Energy per operation | By recipe (default 600 EU) |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

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
