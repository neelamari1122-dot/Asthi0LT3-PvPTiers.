package com.pvptiers.mod.mixin;

import com.pvptiers.mod.PvpMode;
import com.pvptiers.mod.PvpTiersClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client-only nameplates. Never mutates server state or sends commands to a Minecraft server. */
@Mixin(EntityRenderer.class)
public abstract class PlayerTierNametagMixin {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void pvptiers$display(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!(entity instanceof Player) || cir.getReturnValue() == null) return;
        PvpTiersClient.RankData data=PvpTiersClient.getRank(entity.getName().getString());
        if(data==null || data.tiers==null) return;
        PvpMode mode=PvpMode.parse(data.primary);
        if(mode==null) return;
        String tier=data.tiers.get(mode.id());
        if(tier==null) return;
        Component icon=PvpTiersClient.cfg.showIcons
            ? Component.literal(String.valueOf(mode.icon)).withStyle(style -> style.withFont(PvpTiersClient.ICON_FONT))
            : Component.empty();
        Component label=Component.literal(" " + mode.display + " [" + tier + "] ").withStyle(ChatFormatting.AQUA);
        cir.setReturnValue(Component.empty().append(icon).append(label).append(cir.getReturnValue().copy()));
    }
}
