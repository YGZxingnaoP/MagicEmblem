package com.magicemblem.network;

import com.magicemblem.client.gui.AuthScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 积分响应数据包（服务端 -> 客户端）
 * 服务端返回玩家的积分数据
 */
public class ScoreResponsePacket {

    private double score;

    public ScoreResponsePacket() {}

    public ScoreResponsePacket(double score) {
        this.score = score;
    }

    public static void encode(ScoreResponsePacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.score);
    }

    public static ScoreResponsePacket decode(FriendlyByteBuf buf) {
        ScoreResponsePacket packet = new ScoreResponsePacket();
        packet.score = buf.readDouble();
        return packet;
    }

    public static void handle(ScoreResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 客户端线程：更新 AuthScreen 的积分显示
            AuthScreen.setScore(packet.score);
        });
        context.setPacketHandled(true);
    }
}
