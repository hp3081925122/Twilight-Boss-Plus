package com.twilightbossplus.mixin;

import com.twilightbossplus.knight.KnightGroup;
import com.twilightbossplus.knight.KnightGroups;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.ai.goal.PhantomUpdateFormationAndMoveGoal;
import twilightforest.entity.boss.KnightPhantom;

@Mixin(value = PhantomUpdateFormationAndMoveGoal.class, remap = false)
public abstract class PhantomUpdateFormationAndMoveGoalMixin {
    @Shadow
    @Final
    private KnightPhantom boss;

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/control/MoveControl;setWantedPosition(DDDD)V", remap = true), index = 3, remap = true)
    private double twilightbossplus$speed(double speed) {
        return speed * KnightGroups.speedFactor(this.boss);
    }

    @Inject(method = "pickRandomFormation", at = @At("TAIL"))
    private void twilightbossplus$formationPicked(CallbackInfo ci) {
        KnightGroup group = KnightGroups.of(this.boss);
        if (group != null) {
            group.onFormationPicked(this.boss);
        }
    }

    @Inject(method = "makeARandomKnightCharge", at = @At("HEAD"), cancellable = true)
    private void twilightbossplus$blockCharge(List<KnightPhantom> knights, CallbackInfo ci) {
        KnightGroup group = KnightGroups.of(this.boss);
        if (group != null && group.blocksAttackAssignment()) {
            ci.cancel();
        }
    }

    @Inject(method = "makeARandomKnightCharge", at = @At("TAIL"))
    private void twilightbossplus$chargeAssigned(List<KnightPhantom> knights, CallbackInfo ci) {
        KnightGroup group = KnightGroups.of(this.boss);
        if (group != null) {
            group.onChargeAssigned(knights);
        }
    }

    @Redirect(method = "switchToNextFormation", at = @At(value = "INVOKE", target = "Ltwilightforest/entity/boss/KnightPhantom;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V", remap = true))
    private void twilightbossplus$lastKnightWeapon(KnightPhantom knight, EquipmentSlot slot, ItemStack stack) {
        KnightGroup group = KnightGroups.of(knight);
        if (group != null && group.onLastKnightSwitch(knight)) {
            return;
        }
        knight.setItemSlot(slot, stack);
    }
}
