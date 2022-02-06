package codes.moulberry.buildermod.commands;

import codes.moulberry.buildermod.gui.GuiToolMenu;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.ServerCommandSource;

import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;

public class ToolMenuCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("toolmenu").executes(ctx -> execute()));
    }

    private static int execute() {
        MinecraftClient.getInstance().send(() -> {
            MinecraftClient.getInstance().setScreen(new GuiToolMenu(MinecraftClient.getInstance().player.getInventory()));
        });
        return 0;
    }

}
