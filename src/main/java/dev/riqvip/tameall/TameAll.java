package dev.riqvip.tameall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import dev.riqvip.tameall.menu.CompanionMenu;
import dev.riqvip.tameall.network.CompanionNetworking;
import dev.riqvip.tameall.companion.CompanionRuntime;
import dev.riqvip.tameall.companion.CompanionInventory;
import dev.riqvip.tameall.companion.CompanionContainer;
import dev.riqvip.tameall.companion.CompanionEquipment;
import dev.riqvip.tameall.companion.CompanionAttachments;

/** Main server/common entrypoint for TameAll. */
public final class TameAll implements ModInitializer {
	public static final String MOD_ID = "tameall";
	public static final String MOD_NAME = "TameAll";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ExtendedMenuType<CompanionMenu, Integer> COMPANION_MENU = Registry.register(
			BuiltInRegistries.MENU, id("companion_inventory"),
			new ExtendedMenuType<>(CompanionMenu::client, ByteBufCodecs.VAR_INT));

	@Override
	public void onInitialize() {
		ModItems.initialize();
		// Force the persistent cargo attachment to register during mod initialization,
		// rather than on the first player interaction.
		CompanionInventory.CARGO.toString();
		CompanionContainer.CONTENTS.toString();
		CompanionEquipment.ITEMS.toString();
		CompanionAttachments.STATE.toString();
		CompanionAttachments.PRESENTATION.toString();
		CompanionAttachments.GOLEM_PROVOCATION.toString();
		CompanionAttachments.SUMMON_AFFILIATION.toString();
		CompanionAttachments.CREEPER_COOLDOWN.toString();
		CompanionAttachments.TARGET_SELECTION.toString();
		CompanionAttachments.VEX_LIFETIME.toString();
		CompanionNetworking.registerCommon();
        CompanionRuntime.initialize();
        CompanionNetworking.registerServer(CompanionRuntime::handleAction);
        CompanionNetworking.registerRosterActionServer(CompanionRuntime::handleRosterAction);
        CompanionNetworking.registerTargetSelectionServer(CompanionRuntime::handleTargetSelection);
		LOGGER.info("{} common content initialized", MOD_NAME);
	}

	/** Creates an identifier in the TameAll namespace. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Creates an item key in the TameAll namespace. */
	public static ResourceKey<Item> itemKey(String path) {
		return ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id(path));
	}

}
