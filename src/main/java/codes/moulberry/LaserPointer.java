package codes.moulberry;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class LaserPointer {

    private static LaserPointer INSTANCE = new LaserPointer();

    public static LaserPointer getInstance() {
        return INSTANCE;
    }

    public static class Chain {
        public List<Vec3d> points = new ArrayList<>();
        public List<Vec3d> renderPoints = null;
        public boolean looped = false;

        public void updateRenderPoints() {
            renderPoints = CatmullRomSpline.getCatmullRomChain(points, looped);
        }

    }

    private HashMap<UUID, Chain> chains = new HashMap<>();

    private Chain currentChain;
    private UUID currentChainUUID;

    public void clearAll() {
        endChain();
        chains.clear();
    }

    public void startChain() {
        if(currentChainUUID == null || currentChain == null) {
            currentChainUUID = UUID.randomUUID();
            currentChain = new Chain();

            chains.put(currentChainUUID, currentChain);
        }
    }

    public void endChain() {
        if(currentChainUUID != null && currentChain != null) {
            if(currentChain.points.isEmpty()) {
                chains.remove(currentChainUUID);
            } else {
                //Send to server
                chains.put(currentChainUUID, currentChain);
            }
        }
        currentChainUUID = null;
        currentChain = null;
    }

    public void addPoint(Vec3d position) {
        if(currentChainUUID != null && currentChain != null) {
            if(currentChain.points.size() > 1) {
                Vec3d first = currentChain.points.get(0);

                double distSq = first.distanceTo(position);
                if(distSq < 1) {
                    currentChain.looped = true;
                    currentChain.updateRenderPoints();
                    endChain();
                    return;
                }
            }

            currentChain.points.add(position);
            currentChain.updateRenderPoints();
        }
    }

    public HashMap<UUID, Chain> getChains() {
        return chains;
    }
}
