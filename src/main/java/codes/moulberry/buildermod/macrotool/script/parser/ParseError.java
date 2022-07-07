package codes.moulberry.buildermod.macrotool.script.parser;

public class ParseError extends RuntimeException {

    public ParseError(String msg, Object... args) {
        super(String.format(msg, args), null, true, false); // no stack trace
    }

    @Override public int hashCode() {
        return 31 * getMessage().hashCode();
    }

    @Override public boolean equals (Object obj) {
        return obj instanceof ParseError && ((ParseError) obj).getMessage().equals(getMessage());
    }
}