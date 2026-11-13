package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.villager.ModVillagers;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

/** 模组职业使用干净底层；生物群系服装层由 Mixin 跳过，职业服装和腰牌保留。 */
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
