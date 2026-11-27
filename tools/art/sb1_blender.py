"""Open the authored SB1 game meshes as a portable, editable Blender project.

Run with Blender 4.4+: blender -b --factory-startup --python tools/art/sb1_blender.py
The mesh retains the reference template's local XYZ and its Y-up OBJ to Z-up
Blender object transform. Toggle a collection to edit another state.
"""
from math import radians
from pathlib import Path

import bpy
from mathutils import Euler


ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/mio_icif/model/slashblade/nano"
OUTPUT = ROOT / "docs/art/sb1/blender/nano.blend"
GROUPS = (
    ("Blade", "blade"),
    ("Sheath", "sheath"),
    ("Broken blade", "blade_damaged"),
    ("Tip fragment", "blade_fragment"),
    ("Inventory - complete", "item_blade"),
    ("Inventory - broken", "item_damaged"),
    ("Inventory - unsheathed", "item_bladens"),
    ("Charge shell", "effect"),
)


def material(name, image, luminous=False):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nodes, link = mat.node_tree.nodes, mat.node_tree.links.new
    nodes.clear()
    texture = nodes.new("ShaderNodeTexImage")
    texture.image = image
    texture.interpolation = "Closest"
    texture.extension = "REPEAT" if image.name == "charge.png" else "EXTEND"
    texture.location = (-420, 100)
    surface = nodes.new("ShaderNodeEmission" if luminous else "ShaderNodeBsdfPrincipled")
    surface.location = (-115, 165)
    if not luminous:
        surface.inputs["Roughness"].default_value = .54
        surface.inputs["Metallic"].default_value = .18
    link(texture.outputs["Color"], surface.inputs["Color" if luminous else "Base Color"])
    transparent = nodes.new("ShaderNodeBsdfTransparent")
    transparent.location = (-115, -110)
    mix = nodes.new("ShaderNodeMixShader")
    mix.location = (120, 80)
    link(texture.outputs["Alpha"], mix.inputs[0])
    link(transparent.outputs[0], mix.inputs[1])
    link(surface.outputs[0], mix.inputs[2])
    output = nodes.new("ShaderNodeOutputMaterial")
    output.location = (340, 80)
    link(mix.outputs[0], output.inputs["Surface"])
    nodes.active = texture
    return mat


def main():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.wm.obj_import(filepath=str(ASSETS / "model.obj"), forward_axis="NEGATIVE_Z", up_axis="Y",
                          use_split_objects=True, use_split_groups=False)
    images = {}
    for name in ("texture.png", "charge.png"):
        image = bpy.data.images.load(str(ASSETS / name))
        image.pack()
        image.filepath_raw = bpy.path.relpath(str(ASSETS / name), start=str(OUTPUT.parent))
        images[name] = image
    body = material("Nano | Carbon and ceramic", images["texture.png"])
    emitter = material("Nano | Luminous trim", images["texture.png"], luminous=True)
    charge = material("Nano | Charge scroll", images["charge.png"], luminous=True)
    scene = bpy.context.scene
    edit_layer = bpy.context.view_layer
    edit_layer.name = "Edit - drawn"
    for label, primary in GROUPS:
        collection = bpy.data.collections.new(label)
        scene.collection.children.link(collection)
        names = (primary,) if primary == "effect" else (primary, primary + "_luminous")
        for name in names:
            obj = bpy.data.objects[name]
            for old in list(obj.users_collection):
                old.objects.unlink(obj)
            collection.objects.link(obj)
            obj.data.materials.clear()
            obj.data.materials.append(charge if name == "effect" else emitter if name.endswith("_luminous") else body)
        edit_layer.layer_collection.children[collection.name].exclude = primary != "blade"
    # Distinct game states share a grip-space origin. Keep the edit view clean
    # and offer a second view layer for a complete native OBJ export.
    export_layer = scene.view_layers.new("Export - all parts")
    for child in export_layer.layer_collection.children:
        child.exclude = False
    bpy.context.window.view_layer = edit_layer
    blade = bpy.data.objects["blade"]
    bpy.context.view_layer.objects.active = blade
    for obj in bpy.context.selected_objects:
        obj.select_set(False)
    scene.view_settings.view_transform = "Standard"
    for screen in bpy.data.screens:
        for area in screen.areas:
            if area.type == "VIEW_3D":
                view = area.spaces.active
                view.shading.color_type = "TEXTURE"
                view.region_3d.view_perspective = "ORTHO"
                view.region_3d.view_rotation = Euler((radians(78), radians(-7), radians(-12))).to_quaternion()
                view.region_3d.view_distance = 275
                view.region_3d.view_location = (-125, 0, -4)
                view.clip_end = 2000
                view.overlay.show_overlays = False
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(OUTPUT), compress=True, relative_remap=False)


if __name__ == "__main__":
    main()
