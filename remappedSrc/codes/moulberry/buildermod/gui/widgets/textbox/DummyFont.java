package codes.moulberry.buildermod.gui.widgets.textbox;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.text.CharacterIterator;

public class DummyFont extends Font {

    public static final DummyFont INSTANCE = new DummyFont();
    public static final TextRenderer font = MinecraftClient.getInstance().textRenderer;

    public DummyFont() {
        super("Minecraft", Font.PLAIN, 10);
    }

    @Override
    public Rectangle2D getStringBounds(String str, FontRenderContext frc) {
        float width = font.getWidth(str);
        return new Rectangle2D.Float(0f, 0, width, font.fontHeight);
    }

    @Override
    public Rectangle2D getStringBounds(char[] chars, int beginIndex, int limit,
                                       FontRenderContext frc) {
        return getStringBounds(new String(chars), beginIndex, limit, frc);
    }

}
