package com.omnica.common.crafter;

import com.omnica.common.blockentity.NexusNodeBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Blocks;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Native coupling with vanilla 1.21 Crafters.
 * Monitors recipe readiness, pulses the Crafter, and captures crafted output for network distribution.
 */
public class CrafterCoupler {

    public static boolean isAttachedToCrafter(ServerWorld world, NexusNodeBlockEntity node) {
        BlockPos attachedPos = node.getAttachedBlockPos();
        return world.getBlockState(attachedPos).isOf(Blocks.CRAFTER);
    }

    /**
     * Attempts to execute an automated craft cycle on an attached Crafter if inputs are ready.
     */
    public static Optional<ItemStack> tryCraft(ServerWorld world, NexusNodeBlockEntity node) {
        BlockPos attachedPos = node.getAttachedBlockPos();
        if (!(world.getBlockEntity(attachedPos) instanceof CrafterBlockEntity crafter)) {
            return Optional.empty();
        }

        CraftingRecipeInput recipeInput = crafter.createRecipeInput();
        Optional<RecipeEntry<CraftingRecipe>> match = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, recipeInput, world);

        if (match.isEmpty()) {
            return Optional.empty();
        }

        RecipeEntry<CraftingRecipe> recipeEntry = match.get();
        ItemStack result = recipeEntry.value().craft(recipeInput, world.getRegistryManager());
        if (result.isEmpty()) {
            return Optional.empty();
        }

        // Consume ingredients from the Crafter grid
        for (int i = 0; i < crafter.size(); i++) {
            ItemStack stackInSlot = crafter.getStack(i);
            if (!stackInSlot.isEmpty()) {
                stackInSlot.decrement(1);
                crafter.setStack(i, stackInSlot);
            }
        }

        // Trigger Crafter sound & visual feedback
        world.playSound(null, attachedPos, SoundEvents.BLOCK_CRAFTER_CRAFT, SoundCategory.BLOCKS, 1.0f, 1.0f);

        return Optional.of(result);
    }
}
