---
navigation:
  title: "Item Transport Pipe"
  icon: mio_icif:pipe/block_pipe_item
  parent: heavy.md
  position: 3
item_ids:
  - mio_icif:pipe/block_pipe_item
---

# Item Transport Pipe

<Row>
  <BlockImage id="mio_icif:pipe/block_pipe_item" scale="3" />
</Row>

## Function

The Item Transport Pipe moves items between pipes and puts the items into connected containers.
The pipe receives items from an Item Extraction Pipe. The pipe searches the network for a container with free space.

## Power data

This block uses no EU.

## Procedure

1. Place an Item Extraction Pipe next to the source container.
2. Connect Item Transport Pipes from the extraction pipe to the target containers.
3. Right-click an arm of the pipe with a wrench to close that side.

## Notes

- When you break the pipe, the items in the pipe drop on the ground.

## Recipe

<RecipesFor id="mio_icif:pipe/block_pipe_item" fallbackText="-" />
