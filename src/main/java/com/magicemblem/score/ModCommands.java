package com.magicemblem.score;

import com.magicemblem.MagicEmblem;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * 积分指令注册
 * 
 * 指令：
 * - /magice score        查询所有玩家积分
 * - /magice score <name> 查询指定玩家积分
 */
@Mod.EventBusSubscriber(modid = MagicEmblem.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("magice")
                .then(Commands.literal("score")
                        .executes(ModCommands::listAllScores)
                        .then(Commands.argument("player", StringArgumentType.string())
                                .executes(ModCommands::queryPlayerScore))));
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        PlayerScoreManager.loadScores();
        MagicEmblem.LOGGER.info("Score system initialized");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PlayerScoreManager.forceSave();
        MagicEmblem.LOGGER.info("Score system saved");
    }

    /**
     * /magice score  — 列出所有玩家积分
     */
    private static int listAllScores(CommandContext<CommandSourceStack> context) {
        Map<String, Double> scores = PlayerScoreManager.getAllScores();
        CommandSourceStack source = context.getSource();

        if (scores.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7暂无积分记录"), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("§6§l=== 玩家积分列表 ==="), false);
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            source.sendSuccess(() -> Component.literal(
                    String.format("§e%s§r: §b%.2f 分", entry.getKey(), entry.getValue())), false);
        }
        source.sendSuccess(() -> Component.literal(
                String.format("§7共 §f%d§7 名玩家", scores.size())), false);

        return Command.SINGLE_SUCCESS;
    }

    /**
     * /magice score <player>  — 查询指定玩家积分
     */
    private static int queryPlayerScore(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        double score = PlayerScoreManager.getScore(playerName);
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal(
                String.format("§e%s§r 的积分: §b%.2f 分", playerName, score)), false);

        return Command.SINGLE_SUCCESS;
    }
}
