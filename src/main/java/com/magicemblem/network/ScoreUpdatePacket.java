package com.magicemblem.network;

import com.magicemblem.score.PlayerScoreManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 积分输入状态包（客户端 -> 服务端）
 * 
 * 客户端检测到玩家有WASD/鼠标输入时，发送此包通知服务端。
 * 服务端据此更新玩家最后输入时间，用于积分计算和挂机检测。
 */
public class ScoreUpdatePacket {

    private final boolean isInputting;

    public ScoreUpdatePacket(boolean isInputting) {
        this.isInputting = isInputting;
    }

    public static void encode(ScoreUpdatePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.isInputting);
    }

    public static ScoreUpdatePacket decode(FriendlyByteBuf buf) {
        return new ScoreUpdatePacket(buf.readBoolean());
    }

    public static void handle(ScoreUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && packet.isInputting) {
                PlayerScoreManager.recordInput(player.getGameProfile().getName());
            }
        });
        context.setPacketHandled(true);
    }
}
