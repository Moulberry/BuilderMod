package codes.moulberry.buildermod.gui;

import codes.moulberry.buildermod.Identifiers;
import codes.moulberry.buildermod.gui.widgets.textbox.WScriptBox;
import codes.moulberry.buildermod.gui.widgets.textbox.WTextBox;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WBox;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.Objects;

public class ToolCreateMenu extends LightweightGuiDescription {

    public static void open() {
        Text menuName = new LiteralText("Create Tool");
        ToolCreateMenu menu = new ToolCreateMenu();
        MinecraftClient.getInstance().setScreen(new CottonClientScreen(menuName, menu) {
            @Override
            public void removed() {
                menu.removed();
                super.removed();
            }
        });
    }

    private WTextBox textBox;

    public ToolCreateMenu() {
        WBox root = new WBox(Axis.VERTICAL);
        setRootPanel(root);
        root.setSize(0, 0);
        root.setInsets(Insets.ROOT_PANEL);
        root.add(new WWidget(), 0, 8);

        addWidgets(root);

        root.validate(this);
    }

    private void removed() {
        if (textBox.getText().trim().isEmpty()) {
            return;
        }

        ItemStack itemStack = new ItemStack(Items.SPECTRAL_ARROW);
        itemStack.setCustomName(Text.of("Custom Tool"));
        NbtCompound compound = itemStack.getOrCreateSubNbt(Identifiers.TOOL_NBT_KEY);

        NbtCompound script = new NbtCompound();

        NbtList source = new NbtList();
        for (String line : textBox.getText().split("\n")) {
            source.add(NbtString.of(line));
        }

        script.put(Identifiers.TOOL_SOURCE_KEY, source);
        script.put(Identifiers.TOOL_HASH_KEY, NbtInt.of(source.hashCode()));

        compound.put(Identifiers.TOOL_SCRIPT_KEY, script);

        ClientPlayerEntity player = Objects.requireNonNull(MinecraftClient.getInstance().player);
        player.getInventory().insertStack(itemStack);
    }

    private void addWidgets(WBox root) {
        root.add(textBox = new WScriptBox(), 250, 250);
    }

}
