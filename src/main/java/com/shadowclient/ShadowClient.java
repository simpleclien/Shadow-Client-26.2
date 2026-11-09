package com.shadowclient;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShadowClient implements ClientModInitializer {
    public static final String MOD_ID = "shadowclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Shadow Client 26.2 initialized.");
    }
}
