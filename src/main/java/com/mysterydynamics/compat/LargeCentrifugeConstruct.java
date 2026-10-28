package com.mysterydynamics.compat;

import com.simibubi.create.AllBlocks;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchBookApi;
import thaumcraft.common.registry.TCBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * 大型奥法离心机结构页。
 *
 * <p>3×3×3：底层 3×3 奥术石砖，中心动力轴承，橙色位置的炼金构材；
 * 中层中心炼金构材；顶层中心离合器，其余为空气。</p>
 */
public final class LargeCentrifugeConstruct {

    private LargeCentrifugeConstruct() {
    }

    public static ResearchBookApi.Construct create() {
        int width = 3;
        int height = 3;
        int depth = 3;
        List<ItemStack> blocks = new ArrayList<>(width * height * depth);

        // ConstructDisplay 的 y 轴向下画，列表从顶层开始。
        for (int y = height - 1; y >= 0; y--) {
            for (int z = depth - 1; z >= 0; z--) {
                for (int x = width - 1; x >= 0; x--) {
                    blocks.add(stackAt(x, y, z));
                }
            }
        }

        // 结构页底部显示的就是法杖激活消耗，与实际触发共用同一份数据。
        AspectList cost = LargeCentrifugeTrigger.activationCost();
        return new ResearchBookApi.Construct(cost, List.copyOf(blocks), width, height, depth);
    }

    private static ItemStack stackAt(int x, int y, int z) {
        if (y == 2) {
            return x == 1 && z == 1
                    ? new ItemStack(AllBlocks.CLUTCH.get())
                    : ItemStack.EMPTY;
        }
        if (y == 1) {
            return x == 1 && z == 1
                    ? new ItemStack(TCBlocks.ALCHEMICAL_CONSTRUCT.get())
                    : ItemStack.EMPTY;
        }
        if (x == 1 && z == 1) {
            return new ItemStack(AllBlocks.MECHANICAL_BEARING.get());
        }
        if (x == 1 && z == 2) {
            return new ItemStack(TCBlocks.ALCHEMICAL_CONSTRUCT.get());
        }
        return new ItemStack(TCBlocks.ARCANE_STONE_BRICKS.get());
    }
}
