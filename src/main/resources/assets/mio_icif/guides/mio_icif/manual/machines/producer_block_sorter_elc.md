---
navigation:
  title: "Electric Sorter"
  icon: mio_icif:producer/block_sorter_elc
  parent: machines.md
  position: 22
item_ids:
  - mio_icif:producer/block_sorter_elc
---

# Electric Sorter

<Row>
  <BlockImage id="mio_icif:producer/block_sorter_elc" scale="3" />
</Row>

## Function

The Electric Sorter sends items to the six faces by filters. Each face has its own filter slots.
Items that match no filter go to the default output face. Each moved item costs EU.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 15,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an MV cable to the machine.
2. Place inventories against the output faces.
3. Put sample items into the filter slots of each face in the GUI.
4. Select the default output face in the GUI.
5. Insert the items into the sorter.

## Notes

- The stack size in a filter slot sets the batch size for that filter.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_sorter_elc" fallbackText="-" />
