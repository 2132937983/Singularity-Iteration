package com.singularity_iteration.mio_icif.Items.Resource;

import net.minecraft.world.item.Item;

@SuppressWarnings("null")
public class mio_icif_nuclear_material extends Item {

    private final float radioactivity;

    public mio_icif_nuclear_material(Properties properties) {
        this(properties, 1.0F);
    }

    public mio_icif_nuclear_material(Properties properties, float radioactivity) {
        super(properties);
        this.radioactivity = radioactivity;
    }

    public mio_icif_nuclear_material(Properties properties, float radioactivity, boolean damageWhenHeld) {
        super(properties);
        this.radioactivity = radioactivity;
    }

    public float getRadioactivity() {
        return radioactivity;
    }
}

