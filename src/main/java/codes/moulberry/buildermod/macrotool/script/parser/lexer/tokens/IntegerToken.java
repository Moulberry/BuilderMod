package codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens;

public class IntegerToken extends Token {

    public final int value;

    public IntegerToken(int value, String input, int start, int end) {
        super(input, start, end);
        this.value = value;
    }

}
