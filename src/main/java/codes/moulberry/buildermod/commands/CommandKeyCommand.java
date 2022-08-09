package codes.moulberry.buildermod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandKeyCommand {

    public static String command = null;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder =
                literal("setcommandkey")
                    .then(argument("command", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            var command = StringArgumentType.getString(ctx, "command");
                            if (command.startsWith("/")) command = command.substring(1);
                            CommandKeyCommand.command = command.isBlank() ? null : command;
                            return Command.SINGLE_SUCCESS;
                        })
                    );

        dispatcher.register(builder);
    }

}
