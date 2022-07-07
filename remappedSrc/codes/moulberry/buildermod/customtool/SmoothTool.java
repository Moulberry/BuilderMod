package codes.moulberry.buildermod.customtool;

import codes.moulberry.buildermod.BuilderMod;
import codes.moulberry.buildermod.Identifiers;
import codes.moulberry.buildermod.config.BMConfig;
import codes.moulberry.buildermod.gui.SmoothToolConfigureMenu;
import codes.moulberry.buildermod.render.regions.BooleanRegion;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Objects;

public class SmoothTool extends GenericTool {

    @Override
    public void leftClick() {
        MinecraftClient.getInstance().setScreen(new CottonClientScreen(Text.of("Smooth Tool Configure"),
                new SmoothToolConfigureMenu()));
    }

    private static float gaussian(float a, int x) {
        return (float)(Math.sqrt(a/Math.PI)*Math.exp(-a*x*x));
    }

    private static float[] createKernel(int radius, float rawStrength) {
        if (rawStrength < 0.001f) rawStrength = 0.001f;
        float strength = (float)(Math.log(2/rawStrength)/Math.log(2));

        float a = 0.05f*strength;
        float[] kernel = new float[radius*2+1];
        for (int i=0; i<kernel.length; i++) {
            kernel[i] = gaussian(a, i-radius);
        }
        return kernel;
    }

    private static final class BlurData {
        private float weight;
        private float highest;
        private short block;

        private BlurData(float weight, float highest, short block) {
            this.weight = weight;
            this.highest = highest;
            this.block = block;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlurData blurData = (BlurData) o;
            return Float.compare(blurData.weight, weight) == 0 && Float.compare(blurData.highest, highest) == 0 && block == blurData.block;
        }

        @Override
        public int hashCode() {
            return Objects.hash(weight, highest, block);
        }
    }

    @Override
    protected void apply(BooleanRegion region) {
        BMConfig config = BuilderMod.getInstance().config;

        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        int radius = config.smoothRadius;
        float[] kernel = createKernel(radius, config.smoothStrength/100f);

        Long2ObjectMap<BlockPos> map = region.getIncludedBlocksMap();
        LongSet settableBlocks = new LongOpenHashSet(map.keySet());

        /**
         * Extend region
         */
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (BlockPos pos : new ArrayList<>(map.values())) {
            for (int i=1; i<=radius; i++) {
                mutable.set(pos, 0, i, 0);
                if (addNeighbor(region, world, map, settableBlocks, mutable)) break;
            }
            for (int i=1; i<=radius; i++) {
                mutable.set(pos, 0, -i, 0);
                if (addNeighbor(region, world, map, settableBlocks, mutable)) break;
            }
        }
        for (BlockPos pos : new ArrayList<>(map.values())) {
            for (int i=1; i<=radius; i++) {
                mutable.set(pos, i, 0, 0);
                if (addNeighbor(region, world, map, settableBlocks, mutable)) break;
            }
            for (int i=1; i<=radius; i++) {
                mutable.set(pos, -i, 0, 0);
                if (addNeighbor(region, world, map, settableBlocks, mutable)) break;
            }
        }
        for (BlockPos pos : new ArrayList<>(map.values())) {
            for (int i=1; i<=radius; i++) {
                mutable.set(pos, 0, 0, i);
                if (addNeighbor(region, world, map, settableBlocks, mutable)) break;
            }
            for (int i=1; i<=radius; i++) {
                mutable.set(pos, 0, 0, -i);
                if (addNeighbor(region, world, map, settableBlocks, mutable)) break;
            }
        }

        /**
         * Calculate first Y gaussian blur
         */
        AbstractLong2ObjectMap<BlurData> map1 = new Long2ObjectOpenHashMap<>(map.size());
        for (BlockPos value : map.values()) {
            BlockState blockState = world.getBlockState(value);
            short stateId = (short)Block.getRawIdFromState(blockState);

            for (int i=0; i<kernel.length; i++) {
                float amount = kernel[i];
                long id = BlockPos.asLong(value.getX(), value.getY()+i-radius, value.getZ());

                if (settableBlocks.contains(id) || map.containsKey(id)) {
                    set(map1, stateId, amount, id);
                }
            }
        }

        /**
         * Calculate X & Z blurs
         */
        ShortSet stateIds = new ShortOpenHashSet();
        AbstractLong2ObjectMap<BlurData> map2 = blurX(map1, kernel, radius, settableBlocks);
        AbstractLong2ObjectMap<BlurData> map3 = blurZ(map2, stateIds, kernel, radius, settableBlocks);

        /**
         * Sort based on the weight
         */
        var list = new ArrayList<>(map3.long2ObjectEntrySet());
        list.sort((entry1, entry2) -> Float.compare(entry2.getValue().weight, entry1.getValue().weight));

        /**
         * Send changed blocks to server
         */
        int total = Math.round(region.totalCubes()*(1+config.smoothAddBlockRatio/100f));
        BlockPos playerBlockPos = MinecraftClient.getInstance().player.getBlockPos();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        switch (config.smoothMode) {
            case 0 -> buf.writeVarInt(list.size() - total);
            case 2 -> buf.writeVarInt(total);
            default -> buf.writeVarInt(list.size());
        }
        buf.writeBlockPos(playerBlockPos);

        Short2IntMap palette = new Short2IntOpenHashMap(stateIds.size());
        buf.writeVarInt(stateIds.size()+1);
        buf.writeShort(0);
        int index = 1;
        for (short s : stateIds) {
            if (s == 0) continue;
            palette.put(s, index++);
            buf.writeShort(s);
        }

        index = 0;
        for (Long2ObjectMap.Entry<BlurData> entry : list) {
            if (index++ < total) {
                if (config.smoothMode != 0) {
                    buf.writeInt(createEncodedOffset(playerBlockPos, entry.getLongKey()));
                    buf.writeVarInt(palette.get(entry.getValue().block));
                }
            } else {
                if (config.smoothMode != 2) {
                    buf.writeInt(createEncodedOffset(playerBlockPos, entry.getLongKey()));
                    buf.writeVarInt(0);
                }
            }
        }

        ClientPlayNetworking.send(Identifiers.SETBLOCK_MULTI, buf);
    }

