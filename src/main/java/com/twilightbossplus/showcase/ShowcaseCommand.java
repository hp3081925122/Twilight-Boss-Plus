package com.twilightbossplus.showcase;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.twilightbossplus.TwilightBossPlus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShowcaseCommand {
    private static final SuggestionProvider<CommandSourceStack> BOSS_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest("all");
        for (BossShowcaseManager.BossKind kind : BossShowcaseManager.BossKind.values()) {
            builder.suggest(kind.id());
        }
        return SharedSuggestionProvider.suggest(new String[0], builder);
    };

    private ShowcaseCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tbpshowcase")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(ctx -> start(ctx.getSource(), "all", true))
                        .then(Commands.argument("boss", StringArgumentType.word())
                                .suggests(BOSS_SUGGESTIONS)
                                .executes(ctx -> start(ctx.getSource(), StringArgumentType.getString(ctx, "boss"), true))
                                .then(Commands.argument("hideGui", BoolArgumentType.bool())
                                        .executes(ctx -> start(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "boss"),
                                                BoolArgumentType.getBool(ctx, "hideGui"))))))
                .then(Commands.literal("stop")
                        .executes(ctx -> {
                            BossShowcaseManager.stop(true);
                            ctx.getSource().sendSuccess(() -> Component.literal("Showcase stopped."), false);
                            return 1;
                        }))
                .then(Commands.literal("hud")
                        .then(Commands.literal("hide")
                                .executes(ctx -> setHud(ctx.getSource(), true)))
                        .then(Commands.literal("show")
                                .executes(ctx -> setHud(ctx.getSource(), false)))));
    }

    private static int start(CommandSourceStack source, String bossArg, boolean hideGui) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Player required."));
            return 0;
        }
        BossShowcaseManager.BossKind single = null;
        if (!"all".equalsIgnoreCase(bossArg)) {
            single = BossShowcaseManager.BossKind.byId(bossArg);
            if (single == null) {
                source.sendFailure(Component.literal("Unknown boss: " + bossArg));
                return 0;
            }
        }
        BossShowcaseManager.start(source.getLevel(), player, single, hideGui);
        return 1;
    }

    private static int setHud(CommandSourceStack source, boolean hide) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Player required."));
            return 0;
        }
        BossShowcaseManager.setPlayerHudHidden(player, hide);
        return 1;
    }
}
