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

The left side of the GUI is the input tank and the right side is the output tank, 8,000 mB each. Moving dots show on the pipes that the current mode uses. The "Switch mode" and "Swap" buttons are in the GUI. "Swap" exchanges the two tanks. Each recipe runs at its own operation time and power.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 800 EU |
| Use while working | By recipe (default 4 EU/t) |
| Operation time | By recipe (default 200 tick) |
| Energy per operation | By recipe (default 800 EU) |
| Tanks | Input and output, 8,000 mB each |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

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
