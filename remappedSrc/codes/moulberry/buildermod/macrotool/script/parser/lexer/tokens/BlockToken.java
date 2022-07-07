package codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens;

public class BlockToken extends Token {

    public final String content;

    public BlockToken(String content, String input, int start, int end) {
        super(input, start, end);
        this.content = content;
    }

}
