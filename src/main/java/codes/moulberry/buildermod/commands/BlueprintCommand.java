package codes.moulberry.buildermod.commands;

import codes.moulberry.buildermod.WorldEditCUI;
import codes.moulberry.buildermod.blueprint.ProtoBlueprint;
import codes.moulberry.buildermod.gui.blueprints.BlueprintCreateMenu;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class BlueprintCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder =
                literal("blueprint")
                        .then(literal("create").executes(BlueprintCommand::create));

        dispatcher.register(builder);
    }

    private static int create(CommandContext<FabricClientCommandSource> ctx) {
        WorldEditCUI worldEditCUI = WorldEditCUI.getInstance();

        BlockPos pos1 = worldEditCUI.getPos1();
        BlockPos pos2 = worldEditCUI.getPos2();
        if (pos1 != null && pos2 != null) {
            BlockPos min = new BlockPos(
                    Math.min(pos1.getX(), pos2.getX()),
                    Math.min(pos1.getY(), pos2.getY()),
                    Math.min(pos1.getZ(), pos2.getZ())
            );
            BlockPos dim = new BlockPos(
                    Math.abs(pos1.getX() - pos2.getX()) + 1,
                    Math.abs(pos1.getY() - pos2.getY()) + 1,
                    Math.abs(pos1.getZ() - pos2.getZ()) + 1
            );

            ProtoBlueprint proto = ProtoBlueprint.createFromWorld(MinecraftClient.getInstance().world, min, dim);
            MinecraftClient.getInstance().send(() -> {
                MinecraftClient.getInstance().setScreen(BlueprintCreateMenu.createScreen(proto));
            });
            return 0;
        } else {
            ctx.getSource().sendError(Text.of("Select a region with WorldEdit first"));
            return -1;
        }
    }

}
