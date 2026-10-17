package com.twilightbossplus.mixin;

import com.twilightbossplus.client.fx.YetiFxManager;
import com.twilightbossplus.config.AlphaYetiConfig;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.client.model.entity.AlphaYetiModel;
import twilightforest.entity.boss.AlphaYeti;

@Mixin(AlphaYetiModel.class)
public abstract class AlphaYetiModelMixin extends HumanoidModel<AlphaYeti> {
    protected AlphaYetiModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Ltwilightforest/entity/boss/AlphaYeti;FFFFF)V", at = @At("TAIL"), remap = false)
    private void twilightbossplus$castPose(AlphaYeti entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (AlphaYetiConfig.ENABLED.get()) {
            YetiFxManager.applyPose(entity, ageInTicks, this.head, this.body, this.rightArm, this.leftArm);
        }
    }
}
