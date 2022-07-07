package codes.moulberry.buildermod;

import codes.moulberry.buildermod.commands.BuildModeCommand;
import codes.moulberry.buildermod.commands.CreateToolCommand;
import codes.moulberry.buildermod.commands.ToolMenuCommand;
import codes.moulberry.buildermod.config.BMConfig;
import codes.moulberry.buildermod.config.serialize.GSONHolder;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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

	public KeyBinding clearLaserKeybind;
	public KeyBinding replaceModeKeyBind;
	public KeyBinding wheelKeyBind;

	@Override
	public void onInitialize() {
		INSTANCE = this;

		ClientPlayConnectionEvents.JOIN.register(WorldEditCUI.getInstance()::sendEnablePacket);
		ClientPlayConnectionEvents.JOIN.register(CustomBlocks::sendEnablePacket);
		ClientPlayConnectionEvents.JOIN.register(WheelGUI::sendEnablePacket);
		ClientPlayConnectionEvents.JOIN.register(Capabilities::requestCapabilities);
		Capabilities.setupPackets();
		WheelGUI.setupPackets();
		WorldEditCUI.getInstance().setupPackets();
		CustomBlocks.setupPackets();

		configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "buildermod/config.json");
		configFile.getParentFile().mkdirs();
		loadConfig();

		clearLaserKeybind = new KeyBinding("buildermod.clearlaser", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_DELETE, "BuilderMod");
		replaceModeKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding("buildermod.replace", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "BuilderMod"));
		wheelKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding("buildermod.wheel",
				InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "BuilderMod"));

		CreateToolCommand.register(ClientCommandManager.DISPATCHER);
		ToolMenuCommand.register(ClientCommandManager.DISPATCHER);
		BuildModeCommand.register(ClientCommandManager.DISPATCHER);
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
}
