package codes.moulberry.buildermod.gui.blueprints;

import codes.moulberry.buildermod.blueprint.Blueprint;
import codes.moulberry.buildermod.customtool.TestTool;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class BlueprintLibraryMenu extends LightweightGuiDescription {

    public static Blueprint selected = null;
    public static List<Blueprint> blueprints = new ArrayList<>();

    public static Screen createScreen() {
        return new CottonClientScreen(Text.of("Select Blueprint"), new BlueprintLibraryMenu());
    }

    private BlueprintLibraryMenu() {
        WBox root = new WBox(Axis.VERTICAL);
        setRootPanel(root);

        root.setSize(300, 222);
        root.setInsets(Insets.ROOT_PANEL);
        root.add(new WWidget(), 0, 8);

        WGridPanel gridPanel = new WGridPanel(44);
        root.add(gridPanel);

        int index = 0;
        out:
        for (int y=0; y<5; y++) {
            for (int x=0; x<7; x++) {
                if (index >= blueprints.size()) break out;
                final Blueprint blueprint = blueprints.get(index++);

                WBox box = new WBox(Axis.VERTICAL) {
                    @Override
                    public InputResult onClick(int x, int y, int button) {
                        selected = blueprint;
                        TestTool.dirty = true;
                        MinecraftClient.getInstance().setScreen(null);
                        return InputResult.PROCESSED;
                    }
                };
                box.setHorizontalAlignment(HorizontalAlignment.CENTER);
                box.setSpacing(2);
                //box.setInsets(new Insets(0, 6));

                box.add(new WSprite(blueprint.getTextureId()), 32, 32);
                int width = MinecraftClient.getInstance().textRenderer.getWidth(blueprint.identifier.getPath());
                box.add(new WLabel(blueprint.identifier.getPath()), width, 12);

                gridPanel.add(box, x, y);
            }
        }

        root.validate(this);
    }

}
