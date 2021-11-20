package codes.moulberry;

import codes.moulberry.commands.ClientCommandManager;
import codes.moulberry.commands.ToolMenuCommand;
import codes.moulberry.config.BMConfig;
import codes.moulberry.config.serialize.GSONHolder;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.command.ServerCommandSource;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.file.Files;

public class BuilderMod implements ModInitializer {

	private static BuilderMod INSTANCE;

	public static BuilderMod getInstance() {
		return INSTANCE;
	}

	public BMConfig config = null;
	private File configFile;

	public KeyBinding clearLaserKeybind = new KeyBinding("buildermod.clearlaser", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_DELETE, "BuilderMod");

	@Override
	public void onInitialize() {
		INSTANCE = this;

		ClientPlayConnectionEvents.JOIN.register(WorldEditCUI.getInstance()::onPlayReady);

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
