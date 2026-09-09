package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Tool.mio_icif_tool_menus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 屏幕注册类
 * 用于注册所有模组的 GUI 界面
 */
@SuppressWarnings("null")
public class mio_icif_screens {
    
    /**
     * 注册所有GUI屏幕
     * @param eventBus 事件总线
     */
    public static void register(IEventBus eventBus) {
        // 使用addListener注册RegisterMenuScreensEvent事件
        eventBus.addListener(mio_icif_screens::registerMenuScreens);
    }
    
    /**
     * 注册菜单屏幕
     * @param event 注册菜单屏幕事件
     */
    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        // 注册火力发电机的 GUI 界面
        event.register(
            mio_icif_menus.THERMAL_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_Thermal_Generator::new
        );

        // 注册 BatBox 的GUI 界面
        event.register(
            mio_icif_menus.BAT_BOX_MENU_TYPE.get(),
            mio_icif_gui_bat_box::new
        );

 // 注册电的GUI 界面
        event.register(
            mio_icif_menus.FURNACE_ELC_MENU_TYPE.get(),
            mio_icif_gui_furnace_elc::new
        );

        // 注册打粉机的 GUI 界面
        event.register(
            mio_icif_menus.POWDER_ELC_MENU_TYPE.get(),
            mio_icif_gui_powder_elc::new
        );

        // 注册进阶打粉机的 GUI 界面
        event.register(
            mio_icif_menus.POWDER_ADVANCED_ELC_MENU_TYPE.get(),
            mio_icif_gui_powder_advanced_elc::new
        );

        // 注册进阶压缩机的 GUI 界面
        event.register(
            mio_icif_menus.COMPRESSOR_ADVANCED_ELC_MENU_TYPE.get(),
            mio_icif_gui_compressor_advanced_elc::new
        );

        // 注册挤压机的 GUI 界面
        event.register(
            mio_icif_menus.METAL_FORMER_ADVANCED_MENU_TYPE.get(),
            mio_icif_gui_metal_former_advanced::new
        );

        // 注册洗矿机的 GUI 界面
        event.register(
            mio_icif_menus.WASHER_ELC_MENU_TYPE.get(),
            mio_icif_gui_washer_elc::new
        );

        // 注册发酵机的 GUI 界面
        event.register(
            mio_icif_menus.FERMENTER_ELC_MENU_TYPE.get(),
            mio_icif_gui_fermenter_elc::new
        );

        // 注册原油精炼机的 GUI 界面
        event.register(
            mio_icif_menus.OIL_REFINERY_ELC_MENU_TYPE.get(),
            mio_icif_gui_oil_refinery_elc::new
        );

        // 娉ㄥ唽楂樼倝鐨凣UI 鐣岄潰
        event.register(
            mio_icif_menus.BLAST_FURNACE_MENU_TYPE.get(),
            mio_icif_gui_blast_furnace::new
        );

        event.register(
            mio_icif_menus.BLAST_FURNACE_ELC_MENU_TYPE.get(),
            mio_icif_gui_blast_furnace_elc::new
        );

        event.register(
            mio_icif_menus.BLAST_FURNACE_ADVANCED_MENU_TYPE.get(),
            mio_icif_gui_blast_furnace_advanced::new
        );

        // 注册蒸汽机的 GUI 界面
        event.register(
            mio_icif_menus.STEAM_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_steam_generator::new
        );

        // 注册蒸汽再加压机的GUI 界面
        event.register(
            mio_icif_menus.STEAM_REPRESSURIZER_MENU_TYPE.get(),
            mio_icif_gui_steam_repressurizer::new
        );

        // 注册太阳能蒸馏机的GUI 界面
        event.register(
            mio_icif_menus.SOLAR_DISTILLER_MENU_TYPE.get(),
            mio_icif_gui_solar_distiller::new
        );

        // 注册冷凝机的 GUI 界面
        event.register(
            mio_icif_menus.CONDENSER_MENU_TYPE.get(),
            mio_icif_gui_condenser::new
        );

