---
navigation:
  title: "GESU Input Module (IV)"
  icon: mio_icif:wiring/block_gesu_input_iv
  parent: power.md
  position: 14
item_ids:
  - mio_icif:wiring/block_gesu_input_iv
---

# GESU Input Module (IV)

<Row>
  <BlockImage id="mio_icif:wiring/block_gesu_input_iv" scale="3" />
</Row>

## Function

The GESU Input Module receives EU from cables and moves the EU into the GESU Core.
The module is a structural part. The module works only in a complete GESU structure.
Each input module adds to the input rate of the core.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MAX (2,147,483,648 EU) |
| Maximum input | 2,147,483,647 EU/t |
| Energy storage | 4,294,967,294 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the module directly on a face of the GESU Core.
2. Fill the other faces of the core with modules.
3. Connect the generator cable to the outer faces of the input module.
4. Add more input modules to raise the input rate.

## Recipe

<RecipesFor id="mio_icif:wiring/block_gesu_input_iv" fallbackText="-" />
