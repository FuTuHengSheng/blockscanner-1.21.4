package com.gl.blockscanner;

import com.gl.blockscanner.core.ContinuousScanCommands;
import com.gl.blockscanner.core.ContinuousScanner;
import com.gl.blockscanner.core.ModCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class BlockScannerMod implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
			ModCommands.register(dispatcher, registry);
			ContinuousScanCommands.register(dispatcher); // 新增命令注册
		});

		// 每服务器Tick执行扫描检查
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			ModCommands.tickScans();
			ContinuousScanner.tickAll();
		});

		System.out.println("BlockScanner模组已加载！");
	}
}