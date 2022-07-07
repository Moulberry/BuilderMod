package codes.moulberry.buildermod.macrotool.script.parser.lexer;

import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.Token;

import java.util.Arrays;
import java.util.Locale;

public final class Lexer {

    

    /**
     * Line feed character.
     */
    private final static byte LF = 0xA;

    /**
     * Form feed character.
     */
    private final static byte FF = 0xC;

    /**
     * Carriage return character.
     */
    final static byte CR = 0xD;

    /**
     * End of input character.
     */
    final static byte EOI = 0x1A;

    

    /**
     * Input string to be tokenized.
     */
    public final int[] string;

    

    public Lexer(String string) {
        this.string = string.toLowerCase(Locale.ROOT).codePoints().toArray();
    }

    

    /**
     * Input position.
     */
    private int i;

    /**
     * Buffer used to read in data whenever characters can be skipped.
     *
     * <p>For identifiers this includes for instance some control characters.
     *
     * <p>For numbers, this includes everything that would preclude the literal from being parsed
     * by {@link Integer#parseInt} & co, so things like underscores and some suffixes.
     */
    private int[] buf = new int[128];

    /**
     * Buffer Pointer
     */
    private int bp = 0;

    /**
     * Position of the first character of {@link #buf} in the input.
     */
    private int start;

    

    /**
     * Appends {@code c} to {@link #buf}.
     */
    private void put_char(int c) {
        if (bp == buf.length)
            buf = Arrays.copyOf(buf, (int) (buf.length * 1.5 + 1));

        buf[bp++] = c;
    }

    private void put_char_and_advance(int c) {
        if (bp == buf.length)
            buf = Arrays.copyOf(buf, (int) (buf.length * 1.5 + 1));

        buf[bp++] = c;
        ++i;
    }

    

    private String popContent() {
        int len = bp;
        bp = 0;
        return new String(buf, 0, len);
    }

    private String input() {
        return new String(string, start, i - start);
    }

    private boolean isWhitespace(int codepoint) {
        return Character.isWhitespace(codepoint) || codepoint == ',';
    }

    private int get_char(int i) {
        return i < string.length
                ? string[i]
                : EOI;
    }

    

    /**
     * Skips past and emits the next token.
     */
    public Token next() {
        int c;
        while (true) {
            start = i;

            int original = get_char(i);
            switch (original) {
                // skip whitespace

                case ' ':
                case '\t':
                case ',':
                case FF:
                    do {
                        c = get_char(++i);
                    } while (c == ' ' || c == ',' || c == '\t' || c == FF);
                    break;

                case LF:
                    ++i;
                    break;

                case CR:
                    ++i;
                    if (get_char(i) == LF) ++i;
                    break;

                // identifiers
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                case '_':
                case '$':
                    return scan_ident();

                case '.':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    return scan_number();

                // end of input

                case EOI:
                    return (i >= string.length - 1)
                            ? null
                            : scan_ident();

                // comments and slashes

                case '/':
                    c = get_char(++i);
                    if (c == '/') {
                        do {
                            c = get_char(++i);
                        }
                        while (c != CR && c != LF && i < string.length);
                        break;
                    } else if (c == '*') {
                        c = get_char(++i);
                        if (c == '*') {
                            c = get_char(i + 1);
                        }
                        while (i < string.length) {
                            if (c == '*') {
                                c = get_char(++i);
                                if (c == '/') break;
                            } else
                                c = get_char(++i);
                        }
                        if (c != '/')
                            return error("unclosed comment");
                        ++i;
                        break;
                    } else {
                        return error("illegal char: /");
                    }

                // string literal
                case '\'':
                case '\"':
                    int x;
                    c = get_char(++i);
                    while (c != original) {
                        x = scan_lit_char();
                        if (x == -1)
                            return literal_error();
                        put_char(x);
                        c = get_char(i);
                        if (c == CR || c == LF)
                            return error("unclosed string literal");
                    }
                    ++i;
                    return Token.string(popContent(), input(), start, i);

                // weird identifier parts and illegal characters

                default:
                    c = get_char(i);
                    if (c >= '\u0080') { // not ascii
                        if (Character.isJavaIdentifierStart(c))
                            return scan_ident();
                    }
                    String arg = (32 < c && c < 127) // printable ascii char?
                            ? String.format("%s", c)
                            : String.format("\\u%04x", c);
                    ++i;
                    return error("illegal char: " + arg);
            }
        }
    }

    

