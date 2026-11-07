package com.mysterydynamics.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.InfernalBoilerBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;

/** 用 bbmodel 数据绘制整栋 4×4×5 锅炉模型。 */
public final class InfernalBoilerRenderer implements BlockEntityRenderer<InfernalBoilerBlockEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<BlockPos> RENDERED_THIS_FRAME = new HashSet<>();

    private record Vertex(float x, float y, float z, float u, float v) {
    }

    private record Quad(Vertex[] vertices, ResourceLocation texture, float nx, float ny, float nz,
                        boolean bright, boolean translucent) {
    }

    private record ModelData(List<Quad> quads) {
    }

    private static ModelData normal;
    private static ModelData overclock;
    private static final Map<ResourceLocation, TextureAtlasSprite> SPRITES = new HashMap<>();

    public InfernalBoilerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(InfernalBoilerBlockEntity boiler, float partialTick, PoseStack pose,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!boiler.isFormedMultiBlock()) {
            return;
        }
        RENDERED_THIS_FRAME.add(boiler.getBlockPos());
        // BER 的 pose 已经被 dispatcher 平移到 BE 位置，所以这里必须传 BE 位置，
        // 不能传真实相机位置，否则会再减一次相机坐标，模型直接飞出屏幕。
        Vec3 anchor = Vec3.atLowerCornerOf(boiler.getBlockPos());
        renderAt(boiler, pose, anchor, bufferSource, packedLight);
    }

    public static boolean wasRenderedThisFrame(BlockPos controllerPos) {
        return RENDERED_THIS_FRAME.contains(controllerPos);
    }

    public static void clearFrameCache() {
        RENDERED_THIS_FRAME.clear();
    }

    /** 资源重载（F3+T / 资源包切换）后丢弃已解析的模型和贴图，下次渲染时重新加载。 */
    public static void clearCaches() {
        normal = null;
        overclock = null;
        SPRITES.clear();
    }

    public static void renderAt(InfernalBoilerBlockEntity boiler, PoseStack pose, Vec3 camera,
                                MultiBufferSource bufferSource, int packedLight) {
        if (!boiler.isFormedMultiBlock()) {
            return;
        }
        // isFormedMultiBlock 已保证 origin 有效。
        BlockPos origin = boiler.boilerOrigin();
        ModelData data = boiler.isOverclock() ? overclock() : normal();
        if (data == null || data.quads().isEmpty()) {
            return;
        }

        pose.pushPose();
        pose.translate(origin.getX() - camera.x, origin.getY() - camera.y,
                origin.getZ() - camera.z);
        // 模型数据已平移为从 (0,0,0) 开始；绕 4×4 底座中心旋转。
        pose.translate(2.0D, 0.0D, 2.0D);
        pose.mulPose(Axis.YP.rotationDegrees(boiler.rotation() * 90.0F));
        pose.translate(-2.0D, 0.0D, -2.0D);

        Function<ResourceLocation, TextureAtlasSprite> atlas =
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        drawPass(data, false, RenderType.cutout(), pose, bufferSource, atlas, packedLight);
        drawPass(data, true, RenderType.translucent(), pose, bufferSource, atlas, packedLight);
        pose.popPose();
    }

    private static void drawPass(ModelData data, boolean translucent, RenderType renderType,
                                 PoseStack pose, MultiBufferSource bufferSource,
                                 Function<ResourceLocation, TextureAtlasSprite> atlas,
                                 int packedLight) {
        boolean any = false;
        for (Quad quad : data.quads()) {
            if (quad.translucent() == translucent) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        for (Quad quad : data.quads()) {
            if (quad.translucent() != translucent) {
                continue;
            }
            TextureAtlasSprite sprite = SPRITES.computeIfAbsent(quad.texture(), atlas);
            int light = quad.bright() ? LightTexture.FULL_BRIGHT : packedLight;
            for (Vertex vertex : quad.vertices()) {
                consumer.addVertex(pose.last(), vertex.x() / 16.0F, vertex.y() / 16.0F,
                                vertex.z() / 16.0F)
                        .setColor(255, 255, 255, 255)
                        .setUv(sprite.getU(vertex.u() / 16.0F),
                                sprite.getV(vertex.v() / 16.0F))
                        .setLight(light)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setNormal(pose.last(), quad.nx(), quad.ny(), quad.nz());
            }
        }
    }

    private static ModelData normal() {
        if (normal == null) {
            normal = load("infernal_boiler/formed");
        }
        return normal;
    }

    private static ModelData overclock() {
        if (overclock == null) {
            overclock = load("infernal_boiler/formed_overclock");
        }
        return overclock;
    }

    private static ModelData load(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID,
                path + ".json");
        try (var stream = Minecraft.getInstance().getResourceManager().open(id)) {
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, ResourceLocation> textures = new HashMap<>();
            for (var entry : root.getAsJsonObject("textures").entrySet()) {
                textures.put(entry.getKey(),
                        ResourceLocation.parse(entry.getValue().getAsString()));
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
                            rotation, texture, texture.getPath().contains("fire")
                                    || texture.getPath().contains("lava")
                                    || texture.getPath().contains("soul"),
                            texture.getPath().contains("warded_glass")));
                }
            }
            return new ModelData(List.copyOf(quads));
        } catch (Exception exception) {
            LOGGER.error("Failed to load infernal boiler model {}", path, exception);
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
                                   int rotation, ResourceLocation texture, boolean bright,
                                   boolean translucent) {
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
        float nx = direction.getStepX();
        float ny = direction.getStepY();
        float nz = direction.getStepZ();
        return new Quad(vertices, texture, nx, ny, nz, bright, translucent);
    }
}
