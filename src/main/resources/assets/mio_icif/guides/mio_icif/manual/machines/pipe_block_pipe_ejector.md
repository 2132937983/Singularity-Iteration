---
navigation:
  title: "Ejector Pipe"
  icon: mio_icif:pipe/block_pipe_ejector
  parent: machines.md
  position: 12
item_ids:
  - mio_icif:pipe/block_pipe_ejector
---

# Ejector Pipe

<Row>
  <BlockImage id="mio_icif:pipe/block_pipe_ejector" scale="3" />
</Row>

## Function

The Ejector Pipe moves items and fluids out of a pipe line or a container into the world.
The nozzle points away from the block that you put the pipe on.

## Rules

* Pipes and machines can put items and fluids into the ejector from all sides except the nozzle.
* When the ejector is empty, it takes up to 16 items or 1 bucket from the container behind it.
* Every 8 ticks, the ejector throws its items out of the nozzle at high speed.
* Every second, with 1 full bucket, the ejector puts a source block of the fluid in front of the nozzle. If the position is not free, the ejector waits. It does not delete fluids.
* A redstone signal stops the ejector.

**CAUTION:** Fluids from the ejector spread as from a bucket. Do a check of the area in front of the nozzle.

## Recipe

<RecipesFor id="mio_icif:pipe/block_pipe_ejector" fallbackText="-" />
