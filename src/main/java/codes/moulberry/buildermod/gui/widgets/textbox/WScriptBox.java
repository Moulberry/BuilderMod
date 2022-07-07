package codes.moulberry.buildermod.gui.widgets.textbox;

import codes.moulberry.buildermod.macrotool.script.parser.lexer.Lexer;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.*;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.*;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.List;

public class WScriptBox extends WTextBox implements DocumentListener {

    private static final Style COMMENT_STYLE = Style.EMPTY.withColor(0x59626F).withItalic(true);
    private static final Style IDENTIFIER_STYLE = Style.EMPTY.withColor(0xC3D3DE);
    private static final Style VARIABLE_STYLE = Style.EMPTY.withColor(0xC679DD);
    private static final Style OPERATOR_STYLE = Style.EMPTY.withColor(0x61AFEF);
    private static final Style NUMERIC_STYLE = Style.EMPTY.withColor(0xD19A66);
    private static final Style STRING_STYLE = Style.EMPTY.withColor(0x98C379);
    private static final Style ERROR_STYLE = Style.EMPTY.withColor(0xE06C75).withUnderline(true);
    private static final Style BLOCK_PREDICATE_STYLE = Style.EMPTY.withColor(0x98C379).withUnderline(true);

    private boolean dirty = true;
    private List<OrderedText> renderedText = new ArrayList<>();

    public WScriptBox() {
        super();
        textComponent.getDocument().addDocumentListener(this);
    }

    @Override
    protected void renderBox(MatrixStack matrices, int x, int y, int borderColor, int backgroundColor) {
        super.renderBox(matrices, x, y, 0xFF707080, 0xFF252930);
    }

    @Override
    protected void renderText(MatrixStack matrices, int x, int y) {
        if (dirty) {
            rerenderText();
        }
        for (OrderedText text : renderedText) {
            font.draw(matrices, text, x + TEXT_PADDING_X, y + TEXT_PADDING_Y, TEXT_COLOR);
            y += this.font.fontHeight;
        }
    }

    public void rerenderText() {
        dirty = false;
        renderedText.clear();

        String rawText = this.textComponent.getText();
        MutableText formattedText = LiteralText.EMPTY.copy();

        Lexer lexer = new Lexer(rawText);
        Token token;
        int lastIndex = 0;
        while ((token = lexer.next()) != null) {
            if (token.start > lastIndex) {
                String before = rawText.substring(lastIndex, token.start);
                formattedText.append(new LiteralText(before).setStyle(COMMENT_STYLE));
            }
            lastIndex = token.end;

            String text = rawText.substring(token.start, token.end);
            Style style;
            if (token instanceof IdentifierToken identifierToken) {
                if (identifierToken.type == IdentifierType.UNKNOWN) {
                    style = IDENTIFIER_STYLE;
                } else {
                    style = OPERATOR_STYLE;
                }
            } else if (token instanceof VariableToken) {
                style = VARIABLE_STYLE;
            } else if (token instanceof StringToken) {
                style = STRING_STYLE;
            } else if (token instanceof IntegerToken || token instanceof FloatToken) {
                style = NUMERIC_STYLE;
            } else if (token instanceof BlockToken) {
                style = BLOCK_PREDICATE_STYLE;
            } else {
                style = ERROR_STYLE;
            }
            formattedText.append(new LiteralText(text).setStyle(style));
        }
        if (rawText.length() > lastIndex) {
            String before = rawText.substring(lastIndex);
            formattedText.append(new LiteralText(before).setStyle(COMMENT_STYLE));
        }

        renderedText.addAll(ChatMessages.breakRenderedChatMessageLines(
                formattedText,
                this.width - TEXT_PADDING_X*2,
                font
        ));
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        dirty = true;
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        dirty = true;
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        dirty = true;
    }
}
