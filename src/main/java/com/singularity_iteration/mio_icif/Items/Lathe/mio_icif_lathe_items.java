package com.singularity_iteration.mio_icif.Items.Lathe;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 车床相关物品注册（车刀类）
 * 加工件在 mio_icif_normal 中注册以保持兼容纳?
 */
@SuppressWarnings("null")
public class mio_icif_lathe_items {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    // 车床车刀
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

