package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.PokebagManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

// [线性-B05zf] 请求Pokebag库存同步：客户端→服务端，打开战斗背包界面时请求
public record RequestPokebagContentsPacket() implements CustomPacketPayload {

    public static final Type<RequestPokebagContentsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "request_pokebag_contents"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPokebagContentsPacket> STREAM_CODEC =
            StreamCodec.unit(new RequestPokebagContentsPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // [线性-B05zf] 服务端处理：遍历Pokebag库存，打包为SyncPokebagContentsPacket发回客户端
    public static void handleServerSide(RequestPokebagContentsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;

            PokebagManager manager = CharmPlayerData.get(serverPlayer).getPokebagManager();
            List<SyncPokebagContentsPacket.ItemEntry> entries = new ArrayList<>();

            for (int i = 0; i < manager.getBagInventory().getContainerSize(); i++) {
                ItemStack stack = manager.getBagInventory().getItem(i);
                if (!stack.isEmpty()) {
                    String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getKey(stack.getItem()).toString();
                    entries.add(new SyncPokebagContentsPacket.ItemEntry(itemId, stack.getCount()));
                }
            }
            PacketDistributor.sendToPlayer(serverPlayer,
                    new SyncPokebagContentsPacket(entries));
        });
    }
}