        // 注册方块切割机的 GUI 界面
        event.register(
            mio_icif_menus.BLOCK_CUTTER_MENU_TYPE.get(),
            mio_icif_gui_block_cutter::new
        );

        // 注册车床的GUI 界面
        event.register(
            mio_icif_menus.LATHE_MENU_TYPE.get(),
            mio_icif_gui_lathe::new
        );

        // 注册蒸汽动能发生机的 GUI 界面
        event.register(
            mio_icif_menus.STEAM_KINETIC_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_steam_kinetic_generator::new
        );

        // 注册热能离心机的 GUI 界面
        event.register(
            mio_icif_menus.CENTRIFUGE_ELC_MENU_TYPE.get(),
            mio_icif_gui_centrifuge_elc::new
        );

        // 注册提取机的 GUI 界面
        event.register(
            mio_icif_menus.EXTRACTOR_ELC_MENU_TYPE.get(),
            mio_icif_gui_extrator_elc::new
        );

        // 注册地热发电机的 GUI 界面
        event.register(
            mio_icif_menus.GEO_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_geo_generator::new
        );

        // 注册太阳能发电机的GUI 界面
        event.register(
            mio_icif_menus.SOLAR_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_solar_generator::new
        );

        // 注册地磁发电机的 GUI 界面
        event.register(
            mio_icif_menus.GEOMAGNETIC_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_geomagnetic_generator::new
        );

        // 注册柴油发电机的 GUI 界面
        event.register(
            mio_icif_menus.DIESEL_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_diesel_generator::new
        );

        // 注册风力发电机的 GUI 界面
        event.register(
            mio_icif_menus.WIND_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_wind_generator::new
        );

        // 注册水力发电机的 GUI 界面
        event.register(
            mio_icif_menus.WATER_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_water_generator::new
        );

        // 注册半流质发电机的GUI 界面
        event.register(
            mio_icif_menus.SEMIFLUID_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_semifluid_generator::new
        );

        // 注册核反应堆发电机的 GUI 界面
        event.register(
            mio_icif_menus.NUCLEAR_REACTOR_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_nuclear_reactor_generator::new
        );

        // 注册电力发热机的 GUI 界面
        event.register(
            mio_icif_menus.HEAT_GENERATOR_ELC_MENU_TYPE.get(),
            mio_icif_gui_heat_generator_elc::new
        );

        // 注册固体加热机的 GUI 界面
        event.register(
            mio_icif_menus.SOLID_HEAT_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_solid_heat_generator::new
        );

        // 注册流体加热机的 GUI 界面
        event.register(
            mio_icif_menus.FLUID_HEAT_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_fluid_heat_generator::new
        );

        // 注册放射性同位素温差加热机的 GUI 界面
        event.register(
            mio_icif_menus.RT_HEAT_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_rt_heat_generator::new
        );

        // 注册斯特林发电机的GUI 界面
        event.register(
            mio_icif_menus.STIRLING_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_stirling_generator::new
        );

        event.register(
            mio_icif_menus.DROP_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_drop_generator::new
        );

        event.register(
            mio_icif_menus.ADVANCED_DROP_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_advanced_drop_generator::new
        );

        event.register(
            mio_icif_menus.ADVANCED_STIRLING_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_advanced_stirling_generator::new
        );

        event.register(
            mio_icif_menus.ADVANCED_SEMIFLUID_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_advanced_semifluid_generator::new
        );

        event.register(
            mio_icif_menus.EXPERIENCE_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_experience_generator::new
        );

        event.register(
            mio_icif_menus.ADVANCED_EXPERIENCE_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_advanced_experience_generator::new
        );

        // 注册动能发电机的 GUI 界面
        event.register(
            mio_icif_menus.KINETIC_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_kinetic_generator::new
        );

        event.register(
            mio_icif_menus.TURBO_KINETIC_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_turbo_kinetic_generator::new
        );

        event.register(
            mio_icif_menus.TWIN_TURBO_KINETIC_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_twin_turbo_kinetic_generator::new
        );

        // 注册电力动能机的 GUI 界面
        event.register(
            mio_icif_menus.KINETIC_GENERATOR_ELC_MENU_TYPE.get(),
            mio_icif_gui_kinetic_generator_elc::new
        );

