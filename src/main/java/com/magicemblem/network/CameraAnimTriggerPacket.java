package com.magicemblem.network;

import com.magicemblem.MagicEmblem;
import com.magicemblem.client.CameraAnimationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 运镜触发包（服务端 -> 客户端）
 * 
 * 服务端检测到玩家放置魔法校徽方块时发送，
 * 通知客户端播放运镜动画。
 * 
 * 校歌播放已分离到 PlayAnthemPacket，不再由此包处理。
 */
public class CameraAnimTriggerPacket {

    /** 方块位置（用于确定运镜中心点） */
    private BlockPos blockPos;

    public CameraAnimTriggerPacket() {}

    public CameraAnimTriggerPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public static void encode(CameraAnimTriggerPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.blockPos);
    }

    public static CameraAnimTriggerPacket decode(FriendlyByteBuf buf) {
        CameraAnimTriggerPacket packet = new CameraAnimTriggerPacket();
        packet.blockPos = buf.readBlockPos();
        return packet;
    }

    public static void handle(CameraAnimTriggerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            MagicEmblem.LOGGER.info("[MagicEmblem] Received camera anim trigger packet for pos {}", packet.blockPos);
            // 仅触发运镜动画（校歌已分离到 PlayAnthemPacket）
            CameraAnimationManager.startAnimation(packet.blockPos);
        });
        context.setPacketHandled(true);
    }
}
