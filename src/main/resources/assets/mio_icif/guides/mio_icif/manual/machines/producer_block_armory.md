---
navigation:
  title: "Armory"
  icon: mio_icif:producer/block_armory
  parent: machines.md
  position: 9
item_ids:
  - mio_icif:producer/block_armory
---

# Armory

<Row>
  <BlockImage id="mio_icif:producer/block_armory" scale="3" />
</Row>

## Function

The Armory stores six suits. Each suit has six pieces: helmet, chestplate, leggings, boots, main hand and off hand.
The Armory flies a stored suit to its owner on command.
The Armory repairs and charges the stored pieces with EU. Botania gear uses mana from the Armory mana tank.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 1,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an HV cable to the Armory.
2. Right-click the powered Armory to become its owner.
3. Put your gear into the suit slots in the GUI.
4. Sneak and right-click the Armory with an Armory Remote Controller to pair the remote.
5. Wear the remote in a charm or belt accessory slot.
6. Open the Armory section of the Equipment Console and click SUMMON.

## Notes

- The Armory needs stored EU before it accepts an owner. Only the owner can open, use or break a bound Armory.
- Equipment from other mods needs a Connector Kit before the Armory accepts the equipment.
- Each summon costs EU. The cost increases with the distance and with a change of dimension.

## Recipe

<RecipesFor id="mio_icif:producer/block_armory" fallbackText="-" />
