# Singularity Iteration API Guide (0.1.8.0)

This guide is for addon authors. The text follows ASD-STE100 (Simplified Technical English):
short sentences, active voice, one instruction in each step.

## 1. Artifacts

| Artifact | Contents | Use |
|---|---|---|
| `mio_icif-api-<version>.jar` | `com.miophas.singularity_iteration.core.api.**` only | Compile an addon that only uses the API. |
| `singularity-iteration-core-<version>.jar` | API, runtime, prefabs, platform helpers | Compile an addon that extends the prefab block entities. |
| `singularity-iteration-<version>.jar` | The full mod | Run the game. |

The API packages refer to no runtime, prefab or `common` class. The build checks this
(`checkCoreArchitecture`). The example addon (`src/addonExample`) compiles against the core JAR
only.

### 1.1 Procedure: add the dependency

1. Put the API JAR in your build as a compile-only dependency.
2. Put the full mod JAR in your run classpath.
3. In `neoforge.mods.toml`, add a dependency on `mio_icif`. Set `type="required"` if your addon
   cannot run without it. Set `type="optional"` if your addon only adds integration.
4. If the dependency is optional, put each call to the API in a class that loads only when
   `ModList.get().isLoaded("mio_icif")` is true.

## 2. General rules

* Call the API on the server thread, unless a section gives a different rule.
* Register during mod construction or during a setup event. Do not register after the server
  starts.
* Registries in the API are open on both sides. Register on both sides, because the client uses
  the data for names, colours and screens.
* The API does not change the save format of a world. Data that an addon adds stays in the
  addon's own data components or attachments.

**CAUTION:** Do not keep a reference to an `ItemStack` that the API gives to a callback. The stack
can change after the callback.

## 3. Quantum suit units (`core.api.suit`)

An addon can add an upgrade unit for the quantum suit. The unit behaves like a built-in unit: the
quantum modification station installs and removes it, the Singularity Management Panel shows its
switch, and its energy cost uses the full-set energy pool.

| Type | Function |
|---|---|
| `SuitModuleSpec` | The data of the unit: id, slots, drain (EU/t), colour, visor need, unit item. |
| `ISuitModuleItem` | Interface of the unit item. It returns the spec. |
| `SuitModuleBehavior` | The work of the unit. It runs once each server tick while the unit is active. |
| `SuitModuleRegistry` | Registration and the query `isActive(player, id)`. |

### 3.1 Rules

* The id must have your mod namespace, for example `myaddon:sonar`. Built-in units have no
  namespace.
* An addon unit counts against the unit capacity of the piece (helmet 4, chestplate 3,
  leggings 2, boots 2).
* "Active" means: installed in a worn quantum piece, switched on in the panel, enough energy
  (at least one tick of drain), and a quantum helmet worn when `needsVisor` is true.
* The drain is collected once each second. Energy Saving of the piece applies.
* A behavior that throws an exception is logged. The unit stays off for that tick.
* A piece keeps the id of a unit whose mod was removed. That unit does nothing and does not count
  against the capacity.

### 3.2 Procedure: add a unit

1. Make an item class that extends `Item` and implements `ISuitModuleItem`.
2. Make a `SuitModuleSpec`. Example:

   ```java
   public static final SuitModuleSpec SPEC = new SuitModuleSpec(
       ResourceLocation.fromNamespaceAndPath("myaddon", "sonar"),
       Set.of(EquipmentSlot.HEAD), 3, 0x33CCAA, true,
       ResourceLocation.fromNamespaceAndPath("myaddon", "sonar_unit"));
   ```

3. In the mod constructor, register the spec and the behavior:

   ```java
   SuitModuleRegistry.register(SPEC, (player, piece) -> {
       if (player.tickCount % 40 == 0) { /* work of the unit */ }
   });
   ```

4. Add the translation keys `module.myaddon.sonar` (name) and `module.myaddon.sonar.desc`
   (description).
5. Add a recipe for the unit item.

The panel switch key is `fcs_<namespace>_<path>` (`SuitModuleSpec.featureKey()`).

## 4. Ballistics (`core.api.item`)

The fire-control HUD, the Smart Trajectory Guidance unit and the drones use one ballistic model.
One step is one tick: move, then drag, then gravity.

| Type | Function |
|---|---|
| `BallisticProfile` | Launch speed, gravity, drag, water drag, "ready" flag. Factories: `arrow`, `thrown`, `straight`. |
| `IBallisticWeapon` | Interface of a weapon item. It returns the profile for a shot now. |
| `BallisticProfiles` | Lookup and providers for items of other mods. |

The lookup order is:

1. The item implements `IBallisticWeapon`.
2. A registered provider (`BallisticProfiles.register`), in registration order.
3. The profile that the client learned from shots of the item (see 4.2).
4. The built-in vanilla profiles (bow, crossbow, trident, thrown items).

### 4.1 Procedure: describe a weapon

1. Implement `IBallisticWeapon` on the weapon item.
2. Return a profile for the shot. Use `useTicks` for a weapon with a draw time.
   `BallisticProfile.drawPower(useTicks, fullDrawTicks)` gives the vanilla draw curve.
3. Return `null` when the weapon cannot fire.

For an item of a different mod, register a provider. The provider must return `null` for every
item that it does not know.

### 4.2 Learned profiles

When the local player fires a weapon without a profile, the client watches the new projectile for
one tick. It measures the launch speed, the drag and the gravity. It keeps up to 12 shots for each
item in `config/mio_icif-ballistics.json`. A shot in a fluid, a shot with thrust, and a shot that
hits something at once are not used.

