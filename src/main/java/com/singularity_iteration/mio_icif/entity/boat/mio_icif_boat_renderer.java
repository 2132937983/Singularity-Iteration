package com.singularity_iteration.mio_icif.entity.boat;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

@SuppressWarnings("null")
public class mio_icif_boat_renderer extends BoatRenderer {

    private static final ResourceLocation CARBON_BOAT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/boats/entity_boat_carbon.png");
    private static final ResourceLocation RUBBER_BOAT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/boats/entity_boat_rubber.png");
    private static final ResourceLocation ELECTRIC_BOAT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/boats/entity_boat_electric.png");

    public static final ModelLayerLocation CARBON_BOAT_LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "boat/carbon"), "main");
    public static final ModelLayerLocation RUBBER_BOAT_LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "boat/rubber"), "main");
    public static final ModelLayerLocation ELECTRIC_BOAT_LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "boat/electric"), "main");

    private final Pair<ResourceLocation, ListModel<Boat>> carbonResources;
    private final Pair<ResourceLocation, ListModel<Boat>> rubberResources;
    private final Pair<ResourceLocation, ListModel<Boat>> electricResources;

    public mio_icif_boat_renderer(EntityRendererProvider.Context context, ModelLayerLocation layerLocation) {
        super(context, false);
        ModelPart modelPart = context.bakeLayer(layerLocation);
        BoatModel model = new BoatModel(modelPart);
        this.carbonResources = Pair.of(CARBON_BOAT_TEXTURE, model);
        this.rubberResources = Pair.of(RUBBER_BOAT_TEXTURE, model);
        this.electricResources = Pair.of(ELECTRIC_BOAT_TEXTURE, model);
    }

    @Override
    public ResourceLocation getTextureLocation(Boat entity) {
        return getModelWithLocation(entity).getFirst();
    }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat boat) {
        if (boat instanceof mio_icif_carbon_boat) {
            return carbonResources;
        } else if (boat instanceof mio_icif_rubber_boat) {
            return rubberResources;
        } else if (boat instanceof mio_icif_electric_boat) {
            return electricResources;
        }
        return carbonResources;
    }
}

