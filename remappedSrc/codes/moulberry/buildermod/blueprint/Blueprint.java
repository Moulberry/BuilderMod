package codes.moulberry.buildermod.blueprint;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
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

    // Icon
    private Identifier textureId = null;
    private byte[] icon;

    public Blueprint(ProtoBlueprint protoBlueprint, Identifier identifier, String author,
                     int pivotX, int pivotY, int pivotZ, List<String> tags, byte[] icon) {
        this.blockStates = protoBlueprint.blockStates();
        this.blockEntities = protoBlueprint.blockEntities();
        this.sizeX = protoBlueprint.sizeX();
        this.sizeY = protoBlueprint.sizeY();
        this.sizeZ = protoBlueprint.sizeZ();
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.pivotZ = pivotZ;

        this.identifier = identifier;
        this.author = author;
        this.tags = tags;

        this.icon = icon;

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

}
