package codes.moulberry.buildermod.gui.blueprints;

import codes.moulberry.buildermod.blueprint.Blueprint;
import codes.moulberry.buildermod.blueprint.ProtoBlueprint;
import codes.moulberry.buildermod.gui.widgets.WBlueprintPreview;
import codes.moulberry.buildermod.gui.widgets.WIdentifierField;
import codes.moulberry.buildermod.gui.widgets.WIntField;
import codes.moulberry.buildermod.gui.widgets.WTagsField;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class BlueprintCreateMenu extends LightweightGuiDescription {

    public static Screen createScreen(ProtoBlueprint protoBlueprint) {
        return new CottonClientScreen(Text.of("Create New Blueprint"), new BlueprintCreateMenu(protoBlueprint));
    }

    private BlueprintCreateMenu(ProtoBlueprint protoBlueprint) {
        WBox root = new WBox(Axis.VERTICAL);
        setRootPanel(root);

        root.setInsets(Insets.ROOT_PANEL);

        root.add(new WWidget(), 0, 8);

        final String defaultAuthor = MinecraftClient.getInstance().getSession().getUsername();

        WBlueprintPreview blueprintPreview;
        WIdentifierField nameField;
        WTextField authorField;
        WIntField pivotXField;
        WIntField pivotYField;
        WIntField pivotZField;
        WTagsField tagsField;

        { // Split box
            WBox splitBox = new WBox(Axis.HORIZONTAL);
            root.add(splitBox);

            // Blueprint preview
            splitBox.add(blueprintPreview = new WBlueprintPreview(protoBlueprint), 142, 142);

            { // Settings
                WBox settings = new WBox(Axis.VERTICAL);
                splitBox.add(settings, 142, 142);

                settings.add(new WWidget(), 0, -3);

                // Name
                settings.add(new WLabel("Name: "), 0, 8);
                nameField = new WIdentifierField("minecraft", "minecraft:tree");
                settings.add(nameField, 142, 20);

                // Author
                settings.add(new WLabel("Author: "), 0, 8);
                authorField = new WTextField(Text.of(defaultAuthor)).setSuggestionColor(0xEEEEEE);
                settings.add(authorField, 142, 20);

                settings.add(new WLabel("Pivot: "), 0, 8);

                { // Pivot settings
                    WBox pivotSettings = new WBox(Axis.HORIZONTAL);
                    settings.add(pivotSettings, 142, 20);
                    pivotSettings.add(pivotXField = new WIntField(protoBlueprint.sizeX()/2), 45, 20);
                    pivotSettings.add(pivotYField = new WIntField(protoBlueprint.sizeY()/2), 44, 20);
                    pivotSettings.add(pivotZField = new WIntField(protoBlueprint.sizeZ()/2), 45, 20);
                }

                // Tags
                settings.add(new WLabel("Tags: "), 0, 8);
                tagsField = new WTagsField("tree,spruce,big");
                settings.add(tagsField, 142, 20);
            }
        }

        { // Buttons box
            WBox buttonsBox = new WBox(Axis.HORIZONTAL);
            buttonsBox.setSpacing(3);
            root.add(buttonsBox);

            // Cancel
            buttonsBox.add(new WButton(Text.of("Cancel")).setOnClick(() -> {
                MinecraftClient.getInstance().setScreen(null);
            }), 142, 20);

            // Create
            buttonsBox.add(new WButton(Text.of("Create")) {
                @Override
                public void tick() {
                    boolean enabled = nameField.hasValue() && pivotXField.hasValue() &&
                            pivotYField.hasValue() && pivotZField.hasValue();
                    this.setEnabled(enabled);
                    super.tick();
                }
            }.setOnClick(() -> {
                Identifier name = nameField.getValue();
                if (name == null) return;

                String author = authorField.getText();
                if (author == null) author = defaultAuthor;

                int pivotX = pivotXField.getValue();
                int pivotY = pivotYField.getValue();
                int pivotZ = pivotZField.getValue();

                List<String> tags = tagsField.getValue();

                byte[] icon = blueprintPreview.getIcon();
                if (icon == null) {
                    MinecraftClient.getInstance().setScreen(null);
                    MinecraftClient.getInstance().player.sendMessage(Text.of("Failed to render icon!"), false);
                    return;
                }

                Blueprint blueprint = new Blueprint(protoBlueprint, name, author,
                        pivotX, pivotY, pivotZ, tags, icon);
                BlueprintLibraryMenu.blueprints.add(blueprint);
                MinecraftClient.getInstance().setScreen(null);
            }), 144, 20);
        }

        root.validate(this);
    }

}
