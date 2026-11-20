---
navigation:
  title: "Induction Furnace"
  icon: mio_icif:producer/block_induction_elc
  parent: machines.md
  position: 31
item_ids:
  - mio_icif:upgrade/thermal_retention_upgrade
  - mio_icif:producer/block_induction_elc
---

# Induction Furnace

<Row>
  <BlockImage id="mio_icif:producer/block_induction_elc" scale="3" />
</Row>

## Function

The Induction Furnace uses EU to smelt two items at the same time with the furnace recipes.

**Principle:** An induction coil heats the melt pool in the crucible. The heating power is up to 64 EU/t. Heating from room temperature to 1,600 °C takes about 30 seconds. The melt pool loses heat according to its temperature: 8 EU/t at full temperature. Once at temperature, the heater only makes up for the heat loss. Without heating, the melt pool cools with a time constant of about 4 minutes.

**Smelting speed:** Smelting starts at 600 °C. A higher temperature is faster, and 1,600 °C is full speed: each of the two slots smelts one item every 12 tick. Smelting uses another 15 EU/t. Overclocking speeds up smelting, not heating.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 10,000 EU |
| Heating power | Up to 64 EU/t |
| Smelting use | 15 EU/t |
| Speed at full temperature | One item per slot every 12 tick |
| Start / full-speed temperature | 600 °C / 1,600 °C |
| Heat loss at full temperature | 8 EU/t (2 EU/t with the Thermal Retention Unit) |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

## Procedure

1. Connect an MV cable to the machine.
2. Put the items to smelt into the two input slots.
3. Keep the machine busy to hold the heat.
4. Take the results from the two output slots.

## Notes

- A redstone signal keeps the machine hot and pauses the process.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Heat hold

When the "Heat hold" button in the GUI is on, the machine heats the melt pool to 1,600 °C and holds it there even when it has nothing to smelt. The next item smelts at full speed at once. While holding, the heater only makes up for the heat loss, about 8 EU/t at full temperature.
With a <ItemLink id="mio_icif:upgrade/thermal_retention_upgrade" /> in an upgrade slot, the heat loss drops to one quarter, and holding costs about 2 EU/t.

## Recipe

<RecipesFor id="mio_icif:producer/block_induction_elc" fallbackText="-" />
