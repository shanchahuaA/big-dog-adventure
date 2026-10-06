package com.mymod.bigdog.client.render;

import com.mymod.bigdog.entity.WizardChicken;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WizardChickenRenderer extends GeoEntityRenderer<WizardChicken> {
    public WizardChickenRenderer(EntityRendererFactory.Context context) {
        super(context, new WizardChickenModel());
        this.shadowRadius = 0.4f;
    }
}
