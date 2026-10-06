package com.mymod.bigdog.mixin;

import com.mymod.bigdog.util.ColoredHorse;
import com.mymod.bigdog.util.HorseColorUtil;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseEntity.class)
public abstract class HorseNbtMixin implements ColoredHorse {
    @Unique
    private String bigdog$color;

    @Override
    public String bigdog_getColor() {
        return this.bigdog$color;
    }

    @Override
    public void bigdog_setColor(String color) {
        this.bigdog$color = color;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void bigdog$writeColor(NbtCompound nbt, CallbackInfo ci) {
        if (this.bigdog$color != null) {
            nbt.putString(HorseColorUtil.NBT_KEY, this.bigdog$color);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void bigdog$readColor(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(HorseColorUtil.NBT_KEY, NbtElement.STRING_TYPE)) {
            this.bigdog$color = nbt.getString(HorseColorUtil.NBT_KEY);
        } else {
            this.bigdog$color = null;
        }
    }
}
