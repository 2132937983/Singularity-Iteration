---
navigation:
  title: "Drills and Mining Laser"
  icon: mio_icif:item_tool_iridium_driller
  parent: tools.md
  position: 1
item_ids:
  - mio_icif:item_tool_iron_driller
  - mio_icif:item_tool_diamond_driller
  - mio_icif:item_tool_iridium_driller
  - mio_icif:item_tool_laser_miner
  - mio_icif:item_tool_bronze_pickaxe
  - mio_icif:item_tool_bronze_axe
  - mio_icif:item_tool_bronze_shovel
  - mio_icif:item_tool_bronze_hoe
---

# Drills and Mining Laser

## Drills

A drill is an electric pickaxe and shovel. With energy it mines fast and drops blocks; without energy it mines like a hand.

| Drill | Drop tier | Speed | EU per block | Capacity | Areas (G) | Attack in hand |
|---|---|---|---|---|---|---|
| <ItemLink id="mio_icif:item_tool_iron_driller" /> | iron | 8 | 50 EU | 30,000 EU | 1×1 | 6 |
| <ItemLink id="mio_icif:item_tool_diamond_driller" /> | diamond | 16 | 80 EU | 30,000 EU | 1×1, 3×3 | 7 |
| <ItemLink id="mio_icif:item_tool_iridium_driller" /> | netherite | 25 | 800 EU | 1,000,000 EU | 1×1, 3×3, 5×5 | 10 |

- **Area mining.** Each block of the area breaks as the player breaks it and costs one use. A 3×3 mines at half speed, a 5×5 at 0.3. A block much harder than the centre (hardness above centre × 2 + 1) is left. With the view steeper than 45° the drill mines a flat plane, else an upright one.
- **Iridium drill.** Sneak + G switches Fortune III and Silk Touch.
- **Attack.** Each hit costs one use. Without energy there is no attack damage.
- **Miner.** All three drills fit the Miner.

## Mining laser

The <ItemLink id="mio_icif:item_tool_laser_miner" /> mines with a laser. G switches the mode: mining, low focus, long range, horizontal, combat, super heat (smelts the drops), scatter, explosive, 3×3 and nuclear explosive (off on servers by default). The tooltip shows the cost and range of each mode. The laser checks claims block by block.

## Bronze tools

The bronze pickaxe, axe, shovel, hoe and sword are plain tools: iron mining level, 250 uses, repaired with bronze ingots, all their usual enchantments.
