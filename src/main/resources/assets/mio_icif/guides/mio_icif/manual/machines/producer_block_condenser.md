---
navigation:
  title: "Condenser"
  icon: mio_icif:producer/block_condenser
  parent: machines.md
  position: 18
item_ids:
  - mio_icif:producer/block_condenser
---

# Condenser

<Row>
  <BlockImage id="mio_icif:producer/block_condenser" scale="3" />
</Row>

## Function

The Condenser turns Steam or Superheated Steam into Distilled Water.
Heat Vents in the four vent slots increase the speed. Each Heat Vent uses EU.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 10,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Pump steam into the Condenser, or place the Condenser next to a Steam Kinetic Generator.
2. Put Heat Vents into the vent slots to increase the speed.
3. Connect an HV cable to the machine when Heat Vents are installed.
4. Take the Distilled Water with a pipe, or put an empty bucket or cell into the container slot.

## Notes

- Without Heat Vents the Condenser uses no EU.

> **WARNING:** The machine explodes on overvoltage above HV.


## Recipe

<RecipesFor id="mio_icif:producer/block_condenser" fallbackText="-" />
