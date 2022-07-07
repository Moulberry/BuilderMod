package codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens;

public class FloatToken extends Token {

    public final float value;

    public FloatToken(float value, String input, int start, int end) {
        super(input, start, end);
        this.value = value;
    }

}
