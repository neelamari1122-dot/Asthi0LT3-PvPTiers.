
package com.pvptiers.mod.mixin;

import com.pvptiers.mod.PvpTiersClient;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class PlayerTierHeadVisibilityMixin {
    @Inject(method = "shouldShowName",
            at = @At("RETURN"), cancellable = true)
    private void pvptiers$showHeadTag(
            Avatar avatar,
            double distance,
            CallbackInfoReturnable<Boolean> cir) {

        if (avatar instanceof Player player &&
            PvpTiersClient.getRank(
                player.getGameProfile().getName()
            ) != null) {
            cir.setReturnValue(true);
        }
    }
}
