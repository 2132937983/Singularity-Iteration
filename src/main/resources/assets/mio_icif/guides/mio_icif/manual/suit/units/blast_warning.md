---
navigation:
  title: "Blast Warning Timer"
  icon: mio_icif:module/item_module_blast_warning
  parent: suit/index.md
  position: 14
item_ids:
  - mio_icif:module/item_module_blast_warning
---

# Blast Warning Timer

<ItemImage id="mio_icif:module/item_module_blast_warning" scale="3" />

## Function

The unit measures the fuse of the explosives within 28 blocks (a nuke from up to 100 blocks): see Detected explosives.
A round icon floats above each explosive. Its ring empties as the fuse burns, and a tag shows the seconds left.
A zone on the ground shows the blast radius. The zone fills as the fuse burns.
When an explosive is near you, an icon with an arrow appears around the aim point and points to the explosive. A beep sounds faster as the fuse burns.
Inside the blast radius, the edge of the view flashes red.

## Detected explosives

| Explosive | Radius | Timer |
|---|---|---|
| Primed TNT, and the TNT of other mods that is primed TNT | 4 | fuse |
| TNT minecart, swelling creeper (also creepers of other mods) | 4 / 3 (charged 6) | fuse / swell |
| Industrial TNT (ITNT) | 8 | fuse |
| Nuke | 40 | fuse (30 s) |
| Dynamite / sticky dynamite | 3 / 2 | fuse; a remote sticky charge shows "--" |
| Rocket, ghast fireball, wither skull, dragon fireball | 5 / 2 / 2 / 3 | time to impact, only while it flies at you |
| Other mods: an entity whose id has an explosive word (tnt, nuke, bomb, dynamite, grenade, explosive, missile, warhead, landmine, c4, primed, rocket) | 4 (nuke words 24) | its fuse getter, else "--" |

- A data pack can describe any explosive: data map mio_icif:blast_warning on entity types (radius in blocks, fuse in seconds, label), or the entity type tag mio_icif:blast_warning (radius 4).
- "--" means that the charge has no timer: the zone and the icon show where it is.

## Power data

| Item | Value |
|---|---|
| Fits | Chestplate / Helmet |
| Power | 2 EU/t while on |
| Display | Needs a quantum helmet visor |

## Procedure

1. Install the unit into the quantum chestplate or helmet.
2. Watch the icons around the aim point.
3. Move away in the direction opposite to the arrow.
4. Stay out of the zone on the ground until the explosion.

## Notes

- The icon and the zone turn red in the last second.

## Recipe

<RecipesFor id="mio_icif:module/item_module_blast_warning" fallbackText="-" />
