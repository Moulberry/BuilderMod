package codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens;

public enum IdentifierType {
    UNKNOWN(IdentifierTag.NONE),

    // settings
    REQUIRE_INT(IdentifierTag.SETTING, "require_int"),

    // functions
    SPHERE(IdentifierTag.FUNCTION, "sphere"),

    // masks
    MASK(IdentifierTag.MASK,"mask", "replace"),
    BELOW(IdentifierTag.MASK, "below"),
    NEAR(IdentifierTag.MASK, "near"),

    // updates
    SET(IdentifierTag.UPDATE, "set", "with");

    public final IdentifierTag tag;
    IdentifierType(IdentifierTag tag, String... aliases) {
        this.tag = tag;
        for (String alias : aliases) {
            Token.registerAlias(this, alias);
        }
    }
}
