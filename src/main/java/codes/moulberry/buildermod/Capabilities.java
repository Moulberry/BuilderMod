package codes.moulberry.buildermod;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class Capabilities {

    public static final Capability NO_NEIGHBOR_UPDATES = new Capability("No Neighbor Updates", () -> BuilderMod.getInstance().config.stopNeighborUpdate);
    public static final Capability REPLACE_MODE = new Capability("Replace Mode", () -> BuilderMod.getInstance().config.replaceMode);
    public static final Capability INSTABREAK = new Capability("InstaBreak", () -> BuilderMod.getInstance().config.instabreak);
    public static final Capability ENHANCED_FLIGHT = new Capability("Enhanced Flight", () -> BuilderMod.getInstance().config.enhancedFlight);

    public static final Map<String, Capability> NAMED = new LinkedHashMap<>();
    static {
        NAMED.put(Identifiers.Capabilities.NO_NEIGHBOR_UPDATES, NO_NEIGHBOR_UPDATES);
        NAMED.put(Identifiers.Capabilities.ENHANCED_FLIGHT, ENHANCED_FLIGHT);
        NAMED.put(Identifiers.Capabilities.REPLACEMODE, REPLACE_MODE);
        NAMED.put(Identifiers.Capabilities.INSTABREAK, INSTABREAK);
    }

    private record CapabilityState(boolean allows, boolean forces) {
        private static CapabilityState DEFAULT = new CapabilityState(false, false);
    }

    public static final class Capability {
        private final String prettyName;
        private CapabilityState state = CapabilityState.DEFAULT;
        private final BooleanSupplier supplier;

        public Capability(String prettyName, BooleanSupplier supplier) {
            this.prettyName = prettyName;
            this.supplier = supplier;
        }

        public String getPrettyName() {
            return prettyName;
        }

        public boolean isEnabled() {
            return state.forces || (state.allows && supplier.getAsBoolean());
        }
    }

    public static void requestCapabilities(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        ClientPlayNetworking.send(Identifiers.CAPABILITIES, new PacketByteBuf(Unpooled.buffer()));
        NO_NEIGHBOR_UPDATES.state = CapabilityState.DEFAULT;
        REPLACE_MODE.state = CapabilityState.DEFAULT;
        ENHANCED_FLIGHT.state = CapabilityState.DEFAULT;
        INSTABREAK.state = CapabilityState.DEFAULT;
    }

    public static void setupPackets() {
        ClientPlayNetworking.registerGlobalReceiver(Identifiers.CAPABILITIES, (client, handler, buf, responseSender) -> {
            int max = buf.readVarInt();
            for (int i=0; i<max; i++) {
                String id = buf.readString();
                boolean allows = buf.readBoolean();
                boolean forces = buf.readBoolean();
                if (NAMED.containsKey(id)) {
                    NAMED.get(id).state = new CapabilityState(allows, forces);
                }
            }
        });
    }

}
