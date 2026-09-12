package dev.riqvip.tameall.client;

import dev.riqvip.tameall.client.CompanionInventoryScreen;
import dev.riqvip.tameall.menu.CompanionMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Keeps the package-private vanilla screen constructor out of gameplay code. */
public final class MenuScreensBridge {
    private MenuScreensBridge() {}

    public static void registerCompanion() {
        // Fabric's menu class tweaker exposes this registration method at
        // runtime.  The Fabric menu API sends the extended opening payload and
        // invokes create(...) itself; trying to recreate an ExtendedMenuType
        // with the two-argument MenuType#create overload throws
        // UnsupportedOperationException.
        net.minecraft.client.gui.screens.MenuScreens.register(
                dev.riqvip.tameall.TameAll.COMPANION_MENU,
                (CompanionMenu menu, Inventory inventory, Component title) ->
                        new CompanionInventoryScreen(menu, inventory, title));
    }
}
