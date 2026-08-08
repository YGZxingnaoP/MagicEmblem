package com.magicemblem.network;

import com.magicemblem.score.PlayerScoreManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 积分查询数据包（客户端 -> 服务端）
 * 客户端打开UI时请求自己的积分数据
 */
public class ScoreQueryPacket {

    public ScoreQueryPacket() {}

    public static void encode(ScoreQueryPacket packet, FriendlyByteBuf buf) {
        // 空包，服务端自动返回发送者的积分
    }

    public static ScoreQueryPacket decode(FriendlyByteBuf buf) {
        return new ScoreQueryPacket();
    }

    public static void handle(ScoreQueryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            String name = player.getGameProfile().getName();
            double score = PlayerScoreManager.getScore(name);

            // 发送积分响应到客户端
            ModNetwork.CHANNEL.sendTo(
                    new ScoreResponsePacket(score),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT);
        });
        context.setPacketHandled(true);
    }
}
