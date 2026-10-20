package com.miophas.singularity_iteration.common.item.lathe;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration of the lathe items (lathing tools).
 * The workpieces are registered in mio_icif_normal for compatibility.
 */
@SuppressWarnings("null")
public class mio_icif_lathe_items {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    // Lathing tools: the iron one is IC2's Lathing Tool, the diamond one a project extension.
    public static final DeferredItem<LathingToolItem> IRON_LATHING_TOOL = 
        ITEMS.register("item_iron_lathing_tool", 
            () -> new LathingToolItem(LathingToolItem.ToolMaterial.IRON));
    
    public static final DeferredItem<LathingToolItem> DIAMOND_LATHING_TOOL = 
        ITEMS.register("item_diamond_lathing_tool", 
            () -> new LathingToolItem(LathingToolItem.ToolMaterial.DIAMOND));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

