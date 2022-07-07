package codes.moulberry.buildermod.gui.widgets.textbox;

import net.minecraft.client.font.TextRenderer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class DummyFontMetrics extends FontMetrics {

    private final TextRenderer textRenderer;
    private final int[] widths = new int[256];

    public DummyFontMetrics(TextRenderer textRenderer, Font font) {
        super(font);
        this.textRenderer = textRenderer;

        for (char c = 0; c < 256; c++) {
            widths[c] = textRenderer.getWidth(""+c);
        }
    }

    @Override
    public int getAscent() {
        return 0;
    }

    @Override
    public int getDescent() {
        return textRenderer.fontHeight;
    }

    @Override
    public int charWidth(char ch) {
        if (ch < widths.length) {
            return widths[ch];
        }
        return textRenderer.getWidth(""+ch);
    }

    @Override
    public int stringWidth(@NotNull String str) {
        return textRenderer.getWidth(str);
    }

    @Override
    public int[] getWidths() {
        return widths;
    }
}
