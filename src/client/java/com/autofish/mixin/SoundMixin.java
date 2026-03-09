package com.autofish.mixin;

import com.autofish.AutoFishClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class SoundMixin {
    @Inject(method = "onPlaySound", at = @At("HEAD"))
    private void catchSound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        String soundName = packet.getSound().value().toString();
        // Pass the sound name, pitch, volume, AND exact X, Y, Z coordinates to the main mod
        AutoFishClient.onSoundDetected(soundName, packet.getPitch(), packet.getVolume(), packet.getX(), packet.getY(), packet.getZ());
    }
}
