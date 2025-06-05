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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import static net.minecraft.server.command.CommandManager.*;

public class ModCommands {
    private static final Map<ServerPlayerEntity, BlockScanTask> activeScans = new ConcurrentHashMap<>();
    // 解析方块ID集合
    private static Set<Identifier> parseBlockIds(String input) {
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .map(Identifier::tryParse)
                .filter(id -> id != null && Registries.BLOCK.containsId(id))
                .collect(Collectors.toSet());
    }

    // Tick事件处理
    public static void tickScans() {
        activeScans.entrySet().removeIf(entry -> {
            BlockScanTask task = entry.getValue();
            boolean hasMore = task.tick();
            if (!hasMore) {
                ServerPlayerEntity player = entry.getKey();
                player.sendMessage(Text.literal("扫描完成！结果文件: " + task.getFileName()), false);
                return true;
            }
            return false;
        });
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        // 方块扫描命令：/scanblocks <半径> <方块ID列表>
        dispatcher.register(
                literal("scanblocks")
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
                                            Set<Identifier> targetBlocks = parseBlockIds(input);

                                            if (targetBlocks.isEmpty()) {
                                                source.sendError(Text.literal("未提供有效的方块ID！"));
                                                return 0;
                                            }

                                            // 覆盖旧任务
                                            if (activeScans.containsKey(player)) {
                                                source.sendFeedback(() -> Text.literal("已覆盖之前的扫描任务"), false);
                                            }
                                            FileWriter.scanCount = 0;
                                            // 创建新任务
                                            BlockScanTask task = new BlockScanTask(player, radius, targetBlocks);
                                            activeScans.put(player, task);
                                            source.sendFeedback(() -> Text.literal("开始分帧扫描，结果将逐步写入文件"), false);
                                            return 1;
                                        })
                                )
                        )
        );

        // 实体扫描命令：/scanentities <半径> <实体ID列表>
        dispatcher.register(
                literal("scanentities")
                        .then(argument("radius", IntegerArgumentType.integer(1, 64))
                                .then(argument("entities", StringArgumentType.greedyString())
                                        .suggests(SuggestionProviders.ENTITY_IDS)
                                        .executes(ctx -> {
                                            ServerCommandSource source = ctx.getSource();
                                            ServerPlayerEntity player = source.getPlayer();
                                            if (player == null) {
                                                source.sendError(Text.literal("该命令只能由玩家执行！"));
                                                return 0;
                                            }

                                            int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                            String input = StringArgumentType.getString(ctx, "entities");

                                            // 解析为 Set<Identifier> 并过滤无效ID
                                            Set<Identifier> targetEntityIds = Arrays.stream(input.split(","))
                                                    .map(String::trim)
                                                    .map(id -> Identifier.tryParse(id))
                                                    .filter(identifier -> identifier != null && Registries.ENTITY_TYPE.containsId(identifier))
                                                    .collect(Collectors.toSet());

                                            if (targetEntityIds.isEmpty()) {
                                                source.sendError(Text.literal("未提供有效的实体ID！"));
                                                return 0;
                                            }

                                            String fileName = FileWriter.generateFileName(player);
                                            FileWriter.scanCount = 0;
                                            int totalFound = EntityScanner.startScan(player, radius, targetEntityIds, fileName);

                                            if (totalFound == 0) {
                                                source.sendFeedback(() -> Text.literal("未找到任何实体"), false);
                                                FileWriter.cleanEmptyFile(fileName);
                                            } else {
                                                source.sendFeedback(() -> Text.literal("扫描完成！找到 " + totalFound + " 个实体，结果保存在: " + fileName), false);
                                            }
                                            return 1;
                                        })
                                )
                        ));
    }
}