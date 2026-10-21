package com.twilightbossplus.mixin;

import com.twilightbossplus.client.vfx.YetiPose;
import com.twilightbossplus.yeti.YetiFx;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private static float tbp$ramp(float t, float from, float to) {
        float x = Mth.clamp((t - from) / Math.max(0.001F, to - from), 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    @Inject(method = {"setupAnim(Ltwilightforest/entity/boss/AlphaYeti;FFFFF)V"}, at = @At("TAIL"), remap = false, require = 1)
    private void tbp$pose(AlphaYeti entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        YetiPose.State state = YetiPose.get(entity, ageInTicks);
        if (state == null) {
            return;
        }
        float t = ageInTicks - state.start;
        float w = state.windup;
        float d = state.duration;
        float weight = tbp$ramp(t, 0.0F, 4.0F) * (1.0F - tbp$ramp(t, d, d + 8.0F));
        float armX = -1.57F;
        float spreadY = 0.0F;
        float spreadZ = 0.0F;
        float lean = 0.0F;
        float bend = 0.0F;
        float tremble = 0.0F;
        if (state.kind == YetiFx.POSE_CLAP) {
            float clap = tbp$ramp(t, w - 3.0F, w);
            armX = -1.45F;
            spreadY = Mth.lerp(clap, 0.95F, -0.35F);
            lean = Mth.lerp(clap, -0.1F, 0.22F);
            tremble = 0.05F * (1.0F - clap);
        } else if (state.kind == YetiFx.POSE_CROUCH) {
            float rise = tbp$ramp(t, w - 1.0F, w + 3.0F);
            float land = tbp$ramp(t, d - 3.0F, d);
            armX = Mth.lerp(land, Mth.lerp(rise, 0.5F, -2.9F), -0.6F);
            spreadZ = 0.25F * rise * (1.0F - land);
            bend = 0.9F * tbp$ramp(t, 0.0F, w) * (1.0F - rise) + 0.6F * land * (1.0F - tbp$ramp(t, d + 2.0F, d + 8.0F));
            lean = Mth.lerp(rise, 0.25F, -0.1F) + 0.3F * land;
        } else if (state.kind == YetiFx.POSE_SCOOP) {
            float down = tbp$ramp(t, 0.0F, w - 4.0F);
            float push = tbp$ramp(t, w - 2.0F, w);
            armX = Mth.lerp(push, Mth.lerp(down, -1.57F, -0.35F), -1.75F);
            spreadY = -0.25F;
            lean = Mth.lerp(push, 0.32F * down, -0.05F);
            bend = 0.4F * down * (1.0F - push);
        } else if (state.kind == YetiFx.POSE_SLAM) {
            float raise = tbp$ramp(t, 0.0F, w - 4.0F);
            float slam = tbp$ramp(t, w - 3.0F, w);
            armX = Mth.lerp(slam, Mth.lerp(raise, -1.57F, -3.0F), -0.5F);
            lean = Mth.lerp(slam, -0.15F * raise, 0.32F);
            bend = 0.5F * slam;
            tremble = 0.04F * raise * (1.0F - slam);
        } else if (state.kind == YetiFx.POSE_ROAR) {
            float open = tbp$ramp(t, 0.0F, 6.0F);
            armX = Mth.lerp(open, -1.57F, -0.45F);
            spreadZ = 1.3F * open;
            lean = -0.25F * open;
            tremble = 0.06F * open;
        }
        float shake = Mth.sin(ageInTicks * 2.3F) * tremble;
        this.rightArm.xRot = Mth.lerp(weight, this.rightArm.xRot, armX + shake);
        this.leftArm.xRot = Mth.lerp(weight, this.leftArm.xRot, armX - shake);
        this.rightArm.yRot = Mth.lerp(weight, this.rightArm.yRot, spreadY);
        this.leftArm.yRot = Mth.lerp(weight, this.leftArm.yRot, -spreadY);
        this.rightArm.zRot = Mth.lerp(weight, this.rightArm.zRot, spreadZ);
        this.leftArm.zRot = Mth.lerp(weight, this.leftArm.zRot, -spreadZ);
        float k = lean * weight;
        float b = bend * weight;
        float drop = 20.0F * (1.0F - Mth.cos(b));
        this.body.xRot = this.body.xRot + k;
        this.body.y = this.body.y + drop;
        float armY = this.body.y - 20.0F * Mth.cos(k);
        float armZ = -20.0F * Mth.sin(k);
        this.rightArm.y = armY;
        this.leftArm.y = armY;
        this.rightArm.z = armZ;
        this.leftArm.z = armZ;
        this.rightArm.xRot = this.rightArm.xRot + k;
        this.leftArm.xRot = this.leftArm.xRot + k;
        this.rightLeg.y = this.rightLeg.y + drop;
        this.leftLeg.y = this.leftLeg.y + drop;
        this.rightLeg.xRot = Mth.lerp(weight, this.rightLeg.xRot, -b);
        this.leftLeg.xRot = Mth.lerp(weight, this.leftLeg.xRot, -b);
    }
}