        // 注册风力动能发生机的 GUI 界面
        event.register(
            mio_icif_menus.WIND_KINETIC_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_wind_kinetic_generator::new
        );

        // 注册水力动能发生机的 GUI 界面
        event.register(
            mio_icif_menus.WATER_KINETIC_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_water_kinetic_generator::new
        );

        // 注册斯特林动能发生机的GUI 界面
        event.register(
            mio_icif_menus.STIRLING_KINETIC_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_stirling_kinetic_generator::new
        );

        // 注册变压器的 GUI 界面
        event.register(
            mio_icif_menus.TRANSFORMER_MENU_TYPE.get(),
            mio_icif_gui_transformer::new
        );

        // 注册期货机的 GUI 界面
        event.register(
            mio_icif_menus.FUTURE_ELC_MENU_TYPE.get(),
            mio_icif_gui_future_elc::new
        );

        // 注册热交换机的GUI 界面
        event.register(
            mio_icif_menus.HEAT_SOURCE_FLUID_MENU_TYPE.get(),
            mio_icif_gui_heat_source_fluid::new
        );

        // 注册压缩机的 GUI 界面
        event.register(
            mio_icif_menus.COMPRESSOR_ELC_MENU_TYPE.get(),
            mio_icif_gui_compressor_elc::new
        );

        // 注册装罐机的 GUI 界面
        event.register(
            mio_icif_menus.CANNER_ELC_MENU_TYPE.get(),
            mio_icif_gui_canner_elc::new
        );

        // 注册采矿机的 GUI 界面
        event.register(
            mio_icif_menus.MINER_ELC_MENU_TYPE.get(),
            mio_icif_gui_miner_elc::new
        );

        // 注册高级采矿机的 GUI 界面
        event.register(
            mio_icif_menus.ADVANCED_MINER_ELC_MENU_TYPE.get(),
            mio_icif_gui_advanced_miner_elc::new
        );

        // 注册泵的 GUI 界面
        event.register(
            mio_icif_menus.PUMP_ELC_MENU_TYPE.get(),
            mio_icif_gui_pump_elc::new
        );

        // 注册电解机的 GUI 界面
        event.register(
            mio_icif_menus.ELECTROLYZER_MENU_TYPE.get(),
            mio_icif_gui_electrolyzer_elc::new
        );

        // 注册回收机的 GUI 界面
        event.register(
            mio_icif_menus.RECYCLER_ELC_MENU_TYPE.get(),
            mio_icif_gui_recycler_elc::new
        );

        // 娉ㄥ唽鎰熷簲鐐夌殑 GUI 鐣岄潰
        event.register(
            mio_icif_menus.INDUCTION_ELC_MENU_TYPE.get(),
            mio_icif_gui_induction_elc::new
        );

        // 注册UU物质生成机的 GUI 界面
        event.register(
            mio_icif_menus.MATTER_ELC_MENU_TYPE.get(),
            mio_icif_gui_matter_elc::new
        );

        // 注册粒子聚合发生器的 GUI 界面
        event.register(
            mio_icif_menus.NEUTRON_POLYMERIZER_MENU_TYPE.get(),
            mio_icif_gui_neutron_polymerizer::new
        );

        // 注册金属成型机的 GUI 界面
        event.register(
            mio_icif_menus.METAL_FORMER_MENU_TYPE.get(),
            mio_icif_gui_metal_former::new
        );

        // 注册 OD 扫描器的 GUI 界面
        event.register(
            mio_icif_tool_menus.OD_SCANNER_MENU.get(),
            mio_icif_gui_od_scanner::new
        );

        // 注册作物监管机的 GUI 界面
        event.register(
            mio_icif_menus.MATRON_ELC_MENU_TYPE.get(),
            mio_icif_gui_matron_elc::new
        );

        // 注册流体核反应堆流体端口的GUI 界面
        event.register(
            mio_icif_menus.REACTOR_FLUID_PORT_MENU_TYPE.get(),
            mio_icif_gui_reactor_fluid_port::new
        );

