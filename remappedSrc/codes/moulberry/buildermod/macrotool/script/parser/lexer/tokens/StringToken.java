package codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens;

public class StringToken extends Token {

    public final String value;

    public StringToken(String value, String input, int start, int end) {
        super(input, start, end);
        this.value = value;
    }

}
