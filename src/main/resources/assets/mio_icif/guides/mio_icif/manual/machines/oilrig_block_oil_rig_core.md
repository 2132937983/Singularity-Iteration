---
navigation:
  title: "Oil Rig Core"
  icon: mio_icif:oilrig/block_oil_rig_core
  parent: heavy.md
  position: 6
item_ids:
  - mio_icif:oilrig/block_oil_rig_core
---

# Oil Rig Core

<Row>
  <BlockImage id="mio_icif:oilrig/block_oil_rig_core" scale="3" />
</Row>

## Function

The Oil Rig Core controls the oil rig multiblock. The rig drills down to bedrock and produces crude oil.
The drill removes the blocks below the rig. Each removed block can give crude oil to the Output Modules.
The rig stops after the drill finishes a limited area around the core.

## Power data

| Item | Value |
|---|---|
| Energy storage | 50,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the Oil Rig Core and put eight modules around the core on the same layer.
2. Stack two modules on top of the core.
3. Stack three Titanium Drill Frames on top of these two modules.
4. Place two frames on each of the four sides of the stacked modules.
5. Include at least one Input Module and one Output Module.
6. Connect an HV cable to an Input Module.

## Notes

- Right-click the Oil Rig Panel to see the rig status. The core has no GUI.

> **WARNING:** The drill destroys the blocks in its columns and drops no items. Do not build the rig above your base.


## Recipe

<RecipesFor id="mio_icif:oilrig/block_oil_rig_core" fallbackText="-" />
