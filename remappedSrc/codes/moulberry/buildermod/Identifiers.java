package codes.moulberry.buildermod;

import net.minecraft.util.Identifier;

public class Identifiers {

    // =============================================================================================
    // region [Custom Tools]
    // =============================================================================================

    public static final String TOOL_NBT_KEY = "axiom_tool";

    // Script
    public static final String TOOL_SCRIPT_KEY = "script";
    public static final String TOOL_SOURCE_KEY = "source";
    public static final String TOOL_HASH_KEY = "hash";

    // Settings
    public static final String TOOL_SETTINGS_KEY = "settings";

    // endregion
    // =============================================================================================
    // region [Packets]
    // =============================================================================================
    public static final Identifier SETBLOCK_MULTI = new Identifier("buildermod:setblock/multi");
    public static final Identifier SETBLOCK_SINGLE = new Identifier("buildermod:setblock/single");
    public static final Identifier CAPABILITIES = new Identifier("buildermod:capabilities");

    public static class Capabilities {
        public static final String NO_NEIGHBOR_UPDATES = "buildermod:capabilities/no_neighbor_updates";
        public static final String REPLACEMODE = "buildermod:capabilities/replacemode";
        public static final String INSTABREAK = "buildermod:capabilities/instabreak";
        public static final String ENHANCED_FLIGHT = "buildermod:capabilities/enhanced_flight";
    }
    // endregion
    // =============================================================================================

}
