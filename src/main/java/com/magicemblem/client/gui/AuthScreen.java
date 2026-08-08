package com.magicemblem.client.gui;

import com.magicemblem.common.blockentity.MagicEmblemBlockEntity;
import com.magicemblem.network.AuthRequestPacket;
import com.magicemblem.network.ModNetwork;
import com.magicemblem.network.PlayAnthemPacket;
import com.magicemblem.network.ScoreQueryPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 认证界面（重新设计）
 * 
 * 布局：
 * - 顶部：USST 校徽图片
 * - 左侧：学号输入框 + 密码输入框 + 验证按钮
 * - 右侧：玩家积分显示面板
 * - 底部：停止音乐按钮 + 认证结果
 */
public class AuthScreen extends Screen {

    private static final ResourceLocation BADGE_TEXTURE =
            new ResourceLocation("magicemblem", "textures/gui/usst_badge.png");

    /** 方块位置 */
    private final BlockPos blockPos;

    /** 学校标识（用于认证和显示） */
    private final String schoolId;

    // --- 输入组件 ---
    private EditBox studentIdBox;
    private EditBox passwordBox;
    private Button verifyButton;
    private Button stopMusicButton;

    // --- 积分显示 ---
    private static double playerScore = 0.0;
    private static boolean scoreReceived = false;

    // --- 认证结果 ---
    private static boolean authResultSuccess = false;
    private static String authResultMessage = "";
    private static boolean hasResult = false;

    // --- 布局常量 ---
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 260;
    private static final int BADGE_SIZE = 80;

    // --- 颜色 ---
    private static final int COLOR_PANEL_BG = 0xDD1A1A2E;
    private static final int COLOR_PANEL_BORDER = 0xFF3D5AFE;
    private static final int COLOR_SECTION_BG = 0xCC252540;
    private static final int COLOR_TITLE = 0xFFE8B84B;
    private static final int COLOR_LABEL = 0xFFBBBBCC;
    private static final int COLOR_SCORE_VALUE = 0xFF4FC3F7;
    private static final int COLOR_SUCCESS = 0xFF4CAF50;
    private static final int COLOR_ERROR = 0xFFE53935;

    public AuthScreen(BlockPos blockPos, String schoolId) {
        super(Component.translatable("gui.magicemblem.auth.title"));
        this.blockPos = blockPos;
        this.schoolId = schoolId != null ? schoolId : "USST";
    }

    /**
     * 接收认证结果（由 AuthResultPacket 调用）
     */
    public static void setResult(boolean success, String message) {
        authResultSuccess = success;
        authResultMessage = message;
        hasResult = true;
    }

    /**
     * 接收积分数据（由 ScoreResponsePacket 调用）
     */
    public static void setScore(double score) {
        playerScore = score;
        scoreReceived = true;
    }

