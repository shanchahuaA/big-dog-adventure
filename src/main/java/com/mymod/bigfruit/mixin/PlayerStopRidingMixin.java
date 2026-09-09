package com.mymod.bigfruit.mixin;

import com.mymod.bigfruit.util.HorseColorUtil;
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
    private void bigfruit$lockColoredHorse(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (!(self.getVehicle() instanceof HorseEntity horse)) {
            return;
        }
        String color = HorseColorUtil.getColor(horse);
        if (color == null) {
            return;
        }
        if (!self.isSneaking() || !horse.isAlive() || !self.isAlive()) {
            return;
        }
        self.sendMessage(Text.literal(
                HorseColorUtil.RED.equals(color) ? "红马拒绝让你下马" : "绿马拒绝让你下马"), true);
        self.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(horse));
        ci.cancel();
    }
}
