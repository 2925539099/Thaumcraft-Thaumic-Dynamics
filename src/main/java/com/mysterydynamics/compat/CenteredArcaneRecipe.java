package com.mysterydynamics.compat;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.research.ResearchApi;

import java.util.List;

/**
 * 书页里水平居中显示、但合成时仍可左右平移的奥术配方。
 *
 * <p>显示用的图案补了空格（宽度 3，材料在中间列），而 {@link #matches} 用去掉空格后的
 * 内层图案在 3×3 工作台里扫描偏移，所以和原版一样，放在左/中/右都能合成。</p>
 */
public class CenteredArcaneRecipe extends ShapedArcaneRecipe {

    private final String researchKey;
    private final int innerWidth;
    private final int innerHeight;
    /** 去掉空格后的图案，按行优先存放，null 表示空位。 */
    private final Object[] innerInput;

    public CenteredArcaneRecipe(String research, ItemStack output, AspectList aspects,
                                int innerWidth, int innerHeight, Object... paddedRecipe) {
        super(research, output, aspects, paddedRecipe);
        this.researchKey = research == null ? "" : research;
        this.innerWidth = innerWidth;
        this.innerHeight = innerHeight;
        this.innerInput = new Object[innerWidth * innerHeight];

        int offsetX = (width - innerWidth) / 2;
        for (int y = 0; y < innerHeight; y++) {
            for (int x = 0; x < innerWidth; x++) {
                int index = y * width + x + offsetX;
                innerInput[y * innerWidth + x] = index >= 0 && index < input.length ? input[index] : null;
            }
        }
    }

    @Override
    public boolean matches(Container container, Level level, Player player) {
        // 工作台前 9 格是 3×3 合成格；布局异常时退回默认匹配
        if (container.getContainerSize() < 9) {
            return super.matches(container, level, player);
        }
        // 与 TC4 ShapedArcaneRecipe 一致：未完成对应研究时不匹配
        if (!researchKey.isEmpty() && (player == null || !ResearchApi.isComplete(player, researchKey))) {
            return false;
        }
        for (int offsetY = 0; offsetY <= 3 - innerHeight; offsetY++) {
            for (int offsetX = 0; offsetX <= 3 - innerWidth; offsetX++) {
                if (matchesAt(container, offsetX, offsetY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 图案内的格子必须匹配（空位必须为空），图案外的格子也必须为空。 */
    private boolean matchesAt(Container container, int offsetX, int offsetY) {
        for (int gridY = 0; gridY < 3; gridY++) {
            for (int gridX = 0; gridX < 3; gridX++) {
                ItemStack stack = container.getItem(gridY * 3 + gridX);
                int x = gridX - offsetX;
                int y = gridY - offsetY;
                Object ingredient = x >= 0 && x < innerWidth && y >= 0 && y < innerHeight
                        ? innerInput[y * innerWidth + x] : null;
                if (ingredient == null) {
                    if (!stack.isEmpty()) {
                        return false;
                    }
                } else if (!matchesIngredient(ingredient, stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean matchesIngredient(Object ingredient, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (ingredient instanceof ItemStack required) {
            return matchesStack(required, stack);
        }
        if (ingredient instanceof Item item) {
            return stack.is(item);
        }
        if (ingredient instanceof Block block) {
            return stack.is(block.asItem());
        }
        if (ingredient instanceof Ingredient vanilla) {
            return vanilla.test(stack);
        }
        if (ingredient instanceof ItemStack[] options) {
            for (ItemStack option : options) {
                if (matchesStack(option, stack)) {
                    return true;
                }
            }
        }
        if (ingredient instanceof List<?> options) {
            for (Object option : options) {
                if (option instanceof ItemStack optionStack && matchesStack(optionStack, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 同 TC4 的 checkItemEquals：物品相同即可；只有配方材料自带组件时才要求组件一致。 */
    private static boolean matchesStack(ItemStack required, ItemStack stack) {
        if (required.isEmpty() || !stack.is(required.getItem())) {
            return false;
        }
        return required.getComponentsPatch().isEmpty()
                || ItemStack.isSameItemSameComponents(required, stack);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(Container container) {
        return NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
    }
}
