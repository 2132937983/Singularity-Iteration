---
navigation:
  title: "Armory"
  icon: mio_icif:producer/block_armory
  parent: machines.md
  position: 9
item_ids:
  - mio_icif:producer/block_armory
---

# Armory

<Row>
  <BlockImage id="mio_icif:producer/block_armory" scale="3" />
</Row>

## Function

The Armory stores six suits. Each suit has six pieces: helmet, chestplate, leggings, boots, main hand and off hand.
The Armory flies a stored suit to its owner on command.
The Armory repairs and charges the stored pieces with EU. Botania gear uses mana from the Armory mana tank.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 1,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an HV cable to the Armory.
2. Right-click the powered Armory to become its owner.
3. Put your gear into the suit slots in the GUI.
4. Sneak and right-click the Armory with an Armory Remote Controller to pair the remote.
5. Wear the remote in a charm or belt accessory slot.
6. Open the Armory section of the Equipment Console and click SUMMON.

## Notes

- The Armory needs stored EU before it accepts an owner. Only the owner can open, use or break a bound Armory.
- Equipment from other mods needs a Connector Kit before the Armory accepts the equipment.
- Each summon costs EU. The cost increases with the distance and with a change of dimension.
- Since 0.1.10.32 the Armory is a gantry: an iris hatch on top twists open before a piece launches and as a returning piece comes in; small holograms of the stored suits circle above it (within 8 blocks); while it repairs and charges a ring with running arcs lies on the floor and a scan ring moves along its sides (amber or violet blinking: waiting for power or mana). A summon from far away or from another dimension opens a transfer gate in the sky for each piece: the piece leaves it with afterimages, and a piece that flies home to the sky goes into a gate. The console shows the suit under the mouse as a hologram beside its window, and a frame in the colour of the charge round each stored suit piece. Client option armory_fx.

## Wingmen

A Wingman is a stored suit that flies and fights next to you as an empty suit of armor. It wears the real items of the suit (four armor pieces, main hand, off hand), so it has the armor value, the weapon damage and the enchantments of the suit.

- **Deploy:** in the Armory page of the console, choose "Wingman", then press "Deploy" beside a suit. The suit needs at least one armor piece. The cost is the same as a summon.
- **Modes:** Offensive (attacks what you attack, else the nearest hostile mob near you), Dispersal (each holds a sector of a wide ring around you and draws the mobs that hunt you), Escort (stays close and destroys projectiles that fly at you), Fulton (takes the marked mob to the Armory, 4,000 EU each; a hostile mob must be below half health first). The mode button of the console sets every Wingman.
- **Recall:** "Recall" or "Recall all" in the console, or crouch and use an empty hand on your Wingman. A Wingman also goes home when you log off, die, change dimension or become a spectator.
- **Damage:** a fatal hit does not destroy a Wingman: it keeps 1 health and is invulnerable on its way home. Only the void and /kill remove it at once; its items still go to the Armory (to your inventory if the Armory is gone, else they drop at the Wingman). You and your other Wingmen cannot hurt it.
- **Wear:** the armor and the weapons of a Wingman lose no durability, and bows and crossbows use no arrows. Energy weapons use their own charge; without charge the Wingman does not fire.

## Sortie and return

- A player has at most 4 Wingmen (config Wingmen.maxPerPlayer), counting the ones in unloaded chunks.
- A suit out as a Wingman shows "Out" in the console. Its positions are kept for the Wingman: "send home", pipes and storage buses put nothing there, and the pieces come back to their places.
- When the Armory is far away or in another dimension, the Wingman drops in where there is room above you (up to 18 blocks), else beside you; with no room at all it does not deploy and nothing is paid.
- "Recall all" in the console also reaches the Wingmen in unloaded chunks: their chunk loads for a moment and the suit goes straight home. The same happens when you log off.
- Upkeep is 160 EU per second by default (config Wingmen.upkeepEuPerSecond). At deployment the Armory charges the Wingman with an upkeep reserve of 10 minutes (96,000 EU, config Wingmen.reserveMinutes; less when the Armory has less). The electric pieces of the suit pay first, then the reserve, then the Armory (only while it is loaded), so a Wingman in plain armor flies about 10 minutes away from your base. The rest of the reserve goes back to the Armory when the Wingman returns. After 5 seconds without upkeep the Wingman returns (config Wingmen.lowPowerSeconds).
- The flight time left (what the suit charge and the reserve pay for) shows in the Wingman row of the console, in the squad HUD and in Jade; it turns red under one minute, and "Armory" means the Armory is paying.

## Wingman safety

- Mob capture tools (soul vials, the mob imprisonment tool, containment jars, capture balls) cannot take a Wingman: an item used on a Wingman does nothing, and the Wingman is in the capture blacklist tags of these mods and in c:capturing_not_supported.
- Each deployment has a sortie token. A copy of a Wingman whose suit already went home (for example from a capture tool of an older version) is removed when it appears, without items. The owner gets a chat message.
- If the tick of a Wingman or of an effect that it made (a slash of SlashBlade, for example) fails, the suit goes home and the Wingman leaves instead of a crash. The server log has the details.
- A Wingman has all player attributes (reach, sweeping, luck and others), so weapons of other mods work in its hands.

## Wingman tactics

- **Targets:** first the mob you hit (all Wingmen focus it), then a creeper near you (more urgent when it fuses), the mob that hit you, mobs that hunt you (ranged attackers first), then the nearest hostile mobs. Wingmen spread over different targets unless you call one.
- **Ranged weapons:** the Wingman picks a firing position with a clear line of fire on the far side of the target, up high, never with you in the line. A mob that comes too close makes it boost away with its thrusters.
- **Melee and blades:** it comes in from the flank or from behind, strikes and boosts away, then makes a new pass. A creeper is knocked away from you.
- **Effects:** a scan laser and a hologram marker show its target, in the colour of its mode; the guns flash, the thrusters burst on a boost and small jets correct every turn. Client option wingman_fx.
- **Idle landing:** with no target for 2 seconds while you stand on the ground, a Wingman sets down on the ground next to you and switches its thrusters off. It also lands at the post of a hold order. A target, an order, or you walking away, flying or gliding make it take off at once.
- **SlashBlade blades:** the arm holds the blade still at the ready and plays each cut: a diagonal cut for a combo, a flat sweep for the dash, an overhead cut with both hands for a Slash Art. The combo inputs come every 8 ticks so that every cut plays out.

With an Entanglement Relay on a quantum suit you can also give your Wingmen orders (move, hold, suppress, focus) from the command wheel (see Quantum command).

## Recipe

<RecipesFor id="mio_icif:producer/block_armory" fallbackText="-" />
