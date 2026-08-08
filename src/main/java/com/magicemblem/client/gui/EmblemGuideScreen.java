package com.magicemblem.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 校徽模型添加指南界面
 *
 * 展示如何向模组中添加新的校徽模型的详细步骤。
 * 每个校徽方块可以对应不同的引导界面（由方块的 use 方法决定打开哪个 Screen）。
 *
 * 此界面仅供 Example 方块使用，其他校徽方块应创建自己的引导界面。
 */
public class EmblemGuideScreen extends Screen {

    // --- 布局常量 ---
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 310;

    // --- 颜色 ---
    private static final int COLOR_PANEL_BG = 0xDD1A1A2E;
    private static final int COLOR_PANEL_BORDER = 0xFF3D5AFE;
    private static final int COLOR_TITLE = 0xFFE8B84B;
    private static final int COLOR_STEP = 0xFF4FC3F7;
    private static final int COLOR_CONTENT = 0xFFCCCCCC;
    private static final int COLOR_HINT = 0xFF888899;

    /** 引导步骤内容（详细版） */
    private static final String[] GUIDE_LINES = {
        "1. 用 Blockbench 导出 geo.json 模型文件",
        "   注意: 骨骼名含 GlowingPart 前缀的会发光",
        "2. 导出 Bedrock 动画文件 animation.json",
        "   方块用 idle/on_hand 名称，或统一一个动画名",
        "3. 准备贴图 texture.png 放入 textures/emblem/",
        "4. geo 放入 assets/magicemblem/geo/ 目录",
        "5. animation 放入 assets/magicemblem/animations/",
        "6. 创建 Block 类继承 AbstractEmblemBlock",
        "   重写 getBlockEntityType() 和 use() 方法",
        "7. 创建 BlockEntity 继承 AbstractEmblemBlockEntity",
        "   提供模型/贴图/动画路径和 schoolId",
        "8. 创建 Item 类继承 AbstractEmblemItem",
        "   在 BEWLR 中区分手持/物品栏动画",
        "9. 在 ModBlocks/ModBlockEntities/ModItems 注册",
        "10. 在 MagicEmblem.ClientModEvents 注册渲染器",
        "    BlockEntityRenderers.register(XX_BE, EmblemBlockRenderer::new)",
        "11. 添加 blockstate/model/lang JSON 资源文件",
        "12. (可选) 在 SchoolRegistry 注册校歌音效"
    };

    public EmblemGuideScreen() {
        super(Component.translatable("gui.magicemblem.guide.title"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 半透明全屏背景
        g.fill(0, 0, this.width, this.height, 0x88000000);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // 外边框
        g.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT + 1, COLOR_PANEL_BORDER);
        // 面板填充
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, COLOR_PANEL_BG);

        // 标题
        Component title = Component.translatable("gui.magicemblem.guide.title");
        int titleWidth = this.font.width(title);
        g.drawString(this.font, title,
                panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 12,
                COLOR_TITLE, true);

        // 分隔线
        g.fill(panelX + 16, panelY + 28, panelX + PANEL_WIDTH - 16, panelY + 29, COLOR_PANEL_BORDER);

        // 引导步骤
        int lineY = panelY + 38;
        for (int i = 0; i < GUIDE_LINES.length; i++) {
            String line = GUIDE_LINES[i];
            // 步骤编号用高亮色，补充说明用暗色，内容用普通色
            boolean isSubLine = line.startsWith("   ");
            if (isSubLine) {
                // 补充说明行（缩进、暗色）
                g.drawString(this.font, line, panelX + 20, lineY, COLOR_HINT, false);
            } else {
                int dotPos = line.indexOf('.');
                if (dotPos > 0) {
                    String stepNum = line.substring(0, dotPos + 1);
                    String content = line.substring(dotPos + 1);
                    g.drawString(this.font, stepNum, panelX + 20, lineY, COLOR_STEP, false);
                    g.drawString(this.font, content, panelX + 20 + this.font.width(stepNum) + 4, lineY, COLOR_CONTENT, false);
                } else {
                    g.drawString(this.font, line, panelX + 20, lineY, COLOR_CONTENT, false);
                }
            }
            lineY += isSubLine ? 12 : 14;
        }

        // 底部提示
        Component hint = Component.translatable("gui.magicemblem.guide.close_hint");
        int hintWidth = this.font.width(hint);
        g.drawString(this.font, hint,
                panelX + (PANEL_WIDTH - hintWidth) / 2, panelY + PANEL_HEIGHT - 20,
                COLOR_HINT, false);

        // 渲染子组件（无）
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) { // ESC
            onClose();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }
}
