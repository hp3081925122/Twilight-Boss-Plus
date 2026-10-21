package com.twilightbossplus.mixin;

import com.twilightbossplus.client.vfx.QueenPose;
import com.twilightbossplus.queen.QueenFx;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private static float tbp$ramp(float t, float from, float to) {
        float x = Mth.clamp((t - from) / Math.max(0.001F, to - from), 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    @Inject(method = {"setupAnim(Ltwilightforest/entity/boss/SnowQueen;FFFFF)V"}, at = @At("TAIL"), remap = false, require = 1)
    private void tbp$pose(SnowQueen entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        QueenPose.State state = QueenPose.get(entity, ageInTicks);
        if (state == null) {
            return;
        }
        float t = ageInTicks - state.start;
        float w = state.windup;
        float d = state.duration;
        float weight = tbp$ramp(t, 0.0F, 4.0F) * (1.0F - tbp$ramp(t, d, d + 8.0F));
        float armX = 0.0F;
        float armY = 0.0F;
        float armZ = 0.0F;
        float lean = 0.0F;
        float sway = 0.0F;
        if (state.kind == QueenFx.POSE_WALTZ) {
            float open = tbp$ramp(t, 0.0F, w);
            armX = Mth.lerp(open, -0.6F, -0.15F);
            armZ = Mth.lerp(open, 0.3F, 1.45F);
            lean = 0.25F * (1.0F - open) - 0.08F * open;
            sway = 0.06F * open;
        } else if (state.kind == QueenFx.POSE_GUARD) {
            float cross = tbp$ramp(t, 0.0F, w);
            armX = Mth.lerp(cross, -0.4F, -1.35F);
            armY = -0.8F * cross;
            armZ = 0.1F;
            lean = 0.12F * cross;
        } else if (state.kind == QueenFx.POSE_BREATH) {
            float reach = tbp$ramp(t, 0.0F, w);
            armX = Mth.lerp(reach, -0.3F, -1.15F);
            armY = -0.15F * reach;
            armZ = 0.05F;
            lean = 0.3F * reach;
            sway = 0.04F * reach;
        } else if (state.kind == QueenFx.POSE_RAISE) {
            float lift = tbp$ramp(t, 0.0F, w);
            armX = Mth.lerp(lift, -0.3F, -2.85F);
            armZ = 0.4F * lift;
            lean = -0.15F * lift;
            sway = 0.05F * lift;
        }
        float shake = Mth.sin(ageInTicks * 0.9F) * sway;
        this.rightArm.xRot = Mth.lerp(weight, this.rightArm.xRot, armX + shake);
        this.leftArm.xRot = Mth.lerp(weight, this.leftArm.xRot, armX - shake);
        this.rightArm.yRot = Mth.lerp(weight, this.rightArm.yRot, armY);
        this.leftArm.yRot = Mth.lerp(weight, this.leftArm.yRot, -armY);
        this.rightArm.zRot = Mth.lerp(weight, this.rightArm.zRot, armZ);
        this.leftArm.zRot = Mth.lerp(weight, this.leftArm.zRot, -armZ);
        this.body.xRot = this.body.xRot + lean * weight;
        this.head.xRot = this.head.xRot + lean * weight * 0.5F;
    }
}
