package com.magicemblem.network;

import com.magicemblem.common.event.ModEventHandlers;
import com.magicemblem.init.ModEffects;
import com.magicemblem.school.SchoolPasswordManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 认证请求数据包（客户端 -> 服务端）
 * 玩家输入学号 + 密码后发送给服务端进行验证
 */
public class AuthRequestPacket {

    /** 学号 */
    private String studentId;
    /** 密码 */
    private String password;
    /** 学校标识 */
    private String schoolId;

    public AuthRequestPacket() {}

    public AuthRequestPacket(String studentId, String password, String schoolId) {
        this.studentId = studentId;
        this.password = password;
        this.schoolId = schoolId;
    }

    public static void encode(AuthRequestPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.studentId);
        buf.writeUtf(packet.password);
        buf.writeUtf(packet.schoolId);
    }

    public static AuthRequestPacket decode(FriendlyByteBuf buf) {
        AuthRequestPacket packet = new AuthRequestPacket();
        packet.studentId = buf.readUtf(64);
        packet.password = buf.readUtf(128);
        packet.schoolId = buf.readUtf(32);
        return packet;
    }

    public static void handle(AuthRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 防止重复认证：已认证过的玩家不再处理
            CompoundTag pdata = player.getPersistentData();
            boolean alreadyAuthed = pdata.contains("magicemblem")
                    && pdata.getCompound("magicemblem").getBoolean("authenticated");
            if (alreadyAuthed) {
                ModNetwork.CHANNEL.sendTo(
                        new AuthResultPacket(false, "你已经认证过了，无需重复验证。"),
                        player.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT);
                return;
            }

            // 使用 packet 中的 schoolId 进行本地密码比对
            String school = packet.schoolId != null && !packet.schoolId.isEmpty() ? packet.schoolId : "USST";
            boolean success = SchoolPasswordManager.authenticate(school, packet.password);

            if (success) {
                // 认证成功：给予"我是光荣的大学牲"buff
                player.addEffect(new MobEffectInstance(
                        ModEffects.GLORIOUS_STUDENT.get(), Integer.MAX_VALUE, 0, false, false));

                // 触发认证成就
                ModEventHandlers.onAuthenticationSuccess(player);

                // 发送成功结果到客户端
                ModNetwork.CHANNEL.sendTo(
                        new AuthResultPacket(true, "认证成功！欢迎，" + school + "的同学！"),
                        player.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT);
            } else {
                // 认证失败
                ModNetwork.CHANNEL.sendTo(
                        new AuthResultPacket(false, "认证失败，密码不正确。"),
                        player.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT);
            }
        });
        context.setPacketHandled(true);
    }
}
