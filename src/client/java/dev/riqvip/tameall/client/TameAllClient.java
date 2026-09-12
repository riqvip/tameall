package dev.riqvip.tameall.client;

import net.fabricmc.api.ClientModInitializer;
import dev.riqvip.tameall.client.MenuScreensBridge;

/** Client entrypoint for packet receivers and client-only rendering hooks. */
public final class TameAllClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreensBridge.registerCompanion();
        CompanionClientNetworking.register();
    }
}
