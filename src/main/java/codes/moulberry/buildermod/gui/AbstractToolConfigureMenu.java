package codes.moulberry.buildermod.gui;

import codes.moulberry.buildermod.BuilderMod;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.minecraft.text.Text;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public abstract class AbstractToolConfigureMenu extends LightweightGuiDescription {

    public AbstractToolConfigureMenu() {
        WBox root = new WBox(Axis.VERTICAL);
        setRootPanel(root);
        root.setSize(200, 0);
        root.setInsets(Insets.ROOT_PANEL);

        root.add(new WWidget(), 0, 8);

        create(root);

        root.validate(this);
    }

    protected final WBox createRadio(IntSupplier supplier, IntConsumer consumer, String... text) {
        WBox box = new WBox(Axis.HORIZONTAL);
        box.setSize(186, 20);

        float size = (186-(box.getSpacing())*(2))/3f;
        int floorSize = (int)Math.floor(size);
        int ceilSize = (int)Math.ceil(size + (size - floorSize)*3);

        int clicked = supplier.getAsInt();

        WButton[] buttons = new WButton[text.length];
        for (int index = 0; index < text.length; index++) {
            final int i = index;
            WButton button = new WButton(Text.of(text[i]));
            buttons[i] = button;

            if (clicked == i) {
                button.setEnabled(false);
            }
            button.setOnClick(() -> {
                consumer.accept(i);
                for (WButton button2 : buttons) {
                    button2.setEnabled(button2 != button);
                }
            });


            box.add(button, i == text.length/2 ? ceilSize : floorSize, 0);
        }

        return box;
    }

    protected final WLabeledSlider createSlider(String text, int min, int max, IntSupplier supplier, IntConsumer consumer) {
        return createSlider(text, "", min, max, supplier, consumer);
    }

    protected final WLabeledSlider createSlider(String text, String suffix, int min, int max, IntSupplier supplier, IntConsumer consumer) {
        final Text toolRadiusText = Text.of(text + ": " + supplier.getAsInt() + suffix);
        WLabeledSlider slider = new WLabeledSlider(min, max, Axis.HORIZONTAL, toolRadiusText);
        slider.setValue(supplier.getAsInt(), false);
        slider.setDraggingFinishedListener(consumer);
        slider.setLabelUpdater((val) -> Text.of(text + ": " + val + suffix));
        return slider;
    }

    protected abstract void create(WBox root);

}
