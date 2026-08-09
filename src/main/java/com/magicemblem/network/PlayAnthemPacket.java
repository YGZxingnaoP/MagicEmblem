package com.magicemblem.network;

import com.magicemblem.MagicEmblem;
import com.magicemblem.school.SchoolRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 校歌播放包（服务端 -> 客户端）
 *
 * 服务端在以下事件触发时发送：
 * - 方块放置时
 * - 右键打开认证界面时
 * - 获得严重违纪buff时
 *
 * 通知客户端根据 schoolId 查找对应校歌并播放。
 * 播放时会掐停原版游戏背景音乐，并确保同一玩家不会同时播放多个校歌。
 * 若校歌已在播放中，则继续播放当前的，不重新开始。
 */
public class PlayAnthemPacket {

    /** 学校标识 */
    private String schoolId;

    /** 当前正在播放的校歌音效实例（静态引用，供停止按钮使用） */
    private static SimpleSoundInstance currentAnthemInstance;

    /** 当前播放的校歌对应的学校ID */
    private static String currentSchoolId;

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

            // 如果校歌正在播放且是同一首，则继续播放，不重新开始
            if (isAnthemPlaying() && packet.schoolId.equals(currentSchoolId)) {
                MagicEmblem.LOGGER.info("[MagicEmblem] Anthem already playing for {}, skipping", packet.schoolId);
                return;
            }

            // 停止上一首正在播放的校歌
            stopCurrentAnthem();

            // 掐停原版游戏背景音乐
            stopBackgroundMusic(mc);

            // 在玩家位置播放校歌
            Vec3 playerPos = mc.player.position();
            currentAnthemInstance = SimpleSoundInstance.forRecord(sound, playerPos);
            currentSchoolId = packet.schoolId;
            mc.getSoundManager().play(currentAnthemInstance);
        });
        context.setPacketHandled(true);
    }

    /**
     * 掐停原版游戏背景音乐
     * 停止 Minecraft 的 MUSIC 声道中正在播放的所有音效
     */
    private static void stopBackgroundMusic(Minecraft mc) {
        try {
            SoundManager soundManager = mc.getSoundManager();
            // 停止所有 MUSIC 声道的音效（原版背景音乐）
            soundManager.stop(null, net.minecraft.sounds.SoundSource.MUSIC);
        } catch (Exception e) {
            MagicEmblem.LOGGER.warn("[MagicEmblem] Failed to stop background music", e);
        }
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
            currentSchoolId = null;
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
