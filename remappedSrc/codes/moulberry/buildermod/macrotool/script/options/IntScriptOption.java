package codes.moulberry.buildermod.macrotool.script.options;

import io.github.cottonmc.cotton.gui.widget.WLabeledSlider;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.soabase.recordbuilder.core.RecordBuilder;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.OptionalInt;

@RecordBuilder
public record IntScriptOption(String id, String name, int defaultValue,
                              OptionalInt min, OptionalInt max) implements ScriptOption {

    public WWidget createConfigureWidget(NbtCompound settings) {
        int value = settings.contains(id, NbtType.INT) ? settings.getInt(id) : defaultValue;

        final Text text = Text.of(name + ": " + value);
        WLabeledSlider slider = new WLabeledSlider(min.orElse(0), max.orElse(100), Axis.HORIZONTAL, text);
        slider.setValue(value, false);
        slider.setDraggingFinishedListener((val) -> {
            if (val == defaultValue) {
                settings.remove(id);
            } else {
                settings.putInt(id, val);
            }
        });
        slider.setLabelUpdater((val) -> Text.of(name + ": " + val));
        return slider;
    }

}
