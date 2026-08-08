package com.magicemblem.network;

import com.magicemblem.MagicEmblem;
import com.magicemblem.school.SchoolRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 校歌播放包（服务端 -> 客户端）
 *
 * 服务端在特定事件（如严重违纪）触发时发送，
 * 通知客户端根据 schoolId 查找对应校歌并播放。
 *
 * 使用 {@link SchoolRegistry} 查找 schoolId 对应的音效。
 */
public class PlayAnthemPacket {

    /** 学校标识 */
    private String schoolId;

    /** 当前正在播放的校歌音效实例（静态引用，供停止按钮使用） */
    private static SimpleSoundInstance currentAnthemInstance;

    public PlayAnthemPacket() {}

    public PlayAnthemPacket(String schoolId) {
        this.schoolId = schoolId;
    }

    public static void encode(PlayAnthemPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.schoolId);
    }

    public static PlayAnthemPacket decode(FriendlyByteBuf buf) {
        PlayAnthemPacket packet = new PlayAnthemPacket();
        packet.schoolId = buf.readUtf(32);
        return packet;
    }

    public static void handle(PlayAnthemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            MagicEmblem.LOGGER.info("[MagicEmblem] Received play anthem packet for school: {}", packet.schoolId);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            SoundEvent sound = SchoolRegistry.getAnthem(packet.schoolId);
            if (sound == null) {
                MagicEmblem.LOGGER.warn("[MagicEmblem] No anthem registered for school: {}", packet.schoolId);
                return;
            }

            // 停止上一首正在播放的校歌
            stopCurrentAnthem();

            // 在玩家位置播放校歌
            Vec3 playerPos = mc.player.position();
            currentAnthemInstance = SimpleSoundInstance.forRecord(sound, playerPos);
            mc.getSoundManager().play(currentAnthemInstance);
        });
        context.setPacketHandled(true);
    }

    /**
     * 停止当前播放的校歌
     * 供 AuthScreen 停止按钮和 PlayAnthemPacket 调用
     */
    public static void stopCurrentAnthem() {
        if (currentAnthemInstance != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSoundManager().isActive(currentAnthemInstance)) {
                mc.getSoundManager().stop(currentAnthemInstance);
            }
            currentAnthemInstance = null;
        }
    }

    /**
     * 获取当前校歌是否正在播放
     */
    public static boolean isAnthemPlaying() {
        if (currentAnthemInstance == null) return false;
        return Minecraft.getInstance().getSoundManager().isActive(currentAnthemInstance);
    }
}
