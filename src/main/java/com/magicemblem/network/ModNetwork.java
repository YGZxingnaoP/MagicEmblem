package com.magicemblem.network;

import com.magicemblem.MagicEmblem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络包注册中心
 * 注册所有客户端↔服务端的网络数据包
 */
public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    /** 网络通道 */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MagicEmblem.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    /**
     * 注册所有网络数据包
     * 在FMLCommonSetupEvent中调用
     */
    public static void register() {
        // 客户端 -> 服务端：认证请求（学号 + 密码）
        CHANNEL.registerMessage(0, AuthRequestPacket.class,
                AuthRequestPacket::encode,
                AuthRequestPacket::decode,
                AuthRequestPacket::handle);

        // 服务端 -> 客户端：认证结果
        CHANNEL.registerMessage(1, AuthResultPacket.class,
                AuthResultPacket::encode,
                AuthResultPacket::decode,
                AuthResultPacket::handle);

        // 客户端 -> 服务端：积分输入状态更新
        CHANNEL.registerMessage(2, ScoreUpdatePacket.class,
                ScoreUpdatePacket::encode,
                ScoreUpdatePacket::decode,
                ScoreUpdatePacket::handle);

        // 服务端 -> 客户端：运镜动画触发（首次放置魔法校徽方块）
        CHANNEL.registerMessage(3, CameraAnimTriggerPacket.class,
                CameraAnimTriggerPacket::encode,
                CameraAnimTriggerPacket::decode,
                CameraAnimTriggerPacket::handle);

        // 客户端 -> 服务端：积分查询
        CHANNEL.registerMessage(4, ScoreQueryPacket.class,
                ScoreQueryPacket::encode,
                ScoreQueryPacket::decode,
                ScoreQueryPacket::handle);

        // 服务端 -> 客户端：积分响应
        CHANNEL.registerMessage(5, ScoreResponsePacket.class,
                ScoreResponsePacket::encode,
                ScoreResponsePacket::decode,
                ScoreResponsePacket::handle);

        // 服务端 -> 客户端：播放校歌（携带 schoolId）
        CHANNEL.registerMessage(6, PlayAnthemPacket.class,
                PlayAnthemPacket::encode,
                PlayAnthemPacket::decode,
                PlayAnthemPacket::handle);
    }
}
