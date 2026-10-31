package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mysterydynamics.block.ArcaneTurbineBlock;
import com.mysterydynamics.block.ArcaneTurbineBlockEntity;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** 只渲染旋转的扇叶与传动杆，外壳由方块模型渲染。 */
public class ArcaneTurbineRenderer implements BlockEntityRenderer<ArcaneTurbineBlockEntity> {

    public ArcaneTurbineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ArcaneTurbineBlockEntity turbine, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        boolean lower = turbine.getBlockState().getValue(ArcaneTurbineBlock.HALF)
                == DoubleBlockHalf.LOWER;
        PartialModel model = lower
                ? ArcaneTurbinePartialModels.LOWER_ROTOR
                : ArcaneTurbinePartialModels.UPPER_ROTOR;
        SuperByteBuffer buffer = CachedBuffers.partial(model, turbine.getBlockState());
        if (buffer.isEmpty()) {
            return;
        }
        buffer.rotateYCenteredDegrees(turbine.getRenderAngle(partialTick));
        buffer.light(packedLight);
        buffer.renderInto(pose, buffers.getBuffer(RenderType.cutout()));
    }
}
