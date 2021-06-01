package codes.moulberry.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ClientCommandManager {

    public interface IServerCommandSource {
        int getLevel();
    }

    public static class FakeCommandSource extends ServerCommandSource {
        public FakeCommandSource(ClientPlayerEntity player) {
            super(player, player.getPos(), player.getRotationClient(), null, 0, player.getEntityName(), player.getName(), null, player);
        }

        @Override
        public Collection<String> getPlayerNames() {
            return MinecraftClient.getInstance().getNetworkHandler().getPlayerList()
                    .stream().map(e -> e.getProfile().getName()).collect(Collectors.toList());
        }
    }

    private static final Set<String> clientSideCommands = new HashSet<>();

    public static void clearClientSideCommands() {
        clientSideCommands.clear();
    }

    public static void addClientSideCommand(String name) {
        clientSideCommands.add(name);
    }

    public static boolean isClientSideCommand(String name) {
        return clientSideCommands.contains(name);
    }

    public static boolean getFlag(CommandContext<ServerCommandSource> ctx, int flag) {
        return getFlag(ctx.getSource(), flag);
    }

    public static boolean getFlag(ServerCommandSource source, int flag) {
        return (((IServerCommandSource) source).getLevel() & flag) != 0;
    }

    public static ServerCommandSource withFlags(ServerCommandSource source, int flags, boolean value) {
        if (value) {
            return source.withLevel(((IServerCommandSource) source).getLevel() | flags);
        } else {
            return source.withLevel(((IServerCommandSource) source).getLevel() & ~flags);
        }
    }

    public static void sendError(Text error) {
        sendFeedback(new LiteralText("").append(error).formatted(Formatting.RED));
    }

    public static void sendFeedback(String message, Object... args) {
        sendFeedback(new TranslatableText(message, args));
    }

    public static void sendFeedback(Text message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(message);
    }

    /*public static void addOverlayMessage(Text message, int time) {
        InGameHud inGameHud = MinecraftClient.getInstance().inGameHud;
        inGameHud.setOverlayMessage(message, false);
        ((InGameHudAccessor) inGameHud).setOverlayRemaining(time);
    }*/

    public static int executeCommand(StringReader reader, String command) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        try {
            return player.networkHandler.getCommandDispatcher().execute(reader, new FakeCommandSource(player));
        } catch (CommandException e) {
            ClientCommandManager.sendError(e.getTextMessage());
        } catch (CommandSyntaxException e) {
            ClientCommandManager.sendError(Texts.toText(e.getRawMessage()));
            if (e.getInput() != null && e.getCursor() >= 0) {
                int cursor = Math.min(e.getCursor(), e.getInput().length());
                MutableText text = new LiteralText("").formatted(Formatting.GRAY)
                        .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
                if (cursor > 10)
                    text.append("...");

                text.append(e.getInput().substring(Math.max(0, cursor - 10), cursor));
                if (cursor < e.getInput().length()) {
                    text.append(new LiteralText(e.getInput().substring(cursor)).formatted(Formatting.RED, Formatting.UNDERLINE));
                }

                text.append(new TranslatableText("command.context.here").formatted(Formatting.RED, Formatting.ITALIC));
                ClientCommandManager.sendError(text);
            }
        } catch (Exception e) {
            LiteralText error = new LiteralText(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            ClientCommandManager.sendError(new TranslatableText("command.failed")
                    .styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, error))));
            e.printStackTrace();
        }
        return 1;
    }

    public static Text getCommandTextComponent(String translationKey, String command) {
        return getCommandTextComponent(new TranslatableText(translationKey), command);
    }

    public static Text getCommandTextComponent(TranslatableText translatableText, String command) {
        return translatableText.styled(style -> style.withFormatting(Formatting.UNDERLINE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(command))));
    }

}