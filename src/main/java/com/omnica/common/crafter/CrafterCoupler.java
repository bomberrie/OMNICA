package com.omnica.common.crafter;

import com.omnica.common.blockentity.NexusNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;

import java.util.Optional;

/**
 * Native coupling with vanilla Crafters.
 * Monitors recipe readiness, pulses the Crafter, and captures crafted output for network distribution.
 */
public class CrafterCoupler {

    public static boolean isAttachedToCrafter(ServerLevel level, NexusNodeBlockEntity node) {
        BlockPos attachedPos = node.getAttachedBlockPos();
        return level.getBlockState(attachedPos).is(Blocks.CRAFTER);
    }

    /**
     * Attempts to execute an automated craft cycle on an attached Crafter if inputs are ready.
     */
    public static Optional<ItemStack> tryCraft(ServerLevel level, NexusNodeBlockEntity node) {
        BlockPos attachedPos = node.getAttachedBlockPos();
        if (!(level.getBlockEntity(attachedPos) instanceof CrafterBlockEntity crafter)) {
            return Optional.empty();
        }

        CraftingInput recipeInput = CraftingInput.of(3, 3, crafter.getItems());
        Optional<RecipeHolder<CraftingRecipe>> match = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, recipeInput, level);

        if (match.isEmpty()) {
            return Optional.empty();
        }

        RecipeHolder<CraftingRecipe> recipeHolder = match.get();
        ItemStack result = recipeHolder.value().assemble(recipeInput);
        if (result.isEmpty()) {
            return Optional.empty();
        }

        // Consume ingredients from the Crafter grid
        for (int i = 0; i < crafter.getContainerSize(); i++) {
            ItemStack stackInSlot = crafter.getItem(i);
            if (!stackInSlot.isEmpty()) {
                stackInSlot.shrink(1);
                crafter.setItem(i, stackInSlot);
            }
        }

        // Trigger Crafter sound & visual feedback
        level.playSound(null, attachedPos, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 1.0f, 1.0f);

        return Optional.of(result);
    }
}
