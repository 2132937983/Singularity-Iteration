---
navigation:
  title: "Energy Converter"
  icon: mio_icif:energy_converter/energy_converter
  parent: machines.md
  position: 24
item_ids:
  - mio_icif:energy_converter/energy_converter
---

# Energy Converter

<Row>
  <BlockImage id="mio_icif:energy_converter/energy_converter" scale="3" />
</Row>

## Function

The Energy Converter converts energy between EU and FE. The rate is 1 EU = 4 FE.
In EU to FE mode, the machine receives EU and outputs FE at the front face.
In FE to EU mode, the machine takes FE from the other five faces and outputs EU to the cable network at HV.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 2,048 EU/t |
| Energy storage | 100,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the converter with the front face toward the target.
2. Sneak and right-click the converter to switch the mode.
3. Connect the source to the other faces.

## Notes


> **WARNING:** The machine explodes on overvoltage above HV.


## Recipe

<RecipesFor id="mio_icif:energy_converter/energy_converter" fallbackText="-" />
