---
navigation:
  title: "Pattern Scanner"
  icon: mio_icif:producer/block_scanner_elc
  parent: machines.md
  position: 51
item_ids:
  - mio_icif:producer/block_scanner_elc
---

# Pattern Scanner

<Row>
  <BlockImage id="mio_icif:producer/block_scanner_elc" scale="3" />
</Row>

## Function

The Pattern Scanner uses EU to scan an item and record its pattern with the UU-Matter cost and the EU cost.
A completed scan consumes the scanned item. The machine saves the pattern to a Pattern Storage Crystal or to an adjacent Pattern Storage.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 512,000 EU |
| Use while working | 256 EU/t |
| Operation time | 3,300 tick (165 s) |
| Energy per operation | 844,800 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an HV cable to the machine.
2. Put the item to scan into the scanner slot.
3. Put a blank Pattern Storage Crystal into the memory slot, or place a Pattern Storage next to the machine.
4. Save the result in the GUI when the scan is complete.

## Notes

- The machine does not scan an item that the storage already holds.
- A redstone signal stops the machine.

> **WARNING:** The machine explodes on overvoltage above HV.


## Recipe

<RecipesFor id="mio_icif:producer/block_scanner_elc" fallbackText="-" />
