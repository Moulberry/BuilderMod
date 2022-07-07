package codes.moulberry.buildermod.blueprint;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ShortMap;
import it.unimi.dsi.fastutil.ints.Int2ShortRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.Short2IntRBTreeMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Blueprint {

    public BlockState[][][] blockStates;
    private Long2ObjectMap<NbtCompound> blockEntities;
    public int sizeX;
    public int sizeY;
    public int sizeZ;
    public int pivotX;
    public int pivotY;
    public int pivotZ;

    // Metadata
    public Identifier identifier;
    public String author;
    public List<String> tags;
    public int version = 1;

    // Icon
    private Identifier textureId = null;
    private byte[] icon;

    private Blueprint(BlockState[][][] blockStates, Long2ObjectMap<NbtCompound> blockEntities,
                      int sizeX, int sizeY, int sizeZ, Identifier identifier, String author,
                      int pivotX, int pivotY, int pivotZ, List<String> tags, byte[] icon) {
        this.blockStates = blockStates;
        this.blockEntities = blockEntities;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.pivotZ = pivotZ;

        this.identifier = identifier;
        this.author = author;
        this.tags = tags;

        this.icon = icon;
    }

    public Blueprint(ProtoBlueprint protoBlueprint, Identifier identifier, String author,
                     int pivotX, int pivotY, int pivotZ, List<String> tags, byte[] icon) {
        this(protoBlueprint.blockStates(), protoBlueprint.blockEntities(),
                protoBlueprint.sizeX(), protoBlueprint.sizeY(), protoBlueprint.sizeZ(),
                identifier, author, pivotX, pivotY, pivotZ, tags, icon);
    }

    public Identifier getTextureId() {
        if (textureId != null) {
            return textureId;
        }

        ByteBuffer buffer = null;
        try {
            buffer = MemoryUtil.memAlloc(icon.length);
            buffer.put(icon);
            buffer.rewind();
            NativeImage nativeImage = NativeImage.read(buffer);

            AbstractTexture texture = new NativeImageBackedTexture(nativeImage);

            this.textureId = new Identifier(identifier.getNamespace(), identifier.getPath() + "_icon");
            MinecraftClient.getInstance().getTextureManager().registerTexture(this.textureId, texture);
            return textureId;
        } catch(Exception e) {
            throw new RuntimeException("Failed to create image", e);
        } finally {
            if (buffer != null) {
                MemoryUtil.memFree(buffer);
            }
        }
    }

    public void write(PacketByteBuf buf) {
        // Write basic information
        buf.writeVarInt(version);

        buf.writeVarInt(sizeX);
        buf.writeVarInt(sizeY);
        buf.writeVarInt(sizeZ);

        buf.writeVarInt(pivotX);
        buf.writeVarInt(pivotY);
        buf.writeVarInt(pivotZ);

        buf.writeIdentifier(identifier);
        buf.writeString(author);

        buf.writeVarInt(tags.size());
        tags.forEach(buf::writeString);

        buf.writeByteArray(icon);

        Short2IntMap paletteMap = new Short2IntRBTreeMap();
        ShortList stateList = new ShortArrayList();
        int totalStates = 0;
        int totalBlocks = 0;

        Short2IntMap stateCount = new Short2IntRBTreeMap();

        for (int x = 0; x < this.sizeX; x++) {
            for (int y = 0; y < this.sizeY; y++) {
                for (int z = 0; z < this.sizeZ; z++) {
                    BlockState state = this.blockStates[x][y][z];

                    totalBlocks++;
                    short stateId = (short) Block.getRawIdFromState(state);

                    if (!paletteMap.containsKey(stateId)) {
                        paletteMap.put(stateId, totalStates++);
                        stateList.add(stateId);
                    }

                    stateCount.compute(stateId, (id, count) -> {
                        if (count == null) return 1;
                        return count+1;
                    });
                }
            }
        }

        // Write palette
        buf.writeVarInt(totalStates);
        for (short stateId : stateList) {
            buf.writeShort(stateId);
        }

        // Actual blocks
        for (int y = 0; y<this.sizeY; y++) {
            for (int x = 0; x<this.sizeX; x++) {
                for (int z = 0; z<this.sizeZ; z++) {
                    BlockState state = this.blockStates[x][y][z];
                    buf.writeVarInt(paletteMap.get((short)Block.getRawIdFromState(state)));
                }
            }
        }
    }

    public void save(File file) {
        File blueprintFile = new File(file, identifier.getNamespace()+"/"+identifier.getPath()+".blueprint");
        blueprintFile.getParentFile().mkdirs();

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        this.write(buf);

        try {
            Files.write(blueprintFile.toPath(), buf.getWrittenBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Blueprint load(File file) {
        try {
            ByteBuf byteBuf = Unpooled.wrappedBuffer(Files.readAllBytes(file.toPath()));
            PacketByteBuf buf = new PacketByteBuf(byteBuf);

            int version = buf.readVarInt();
            int sizeX = buf.readVarInt();
            int sizeY = buf.readVarInt();
            int sizeZ = buf.readVarInt();
            int pivotX = buf.readVarInt();
            int pivotY = buf.readVarInt();
            int pivotZ = buf.readVarInt();

            Identifier identifier = buf.readIdentifier();
            String author = buf.readString();

            int tagsLength = buf.readVarInt();
            List<String> tags = new ArrayList<>(tagsLength);
            for (int i=0; i<tagsLength; i++) {
                tags.add(buf.readString());
            }

            byte[] icon = buf.readByteArray();

            Int2ShortMap paletteMap = new Int2ShortRBTreeMap();

            int currentPaletteSize = 0;
            int paletteEntries = buf.readVarInt();
            for (int i=0; i<paletteEntries; i++) {
                paletteMap.put(currentPaletteSize++, buf.readShort());
            }

            BlockState[][][] blockStates = new BlockState[sizeX][sizeY][sizeZ];

            for (int y = 0; y < sizeY; y++) {
                for (int x = 0; x < sizeX; x++) {
                    for (int z = 0; z < sizeZ; z++) {
                        short stateId = paletteMap.get(buf.readVarInt());
                        blockStates[x][y][z] = Block.getStateFromRawId(stateId);
                    }
                }
            }

            return new Blueprint(blockStates, null, sizeX, sizeY, sizeZ, identifier, author, pivotX,
                    pivotY, pivotZ, tags, icon);
        } catch(IOException e) {
            return null;
        }
    }

}
