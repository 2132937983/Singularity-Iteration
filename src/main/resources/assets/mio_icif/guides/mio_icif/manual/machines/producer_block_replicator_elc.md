---
navigation:
  title: "Replicator"
  icon: mio_icif:producer/block_replicator_elc
  parent: machines.md
  position: 57
item_ids:
  - mio_icif:producer/block_replicator_elc
---

# Replicator

<Row>
  <BlockImage id="mio_icif:producer/block_replicator_elc" scale="3" />
</Row>

## Function

The Replicator uses UU-Matter and EU to make a copy of an item from a stored pattern.
The pattern comes from a Pattern Storage Crystal in the memory slot or from an adjacent Pattern Storage.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | EV (2,048 EU) |
| Maximum input | 8,192 EU/t |
| Energy storage | 2,000,000 EU |
| Use while working | 512 EU/t |
| Operation time | 100 tick (5 s) |
| Energy per operation | 51,200 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an EV supply to the machine through transformers.
2. Supply UU-Matter with UU-Matter Cells or with a pipe.
3. Put a Pattern Storage Crystal into the memory slot, or place a Pattern Storage next to the machine.
4. Click Single or Loop in the GUI to start.
5. Take the result from the output slot.

## Copying

- A copy goes in steps: each tick pays 0.1 mB of UU-matter and 512 EU (a replication list can set its own EU per item). The tank need not hold the whole price: with a steady supply the copy finishes, also for items that cost more than the tank holds (64 buckets). It takes the price / 0.1 mB in ticks; one bucket of UU is about 8.3 minutes.
- The UU paid into a copy is not lost:
  - The memory crystal taken out, or the pattern storage gone: the copy pauses and keeps its progress; it goes on when the same pattern is back.
  - A crystal with another pattern: the copy ends and the paid UU goes back to the balance.
  - Stop: pauses; Single or Loop goes on. Stop while paused ends the copy; the paid UU goes back to the balance, the EU spent does not.
  - A new price for the item: the copy goes on at the new price; if more than the new price is paid it finishes at once and the excess goes back to the balance; an item that can no longer be copied ends the copy with a refund.
  - No UU, no power, a full output slot: the copy waits and keeps its progress.
  - Taken down with a wrench: the tank, the balance and the progress stay with the item; place it again and put the crystal in to go on.
- Balance: refunded UU and the small rest of each step stay in the machine and are used before the tank. The status line says why the machine copies, waits or stands; its tooltip shows the paid UU and the balance.

## Replicable items and data packs

- The replicator makes an item from its pattern (scan the item first) when the item has a UU price.
- Prices come from the IC2 reference table, from the recipes that lead to an item (a crafted or smelted item gets the price of its inputs), and from replication lists.
- The default list adds 95 items: newer ores and raw ores (copper, deepslate ores, ancient debris), quartz, amethyst, new stone types, mob drops (ghast tear, shulker shell, prismarine shard, phantom membrane, ink sac, nautilus shell, nether star, raw meat and fish) and plants (vines, nether wart, bamboo, kelp, berries, chorus fruit), and the raw ores and the sticky resin of this mod. The dragon egg is never replicable.
- A data pack adds its own list: a JSON file in the folder uu_replication of its data namespace (data/(namespace)/uu_replication/(name).json). A file at data/mio_icif/uu_replication/defaults.json replaces the default list.
- Each entry of the array "entries" has "item" (an item id) or "tag" (an item tag), and "uu" (mB of UU matter per copy). Optional: "eu" (EU per copy; the standard is 5,120 EU per mB), "deny": true (the item is never replicable), "required": false (an unknown id is skipped without a warning).
- Example entry: `{ "item": "minecraft:netherite_scrap", "uu": 1500, "eu": 2000000 }`
- Files load in the order of their ids; a later entry for the same item wins. A wrong entry is skipped with a warning in the server log; the rest loads. The log line "[UU] Replication lists" gives the counts.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above EV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_replicator_elc" fallbackText="-" />
