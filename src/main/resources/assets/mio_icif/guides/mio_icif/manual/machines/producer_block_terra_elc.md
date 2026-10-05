---
navigation:
  title: "Terraformer"
  icon: mio_icif:producer/block_terra_elc
  parent: machines.md
  position: 65
item_ids:
  - mio_icif:producer/block_terra_elc
---

# Terraformer

<Row>
  <BlockImage id="mio_icif:producer/block_terra_elc" scale="3" />
</Row>

## Function

The Terraformer uses EU to change the terrain around the machine with a Terraformer Template.
Templates include Cultivation, Desert, Irrigation, Chilling, Flatification and Mushroom.
The machine has no GUI. The default radius is 128 blocks. The server configuration sets the radius.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 100,000 EU |
| Use while working | 100 EU/t |

The game measured these values from the block entity of this version.

## Procedure

1. Place the Terraformer in the center of the area to change.
2. Connect an HV cable to the machine.
3. Right-click the machine with a template to insert the template.
4. Sneak and right-click the machine to remove the template.

## Notes


> **WARNING:** The terrain changes are permanent. Test the template in an area without buildings.

- A redstone signal stops the machine.

> **WARNING:** The machine explodes on overvoltage above HV.


## Recipe

<RecipesFor id="mio_icif:producer/block_terra_elc" fallbackText="-" />
