package com.createatomic.client;

import com.createatomic.block.entity.ReactorCoreBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/** Draws a rotating Create shaft through the core along its axis. */
public class ReactorCoreRenderer extends KineticBlockEntityRenderer<ReactorCoreBlockEntity> {

    public ReactorCoreRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected BlockState getRenderedBlockState(ReactorCoreBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }
}
