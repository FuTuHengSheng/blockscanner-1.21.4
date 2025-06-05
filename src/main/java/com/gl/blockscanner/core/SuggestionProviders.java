package com.gl.blockscanner.core;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import java.util.concurrent.CompletableFuture;

public class SuggestionProviders {
    // 方块ID建议（直接返回 Identifier 字符串）
    public static final SuggestionProvider<ServerCommandSource> BLOCK_IDS = (context, builder) -> {
        Registries.BLOCK.getIds().forEach(id -> builder.suggest(id.toString()));
        return CompletableFuture.completedFuture(builder.build());
    };

    // 实体ID建议（直接返回 Identifier 字符串）
    public static final SuggestionProvider<ServerCommandSource> ENTITY_IDS = (context, builder) -> {
        Registries.ENTITY_TYPE.getIds().forEach(id -> builder.suggest(id.toString()));
        return CompletableFuture.completedFuture(builder.build());
    };
}