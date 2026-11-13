package com.mysterydynamics.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.EssentiaIntegrationBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

/** 直接读取 bbmodel 转换出的 JSON，用方块图集手动绘制 3×3×3 集成存储站。 */
public final class EssentiaIntegrationRenderer implements BlockEntityRenderer<EssentiaIntegrationBlockEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, TextureAtlasSprite> SPRITES = new HashMap<>();
    private static ModelData model;
    private static final Set<BlockPos> RENDERED_THIS_FRAME = new HashSet<>();
    private static final RenderType LIQUID = RenderType.create(
            "mystic_essentia_liquid",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_TRANSLUCENT_SHADER)
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderType.NO_CULL)
                    .createCompositeState(false));

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
        RENDERED_THIS_FRAME.add(blockEntity.getBlockPos());
        draw(blockEntity, pose, bufferSource, packedLight);
    }

    public static boolean wasRenderedThisFrame(BlockPos pos) {
        return RENDERED_THIS_FRAME.contains(pos);
    }

    public static void clearFrameCache() {
        RENDERED_THIS_FRAME.clear();
    }

    /** 视锥兜底渲染：pose 为相机空间，需要自行平移到控制器。 */
    public static void renderAt(EssentiaIntegrationBlockEntity blockEntity, float partialTick,
                                PoseStack pose, Vec3 camera, MultiBufferSource bufferSource,
                                int packedLight) {
        if (!blockEntity.isFormed()) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        pose.pushPose();
        pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        draw(blockEntity, pose, bufferSource, packedLight);
        pose.popPose();
    }

    private static void draw(EssentiaIntegrationBlockEntity blockEntity, PoseStack pose,
                             MultiBufferSource bufferSource, int packedLight) {
        ModelData data = model();
        if (data == null || data.quads().isEmpty()) {
            return;
        }

        pose.pushPose();
        // 模型坐标 -16..32 以控制器方块为中心；最低点 -16 需要上移一格。
        pose.translate(0.0D, 1.0D, 0.0D);
        int rotation = blockEntity.getRotation();
        if (rotation != 0) {
            pose.translate(0.5D, 0.0D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(rotation * 90.0F));
            pose.translate(-0.5D, 0.0D, -0.5D);
        }

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
        drawEssentia(blockEntity, pose, bufferSource, packedLight);
        pose.popPose();
    }

    private static final double[][] TANKS = {
            {18.125, 17.225, -12, 29.125, 27.225, -2},
            {18.125, 17.225, 3, 29.125, 27.225, 13},
            {18.125, 17.225, 19, 29.125, 27.225, 29},
            {18.125, 1.225, -12, 29.125, 11.225, -2},
            {18.125, 1.225, 3, 29.125, 11.225, 13},
            {18.125, 1.225, 19, 29.125, 11.225, 29},
            {-13, 17.225, -12, -2, 27.225, -2},
            {-13, 17.225, 3, -2, 27.225, 13},
            {-13, 17.225, 19, -2, 27.225, 29},
            {-13, 1.225, -12, -2, 11.225, -2},
            {-13, 1.225, 3, -2, 11.225, 13},
            {-13, 1.225, 19, -2, 11.225, 29},
    };

    private static void drawEssentia(EssentiaIntegrationBlockEntity blockEntity, PoseStack pose,
                                     MultiBufferSource bufferSource, int packedLight) {
        pose.pushPose();
        pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        try {
            drawEssentiaScaled(blockEntity, pose, bufferSource, packedLight);
        } finally {
            pose.popPose();
        }
    }

    private static void drawEssentiaScaled(EssentiaIntegrationBlockEntity blockEntity, PoseStack pose,
                                           MultiBufferSource bufferSource, int packedLight) {
        AspectList aspects = blockEntity.getAspects();
        int total = aspects.totalAmount();
        if (total <= 0) return;
        Aspect dominant = null;
        int best = 0;
        for (Aspect aspect : aspects.aspects()) {
            int amount = aspects.amount(aspect);
            if (amount > best) {
                best = amount;
                dominant = aspect;
            }
        }
        if (dominant == null) return;

        int color = dominant.color();
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float fraction = Math.min(1.0F, total / (EssentiaIntegrationBlockEntity.CAPACITY / (float) TANKS.length));

        VertexConsumer fill = bufferSource.getBuffer(RenderType.debugFilledBox());
        for (double[] tank : TANKS) {
            double x1 = tank[0] + 1.2, y1 = tank[1] + 1.0, z1 = tank[2] + 1.2;
            double x2 = tank[3] - 1.2, z2 = tank[5] - 1.2;
            double y2 = y1 + (tank[4] - tank[1] - 2.0) * fraction;
            liquidCube(fill, pose, (float) x1, (float) y1, (float) z1,
                    (float) x2, (float) y2, (float) z2, r, g, b, 230, packedLight);
            // 罐体玻璃前面的液面，避免液体被玻璃深度遮挡。
            liquidQuad(fill, pose, (float) (x1 - 0.02), (float) y1, (float) z2,
                    (float) (x1 - 0.02), (float) y1, (float) z1,
                    (float) (x1 - 0.02), (float) y2, (float) z1,
                    (float) (x1 - 0.02), (float) y2, (float) z2,
                    r, g, b, 230, packedLight, -1, 0, 0);
            liquidQuad(fill, pose, (float) (x2 + 0.02), (float) y1, (float) z1,
                    (float) (x2 + 0.02), (float) y1, (float) z2,
                    (float) (x2 + 0.02), (float) y2, (float) z2,
                    (float) (x2 + 0.02), (float) y2, (float) z1,
                    r, g, b, 230, packedLight, 1, 0, 0);
            liquidQuad(fill, pose, (float) x1, (float) y1, (float) (z1 - 0.02),
                    (float) x2, (float) y1, (float) (z1 - 0.02),
                    (float) x2, (float) y2, (float) (z1 - 0.02),
                    (float) x1, (float) y2, (float) (z1 - 0.02),
                    r, g, b, 230, packedLight, 0, 0, -1);
            liquidQuad(fill, pose, (float) x2, (float) y1, (float) (z2 + 0.02),
                    (float) x1, (float) y1, (float) (z2 + 0.02),
                    (float) x1, (float) y2, (float) (z2 + 0.02),
                    (float) x2, (float) y2, (float) (z2 + 0.02),
                    r, g, b, 230, packedLight, 0, 0, 1);
        }
    }

    private static void liquidCube(VertexConsumer c, PoseStack pose,
                                   float x1, float y1, float z1, float x2, float y2, float z2,
                                   float r, float g, float b, int a, int light) {
        liquidQuad(c, pose, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, r, g, b, a, light, 0, 1, 0);
        liquidQuad(c, pose, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1, r, g, b, a, light, 0, -1, 0);
        liquidQuad(c, pose, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, r, g, b, a, light, 0, 0, -1);
        liquidQuad(c, pose, x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2, r, g, b, a, light, 0, 0, 1);
        liquidQuad(c, pose, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2, r, g, b, a, light, -1, 0, 0);
        liquidQuad(c, pose, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, r, g, b, a, light, 1, 0, 0);
    }

    private static void liquidQuad(VertexConsumer c, PoseStack pose,
                                   float x1, float y1, float z1, float x2, float y2, float z2,
                                   float x3, float y3, float z3, float x4, float y4, float z4,
                                   float r, float g, float b, int a, int light,
                                   float nx, float ny, float nz) {
        c.addVertex(pose.last(), x1, y1, z1).setColor(r, g, b, a / 255.0F);
        c.addVertex(pose.last(), x2, y2, z2).setColor(r, g, b, a / 255.0F);
        c.addVertex(pose.last(), x3, y3, z3).setColor(r, g, b, a / 255.0F);
        c.addVertex(pose.last(), x4, y4, z4).setColor(r, g, b, a / 255.0F);
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
                    {x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}, {x1, y1, z2}};
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
