package com.shadowclient.mixin;

import com.shadowclient.FreecamController;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void shadowclient$moveDetachedCamera(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!FreecamController.isEnabled()) return;
        Vec3 position = FreecamController.getCameraPosition();
        ((CameraAccessor) this).shadowclient$setPosition(position.x, position.y, position.z);
    }
}
