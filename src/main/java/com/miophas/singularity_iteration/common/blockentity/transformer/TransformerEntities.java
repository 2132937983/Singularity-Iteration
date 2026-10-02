package com.miophas.singularity_iteration.common.blockentity.transformer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity;
import com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Single source of truth for which entity class backs each transformer tier.
 *
 * <p>Root cause of the "works only after re-placing / GUI will not open after a
 * server restart" bug for the IV-LuV and LuV-ZPMV transformers: placement went
 * through the chunk substitution hook ({@code IndependentTransformerFactory.placed})
 * and produced an {@link IndependentTransformerBlockEntity}, but chunk loading
 * goes through {@code BlockEntity.loadStatic → BlockEntityType.create}, whose
 * registered factory still built the legacy {@code mio_icif_transformer_*} class.
 * After a restart the server therefore held a legacy entity that the independent
 * energy engine ignores, while clients rebuilt the independent class — the two
 * sides disagreed and the menu/engine both broke.
 *
 * <p>Every path (BlockEntityType factory, {@code newBlockEntity}, the chunk
 * substitution hook) now asks this class, so the entity class is identical for
 * placement, chunk load, client chunk rebuild and feature-flag state.
 */
public final class TransformerEntities {

    public enum Tier {
        LV_MV(32),
        MV_HV(128),
        HV_EV(512),
        EV_SC(2048),
        IV_LUV(8192),
        LUV_ZPMV(32768);

        public final long lowPacket;

        Tier(long lowPacket) {
            this.lowPacket = lowPacket;
        }
    }

    private TransformerEntities() {}

    public static boolean independent() {
        return IndependentEnergyMode.feature("transformers");
    }

    public static BlockEntity create(Tier tier, BlockPos pos, BlockState state) {
        if (independent()) {
            return new IndependentTransformerBlockEntity(type(tier).get(), pos, state, tier.lowPacket, 1);
        }
        return switch (tier) {
            case LV_MV -> new mio_icif_transformer_lTom(pos, state);
            case MV_HV -> new mio_icif_transformer_mToh(pos, state);
            case HV_EV -> new mio_icif_transformer_hToe(pos, state);
            case EV_SC -> new mio_icif_transformer_eTos(pos, state);
            case IV_LUV -> new mio_icif_transformer_iv(pos, state);
            case LUV_ZPMV -> new mio_icif_transformer_luv(pos, state);
        };
    }

    public static Supplier<BlockEntityType<BlockEntity>> type(Tier tier) {
        return switch (tier) {
            case LV_MV -> mio_icif_block_entities.TRANSFORMER_LV_MV;
            case MV_HV -> mio_icif_block_entities.TRANSFORMER_MV_HV;
            case HV_EV -> mio_icif_block_entities.TRANSFORMER_HV_EV;
            case EV_SC -> mio_icif_block_entities.TRANSFORMER_EV_SC;
            case IV_LUV -> mio_icif_block_entities.TRANSFORMER_IV_LUV;
            case LUV_ZPMV -> mio_icif_block_entities.TRANSFORMER_LUV_ZPMV;
        };
    }
}
