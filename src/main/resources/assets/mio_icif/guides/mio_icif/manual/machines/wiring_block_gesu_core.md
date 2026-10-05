---
navigation:
  title: "GESU Core"
  icon: mio_icif:wiring/block_gesu_core
  parent: power.md
  position: 13
item_ids:
  - mio_icif:wiring/block_gesu_core
---

# GESU Core

<Row>
  <BlockImage id="mio_icif:wiring/block_gesu_core" scale="3" />
</Row>

## Function

The GESU Core is the center of the GESU multiblock and stores EU.
The structure forms when each of the six faces touches a GESU input or output module.
Input modules give the input rate. Output modules give the output rate.

## Power data

| Item | Value |
|---|---|
| Energy storage | 2,147,483,647 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the GESU Core.
2. Place at least one GESU Input Module on a face of the core.
3. Place GESU Output Modules on the other faces of the core.
4. Connect the generator cables to the input modules.
5. Connect the machine cables to the output modules.
6. Open the GUI of the core to check the module count.

## Notes

- The structure is a cross of seven blocks: the core and six modules.
- If you remove one module, the GESU stops until you replace the module.
- The GUI has one slot to charge an item and one slot to take EU from a battery.

## Recipe

<RecipesFor id="mio_icif:wiring/block_gesu_core" fallbackText="-" />
