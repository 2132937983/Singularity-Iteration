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
The machine heats up during operation. Higher heat gives a higher speed. Without power, the heat drops.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 10,000 EU |
| Use while working | 15 EU/t |
| Operation time | 4,000 tick (200 s) |
| Energy per operation | 60,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an MV cable to the machine.
2. Put the items to smelt into the two input slots.
3. Keep the machine busy to hold the heat.
4. Take the results from the two output slots.

## Notes

- A redstone signal keeps the machine hot and pauses the process.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Heat hold

The heat hold switch (flame button in the side dock) keeps the furnace heating while it has nothing to smelt, up to full heat, and holds it there. The next item smelts at full speed at once. Holding full heat costs 1 EU/t.

A <ItemLink id="mio_icif:upgrade/thermal_retention_upgrade" /> in an upgrade slot stops the heat loss of an idle furnace and makes the hold free. The heat gauge above the slots shows HOLD and INS for the two.

## Recipe

<RecipesFor id="mio_icif:producer/block_induction_elc" fallbackText="-" />
