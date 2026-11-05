package com.mysterydynamics.villager;

import com.google.common.collect.ImmutableSet;
import com.mysterydynamics.MysteryDynamics;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 三个工作站对应的村民职业与 POI。 */
public final class ModVillagers {

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, MysteryDynamics.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, MysteryDynamics.MODID);

    public static final DeferredHolder<PoiType, PoiType> THAUMATURGE_POI =
            POI_TYPES.register("thaumaturge_workstation", () -> new PoiType(
                    ImmutableSet.copyOf(MysteryDynamics.THAUMATURGE_WORKSTATION.get()
                            .getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<PoiType, PoiType> ALCHEMIST_POI =
            POI_TYPES.register("alchemy_workshop", () -> new PoiType(
                    ImmutableSet.copyOf(MysteryDynamics.ALCHEMY_WORKSHOP.get()
                            .getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<PoiType, PoiType> MECHANIST_POI =
            POI_TYPES.register("mechanist_workstation", () -> new PoiType(
                    ImmutableSet.copyOf(MysteryDynamics.MECHANIST_WORKSTATION.get()
                            .getStateDefinition().getPossibleStates()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> THAUMATURGE =
            PROFESSIONS.register("thaumaturge", () -> new VillagerProfession(
                    "thaumaturge",
                    holder -> holder.is(THAUMATURGE_POI.getKey()),
                    holder -> holder.is(THAUMATURGE_POI.getKey()),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_TOOLSMITH));
    public static final DeferredHolder<VillagerProfession, VillagerProfession> ALCHEMIST =
            PROFESSIONS.register("alchemist", () -> new VillagerProfession(
                    "alchemist",
                    holder -> holder.is(ALCHEMIST_POI.getKey()),
                    holder -> holder.is(ALCHEMIST_POI.getKey()),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CLERIC));
    public static final DeferredHolder<VillagerProfession, VillagerProfession> MECHANIST =
            PROFESSIONS.register("mechanist", () -> new VillagerProfession(
                    "mechanist",
                    holder -> holder.is(MECHANIST_POI.getKey()),
                    holder -> holder.is(MECHANIST_POI.getKey()),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_TOOLSMITH));

    private ModVillagers() {
    }

    public static void register(IEventBus bus) {
        POI_TYPES.register(bus);
        PROFESSIONS.register(bus);
    }
}
