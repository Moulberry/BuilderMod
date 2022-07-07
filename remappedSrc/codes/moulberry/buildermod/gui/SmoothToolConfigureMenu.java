package codes.moulberry.buildermod.gui;

import codes.moulberry.buildermod.BuilderMod;
import codes.moulberry.buildermod.config.BMConfig;
import io.github.cottonmc.cotton.gui.widget.WBox;

public class SmoothToolConfigureMenu extends AbstractToolConfigureMenu {

    @Override
    protected void create(WBox root) {
        BMConfig config = BuilderMod.getInstance().config;

        root.add(createSlider("Tool Radius", 2, 50, () -> config.smoothToolRadius, (val) -> config.smoothToolRadius = val), 186, 0);
        root.add(createSlider("Gaussian Range", 1, 10, () -> config.smoothRadius, (val) -> config.smoothRadius = val), 186, 0);
        root.add(createSlider("Gaussian Strength", "%", 1, 100, () -> config.smoothStrength, (val) -> config.smoothStrength = val), 186, 0);
        root.add(createSlider("Block Bias", "%", -50, 50,
                () -> config.smoothAddBlockRatio, (val) -> config.smoothAddBlockRatio = val), 186, 0);
        root.add(createRadio(() -> config.smoothMode, (val) -> config.smoothMode = val, "Melt", "Stable", "Grow"));
    }
}