        // 注册流体核反应堆的GUI 界面
        event.register(
            mio_icif_menus.FLUID_REACTOR_MENU_TYPE.get(),
            mio_icif_gui_nuclear_reactor_fluid::new
        );

        // 注册反应堆冷却液注入器的 GUI 界面
        event.register(
            mio_icif_menus.REDSTONE_REACTOR_COOLANT_INJECTOR_MENU_TYPE.get(),
            mio_icif_gui_redstone_reactor_coolant_injector::new
        );

        // 注册青金石反应堆冷却液注入器的GUI 界面
        event.register(
            mio_icif_menus.LAPIS_REACTOR_COOLANT_INJECTOR_MENU_TYPE.get(),
            mio_icif_gui_lapis_reactor_coolant_injector::new
        );

        // 注册核弹的GUI 界面
        event.register(
            mio_icif_menus.NUKE_MENU_TYPE.get(),
            mio_icif_gui_nuke::new
        );

        // 注册放射性同位素温差发电机的 GUI 界面
        event.register(
            mio_icif_menus.RT_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_rt_generator::new
        );

        // 注册高级太阳能发电机的 GUI 界面
        event.register(
            mio_icif_menus.ADVANCED_SOLAR_PANEL_MENU_TYPE.get(),
            mio_icif_gui_advanced_solar_panel::new
        );

        // 注册混合太阳能发电机的 GUI 界面
        event.register(
            mio_icif_menus.HYBRID_SOLAR_PANEL_MENU_TYPE.get(),
            mio_icif_gui_hybrid_solar_panel::new
        );

        // 注册终极混合太阳能发电机的 GUI 界面
        event.register(
            mio_icif_menus.ULTIMATE_HYBRID_SOLAR_PANEL_MENU_TYPE.get(),
            mio_icif_gui_ultimate_hybrid_solar_panel::new
        );

        // 注册量子太阳能发电机的 GUI 界面
        event.register(
            mio_icif_menus.QUANTUM_SOLAR_PANEL_MENU_TYPE.get(),
            mio_icif_gui_quantum_solar_panel::new
        );

        // 注册METS进阶太阳能发电机的 GUI 界面
        event.register(
            mio_icif_menus.METS_ADVANCED_SOLAR_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_mets_advanced_solar_generator::new
        );

        // 注册光子振谐太阳能发电机的 GUI 界面
        event.register(
            mio_icif_menus.PHOTON_RESONANCE_SOLAR_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_photon_resonance_solar_generator::new
        );

        // 注册终极光子振谐太阳能发电机的 GUI 界面
        event.register(
            mio_icif_menus.ULTIMATE_PHOTON_RESONANCE_SOLAR_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_ultimate_photon_resonance_solar_generator::new
        );

        // 注册量子发电机的 GUI 界面
        event.register(
            mio_icif_menus.QUANTUM_GENERATOR_MENU_TYPE.get(),
            mio_icif_gui_quantum_generator::new
        );

        // 注册磁化机的 GUI 界面
        event.register(
            mio_icif_menus.MAGNETIZER_MENU_TYPE.get(),
            mio_icif_gui_magnetizer::new
        );

        // 注册作物收割机的 GUI 界面
        event.register(
            mio_icif_menus.HARVEST_ELC_MENU_TYPE.get(),
            mio_icif_gui_harvest_elc::new
        );

        // 注册模式扫描机的 GUI 界面
        event.register(
            mio_icif_menus.SCANNER_ELC_MENU_TYPE.get(),
            mio_icif_gui_scanner_elc::new
        );

        // 注册分子重组仪的 GUI 界面
        event.register(
            mio_icif_menus.MOLECULAR_TRANSFORMER_MENU_TYPE.get(),
            mio_icif_gui_molecular_transformer::new
        );

        // 注册模式存储机的 GUI 界面
        event.register(
            mio_icif_menus.PATTERN_STORAGE_MENU_TYPE.get(),
            mio_icif_gui_pattern_storage::new
        );

