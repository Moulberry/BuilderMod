package codes.moulberry.buildermod.commands;

import codes.moulberry.buildermod.Identifiers;
import codes.moulberry.buildermod.gui.ToolCreateMenu;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;

public class CreateToolCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("createtool")
                .executes(CreateToolCommand::createtool);
        dispatcher.register(node);
    }

    private static int createtool(CommandContext<FabricClientCommandSource> context) {
        // Run on next tick to avoid chat closing the screen
        MinecraftClient.getInstance().send(ToolCreateMenu::open);
        return 0;
    }

}
