---
navigation:
  title: "GESU Output Module (IV)"
  icon: mio_icif:wiring/block_gesu_output_iv
  parent: power.md
  position: 15
item_ids:
  - mio_icif:wiring/block_gesu_output_iv
---

# GESU Output Module (IV)

<Row>
  <BlockImage id="mio_icif:wiring/block_gesu_output_iv" scale="3" />
</Row>

## Function

The GESU Output Module (IV) takes EU from the GESU Core and sends the EU to cables at IV.
The module is a structural part. The module works only in a complete GESU structure.
Each output module adds to the output rate of the core.

## Power data

| Item | Value |
|---|---|
| Energy storage | 16,384 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the module directly on a face of the GESU Core.
2. Fill the other faces of the core with modules.
3. Connect an IV cable to the outer faces of the output module.
4. Put a transformer between the cable and each machine with a lower voltage tier.

## Notes


> **WARNING:** The output is IV. A machine with a lower voltage tier explodes on overvoltage.


## Recipe

<RecipesFor id="mio_icif:wiring/block_gesu_output_iv" fallbackText="-" />
