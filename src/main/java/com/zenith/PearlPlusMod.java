package com.zenith;

import com.zenith.config.Config;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PearlPlusMod implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("PearlPlus");
    public static Config config;

    public static String getPlayerName() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) {
            return "";
        }
        return mc.player.getGameProfile().getName();
    }

    @Override
    public void onInitializeClient() {
        config = Config.loadConfig();
    }
}
