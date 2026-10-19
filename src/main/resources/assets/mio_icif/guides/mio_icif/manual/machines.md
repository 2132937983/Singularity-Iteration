---
navigation:
  title: "Machines"
  icon: mio_icif:producer/block_powder_elc
  parent: index.md
  position: 11
item_ids:
  - mio_icif:normal/item_recovered_parts
---

# Machines

Machines use EU to process items, fluids and blocks.
Each page gives the input voltage tier, the use while working and the operation time.

## Picked-up machines as materials

A machine picked up with the wrench keeps its inventory, upgrades included. When such a machine is a crafting material (for example for the next tier), its items come back: one stack as it is, several stacks in a <ItemLink id="mio_icif:normal/item_recovered_parts" /> that unpacks itself in your inventory. This also works in a Crafter and in AE2 molecular assemblers.

## Overclock slider

- When a machine has at least one Overclocker Upgrade, its screen shows an OC slider at the bottom of the left side panel.
- Drag the knob, click the track or use the mouse wheel over it to set how many of the installed overclockers work. The text under the track shows working / installed.
- The tooltip shows the speed (progress per tick and ticks per operation) and the energy (factor and EU/t) of the setting. Values that start with ~ are an estimate until the machine runs with the new setting.
- The machine saves the setting; it stays when the chunk unloads. The energy storage bonus of all installed overclockers stays at every setting.

## Ejector and Pulling upgrades

- An ejector without a direction tries storage first: chests, barrels, shulker boxes, the storage crates of this mod and every block of the block tag mio_icif:automation/storage. Other blocks come next, machines last (block tag mio_icif:automation/processor marks more machines).
- A machine does not send items back to a machine that it pulled from in the last 10 seconds, and it does not pull from a machine that it sent items to. It also does not send items into a machine whose ejector points back at it. Two machines with pulling and ejector upgrades next to a chest no longer pass the same items back and forth.
- Pulling from a chest and ejecting into the same chest still works.
- After a run that moved nothing, the ejector and pulling upgrades (items and fluids) wait 4, 8, 16, then 20 ticks before they try again. A change of the machine's own inventory or tanks makes them try at the next tick. While something moves they run every tick. The config option Performance / automationPacing switches this off.
- Idle sleep: a macerator, electric furnace, extractor, compressor, recycler or metal former that stood idle for 1 second stops checking its recipe. A change of its inventory, its settings or its stored energy wakes it at the next tick, and it checks again at the latest after 1 second. Its upgrades keep working while it sleeps. The config option Performance / machineSleep switches this off.

## Fluid containers

- The container slots of fluid machines take every container that holds the right fluid: buckets, partly filled cells, and fluid containers of other mods.
- The machine takes only what fits into its tank. The empty container goes to the empty-container slot, never lost.

<SubPages id="machines.md" icons={true} alphabetical={true} />
