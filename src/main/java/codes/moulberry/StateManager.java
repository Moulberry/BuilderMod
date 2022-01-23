package codes.moulberry;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

public class StateManager {

    private static final Identifier CHANNEL_NEIGHBORUPDATES = new Identifier("gauntlet_build:block_neighbor_updates");
    public static boolean stopNeighborUpdate = false;

    private static final Identifier CHANNEL_REPLACE_MODE = new Identifier("gauntlet_build:replace_mode");
    public static boolean replaceMode = false;

    public static void setupPackets() {
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_NEIGHBORUPDATES, (client, handler, buf, responseSender) ->{
            boolean data = buf.readBoolean();
            System.out.println("NEIGHBOR UPDATE: " + data);
            stopNeighborUpdate = data;
        });
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_REPLACE_MODE, (client, handler, buf, responseSender) ->{
            boolean data = buf.readBoolean();
            System.out.println("REPLACE MODE: " + data);
            replaceMode = data;
        });
    }

    public static void onWorldChange() {
        replaceMode = false;
        stopNeighborUpdate = false;
    }

}
