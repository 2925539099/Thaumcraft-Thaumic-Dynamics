package com.mysterydynamics.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.EssentiaIntegrationBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

/** 直接读取 bbmodel 转换出的 JSON，用方块图集手动绘制 3×3×3 集成存储站。 */
public final class EssentiaIntegrationRenderer implements BlockEntityRenderer<EssentiaIntegrationBlockEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, TextureAtlasSprite> SPRITES = new HashMap<>();
    private static ModelData model;

    private record Vertex(float x, float y, float z, float u, float v) {
    }

    private record Quad(Vertex[] vertices, ResourceLocation texture, float nx, float ny, float nz) {
    }

    private record ModelData(List<Quad> quads) {
    }

    public EssentiaIntegrationRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EssentiaIntegrationBlockEntity blockEntity, float partialTick, PoseStack pose,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!blockEntity.isFormed()) {
            return;
        }
        ModelData data = model();
        if (data == null || data.quads().isEmpty()) {
            return;
        }

        pose.pushPose();
        // 模型坐标是 -16..32；控制器在底层中心，平移一格后正好覆盖 0..3 格。
        pose.translate(1.0D, 1.0D, 1.0D);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        for (Quad quad : data.quads()) {
            TextureAtlasSprite sprite = SPRITES.computeIfAbsent(quad.texture(), atlas);
            for (Vertex vertex : quad.vertices()) {
                consumer.addVertex(pose.last(), vertex.x() / 16.0F, vertex.y() / 16.0F,
                                vertex.z() / 16.0F)
                        .setColor(255, 255, 255, 255)
                        .setUv(sprite.getU(vertex.u() / 16.0F), sprite.getV(vertex.v() / 16.0F))
                        .setLight(packedLight)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setNormal(pose.last(), quad.nx(), quad.ny(), quad.nz());
            }
        }
        pose.popPose();
    }

    private static ModelData model() {
        if (model == null) {
            model = load();
        }
        return model;
    }

    private static ModelData load() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID,
                "models/block/essentia_integration_station.json");
        try (var stream = Minecraft.getInstance().getResourceManager().open(id)) {
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, ResourceLocation> textures = new HashMap<>();
            for (var entry : root.getAsJsonObject("textures").entrySet()) {
                textures.put(entry.getKey(), ResourceLocation.parse(entry.getValue().getAsString()));
            }
            List<Quad> quads = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("elements")) {
                JsonObject object = element.getAsJsonObject();
                float[] from = readVec(object.getAsJsonArray("from"));
                float[] to = readVec(object.getAsJsonArray("to"));
                for (var faceEntry : object.getAsJsonObject("faces").entrySet()) {
                    JsonObject face = faceEntry.getValue().getAsJsonObject();
                    String textureKey = face.get("texture").getAsString();
                    if (textureKey.startsWith("#")) {
                        textureKey = textureKey.substring(1);
                    }
                    ResourceLocation texture = textures.get(textureKey);
                    if (texture == null) {
                        continue;
                    }
                    float[] uv = face.has("uv") ? readVec(face.getAsJsonArray("uv"))
                            : new float[]{0, 0, 16, 16};
                    int rotation = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
                    quads.add(createQuad(Direction.byName(faceEntry.getKey()), from, to, uv,
                            rotation, texture));
                }
            }
            return new ModelData(List.copyOf(quads));
        } catch (Exception exception) {
            LOGGER.error("Failed to load essentia integration station model", exception);
            return new ModelData(List.of());
        }
    }

    private static float[] readVec(JsonArray array) {
        float[] values = new float[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return values;
    }

    private static Quad createQuad(Direction direction, float[] from, float[] to, float[] uv,
                                   int rotation, ResourceLocation texture) {
        float x1 = from[0], y1 = from[1], z1 = from[2];
        float x2 = to[0], y2 = to[1], z2 = to[2];
        float[][] corners = switch (direction) {
            case NORTH -> new float[][]{
                    {x2, y2, z1}, {x2, y1, z1}, {x1, y1, z1}, {x1, y2, z1}};
            case SOUTH -> new float[][]{
                    {x1, y2, z2}, {x1, y1, z2}, {x2, y1, z2}, {x2, y2, z2}};
            case WEST -> new float[][]{
                    {x1, y2, z1}, {x1, y1, z1}, {x1, y1, z2}, {x1, y2, z2}};
            case EAST -> new float[][]{
                    {x2, y2, z2}, {x2, y1, z2}, {x2, y1, z1}, {x2, y2, z1}};
            case UP -> new float[][]{
                    {x1, y2, z1}, {x1, y2, z2}, {x2, y2, z2}, {x2, y2, z1}};
            case DOWN -> new float[][]{
                    {x1, y1, z1}, {x1, y1, z2}, {x2, y1, z2}, {x2, y1, z1}};
            default -> new float[0][];
        };
        float[][] uvs = {
                {uv[0], uv[1]}, {uv[0], uv[3]}, {uv[2], uv[3]}, {uv[2], uv[1]}
        };
        int steps = (rotation / 90) & 3;
        for (int step = 0; step < steps; step++) {
            float[] first = uvs[0];
            uvs[0] = uvs[1];
            uvs[1] = uvs[2];
            uvs[2] = uvs[3];
            uvs[3] = first;
        }
        Vertex[] vertices = new Vertex[4];
        for (int i = 0; i < 4; i++) {
            vertices[i] = new Vertex(corners[i][0], corners[i][1], corners[i][2],
                    uvs[i][0], uvs[i][1]);
        }
        return new Quad(vertices, texture, direction.getStepX(), direction.getStepY(),
                direction.getStepZ());
    }
}
