package codes.moulberry.buildermod;

import com.mojang.brigadier.StringReader;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.impl.item.group.ItemGroupExtensions;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

public class CustomBlocks {

    private static final Identifier REGISTER = new Identifier("gauntlet_build:give_blocks");
    private static final Identifier RECEIVER = new Identifier("gauntlet_build:custom_blocks");

    static {
        createItemGroup();
    }

    private static final Set<ItemStack> CUSTOM_BLOCKS = new LinkedHashSet<>();
    private static final Map<BlockState, ItemStack> STATES_TO_ITEMS = new LinkedHashMap<>();

    public static void sendEnablePacket(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        CUSTOM_BLOCKS.clear();
        STATES_TO_ITEMS.clear();
        ClientPlayNetworking.send(REGISTER, new PacketByteBuf(Unpooled.copiedBuffer("L Ratio", StandardCharsets.UTF_8)));
    }

    public static void setupPackets() {
        ClientPlayNetworking.registerGlobalReceiver(RECEIVER, CustomBlocks::addCustomBlocks);
    }

    public static ItemStack getCustomBlock(BlockState state) {
        return STATES_TO_ITEMS.getOrDefault(state, ItemStack.EMPTY);
    }

    private static void addCustomBlocks(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        CUSTOM_BLOCKS.clear();
        STATES_TO_ITEMS.clear();
        IntStream.range(0, buf.readVarInt())
                .mapToObj(i -> new CustomBlockEntry(buf.readString(), buf.readItemStack(), buf.readList(PacketByteBuf::readString)))
                .forEach(CustomBlockEntry::addToMaps);
    }

    record CustomBlockEntry(String key, ItemStack blockItem, List<String> blockStates) {

        public static void addToMaps(CustomBlockEntry entry) {
            CUSTOM_BLOCKS.add(entry.blockItem);
            for (String blockState : entry.blockStates()) {
                try {
                    BlockState state = BlockArgumentParser.block(Registry.BLOCK, blockState, false).blockState();
                    STATES_TO_ITEMS.put(state, entry.blockItem);
                }catch (Exception ignored) {}
            }
        }
    }

    private static void createItemGroup() {
        ((ItemGroupExtensions) ItemGroup.BUILDING_BLOCKS).fabric_expandArray();
        new ItemGroup(ItemGroup.GROUPS.length - 1, "buildermod.customblocks") {

            @Override
            public ItemStack createIcon() {
                return new ItemStack(Items.BRICK);
            }

            @Override
            public void appendStacks(DefaultedList<ItemStack> stacks) {
                CUSTOM_BLOCKS.stream().map(ItemStack::copy).forEach(stacks::add);
            }
        };
    }
}
