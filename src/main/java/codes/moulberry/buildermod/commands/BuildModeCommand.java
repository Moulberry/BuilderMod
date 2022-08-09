package codes.moulberry.buildermod.commands;

import codes.moulberry.buildermod.BuilderMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class BuildModeCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("buildmode")
                .then(literal("replace").executes(ctx -> toggleReplaceMode()))
                .then(literal("instabreak").executes(ctx -> toggleInstaBreak()))
                .then(literal("flight").executes(ctx -> toggleFlight()));

        dispatcher.register(node);
    }

    private static int toggleReplaceMode() {
        BuilderMod.getInstance().config.replaceMode = !BuilderMod.getInstance().config.replaceMode;
        if (BuilderMod.getInstance().config.replaceMode && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.playSound(SoundEvents.ENTITY_ENDER_EYE_DEATH, 0.7f, 1.5f);
        } else {
            MinecraftClient.getInstance().player.playSound(SoundEvents.ENTITY_ENDER_EYE_DEATH, 0.7f, 0.5f);
        }
        BuilderMod.getInstance().saveConfig();
        return 0;
    }

    private static int toggleInstaBreak() {
        BuilderMod.getInstance().config.instabreak = !BuilderMod.getInstance().config.instabreak;
        BuilderMod.getInstance().saveConfig();
        return 0;
    }

    private static int toggleFlight() {
        BuilderMod.getInstance().config.enhancedFlight = !BuilderMod.getInstance().config.enhancedFlight;
        BuilderMod.getInstance().saveConfig();
        return 0;
    }

}
