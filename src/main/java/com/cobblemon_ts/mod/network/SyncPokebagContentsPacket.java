package com.cobblemon_ts.mod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

// [线性-B05zf] Pokebag库存同步包：服务端→客户端，携带物品ID和数量列表
public record SyncPokebagContentsPacket(List<ItemEntry> items) implements CustomPacketPayload {

    // [线性-B05zf] 物品条目：含物品ID、数量，以及客户端渲染缓存
    // [线性-O06] 添加renderStack和cachedName避免每帧创建对象
    public static final class ItemEntry {
        private final String itemId;
        private final int count;
        // [线性-O06] 客户端渲染缓存，延迟初始化
        private ItemStack renderStack;
        private String cachedName;

        public ItemEntry(String itemId, int count) {
            this.itemId = itemId;
            this.count = count;
        }

        public String itemId() { return itemId; }
        public int count() { return count; }

        // [线性-O06] 获取渲染用的ItemStack（延迟初始化，复用）
        public ItemStack renderStack() { return renderStack; }
        public void setRenderStack(ItemStack stack) { this.renderStack = stack; }

        // [线性-O06] 获取缓存的物品名称
        public String cachedName() { return cachedName; }
        public void setCachedName(String name) { this.cachedName = name; }
    }

    public static final Type<SyncPokebagContentsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "sync_pokebag_contents"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPokebagContentsPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncPokebagContentsPacket decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<ItemEntry> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                entries.add(new ItemEntry(buf.readUtf(), buf.readVarInt()));
            }
            return new SyncPokebagContentsPacket(entries);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SyncPokebagContentsPacket packet) {
            buf.writeVarInt(packet.items().size());
            for (ItemEntry entry : packet.items()) {
                buf.writeUtf(entry.itemId());
                buf.writeVarInt(entry.count());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // [线性-B05zf] 客户端处理：缓存到PokebagClientCache，供PkBagActionSelection读取
    public static void handleClientSide(SyncPokebagContentsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            PokebagClientCache.update(packet.items());
        });
    }
}