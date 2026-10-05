---
navigation:
  title: "Water Pipe"
  icon: mio_icif:pipe/block_pipe_water
  parent: heavy.md
  position: 16
item_ids:
  - mio_icif:pipe/block_pipe_water
---

# Water Pipe

<Row>
  <BlockImage id="mio_icif:pipe/block_pipe_water" scale="3" />
</Row>

## Function

The Water Pipe moves fluid between connected pipes and fluid containers. The pipe holds one fluid type at a time.
The pipe connects to each adjacent pipe and to each adjacent block with a fluid tank.

## Power data

This block uses no EU.

## Procedure

1. Put a Water Extraction Pipe or a machine with fluid output at the start of the line.
2. Place Water Pipes from that point to the target tank.
3. Right-click an arm of the pipe with a wrench to close that side.
4. Right-click the pipe center on the closed side with a wrench to open the side again.

## Notes

- The pipe does not pull fluid. A machine or an extraction pipe must push fluid into the pipe.
- When you break the pipe, the fluid in the pipe disappears.

## Recipe

<RecipesFor id="mio_icif:pipe/block_pipe_water" fallbackText="-" />
