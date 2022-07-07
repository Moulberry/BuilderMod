package codes.moulberry.buildermod.gui;

import codes.moulberry.buildermod.macrotool.script.CompiledScript;
import codes.moulberry.buildermod.macrotool.script.options.ScriptOption;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WBox;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class ToolConfigureMenu extends LightweightGuiDescription {

    public static void open(Text toolName, CompiledScript script, NbtCompound settings) {
        Text menuName = new LiteralText("Configure: ").append(toolName);
        ToolConfigureMenu menu = new ToolConfigureMenu(script, settings);
        MinecraftClient.getInstance().setScreen(new CottonClientScreen(menuName, menu));
    }

    public ToolConfigureMenu(CompiledScript script, NbtCompound settings) {
        WBox root = new WBox(Axis.VERTICAL);
        setRootPanel(root);
        root.setSize(200, 0);
        root.setInsets(Insets.ROOT_PANEL);
        root.add(new WWidget(), 0, 8);

        addOptionWidgets(root, script, settings);

        root.validate(this);
    }

    private void addOptionWidgets(WBox root, CompiledScript script, NbtCompound settings) {
        for (ScriptOption option : script.options().values()) {
            root.add(option.createConfigureWidget(settings), 186, 0);
        }
    }

}
