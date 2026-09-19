package dev.riqvip.tameall.companion;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Localized fallback names for companions without an explicit nametag. */
public final class CompanionNames {
    private CompanionNames() {}

    public static String localizedType(String creatureType) {
        if (creatureType == null || creatureType.isBlank()) return "Companion";
        Identifier id = Identifier.tryParse(creatureType);
        if (id == null) return creatureType;
        return BuiltInRegistries.ENTITY_TYPE.get(id)
                .map(holder -> holder.value().getDescription().getString())
                .orElse(creatureType);
    }

    public static String displayName(String explicitName, String creatureType) {
        return explicitName == null || explicitName.isBlank()
                ? localizedType(creatureType) : explicitName;
    }
}
