package codes.moulberry.commands;

import codes.moulberry.gui.GuiToolMenu;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.ServerCommandSource;

import static codes.moulberry.commands.ClientCommandManager.*;
import static net.minecraft.server.command.CommandManager.*;

public class ToolMenuCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        addClientSideCommand("toolmenu");

        dispatcher.register(literal("toolmenu").executes(ctx -> execute()));
    }

    private static int execute() {
        MinecraftClient.getInstance().send(() -> {
            MinecraftClient.getInstance().openScreen(new GuiToolMenu(MinecraftClient.getInstance().player.inventory));
        });
        return 0;
    }

}
