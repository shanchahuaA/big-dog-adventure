package com.mymod.bigdog.client.render;

import com.mymod.bigdog.BigDogMod;
import com.mymod.bigdog.entity.WizardChicken;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * 资产路径由 {@link DefaultedEntityGeoModel} 的 "entity" 子类型推出：
 *   geo/entity/wizard_chicken.geo.json
 *   animations/entity/wizard_chicken.animation.json
 *   textures/entity/wizard_chicken.png
 * 第二个参数把 "head" 骨骼交给 GeckoLib 自动转头（netHeadYaw/headPitch），
 * 我们的 head 骨 pivot 就在**颈根**，所以整条脖子跟着转，正好是鹅颈感。
 */
public class WizardChickenModel extends DefaultedEntityGeoModel<WizardChicken> {
    public WizardChickenModel() {
        super(BigDogMod.id("wizard_chicken"), "head");
    }
}
