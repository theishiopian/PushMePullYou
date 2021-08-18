package com.theishiopian.PushPull;

import com.theishiopian.PushPull.Registration.ModBlocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("pushpull")
public class PushMePullYou
{
    public static final String MOD_ID = "pushpull";
    public static final Logger LOGGER = LogManager.getLogger();

    public PushMePullYou()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.BLOCK_ITEMS.register(bus);
    }
}
