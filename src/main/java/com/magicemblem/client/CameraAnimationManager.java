package com.magicemblem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 相机动画管理器（客户端）
 * 
 * 首次放置魔法校徽方块时触发电影级运镜：
 * 
 * 阶段1（0~5秒）：
 * - 相机从方块正前方1格处开始
 * - FOV强制40
 * - 缓慢向前推进0.5格
 * - 末尾黑场淡出
 * 
 * 阶段2（5~10秒）：
 * - 以方块为中心，从正面偏移30°处开始
 * - 3格距离环绕旋转180°（实际 ORBIT_RADIUS=3.0）
 * - 相机始终朝向方块中心
 * 
 * 总时长10秒，播放校歌音频
 * 
 * 使用 System.currentTimeMillis() 驱动，不依赖帧率
 */
public class CameraAnimationManager {

    /** 动画总时长（毫秒） */
    private static final long TOTAL_DURATION_MS = 10000;
    /** 阶段1结束时间（毫秒） */
    private static final long PHASE1_END_MS = 5000;
    /** 淡入时长（毫秒） */
    private static final long FADE_IN_MS = 500;
    /** 黑场淡出时长（毫秒） */
    private static final long FADE_OUT_MS = 500;
    /** 推进距离（格） */
    private static final double PUSH_DISTANCE = 0.5;
    /** 环绕半径（格） */
    private static final double ORBIT_RADIUS = 3.0;
    /** 阶段1起始距离（格）- 方块正前方3格 */
    private static final double START_DISTANCE = 3.0;
    /** 起始角度偏移（度）- 从正面偏移30° */
    private static final double START_ANGLE_DEG = 30.0;
    /** 旋转总角度（度）- 180° */
    private static final double ROTATE_DEG = 180.0;

    // ===== 动画状态 =====
    private static boolean active = false;
    private static long startTime = 0;
    private static long elapsedMs = 0;
    private static BlockPos targetBlock = null;
    private static float originalFov = 70.0f;
    private static Vec3 playerStartPos = null;
    /** 动画开始时玩家朝向角度（用于确定方块"正面"方向） */
    private static float startYaw = 0.0f;

