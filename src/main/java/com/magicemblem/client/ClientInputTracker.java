package com.magicemblem.client;

import com.magicemblem.network.ModNetwork;
import com.magicemblem.network.ScoreUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.magicemblem.MagicEmblem;

/**
 * 客户端输入追踪器
 * 
 * 检测玩家的WASD按键和鼠标移动（视角变化），
 * 当有输入时向服务端发送网络包。
 * 打开UI或聊天框时不记录。
 */
@Mod.EventBusSubscriber(modid = MagicEmblem.MODID, value = Dist.CLIENT)
public class ClientInputTracker {

    /** 上次发送的输入状态（避免重复发包） */
    private static boolean lastInputState = false;
    /** 发送间隔计数器（每20 tick = 1秒检测一次） */
    private static int tickCounter = 0;

    /** 上一帧的玩家视角 */
    private static float lastYaw = 0;
    private static float lastPitch = 0;

    /** 持续输入时的定期发包计数器 */
    private static int resendCounter = 0;
    /** 持续输入时每5秒（100 tick）重发一次确认包 */
    private static final int RESEND_INTERVAL = 100;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        tickCounter++;
        if (tickCounter < 20) return; // 每20 tick（1秒）检测一次
        tickCounter = 0;

        // 如果打开了任何UI界面（包括聊天框、物品栏等），不记录输入
        if (mc.screen != null) {
            sendIfChanged(false);
            return;
        }

        boolean hasInput = false;

        // 检测WASD按键（通过KeyMapping，兼容自定义按键绑定）
        if (mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()) {
            hasInput = true;
        }

        // 检测鼠标移动（通过视角变化）
        if (!hasInput) {
            float currentYaw = mc.player.getYRot();
            float currentPitch = mc.player.getXRot();
            if (Math.abs(currentYaw - lastYaw) > 0.01f
                    || Math.abs(currentPitch - lastPitch) > 0.01f) {
                hasInput = true;
            }
            lastYaw = currentYaw;
            lastPitch = currentPitch;
        } else {
            // 有按键输入时同步视角记录
            lastYaw = mc.player.getYRot();
            lastPitch = mc.player.getXRot();
        }

        sendIfChanged(hasInput);
    }

    /**
     * 仅在状态变化时发送网络包，
     * 持续输入时定期重发确认包（防止服务端lastInputTime过期导致误判AFK）
     */
    private static void sendIfChanged(boolean hasInput) {
        if (hasInput != lastInputState) {
            lastInputState = hasInput;
            resendCounter = 0;
            ModNetwork.CHANNEL.sendToServer(new ScoreUpdatePacket(hasInput));
        } else if (hasInput) {
            // 持续输入时定期重发（每5秒一次），确保服务端时间戳保持更新
            resendCounter += 20; // 每次调用间隔为20 tick
            if (resendCounter >= RESEND_INTERVAL) {
                resendCounter = 0;
                ModNetwork.CHANNEL.sendToServer(new ScoreUpdatePacket(true));
            }
        }
    }
}
