package com.twilightbossplus.mixin;

import com.twilightbossplus.client.vfx.KnightPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.client.model.entity.KnightPhantomModel;
import twilightforest.entity.boss.KnightPhantom;

@Mixin(KnightPhantomModel.class)
public abstract class KnightPhantomModelMixin extends HumanoidModel<KnightPhantom> {
    protected KnightPhantomModelMixin(ModelPart root) {
        super(root);
    }

    @Unique
    private static float tbp$ramp(float t, float from, float to) {
        float x = Mth.clamp((t - from) / Math.max(0.001F, to - from), 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    @Inject(method = {"setupAnim(Ltwilightforest/entity/boss/KnightPhantom;FFFFF)V"}, at = @At("TAIL"), remap = false, require = 1)
    private void tbp$pose(KnightPhantom entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        KnightPose.State state = KnightPose.get(entity, ageInTicks);
        if (state == null) {
            return;
        }
        float t = ageInTicks - state.start;
        float w = state.windup;
        float d = state.duration;
        float weight = tbp$ramp(t, 0.0F, 4.0F) * (1.0F - tbp$ramp(t, d, d + 10.0F));
        float rightX = this.rightArm.xRot;
        float rightY = this.rightArm.yRot;
        float rightZ = this.rightArm.zRot;
        float leftX = this.leftArm.xRot;
        float leftY = this.leftArm.yRot;
        float leftZ = this.leftArm.zRot;
        float bodyY = this.body.yRot;
        if (state.kind == KnightPose.SLASH) {
            float strike = tbp$ramp(t, w, w + 3.0F);
            float tremble = Mth.sin(ageInTicks * 2.2F) * 0.05F * (1.0F - strike);
            rightX = Mth.lerp(strike, -2.7F, -0.7F) + tremble;
            rightY = Mth.lerp(strike, 0.5F, -0.9F) * state.dir;
            rightZ = Mth.lerp(strike, 0.45F, -0.2F);
            leftX = Mth.lerp(strike, -0.4F, -0.9F);
            leftZ = -0.35F;
            bodyY = Mth.lerp(strike, 0.45F, -0.55F) * state.dir;
        } else if (state.kind == KnightPose.RAISE) {
            float lift = tbp$ramp(t, 0.0F, Math.max(2.0F, w * 0.6F));
            rightX = Mth.lerp(lift, -0.6F, -2.9F) + Mth.sin(ageInTicks * 1.8F) * 0.05F * lift;
            rightZ = -0.1F;
            leftX = -0.5F;
            leftZ = -0.45F * lift;
        } else if (state.kind == KnightPose.THROW) {
            float lift = tbp$ramp(t, 0.0F, Math.max(1.0F, w - 2.0F));
            float release = tbp$ramp(t, w, w + 3.0F);
            rightX = Mth.lerp(release, Mth.lerp(lift, -0.8F, -3.1F), -1.5F);
            rightZ = Mth.lerp(release, 0.35F, 0.05F);
            leftX = Mth.lerp(release, -0.9F, -0.3F);
            leftZ = -0.3F;
            bodyY = Mth.lerp(release, 0.35F * lift, -0.25F);
        } else if (state.kind == KnightPose.LANCE) {
            float level = tbp$ramp(t, Math.max(0.0F, w - 8.0F), w);
            rightX = Mth.lerp(level, -0.9F, -1.5F);
            rightY = 0.12F;
            rightZ = 0.05F;
            leftX = -0.7F;
            leftY = 0.45F;
            leftZ = -0.2F;
            bodyY = -0.15F * level;
        } else if (state.kind == KnightPose.OATH) {
            float lift = tbp$ramp(t, 0.0F, Math.max(2.0F, w));
            float pulse = Mth.sin(ageInTicks * 0.6F) * 0.04F;
            rightX = Mth.lerp(lift, -0.6F, -2.95F) + pulse;
            leftX = Mth.lerp(lift, -0.6F, -2.95F) - pulse;
            rightZ = 0.25F * lift;
            leftZ = -0.25F * lift;
        } else if (state.kind == KnightPose.CHAINS) {
            float open = tbp$ramp(t, 0.0F, Math.max(2.0F, w));
            float strain = Mth.sin(ageInTicks * 3.0F) * 0.06F * open;
            rightX = -0.35F;
            leftX = -0.35F;
            rightZ = 1.15F * open + strain;
            leftZ = -1.15F * open - strain;
        }
        this.rightArm.xRot = Mth.lerp(weight, this.rightArm.xRot, rightX);
        this.rightArm.yRot = Mth.lerp(weight, this.rightArm.yRot, rightY);
        this.rightArm.zRot = Mth.lerp(weight, this.rightArm.zRot, rightZ);
        this.leftArm.xRot = Mth.lerp(weight, this.leftArm.xRot, leftX);
        this.leftArm.yRot = Mth.lerp(weight, this.leftArm.yRot, leftY);
        this.leftArm.zRot = Mth.lerp(weight, this.leftArm.zRot, leftZ);
        this.body.yRot = Mth.lerp(weight, this.body.yRot, bodyY);
        this.rightArm.x = -5.0F * Mth.cos(this.body.yRot);
        this.rightArm.z = 5.0F * Mth.sin(this.body.yRot);
        this.leftArm.x = 5.0F * Mth.cos(this.body.yRot);
        this.leftArm.z = -5.0F * Mth.sin(this.body.yRot);
    }
}
