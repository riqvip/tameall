package dev.riqvip.tameall;

import dev.riqvip.tameall.companion.CompanionRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Reusable owner-only directory and manual-recall tool. */
public final class CompanionWhistleItem extends Item {
    public CompanionWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            CompanionRuntime.sendRoster(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