        // 注册复制机的 GUI 界面
        event.register(
            mio_icif_menus.REPLICATOR_ELC_MENU_TYPE.get(),
            mio_icif_gui_replicator_elc::new
        );

        // 注册储物箱的 GUI 界面（按槽位数量分别注册）
        event.register(mio_icif_menus.STORAGE_BOX_MENU_27.get(), mio_icif_gui_storage_box::new);
        event.register(mio_icif_menus.STORAGE_BOX_MENU_45.get(), mio_icif_gui_storage_box::new);
        event.register(mio_icif_menus.STORAGE_BOX_MENU_63.get(), mio_icif_gui_storage_box::new);
        event.register(mio_icif_menus.STORAGE_BOX_MENU_84.get(), mio_icif_gui_storage_box::new);
        event.register(mio_icif_menus.STORAGE_BOX_MENU_126.get(), mio_icif_gui_storage_box::new);

        // 注册 EU电表的GUI 界面
        event.register(
            mio_icif_tool_menus.EU_METER_MENU.get(),
            mio_icif_gui_eu_meter::new
        );

        // 注册作物分析仪的 GUI 界面
        event.register(
            mio_icif_tool_menus.CROP_ANALYZER_MENU.get(),
            mio_icif_gui_crop_analyzer::new
        );

        // 注册酒桶的GUI 界面
        event.register(
            mio_icif_menus.BARREL_MENU_TYPE.get(),
            mio_icif_gui_barrel::new
        );

        // 娉ㄥ唽鐗╁搧缂撳啿鏈虹殑 GUI 鐣岄潰
        event.register(
            mio_icif_menus.ITEM_BUFFER_MENU_TYPE.get(),
            mio_icif_gui_item_buffer::new
        );

        // 注册电动分拣机的 GUI 界面
        event.register(
            mio_icif_menus.SORTER_ELC_MENU_TYPE.get(),
            mio_icif_gui_sorter_elc::new
        );

        // 注册高级物品分配机的 GUI 界面
        event.register(
            mio_icif_menus.ITEM_DISTRIBUTOR_ELC_MENU_TYPE.get(),
            mio_icif_gui_item_distributor_elc::new
        );

        // 注册流体分配机的 GUI 界面
        event.register(
            mio_icif_menus.FLUID_DISTRIBUTOR_ELC_MENU_TYPE.get(),
            mio_icif_gui_fluid_distributor_elc::new
        );

        // 注册高级流体分配机的 GUI 界面
        event.register(
            mio_icif_menus.WEIGHTED_FLUID_DISTRIBUTOR_ELC_MENU_TYPE.get(),
            mio_icif_gui_weighted_fluid_distributor_elc::new
        );

        // 注册流体流量调节机的 GUI 界面
        event.register(
            mio_icif_menus.FLUID_REGULATOR_ELC_MENU_TYPE.get(),
            mio_icif_gui_fluid_regulator_elc::new
        );

        // 注册工业工作台的 GUI 界面
        event.register(
            mio_icif_menus.INDUSTRIAL_WORKBENCH_MENU_TYPE.get(),
            mio_icif_gui_industrial_workbench::new
        );

        event.register(
            mio_icif_menus.BATCH_CRAFTER_MENU_TYPE.get(),
            mio_icif_gui_batch_crafter::new
        );

        event.register(
            mio_icif_menus.CHUNK_LOADER_MENU_TYPE.get(),
            mio_icif_gui_chunk_loader::new
        );

        // 注册GESU核心的 GUI 界面
        event.register(
            mio_icif_menus.GESU_CORE_MENU_TYPE.get(),
            mio_icif_gui_gesu_core::new
        );

        // 注册大型物质生成机核心的 GUI 界面
        event.register(
            mio_icif_menus.LARGE_FABRICATOR_CORE_MENU_TYPE.get(),
            mio_icif_gui_large_fabricator_core::new
        );

        // 注册石油钻机面板的 GUI 界面
        event.register(
            mio_icif_menus.OIL_RIG_PANEL_MENU_TYPE.get(),
            mio_icif_gui_oil_rig_panel::new
        );
    }
}