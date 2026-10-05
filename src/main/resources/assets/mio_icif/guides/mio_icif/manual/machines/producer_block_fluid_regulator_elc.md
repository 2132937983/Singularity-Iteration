---
navigation:
  title: "Fluid Flow Regulator"
  icon: mio_icif:producer/block_fluid_regulator_elc
  parent: machines.md
  position: 28
item_ids:
  - mio_icif:producer/block_fluid_regulator_elc
---

# Fluid Flow Regulator

<Row>
  <BlockImage id="mio_icif:producer/block_fluid_regulator_elc" scale="3" />
</Row>

## Function

The Fluid Flow Regulator stores fluid in its tank and outputs the fluid at the front face at a set rate.
The GUI sets the amount and the unit, per second or per tick. Each transfer costs EU.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | EV (2,048 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 10,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an EV cable to the machine.
2. Pump the fluid into the regulator, or put a filled container into the input slot.
3. Place the target tank or pipe at the front face.
4. Set the flow rate in the GUI.

## Notes

- With a flow rate of zero, the regulator moves no fluid.

> **WARNING:** The machine explodes on overvoltage above EV.


## Recipe

<RecipesFor id="mio_icif:producer/block_fluid_regulator_elc" fallbackText="-" />
