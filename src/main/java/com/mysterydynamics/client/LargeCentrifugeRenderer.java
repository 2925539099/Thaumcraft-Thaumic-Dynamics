package com.mysterydynamics.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.LargeCentrifugeControllerBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 实装大型奥法离心机模型的 BER：按炼狱锅炉方式使用方块图集 sprite。 */
public final class LargeCentrifugeRenderer implements BlockEntityRenderer<LargeCentrifugeControllerBlockEntity> {

    private record Vertex(float x, float y, float z, float u, float v) {
    }

    private record Quad(Vertex[] vertices, float nx, float ny, float nz, boolean translucent,
                        ResourceLocation texture, float tw, float th) {
    }

    private record ModelData(List<Quad> quads) {
    }

    private static ModelData staticModel;
    private static ModelData rotorModel;
    private static final Map<ResourceLocation, TextureAtlasSprite> SPRITES = new HashMap<>();

    public LargeCentrifugeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LargeCentrifugeControllerBlockEntity controller, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ModelData statics = staticModel();
        ModelData rotor = rotorModel();
        if (statics.quads().isEmpty() && rotor.quads().isEmpty()) {
            return;
        }

        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(controller.rotation() * 90.0F));
        pose.translate(-0.5D, -0.5D, -0.5D);
        draw(statics, pose, buffers, packedLight);

        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(-controller.spin(partialTick)));
        pose.translate(-0.5D, -0.5D, -0.5D);
        draw(rotor, pose, buffers, packedLight);
        pose.popPose();
        pose.popPose();
    }

    private static void draw(ModelData model, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        drawPass(model, false, RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS),
                pose, buffers, packedLight);
        drawPass(model, true, RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS),
                pose, buffers, packedLight);
    }

    private static void drawPass(ModelData model, boolean translucent, RenderType renderType,
                                 PoseStack pose, MultiBufferSource buffers, int packedLight) {
        boolean any = false;
        for (Quad quad : model.quads()) {
            if (quad.translucent() == translucent) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        VertexConsumer consumer = buffers.getBuffer(renderType);
        for (Quad quad : model.quads()) {
            if (quad.translucent() != translucent) {
                continue;
            }
            TextureAtlasSprite sprite = SPRITES.computeIfAbsent(quad.texture(), atlas);
            for (Vertex vertex : quad.vertices()) {
                consumer.addVertex(pose.last(), vertex.x() / 16.0F, vertex.y() / 16.0F,
                                vertex.z() / 16.0F)
                        .setColor(255, 255, 255, 255)
                        .setUv(sprite.getU(vertex.u() / quad.tw()),
                                sprite.getV(vertex.v() / quad.th()))
                        .setLight(packedLight)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setNormal(pose.last(), quad.nx(), quad.ny(), quad.nz());
            }
        }
    }

    private static ModelData staticModel() {
        if (staticModel == null) {
            staticModel = load("large_centrifuge/static.json");
        }
        return staticModel;
    }

    private static ModelData rotorModel() {
        if (rotorModel == null) {
            rotorModel = load("large_centrifuge/rotor.json");
        }
        return rotorModel;
    }

    private static ModelData load(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID, path);
        try (var stream = Minecraft.getInstance().getResourceManager().open(id)) {
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Quad> quads = new ArrayList<>();
            for (var element : root.getAsJsonArray("quads")) {
                JsonObject object = element.getAsJsonObject();
                JsonArray vertices = object.getAsJsonArray("v");
                int vertexCount = vertices.size();
                Vertex[] corners = new Vertex[vertexCount];
                for (int i = 0; i < vertexCount; i++) {
                    JsonArray value = vertices.get(i).getAsJsonArray();
                    corners[i] = new Vertex(value.get(0).getAsFloat(), value.get(1).getAsFloat(),
                            value.get(2).getAsFloat(), value.get(3).getAsFloat(),
                            value.get(4).getAsFloat());
                }
                JsonArray normal = object.getAsJsonArray("n");
                String texture = object.get("tex").getAsString();
                int colon = texture.indexOf(':');
                String namespace = colon > 0 ? texture.substring(0, colon) : MysteryDynamics.MODID;
                String texturePath = colon > 0 ? texture.substring(colon + 1) : texture;
                ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath(namespace, texturePath);
                quads.add(new Quad(corners, normal.get(0).getAsFloat(), normal.get(1).getAsFloat(),
                        normal.get(2).getAsFloat(), object.get("t").getAsBoolean(),
                        textureId, object.get("tw").getAsFloat(), object.get("th").getAsFloat()));
            }
            return new ModelData(List.copyOf(quads));
        } catch (Exception exception) {
            System.err.println("[mystery_dynamics] Failed to load large centrifuge model " + path
                    + ": " + exception);
            return new ModelData(List.of());
        }
    }
}