    /**
     * Skips past and emits an identifier token.
     *
     * <p>Assumes we are positioned over a valid start of identifier character.
     */
    private Token scan_ident() {
        int c = get_char(i);
        if (c == '$') put_char_and_advance(c);
        while (true) {
            c = get_char(i);
            switch (c) {
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                case '_':
                    put_char_and_advance(c);
                    continue;
                case '\u0000':
                case '\u0001':
                case '\u0002':
                case '\u0003':
                case '\u0004':
                case '\u0005':
                case '\u0006':
                case '\u0007':
                case '\u0008':
                case '\u000E':
                case '\u000F':
                case '\u0010':
                case '\u0011':
                case '\u0012':
                case '\u0013':
                case '\u0014':
                case '\u0015':
                case '\u0016':
                case '\u0017':
                case '\u0018':
                case '\u0019':
                case '\u001B':
                case '\u007F': // control characters
                    ++i;
                    continue;
                case EOI:
                    if (i >= string.length - 1)
                        return handle_ident();
                    // otherwise treat as a control character
                    ++i;
                    continue;
                default:
                    if (c < '\u0080') // all ASCII range chars already handled above
                        return handle_ident();
                    else if (Character.isIdentifierIgnorable(c))
                        ++i;
                    else if (Character.isJavaIdentifierPart(c))
                        put_char_and_advance(c);
                    else
                        return handle_ident();
            }
        }
    }

    private Token handle_ident() {
        return Token.identifier(popContent(), input(), start, i);

    }

    

    /**
     * Skips past a number (integral or floating point) literal with the given radix, and emits a
     * token for it.
     *
     * <p>Since floating point literals may start with a 0, a radix of 8 will be treated as
     * effectively as radix of 10. However, the emitted token will have the proper radix field.
     * Octal literals must be checked for invalid digits later.
     *
     * <p>Assumes we are positionned at a position where we expect a number to start, but past
     * the radix indicator (0x or 0b) if present.
     */
    private Token scan_number() {
        boolean seen_period = false;
        while (true) {
            int c = get_char(i);
            if (c == '.') {
                if (seen_period) {
                    return Token.error("invalid character in number literal", input(), start, i);
                } else {
                    seen_period = true;
                    put_char_and_advance(c);
                }
            } else if ('0' <= c && c <= '9') {
                put_char_and_advance(c);
            } else if (c == EOI || isWhitespace(c)) {
                return Token.numberToken(popContent(), input(), start, i);
            } else {
                return Token.error("invalid character in number literal", input(), start, i);
            }
        }
    }

    /**
     * Scans the next character literal, which may be an escape sequence. Returns the scanned char,
     * or -1 if either we have reached the end of the input while scanning, or an invalid escape is
     * found.
     *
     * <p>Leaves the position right after the last scanned character, or at the end of the input.
     *
     * <p>Note that Java is very permissive, it allows even non-printable characters inside
     * literals.
     */
    private int scan_lit_char() {
        int c = get_char(i);

        if (c != '\\')
            return i < string.length
                    ? get_char(i++)
                    : -1;

        switch (c = get_char(++i)) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
                int lead = c;
                int oct = Integer.parseInt(Character.toString(lead), 8);
                c = get_char(++i);
                if ('0' <= c && c <= '7') {
                    oct = oct * 8 + Integer.parseInt(Character.toString(c), 8);
                    c = get_char(++i);
                    if (lead <= '3' && '0' <= c && c <= '7') {
                        oct = oct * 8 + Integer.parseInt(Character.toString(c), 8);
                        ++i;
                    }
                }
                return oct;

            case 'u':
                int hex = 0;
                for (int j = 0; j < 4; ) {
                    c = get_char(++i);
                    if (c == 'u') {
                        continue;
                    }
                    if ('0' <= c && c <= '9' || 'a' <= c && c <= 'f') {
                        hex = hex * 16 + Integer.parseInt(Character.toString(c), 16);
                        ++j;
                    } else {
                        return -1;
                    }
                }
                ++i;
                return hex;

            case 'b':
                ++i;
                return '\b';
            case 't':
                ++i;
                return '\t';
            case 'n':
                ++i;
                return '\n';
            case 'f':
                ++i;
                return '\f';
            case 'r':
                ++i;
                return '\r';
            case '\'':
                ++i;
                return '\'';
            case '\"':
                ++i;
                return '\"';
            case '\\':
                ++i;
                return '\\';
            default:
                ++i;
                return -1;
        }
    }

    /**
     * Emits the proper error token when {@link #scan_lit_char()} fails.
     */
    private Token literal_error() {
        if (i == string.length) {
            return error("unclosed literal");
        } else {
            return error("illegal escape in literal");
        }
    }

    private Token error(String msg) {
        return Token.error(msg, input(), start, i);
    }

}
