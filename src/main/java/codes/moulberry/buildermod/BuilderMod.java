package codes.moulberry.buildermod;

import codes.moulberry.buildermod.blueprint.BlueprintLibrary;
import codes.moulberry.buildermod.commands.*;
import codes.moulberry.buildermod.config.BMConfig;
import codes.moulberry.buildermod.config.serialize.GSONHolder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.impl.item.group.ItemGroupExtensions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BuilderMod implements ModInitializer {

	private static BuilderMod INSTANCE;

	public static BuilderMod getInstance() {
		return INSTANCE;
	}

	public BMConfig config = null;
	private File configFile;
	private File blueprintsFolder;

	public KeyBinding clearLaserKeybind;
	public KeyBinding replaceModeKeyBind;
	public KeyBinding wheelKeyBind;
	public KeyBinding commandKeyBind;

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
		createServerGroup();
		createServerColorGroup();

		File configDir = FabricLoader.getInstance().getConfigDir().toFile();

		blueprintsFolder = new File(configDir, "buildermod/blueprints");
		blueprintsFolder.mkdirs();

		BlueprintLibrary.loadBlueprints(blueprintsFolder);

		configFile = new File(configDir, "buildermod/config.json");
		loadConfig();

		clearLaserKeybind = new KeyBinding("buildermod.clearlaser", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_DELETE, "BuilderMod");
		replaceModeKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding("buildermod.replace", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "BuilderMod"));
		wheelKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding("buildermod.wheel",
				InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "BuilderMod"));
		commandKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding("buildermod.commandkey", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "BuilderMod"));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			CreateToolCommand.register(dispatcher);
			ToolMenuCommand.register(dispatcher);
			BuildModeCommand.register(dispatcher);
			BlueprintCommand.register(dispatcher);
			CommandKeyCommand.register(dispatcher);
		});
	}

	public File getBlueprintsFolder() {
		return blueprintsFolder;
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

	private static void createServerGroup() {
		((ItemGroupExtensions) ItemGroup.BUILDING_BLOCKS).fabric_expandArray();
		new ItemGroup(ItemGroup.GROUPS.length - 1, "buildermod.custom_groups.server") {

			@Override
			public ItemStack createIcon() {
				return new ItemStack(Items.PAPER);
			}

			@Override
			public void appendStacks(DefaultedList<ItemStack> stacks) {
				IntStream.range(1, 100).mapToObj(BuilderMod::createPaperWithId).forEach(stacks::add);
			}
		};
	}

	private static ItemStack createPaperWithId(int id) {
		ItemStack stack = new ItemStack(Items.PAPER);
		NbtCompound tag = new NbtCompound();
		tag.putInt("CustomModelData", id);
		stack.setNbt(tag);
		return stack;
	}

	private static void createServerColorGroup() {
		((ItemGroupExtensions) ItemGroup.BUILDING_BLOCKS).fabric_expandArray();
		new ItemGroup(ItemGroup.GROUPS.length - 1, "buildermod.custom_groups.server_colored") {

			@Override
			public ItemStack createIcon() {
				return new ItemStack(Items.PAPER);
			}

			@Override
			public void appendStacks(DefaultedList<ItemStack> stacks) {
				IntStream.range(1, 10).mapToObj(BuilderMod::createHorseArmorWithId).flatMap(BuilderMod::createBaseColor).forEach(stacks::add);
			}
		};
	}

	private static Stream<ItemStack> createBaseColor(ItemStack stack) {
		List<ItemStack> stacks = new ArrayList<>();

		for (DyeColor value : DyeColor.values()) {
			ItemStack output = stack.copy();
			output.getOrCreateSubNbt("display").putInt("color", getIntFromColor(value.getColorComponents()));
			stacks.add(output);
		}
		return stacks.stream();
	}

	public static int getIntFromColor(float[] colors){
		int R = Math.round(255 * colors[0]);
		int G = Math.round(255 * colors[1]);
		int B = Math.round(255 * colors[2]);

		R = (R << 16) & 0x00FF0000;
		G = (G << 8) & 0x0000FF00;
		B = B & 0x000000FF;

		return R | G | B;
	}

	private static ItemStack createHorseArmorWithId(int id) {
		ItemStack stack = new ItemStack(Items.LEATHER_HORSE_ARMOR);
		NbtCompound tag = new NbtCompound();
		tag.putInt("CustomModelData", id);
		stack.setNbt(tag);
		return stack;
	}
}
