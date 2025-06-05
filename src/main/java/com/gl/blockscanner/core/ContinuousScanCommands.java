package com.gl.blockscanner.core;

import com.gl.blockscanner.util.FileWriter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.*;

public class ContinuousScanCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // 启动扫描命令：/scan start <radius> <block>
        dispatcher.register(
                literal("scan")
                        .then(literal("start")
                                .then(argument("radius", IntegerArgumentType.integer(1, 16))
                                        .then(argument("blocks", StringArgumentType.greedyString())
                                                .suggests(SuggestionProviders.BLOCK_IDS)
                                                .executes(ctx -> {
                                                    ServerCommandSource source = ctx.getSource();
                                                    ServerPlayerEntity player = source.getPlayer();
                                                    if (player == null) {
                                                        source.sendError(Text.literal("该命令只能由玩家执行！"));
                                                        return 0;
                                                    }

                                                    int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                                    String input = StringArgumentType.getString(ctx, "blocks");
                                                    Set<Identifier> targetBlocks = Arrays.stream(input.split(","))
                                                            .map(String::trim)
                                                            .map(Identifier::tryParse)
                                                            .filter(id -> id != null && Registries.BLOCK.containsId(id))
                                                            .collect(Collectors.toSet());

                                                    if (targetBlocks.isEmpty()) {
                                                        source.sendError(Text.literal("未提供有效的方块ID！"));
                                                        return 0;
                                                    }
                                                    FileWriter.scanCount = 0;
                                                    ContinuousScanner.startScan(player, radius, targetBlocks);
                                                    source.sendFeedback(() -> Text.literal("持续扫描已启动，半径: " + radius + " 目标方块: " + targetBlocks), false);
                                                    return 1;
                                                })
                                        )
                                )
                        ));

        // 停止扫描命令：/scan stop
        dispatcher.register(
                literal("scan")
                        .then(literal("stop")
                                .executes(ctx -> {
                                    ServerCommandSource source = ctx.getSource();
                                    ServerPlayerEntity player = source.getPlayer();
                                    if (player == null) {
                                        source.sendError(Text.literal("该命令只能由玩家执行！"));
                                        return 0;
                                    }

                                    if (ContinuousScanner.stopScan(player)) {
                                        source.sendFeedback(() -> Text.literal("已停止持续扫描"), false);
                                    } else {
                                        source.sendError(Text.literal("没有正在进行的扫描任务"));
                                    }
                                    return 1;
                                })
                        )
        );
    }
}