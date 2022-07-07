package codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens;

public class IdentifierToken extends Token {

    public final IdentifierType type;
    public final String identifier;

    public IdentifierToken(IdentifierType type, String identifier, String input, int start, int end) {
        super(input, start, end);
        this.type = type;
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return "IdentifierToken{" +
                "type=" + type +
                ", identifier='" + identifier + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
