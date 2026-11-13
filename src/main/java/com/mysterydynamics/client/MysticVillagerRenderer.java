package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.villager.ModVillagers;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

/**
 * 神秘使/炼金师/机械师使用无生物群系袍子的干净底层贴图。
 * 职业外套仍由原版 VillagerProfessionLayer 叠加，因此镂空处露出的是皮肤/裤子。
 */
public class MysticVillagerRenderer extends VillagerRenderer {

    private static final ResourceLocation CLEAN_BASE =
            ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID,
                    "textures/entity/villager/mystic_base_clean.png");

    public MysticVillagerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Villager villager) {
        var profession = villager.getVillagerData().getProfession();
        if (profession == ModVillagers.THAUMATURGE.get()
                || profession == ModVillagers.ALCHEMIST.get()
                || profession == ModVillagers.MECHANIST.get()) {
            return CLEAN_BASE;
        }
        return super.getTextureLocation(villager);
    }
}
