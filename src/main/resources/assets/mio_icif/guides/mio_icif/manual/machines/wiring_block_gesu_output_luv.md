---
navigation:
  title: "GESU Output Module (LuV)"
  icon: mio_icif:wiring/block_gesu_output_luv
  parent: power.md
  position: 16
item_ids:
  - mio_icif:wiring/block_gesu_output_luv
---

# GESU Output Module (LuV)

<Row>
  <BlockImage id="mio_icif:wiring/block_gesu_output_luv" scale="3" />
</Row>

## Function

The GESU Output Module (LuV) takes EU from the GESU Core and sends the EU to cables at LuV.
The module is a structural part. The module works only in a complete GESU structure.
Each output module adds to the output rate of the core.

## Power data

| Item | Value |
|---|---|
| Energy storage | 65,536 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the module directly on a face of the GESU Core.
2. Fill the other faces of the core with modules.
3. Connect an LuV cable to the outer faces of the output module.
4. Put a transformer between the cable and each machine with a lower voltage tier.

## Notes


> **WARNING:** The output is LuV. A machine with a lower voltage tier explodes on overvoltage.


## Recipe

<RecipesFor id="mio_icif:wiring/block_gesu_output_luv" fallbackText="-" />
