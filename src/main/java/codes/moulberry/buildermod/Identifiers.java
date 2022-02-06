package codes.moulberry.buildermod;

import net.minecraft.util.Identifier;

public class Identifiers {

    public static final Identifier SETBLOCK_MULTI = new Identifier("buildermod:setblock/multi");
    public static final Identifier SETBLOCK_SINGLE = new Identifier("buildermod:setblock/single");

    public static final Identifier CAPABILITIES = new Identifier("buildermod:capabilities");

    public static class Capabilities {
        public static final String NO_NEIGHBOR_UPDATES = "buildermod:capabilities/no_neighbor_updates";
        public static final String REPLACEMODE = "buildermod:capabilities/replacemode";
        public static final String INSTABREAK = "buildermod:capabilities/instabreak";
        public static final String ENHANCED_FLIGHT = "buildermod:capabilities/enhanced_flight";
    }

}
