package codes.moulberry;

import codes.moulberry.commands.ClientCommandManager;
import codes.moulberry.commands.ToolMenuCommand;
import codes.moulberry.config.BMConfig;
import codes.moulberry.config.serialize.GSONHolder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.server.command.ServerCommandSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class BuilderMod implements ModInitializer {

	private static BuilderMod INSTANCE;

	public static BuilderMod getInstance() {
		return INSTANCE;
	}

	public BMConfig config = null;
	private File configFile;

	@Override
	public void onInitialize() {
		INSTANCE = this;

		configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "buildermod/config.json");
		configFile.getParentFile().mkdirs();
		loadConfig();
	}

	public void loadConfig() {
		try {
			config = GSONHolder.GSON.fromJson(Files.newBufferedReader(configFile.toPath()), BMConfig.class);
		} catch(Exception ignored) {}

		if(config == null) {
			config = new BMConfig();
		}
	}

	public void saveConfig() {
		try {
			configFile.createNewFile();
			Files.write(configFile.toPath(), GSONHolder.GSON.toJson(config).getBytes());

			//configFile.createNewFile();
			//try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
			//	writer.write(GSON.toJson(config));
			//}
		} catch(Exception ignored) {}
	}

	public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		ClientCommandManager.clearClientSideCommands();
		ToolMenuCommand.register(dispatcher);
	}

}