## 5. Armory (`core.api.armory`)

Singularity Iteration posts these events on `NeoForge.EVENT_BUS`, on the server thread.

| Event | When | Cancelable |
|---|---|---|
| `ArmoryEvent.Summon` | Before a suit flies to a wearer (the player or a companion). | Yes. No energy is spent and no item moves. |
| `ArmoryEvent.PieceDocked` | After a flying piece is equipped. | No. |
| `ArmoryEvent.WingmanDeploy` | Before a stored suit deploys as a Wingman drone. | Yes. |

### 5.1 Data tags

| Tag | Type | Function |
|---|---|---|
| `mio_icif:armory/remote_equip` | Entity types | Companions that can receive a suit from the Armory. The entity must be `OwnableEntity` and owned by the player. Default: `touhou_little_maid:maid` (optional entry). |
| `mio_icif:armory/drawn_blade` | Items | Items that fly to the hand as a drawn blade. SlashBlade: Resharped blades are found without the tag. |
| `mio_icif:wingman/fulton_immune` | Entity types | Mobs that the Fulton recovery cannot take. Bosses (`c:bosses`) are always immune. |
| `mio_icif:resonance_immune` | Blocks | Blocks that the Resonance Deconstruction Field does not break. |

## 6. Energy (`core.api.energy`)

The energy API did not change its contracts in 0.1.8.0. The EU network uses fair-share
distribution by default (`distributionMode=fair` in `config/core.ini`, or the common config).

1. Register the role of each block with `EnergyNodeRegistry.register(id, profile)`.
   Use `EnergyNodeProfile.generator(packet)`, `storage(packet)` or `consumer()`.
2. Expose the block capability `EUApi.SIDED`. For a prefab block entity, call
   `CoreCapabilityRegistration.registerEntity(event, type)`.
3. The network gives energy in four stages: loads up to their rated input, then the remaining room
   of the loads, then storages up to their rated input, then the remaining room of the storages.
   In each stage, a receiver gets energy in proportion to its remaining demand. Generators give
   before storages.

## 7. Blueprint Builder and Material Interface

* The Armory Material Interface exposes `Capabilities.ItemHandler.BLOCK` (27 slots). Any storage
  next to it that exposes the same capability supplies the drones.
* The drones take only plain stacks: no changed components (contents, name, enchantments).
* The builder reads `.nbt` structure files and `.litematic` files from the `schematics` folder,
  and deployed Create schematics (data components `create:schematic_*`).
* Each placement fires `BlockEvent.EntityPlaceEvent` with a fake player that has the owner's
  profile. A protection mod can cancel it.

## 8. Documentation hooks

* GuideME: the manual is a code-built guide (`mio_icif:manual`). The tag
  `<ReactorSimulator chambers="N" preset="row/row/..."/>` shows the reactor simulator. A preset row
  is a comma-separated list of component keys (`_` = empty).
* Create Ponder: scenes for the Armory, the Nuclear Reactor Generator, the Blueprint Builder and the
  Ejector Pipe. Text keys: `mio_icif.ponder.<scene>.header` and `.text_N`.

## 9. Mixins

0.1.8.0 removes the invoker mixin `CropBiomeAccessor`. An access transformer
(`META-INF/accesstransformer.cfg`) replaces it. These features use NeoForge events or data
attachments, not mixins: electric armor absorption (`LivingDamageEvent.Pre`), the suit energy pool,
the Armory origin (player attachment `mio_icif:armory_origin`), the Wingman owner registry, the
smart guidance and the resonance field (tick events).

The remaining mixins and the reason for each:

| Mixin | Reason |
|---|---|
| `ItemStackMaxStackMixin`, `DataComponentMaxStackMixin`, `ContainerMaxStackMixin` | Stack sizes above 99. NeoForge has no hook for the codec limit. |
| `LivingEquipEnergyMixin` | A stack that differs only in stored energy must not trigger the equip sound and the game event. No cancelable NeoForge event exists. |
| `ArmorEquipSoundMixin` | A null equip sound of a different mod crashes footstep mods. The mixin gives a fallback. |
| `client.ItemInHandReequipMixin` | No re-equip animation for energy-only changes on all items (also addon items). `IItemExtension` covers only own items. |
| `client.AdvancementTabMiophaMixin`, `client.AdvancementWidgetHintMixin` | Advancement screen layout. No event exists. |
| `client.FcsGlowMixin`, `client.FcsTeamColorMixin` | Client-only outline glow of the suit sensors. No event exists. |
| `client.PlayerManeuverPoseMixin` | Limb poses of the flight. `RenderPlayerEvent` changes only the pose stack, not the model parts. |
| `IndependentChunkMixin` (core) | The energy network topology clock and the cable / transformer block entity factory. No NeoForge hook creates a block entity. |

## 10. Version rules (proposed policy)

These rules are a proposal for the API. The project has not adopted them formally yet.

* In the version iteration rules, the version format is 0.1.x.y.
* Under this version format, 0 indicates an unofficial release; 1 represents the first series of Singularity Iteration releases; x represents the x-th major        update in that series; and y represents the y-th addition or bug fix made on top of the x-th update.
* A minor version (0.1.8.x) keeps the API source compatible. A removal is announced one version
  before.
* `@Deprecated` API stays for one minor version.
* Check `MioIcifAPI.instance()` at run time. Do not call internal classes.
