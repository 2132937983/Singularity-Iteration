---
navigation:
  title: "Advanced Item Distributor"
  icon: mio_icif:producer/block_item_distributor_elc
  parent: machines.md
  position: 3
item_ids:
  - mio_icif:producer/block_item_distributor_elc
---

# Advanced Item Distributor

<Row>
  <BlockImage id="mio_icif:producer/block_item_distributor_elc" scale="3" />
</Row>

## Function

The Advanced Item Distributor sends the items in its 9-slot buffer to adjacent inventories in a priority order.
The GUI sets the face order. The block uses no EU.

## Power data

This block uses no EU.

## Procedure

1. Place inventories against the faces of the distributor.
2. Open the GUI and add the faces in the order of priority.
3. Insert items into the distributor.

## Notes

- The next face receives items only when the faces before it cannot take all items.

## Recipe

<RecipesFor id="mio_icif:producer/block_item_distributor_elc" fallbackText="-" />
