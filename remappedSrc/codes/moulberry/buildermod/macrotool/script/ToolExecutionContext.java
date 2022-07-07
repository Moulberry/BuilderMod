package codes.moulberry.buildermod.macrotool.script;

import codes.moulberry.buildermod.macrotool.script.options.IntScriptOption;
import codes.moulberry.buildermod.macrotool.script.options.ScriptOption;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;

import java.util.Objects;

public class ToolExecutionContext {

    private final MinecraftClient minecraft;
    private final ClientWorld world;
    private final ClientPlayerEntity player;

    private final CompiledScript script;
    private final NbtCompound settings;

    public ToolExecutionContext(MinecraftClient minecraft, CompiledScript script, NbtCompound settings) {
        this.minecraft = minecraft;
        this.world = minecraft.world;
        this.player = Objects.requireNonNull(minecraft.player);
        this.script = script;
        this.settings = settings;
    }

    public int getIntSetting(String id) {
        if (settings.contains(id, NbtType.INT)) {
            return settings.getInt(id);
        } else {
            ScriptOption option = script.options().get(id);
            if (option instanceof IntScriptOption intScriptOption) {
                return intScriptOption.defaultValue();
            }
        }
        return 0;
    }

    public MinecraftClient getMinecraft() {
        return minecraft;
    }

    public ClientWorld getWorld() {
        return world;
    }

    public ClientPlayerEntity getPlayer() {
        return player;
    }
}
