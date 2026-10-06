package com.mymod.bigdog.mixin;

import com.mymod.bigdog.util.HorseColorUtil;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerStopRidingMixin {
    @Inject(method = "stopRiding", at = @At("HEAD"), cancellable = true)
    private void bigdog$lockColoredHorse(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        String color = HorseColorUtil.getRiddenColor(self);
        if (color == null) {
            return;
        }
        HorseEntity horse = (HorseEntity) self.getVehicle();
        if (!self.isSneaking() || !horse.isAlive() || !self.isAlive()) {
            return;
        }
        self.sendMessage(Text.literal(
                HorseColorUtil.DEMONIZED.equals(color) ? "魔化马拒绝让你下马" : "升华马拒绝让你下马"), true);
        self.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(horse));
        ci.cancel();
    }
}
