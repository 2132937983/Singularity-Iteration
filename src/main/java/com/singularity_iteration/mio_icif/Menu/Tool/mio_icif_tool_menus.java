package com.singularity_iteration.mio_icif.Menu.Tool;

import com.singularity_iteration.mio_icif.Singularity_Iteration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class mio_icif_tool_menus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(BuiltInRegistries.MENU, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<mio_icif_od_scanner_menu>> OD_SCANNER_MENU =
        MENU_TYPES.register("od_scanner", () ->
            IMenuTypeExtension.create(mio_icif_od_scanner_menu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<mio_icif_meter_menu>> EU_METER_MENU =
        MENU_TYPES.register("eu_meter", () ->
            IMenuTypeExtension.create(mio_icif_meter_menu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CropAnalyzerMenu>> CROP_ANALYZER_MENU =
        MENU_TYPES.register("crop_analyzer", () ->
            IMenuTypeExtension.create(CropAnalyzerMenu::new));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }

}

