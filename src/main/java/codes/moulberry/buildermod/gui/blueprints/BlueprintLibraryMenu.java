package codes.moulberry.buildermod.gui.blueprints;

import codes.moulberry.buildermod.BuilderMod;
import codes.moulberry.buildermod.blueprint.Blueprint;
import codes.moulberry.buildermod.blueprint.BlueprintLibrary;
import codes.moulberry.buildermod.customtool.TestTool;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.util.*;

public class BlueprintLibraryMenu extends LightweightGuiDescription {

    public static Screen createScreen() {
        return new CottonClientScreen(Text.of("Select Blueprint"), new BlueprintLibraryMenu());
    }

    private BlueprintLibraryMenu() {
        WBox root = new WBox(Axis.VERTICAL);
        setRootPanel(root);

        root.setInsets(Insets.ROOT_PANEL);
        root.add(new WWidget(), 0, 8);

        WGridPanel gridPanel = new WGridPanel(36);
        root.add(gridPanel);

        gridPanel.setSize(44*6, 196);
        //gridPanel.setBackgroundPainter(BackgroundPainter.SLOT);

        Iterator<Map.Entry<Identifier, Blueprint>> iterator = BlueprintLibrary.blueprints.entrySet().iterator();

        out:
        for (int y=0; y<5; y++) {
            for (int x=0; x<6; x++) {
                if (!iterator.hasNext()) break out;
                final Blueprint blueprint = iterator.next().getValue();

                WBox box = new WBox(Axis.VERTICAL) {
                    @Override
                    public InputResult onClick(int x, int y, int button) {
                        BlueprintLibrary.selected = blueprint;
                        TestTool.dirty = true;
                        MinecraftClient.getInstance().setScreen(null);
                        return InputResult.PROCESSED;
                    }


                    @Override
                    public void addTooltip(TooltipBuilder tooltip) {
                        tooltip.add(
                                Text.of(blueprint.identifier.getPath())
                        );
                    }
                };

                box.setBackgroundPainter((matrices, left, top, panel) -> {
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.enableBlend();
                    ScreenDrawing.drawBeveledPanel(matrices, left, top,
                            panel.getWidth(), panel.getHeight(),
                            0xFF373737, 0xFF8B8B8B, 0xFFFFFFFF);
                });

                box.setHorizontalAlignment(HorizontalAlignment.CENTER);
                box.setSpacing(0);

                box.add(new WWidget(), 0, 2);
                box.add(new WSprite(blueprint.getTextureId()) {
                    @Override
                    public void addTooltip(TooltipBuilder tooltip) {
                        tooltip.add(
                                Text.of(blueprint.identifier.getPath())
                        );
                    }
                }, 32, 32);
                box.add(new WWidget(), 0, 2);

                gridPanel.add(box, x, y);
            }
        }

        root.validate(this);
        System.out.println(root.getWidth()+":"+root.getHeight());
    }

}
