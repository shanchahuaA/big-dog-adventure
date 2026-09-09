package com.mymod.bigfruit.mixin;

import com.mymod.bigfruit.util.ColoredHorse;
import com.mymod.bigfruit.util.HorseColorUtil;
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
    private String bigfruit$color;

    @Override
    public String bigfruit_getColor() {
        return this.bigfruit$color;
    }

    @Override
    public void bigfruit_setColor(String color) {
        this.bigfruit$color = color;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void bigfruit$writeColor(NbtCompound nbt, CallbackInfo ci) {
        if (this.bigfruit$color != null) {
            nbt.putString(HorseColorUtil.NBT_KEY, this.bigfruit$color);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void bigfruit$readColor(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(HorseColorUtil.NBT_KEY, NbtElement.STRING_TYPE)) {
            this.bigfruit$color = nbt.getString(HorseColorUtil.NBT_KEY);
        } else {
            this.bigfruit$color = null;
        }
    }
}
