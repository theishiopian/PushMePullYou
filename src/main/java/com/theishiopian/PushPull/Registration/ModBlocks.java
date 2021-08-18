package com.theishiopian.PushPull.Registration;

import com.theishiopian.PushPull.Blocks.WinchBlock;
import com.theishiopian.PushPull.PushMePullYou;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PushMePullYou.MOD_ID);
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PushMePullYou.MOD_ID);

    public static final RegistryObject<Block> WINCH = BLOCKS.register
    ("winch", () ->
        {
            Block winch = new WinchBlock(BlockBehaviour.Properties.of(Material.STONE).requiresCorrectToolForDrops().strength(3.5F));
            BLOCK_ITEMS.register("winch", () -> new BlockItem(winch, new Item.Properties().tab(CreativeModeTab.TAB_REDSTONE)));

            return winch;
        }
    );
}
