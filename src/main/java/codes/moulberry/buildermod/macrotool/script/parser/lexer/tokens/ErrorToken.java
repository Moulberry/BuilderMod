package codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens;

public class ErrorToken extends Token {

    public final String msg;

    public ErrorToken(String msg, String input, int start, int end) {
        super(input, start, end);
        this.msg = msg;
    }

}
