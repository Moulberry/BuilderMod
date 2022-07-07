package codes.moulberry.buildermod.macrotool;

import codes.moulberry.buildermod.Identifiers;
import codes.moulberry.buildermod.gui.ToolConfigureMenu;
import codes.moulberry.buildermod.macrotool.script.CompiledScript;
import codes.moulberry.buildermod.macrotool.script.ToolExecutionContext;
import codes.moulberry.buildermod.macrotool.script.parser.ScriptParser;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Matrix4f;

import java.util.*;
import java.util.function.Consumer;

public class ToolHandler {

    private static final Cache<Integer, CompiledScript> scriptCache = Caffeine.newBuilder()
            .maximumSize(100).build();

    public static boolean handleAction(MinecraftClient client, ToolAction action) {
        NbtCompound toolNbt = getToolNbt(client);
        if (toolNbt != null) {
            if (action != ToolAction.LEFT_CLICK_CONTINUOUS) {
                handleActionForTool(client, action, toolNbt);
            }
            return true;
        }
        return false;
    }

    public static boolean handleRender(MinecraftClient client, MatrixStack matrices,
                                       Matrix4f projection, float tickDelta) {
        NbtCompound toolNbt = getToolNbt(client);
        if (toolNbt != null) {
            CompiledScript script = getScriptForTool(toolNbt);
            if (script != null) {
                NbtCompound settings = toolNbt.getCompound(Identifiers.TOOL_SETTINGS_KEY);
                ToolExecutionContext context = new ToolExecutionContext(client, script, settings);
                script.function().render(context, matrices, projection, tickDelta);
                return true;
            }
        }
        return false;
    }

    private static NbtCompound getToolNbt(MinecraftClient client) {
        ClientPlayerEntity player = Objects.requireNonNull(client.player);

        for (Hand hand : Hand.values()) {
            ItemStack itemStack = player.getStackInHand(hand);

            NbtCompound nbt = itemStack.getNbt();
            if (nbt != null) {
                if (nbt.contains(Identifiers.TOOL_NBT_KEY, NbtType.COMPOUND)) {
                    return nbt.getCompound(Identifiers.TOOL_NBT_KEY);
                }
            }
        }

        return null;
    }

    private static CompiledScript getScriptForTool(NbtCompound toolNbt) {
        if (toolNbt.contains(Identifiers.TOOL_SCRIPT_KEY, NbtType.COMPOUND)) {
            NbtCompound scriptTag = toolNbt.getCompound(Identifiers.TOOL_SCRIPT_KEY);
            NbtList source = scriptTag.getList(Identifiers.TOOL_SOURCE_KEY, NbtType.STRING);
            int hash = scriptTag.getInt(Identifiers.TOOL_HASH_KEY);

            if (!source.isEmpty() && hash != 0) {
                return getOrCreateScript(source, hash);
            }
        }
        return null;
    }

    private static void handleActionForTool(MinecraftClient client, ToolAction action,
                                            NbtCompound toolNbt) {
        CompiledScript script = getScriptForTool(toolNbt);
        if (script != null) {
            NbtCompound settings = toolNbt.getCompound(Identifiers.TOOL_SETTINGS_KEY);
            if (action == ToolAction.LEFT_CLICK) {
                ToolConfigureMenu.open(Text.of("Custom Tool"), script, settings);
                toolNbt.put(Identifiers.TOOL_SETTINGS_KEY, settings);
            } else {
                ToolExecutionContext context = new ToolExecutionContext(client, script, settings);
                if (action == ToolAction.RIGHT_CLICK) {
                    script.function().rightClick(context);
                }
            }
        }
    }

    private static CompiledScript getOrCreateScript(NbtList source, int hash) {
        return scriptCache.get(hash, (hash2) -> {
            StringBuilder builder = new StringBuilder();
            for (NbtElement element : source) {
                if (element instanceof NbtString nbtString) {
                    builder.append(nbtString.asString());
                    builder.append("\n");
                }
            }
            return ScriptParser.INSTANCE.parse("<script>", builder.toString());
        });
    };

}
