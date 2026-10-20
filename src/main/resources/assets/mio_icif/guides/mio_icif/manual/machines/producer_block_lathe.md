---
navigation:
  title: "Lathe"
  icon: mio_icif:producer/block_lathe
  parent: machines.md
  position: 39
item_ids:
  - mio_icif:producer/block_lathe
---

# Lathe

<Row>
  <BlockImage id="mio_icif:producer/block_lathe" scale="3" />
</Row>

## Function

The Lathe uses kinetic energy (KU) from an adjacent kinetic source to shape Turning Blanks with a Lathe Tool.
The workpiece has five positions. Each cut can give a by-product, for example Sticks or Small Iron Dust.
The Lathe Tool must be harder than the workpiece. Each cut uses tool durability.
A position can be cut down to a thickness of 1.
The icon and the tooltip of a Turning Blank show its current profile. The bar under the icon shows the material that is left to cut.

## Parts

Some recipes need an Iron Turning Blank cut to an exact profile (the thickness of positions 1 to 5).
A finished part has a teal outline and its name in the tooltip. Hold Shift on a blank to see the parts it can still become.

| Part | Profile | Used for |
|---|---|---|
| Drill Tip | 5-4-3-2-1 | Diamond Drill |
| Water Wheel Shaft | 4-3-4-3-4 | Water Kinetic Generator |
| Piston Rod | 3-2-3-2-3 | Stirling Kinetic Generator |
| Laser Barrel | 4-3-2-2-2 | Mining Laser |
| Crank Handle | 5-5-5-2-2 | Manual Kinetic Generator |
| Rotor Hub | 5-3-3-3-5 | Wind Kinetic Generator |
| Turbine Spindle | 1-3-5-3-1 | Steam Kinetic Generator |

Every kinetic generator except the Electric Kinetic Generator needs a part. Use an Electric Kinetic Generator to power your first Lathe.

| Item | Hardness | Uses |
|---|---|---|
| Iron Lathe Tool | 3 | 50 |
| Diamond Lathe Tool | 4 | 64 |
| Wooden Turning Blanks (width 3, Sticks 10%) | 0 | - |
| Iron Turning Blanks (width 5, Small Iron Dust 50%) | 2 | - |

## Power data

This block uses no EU. It stores up to 10,000 KU and takes KU from any side.
The buffer loses 1% per tick. The Lathe is active above 500 KU. Each cut uses 1,000 KU.

## Procedure

1. Place the Lathe next to a kinetic generator.
2. Put a Lathe Tool into the tool slot.
3. Put Turning Blanks into the workpiece slot.
4. Click a position in the GUI to cut the workpiece at that position.
5. Take the by-products from the output slot.

## Recipe

<RecipesFor id="mio_icif:producer/block_lathe" fallbackText="-" />
