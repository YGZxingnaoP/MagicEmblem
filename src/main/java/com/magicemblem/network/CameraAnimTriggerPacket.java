package com.magicemblem.network;

import com.magicemblem.MagicEmblem;
import com.magicemblem.client.CameraAnimationManager;
import com.magicemblem.common.blockentity.AbstractEmblemBlockEntity;
import com.magicemblem.school.SchoolRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 运镜触发包（服务端 -> 客户端）
 * 
 * 服务端检测到玩家首次放置魔法校徽方块时发送，
 * 通知客户端播放运镜动画和校歌。
 * 
 * 仅在每个玩家的生涯中首次放置时触发（玩家级别flag，非方块级别）。
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
            // 触发运镜动画
            CameraAnimationManager.startAnimation(packet.blockPos);
            // 根据方块实体获取学校ID播放对应校歌
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                String schoolId = "USST"; // 默认
                if (mc.level.getBlockEntity(packet.blockPos) instanceof AbstractEmblemBlockEntity be
                        && be.getSchoolId() != null) {
                    schoolId = be.getSchoolId();
                }
                SoundEvent anthem = SchoolRegistry.getAnthem(schoolId);
                if (anthem != null) {
                    mc.level.playSound(null, packet.blockPos,
                            anthem, SoundSource.RECORDS, 3.0f, 1.0f);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
