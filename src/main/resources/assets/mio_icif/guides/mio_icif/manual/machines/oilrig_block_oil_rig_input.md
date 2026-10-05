---
navigation:
  title: "Oil Rig Input Module"
  icon: mio_icif:oilrig/block_oil_rig_input
  parent: heavy.md
  position: 7
item_ids:
  - mio_icif:oilrig/block_oil_rig_input
---

# Oil Rig Input Module

<Row>
  <BlockImage id="mio_icif:oilrig/block_oil_rig_input" scale="3" />
</Row>

## Function

The Oil Rig Input Module receives EU from cables and stores the EU for the rig. The core takes EU from all Input Modules.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 50,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Put the Input Module in a module position of the rig.
2. Connect an HV cable to the module.
3. Add more Input Modules to store more EU.

## Notes

- The rig needs at least one Input Module.

## Recipe

<RecipesFor id="mio_icif:oilrig/block_oil_rig_input" fallbackText="-" />
