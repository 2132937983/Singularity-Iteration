package com.singularity_iteration.mio_icif.Blocks.entity.generator;

/**
 * 核反应堆运行模式枚举
 */
@SuppressWarnings("null")
public enum mio_icif_reactor_mode {
    /**
     * 发电模式 - 默认模式，产生 EU 电力
     */
    GENERATOR("generator", "gui.mio_icif.reactor.mode.generator"),

    /**
     * 流体反应堆模式 - 只产生热量，不发电
     * 通过加热冷却液产生热冷却液
     */
    FLUID("fluid", "gui.mio_icif.reactor.mode.fluid");
    
    private final String name;
    private final String translationKey;
    
    mio_icif_reactor_mode(String name, String translationKey) {
        this.name = name;
        this.translationKey = translationKey;
    }
    
    public String getName() {
        return name;
    }
    
    public String getTranslationKey() {
        return translationKey;
    }
    
    /**
     * 从字符串获取模式
     */
    public static mio_icif_reactor_mode fromString(String mode) {
        for (mio_icif_reactor_mode value : values()) {
            if (value.name.equalsIgnoreCase(mode)) {
                return value;
            }
        }
        return GENERATOR; // 默认为发电模式
}
}