    private boolean addNeighbor(BooleanRegion region, World world, Long2ObjectMap<BlockPos> map, LongSet settableBlocks, BlockPos.Mutable mutable) {
        long id = mutable.asLong();
        if (region.isInRegion(id)) return true;
        BlockState blockAt = world.getBlockState(mutable);
        if (blockAt.isAir()) {
            settableBlocks.add(id);
        } else {
            map.put(id, mutable.toImmutable());
        }
        return false;
    }

    private static AbstractLong2ObjectMap<BlurData> blurX(AbstractLong2ObjectMap<BlurData> in,
                                                         float[] kernel, int radius, LongSet includedBlocks) {
        AbstractLong2ObjectMap<BlurData> returnMap = new Long2ObjectOpenHashMap<>(in.size());

        for (Long2ObjectMap.Entry<BlurData> entry : in.long2ObjectEntrySet()) {
            long key = entry.getLongKey();
            short stateId = entry.getValue().block;

            for (int i=0; i<kernel.length; i++) {
                float amount = kernel[i] * entry.getValue().weight;
                long id = BlockPos.add(key, i-radius, 0, 0);
                if (includedBlocks.contains(id) || in.containsKey(id)) {
                    set(returnMap, stateId, amount, id);
                }
            }
        }
        return returnMap;
    }

    private static AbstractLong2ObjectMap<BlurData> blurZ(AbstractLong2ObjectMap<BlurData> in, ShortSet blockTypes,
                                                          float[] kernel, int radius, LongSet includedBlocks) {
        AbstractLong2ObjectMap<BlurData> returnMap = new Long2ObjectOpenHashMap<>(in.size());

        for (Long2ObjectMap.Entry<BlurData> entry : in.long2ObjectEntrySet()) {
            long key = entry.getLongKey();
            short stateId = entry.getValue().block;

            for (int i=0; i<kernel.length; i++) {
                float amount = kernel[i] * entry.getValue().weight;
                long id = BlockPos.add(key, 0, 0, i-radius);
                if (includedBlocks.contains(id)) {
                    blockTypes.add(stateId);
                    set(returnMap, stateId, amount, id);
                }
            }
        }
        return returnMap;
    }

    private static void set(AbstractLong2ObjectMap<BlurData> returnMap, short stateId, float amount, long id) {
        BlurData data = returnMap.get(id);
        if (data != null) {
            data.weight = data.weight + amount;
            if (amount > data.highest) {
                data.highest = amount;
                data.block = stateId;
            }
        } else {
            returnMap.put(id, new BlurData(amount, amount, stateId));
        }
    }

    @Override
    protected int toolRadius() {
        return BuilderMod.getInstance().config.smoothToolRadius;
    }

    @Override
    protected boolean isSelectable(BlockState blockState) {
        return !blockState.isAir();
    }

}
