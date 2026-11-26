---
navigation:
  title: "GESU Wireless Induction Port"
  icon: mio_icif:wiring/block_gesu_induction_port
  parent: power.md
  position: 16
item_ids:
  - mio_icif:wiring/block_gesu_induction_port
---

# GESU Wireless Induction Port

<Row>
  <BlockImage id="mio_icif:wiring/block_gesu_induction_port" scale="3" />
</Row>

## Function

A GESU module: put it on any face of the GESU core, in place of an input or output module. While the GESU is formed, it charges
every player within ±8 blocks of itself from the GESU: the electric items in their armor, hands and inventory (charge pad order),
at most 32,768 EU an item a tick (one LuV packet). It takes no cables and sends nothing into the grid.

## Use

1. Build the GESU with this port on one of the six faces of the core.
2. Stand within 8 blocks of the port. Your electric gear charges; now and then sparks show that it does.
3. Right-click the port to open the GESU's screen.

## Recipe

<RecipesFor id="mio_icif:wiring/block_gesu_induction_port" fallbackText="-" />
