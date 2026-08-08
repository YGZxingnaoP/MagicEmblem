package com.magicemblem.score;

import com.magicemblem.MagicEmblem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 积分管理器
 * 
 * 管理玩家积分的计算、存储和查询。
 * - 玩家持续输入（WASD+鼠标移动）时，每2秒+0.01分
 * - 挂机超过10分钟后，每10秒-0.01分
 * - 积分按日保存为CSV文件（config/MagicEmblem/scores/yyyy-MM-dd.csv）
 */
public class PlayerScoreManager {

    private static final Map<String, Double> scores = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastInputTime = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastScoreTick = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastPenaltyTick = new ConcurrentHashMap<>();

    /** 积分累加间隔（2秒 = 40 tick） */
    private static final long SCORE_INTERVAL_TICKS = 40;
    /** 每次增加的积分 */
    private static final double SCORE_INCREMENT = 0.01;
    /** 挂机惩罚启动时间（10分钟 = 600,000 毫秒） */
    private static final long AFK_PENALY_START_MS = 600000;
    /** 惩罚间隔（10秒 = 200 tick） */
    private static final long PENALTY_INTERVAL_TICKS = 200;
    /** 每次扣除的积分 */
    private static final double PENALTY_DECREMENT = 0.01;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static String currentDateString = "";
    private static boolean needsSave = false;

    /**
     * 记录玩家输入（客户端发来的消息）
     */
    public static void recordInput(String playerName) {
        lastInputTime.put(playerName, System.currentTimeMillis());
    }

    /**
     * 每 tick 调用，处理积分累加和挂机惩罚
     */
    public static void tick(ServerPlayer player) {
        String name = player.getGameProfile().getName();
        long currentTime = System.currentTimeMillis();
        long gameTime = player.level().getGameTime();

        // 确保有记录
        scores.putIfAbsent(name, 0.0);
        lastInputTime.putIfAbsent(name, currentTime);
        lastScoreTick.putIfAbsent(name, gameTime);
        lastPenaltyTick.putIfAbsent(name, gameTime);

        long lastInput = lastInputTime.get(name);
        long timeSinceInput = currentTime - lastInput;

        // ===== 积分累加 =====
        if (timeSinceInput < AFK_PENALY_START_MS) {
            long lastTick = lastScoreTick.get(name);
            if (gameTime - lastTick >= SCORE_INTERVAL_TICKS) {
                scores.merge(name, SCORE_INCREMENT, Double::sum);
                lastScoreTick.put(name, gameTime);
                needsSave = true;
            }
        }

        // ===== 挂机惩罚 =====
        if (timeSinceInput >= 600000) { // 10分钟 = 600,000 ms
            long lastPTick = lastPenaltyTick.get(name);
            if (gameTime - lastPTick >= PENALTY_INTERVAL_TICKS) {
                double current = scores.getOrDefault(name, 0.0);
                scores.put(name, Math.max(0, current - PENALTY_DECREMENT));
                lastPenaltyTick.put(name, gameTime);
                needsSave = true;
            }
        }
    }

    /**
     * 获取指定玩家积分
     */
    public static double getScore(String playerName) {
        return scores.getOrDefault(playerName, 0.0);
    }

    /**
     * 获取所有玩家积分（不可变副本）
     */
    public static Map<String, Double> getAllScores() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(scores));
    }

    /**
     * 获取积分保存目录路径
     */
    private static Path getScoreDir() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("MagicEmblem").resolve("scores");
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                MagicEmblem.LOGGER.error("Failed to create score directory", e);
            }
        }
        return dir;
    }

    /**
     * 加载今日积分（从CSV文件读取）
     */
    public static void loadScores() {
        scores.clear();
        lastInputTime.clear();
        lastScoreTick.clear();
        lastPenaltyTick.clear();

        currentDateString = LocalDate.now().format(DATE_FORMAT);
        Path csvFile = getScoreDir().resolve(currentDateString + ".csv");

        if (Files.exists(csvFile)) {
            try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        try {
                            scores.put(parts[0].trim(), Double.parseDouble(parts[1].trim()));
                        } catch (NumberFormatException e) {
                            MagicEmblem.LOGGER.warn("Invalid score line: {}", line);
                        }
                    }
                }
            } catch (IOException e) {
                MagicEmblem.LOGGER.error("Failed to load scores", e);
            }
        }
        MagicEmblem.LOGGER.info("Loaded {} player scores from {}", scores.size(), currentDateString);
    }

    /**
     * 保存今日积分到CSV文件
     */
    public static void saveScores() {
        if (!needsSave) return;

        String today = LocalDate.now().format(DATE_FORMAT);
        if (!today.equals(currentDateString)) {
            // 日期变更，重新加载（旧文件已保留）
            loadScores();
        }

        Path csvFile = getScoreDir().resolve(today + ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile)) {
            writer.write("# 玩家名称,积分\n");
            for (Map.Entry<String, Double> entry : scores.entrySet()) {
                writer.write(entry.getKey() + "," + String.format("%.2f", entry.getValue()));
                writer.newLine();
            }
            needsSave = false;
            MagicEmblem.LOGGER.info("Saved {} player scores to {}", scores.size(), today);
        } catch (IOException e) {
            MagicEmblem.LOGGER.error("Failed to save scores", e);
        }
    }

    /**
     * 强制保存（用于服务器关闭等关键时机）
     */
    public static void forceSave() {
        needsSave = true;
        saveScores();
    }
}
