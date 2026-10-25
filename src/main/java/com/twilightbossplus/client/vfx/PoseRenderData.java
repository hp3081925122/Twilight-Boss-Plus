package com.twilightbossplus.client.vfx;

import com.google.common.reflect.TypeToken;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import twilightforest.entity.boss.AlphaYeti;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.entity.boss.Minoshroom;
import twilightforest.entity.boss.SnowQueen;

public final class PoseRenderData {
    public static final ContextKey<KnightPose.State> KNIGHT = new ContextKey<>(Identifier.fromNamespaceAndPath("twilightbossplus", "knight_pose"));
    public static final ContextKey<MinoPose.State> MINO = new ContextKey<>(Identifier.fromNamespaceAndPath("twilightbossplus", "mino_pose"));
    public static final ContextKey<YetiPose.State> YETI = new ContextKey<>(Identifier.fromNamespaceAndPath("twilightbossplus", "yeti_pose"));
    public static final ContextKey<QueenPose.State> QUEEN = new ContextKey<>(Identifier.fromNamespaceAndPath("twilightbossplus", "queen_pose"));
    public static final ContextKey<Boolean> CHARGING = new ContextKey<>(Identifier.fromNamespaceAndPath("twilightbossplus", "charging"));

    private PoseRenderData() {
    }

    public static void register(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<EntityRenderer<Entity, EntityRenderState>>() {}, (entity, state) -> {
            if (entity instanceof KnightPhantom) {
                state.setRenderData(KNIGHT, KnightPose.get(entity, state.ageInTicks));
            } else if (entity instanceof Minoshroom mino) {
                state.setRenderData(MINO, MinoPose.get(entity, state.ageInTicks));
                state.setRenderData(CHARGING, mino.isCharging());
            } else if (entity instanceof AlphaYeti) {
                state.setRenderData(YETI, YetiPose.get(entity, state.ageInTicks));
            } else if (entity instanceof SnowQueen) {
                state.setRenderData(QUEEN, QueenPose.get(entity, state.ageInTicks));
            }
        });
    }
}