    @Override
    protected void init() {
        super.init();
        hasResult = false;
        scoreReceived = false;
        playerScore = 0.0;

        // 请求积分数据
        ModNetwork.CHANNEL.sendToServer(new ScoreQueryPacket());

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // 左右分栏坐标
        int leftX = panelX + 16;
        int rightX = panelX + PANEL_WIDTH / 2 + 10;
        int inputWidth = PANEL_WIDTH / 2 - 26;
        int contentTop = panelY + BADGE_SIZE + 20;

        // ===== 左侧：输入区 =====

        // 学号输入框
        studentIdBox = new EditBox(this.font, leftX, contentTop + 14, inputWidth, 20,
                Component.translatable("gui.magicemblem.auth.student_id"));
        studentIdBox.setMaxLength(32);
        studentIdBox.setHint(Component.translatable("gui.magicemblem.auth.student_id.hint"));
        addRenderableWidget(studentIdBox);

        // 密码输入框
        passwordBox = new EditBox(this.font, leftX, contentTop + 50, inputWidth, 20,
                Component.translatable("gui.magicemblem.auth.password"));
        passwordBox.setMaxLength(128);
        passwordBox.setHint(Component.translatable("gui.magicemblem.auth.password.hint"));
        addRenderableWidget(passwordBox);

        // 验证按钮
        verifyButton = Button.builder(
                Component.translatable("gui.magicemblem.auth.verify"),
                this::onVerifyClicked)
                .bounds(leftX, contentTop + 82, inputWidth, 20)
                .build();
        addRenderableWidget(verifyButton);

        // ===== 右下角：停止音乐按钮 =====
        stopMusicButton = Button.builder(
                Component.translatable("gui.magicemblem.auth.stop_music"),
                this::onStopMusicClicked)
                .bounds(panelX + PANEL_WIDTH - 110, panelY + PANEL_HEIGHT - 30, 100, 20)
                .build();
        addRenderableWidget(stopMusicButton);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 半透明全屏背景
        g.fill(0, 0, this.width, this.height, 0x88000000);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // ===== 面板背景 + 边框 =====
        // 外边框
        g.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT + 1, COLOR_PANEL_BORDER);
        // 面板填充
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, COLOR_PANEL_BG);

        // ===== 顶部校徽图片 =====
        int badgeX = panelX + (PANEL_WIDTH - BADGE_SIZE) / 2;
        int badgeY = panelY + 8;
        g.blit(BADGE_TEXTURE, badgeX, badgeY, 0, 0, BADGE_SIZE, BADGE_SIZE, BADGE_SIZE, BADGE_SIZE);

        // ===== 标题 =====
        Component title = Component.translatable("gui.magicemblem.auth.title");
        int titleWidth = this.font.width(title);
        g.drawString(this.font, title,
                panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + BADGE_SIZE + 4,
                COLOR_TITLE, true);

        // ===== 左侧标签 =====
        int leftX = panelX + 16;
        int contentTop = panelY + BADGE_SIZE + 20;

        g.drawString(this.font,
                Component.translatable("gui.magicemblem.auth.student_id.label"),
                leftX, contentTop + 2, COLOR_LABEL, false);

        g.drawString(this.font,
                Component.translatable("gui.magicemblem.auth.password.label"),
                leftX, contentTop + 38, COLOR_LABEL, false);

        // ===== 右侧积分面板 =====
        int rightX = panelX + PANEL_WIDTH / 2 + 10;
        int rightWidth = PANEL_WIDTH / 2 - 26;

        // 积分面板背景
        g.fill(rightX - 4, contentTop, rightX + rightWidth + 4, contentTop + 102, COLOR_SECTION_BG);

        // 积分标题
        Component scoreTitle = Component.translatable("gui.magicemblem.auth.score_title");
        g.drawString(this.font, scoreTitle, rightX, contentTop + 6, COLOR_LABEL, false);

        // 积分数值
        String scoreText = String.format("%.2f", playerScore);
        int scoreValueWidth = this.font.width(scoreText);
        g.drawString(this.font, scoreText,
                rightX + (rightWidth - scoreValueWidth) / 2, contentTop + 28,
                COLOR_SCORE_VALUE, true);

        // 积分状态（缩小文字防止超出面板）
        Component statusText = scoreReceived
                ? Component.translatable("gui.magicemblem.auth.score_online")
                : Component.translatable("gui.magicemblem.auth.score_loading");
        g.pose().pushPose();
        g.pose().translate(rightX, contentTop + 50, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(this.font, statusText, 0, 0, COLOR_LABEL, false);
        g.pose().popPose();

        // 积分说明（缩小文字防止超出面板）
        g.pose().pushPose();
        g.pose().translate(rightX, contentTop + 66, 0);
        g.pose().scale(0.65f, 0.65f, 1);
        g.drawString(this.font,
                Component.translatable("gui.magicemblem.auth.score_hint"),
                0, 0, 0xFF666677, false);
        g.pose().popPose();

        // ===== 渲染组件（输入框、按钮） =====
        super.render(g, mouseX, mouseY, partialTick);

        // ===== 认证结果 =====
        if (hasResult) {
            int color = authResultSuccess ? COLOR_SUCCESS : COLOR_ERROR;
            int msgWidth = this.font.width(authResultMessage);
            int msgX = panelX + (PANEL_WIDTH - msgWidth) / 2;
            int msgY = panelY + PANEL_HEIGHT - 48;

            // 结果背景条
            g.fill(msgX - 6, msgY - 3, msgX + msgWidth + 6, msgY + 12, 0x44000000);
            g.drawString(this.font, authResultMessage, msgX, msgY, color, false);
        }
    }

    private void onVerifyClicked(Button button) {
        String studentId = studentIdBox.getValue().trim();
        String password = passwordBox.getValue();

        if (studentId.isEmpty()) {
            hasResult = true;
            authResultSuccess = false;
            authResultMessage = "请输入学号";
            return;
        }
        if (password.isEmpty()) {
            hasResult = true;
            authResultSuccess = false;
            authResultMessage = "请输入密码";
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new AuthRequestPacket(studentId, password, schoolId));
        hasResult = true;
        authResultSuccess = false;
        authResultMessage = "验证中...";
    }

    private void onStopMusicClicked(Button button) {
        // 停止方块实体的校歌音效
        if (minecraft != null && minecraft.level != null) {
            var be = minecraft.level.getBlockEntity(blockPos);
            if (be instanceof MagicEmblemBlockEntity emblemBE) {
                emblemBE.stopAnthem();
            }
        }
        // 同时停止通过 PlayAnthemPacket 播放的校歌
        PlayAnthemPacket.stopCurrentAnthem();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }
}
