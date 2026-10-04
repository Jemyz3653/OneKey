package dev.jemyz.onekey.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import dev.jemyz.onekey.client.OneKeyClient;

/**
 * Invisible players: vanilla draws them translucent (with their name tag) for anyone they are not
 * "invisible to" - the same path spectators use. OneKey just says "not invisible to you".
 */
@Mixin(Entity.class)
abstract class EntityMixin {
	@Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
	private void onekey$seeInvisiblePlayers(Player viewer, CallbackInfoReturnable<Boolean> cir) {
		Entity self = (Entity) (Object) this;

		if (self instanceof Player
				&& self != viewer
				&& self.level().isClientSide()
				&& viewer == Minecraft.getInstance().player
				&& OneKeyClient.invisibleOn()) {
			cir.setReturnValue(false);
		}
	}
}
