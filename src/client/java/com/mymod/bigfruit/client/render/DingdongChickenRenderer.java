package com.mymod.bigfruit.client.render;

import com.mymod.bigfruit.entity.DingdongChicken;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DingdongChickenRenderer extends GeoEntityRenderer<DingdongChicken> {
    public DingdongChickenRenderer(EntityRendererFactory.Context context) {
        super(context, new DingdongChickenModel());
        this.shadowRadius = 0.4f;
    }
}
