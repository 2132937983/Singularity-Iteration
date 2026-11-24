---
navigation:
  title: "Wrenches and Toolbox"
  icon: mio_icif:item_tool_wrench
  parent: tools.md
  position: 3
item_ids:
  - mio_icif:item_tool_wrench
  - mio_icif:item_tool_wrench_elc
  - mio_icif:normal/item_toolbox
  - mio_icif:normal/item_toolbox_x
  - mio_icif:build/item_obscurator
  - mio_icif:build/item_cf_sprayer
  - mio_icif:build/item_painter
---

# Wrenches and Toolbox

## Wrenches

The <ItemLink id="mio_icif:item_tool_wrench" /> (120 uses) and the <ItemLink id="mio_icif:item_tool_wrench_elc" /> (50,000 EU) work on the blocks of the tag `mio_icif:wrenchable`: machines, cables, pipes, storage boxes, multiblock casings and ports.

| Action | Result |
|---|---|
| Right-click a machine's front, or a block without a facing | Dismantles it; the item keeps its settings and energy |
| Right-click another face | Turns the front to that face |
| Sneak + right-click | Turns the front away from the clicked face |
| Right-click an arm of a cable or pipe | Blocks that side; click the centre to open it again |

- Dismantling loses nothing. A storage box dismantles on a plain right-click.
- Electric wrench: 100 EU to dismantle, 5 to turn, 2 for cables and pipes. The equipment console switches "keep contents" and "dismantle from the front". A flat wrench says so on the action bar; creative needs no energy.
- Other mods' wrenches: sneak + right-click dismantles this mod's blocks.
- An addon block can refuse turning or dismantling, and choose its drops, through `IWrenchable`.

## Toolbox

The <ItemLink id="mio_icif:normal/item_toolbox" /> (9 slots) and the <ItemLink id="mio_icif:normal/item_toolbox_x" /> (27 slots) open on right-click and hold tools only. Each change is saved at once. The open toolbox is locked in its hand.

## Building tools

- <ItemLink id="mio_icif:build/item_obscurator" /> (100,000 EU): sneak + right-click a block to scan its look (5,000 EU); right-click one of this mod's machines or cable covers to apply it (1,000 EU).
- <ItemLink id="mio_icif:build/item_cf_sprayer" />: holds construction foam; right-click sprays foam into the open space, sneak + right-click switches the mode. It checks claims block by block.
- <ItemLink id="mio_icif:build/item_painter" />: dyed, right-click wool, terracotta, concrete, glass and similar blocks to give them its colour; construction foam becomes concrete of that colour.
