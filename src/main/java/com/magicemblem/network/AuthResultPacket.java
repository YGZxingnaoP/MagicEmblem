package com.magicemblem.network;

import com.magicemblem.client.gui.AuthScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 认证结果数据包（服务端 -> 客户端）
 * 通知客户端认证是否成功，用于更新GUI显示
 */
public class AuthResultPacket {

    /** 是否认证成功 */
    private boolean success;
    /** 消息文本 */
    private String message;

    public AuthResultPacket() {}

    public AuthResultPacket(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static void encode(AuthResultPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.success);
        buf.writeUtf(packet.message);
    }

    public static AuthResultPacket decode(FriendlyByteBuf buf) {
        AuthResultPacket packet = new AuthResultPacket();
        packet.success = buf.readBoolean();
        packet.message = buf.readUtf(256);
        return packet;
    }

    public static void handle(AuthResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 在客户端线程更新GUI
            AuthScreen.setResult(packet.success, packet.message);
        });
        context.setPacketHandled(true);
    }
}
