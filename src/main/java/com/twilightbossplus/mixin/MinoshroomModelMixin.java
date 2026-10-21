package com.twilightbossplus.mixin;

import com.twilightbossplus.client.vfx.MinoPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.client.model.entity.MinoshroomModel;
import twilightforest.entity.boss.Minoshroom;

@Mixin(MinoshroomModel.class)
public abstract class MinoshroomModelMixin extends HumanoidModel<Minoshroom> {
    @Shadow(remap = false)
    @Final
    ModelPart rightFrontLeg;
    @Shadow(remap = false)
    @Final
    ModelPart leftFrontLeg;

    protected MinoshroomModelMixin(ModelPart root) {
        super(root);
    }

    @Unique
    private static float tbp$ramp(float t, float from, float to) {
        float x = Mth.clamp((t - from) / Math.max(0.001F, to - from), 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    @Inject(method = {"setupAnim(Ltwilightforest/entity/boss/Minoshroom;FFFFF)V"}, at = @At("TAIL"), remap = false, require = 1)
    private void tbp$pose(Minoshroom entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        float lean = 0.0F;
        float twist = 0.0F;
        float headDown = 0.0F;
        float weight = 0.0F;
        float rightX = this.rightArm.xRot;
        float rightY = 0.0F;
        float rightZ = this.rightArm.zRot;
        float leftX = this.leftArm.xRot;
        float leftY = 0.0F;
        float leftZ = this.leftArm.zRot;
        MinoPose.State state = MinoPose.get(entity, ageInTicks);
        if (state != null) {
            float t = ageInTicks - state.start;
            float w = state.windup;
            if (state.kind == MinoPose.CHOP) {
                float raise = tbp$ramp(t, 0.0F, Math.max(1.0F, w - 3.0F));
                float slam = tbp$ramp(t, w - 3.0F, w);
                weight = raise * (1.0F - tbp$ramp(t, w + 6.0F, w + 16.0F));
                float tremble = Mth.sin(ageInTicks * 2.5F) * 0.04F * raise * (1.0F - slam);
                rightX = Mth.lerp(slam, -2.8F, -0.4F) + tremble;
                leftX = Mth.lerp(slam, -2.6F, -0.3F) + tremble;
                rightZ = Mth.lerp(slam, 0.35F, -0.15F);
                leftZ = Mth.lerp(slam, -0.35F, 0.15F);
                lean = Mth.lerp(slam, -0.22F, 0.45F) * weight;
                headDown = 0.3F * slam * weight;
            } else if (state.kind == MinoPose.SWEEP) {
                float swing = tbp$ramp(t, w - 3.0F, w);
                weight = tbp$ramp(t, 0.0F, Math.max(1.0F, w - 3.0F)) * (1.0F - tbp$ramp(t, w + 4.0F, w + 12.0F));
                rightX = Mth.lerp(swing, -1.5F, -1.2F);
                rightY = Mth.lerp(swing, 0.7F, -0.9F) * weight;
                rightZ = 0.0F;
                leftX = Mth.lerp(swing, -1.0F, -0.9F);
                leftY = Mth.lerp(swing, 0.5F, -0.6F) * weight;
                leftZ = 0.0F;
                twist = Mth.lerp(swing, 0.7F, -0.6F) * weight;
                lean = 0.15F * swing * weight;
            } else if (state.kind == MinoPose.WHIRL) {
                float coil = tbp$ramp(t, 0.0F, w);
                float spin = tbp$ramp(t, w, w + 2.0F);
                weight = coil * (1.0F - tbp$ramp(t, state.duration - 6.0F, state.duration));
                rightX = Mth.lerp(spin, -1.0F, -0.2F);
                rightZ = Mth.lerp(spin, 0.2F, 1.4F);
                leftX = Mth.lerp(spin, -0.6F, -0.2F);
                leftZ = Mth.lerp(spin, -0.2F, -1.4F);
                twist = (0.9F * coil * (1.0F - spin) + spin * Mth.sin(ageInTicks * 0.8F) * 0.15F) * weight;
                lean = 0.2F * weight;
            } else if (state.kind == MinoPose.PAW) {
                weight = tbp$ramp(t, 0.0F, 4.0F) * (1.0F - tbp$ramp(t, w, w + 4.0F));
                rightX = 0.5F;
                leftX = 0.4F;
                headDown = 0.7F * weight;
                lean = 0.35F * weight;
                this.rightFrontLeg.xRot = Mth.lerp(weight, this.rightFrontLeg.xRot, Mth.sin(t * 0.9F) * 0.7F - 0.3F);
                this.leftFrontLeg.xRot = Mth.lerp(weight, this.leftFrontLeg.xRot, 0.15F);
            }
        }
        if (entity.isCharging() && (state == null || state.kind == MinoPose.PAW)) {
            weight = 1.0F;
            rightX = 0.9F;
            leftX = 0.6F;
            rightZ = this.rightArm.zRot;
            leftZ = this.leftArm.zRot;
            headDown = 0.8F;
            lean = 0.45F;
        }
        if (state == null && !entity.isCharging() && this.attackTime > 0.0F) {
            float swing = Mth.sin(this.attackTime * Mth.PI);
            weight = 1.0F;
            rightX = this.rightArm.xRot - 2.2F * swing;
            lean = 0.2F * swing;
            headDown = 0.15F * swing;
        }
        this.rightArm.xRot = Mth.lerp(weight, this.rightArm.xRot, rightX);
        this.rightArm.zRot = Mth.lerp(weight, this.rightArm.zRot, rightZ);
        this.leftArm.xRot = Mth.lerp(weight, this.leftArm.xRot, leftX);
        this.leftArm.zRot = Mth.lerp(weight, this.leftArm.zRot, leftZ);
        float cosLean = Mth.cos(lean);
        float sinLean = Mth.sin(lean);
        float cosTwist = Mth.cos(twist);
        float sinTwist = Mth.sin(twist);
        this.body.xRot = lean;
        this.body.yRot = twist;
        this.body.y = this.body.y + 12.0F - 12.0F * cosLean;
        this.body.z = -9.0F - 12.0F * sinLean;
        this.head.y = this.body.y;
        this.head.z = this.body.z;
        this.head.xRot = this.head.xRot + 0.5F * lean + headDown;
        this.head.yRot = this.head.yRot + 0.6F * twist;
        this.hat.copyFrom(this.head);
        float shoulderY = 2.0F * cosLean;
        float shoulderZ = 2.0F * sinLean;
        this.rightArm.x = -5.0F * cosTwist + shoulderZ * sinTwist;
        this.rightArm.y = this.body.y + shoulderY;
        this.rightArm.z = this.body.z + 5.0F * sinTwist + shoulderZ * cosTwist;
        this.leftArm.x = 5.0F * cosTwist + shoulderZ * sinTwist;
        this.leftArm.y = this.body.y + shoulderY;
        this.leftArm.z = this.body.z - 5.0F * sinTwist + shoulderZ * cosTwist;
        this.rightArm.xRot = this.rightArm.xRot + lean;
        this.leftArm.xRot = this.leftArm.xRot + lean;
        this.rightArm.yRot = rightY + twist;
        this.leftArm.yRot = leftY + twist;
    }
}
