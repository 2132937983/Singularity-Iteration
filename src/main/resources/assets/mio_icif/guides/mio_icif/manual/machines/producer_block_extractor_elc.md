---
navigation:
  title: "Extractor"
  icon: mio_icif:producer/block_extractor_elc
  parent: machines.md
  position: 25
item_ids:
  - mio_icif:producer/block_extractor_elc
---

# Extractor

<Row>
  <BlockImage id="mio_icif:producer/block_extractor_elc" scale="3" />
</Row>

## Function

The Extractor uses EU to extract materials from items. Sticky Resin becomes Rubber.
The machine has one input slot, one output slot, one battery slot and four upgrade slots.

**Principle:** Screw press. The screw pushes the material toward the outlet. The screw channel gets shallower toward the front, which squeezes out the juice and the usable parts.

**Time and power:** Each recipe runs at its own operation time and power: most recipes 100 tick × 10 EU/t (1,000 EU); gravel, gunpowder, flint dust, netherrack and similar 150 tick × 12 EU/t; emptying a Tin Can 50 tick × 5 EU/t. Recipes without values (Sticky Resin, rubber sapling, Water Cell) take 300 tick × 2 EU/t.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 1,200 EU |
| Use while working | By recipe (default 2 EU/t) |
| Operation time | By recipe (default 300 tick) |
| Energy per operation | By recipe (default 600 EU) |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

## Procedure

1. Connect an LV cable to the machine.
2. Put the item into the input slot.
3. Install Overclocker Upgrades to process faster.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_extractor_elc" fallbackText="-" />
