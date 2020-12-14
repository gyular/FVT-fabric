package me.flourick.fvt.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.MessageType;
import net.minecraft.stat.StatHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.MathHelper;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.flourick.fvt.FVT;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin extends AbstractClientPlayerEntity
{
	public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile)
	{
		super(world, profile);
	}

	@Shadow
	int ticksLeftToDoubleTapSprint;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onConstructor(MinecraftClient client, ClientWorld world, ClientPlayNetworkHandler networkHandler, StatHandler stats, ClientRecipeBook recipeBook, boolean lastSneaking, boolean lastSprinting, CallbackInfo info)
	{
		if(FVT.OPTIONS.autoReconnect) {
			FVT.VARS.autoReconnectTries = 0;
		}
	}

	@Inject(method = "setShowsDeathScreen", at = @At("HEAD"))
	private void onSetShowsDeathScreen(CallbackInfo info)
	{
		if(FVT.VARS.isAfterDeath && FVT.OPTIONS.sendDeathCoordinates) {
			FVT.VARS.isAfterDeath = false;
			FVT.MC.inGameHud.addChatMessage(MessageType.CHAT, new LiteralText(String.format("You died at X: %.01f Z: %.01f Y: %.01f in %s!", FVT.VARS.getLastDeathX(), FVT.VARS.getLastDeathZ(), FVT.VARS.getLastDeathY(), FVT.VARS.getLastDeathWorld())), UUID.fromString("00000000-0000-0000-0000-000000000000"));
		}
	}

	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	private void onMove(CallbackInfo info)
	{
		if(FVT.VARS.freecam) {
			info.cancel();
		}
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasVehicle()Z", ordinal = 0), method = "tick()V")
	private boolean hijackHasVehicle(ClientPlayerEntity player)
	{
		if(FVT.VARS.freecam) {
			return false;
		}

		return this.hasVehicle();
	}

	@Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
	private void onTickMovement(CallbackInfo info)
	{
		if(FVT.OPTIONS.disableWToSprint) {
			this.ticksLeftToDoubleTapSprint = -1;
		}

		if(FVT.VARS.freecam) {
			float forward = FVT.MC.player.input.movementForward;
			float up = (FVT.MC.player.input.jumping ? 1.0f : 0.0f) - (FVT.MC.player.input.sneaking ? 1.0f : 0.0f);
            float side = FVT.MC.player.input.movementSideways;
			
            FVT.VARS.freecamForwardSpeed = forward != 0 ? updateMotion(FVT.VARS.freecamForwardSpeed, forward) : FVT.VARS.freecamForwardSpeed * 0.5f;
            FVT.VARS.freecamUpSpeed = up != 0 ?  updateMotion(FVT.VARS.freecamUpSpeed, up) : FVT.VARS.freecamUpSpeed * 0.5f;
            FVT.VARS.freecamSideSpeed = side != 0 ?  updateMotion(FVT.VARS.freecamSideSpeed , side) : FVT.VARS.freecamSideSpeed * 0.5f;

            double rotateX = Math.sin(FVT.VARS.freecamYaw * Math.PI / 180.0D);
			double rotateZ = Math.cos(FVT.VARS.freecamYaw * Math.PI / 180.0D);
			double speed = FVT.MC.player.isSprinting() ? 1.2D : 0.55D;

			FVT.VARS.prevFreecamX = FVT.VARS.freecamX;
			FVT.VARS.prevFreecamY = FVT.VARS.freecamY;
			FVT.VARS.prevFreecamZ = FVT.VARS.freecamZ;

			FVT.VARS.freecamX += (FVT.VARS.freecamSideSpeed * rotateZ - FVT.VARS.freecamForwardSpeed * rotateX) * speed;
			FVT.VARS.freecamY += FVT.VARS.freecamUpSpeed * speed;
			FVT.VARS.freecamZ += (FVT.VARS.freecamForwardSpeed * rotateZ + FVT.VARS.freecamSideSpeed * rotateX) * speed;
		}
	}

	private float updateMotion(float motion, float direction)
    {
        return (direction + motion == 0) ? 0.0f : MathHelper.clamp(motion + ((direction < 0) ? -0.35f : 0.35f), -1f, 1f);
    }

	// PREVENTS MOVEMENT (freecam)
	@Inject(method = "isCamera", at = @At("HEAD"), cancellable = true)
	private void onIsCamera(CallbackInfoReturnable<Boolean> info)
	{
		if(FVT.VARS.freecam) {
			info.setReturnValue(false);
		}
	}

	// PREVENTS SNEAKING (freecam)
	@Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void onIsSneaking(CallbackInfoReturnable<Boolean> info)
    {
        if(FVT.VARS.freecam) {
            info.setReturnValue(false);
        }
    }

	@Override
	public void changeLookDirection(double cursorDeltaX, double cursorDeltaY)
	{
		if(FVT.VARS.freecam) {
			FVT.VARS.freecamYaw += cursorDeltaX * 0.15D;
			FVT.VARS.freecamPitch = MathHelper.clamp(FVT.VARS.freecamPitch + cursorDeltaY * 0.15D, -90, 90);
		}
		else {
			super.changeLookDirection(cursorDeltaX, cursorDeltaY);
		}
	}
}