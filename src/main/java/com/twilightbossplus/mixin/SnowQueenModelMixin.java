package com.twilightbossplus.mixin;

import com.twilightbossplus.client.fx.QueenFxManager;
import com.twilightbossplus.config.SnowQueenConfig;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.client.model.entity.SnowQueenModel;
import twilightforest.entity.boss.SnowQueen;

@Mixin(SnowQueenModel.class)
public abstract class SnowQueenModelMixin extends HumanoidModel<SnowQueen> {
    protected SnowQueenModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Ltwilightforest/entity/boss/SnowQueen;FFFFF)V", at = @At("TAIL"), remap = false)
    private void twilightbossplus$castPose(SnowQueen entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (SnowQueenConfig.ENABLED.get()) {
            QueenFxManager.applyPose(entity, ageInTicks, this.head, this.body, this.rightArm, this.leftArm);
        }
    }
}