    public static void startAnimation(BlockPos blockPos) {
        targetBlock = blockPos;
        startTime = System.currentTimeMillis();
        elapsedMs = 0;
        active = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            originalFov = mc.options.fov().get().floatValue();
        }
        if (mc.player != null) {
            playerStartPos = mc.player.getEyePosition(1.0f);
            startYaw = mc.player.getYRot();
        }
    }

    /**
     * 更新动画时间（由 CameraAnimationEvents 每帧调用）
     */
    public static void update() {
        if (!active) return;
        elapsedMs = System.currentTimeMillis() - startTime;
        if (elapsedMs >= TOTAL_DURATION_MS) {
            stop();
        }
    }

    public static void stop() {
        active = false;
        elapsedMs = 0;
        targetBlock = null;
        playerStartPos = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            mc.options.fov().set((int) originalFov);
        }
    }

    public static void reset() {
        active = false;
        elapsedMs = 0;
        targetBlock = null;
        playerStartPos = null;
    }

    public static boolean isActive() {
        return active;
    }

    public static float getProgress() {
        return active ? (float) elapsedMs / TOTAL_DURATION_MS : 0.0f;
    }

    /** 动画期间强制 FOV = 40 */
    public static float getCurrentFov() {
        if (!active) return originalFov;
        return 40.0f;
    }

    // ===== 相机位置计算 =====

    /**
     * 获取当前相机位置
     * 
     * 阶段1：方块正前方（与玩家朝向相反的一侧），缓慢推进
     * 阶段2：围绕方块旋转180°，始终朝向方块
     */
    public static Vec3 getCameraPosition() {
        if (!active || targetBlock == null) return null;
    
        double bx = targetBlock.getX() + 0.5;
        double by = targetBlock.getY() + 0.5;
        double bz = targetBlock.getZ() + 0.5;
    
        // 计算方块“正面”方向（与玩家朝向相反，确保相机在玩家对面看方块）
        // Minecraft: yaw=0 面朝南(+Z), yaw=90 面朝西(-X)
        // 玩家朝向方向: (-sin(yaw), 0, cos(yaw))
        // 相机应该在反方向: (sin(yaw), 0, -cos(yaw))
        float yawRad = (float) Math.toRadians(startYaw);
        double frontX = Math.sin(yawRad);
        double frontZ = -Math.cos(yawRad);
    
        // === 阶段1：从正面推进 ===
        if (elapsedMs < PHASE1_END_MS) {
            float phase1Progress = (float) elapsedMs / PHASE1_END_MS;
            // 从2格距离开始，推进0.5格
            double distance = START_DISTANCE - PUSH_DISTANCE * phase1Progress;
            double camX = bx + frontX * distance;
            double camY = by - 0.1; // 略低于方块中心，形成仰视角度
            double camZ = bz + frontZ * distance;
    
            // 淡入：从玩家位置过渡
            if (elapsedMs < FADE_IN_MS && playerStartPos != null) {
                float t = easeInOutCubic((float) elapsedMs / FADE_IN_MS);
                return new Vec3(
                        Mth.lerp(t, playerStartPos.x, camX),
                        Mth.lerp(t, playerStartPos.y, camY),
                        Mth.lerp(t, playerStartPos.z, camZ));
            }
            return new Vec3(camX, camY, camZ);
        }
    
        // === 阶段2：环绕旋转 ===
        // 从方块正面开始，旋转180°到背面
        float phase2Progress = (float) (elapsedMs - PHASE1_END_MS) / (TOTAL_DURATION_MS - PHASE1_END_MS);
        // 起始角度 = startYaw（对应正面方向），旋转180°
        double angleDeg = startYaw + ROTATE_DEG * phase2Progress;
        double angleRad = Math.toRadians(angleDeg);
    
        double orbitX = bx + Math.sin(angleRad) * ORBIT_RADIUS;
        double orbitY = by + 0.2; // 略高于方块中心，保持仰视角度
        double orbitZ = bz + Math.cos(angleRad) * ORBIT_RADIUS;
    
        return new Vec3(orbitX, orbitY, orbitZ);
    }

    /**
     * 获取相机应看向的目标点（始终看向方块中心偏上）
     */
    public static Vec3 getLookTarget() {
        if (!active || targetBlock == null) return null;
        return new Vec3(
                targetBlock.getX() + 0.5,
                targetBlock.getY() + 0.6,
                targetBlock.getZ() + 0.5);
    }

    // ===== 黑场淡出 =====

    /**
     * 获取黑场淡出透明度（0=全透明，1=全黑）
     * 动画开始时淡入（从黑到透明），阶段1末尾淡出（从透明到黑）
     */
    public static float getFadeAlpha() {
        if (!active) return 0.0f;

        // 开始淡入（前500ms 从黑到透明）
        if (elapsedMs < FADE_IN_MS) {
            return 1.0f - (float) elapsedMs / FADE_IN_MS;
        }

        // 阶段1末尾黑场（最后500ms 从透明到黑）
        if (elapsedMs >= PHASE1_END_MS - FADE_OUT_MS && elapsedMs < PHASE1_END_MS) {
            return (float) (elapsedMs - (PHASE1_END_MS - FADE_OUT_MS)) / FADE_OUT_MS;
        }

        // 阶段2开始淡入（前500ms 从黑到透明）
        if (elapsedMs >= PHASE1_END_MS && elapsedMs < PHASE1_END_MS + FADE_IN_MS) {
            return 1.0f - (float) (elapsedMs - PHASE1_END_MS) / FADE_IN_MS;
        }

        // 动画结束淡出（最后500ms 从透明到黑）
        if (elapsedMs >= TOTAL_DURATION_MS - FADE_OUT_MS) {
            return (float) (elapsedMs - (TOTAL_DURATION_MS - FADE_OUT_MS)) / FADE_OUT_MS;
        }

        return 0.0f;
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0 * t + 2.0, 3) / 2.0f;
    }
}
