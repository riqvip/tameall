package dev.riqvip.tameall;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/** Common item registry. Behavior is supplied by the companion subsystem. */
public final class ModItems {
	public static final java.lang.String GOLDEN_WHEAT_PATH = "golden_wheat";
	public static final net.minecraft.resources.ResourceKey<Item> GOLDEN_WHEAT_KEY = TameAll.itemKey(GOLDEN_WHEAT_PATH);

	/** The single-use universal companion binding item. */
	public static final Item GOLDEN_WHEAT = register(GOLDEN_WHEAT_KEY, Item::new, new Item.Properties().stacksTo(64));
	public static final java.lang.String GILDED_WHEAT_PATH = "gilded_wheat";
	public static final net.minecraft.resources.ResourceKey<Item> GILDED_WHEAT_KEY = TameAll.itemKey(GILDED_WHEAT_PATH);
	/** Six-to-one companion bond attempt and two-heart healing item. */
	public static final Item GILDED_WHEAT = register(GILDED_WHEAT_KEY, Item::new, new Item.Properties().stacksTo(64));
	public static final java.lang.String COMPANION_WHISTLE_PATH = "companion_whistle";
	public static final net.minecraft.resources.ResourceKey<Item> COMPANION_WHISTLE_KEY = TameAll.itemKey(COMPANION_WHISTLE_PATH);
	/** Reusable roster and manual recall item. */
	public static final Item COMPANION_WHISTLE = register(COMPANION_WHISTLE_KEY,
			CompanionWhistleItem::new, new Item.Properties().stacksTo(1));

	/** Registers the item and adds it to the Ingredients creative tab. */
	public static void initialize() {
		var ingredients = net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
				 net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "ingredients"));
		CreativeModeTabEvents.modifyOutputEvent(ingredients)
				.register(entries -> { entries.accept(GOLDEN_WHEAT); entries.accept(GILDED_WHEAT); entries.accept(COMPANION_WHISTLE); });
	}

	public static Item register(net.minecraft.resources.ResourceKey<Item> key,
			Function<Item.Properties, Item> factory, Item.Properties settings) {
		Item item = factory.apply(settings.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	private ModItems() {
	}
}
