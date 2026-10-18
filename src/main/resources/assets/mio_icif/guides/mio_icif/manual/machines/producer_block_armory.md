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

## Wingman safety

- Mob capture tools (soul vials, the mob imprisonment tool, containment jars, capture balls) cannot take a Wingman: an item used on a Wingman does nothing, and the Wingman is in the capture blacklist tags of these mods and in c:capturing_not_supported.
- Each deployment has a sortie token. A copy of a Wingman whose suit already went home (for example from a capture tool of an older version) is removed when it appears, without items. The owner gets a chat message.
- If the tick of a Wingman or of an effect that it made (a slash of SlashBlade, for example) fails, the suit goes home and the Wingman leaves instead of a crash. The server log has the details.
- A Wingman has all player attributes (reach, sweeping, luck and others), so weapons of other mods work in its hands.

## Wingman tactics

- **Targets:** the mob you hit (all Wingmen focus it), a creeper near you (first of all when it fuses), the mob that hit you, mobs that hunt you (ranged attackers first), then the nearest hostile mobs. Wingmen spread over different targets unless you call one.
- **Ranged weapons:** the Wingman picks a firing position with a clear line of fire on the far side of the target, up high, never with you in the line. A mob that comes too close makes it boost away with its thrusters.
- **Melee and blades:** it comes in from the flank or from behind, strikes and boosts away, then makes a new pass. A creeper is knocked away from you.
- **Effects:** a scan laser and a hologram marker show its target, in the colour of its mode; the guns flash, the thrusters burst on a boost and small jets correct every turn. Client option wingman_fx.

With an Entanglement Relay on a quantum suit you can also give your Wingmen orders (move, hold, suppress, focus) from the command wheel (see Quantum command).

## Recipe

<RecipesFor id="mio_icif:producer/block_armory" fallbackText="-" />
