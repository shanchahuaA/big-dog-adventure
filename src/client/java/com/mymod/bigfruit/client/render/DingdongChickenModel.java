package com.mymod.bigfruit.client.render;

import com.mymod.bigfruit.BigFruitMod;
import com.mymod.bigfruit.entity.DingdongChicken;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * 资产路径由 {@link DefaultedEntityGeoModel} 的 "entity" 子类型推出：
 *   geo/entity/dingdong_chicken.geo.json
 *   animations/entity/dingdong_chicken.animation.json
 *   textures/entity/dingdong_chicken.png
 * 第二个参数把 "head" 骨骼交给 GeckoLib 自动转头（netHeadYaw/headPitch），
 * 我们的 head 骨 pivot 就在**颈根**，所以整条脖子跟着转，正好是鹅颈感。
 */
public class DingdongChickenModel extends DefaultedEntityGeoModel<DingdongChicken> {
    public DingdongChickenModel() {
        super(BigFruitMod.id("dingdong_chicken"), "head");
    }
}
