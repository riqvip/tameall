package dev.riqvip.tameall.mixin;

import net.minecraft.world.entity.monster.Vex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access to Vex's native countdown so expiry protection can be reversible. */
@Mixin(Vex.class)
public interface VexLifetimeAccess {
    @Accessor("hasLimitedLife")
    boolean tameall$hasLimitedLife();

    @Accessor("hasLimitedLife")
    void tameall$setHasLimitedLife(boolean value);

    @Accessor("limitedLifeTicks")
    int tameall$limitedLifeTicks();

    @Accessor("limitedLifeTicks")
    void tameall$setLimitedLifeTicks(int value);
}
