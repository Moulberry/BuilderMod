package codes.moulberry.buildermod.gui.widgets;

import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.WTextField;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class WParserField<T> extends WTextField {

    private Consumer<T> valueConsumer = null;
    private T value = null;
    private T defaultValue = null;

    public WParserField(T defaultValue) {
        super(Text.of(defaultValue.toString()));
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.setSuggestionColor(0xEEEEEE);
        super.setChangedListener(this::internalUpdate);
    }

    public WParserField(Text suggestion, T defaultValue) {
        super(suggestion);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        super.setChangedListener(this::internalUpdate);
    }

    public WParserField() {
        super.setChangedListener(this::internalUpdate);
    }

    public WParserField(Text suggestion) {
        super(suggestion);
        super.setChangedListener(this::internalUpdate);
    }

    protected abstract T parse(String str);
    protected String adjust(String str) {
        return null;
    }

    public boolean hasValue() {
        return getValue() != null;
    }

    public T getValue() {
        return this.value;
    }

    public WParserField<T> setValueConsumer(Consumer<T> valueConsumer) {
        this.valueConsumer = valueConsumer;
        return this;
    }

    private void internalUpdate(String str) {
        if (defaultValue != null && str.trim().isEmpty()) {
            this.value = defaultValue;
        } else {
            this.value = parse(str);
        }
        if (valueConsumer != null) valueConsumer.accept(value);
    }

    @Environment(EnvType.CLIENT)
    protected void renderBox(MatrixStack matrices, int x, int y) {
        if (value == null) {
            int borderColor = 0xFFEE0033;
            ScreenDrawing.coloredRect(matrices, x - 1, y - 1, width + 2, height + 2, borderColor);
            ScreenDrawing.coloredRect(matrices, x, y, width, height, 0xFF000000);
        } else {
            super.renderBox(matrices, x, y);
        }
    }

    @Override
    public WTextField setChangedListener(Consumer<String> listener) {
        return super.setChangedListener((str) -> {
            listener.accept(str);
            internalUpdate(str);
        });
    }

    @Override
    public void onFocusLost() {
        String adjusted = adjust(this.getText());
        if (adjusted != null) {
            this.setText(adjusted);
        }
    }

    @Override
    public InputResult onClick(int x, int y, int button) {
        if (button == 1) {
            this.setText("");
        }
        return super.onClick(x, y, button);
    }
}
