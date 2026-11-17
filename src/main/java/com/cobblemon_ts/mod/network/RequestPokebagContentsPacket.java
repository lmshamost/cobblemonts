package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.PokebagManager;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// [线性-B05zf] 请求Pokebag库存同步：客户端→服务端，打开战斗背包界面时请求
public record RequestPokebagContentsPacket() implements CustomPayload {

    public static final Id<RequestPokebagContentsPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "request_pokebag_contents"));

    public static final PacketCodec<RegistryByteBuf, RequestPokebagContentsPacket> STREAM_CODEC =
            PacketCodec.unit(new RequestPokebagContentsPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    // [线性-B05zf] 服务端处理：遍历Pokebag库存，打包为SyncPokebagContentsPacket发回客户端
    public static void handleServerSide(RequestPokebagContentsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayerEntity player)) return;

            PokebagManager manager = CharmPlayerData.get(player).getPokebagManager();
            List<SyncPokebagContentsPacket.ItemEntry> entries = new ArrayList<>();

            for (int i = 0; i < manager.getBagInventory().size(); i++) {
                ItemStack stack = manager.getBagInventory().getStack(i);
                if (!stack.isEmpty()) {
                    String itemId = net.minecraft.registry.Registries.ITEM
                            .getKey(stack.getItem()).toString();
                    entries.add(new SyncPokebagContentsPacket.ItemEntry(itemId, stack.getCount()));
                }
            }
            PacketDistributor.sendToPlayer(player,
                    new SyncPokebagContentsPacket(entries));
        });
    }
}