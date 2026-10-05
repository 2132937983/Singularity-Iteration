---
navigation:
  title: "Item Extraction Pipe"
  icon: mio_icif:pipe/block_pipe_item_input
  parent: heavy.md
  position: 2
item_ids:
  - mio_icif:pipe/block_pipe_item_input
---

# Item Extraction Pipe

<Row>
  <BlockImage id="mio_icif:pipe/block_pipe_item_input" scale="3" />
</Row>

## Function

The Item Extraction Pipe takes items from adjacent containers and sends the items into connected Item Transport Pipes.
Without EU, the pipe moves one item per cycle. EU from an MV cable increases the number of items per cycle.

## Power data

This block uses no EU.

## Procedure

1. Place the Item Extraction Pipe next to the source chest.
2. Connect Item Transport Pipes from the extraction pipe to the target container.
3. Connect an MV cable to the extraction pipe to move more items per cycle.
4. Right-click an arm of the pipe with a wrench to close that side.

## Notes

- The extraction pipe does not put items into containers. The pipe works only when a transport pipe can take the items.

## Recipe

<RecipesFor id="mio_icif:pipe/block_pipe_item_input" fallbackText="-" />
